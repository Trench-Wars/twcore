SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

USE `twcore`;
CREATE TABLE `tblBWJS__Game` (
  `matchID` BIGINT unsigned NOT NULL auto_increment,
  `timeStarted` datetime default NULL,
  `timeEnded` datetime default NULL,
  `type` varchar(35) NULL,
  `winner` SMALLINT(1) default NULL,
  PRIMARY KEY  (`matchID`),
  KEY `type` (`type`),
  KEY `timeStarted` (`timeStarted`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;
