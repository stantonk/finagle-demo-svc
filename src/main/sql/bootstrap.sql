CREATE DATABASE IF NOT EXISTS finagle_demo_svc;

CREATE USER 'finagle'@'localhost' IDENTIFIED BY 'finagle';
GRANT ALL PRIVILEGES ON finagle_demo_svc. * TO 'finagle'@'localhost';
FLUSH PRIVILEGES;

USE finagle_demo_svc;
CREATE TABLE IF NOT EXISTS `person` (
    `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
    `first_name` VARCHAR(60) NOT NULL,
    `last_name` VARCHAR(60) NOT NULL,
    `age` TINYINT(11) UNSIGNED NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
