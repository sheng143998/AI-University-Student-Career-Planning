-- =============================================================================
-- Roadmap 模块数据库表
-- 基于接口文档_8_Roadmap.md 设计
-- =============================================================================

-- =============================================================================
-- 1. job_vertical_paths 表（岗位垂直晋升路径表）
-- =============================================================================
CREATE TABLE IF NOT EXISTS ai_career_plan.job_vertical_paths (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    job_name VARCHAR(255) NOT NULL,
    target_job_id BIGINT,
    target_job_name VARCHAR(255),
    path_steps JSONB DEFAULT '[]',
    total_steps INTEGER DEFAULT 0,
    estimated_total_months INTEGER,
    confidence_score DECIMAL(5,4),
    path_type VARCHAR(50) DEFAULT 'technical',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_career_plan.job_vertical_paths IS '岗位垂直晋升路径表（AI生成）';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.id IS '主键';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.job_id IS '起始岗位ID（关联job.id）';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.job_name IS '起始岗位名称';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.target_job_id IS '目标岗位ID';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.target_job_name IS '目标岗位名称';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.path_steps IS '晋升路径步骤（JSON数组）';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.total_steps IS '总步骤数';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.estimated_total_months IS '预计总时间（月）';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.confidence_score IS 'AI生成置信度(0-1)';
COMMENT ON COLUMN ai_career_plan.job_vertical_paths.path_type IS '路径类型：technical/management/hybrid';

-- =============================================================================
-- 2. job_transition_paths 表（岗位换岗路径表）
-- =============================================================================
CREATE TABLE IF NOT EXISTS ai_career_plan.job_transition_paths (
    id BIGSERIAL PRIMARY KEY,
    from_job_id BIGINT NOT NULL,
    from_job_name VARCHAR(255) NOT NULL,
    to_job_id BIGINT NOT NULL,
    to_job_name VARCHAR(255) NOT NULL,
    transition_difficulty INTEGER DEFAULT 3,
    avg_transition_time_months INTEGER,
    required_skills_gap JSONB DEFAULT '[]',
    similarity_score DECIMAL(5,4),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_career_plan.job_transition_paths IS '岗位换岗路径表（AI生成）';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.id IS '主键';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.from_job_id IS '起始岗位ID（关联job.id）';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.from_job_name IS '起始岗位名称';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.to_job_id IS '目标岗位ID';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.to_job_name IS '目标岗位名称';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.transition_difficulty IS '转换难度(1-5)';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.avg_transition_time_months IS '平均转换时间（月）';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.required_skills_gap IS '需要补充的技能差距';
COMMENT ON COLUMN ai_career_plan.job_transition_paths.similarity_score IS '岗位相似度(0-1)';

-- =============================================================================
-- 3. user_vertical_path_recommendations 表（用户垂直晋升路径推荐表）
-- =============================================================================
CREATE TABLE IF NOT EXISTS ai_career_plan.user_vertical_path_recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    current_job_name VARCHAR(255),
    matched_job_id BIGINT,
    matched_job_name VARCHAR(255),
    recommended_path_id BIGINT,
    path_steps JSONB DEFAULT '[]',
    current_step_index INTEGER DEFAULT 0,
    total_steps INTEGER DEFAULT 0,
    estimated_total_months INTEGER,
    match_score DECIMAL(5,4),
    status VARCHAR(20) DEFAULT 'active',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_career_plan.user_vertical_path_recommendations IS '用户垂直晋升路径推荐表';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.id IS '主键';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.user_id IS '用户ID';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.current_job_name IS '用户当前岗位名称（从简历提取）';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.matched_job_id IS '匹配的岗位ID（RAG检索结果）';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.matched_job_name IS '匹配的岗位名称';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.recommended_path_id IS '推荐的晋升路径ID（关联job_vertical_paths.id）';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.path_steps IS '晋升路径步骤';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.current_step_index IS '当前所在阶段索引（从0开始）';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.total_steps IS '总步骤数';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.estimated_total_months IS '预计总时间（月）';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.match_score IS '岗位匹配度(0-1)';
COMMENT ON COLUMN ai_career_plan.user_vertical_path_recommendations.status IS '路径状态：active/completed/abandoned';

-- =============================================================================
-- 4. user_transition_path_recommendations 表（用户换岗路径推荐表）
-- =============================================================================
CREATE TABLE IF NOT EXISTS ai_career_plan.user_transition_path_recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    current_job_name VARCHAR(255),
    matched_job_id BIGINT,
    recommended_path_id BIGINT,
    to_job_id BIGINT,
    to_job_name VARCHAR(255),
    transition_difficulty INTEGER,
    required_skills_gap JSONB DEFAULT '[]',
    match_score DECIMAL(5,4),
    status VARCHAR(20) DEFAULT 'active',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_career_plan.user_transition_path_recommendations IS '用户换岗路径推荐表';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.id IS '主键';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.user_id IS '用户ID';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.current_job_name IS '用户当前岗位名称';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.matched_job_id IS '匹配的岗位ID';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.recommended_path_id IS '推荐的换岗路径ID（关联job_transition_paths.id）';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.to_job_id IS '目标岗位ID';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.to_job_name IS '目标岗位名称';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.transition_difficulty IS '转换难度(1-5)';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.required_skills_gap IS '需要补充的技能差距';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.match_score IS '技能匹配度(0-1)';
COMMENT ON COLUMN ai_career_plan.user_transition_path_recommendations.status IS '状态：active/completed/abandoned';

-- =============================================================================
-- 索引
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_job_vertical_paths_job_id ON ai_career_plan.job_vertical_paths(job_id);
CREATE INDEX IF NOT EXISTS idx_job_vertical_paths_path_type ON ai_career_plan.job_vertical_paths(path_type);

CREATE INDEX IF NOT EXISTS idx_job_transition_paths_from_job_id ON ai_career_plan.job_transition_paths(from_job_id);
CREATE INDEX IF NOT EXISTS idx_job_transition_paths_to_job_id ON ai_career_plan.job_transition_paths(to_job_id);

CREATE INDEX IF NOT EXISTS idx_user_vertical_path_user_id ON ai_career_plan.user_vertical_path_recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_user_vertical_path_status ON ai_career_plan.user_vertical_path_recommendations(status);

CREATE INDEX IF NOT EXISTS idx_user_transition_path_user_id ON ai_career_plan.user_transition_path_recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_user_transition_path_status ON ai_career_plan.user_transition_path_recommendations(status);
