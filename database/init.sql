-- 设置搜索路径
SET search_path TO ai_career_plan, public;

-- 清理旧表
DROP TABLE IF EXISTS ai_career_plan.recruitment_data CASCADE;
DROP TABLE IF EXISTS ai_career_plan.job_vector_store CASCADE;
DROP TABLE IF EXISTS ai_career_plan.user_vector_store CASCADE;
DROP TABLE IF EXISTS ai_career_plan.users CASCADE;
DROP TABLE IF EXISTS ai_career_plan.resume_analysis_result CASCADE;
DROP TABLE IF EXISTS ai_career_plan.student_capability_profile CASCADE;
DROP TABLE IF EXISTS ai_career_plan.user_career_data CASCADE;
DROP TABLE IF EXISTS ai_career_plan.user_roadmap_steps CASCADE;

-- 启用 vector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建 schema
CREATE SCHEMA IF NOT EXISTS ai_career_plan;

-- =============================================
-- users 表（用户表）
-- =============================================
CREATE TABLE ai_career_plan.users (
                                      id BIGSERIAL PRIMARY KEY,
                                      user_name VARCHAR(255) NOT NULL UNIQUE,
                                      user_password VARCHAR(500) NOT NULL,
                                      create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      user_image VARCHAR(500),
                                      sex INTEGER DEFAULT 1
);

-- =============================================
-- user_vector_store 表（向量存储表）
-- =============================================
CREATE TABLE ai_career_plan.user_vector_store (
                                                  id VARCHAR(255) PRIMARY KEY,
                                                  user_id BIGINT,
                                                  content TEXT,
                                                  resume_file_path VARCHAR(500),
                                                  vector_type VARCHAR(50) DEFAULT 'resume',
                                                  embedding VECTOR(1024),
                                                  metadata JSONB DEFAULT '{}',
                                                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- resume_analysis_result 表（简历分析结果表）
-- =============================================
CREATE TABLE ai_career_plan.resume_analysis_result (
                                                       id BIGSERIAL PRIMARY KEY,
                                                       vector_store_id VARCHAR(255) NOT NULL,
                                                       user_id BIGINT NOT NULL,
                                                       file_type VARCHAR(20) NOT NULL,
                                                       original_file_name VARCHAR(500),
                                                       resume_file_path VARCHAR(500),          -- OSS 文件路径
                                                       status VARCHAR(50) DEFAULT 'pending',   -- 处理状态：pending/processing/completed/failed/stopped
                                                       progress INTEGER DEFAULT 0,             -- 处理进度：0-100
                                                       parsed_data JSONB DEFAULT '{}',
                                                       scores JSONB DEFAULT '{}',
                                                       highlights JSONB DEFAULT '{}',
                                                       suggestions JSONB DEFAULT '[]',
                                                       create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                       update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- student_capability_profile 表（学生就业能力画像表）
-- =============================================
CREATE TABLE ai_career_plan.student_capability_profile (
                                                           id BIGSERIAL PRIMARY KEY,
                                                           user_id BIGINT NOT NULL,
                                                           resume_analysis_id BIGINT NOT NULL,
                                                           overall_score INT NOT NULL,
                                                           completeness_score INT NOT NULL,
                                                           competitiveness_score INT NOT NULL,
                                                           capability_scores JSONB DEFAULT '{}',
                                                           professional_skills JSONB DEFAULT '[]',
                                                           certificates JSONB DEFAULT '[]',
                                                           soft_skills JSONB DEFAULT '{}',
                                                           ai_evaluation TEXT,
                                                           generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- user_career_data 表（用户职业数据表 - Dashboard 模块）
-- =============================================
CREATE TABLE ai_career_plan.user_career_data (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 user_id BIGINT NOT NULL,
                                                 job_profile JSONB,
                                                 match_summary JSONB,
                                                 market_trends JSONB,
                                                 skill_radar JSONB,
                                                 actions JSONB,
                                                 create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- user_roadmap_steps 表（用户职业发展路径表 - Dashboard/Roadmap 模块）
-- =============================================
CREATE TABLE ai_career_plan.user_roadmap_steps (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   user_id BIGINT NOT NULL,
                                                   job_profile_id BIGINT,
                                                   current_step_index INTEGER,
                                                   steps JSONB,
                                                   create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                   update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- job_vector_store 表（岗位向量存储表）
-- =============================================
CREATE TABLE ai_career_plan.job_vector_store (
                                                 id VARCHAR(255) PRIMARY KEY,
                                                 job_id BIGINT,
                                                 content TEXT,
                                                 embedding VECTOR(1024),
                                                 metadata JSONB DEFAULT '{}',
                                                 content_hash VARCHAR(64),
                                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- recruitment_data 表（企业招聘数据表）
-- =============================================
CREATE TABLE ai_career_plan.recruitment_data (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 job_name VARCHAR(255) NOT NULL,
                                                 address VARCHAR(500),
                                                 salary_range VARCHAR(100),
                                                 company_name VARCHAR(255),
                                                 industry VARCHAR(255),
                                                 company_size VARCHAR(100),
                                                 company_type VARCHAR(100),
                                                 job_code VARCHAR(100),
                                                 job_detail TEXT,
                                                 update_date VARCHAR(50),
                                                 company_detail TEXT,
                                                 source_url VARCHAR(500),
                                                 create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- 索引
-- =============================================
CREATE INDEX idx_users_user_name ON ai_career_plan.users(user_name);

CREATE INDEX idx_user_vector_user_id ON ai_career_plan.user_vector_store(user_id);
CREATE INDEX idx_user_vector_type ON ai_career_plan.user_vector_store(vector_type);
CREATE INDEX idx_embedding ON ai_career_plan.user_vector_store USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_resume_analysis_vector_store_id ON ai_career_plan.resume_analysis_result(vector_store_id);
CREATE INDEX idx_resume_analysis_user_id ON ai_career_plan.resume_analysis_result(user_id);

CREATE INDEX idx_student_capability_profile_user_id ON ai_career_plan.student_capability_profile(user_id);
CREATE INDEX idx_student_capability_profile_resume_analysis_id ON ai_career_plan.student_capability_profile(resume_analysis_id);

CREATE INDEX idx_user_career_data_user_id ON ai_career_plan.user_career_data(user_id);
CREATE INDEX idx_user_roadmap_steps_user_id ON ai_career_plan.user_roadmap_steps(user_id);
CREATE INDEX idx_user_roadmap_steps_job_profile_id ON ai_career_plan.user_roadmap_steps(job_profile_id);

-- job_vector_store 表索引
CREATE INDEX idx_job_vector_job_id ON ai_career_plan.job_vector_store(job_id);
CREATE INDEX idx_job_vector_content_hash ON ai_career_plan.job_vector_store(content_hash);
CREATE INDEX idx_job_vector_embedding ON ai_career_plan.job_vector_store USING hnsw (embedding vector_cosine_ops);

-- recruitment_data 表索引
CREATE INDEX idx_recruitment_job_name ON ai_career_plan.recruitment_data(job_name);
CREATE INDEX idx_recruitment_company_name ON ai_career_plan.recruitment_data(company_name);
CREATE INDEX idx_recruitment_industry ON ai_career_plan.recruitment_data(industry);
CREATE INDEX idx_recruitment_address ON ai_career_plan.recruitment_data(address);



-- =============================================
-- 注释
-- =============================================
-- user_vector_store 表注释
COMMENT ON COLUMN ai_career_plan.user_vector_store.content IS '文档内容（简历文件内容）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.resume_file_path IS '简历文件存储路径';
COMMENT ON COLUMN ai_career_plan.user_vector_store.vector_type IS '向量类型：resume/其他';
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

-- job_vector_store 表注释
COMMENT ON TABLE ai_career_plan.job_vector_store IS '岗位向量存储表';
COMMENT ON COLUMN ai_career_plan.job_vector_store.id IS '主键 (UUID)';
COMMENT ON COLUMN ai_career_plan.job_vector_store.job_id IS '关联 recruitment_data.id';
COMMENT ON COLUMN ai_career_plan.job_vector_store.content IS '岗位内容 (合并岗位名称、详情等)';
COMMENT ON COLUMN ai_career_plan.job_vector_store.embedding IS 'embedding 向量 (1024 维)';
COMMENT ON COLUMN ai_career_plan.job_vector_store.metadata IS '元数据 (JSON 格式)';
COMMENT ON COLUMN ai_career_plan.job_vector_store.content_hash IS '内容哈希 (用于去重)';

-- recruitment_data 表注释
COMMENT ON TABLE ai_career_plan.recruitment_data IS '企业招聘数据表';
COMMENT ON COLUMN ai_career_plan.recruitment_data.id IS '主键';
COMMENT ON COLUMN ai_career_plan.recruitment_data.job_name IS '岗位名称';
COMMENT ON COLUMN ai_career_plan.recruitment_data.address IS '工作地址';
COMMENT ON COLUMN ai_career_plan.recruitment_data.salary_range IS '薪资范围 (支持按天/按月/年薪等格式)';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_name IS '公司名称';
COMMENT ON COLUMN ai_career_plan.recruitment_data.industry IS '所属行业';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_size IS '公司规模';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_type IS '公司类型';
COMMENT ON COLUMN ai_career_plan.recruitment_data.job_code IS '岗位编码';
COMMENT ON COLUMN ai_career_plan.recruitment_data.job_detail IS '岗位详情';
COMMENT ON COLUMN ai_career_plan.recruitment_data.update_date IS '更新日期';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_detail IS '公司详情';
COMMENT ON COLUMN ai_career_plan.recruitment_data.source_url IS '岗位来源地址';

-- =============================================
-- 表关系说明
-- =============================================
/*
user_vector_store (向量存储表)
    ↓ (1:1 关系)
resume_analysis_result (分析结果表)
    ↓ (1:1 关系)
student_capability_profile (学生能力画像表)
    ↓ (AI 聚合计算，自动填充)
user_career_data (Dashboard 职业数据表)
user_roadmap_steps (职业发展路径表)

job_vector_store (岗位向量存储表)
    ↓ (关联 recruitment_data.id)
recruitment_data (招聘数据表)
*/

-- =============================================
-- 初始数据
-- =============================================
INSERT INTO ai_career_plan.users (user_name, user_password, sex)
VALUES ('admin', '123456', 1);