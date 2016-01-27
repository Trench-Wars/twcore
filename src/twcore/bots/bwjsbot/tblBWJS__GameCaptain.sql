SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

USE `bots`;

CREATE TABLE `tblBWJS__GameCaptain` (
  `matchID` BIGINT unsigned default NULL,
  `userID` BIGINT unsigned default NULL,
  `team` SMALLINT(1) default 0,
  `startTime` BIGINT default NULL,
  `endTime` BIGINT default NULL,
  KEY  (`matchID`),
  KEY `userID` (`userID`),
  KEY `team` (`team`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 ;