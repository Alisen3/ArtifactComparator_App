-- Fix foreign key constraints to reference store_artifacts instead of artifacts
-- Run this SQL script in your PostgreSQL database

-- Step 1: Drop the old incorrect foreign key constraints
ALTER TABLE comparison_tasks DROP CONSTRAINT IF EXISTS fk1epegyliglmw50el15d7nxipg;
ALTER TABLE comparison_tasks DROP CONSTRAINT IF EXISTS fk_comparison_tasks_artifact_a;
ALTER TABLE comparison_tasks DROP CONSTRAINT IF EXISTS fk_comparison_tasks_artifact_b;

-- Step 2: Add the correct foreign key constraints referencing store_artifacts
ALTER TABLE comparison_tasks
ADD CONSTRAINT fk_comparison_tasks_artifact_a
FOREIGN KEY (artifact_a_id)
REFERENCES store_artifacts(id)
ON DELETE RESTRICT;

ALTER TABLE comparison_tasks
ADD CONSTRAINT fk_comparison_tasks_artifact_b
FOREIGN KEY (artifact_b_id)
REFERENCES store_artifacts(id)
ON DELETE RESTRICT;

-- Verify the changes
SELECT
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
  AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
  AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name='comparison_tasks'
  AND (kcu.column_name = 'artifact_a_id' OR kcu.column_name = 'artifact_b_id');
