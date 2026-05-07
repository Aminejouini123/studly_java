-- Studly Database Setup Script
-- This script recreates the entire database schema and inserts sample data.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Create Database
CREATE DATABASE IF NOT EXISTS `projet_db`;
USE `projet_db`;

-- 2. Drop existing tables to ensure a clean start
DROP TABLE IF EXISTS `project_task`;
DROP TABLE IF EXISTS `project`;
DROP TABLE IF EXISTS `pomodoro_session`;
DROP TABLE IF EXISTS `password_reset_token`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `motivation`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `invitation`;
DROP TABLE IF EXISTS `groups`;
DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `objective`;
DROP TABLE IF EXISTS `exam`;
DROP TABLE IF EXISTS `activity`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `roadmap_step`;
DROP TABLE IF EXISTS `roadmap`;
DROP TABLE IF EXISTS `event`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `personne`;

-- 3. Create Tables

CREATE TABLE `personne` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) NOT NULL,
  `prenom` varchar(255) NOT NULL,
  `age` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `google_id` varchar(255) DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT 0,
  `verification_code` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `roles` varchar(255) DEFAULT 'ROLE_STUDENT',
  `password` varchar(255) NOT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `statut` varchar(50) DEFAULT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  `education_level` varchar(100) DEFAULT NULL,
  `job_title` varchar(100) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `bio` text,
  `skills` text,
  `score` int(11) DEFAULT 0,
  `google_access_token` varchar(255) DEFAULT NULL,
  `google_refresh_token` varchar(255) DEFAULT NULL,
  `google_token_expires_at` timestamp NULL DEFAULT NULL,
  `ban_reason` varchar(500) DEFAULT NULL,
  `github_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `event` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `type` varchar(100) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `priority` varchar(50) DEFAULT NULL,
  `difficulty` int(11) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `start_time` timestamp NULL DEFAULT NULL,
  `end_time` timestamp NULL DEFAULT NULL,
  `color` varchar(50) DEFAULT NULL,
  `category` varchar(100) DEFAULT NULL,
  `notes` text,
  `all_day` tinyint(1) DEFAULT 0,
  `reminder_minutes` int(11) DEFAULT NULL,
  `google_event_id` varchar(255) DEFAULT NULL,
  `motivation_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_event_user` (`user_id`),
  CONSTRAINT `fk_event_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `course` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `course_file` varchar(255) DEFAULT NULL,
  `course_link` varchar(255) DEFAULT NULL,
  `teacher_email` varchar(255) DEFAULT NULL,
  `semester` varchar(50) DEFAULT NULL,
  `difficulty_level` varchar(50) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `priority` varchar(50) DEFAULT NULL,
  `coefficient` double DEFAULT 1.0,
  `status` varchar(50) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `comment` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_course_user` (`user_id`),
  CONSTRAINT `fk_course_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `activity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `file` varchar(255) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `difficulty` varchar(50) DEFAULT NULL,
  `level` varchar(50) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `instructions` text,
  `expected_output` text,
  `hints` text,
  `completed_at` timestamp NULL DEFAULT NULL,
  `course_id` int(11) DEFAULT NULL,
  `assigned_user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_activity_course` (`course_id`),
  KEY `fk_activity_user` (`assigned_user_id`),
  CONSTRAINT `fk_activity_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_activity_user` FOREIGN KEY (`assigned_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `exam` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `date` date DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `grade` double DEFAULT NULL,
  `difficulty` varchar(50) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `file` varchar(255) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  `course_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_exam_course` (`course_id`),
  CONSTRAINT `fk_exam_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `objective` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `estimated_duration` varchar(100) DEFAULT NULL,
  `real_duration` int(11) DEFAULT NULL,
  `priority` varchar(50) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `reason` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `task` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `repeat_count` int(11) DEFAULT 0,
  `status` varchar(50) DEFAULT NULL,
  `difficulty` int(11) DEFAULT NULL,
  `impact` double DEFAULT NULL,
  `deadline` timestamp NULL DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  `objective_id` int(11) DEFAULT NULL,
  `assigned_user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_task_objective` (`objective_id`),
  KEY `fk_task_user` (`assigned_user_id`),
  CONSTRAINT `fk_task_objective` FOREIGN KEY (`objective_id`) REFERENCES `objective` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_task_user` FOREIGN KEY (`assigned_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `capacity` int(11) DEFAULT NULL,
  `group_photo` varchar(255) DEFAULT NULL,
  `category` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `creator_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_group_creator` (`creator_id`),
  CONSTRAINT `fk_group_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `invitation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `status` varchar(50) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `sender_id` int(11) DEFAULT NULL,
  `receiver_id` int(11) DEFAULT NULL,
  `group_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_invitation_sender` (`sender_id`),
  KEY `fk_invitation_receiver` (`receiver_id`),
  KEY `fk_invitation_group` (`group_id`),
  CONSTRAINT `fk_invitation_group` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_invitation_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_invitation_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `message` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `sender_id` int(11) DEFAULT NULL,
  `group_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_message_group` (`group_id`),
  KEY `fk_message_sender` (`sender_id`),
  CONSTRAINT `fk_message_group` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
  -- Important: sender_id must reference `users(id)` (not legacy `user(id)`).
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `motivation` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `motivation_level` int(11) DEFAULT NULL,
  `emotion` varchar(100) DEFAULT NULL,
  `preparation` text,
  `reward` text,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_motivation_user` (`user_id`),
  CONSTRAINT `fk_motivation_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `notification` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_notification_user` (`user_id`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `password_reset_token` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `token` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NULL DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_password_reset_user` (`user_id`),
  CONSTRAINT `fk_password_reset_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `pomodoro_session` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(50) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `started_at` timestamp NULL DEFAULT NULL,
  `ended_at` timestamp NULL DEFAULT NULL,
  `focus_score` double DEFAULT NULL,
  `focus_logs` text,
  `event_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pomodoro_event` (`event_id`),
  CONSTRAINT `fk_pomodoro_event` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `project` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `status` varchar(50) DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `deadline` date DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `group_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_project_group` (`group_id`),
  CONSTRAINT `fk_project_group` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `project_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `status` varchar(50) DEFAULT NULL,
  `deadline` timestamp NULL DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  `deliverable` varchar(255) DEFAULT NULL,
  `grade` int(11) DEFAULT NULL,
  `attachment` varchar(255) DEFAULT NULL,
  `resource_path` varchar(255) DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  `assigned_user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_project_task_project` (`project_id`),
  KEY `fk_project_task_user` (`assigned_user_id`),
  CONSTRAINT `fk_project_task_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_task_user` FOREIGN KEY (`assigned_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `roadmap` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `skill` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `user_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_roadmap_user` (`user_id`),
  CONSTRAINT `fk_roadmap_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `roadmap_step` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `roadmap_id` int(11) NOT NULL,
  `step_number` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `resources_json` text,
  `is_completed` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `fk_step_roadmap` (`roadmap_id`),
  CONSTRAINT `fk_step_roadmap` FOREIGN KEY (`roadmap_id`) REFERENCES `roadmap` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Insert Sample Data

-- Sample Users (Passwords are hashed for testing)
INSERT INTO `users` (`email`, `roles`, `password`, `first_name`, `last_name`, `is_verified`) VALUES
('admin@studly.com', 'ROLE_ADMIN', '$2a$10$8.UnVuG9HHgffUDAlk8Kn.2GYf8IQvO/yXv.o6.a.Z.f.o.6.a.Z.f.', 'Admin', 'Studly', 1),
('superadmin@studly.com', '["ROLE_ADMIN"]', '$2a$12$uR/haoK2Bq.W21dXBcwdrevnRBW2VACmXnmD9xIvFPl4ZafLolSk2', 'Super', 'Admin', 1),
('student@studly.com', 'ROLE_STUDENT', '$2a$10$8.UnVuG9HHgffUDAlk8Kn.2GYf8IQvO/yXv.o6.a.Z.f.o.6.a.Z.f.', 'John', 'Doe', 1),
('teacher@studly.com', 'ROLE_TEACHER', '$2a$10$8.UnVuG9HHgffUDAlk8Kn.2GYf8IQvO/yXv.o6.a.Z.f.o.6.a.Z.f.', 'Jane', 'Smith', 1);

-- Sample Personne
INSERT INTO `personne` (`nom`, `prenom`, `age`) VALUES
('Doe', 'John', 25),
('Smith', 'Jane', 30);

-- Sample Courses
INSERT INTO `course` (`name`, `teacher_email`, `semester`, `difficulty_level`, `user_id`) VALUES
('Java Programming', 'teacher@studly.com', 'S2', 'Intermediate', 2),
('Web Development', 'teacher@studly.com', 'S1', 'Beginner', 2);

-- Sample Objectives
INSERT INTO `objective` (`title`, `description`, `status`) VALUES
('Finish Java Project', 'Complete the final project for the Java course', 'IN_PROGRESS'),
('Prepare for Exams', 'Review all course materials for upcoming exams', 'TODO');

-- Sample Groups
INSERT INTO `groups` (`capacity`, `category`, `creator_id`) VALUES
(10, 'Study', 1),
(5, 'Project', 2);

SET FOREIGN_KEY_CHECKS = 1;
