-- =============================================================================
-- Alter job table to add job_profile column for storing job profile data
-- =============================================================================

-- Add job_profile column (JSONB) to store structured job profile data
ALTER TABLE ai_career_plan.job 
ADD COLUMN IF NOT EXISTS job_profile JSONB DEFAULT '{}';

-- Add comment for the new column
COMMENT ON COLUMN ai_career_plan.job.job_profile IS 'AI-generated job profile including skills, capabilities, certificates, etc.';

-- Create index for JSONB queries
CREATE INDEX IF NOT EXISTS idx_job_profile ON ai_career_plan.job USING GIN (job_profile);
