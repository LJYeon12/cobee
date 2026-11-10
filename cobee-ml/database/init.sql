-- ========================================
-- 1. member_information 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS member_information (
    member_id INTEGER PRIMARY KEY,
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('MAN', 'FEMALE')),
    birth_date DATE NOT NULL,
    
    -- 선호도 (Preferred)
    preferred_gender VARCHAR(10) CHECK (preferred_gender IN ('MAN', 'FEMALE', 'NONE')),
    preferred_life_style VARCHAR(10) CHECK (preferred_life_style IN ('MORNING', 'EVENING')),
    preferred_personality VARCHAR(10) CHECK (preferred_personality IN ('INTROVERT', 'EXTROVERT')),
    possible_smoking BOOLEAN DEFAULT FALSE,
    possible_snoring BOOLEAN DEFAULT FALSE,
    has_pet_allowed BOOLEAN DEFAULT FALSE,
    cohabitant_count INTEGER,
    preferred_age_min INTEGER CHECK (preferred_age_min >= 19 AND preferred_age_min <= 34),
    preferred_age_max INTEGER CHECK (preferred_age_max >= 19 AND preferred_age_max <= 34),
    
    -- 본인 특성 (my)
    my_lifestyle VARCHAR(10) CHECK (my_lifestyle IN ('MORNING', 'EVENING')),
    my_personality VARCHAR(10) CHECK (my_personality IN ('INTROVERT', 'EXTROVERT')),
    is_smoking BOOLEAN DEFAULT FALSE,
    is_snoring BOOLEAN DEFAULT FALSE,
    has_pet BOOLEAN DEFAULT FALSE,
    
    -- 메타 정보
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- member_id 인덱스 (자동 생성되지만 명시적으로 표시)
CREATE INDEX IF NOT EXISTS idx_member_id ON member_information(member_id);

-- ========================================
-- 2. recruit_post 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS recruit_post (
    recruit_post_id INTEGER PRIMARY KEY,
    recruit_count INTEGER NOT NULL DEFAULT 1,
    
    -- 비용 정보
    rent_cost_min INTEGER,
    rent_cost_max INTEGER,
    monthly_cost_min INTEGER,
    monthly_cost_max INTEGER,
    
    -- 선호 조건
    preferred_gender VARCHAR(10) CHECK (preferred_gender IN ('MAN', 'FEMALE', 'NONE')),
    preferred_life_style VARCHAR(10) CHECK (preferred_life_style IN ('MORNING', 'EVENING')),
    preferred_personality VARCHAR(10) CHECK (preferred_personality IN ('INTROVERT', 'EXTROVERT')),
    is_smoking BOOLEAN DEFAULT FALSE,
    is_snoring BOOLEAN DEFAULT FALSE,
    is_pet_allowed BOOLEAN DEFAULT FALSE,
    cohabitant_count INTEGER,
    preferred_age_min INTEGER CHECK (preferred_age_min >= 19 AND preferred_age_min <= 34),
    preferred_age_max INTEGER CHECK (preferred_age_max >= 19 AND preferred_age_max <= 34),
    
    -- 방 정보
    has_room BOOLEAN DEFAULT FALSE,
    address TEXT,
    region_latitude DOUBLE PRECISION,
    region_longitude DOUBLE PRECISION,
    
    -- 상태
    recruit_status VARCHAR(20) NOT NULL CHECK (recruit_status IN ('RECRUITING', 'ON_CONTACT', 'RECRUIT_OVER')),
    
    -- 외래키
    member_id INTEGER NOT NULL,
    
    -- 타임스탬프
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- 외래키 제약조건
    CONSTRAINT fk_recruit_post_member FOREIGN KEY (member_id) REFERENCES member_information(member_id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_recruit_post_member ON recruit_post(member_id);
CREATE INDEX IF NOT EXISTS idx_recruit_post_status ON recruit_post(recruit_status);
CREATE INDEX IF NOT EXISTS idx_recruit_post_created ON recruit_post(created_at DESC);

-- ========================================
-- 3. apply_record 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS apply_record (
    record_id INTEGER PRIMARY KEY,
    match_status VARCHAR(20) NOT NULL CHECK (match_status IN ('ON_WAIT', 'MATCHING', 'REJECTED', 'MATCHED')),
    submitted_at TIMESTAMP NOT NULL,
    
    -- 외래키
    member_id INTEGER NOT NULL,
    recruit_post_id INTEGER NOT NULL,
    
    -- 타임스탬프
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- 외래키 제약조건
    CONSTRAINT fk_apply_record_member FOREIGN KEY (member_id) REFERENCES member_information(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_apply_record_post FOREIGN KEY (recruit_post_id) REFERENCES recruit_post(recruit_post_id) ON DELETE CASCADE,
    
    -- 중복 지원 방지 (한 사용자가 같은 게시글에 여러 번 지원 불가)
    CONSTRAINT uk_apply_record_member_post UNIQUE (member_id, recruit_post_id)
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_apply_record_member ON apply_record(member_id);
CREATE INDEX IF NOT EXISTS idx_apply_record_post ON apply_record(recruit_post_id);
CREATE INDEX IF NOT EXISTS idx_apply_record_status ON apply_record(match_status);
CREATE INDEX IF NOT EXISTS idx_apply_record_submitted ON apply_record(submitted_at DESC);

-- ========================================
-- 4. bookmark 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS bookmark (
    bookmark_id INTEGER PRIMARY KEY,
    
    -- 외래키
    member_id INTEGER NOT NULL,
    recruit_post_id INTEGER NOT NULL,
    
    -- 타임스탬프
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- 외래키 제약조건
    CONSTRAINT fk_bookmark_member FOREIGN KEY (member_id) REFERENCES member_information(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_post FOREIGN KEY (recruit_post_id) REFERENCES recruit_post(recruit_post_id) ON DELETE CASCADE,
    
    -- 중복 북마크 방지
    CONSTRAINT uk_bookmark_member_post UNIQUE (member_id, recruit_post_id)
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_bookmark_member ON bookmark(member_id);
CREATE INDEX IF NOT EXISTS idx_bookmark_post ON bookmark(recruit_post_id);
CREATE INDEX IF NOT EXISTS idx_bookmark_created ON bookmark(created_at DESC);

-- ========================================
-- 5. comment 테이블
-- ========================================
CREATE TABLE IF NOT EXISTS comment (
    comment_id INTEGER PRIMARY KEY,
    
    -- 외래키
    member_id INTEGER NOT NULL,
    recruit_post_id INTEGER NOT NULL,
    
    -- 타임스탬프
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- 외래키 제약조건
    CONSTRAINT fk_comment_member FOREIGN KEY (member_id) REFERENCES member_information(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_post FOREIGN KEY (recruit_post_id) REFERENCES recruit_post(recruit_post_id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_comment_member ON comment(member_id);
CREATE INDEX IF NOT EXISTS idx_comment_post ON comment(recruit_post_id);
CREATE INDEX IF NOT EXISTS idx_comment_created ON comment(created_at DESC);

-- ========================================
-- 뷰 생성 (선택사항)
-- ========================================

-- 추천에 필요한 통계 정보를 빠르게 조회하기 위한 뷰
CREATE OR REPLACE VIEW v_interaction_summary AS
SELECT 
    COUNT(DISTINCT ar.record_id) + COUNT(DISTINCT b.bookmark_id) AS total_interactions,
    COUNT(DISTINCT ar.record_id) AS apply_count,
    COUNT(DISTINCT b.bookmark_id) AS bookmark_count,
    COUNT(DISTINCT c.comment_id) AS comment_count
FROM member_information m
LEFT JOIN apply_record ar ON m.member_id = ar.member_id
LEFT JOIN bookmark b ON m.member_id = b.member_id
LEFT JOIN comment c ON m.member_id = c.member_id;

-- 모집 중인 게시글만 조회하는 뷰
CREATE OR REPLACE VIEW v_recruiting_posts AS
SELECT 
    rp.*,
    m.gender AS author_gender,
    m.my_lifestyle AS author_lifestyle,
    m.my_personality AS author_personality,
    m.is_smoking AS author_is_smoking,
    m.is_snoring AS author_is_snoring,
    m.has_pet AS author_has_pet,
    EXTRACT(YEAR FROM AGE(CURRENT_DATE, m.birth_date)) AS author_age
FROM recruit_post rp
JOIN member_information m ON rp.member_id = m.member_id
WHERE rp.recruit_status = 'RECRUITING';

-- ========================================
-- 완료 메시지
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '===========================================';
    RAISE NOTICE '데이터베이스 초기화 완료';
    RAISE NOTICE '생성된 테이블:';
    RAISE NOTICE '  - member_information';
    RAISE NOTICE '  - recruit_post';
    RAISE NOTICE '  - apply_record';
    RAISE NOTICE '  - bookmark';
    RAISE NOTICE '  - comment';
    RAISE NOTICE '생성된 뷰:';
    RAISE NOTICE '  - v_interaction_summary';
    RAISE NOTICE '  - v_recruiting_posts';
    RAISE NOTICE '===========================================';
END $$;