SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

USE `twcore`;

CREATE TABLE `tblBWJS__GamePlayerShipInfo` (
  `matchID` BIGINT unsigned default NULL,
  `userID` BIGINT unsigned default NULL,
  `ship` int(2) unsigned default 0,
  `score` INT unsigned default 0,
  `deaths` INT unsigned default 0,
  `WBkill` INT unsigned default 0,
  `JAVkill` INT unsigned default 0,
  `SPIDkill` INT unsigned default 0,
  `LEVkill` INT unsigned default 0,
  `TERRkill` INT unsigned default 0,
  `WEASkill` INT unsigned default 0,
  `LANCkill` INT unsigned default 0,
  `SHARKkill` INT unsigned default 0,
  `WBteamkill` INT unsigned default 0,
  `JAVteamkill` INT unsigned default 0,
  `SPIDteamkill` INT unsigned default 0,
  `LEVteamkill` INT unsigned default 0,
  `TERRteamkill` INT unsigned default 0,
  `WEASteamkill` INT unsigned default 0,
  `LANCteamkill` INT unsigned default 0,
  `SHARKteamkill` INT unsigned default 0,
  `flagsclaimed` INT unsigned default 0,
  `bullets` INT unsigned default 0,
  `repels` INT unsigned default 0,
  `bombs` INT unsigned default 0,
  `mines` INT unsigned default 0,
  `burst` INT unsigned default 0,
  `playTime` INT unsigned default 0,
  `rating` INT unsigned default 0,

  KEY  (`matchID`),
  KEY `userID` (`userID`),
  KEY `ship` (`ship`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 ;
