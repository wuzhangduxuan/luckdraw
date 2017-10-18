/*
Navicat MySQL Data Transfer

Source Server         : forex
Source Server Version : 50713
Source Host           : localhost:3306
Source Database       : luckdraw

Target Server Type    : MYSQL
Target Server Version : 50713
File Encoding         : 65001

Date: 2017-10-18 19:01:28
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for prize
-- ----------------------------
DROP TABLE IF EXISTS `prize`;
CREATE TABLE `prize` (
  `prize_level` varchar(10) NOT NULL,
  `prize_size` int(11) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
