CREATE TABLE IF NOT EXISTS `home_content` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hero_title` VARCHAR(255) DEFAULT NULL,
  `brand_slogan` VARCHAR(255) DEFAULT NULL,
  `hero_description` TEXT,
  `core_features_title` VARCHAR(255) DEFAULT NULL,
  `core_features_intro` TEXT,
  `hero_metrics_json` TEXT,
  `features_json` TEXT,
  `advantages_json` TEXT,
  `mobile_operation_tips_json` TEXT,
  `mobile_service_promises_json` TEXT,
  `hero_highlights_json` TEXT,
  `case_kpis_json` TEXT,
  `articles_section_title` VARCHAR(255) DEFAULT NULL,
  `articles_section_subtitle` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `cms_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(128) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `summary` TEXT,
  `cover_url` VARCHAR(512) DEFAULT NULL,
  `content_md` LONGTEXT,
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `sort_order` INT NOT NULL DEFAULT 0,
  `published_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cms_article_slug` (`slug`),
  KEY `idx_cms_article_status_sort_created` (`status`, `sort_order`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
