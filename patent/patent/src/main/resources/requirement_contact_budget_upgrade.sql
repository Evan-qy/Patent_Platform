ALTER TABLE `requirement`
  ADD COLUMN `contact_info` varchar(255) DEFAULT NULL AFTER `cooperation_mode`,
  ADD COLUMN `budget` decimal(12,2) DEFAULT NULL AFTER `contact_info`,
  ADD COLUMN `deadline` varchar(64) DEFAULT NULL AFTER `budget`;
