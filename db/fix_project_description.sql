-- Fix "Data too long for column 'description'" on project/project_task.
ALTER TABLE `project` MODIFY COLUMN `description` TEXT NULL;
ALTER TABLE `project_task` MODIFY COLUMN `description` TEXT NULL;
