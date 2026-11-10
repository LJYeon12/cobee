"""
더미 데이터 생성 스크립트

Faker 라이브러리를 사용하여 현실적인 더미 데이터를 생성합니다.

생성 데이터:
- 회원: 30명
- 게시글: 60개
- 북마크: 80개
- 지원 기록: 70개
- 총 상호작용: 150개

실행 방법:
    python scripts/generate_dummy_data.py

    # 기존 데이터 유지하고 추가만
    python scripts/generate_dummy_data.py --keep-existing

    # 기존 데이터 삭제하고 새로 생성
    python scripts/generate_dummy_data.py --clear
"""

import os
import sys
import random
import argparse
from datetime import datetime, timedelta
from faker import Faker
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

# 프로젝트 루트를 Python 경로에 추가
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.utils.logger import get_logger
from src.utils.database import engine, SessionLocal
from sqlalchemy import text
from src.models.orm_models import (
    MemberInformationORM,
    RecruitPostORM,
    BookmarkORM,
    ApplyRecordORM,
)

logger = get_logger(__name__)

# Faker 초기화 (한국어)
fake = Faker("ko_KR")
Faker.seed(42)
random.seed(42)


class DummyDataGenerator:
    """
    더미 데이터 생성기
    """

    # 상수 정의
    GENDERS = ["MAN", "FEMALE"]
    LIFESTYLES = ["MORNING", "EVENING"]
    PERSONALITIES = ["INTROVERT", "EXTROVERT"]
    MATCH_STATUSES = ["ON_WAIT", "MATCHING", "REJECTED", "MATCHED"]
    RECRUIT_STATUSES = ["RECRUITING", "ON_CONTACT", "RECRUIT_OVER"]

    # 서울 주요 지역 좌표 (위도, 경도)
    SEOUL_LOCATIONS = [
        ("서울시 강남구 역삼동", 37.5013, 127.0360),
        ("서울시 서초구 서초동", 37.4836, 127.0327),
        ("서울시 마포구 상암동", 37.5794, 126.8895),
        ("서울시 송파구 잠실동", 37.5133, 127.1000),
        ("서울시 강남구 삼성동", 37.5087, 127.0632),
        ("서울시 강북구 수유동", 37.6387, 127.0253),
        ("서울시 용산구 이태원동", 37.5345, 126.9945),
        ("서울시 성북구 성북동", 37.5894, 127.0167),
        ("서울시 동작구 사당동", 37.4765, 126.9812),
        ("서울시 관악구 신림동", 37.4844, 126.9295),
        ("서울시 종로구 종로1가", 37.5703, 126.9766),
        ("서울시 중구 명동", 37.5636, 126.9850),
        ("서울시 영등포구 여의도동", 37.5219, 126.9245),
        ("서울시 광진구 건대입구", 37.5403, 127.0695),
        ("서울시 노원구 상계동", 37.6542, 127.0648),
    ]

    def __init__(self):
        """
        기존 database.py의 engine과 SessionLocal 사용
        """
        self.engine = engine
        self.session = SessionLocal()

        logger.info(f"DB 연결: {self.engine.url}")

    def clear_existing_data(self):
        """
        기존 데이터 삭제 (외래키 순서 고려)
        """
        logger.warning("기존 데이터 삭제 중...")

        with self.engine.connect() as conn:
            # 외래키 순서: 자식 → 부모
            conn.execute(text("DELETE FROM comment"))
            conn.execute(text("DELETE FROM bookmark"))
            conn.execute(text("DELETE FROM apply_record"))
            conn.execute(text("DELETE FROM recruit_post"))
            conn.execute(text("DELETE FROM member_information"))
            conn.commit()

        logger.info("기존 데이터 삭제 완료")

    def generate_birthdate(self):
        """
        만 19~34세 생년월일 생성

        Returns:
            date: 생년월일
        """
        today = datetime.now().date()
        # 만 19세 = 오늘부터 19년 전
        # 만 34세 = 오늘부터 34년 전
        min_date = today - timedelta(days=365 * 34)
        max_date = today - timedelta(days=365 * 19)

        return fake.date_between(start_date=min_date, end_date=max_date)

    def generate_members(self, count=30):
        """
        회원 데이터 생성

        Args:
            count: 생성할 회원 수

        Returns:
            list: 생성된 member_id 리스트
        """
        logger.info(f"회원 {count}명 생성 중...")

        member_ids = []

        for i in range(1, count + 1):
            gender = random.choice(self.GENDERS)
            birth_date = self.generate_birthdate()

            # 선호도 (20% 확률로 None)
            preferred_gender = random.choice(self.GENDERS + ["NONE"])
            preferred_life_style = (
                random.choice(self.LIFESTYLES) if random.random() > 0.2 else None
            )
            preferred_personality = (
                random.choice(self.PERSONALITIES) if random.random() > 0.2 else None
            )

            # 나이 범위 (선호 최소/최대)
            age_range_start = random.randint(19, 28)
            age_range_end = age_range_start + random.randint(3, 8)
            preferred_age_min = age_range_start
            preferred_age_max = min(age_range_end, 34)

            # 본인 특성
            my_lifestyle = random.choice(self.LIFESTYLES)
            my_personality = random.choice(self.PERSONALITIES)

            # 습관 (30% 확률로 True)
            is_smoking = random.random() < 0.15  # 15% 흡연자
            is_snoring = random.random() < 0.20  # 20% 코골이
            has_pet = random.random() < 0.25  # 25% 반려동물

            # 수용도 (본인이 해당 습관 있으면 수용 가능성 높음)
            possible_smoking = is_smoking or random.random() < 0.3
            possible_snoring = is_snoring or random.random() < 0.4
            has_pet_allowed = has_pet or random.random() < 0.5

            cohabitant_count = random.randint(1, 4)

            member = MemberInformationORM(
                member_id=i,
                gender=gender,
                birth_date=birth_date,
                preferred_gender=preferred_gender,
                preferred_life_style=preferred_life_style,
                preferred_personality=preferred_personality,
                possible_smoking=possible_smoking,
                possible_snoring=possible_snoring,
                has_pet_allowed=has_pet_allowed,
                cohabitant_count=cohabitant_count,
                preferred_age_min=preferred_age_min,
                preferred_age_max=preferred_age_max,
                my_lifestyle=my_lifestyle,
                my_personality=my_personality,
                is_smoking=is_smoking,
                is_snoring=is_snoring,
                has_pet=has_pet,
                created_at=datetime.now(),
            )

            self.session.add(member)
            member_ids.append(i)

        self.session.commit()
        logger.info(f"회원 {count}명 생성 완료")

        return member_ids

    def generate_recruit_posts(self, member_ids, count=60):
        """
        모집 게시글 생성

        Args:
            member_ids: 회원 ID 리스트
            count: 생성할 게시글 수

        Returns:
            list: 생성된 recruit_post_id 리스트
        """
        logger.info(f"게시글 {count}개 생성 중...")

        post_ids = []

        # 각 회원이 1~3개씩 작성
        posts_per_member = [random.randint(1, 3) for _ in member_ids]

        post_id = 1
        for member_id, num_posts in zip(member_ids, posts_per_member):
            if post_id > count:
                break

            for _ in range(num_posts):
                if post_id > count:
                    break

                # 비용
                rent_min = random.randint(20, 50) * 10000
                rent_max = rent_min + random.randint(10, 30) * 10000
                monthly_min = random.randint(8, 15) * 10000
                monthly_max = monthly_min + random.randint(3, 8) * 10000

                # 선호 조건
                preferred_gender = random.choice(self.GENDERS + ["NONE"])
                preferred_life_style = (
                    random.choice(self.LIFESTYLES) if random.random() > 0.3 else None
                )
                preferred_personality = (
                    random.choice(self.PERSONALITIES) if random.random() > 0.3 else None
                )

                # 나이 범위
                age_start = random.randint(19, 28)
                age_end = age_start + random.randint(4, 10)
                preferred_age_min = age_start
                preferred_age_max = min(age_end, 34)

                # 습관
                is_smoking = random.random() < 0.1
                is_snoring = random.random() < 0.15
                is_pet_allowed = random.random() < 0.3

                cohabitant_count = random.randint(1, 4)
                recruit_count = random.randint(1, 3)
                has_room = random.random() < 0.7  # 70% 방 있음

                # 위치
                address, lat, lon = random.choice(self.SEOUL_LOCATIONS)

                # 모집 상태 (90% RECRUITING, 10% 기타)
                if random.random() < 0.9:
                    recruit_status = "RECRUITING"
                else:
                    recruit_status = random.choice(["ON_CONTACT", "RECRUIT_OVER"])

                # 생성 시간 (최근 3개월 이내)
                created_at = datetime.now() - timedelta(days=random.randint(0, 90))

                post = RecruitPostORM(
                    recruit_post_id=post_id,
                    recruit_count=recruit_count,
                    rent_cost_min=rent_min,
                    rent_cost_max=rent_max,
                    monthly_cost_min=monthly_min,
                    monthly_cost_max=monthly_max,
                    preferred_gender=preferred_gender,
                    preferred_life_style=preferred_life_style,
                    preferred_personality=preferred_personality,
                    is_smoking=is_smoking,
                    is_snoring=is_snoring,
                    is_pet_allowed=is_pet_allowed,
                    cohabitant_count=cohabitant_count,
                    preferred_age_min=preferred_age_min,
                    preferred_age_max=preferred_age_max,
                    has_room=has_room,
                    address=address,
                    region_latitude=lat,
                    region_longitude=lon,
                    recruit_status=recruit_status,
                    member_id=member_id,
                    created_at=created_at,
                )

                self.session.add(post)
                post_ids.append(post_id)
                post_id += 1

        self.session.commit()
        logger.info(f"게시글 {len(post_ids)}개 생성 완료")

        return post_ids

    def generate_interactions(
        self, member_ids, post_ids, bookmark_count=80, apply_count=70
    ):
        """
        상호작용 데이터 생성 (현실적인 분포)

        Args:
            member_ids: 회원 ID 리스트
            post_ids: 게시글 ID 리스트
            bookmark_count: 북마크 개수
            apply_count: 지원 기록 개수
        """
        logger.info(
            f"상호작용 생성 중 (북마크: {bookmark_count}, 지원: {apply_count})..."
        )

        # 파레토 분포: 20%의 게시글이 60%의 상호작용 받음
        # 인기 게시글 선정
        popular_posts = random.sample(post_ids, k=max(1, len(post_ids) // 5))

        # 활발한 사용자 선정
        active_users = random.sample(member_ids, k=max(1, len(member_ids) // 5))

        # 북마크 생성
        bookmarks_created = set()
        bookmark_id = 1

        for _ in range(bookmark_count):
            # 60% 확률로 활발한 사용자
            if random.random() < 0.6 and active_users:
                user_id = random.choice(active_users)
            else:
                user_id = random.choice(member_ids)

            # 60% 확률로 인기 게시글
            if random.random() < 0.6 and popular_posts:
                post_id = random.choice(popular_posts)
            else:
                post_id = random.choice(post_ids)

            # 중복 방지 (같은 사용자가 같은 게시글에 북마크 여러 번 불가)
            if (user_id, post_id) in bookmarks_created:
                continue

            # 본인이 작성한 게시글은 북마크 안 함
            post = (
                self.session.query(RecruitPostORM)
                .filter_by(recruit_post_id=post_id)
                .first()
            )
            if post and post.member_id == user_id:
                continue

            created_at = datetime.now() - timedelta(days=random.randint(0, 60))

            bookmark = BookmarkORM(
                bookmark_id=bookmark_id,
                member_id=user_id,
                recruit_post_id=post_id,
                created_at=created_at,
            )

            self.session.add(bookmark)
            bookmarks_created.add((user_id, post_id))
            bookmark_id += 1

        self.session.commit()
        logger.info(f"북마크 {len(bookmarks_created)}개 생성 완료")

        # 지원 기록 생성
        applies_created = set()
        record_id = 1

        # match_status 분포: ON_WAIT 50%, MATCHING 25%, REJECTED 15%, MATCHED 10%
        status_distribution = (
            ["ON_WAIT"] * 50 + ["MATCHING"] * 25 + ["REJECTED"] * 15 + ["MATCHED"] * 10
        )

        for _ in range(apply_count):
            # 60% 확률로 활발한 사용자
            if random.random() < 0.6 and active_users:
                user_id = random.choice(active_users)
            else:
                user_id = random.choice(member_ids)

            # 60% 확률로 인기 게시글
            if random.random() < 0.6 and popular_posts:
                post_id = random.choice(popular_posts)
            else:
                post_id = random.choice(post_ids)

            # 중복 방지
            if (user_id, post_id) in applies_created:
                continue

            # 본인이 작성한 게시글은 지원 안 함
            post = (
                self.session.query(RecruitPostORM)
                .filter_by(recruit_post_id=post_id)
                .first()
            )
            if post and post.member_id == user_id:
                continue

            match_status = random.choice(status_distribution)
            submitted_at = datetime.now() - timedelta(days=random.randint(0, 60))
            created_at = submitted_at

            apply_record = ApplyRecordORM(
                record_id=record_id,
                match_status=match_status,
                submitted_at=submitted_at,
                member_id=user_id,
                recruit_post_id=post_id,
                created_at=created_at,
            )

            self.session.add(apply_record)
            applies_created.add((user_id, post_id))
            record_id += 1

        self.session.commit()
        logger.info(f"지원 기록 {len(applies_created)}개 생성 완료")

        total_interactions = len(bookmarks_created) + len(applies_created)
        logger.info(f"총 상호작용: {total_interactions}개")

    def generate_all(
        self, member_count=30, post_count=60, bookmark_count=80, apply_count=70
    ):
        """
        모든 더미 데이터 생성

        Args:
            member_count: 회원 수
            post_count: 게시글 수
            bookmark_count: 북마크 수
            apply_count: 지원 기록 수
        """
        logger.info("=" * 60)
        logger.info("더미 데이터 생성 시작")
        logger.info("=" * 60)

        try:
            # 1. 회원 생성
            member_ids = self.generate_members(member_count)

            # 2. 게시글 생성
            post_ids = self.generate_recruit_posts(member_ids, post_count)

            # 3. 상호작용 생성
            self.generate_interactions(
                member_ids, post_ids, bookmark_count, apply_count
            )

            logger.info("=" * 60)
            logger.info("더미 데이터 생성 완료")
            logger.info("=" * 60)
            logger.info(f"  - 회원: {len(member_ids)}명")
            logger.info(f"  - 게시글: {len(post_ids)}개")
            logger.info(f"  - 상호작용: {bookmark_count + apply_count}개 목표")
            logger.info("=" * 60)

        except Exception as e:
            logger.error(f"더미 데이터 생성 실패: {e}", exc_info=True)
            self.session.rollback()
            raise
        finally:
            self.session.close()


def main():
    """메인 실행 함수"""
    parser = argparse.ArgumentParser(description="더미 데이터 생성")
    parser.add_argument("--clear", action="store_true", help="기존 데이터 삭제 후 생성")
    parser.add_argument(
        "--keep-existing", action="store_true", help="기존 데이터 유지하고 추가"
    )
    parser.add_argument("--members", type=int, default=30, help="회원 수 (기본: 30)")
    parser.add_argument("--posts", type=int, default=60, help="게시글 수 (기본: 60)")
    parser.add_argument(
        "--bookmarks", type=int, default=80, help="북마크 수 (기본: 80)"
    )
    parser.add_argument(
        "--applies", type=int, default=70, help="지원 기록 수 (기본: 70)"
    )

    args = parser.parse_args()

    try:

        # 생성기 초기화
        generator = DummyDataGenerator()

        # 기존 데이터 처리
        if args.clear:
            generator.clear_existing_data()
        elif not args.keep_existing:
            # 기본: 사용자에게 확인
            response = input("기존 데이터를 삭제하시겠습니까? (y/N): ")
            if response.lower() == "y":
                generator.clear_existing_data()

        # 더미 데이터 생성
        generator.generate_all(
            member_count=args.members,
            post_count=args.posts,
            bookmark_count=args.bookmarks,
            apply_count=args.applies,
        )

        logger.info("다음 단계: python scripts/feature_engineering.py")

        sys.exit(0)  # 성공

    except Exception as e:
        logger.error(f"실행 실패: {e}", exc_info=True)
        sys.exit(1)  # 실패


if __name__ == "__main__":
    main()
