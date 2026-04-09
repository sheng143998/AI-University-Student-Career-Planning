-- ===========================================================================
-- 数据库迁移脚本：AI 职业规划平台 - 完整数据库架构
-- 执行时间：2026-03-31
-- 用途：支持用户管理、简历分析、能力画像、职业规划及企业招聘数据的存储和向量检索
-- ===========================================================================

-- =============================================
-- 1. 基础设置
-- =============================================
-- 设置搜索路径
SET search_path TO ai_career_plan, public;

-- 清理旧表（按依赖关系倒序删除）
DROP TABLE IF EXISTS ai_career_plan.user_roadmap_steps CASCADE;
DROP TABLE IF EXISTS ai_career_plan.user_career_data CASCADE;
DROP TABLE IF EXISTS ai_career_plan.student_capability_profile CASCADE;
DROP TABLE IF EXISTS ai_career_plan.resume_analysis_result CASCADE;
DROP TABLE IF EXISTS ai_career_plan.user_vector_store CASCADE;
DROP TABLE IF EXISTS ai_career_plan.recruitment_data CASCADE;
DROP TABLE IF EXISTS ai_career_plan.users CASCADE;

-- 启用 vector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建 schema
CREATE SCHEMA IF NOT EXISTS ai_career_plan;

-- =============================================
-- 2. 表结构定义
-- =============================================

-- ---------------------------------------------
-- users 表（用户表）
-- ---------------------------------------------
CREATE TABLE ai_career_plan.users (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    user_password VARCHAR(500) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_image VARCHAR(500),
    sex INTEGER DEFAULT 1
);

-- ---------------------------------------------
-- user_vector_store 表（向量存储表）
-- ---------------------------------------------
CREATE TABLE ai_career_plan.user_vector_store (
    id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT,
    content TEXT,
    resume_file_path VARCHAR(500),
    vector_type VARCHAR(50) DEFAULT 'resume',  -- 'resume' | 'recruitment'
    embedding VECTOR(1024),
    metadata JSONB DEFAULT '{}',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------
-- resume_analysis_result 表（简历分析结果表）
-- ---------------------------------------------
CREATE TABLE ai_career_plan.resume_analysis_result (
    id BIGSERIAL PRIMARY KEY,
    vector_store_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    file_type VARCHAR(20) NOT NULL,             -- pdf / docx / pptx / html / txt
    original_file_name VARCHAR(500),
    resume_file_path VARCHAR(500),              -- OSS 文件路径
    status VARCHAR(50) DEFAULT 'pending',       -- pending/processing/completed/failed/stopped
    progress INTEGER DEFAULT 0,                 -- 0-100
    parsed_data JSONB DEFAULT '{}',
    scores JSONB DEFAULT '{}',
    highlights JSONB DEFAULT '{}',
    suggestions JSONB DEFAULT '[]',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------
-- student_capability_profile 表（学生就业能力画像表）
-- ---------------------------------------------
CREATE TABLE ai_career_plan.student_capability_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resume_analysis_id BIGINT NOT NULL,
    overall_score INT NOT NULL,                 -- 综合能力评分 (0-100)
    completeness_score INT NOT NULL,            -- 简历完整度评分 (0-100)
    competitiveness_score INT NOT NULL,         -- 竞争力评分 (0-100)
    capability_scores JSONB DEFAULT '{}',       -- 各维度能力得分
    professional_skills JSONB DEFAULT '[]',     -- 专业技能列表
    certificates JSONB DEFAULT '[]',            -- 证书列表
    soft_skills JSONB DEFAULT '{}',             -- 软技能评估
    ai_evaluation TEXT,                         -- AI 综合评价
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------
-- user_career_data 表（用户职业数据表 - Dashboard 模块）
-- ---------------------------------------------
CREATE TABLE ai_career_plan.user_career_data (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_profile JSONB,                          -- 岗位画像信息
    match_summary JSONB,                        -- 匹配度摘要
    market_trends JSONB,                        -- 市场趋势数据
    skill_radar JSONB,                          -- 能力雷达图数据
    actions JSONB,                              -- 行动建议列表
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------
-- user_roadmap_steps 表（用户职业发展路径表 - Dashboard/Roadmap 模块）
-- ---------------------------------------------
CREATE TABLE ai_career_plan.user_roadmap_steps (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_profile_id BIGINT,
    current_step_index INTEGER,                 -- 当前所在阶段索引
    steps JSONB,                                -- 职业发展阶段列表
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------
-- recruitment_data 表（企业招聘数据表）
-- ---------------------------------------------
CREATE TABLE ai_career_plan.recruitment_data (
    id BIGSERIAL PRIMARY KEY,
    position_name VARCHAR(255),                 -- 职位名称
    company_name VARCHAR(255),                  -- 公司全称
    industry VARCHAR(100),                      -- 所属行业
    city VARCHAR(100),                          -- 工作城市
    salary_range VARCHAR(100),                  -- 薪资范围
    company_size VARCHAR(100),                  -- 人员规模
    company_nature VARCHAR(100),                -- 企业性质
    position_code VARCHAR(100),                 -- 职位编码
    job_description TEXT,                       -- 职位描述（HTML 原文）
    job_requirements TEXT,                      -- 任职要求
    company_description TEXT,                   -- 公司简介
    source_url VARCHAR(500),                    -- 岗位来源地址
    publish_date VARCHAR(50),                   -- 更新日期
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- 3. 索引定义
-- =============================================

-- users 表索引
CREATE INDEX idx_users_user_name ON ai_career_plan.users(user_name);

-- user_vector_store 表索引
CREATE INDEX idx_user_vector_user_id ON ai_career_plan.user_vector_store(user_id);
CREATE INDEX idx_user_vector_type ON ai_career_plan.user_vector_store(vector_type);
CREATE INDEX idx_embedding ON ai_career_plan.user_vector_store USING hnsw (embedding vector_cosine_ops);

-- resume_analysis_result 表索引
CREATE INDEX idx_resume_analysis_vector_store_id ON ai_career_plan.resume_analysis_result(vector_store_id);
CREATE INDEX idx_resume_analysis_user_id ON ai_career_plan.resume_analysis_result(user_id);

-- student_capability_profile 表索引
CREATE INDEX idx_student_capability_profile_user_id ON ai_career_plan.student_capability_profile(user_id);
CREATE INDEX idx_student_capability_profile_resume_analysis_id ON ai_career_plan.student_capability_profile(resume_analysis_id);

-- user_career_data 表索引
CREATE INDEX idx_user_career_data_user_id ON ai_career_plan.user_career_data(user_id);

-- user_roadmap_steps 表索引
CREATE INDEX idx_user_roadmap_steps_user_id ON ai_career_plan.user_roadmap_steps(user_id);
CREATE INDEX idx_user_roadmap_steps_job_profile_id ON ai_career_plan.user_roadmap_steps(job_profile_id);

-- recruitment_data 表索引
CREATE INDEX idx_recruitment_position_name ON ai_career_plan.recruitment_data(position_name);
CREATE INDEX idx_recruitment_company_name ON ai_career_plan.recruitment_data(company_name);
CREATE INDEX idx_recruitment_city ON ai_career_plan.recruitment_data(city);
CREATE INDEX idx_recruitment_industry ON ai_career_plan.recruitment_data(industry);

-- =============================================
-- 4. 表注释和列注释
-- =============================================

-- user_vector_store 表注释
COMMENT ON TABLE ai_career_plan.user_vector_store IS '向量存储表（支持简历和招聘数据）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.content IS '文档内容（简历文件内容）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.resume_file_path IS '简历文件存储路径';
COMMENT ON COLUMN ai_career_plan.user_vector_store.vector_type IS '向量类型：resume | recruitment';
COMMENT ON COLUMN ai_career_plan.user_vector_store.embedding IS 'embedding 向量（1024 维）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.metadata IS '元数据（JSON 格式）';

-- resume_analysis_result 表注释
COMMENT ON TABLE ai_career_plan.resume_analysis_result IS '简历分析结果表';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.id IS '主键';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.vector_store_id IS '关联 user_vector_store.id';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.file_type IS '文件类型：pdf / docx / pptx / html / txt';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.original_file_name IS '原始文件名';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.resume_file_path IS 'OSS 文件路径';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.status IS '处理状态：pending/processing/completed/failed/stopped';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.progress IS '处理进度：0-100';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.parsed_data IS '解析后的结构化数据';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.scores IS '各维度评分';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.highlights IS '亮点列表';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.suggestions IS '优化建议';

-- student_capability_profile 表注释
COMMENT ON TABLE ai_career_plan.student_capability_profile IS '学生就业能力画像表';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.id IS '主键';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.resume_analysis_id IS '关联 resume_analysis_result.id';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.overall_score IS '综合能力评分 (0-100)';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.completeness_score IS '简历完整度评分 (0-100)';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.competitiveness_score IS '竞争力评分 (0-100)';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.capability_scores IS '各维度能力得分';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.professional_skills IS '专业技能列表';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.certificates IS '证书列表';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.soft_skills IS '软技能评估';
COMMENT ON COLUMN ai_career_plan.student_capability_profile.ai_evaluation IS 'AI 综合评价';

-- user_career_data 表注释
COMMENT ON TABLE ai_career_plan.user_career_data IS '用户职业数据表（Dashboard 模块）';
COMMENT ON COLUMN ai_career_plan.user_career_data.id IS '主键';
COMMENT ON COLUMN ai_career_plan.user_career_data.user_id IS '用户 ID（逻辑外键）';
COMMENT ON COLUMN ai_career_plan.user_career_data.job_profile IS '岗位画像信息（岗位名称、行业、城市等）';
COMMENT ON COLUMN ai_career_plan.user_career_data.match_summary IS '匹配度摘要（分数、描述、标签、各维度得分）';
COMMENT ON COLUMN ai_career_plan.user_career_data.market_trends IS '市场趋势数据（岗位需求、薪资、热门技能）';
COMMENT ON COLUMN ai_career_plan.user_career_data.skill_radar IS '能力雷达图数据（技术/创新/抗压/沟通/学习/实习）';
COMMENT ON COLUMN ai_career_plan.user_career_data.actions IS '行动建议列表';

-- user_roadmap_steps 表注释
COMMENT ON TABLE ai_career_plan.user_roadmap_steps IS '用户职业发展路径表（Dashboard/Roadmap 模块）';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.id IS '主键';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.user_id IS '用户 ID（逻辑外键）';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.job_profile_id IS '关联岗位 ID';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.current_step_index IS '当前所在阶段索引';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.steps IS '职业发展阶段列表';

-- recruitment_data 表注释
COMMENT ON TABLE ai_career_plan.recruitment_data IS '企业招聘数据表';
COMMENT ON COLUMN ai_career_plan.recruitment_data.id IS '主键';
COMMENT ON COLUMN ai_career_plan.recruitment_data.position_name IS '职位名称';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_name IS '公司全称';
COMMENT ON COLUMN ai_career_plan.recruitment_data.industry IS '所属行业';
COMMENT ON COLUMN ai_career_plan.recruitment_data.city IS '工作城市';
COMMENT ON COLUMN ai_career_plan.recruitment_data.salary_range IS '薪资范围';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_size IS '人员规模';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_nature IS '企业性质';
COMMENT ON COLUMN ai_career_plan.recruitment_data.position_code IS '职位编码';
COMMENT ON COLUMN ai_career_plan.recruitment_data.job_description IS '职位描述（HTML 原文）';
COMMENT ON COLUMN ai_career_plan.recruitment_data.job_requirements IS '任职要求';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_description IS '公司简介';
COMMENT ON COLUMN ai_career_plan.recruitment_data.source_url IS '岗位来源地址';
COMMENT ON COLUMN ai_career_plan.recruitment_data.publish_date IS '更新日期';

-- =============================================
-- 5. 表关系说明
-- =============================================
/*
表关系图：

users (用户表)
    ↓ (1:N)
user_vector_store (向量存储表)
    ├── vector_type = 'resume' → 简历数据
    └── vector_type = 'recruitment' → 招聘数据
    ↓ (1:1)
resume_analysis_result (分析结果表)
    ↓ (1:1)
student_capability_profile (学生能力画像表)
    ↓ (AI 聚合计算，自动填充)
user_career_data (Dashboard 职业数据表)
user_roadmap_steps (职业发展路径表)

recruitment_data (企业招聘数据表) - 独立表，用于存储和检索岗位信息
*/

-- =============================================
-- 6. 初始数据
-- =============================================
INSERT INTO ai_career_plan.users (user_name, user_password, sex)
VALUES ('admin', '123456', 1);
