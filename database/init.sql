-- 设置搜索路径
SET search_path TO ai_career_plan, public;

-- 清理旧表
DROP TABLE IF EXISTS ai_career_plan.user_vector_store CASCADE;
DROP TABLE IF EXISTS ai_career_plan.users CASCADE;
DROP TABLE IF EXISTS ai_career_plan.resume_analysis_result CASCADE;

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
                                                  parsing_status VARCHAR(50),
                                                  parsing_progress INTEGER,
                                                  error_message TEXT,
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
                                                       parsed_data JSONB DEFAULT '{}',
                                                       scores JSONB DEFAULT '{}',
                                                       highlights JSONB DEFAULT '{}',
                                                       suggestions JSONB DEFAULT '[]',
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

-- =============================================
-- 初始数据
-- =============================================
INSERT INTO ai_career_plan.users (user_name, user_password, sex)
VALUES ('admin', '123456', 1);

-- =============================================
-- 注释
-- =============================================
-- user_vector_store 表注释
COMMENT ON COLUMN ai_career_plan.user_vector_store.content IS '文档内容（简历文件内容）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.resume_file_path IS '简历文件存储路径';
COMMENT ON COLUMN ai_career_plan.user_vector_store.vector_type IS '向量类型：resume/其他';
COMMENT ON COLUMN ai_career_plan.user_vector_store.embedding IS 'embedding 向量（1024 维）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.metadata IS '元数据（JSON 格式）';
COMMENT ON COLUMN ai_career_plan.user_vector_store.parsing_status IS '解析状态：UPLOADING/PARSING/EMBEDDING/COMPLETED/FAILED';
COMMENT ON COLUMN ai_career_plan.user_vector_store.parsing_progress IS '解析进度：0-100';
COMMENT ON COLUMN ai_career_plan.user_vector_store.error_message IS '解析错误信息';

-- resume_analysis_result 表注释
COMMENT ON TABLE ai_career_plan.resume_analysis_result IS '简历分析结果表';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.id IS '主键';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.vector_store_id IS '关联 user_vector_store.id';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.user_id IS '用户 ID';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.file_type IS '文件类型：pdf / docx / pptx / html / txt';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.original_file_name IS '原始文件名';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.parsed_data IS '解析后的结构化数据';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.scores IS '各维度评分';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.highlights IS '亮点列表';
COMMENT ON COLUMN ai_career_plan.resume_analysis_result.suggestions IS '优化建议';