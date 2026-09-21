SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `patent_wind` (
  `public_num` varchar(32) NOT NULL,
  `legal_status` text,
  `latest_legal_status` text,
  `status` text,
  `title` text,
  `type` text,
  `abstract` text,
  `appli_num` text,
  `appli_date` text,
  `public_date` text,
  `applicant` text,
  `applicant_address` text,
  `patentee` text,
  `patentee_address` text,
  `inventor` text,
  `agent` text,
  `IPC` text,
  `CPC` text,
  `NEC` text,
  `patent_details` text,
  PRIMARY KEY (`public_num`),
  KEY `idx_wind_public_num` (`public_num`),
  KEY `idx_wind_applicant` (`applicant`(255)),
  KEY `idx_wind_inventor` (`inventor`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `patent_solar` (
  `public_num` varchar(32) NOT NULL,
  `legal_status` text,
  `latest_legal_status` text,
  `status` text,
  `title` text,
  `type` text,
  `abstract` text,
  `appli_num` text,
  `appli_date` text,
  `public_date` text,
  `applicant` text,
  `applicant_address` text,
  `patentee` text,
  `patentee_address` text,
  `inventor` text,
  `agent` text,
  `IPC` text,
  `CPC` text,
  `NEC` text,
  `patent_details` text,
  PRIMARY KEY (`public_num`),
  KEY `idx_solar_public_num` (`public_num`),
  KEY `idx_solar_applicant` (`applicant`(255)),
  KEY `idx_solar_inventor` (`inventor`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `patent_biomass` (
  `public_num` varchar(32) NOT NULL,
  `legal_status` text,
  `latest_legal_status` text,
  `status` text,
  `title` text,
  `type` text,
  `abstract` text,
  `appli_num` text,
  `appli_date` text,
  `public_date` text,
  `applicant` text,
  `applicant_address` text,
  `patentee` text,
  `patentee_address` text,
  `inventor` text,
  `agent` text,
  `IPC` text,
  `CPC` text,
  `NEC` text,
  `patent_details` text,
  PRIMARY KEY (`public_num`),
  KEY `idx_biomass_public_num` (`public_num`),
  KEY `idx_biomass_applicant` (`applicant`(255)),
  KEY `idx_biomass_inventor` (`inventor`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `patent_hydrogen` (
  `public_num` varchar(32) NOT NULL,
  `legal_status` text,
  `latest_legal_status` text,
  `status` text,
  `title` text,
  `type` text,
  `abstract` text,
  `appli_num` text,
  `appli_date` text,
  `public_date` text,
  `applicant` text,
  `applicant_address` text,
  `patentee` text,
  `patentee_address` text,
  `inventor` text,
  `agent` text,
  `IPC` text,
  `CPC` text,
  `NEC` text,
  `patent_details` text,
  PRIMARY KEY (`public_num`),
  KEY `idx_hydrogen_public_num` (`public_num`),
  KEY `idx_hydrogen_applicant` (`applicant`(255)),
  KEY `idx_hydrogen_inventor` (`inventor`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `patent_lilon` (
  `public_num` varchar(32) NOT NULL,
  `legal_status` text,
  `latest_legal_status` text,
  `status` text,
  `title` text,
  `type` text,
  `abstract` text,
  `appli_num` text,
  `appli_date` text,
  `public_date` text,
  `applicant` text,
  `applicant_address` text,
  `patentee` text,
  `patentee_address` text,
  `inventor` text,
  `agent` text,
  `IPC` text,
  `CPC` text,
  `NEC` text,
  `patent_details` text,
  PRIMARY KEY (`public_num`),
  KEY `idx_lilon_public_num` (`public_num`),
  KEY `idx_lilon_applicant` (`applicant`(255)),
  KEY `idx_lilon_inventor` (`inventor`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_profile` (
  `user_id` bigint NOT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `avatar_url` varchar(512) DEFAULT NULL,
  `real_name` varchar(64) DEFAULT NULL,
  `id_number` varchar(64) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_up_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `organization` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `type` varchar(32) NOT NULL,
  `credit_code` varchar(64) DEFAULT NULL,
  `address` varchar(512) DEFAULT NULL,
  `contact_name` varchar(64) DEFAULT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  `contact_email` varchar(128) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_org_type` (`type`),
  KEY `idx_org_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_organization` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `org_id` bigint NOT NULL,
  `relation_type` varchar(32) NOT NULL DEFAULT 'MEMBER',
  `position_title` varchar(128) DEFAULT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_org` (`user_id`,`org_id`),
  KEY `idx_uo_org` (`org_id`),
  KEY `idx_uo_user` (`user_id`),
  CONSTRAINT `fk_uo_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_uo_org` FOREIGN KEY (`org_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `expert_profile` (
  `user_id` bigint NOT NULL,
  `field` varchar(255) DEFAULT NULL,
  `expertise` text,
  `achievements` text,
  `contact_info` varchar(255) DEFAULT NULL,
  `cert_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `cert_submit_at` datetime(3) DEFAULT NULL,
  `cert_audit_at` datetime(3) DEFAULT NULL,
  `cert_audit_by` bigint DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`),
  KEY `idx_ep_field` (`field`),
  KEY `idx_ep_cert_status` (`cert_status`),
  CONSTRAINT `fk_ep_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_ep_audit_by` FOREIGN KEY (`cert_audit_by`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(128) NOT NULL,
  `name` varchar(255) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `role_permission` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_patent` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_user_id` bigint NOT NULL,
  `category` varchar(32) NOT NULL,
  `public_num` varchar(32) DEFAULT NULL,
  `legal_status` text,
  `latest_legal_status` text,
  `status` text,
  `title` text,
  `type` text,
  `abstract` text,
  `appli_num` text,
  `appli_date` text,
  `public_date` text,
  `applicant` text,
  `applicant_address` text,
  `patentee` text,
  `patentee_address` text,
  `inventor` text,
  `agent` text,
  `IPC` text,
  `CPC` text,
  `NEC` text,
  `patent_details` text,
  `visibility` varchar(16) NOT NULL DEFAULT 'PUBLIC',
  `cooperation_condition` text,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_patent_cat_pub` (`category`,`public_num`),
  KEY `idx_user_patent_owner` (`owner_user_id`),
  KEY `idx_user_patent_category` (`category`),
  KEY `idx_user_patent_visibility` (`visibility`),
  CONSTRAINT `fk_upat_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `requirement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `description` text,
  `keywords` varchar(1024) DEFAULT NULL,
  `tech_direction` varchar(255) DEFAULT NULL,
  `cooperation_mode` varchar(255) DEFAULT NULL,
  `contact_info` varchar(255) DEFAULT NULL,
  `budget` decimal(12,2) DEFAULT NULL,
  `deadline` varchar(64) DEFAULT NULL,
  `requester_user_id` bigint NOT NULL,
  `requester_org_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `created_date` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_req_status` (`status`),
  KEY `idx_req_created_date` (`created_date`),
  KEY `idx_req_requester_user` (`requester_user_id`),
  KEY `idx_req_requester_org` (`requester_org_id`),
  CONSTRAINT `fk_req_user` FOREIGN KEY (`requester_user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_req_org` FOREIGN KEY (`requester_org_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `requirement_patent_match` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `requirement_id` bigint NOT NULL,
  `patent_source` varchar(16) NOT NULL DEFAULT 'EXTERNAL',
  `patent_category` varchar(32) DEFAULT NULL,
  `patent_public_num` varchar(32) DEFAULT NULL,
  `user_patent_id` bigint DEFAULT NULL,
  `match_score` decimal(6,3) DEFAULT NULL,
  `match_reason` text,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_req_patent` (`requirement_id`,`patent_source`,`patent_category`,`patent_public_num`,`user_patent_id`),
  KEY `idx_rpm_req` (`requirement_id`),
  KEY `idx_rpm_patent_ext` (`patent_source`,`patent_category`,`patent_public_num`),
  KEY `idx_rpm_patent_user` (`user_patent_id`),
  KEY `idx_rpm_created_at` (`created_at`),
  CONSTRAINT `fk_rpm_req` FOREIGN KEY (`requirement_id`) REFERENCES `requirement` (`id`),
  CONSTRAINT `fk_rpm_user_patent` FOREIGN KEY (`user_patent_id`) REFERENCES `user_patent` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `requirement_expert_match` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `requirement_id` bigint NOT NULL,
  `expert_user_id` bigint NOT NULL,
  `match_score` decimal(6,3) DEFAULT NULL,
  `match_reason` text,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_req_expert` (`requirement_id`,`expert_user_id`),
  KEY `idx_rem_req` (`requirement_id`),
  KEY `idx_rem_expert_user` (`expert_user_id`),
  KEY `idx_rem_created_at` (`created_at`),
  CONSTRAINT `fk_rem_req` FOREIGN KEY (`requirement_id`) REFERENCES `requirement` (`id`),
  CONSTRAINT `fk_rem_expert_user` FOREIGN KEY (`expert_user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `patent_valuation_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patent_source` varchar(16) NOT NULL DEFAULT 'EXTERNAL',
  `patent_category` varchar(32) DEFAULT NULL,
  `patent_public_num` varchar(32) DEFAULT NULL,
  `user_patent_id` bigint DEFAULT NULL,
  `tech_value_score` decimal(6,3) DEFAULT NULL,
  `market_value_score` decimal(6,3) DEFAULT NULL,
  `transformation_potential_score` decimal(6,3) DEFAULT NULL,
  `overall_score` decimal(6,3) DEFAULT NULL,
  `predicted_value` decimal(18,2) DEFAULT NULL,
  `report_json` json DEFAULT NULL,
  `model_version` varchar(64) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_pvr_patent_ext` (`patent_source`,`patent_category`,`patent_public_num`),
  KEY `idx_pvr_patent_user` (`user_patent_id`),
  KEY `idx_pvr_created_at` (`created_at`),
  KEY `idx_pvr_overall_score` (`overall_score`),
  CONSTRAINT `fk_pvr_user_patent` FOREIGN KEY (`user_patent_id`) REFERENCES `user_patent` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `patent_valuation_model_param` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `param_key` varchar(128) NOT NULL,
  `param_value` text,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pvmp_key` (`param_key`),
  CONSTRAINT `fk_pvmp_user` FOREIGN KEY (`updated_by`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `transformation_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patent_source` varchar(16) NOT NULL DEFAULT 'EXTERNAL',
  `patent_category` varchar(32) DEFAULT NULL,
  `patent_public_num` varchar(32) DEFAULT NULL,
  `user_patent_id` bigint DEFAULT NULL,
  `expert_user_id` bigint DEFAULT NULL,
  `requirement_id` bigint DEFAULT NULL,
  `partner_org_id` bigint DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `transformation_date` date DEFAULT NULL,
  `status` varchar(64) DEFAULT NULL,
  `benefit_amount` decimal(18,2) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_tr_patent_ext` (`patent_source`,`patent_category`,`patent_public_num`),
  KEY `idx_tr_patent_user` (`user_patent_id`),
  KEY `idx_tr_expert_user` (`expert_user_id`),
  KEY `idx_tr_req` (`requirement_id`),
  KEY `idx_tr_status` (`status`),
  KEY `idx_tr_created_at` (`created_at`),
  CONSTRAINT `fk_tr_expert_user` FOREIGN KEY (`expert_user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_tr_req` FOREIGN KEY (`requirement_id`) REFERENCES `requirement` (`id`),
  CONSTRAINT `fk_tr_partner_org` FOREIGN KEY (`partner_org_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_tr_user_patent` FOREIGN KEY (`user_patent_id`) REFERENCES `user_patent` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(64) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `content` text,
  `related_requirement_id` bigint DEFAULT NULL,
  `read_flag` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_un_type` (`type`),
  KEY `idx_un_user_read` (`user_id`,`read_flag`),
  KEY `idx_un_req` (`related_requirement_id`),
  KEY `idx_un_created_at` (`created_at`),
  CONSTRAINT `fk_un_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_un_req` FOREIGN KEY (`related_requirement_id`) REFERENCES `requirement` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `data_sync_job` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_type` varchar(64) NOT NULL,
  `target_category` varchar(32) DEFAULT NULL,
  `source` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `message` text,
  `started_at` datetime(3) DEFAULT NULL,
  `finished_at` datetime(3) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_dsj_job_type` (`job_type`),
  KEY `idx_dsj_target_category` (`target_category`),
  KEY `idx_dsj_status` (`status`),
  KEY `idx_dsj_created_by` (`created_by`),
  CONSTRAINT `fk_dsj_user` FOREIGN KEY (`created_by`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `action` varchar(128) NOT NULL,
  `resource_type` varchar(64) DEFAULT NULL,
  `resource_id` varchar(128) DEFAULT NULL,
  `detail` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_created_at` (`created_at`),
  KEY `idx_audit_resource` (`resource_type`,`resource_id`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_chat_session_user_updated` (`user_id`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(16) NOT NULL,
  `content` longtext NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_chat_msg_session_created` (`session_id`,`created_at`),
  KEY `idx_chat_msg_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

