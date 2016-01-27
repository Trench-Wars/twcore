SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

USE `bots`;

CREATE TABLE `tblBWJS__GamePlayer` (
  `matchID` BIGINT unsigned default NULL,
  `userID` BIGINT unsigned default NULL,
  `team` SMALLINT(1) default 0,
  `lagouts` INT(2) default 0,
  `MVP` SMALLINT(1) default 0,
  `status` VARCHAR(35) default NULL,
  KEY  (`matchID`),
  KEY `userID` (`userID`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 ;
