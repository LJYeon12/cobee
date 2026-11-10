"""
Phase 자동 업데이트 (단순화 버전)

상호작용 수를 집계하고, 임계값에 따라 Phase를 자동 전환합니다.

실행 방법:
    python scripts/update_phase.py

cron 설정 (매일 새벽 4:50):
    50 4 * * * cd /path/to/cobee-msa-ml && python scripts/update_phase.py
"""

import os
import sys
import json
from datetime import datetime
from sqlalchemy import text

# 프로젝트 루트를 Python 경로에 추가
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.recommender.phase import select_phase
from src.utils.logger import get_logger
from src.utils.config_loader import ConfigLoader
from src.utils.database import SessionLocal

logger = get_logger(__name__)


class PhaseUpdater:
    """
    Phase 자동 업데이트 시스템 (상호작용 수 기반)
    """

    def __init__(self):
        """
        초기화
        """
        self.config = ConfigLoader()
        self.config.load_config()

        self.db = SessionLocal()

        logger.info("PhaseUpdater 초기화 완료")

    def count_interactions(self) -> int:
        """
        현재 상호작용 총합 계산

        상호작용 = ApplyRecord + Bookmark

        Returns:
            int: 상호작용 총합
        """
        try:
            # ApplyRecord 수
            apply_count = self.db.execute(
                text("SELECT COUNT(*) FROM apply_record")
            ).scalar()

            # Bookmark 수
            bookmark_count = self.db.execute(
                text("SELECT COUNT(*) FROM bookmark")
            ).scalar()

            total = apply_count + bookmark_count

            logger.info(
                f"상호작용 집계: ApplyRecord={apply_count}, Bookmark={bookmark_count}, 총합={total}"
            )

            return total

        except Exception as e:
            logger.error(f"상호작용 집계 실패: {e}")
            return 0

    def get_phase_by_interaction_count(self, count: int) -> str:
        """
        상호작용 수 기반 Phase 결정

        Args:
            count: 상호작용 수

        Returns:
            str: Phase ("P1", "P2", "P3")
        """
        thresholds = self.config.get("phase.thresholds", {})

        return select_phase(count, thresholds)

    def update_phase_in_config(self, new_phase: str, interaction_count: int):
        """
        config.json의 Phase 업데이트

        Args:
            new_phase: 새로운 Phase
            interaction_count: 현재 상호작용 수
        """
        old_phase = self.config.get_current_phase()

        self.config._config["phase"]["current"] = new_phase
        self.config._config["phase"]["interaction_count"] = interaction_count
        self.config._config["last_updated"] = datetime.now().isoformat()

        # 파일에 저장
        config_path = self.config._config_path
        with open(config_path, "w", encoding="utf-8") as f:
            json.dump(self.config._config, f, indent=2, ensure_ascii=False)

        if old_phase != new_phase:
            logger.info(
                f"✅ Phase 전환: {old_phase} → {new_phase} (상호작용: {interaction_count})"
            )
        else:
            logger.info(f"✅ Phase 유지: {new_phase} (상호작용: {interaction_count})")

    def run(self):
        """
        Phase 업데이트 메인 로직 (단순화 버전)
        """
        try:
            logger.info("=" * 60)
            logger.info("Phase 업데이트 시작 (상호작용 수 기반)")
            logger.info("=" * 60)

            # 1. 상호작용 수 집계
            current_interaction_count = self.count_interactions()
            current_phase = self.config.get_current_phase()

            logger.info(f"현재 Phase: {current_phase}")
            logger.info(f"상호작용 수: {current_interaction_count}")

            # 2. 상호작용 수로 Phase 결정
            new_phase = self.get_phase_by_interaction_count(current_interaction_count)

            # 임계값 정보 출력
            thresholds = self.config.get("phase.thresholds", {})
            p2_min = thresholds.get("P2", {}).get("min", 100)
            p3_min = thresholds.get("P3", {}).get("min", 1000)

            logger.info(f"Phase 임계값:")
            logger.info(f"  - P1: 0 ~ {p2_min-1}")
            logger.info(f"  - P2: {p2_min} ~ {p3_min-1}")
            logger.info(f"  - P3: {p3_min} ~")

            if new_phase != current_phase:
                logger.info(f"📊 Phase 전환 필요: {current_phase} → {new_phase}")
            else:
                logger.info(f"📊 Phase 유지: {current_phase}")

            # 3. config.json 업데이트
            self.update_phase_in_config(new_phase, current_interaction_count)

            logger.info("=" * 60)
            logger.info(f"Phase 업데이트 완료: {new_phase}")
            logger.info(f"상호작용 총합: {current_interaction_count}")

            # 가중치 정보 출력
            weights = self.config.get_weights(new_phase)
            logger.info(
                f"현재 가중치: Rule-Based={weights['rule_based']*100}%, MF={weights['matrix_factorization']*100}%"
            )
            logger.info("=" * 60)

            return new_phase

        except Exception as e:
            logger.error(f"Phase 업데이트 실패: {e}", exc_info=True)
            raise
        finally:
            self.db.close()


def main():
    """메인 실행 함수"""
    try:
        updater = PhaseUpdater()
        new_phase = updater.run()

        print(f"\n✅ Phase: {new_phase}")

        sys.exit(0)

    except Exception as e:
        logger.error(f"실행 실패: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
