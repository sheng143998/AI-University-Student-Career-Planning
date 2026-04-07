-- ===========================================================================
-- 数据库初始化脚本：AI 职业规划平台（12 张表完整版）
-- PostgreSQL + pgvector
-- ===========================================================================

-- 设置搜索路径
SET search_path TO ai_career_plan, public;

-- =============================================================================
-- 清理旧表（按依赖关系倒序删除）
-- =============================================================================
DROP TABLE IF EXISTS ai_career_plan.user_roadmap_steps CASCADE;
DROP TABLE IF EXISTS ai_career_plan.user_career_data CASCADE;
DROP TABLE IF EXISTS ai_career_plan.student_capability_profile CASCADE;
DROP TABLE IF EXISTS ai_career_plan.resume_analysis_result CASCADE;
DROP TABLE IF EXISTS ai_career_plan.job CASCADE;
DROP TABLE IF EXISTS ai_career_plan.job_vector_store CASCADE;
DROP TABLE IF EXISTS ai_career_plan.recruitment_data CASCADE;
DROP TABLE IF EXISTS ai_career_plan.goal_milestone CASCADE;
DROP TABLE IF EXISTS ai_career_plan.goal CASCADE;
DROP TABLE IF EXISTS ai_career_plan.chat_message CASCADE;
DROP TABLE IF EXISTS ai_career_plan.chat_conversation CASCADE;
DROP TABLE IF EXISTS ai_career_plan.user_vector_store CASCADE;
DROP TABLE IF EXISTS ai_career_plan.users CASCADE;

-- =============================================================================
-- 启用 vector 扩展并创建 schema
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS ai_career_plan;

-- =============================================================================
-- 1. users 表（用户表）
-- =============================================================================
CREATE TABLE ai_career_plan.users (
                                      id BIGSERIAL PRIMARY KEY,
                                      user_name VARCHAR(255) NOT NULL UNIQUE,
                                      user_password VARCHAR(500) NOT NULL,
                                      create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      user_image VARCHAR(500),
                                      sex INTEGER DEFAULT 1
);

-- =============================================================================
-- 2. user_vector_store 表（向量存储表）
-- =============================================================================
CREATE TABLE ai_career_plan.user_vector_store (
                                                  id VARCHAR(255) PRIMARY KEY,
                                                  user_id BIGINT,
                                                  content TEXT,
                                                  resume_file_path VARCHAR(500),
                                                  vector_type VARCHAR(50) DEFAULT 'resume',
                                                  embedding VECTOR(1024),
                                                  metadata JSONB DEFAULT '{}',
                                                  parsing_status VARCHAR(50),
                                                  parsing_progress INTEGER,
                                                  error_message TEXT,
                                                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 3. resume_analysis_result 表（简历分析结果表）
-- =============================================================================
CREATE TABLE ai_career_plan.resume_analysis_result (
                                                       id BIGSERIAL PRIMARY KEY,
                                                       vector_store_id VARCHAR(255) NOT NULL,
                                                       user_id BIGINT NOT NULL,
                                                       file_type VARCHAR(20) NOT NULL,
                                                       original_file_name VARCHAR(500),
                                                       resume_file_path VARCHAR(500),
                                                       status VARCHAR(50) DEFAULT 'pending',
                                                       progress INTEGER DEFAULT 0,
                                                       parsed_data JSONB DEFAULT '{}',
                                                       scores JSONB DEFAULT '{}',
                                                       highlights JSONB DEFAULT '{}',
                                                       suggestions JSONB DEFAULT '[]',
                                                       create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                       update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 4. student_capability_profile 表（学生就业能力画像表）
-- =============================================================================
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

-- =============================================================================
-- 5. chat_conversation 表（聊天会话表）
-- =============================================================================
CREATE TABLE ai_career_plan.chat_conversation (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  user_id BIGINT NOT NULL,
                                                  title VARCHAR(255) NOT NULL,
                                                  last_message_at TIMESTAMP,
                                                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 6. chat_message 表（聊天消息表）
-- =============================================================================
CREATE TABLE ai_career_plan.chat_message (
                                             id BIGSERIAL PRIMARY KEY,
                                             conversation_id BIGINT NOT NULL,
                                             user_id BIGINT NOT NULL,
                                             role VARCHAR(50) NOT NULL,
                                             content TEXT NOT NULL,
                                             create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 7. goal 表（目标表）
-- =============================================================================
CREATE TABLE ai_career_plan.goal (
                                     id BIGSERIAL PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     title VARCHAR(255) NOT NULL,
                                     goal_desc TEXT,
                                     status VARCHAR(50) DEFAULT 'TODO',
                                     progress INTEGER DEFAULT 0,
                                     eta VARCHAR(100),
                                     is_primary BOOLEAN DEFAULT FALSE,
                                     success_salary VARCHAR(100),
                                     success_companies JSONB DEFAULT '[]',
                                     success_cities JSONB DEFAULT '[]',
                                     long_term_aspirations JSONB DEFAULT '[]',
                                     ai_advice TEXT,
                                     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 8. goal_milestone 表（里程碑表）
-- =============================================================================
CREATE TABLE ai_career_plan.goal_milestone (
                                               id BIGSERIAL PRIMARY KEY,
                                               goal_id BIGINT NOT NULL,
                                               user_id BIGINT NOT NULL,
                                               title VARCHAR(255) NOT NULL,
                                               milestone_desc TEXT,
                                               status VARCHAR(50) DEFAULT 'TODO',
                                               progress INTEGER DEFAULT 0,
                                               sort_order INTEGER DEFAULT 1,
                                               create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 10. recruitment_data 表（企业招聘数据表）
-- =============================================================================
CREATE TABLE ai_career_plan.recruitment_data (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 job_name VARCHAR(255),
                                                 company_name VARCHAR(255),
                                                 industry VARCHAR(100),
                                                 city VARCHAR(100),
                                                 salary_range VARCHAR(100),
                                                 company_size VARCHAR(100),
                                                 company_nature VARCHAR(100),
                                                 position_code VARCHAR(100),
                                                 job_description TEXT,
                                                 job_requirements TEXT,
                                                 company_description TEXT,
                                                 source_url VARCHAR(500),
                                                 publish_date VARCHAR(50),
                                                 create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 11. job 表（岗位分类结果表 - AI 智能分类）
-- =============================================================================
CREATE TABLE ai_career_plan.job (
                                    id BIGSERIAL PRIMARY KEY,
                                    job_category_code VARCHAR(50) NOT NULL,
                                    job_category_name VARCHAR(100) NOT NULL,
                                    job_level VARCHAR(20) NOT NULL,
                                    job_level_name VARCHAR(50),

    -- 薪资范围
                                    min_salary NUMERIC(10,2),
                                    max_salary NUMERIC(10,2),
                                    salary_unit VARCHAR(20),

    -- 关联原始数据（逻辑外键）
                                    source_job_ids JSONB DEFAULT '[]',
                                    source_job_count INTEGER DEFAULT 0,

    -- 岗位要求
                                    required_experience_years INT,
                                    required_skills JSONB DEFAULT '[]',
                                    job_description TEXT,

    -- AI 分析元数据
                                    ai_confidence_score NUMERIC(3,2),
                                    analysis_prompt_used VARCHAR(100),

    -- 审计字段
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_category ON ai_career_plan.job(job_category_code);
CREATE INDEX idx_job_level ON ai_career_plan.job(job_level);
CREATE INDEX idx_job_category_level ON ai_career_plan.job(job_category_code, job_level);

-- =============================================================================
-- 12. job_vector_store 表（岗位向量存储表）
-- =============================================================================
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

-- =============================================================================
-- 13. user_career_data 表（用户职业数据表 - Dashboard 模块）
-- =============================================================================
CREATE TABLE ai_career_plan.user_career_data (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 user_id BIGINT NOT NULL,
                                                 target_job VARCHAR(100),
                                                 target_job_id BIGINT,
                                                 job_profile JSONB,
                                                 match_summary JSONB,
                                                 market_trends JSONB,
                                                 skill_radar JSONB,
                                                 actions JSONB,
                                                 create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 14. user_roadmap_steps 表（用户职业发展路径表 - Dashboard/Roadmap 模块）
-- =============================================================================
CREATE TABLE ai_career_plan.user_roadmap_steps (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   user_id BIGINT NOT NULL,
                                                   job_profile_id BIGINT,
                                                   current_step_index INTEGER,
                                                   steps JSONB,
                                                   create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                   update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 索引定义
-- =============================================================================
-- users 表索引
CREATE INDEX idx_users_user_name ON ai_career_plan.users(user_name);

-- user_vector_store 表索引
CREATE INDEX idx_user_vector_user_id ON ai_career_plan.user_vector_store(user_id);
CREATE INDEX idx_user_vector_type ON ai_career_plan.user_vector_store(vector_type);
CREATE INDEX idx_user_vector_embedding ON ai_career_plan.user_vector_store USING hnsw (embedding vector_cosine_ops);

-- resume_analysis_result 表索引
CREATE INDEX idx_resume_analysis_vector_store_id ON ai_career_plan.resume_analysis_result(vector_store_id);
CREATE INDEX idx_resume_analysis_user_id ON ai_career_plan.resume_analysis_result(user_id);

-- student_capability_profile 表索引
CREATE INDEX idx_student_capability_profile_user_id ON ai_career_plan.student_capability_profile(user_id);
CREATE INDEX idx_student_capability_profile_resume_analysis_id ON ai_career_plan.student_capability_profile(resume_analysis_id);

-- chat_conversation 表索引
CREATE INDEX idx_chat_conversation_user_id ON ai_career_plan.chat_conversation(user_id);
CREATE INDEX idx_chat_conversation_last_message_at ON ai_career_plan.chat_conversation(last_message_at);

-- chat_message 表索引
CREATE INDEX idx_chat_message_conversation_id ON ai_career_plan.chat_message(conversation_id);
CREATE INDEX idx_chat_message_user_id ON ai_career_plan.chat_message(user_id);

-- goal 表索引
CREATE INDEX idx_goal_user_id ON ai_career_plan.goal(user_id);
CREATE INDEX idx_goal_is_primary ON ai_career_plan.goal(is_primary);

-- goal_milestone 表索引
CREATE INDEX idx_goal_milestone_goal_id ON ai_career_plan.goal_milestone(goal_id);
CREATE INDEX idx_goal_milestone_user_id ON ai_career_plan.goal_milestone(user_id);

-- recruitment_data 表索引
CREATE INDEX idx_recruitment_job_name ON ai_career_plan.recruitment_data(job_name);
CREATE INDEX idx_recruitment_company_name ON ai_career_plan.recruitment_data(company_name);
CREATE INDEX idx_recruitment_city ON ai_career_plan.recruitment_data(city);
CREATE INDEX idx_recruitment_industry ON ai_career_plan.recruitment_data(industry);

-- job 表索引
CREATE INDEX idx_job_category ON ai_career_plan.job(job_category_code);
CREATE INDEX idx_job_level ON ai_career_plan.job(job_level);
CREATE INDEX idx_job_category_level ON ai_career_plan.job(job_category_code, job_level);

-- job_vector_store 表索引
CREATE INDEX idx_job_vector_job_id ON ai_career_plan.job_vector_store(job_id);
CREATE INDEX idx_job_vector_content_hash ON ai_career_plan.job_vector_store(content_hash);
CREATE INDEX idx_job_vector_embedding ON ai_career_plan.job_vector_store USING hnsw (embedding vector_cosine_ops);

-- user_career_data 表索引
CREATE INDEX idx_user_career_data_user_id ON ai_career_plan.user_career_data(user_id);

-- user_roadmap_steps 表索引
CREATE INDEX idx_user_roadmap_steps_user_id ON ai_career_plan.user_roadmap_steps(user_id);
CREATE INDEX idx_user_roadmap_steps_job_profile_id ON ai_career_plan.user_roadmap_steps(job_profile_id);

-- =============================================================================
-- 表注释
-- =============================================================================
COMMENT ON TABLE ai_career_plan.users IS '用户表';
COMMENT ON COLUMN ai_career_plan.users.id IS '主键';
COMMENT ON COLUMN ai_career_plan.users.user_name IS '用户名（唯一）';
COMMENT ON COLUMN ai_career_plan.users.user_password IS '用户密码';
COMMENT ON COLUMN ai_career_plan.users.create_time IS '创建时间';
COMMENT ON COLUMN ai_career_plan.users.user_image IS '用户头像 URL';
COMMENT ON COLUMN ai_career_plan.users.sex IS '性别：1-男，2-女';

COMMENT ON TABLE ai_career_plan.user_vector_store IS '向量存储表（支持简历和招聘数据）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.id IS '主键 (UUID)';
COMMENT ON COLUMN ai_career_plan.user_vector_store.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.user_vector_store.content IS '文档内容（简历文件内容）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.resume_file_path IS '简历文件存储路径';
COMMENT ON COLUMN ai_career_plan.user_vector_store.vector_type IS '向量类型：resume/job/other';
COMMENT ON COLUMN ai_career_plan.user_vector_store.embedding IS 'embedding 向量（1024 维）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.metadata IS '元数据（JSON 格式）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.parsing_status IS '解析状态：UPLOADING/PARSING/EMBEDDING/COMPLETED/FAILED';
COMMENT ON COLUMN ai_career_plan.user_vector_store.parsing_progress IS '解析进度：0-100';
COMMENT ON COLUMN ai_career_plan.user_vector_store.error_message IS '解析错误信息';

COMMENT ON TABLE ai_career_plan.resume_analysis_result IS '简历分析结果表';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.id IS '主键';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.vector_store_id IS '关联 user_vector_store.id';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.file_type IS '文件类型：pdf/docx/pptx/html/txt';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.original_file_name IS '原始文件名';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.resume_file_path IS 'OSS 文件路径';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.status IS '处理状态：pending/processing/completed/failed/stopped';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.progress IS '处理进度：0-100';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.parsed_data IS '解析后的结构化数据';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.scores IS '各维度评分';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.highlights IS '亮点列表';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.suggestions IS '优化建议';

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

COMMENT ON TABLE ai_career_plan.chat_conversation IS '聊天会话表';
COMMENT ON COLUMN ai_career_plan.chat_conversation.id IS '主键';
COMMENT ON COLUMN ai_career_plan.chat_conversation.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.chat_conversation.title IS '会话标题';
COMMENT ON COLUMN ai_career_plan.chat_conversation.last_message_at IS '最后消息时间';
COMMENT ON COLUMN ai_career_plan.chat_conversation.create_time IS '创建时间';
COMMENT ON COLUMN ai_career_plan.chat_conversation.update_time IS '更新时间';

COMMENT ON TABLE ai_career_plan.chat_message IS '聊天消息表';
COMMENT ON COLUMN ai_career_plan.chat_message.id IS '主键';
COMMENT ON COLUMN ai_career_plan.chat_message.conversation_id IS '关联会话 ID';
COMMENT ON COLUMN ai_career_plan.chat_message.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.chat_message.role IS '角色：user/assistant/system';
COMMENT ON COLUMN ai_career_plan.chat_message.content IS '消息内容';
COMMENT ON COLUMN ai_career_plan.chat_message.create_time IS '创建时间';

COMMENT ON TABLE ai_career_plan.goal IS '目标表';
COMMENT ON COLUMN ai_career_plan.goal.id IS '主键';
COMMENT ON COLUMN ai_career_plan.goal.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.goal.title IS '目标标题';
COMMENT ON COLUMN ai_career_plan.goal.goal_desc IS '目标描述';
COMMENT ON COLUMN ai_career_plan.goal.status IS '状态：TODO/IN_PROGRESS/DONE';
COMMENT ON COLUMN ai_career_plan.goal.progress IS '进度：0-100';
COMMENT ON COLUMN ai_career_plan.goal.eta IS '预计达成时间';
COMMENT ON COLUMN ai_career_plan.goal.is_primary IS '是否为主目标';
COMMENT ON COLUMN ai_career_plan.goal.success_salary IS '成功准则 - 薪资预期';
COMMENT ON COLUMN ai_career_plan.goal.success_companies IS '成功准则 - 目标公司 (JSON 数组)';
COMMENT ON COLUMN ai_career_plan.goal.success_cities IS '成功准则 - 目标城市 (JSON 数组)';
COMMENT ON COLUMN ai_career_plan.goal.long_term_aspirations IS '长期愿景 (JSON 数组)';
COMMENT ON COLUMN ai_career_plan.goal.ai_advice IS 'AI 建议';

COMMENT ON TABLE ai_career_plan.goal_milestone IS '里程碑表';
COMMENT ON COLUMN ai_career_plan.goal_milestone.id IS '主键';
COMMENT ON COLUMN ai_career_plan.goal_milestone.goal_id IS '关联目标 ID';
COMMENT ON COLUMN ai_career_plan.goal_milestone.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.goal_milestone.title IS '里程碑标题';
COMMENT ON COLUMN ai_career_plan.goal_milestone.milestone_desc IS '里程碑描述';
COMMENT ON COLUMN ai_career_plan.goal_milestone.status IS '状态：TODO/IN_PROGRESS/DONE';
COMMENT ON COLUMN ai_career_plan.goal_milestone.progress IS '进度：0-100';
COMMENT ON COLUMN ai_career_plan.goal_milestone.sort_order IS '排序顺序';

COMMENT ON TABLE ai_career_plan.recruitment_data IS '企业招聘数据表';
COMMENT ON COLUMN ai_career_plan.recruitment_data.id IS '主键';
COMMENT ON COLUMN ai_career_plan.recruitment_data.job_name IS '岗位名称';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_name IS '公司名称';
COMMENT ON COLUMN ai_career_plan.recruitment_data.industry IS '所属行业';
COMMENT ON COLUMN ai_career_plan.recruitment_data.city IS '工作城市';
COMMENT ON COLUMN ai_career_plan.recruitment_data.salary_range IS '薪资范围';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_size IS '公司规模';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_nature IS '公司性质';
COMMENT ON COLUMN ai_career_plan.recruitment_data.position_code IS '岗位编码';
COMMENT ON COLUMN ai_career_plan.recruitment_data.job_description IS '岗位职责描述';
COMMENT ON COLUMN ai_career_plan.recruitment_data.job_requirements IS '任职要求';
COMMENT ON COLUMN ai_career_plan.recruitment_data.company_description IS '公司详情';
COMMENT ON COLUMN ai_career_plan.recruitment_data.source_url IS '来源地址';
COMMENT ON COLUMN ai_career_plan.recruitment_data.publish_date IS '发布日期';
COMMENT ON COLUMN ai_career_plan.recruitment_data.create_time IS '创建时间';

COMMENT ON TABLE ai_career_plan.job IS '岗位分类结果表（AI 智能分类）';
COMMENT ON COLUMN ai_career_plan.job.id IS '主键';
COMMENT ON COLUMN ai_career_plan.job.job_category_code IS '岗位类别编码';
COMMENT ON COLUMN ai_career_plan.job.job_category_name IS '岗位类别名称';
COMMENT ON COLUMN ai_career_plan.job.job_level IS '岗位级别：INTERNSHIP/JUNIOR/MID/SENIOR';
COMMENT ON COLUMN ai_career_plan.job.job_level_name IS '岗位级别名称：实习岗/初级岗/中级岗/高级岗';
COMMENT ON COLUMN ai_career_plan.job.min_salary IS '最低薪资';
COMMENT ON COLUMN ai_career_plan.job.max_salary IS '最高薪资';
COMMENT ON COLUMN ai_career_plan.job.salary_unit IS '薪资单位：DAY/MONTH/YEAR';
COMMENT ON COLUMN ai_career_plan.job.source_job_ids IS '关联的原始岗位 ID 列表';
COMMENT ON COLUMN ai_career_plan.job.source_job_count IS '关联的原始岗位数量';
COMMENT ON COLUMN ai_career_plan.job.required_experience_years IS '要求工作年限';
COMMENT ON COLUMN ai_career_plan.job.required_skills IS ' Required skills list';
COMMENT ON COLUMN ai_career_plan.job.job_description IS '岗位描述';
COMMENT ON COLUMN ai_career_plan.job.ai_confidence_score IS 'AI 置信度 (0.00-1.00)';
COMMENT ON COLUMN ai_career_plan.job.analysis_prompt_used IS '使用的 AI 提示词';
COMMENT ON COLUMN ai_career_plan.job.created_at IS '创建时间';
COMMENT ON COLUMN ai_career_plan.job.updated_at IS '更新时间';

COMMENT ON TABLE ai_career_plan.job_vector_store IS '岗位向量存储表';
COMMENT ON COLUMN ai_career_plan.job_vector_store.id IS '主键 (UUID)';
COMMENT ON COLUMN ai_career_plan.job_vector_store.job_id IS '关联 recruitment_data.id';
COMMENT ON COLUMN ai_career_plan.job_vector_store.content IS '岗位内容 (合并岗位名称、详情等)';
COMMENT ON COLUMN ai_career_plan.job_vector_store.embedding IS 'embedding 向量 (1024 维)';
COMMENT ON COLUMN ai_career_plan.job_vector_store.metadata IS '元数据 (JSON 格式)';
COMMENT ON COLUMN ai_career_plan.job_vector_store.content_hash IS '内容哈希 (用于去重)';

COMMENT ON TABLE ai_career_plan.user_career_data IS '用户职业数据表（Dashboard 模块）';
COMMENT ON COLUMN ai_career_plan.user_career_data.id IS '主键';
COMMENT ON COLUMN ai_career_plan.user_career_data.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.user_career_data.job_profile IS '岗位画像信息';
COMMENT ON COLUMN ai_career_plan.user_career_data.match_summary IS '匹配度摘要';
COMMENT ON COLUMN ai_career_plan.user_career_data.market_trends IS '市场趋势数据';
COMMENT ON COLUMN ai_career_plan.user_career_data.skill_radar IS '能力雷达图数据';
COMMENT ON COLUMN ai_career_plan.user_career_data.actions IS '行动建议列表';

COMMENT ON TABLE ai_career_plan.user_roadmap_steps IS '用户职业发展路径表（Dashboard/Roadmap 模块）';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.id IS '主键';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.job_profile_id IS '关联岗位 ID';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.current_step_index IS '当前所在阶段索引';
COMMENT ON COLUMN ai_career_plan.user_roadmap_steps.steps IS '职业发展阶段列表 (JSON)';

-- =============================================================================
-- 表关系说明
-- =============================================================================
/*
表关系说明:

users (用户表)
    ├── (1:N) → user_vector_store (向量存储表)
    ├── (1:N) → resume_analysis_result (简历分析结果表)
    ├── (1:N) → student_capability_profile (学生能力画像表)
    ├── (1:N) → chat_conversation (聊天会话表)
    ├── (1:N) → chat_message (聊天消息表)
    ├── (1:N) → goal (目标表)
    ├── (1:N) → goal_milestone (里程碑表)
    ├── (1:N) → user_career_data (职业数据表)
    └── (1:N) → user_roadmap_steps (职业发展路径表)

user_vector_store (向量存储表)
    └── (1:1) → resume_analysis_result (简历分析结果表)

resume_analysis_result (简历分析结果表)
    └── (1:1) → student_capability_profile (学生能力画像表)

chat_conversation (聊天会话表)
    └── (1:N) → chat_message (聊天消息表)

goal (目标表)
    └── (1:N) → goal_milestone (里程碑表)

recruitment_data (招聘数据表)
    └── (1:1) → job_vector_store (岗位向量存储表)

job (岗位分类结果表)
    └── 通过 source_job_ids 关联 recruitment_data 表
*/

-- =============================================================================
-- 初始数据
-- =============================================================================
INSERT INTO ai_career_plan.users (user_name, user_password, sex)
VALUES ('admin', '123456', 1);
