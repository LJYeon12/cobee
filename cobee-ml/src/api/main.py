"""
FastAPI 메인 애플리케이션

추천 시스템 API 서버
- Phase 자동 업데이트 스케줄러 포함
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
import pytz

# 스케줄러 초기화 시
scheduler = BackgroundScheduler(timezone=pytz.timezone("Asia/Seoul"))

from datetime import datetime
import sys
import os

# 프로젝트 루트를 Python 경로에 추가
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import asyncio
from src.api.routers import recommendations
from src.utils.logger import get_logger
from src.utils.config_loader import ConfigLoader

logger = get_logger(__name__)

# FastAPI 앱 생성
app = FastAPI(
    title="Cobee Roommate Recommendation API",
    description="룸메이트 추천 시스템 API",
    version="1.0.0",
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 특정 도메인만 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(recommendations.router)

# 스케줄러 인스턴스
scheduler = BackgroundScheduler()


def run_phase_update():
    """
    Phase 업데이트 작업

    스케줄러가 호출하는 함수
    """
    logger.info("=" * 60)
    logger.info("스케줄러: Phase 자동 업데이트 시작")
    logger.info(f"실행 시각: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    logger.info("=" * 60)

    try:
        from scripts.update_phase import PhaseUpdater

        updater = PhaseUpdater()
        new_phase = updater.run()

        logger.info(f"스케줄러: Phase 업데이트 완료 - {new_phase}")

    except Exception as e:
        logger.error(f"스케줄러: Phase 업데이트 실패 - {e}", exc_info=True)


async def run_data_sync():
    """
    데이터 동기화 작업을 수행하는 메인 비동기 함수.
    ApiClient와 DataSyncService를 초기화하고 동기화 파이프라인을 실행합니다.
    """
    logger.info("=" * 60)
    logger.info("데이터 동기화 작업 시작...")
    logger.info(f"실행 시각: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    logger.info("=" * 60)
    try:
        from src.clients.msa_backend_client import ApiClient
        from src.services.data_sync_service import DataSyncService

        api_client = ApiClient()
        sync_service = DataSyncService(api_client)
        await sync_service.run_sync()
        logger.info("데이터 동기화 작업 성공적으로 완료.")
    except Exception as e:
        logger.error(f"데이터 동기화 작업 중 오류 발생: {e}", exc_info=True)
    finally:
        logger.info("=" * 60)
        logger.info("데이터 동기화 작업 종료.")
        logger.info("=" * 60)


@app.on_event("startup")
def startup_event():
    """
    서버 시작 시 실행되는 이벤트

    - 스케줄러 시작
    - 초기 Phase 업데이트 (선택사항)
    """
    logger.info("=" * 60)
    logger.info("FastAPI 서버 시작")
    logger.info("=" * 60)

    # Config 로드
    try:
        config = ConfigLoader()
        config.load_config()

        current_phase = config.get_current_phase()
        interaction_count = config.get_interaction_count()

        logger.info(f"현재 Phase: {current_phase}")
        logger.info(f"상호작용 수: {interaction_count}")

    except Exception as e:
        logger.warning(f"Config 로드 실패: {e}")

    # 스케줄러 작업 등록
    try:
        # 1. Phase 업데이트 (매일 새벽 4:50)
        scheduler.add_job(
            run_phase_update,
            CronTrigger(hour=4, minute=50),
            id="phase_update",
            name="Phase 자동 업데이트",
            replace_existing=True,
        )

        # 2. 데이터 동기화 (매 시간)
        loop = asyncio.get_event_loop()
        scheduler.add_job(
            lambda: asyncio.run_coroutine_threadsafe(run_data_sync(), loop),
            "interval",
            hours=1,
            id="data_sync",
            name="MSA 백엔드 데이터 동기화",
            replace_existing=True,
        )

        # 스케줄러 시작
        scheduler.start()

        logger.info("✅ 스케줄러 시작 완료")
        logger.info("   - Phase 자동 업데이트: 매일 04:50")
        logger.info("   - 데이터 동기화: 매 시간")

        # 선택사항: 서버 시작 시 즉시 1회 실행
        # run_phase_update()

    except Exception as e:
        logger.error(f"스케줄러 시작 실패: {e}", exc_info=True)


@app.on_event("shutdown")
def shutdown_event():
    """
    서버 종료 시 실행되는 이벤트

    - 스케줄러 종료
    """
    logger.info("=" * 60)
    logger.info("FastAPI 서버 종료")
    logger.info("=" * 60)

    try:
        scheduler.shutdown()
        logger.info("✅ 스케줄러 종료 완료")
    except Exception as e:
        logger.error(f"스케줄러 종료 실패: {e}")


@app.get("/")
def root():
    """
    루트 엔드포인트

    Returns:
        dict: 서비스 정보
    """
    return {
        "service": "Cobee Roommate Recommendation API",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "health": "/recommendations/health",
            "recommend": "/recommendations/recommend",
            "config": "/recommendations/config",
            "stats": "/recommendations/stats",
            "docs": "/docs",
        },
    }


@app.get("/scheduler/status")
def scheduler_status():
    """
    스케줄러 상태 확인

    Returns:
        dict: 스케줄러 정보
    """
    try:
        jobs = []
        for job in scheduler.get_jobs():
            jobs.append(
                {
                    "id": job.id,
                    "name": job.name,
                    "next_run_time": (
                        job.next_run_time.strftime("%Y-%m-%d %H:%M:%S")
                        if job.next_run_time
                        else None
                    ),
                    "trigger": str(job.trigger),
                }
            )

        return {"scheduler_running": scheduler.running, "jobs": jobs}

    except Exception as e:
        logger.error(f"스케줄러 상태 조회 실패: {e}")
        return {"scheduler_running": False, "error": str(e)}


@app.post("/scheduler/run-now")
def run_phase_update_now():
    """
    Phase 업데이트 즉시 실행 (수동 트리거)

    Returns:
        dict: 실행 결과
    """
    try:
        logger.info("수동 트리거: Phase 업데이트 즉시 실행")
        run_phase_update()

        return {
            "status": "success",
            "message": "Phase 업데이트가 실행되었습니다",
            "timestamp": datetime.now().isoformat(),
        }

    except Exception as e:
        logger.error(f"수동 실행 실패: {e}")
        return {
            "status": "error",
            "message": str(e),
            "timestamp": datetime.now().isoformat(),
        }


@app.post("/scheduler/run-sync-now")
async def run_data_sync_now():
    """
    데이터 동기화 즉시 실행 (수동 트리거)

    Returns:
        dict: 실행 결과
    """
    try:
        logger.info("수동 트리거: 데이터 동기화 즉시 실행")
        await run_data_sync()

        return {
            "status": "success",
            "message": "데이터 동기화가 실행되었습니다. 서버 로그를 확인하세요.",
            "timestamp": datetime.now().isoformat(),
        }
    except Exception as e:
        logger.error(f"데이터 동기화 수동 실행 실패: {e}", exc_info=True)
        return {
            "status": "error",
            "message": str(e),
            "timestamp": datetime.now().isoformat(),
        }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "src.api.main:app", host="0.0.0.0", port=8000, reload=True, log_level="info"
    )
