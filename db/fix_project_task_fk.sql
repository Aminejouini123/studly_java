-- Fix FK mismatch for studly_java:
-- Some DBs have `project_task.assigned_user_id` referencing `user(id)` (often with a Doctrine-style constraint name),
-- while this application uses the `users` table.
--
-- Database: projet_db
--
-- 1) Drop the wrong FK (constraint name from the observed error popup).
--    If your constraint name differs, run:
--      SELECT CONSTRAINT_NAME
--      FROM information_schema.KEY_COLUMN_USAGE
--      WHERE TABLE_SCHEMA = 'projet_db'
--        AND TABLE_NAME = 'project_task'
--        AND COLUMN_NAME = 'assigned_user_id'
--        AND REFERENCED_TABLE_NAME IS NOT NULL;
--
-- 2) Add the correct FK to `users(id)`.

ALTER TABLE project_task
  DROP FOREIGN KEY FK_6BEF133DADF66B1A;

ALTER TABLE project_task
  ADD CONSTRAINT fk_project_task_user
  FOREIGN KEY (assigned_user_id) REFERENCES users(id)
  ON DELETE SET NULL;

