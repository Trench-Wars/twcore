SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

USE `twcore`;

CREATE TABLE `tblBWJS__Player` (
  `userID` BIGINT unsigned NOT NULL auto_increment,
  `playerName` varchar(35) NOT NULL default '',
  PRIMARY KEY  (`userID`),
  KEY `playerName` (`playerName`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;