/*
Navicat MySQL Data Transfer

Source Server         : forex
Source Server Version : 50713
Source Host           : localhost:3306
Source Database       : luckdraw

Target Server Type    : MYSQL
Target Server Version : 50713
File Encoding         : 65001

Date: 2017-10-18 19:01:16
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for draw_log
-- ----------------------------
DROP TABLE IF EXISTS `draw_log`;
CREATE TABLE `draw_log` (
  `draw_log_id` int(11) NOT NULL AUTO_INCREMENT,
  `draw_ip` varchar(15) NOT NULL COMMENT '只允许一个ip抽取一次',
  `draw_content` int(11) DEFAULT NULL COMMENT '抢购结果',
  PRIMARY KEY (`draw_log_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of draw_log
-- ----------------------------
INSERT INTO `draw_log` VALUES ('1', '192.168.31.191', '4');
