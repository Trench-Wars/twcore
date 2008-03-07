/*
SQLyog Community Edition- MySQL GUI v6.15
MySQL - 4.1.20-community-nt : Database - twrc
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `tblPointsData` */

CREATE TABLE `tblPointsData` (
  `fldID` int(30) NOT NULL auto_increment,
  `fldName` int(30) NOT NULL default '0',
  `fldPoints` int(30) NOT NULL default '0',
  `fldReason` varchar(255) NOT NULL default '',
  `fldTime` varchar(20) NOT NULL default '',
  PRIMARY KEY  (`fldID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

/*Table structure for table `tblRace` */

CREATE TABLE `tblRace` (
  `fnRaceID` int(11) NOT NULL auto_increment,
  `fcArena` varchar(128) NOT NULL default '',
  `fcName` varchar(128) NOT NULL default '',
  PRIMARY KEY  (`fnRaceID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `tblRaceCheckPoint` */

CREATE TABLE `tblRaceCheckPoint` (
  `fnTrackID` int(11) NOT NULL default '0',
  `fnCheckPoint` int(11) NOT NULL default '0',
  `fnFlagID` int(11) NOT NULL default '0',
  PRIMARY KEY  (`fnTrackID`,`fnCheckPoint`,`fnFlagID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `tblRaceData` */

CREATE TABLE `tblRaceData` (
  `fldID` int(30) NOT NULL auto_increment,
  `fldDate` varchar(20) NOT NULL default '',
  `fldTrack` varchar(20) NOT NULL default '',
  `fldLaps` int(30) NOT NULL default '0',
  `fldStarters` int(30) NOT NULL default '0',
  `fldFinishers` int(30) NOT NULL default '0',
  `fldType` varchar(20) NOT NULL default '',
  `fldFirst` varchar(20) NOT NULL default '',
  `fldSecond` varchar(20) NOT NULL default '',
  `fldThird` varchar(20) NOT NULL default '',
  `fldHost` varchar(20) NOT NULL default '',
  PRIMARY KEY  (`fldID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

/*Table structure for table `tblRaceNews` */

CREATE TABLE `tblRaceNews` (
  `fldNewsID` int(10) NOT NULL auto_increment,
  `fldNewsTitle` varchar(30) NOT NULL default '',
  `fldNews` text NOT NULL,
  `fldPoster` varchar(20) NOT NULL default '',
  `fldDate` date default NULL,
  PRIMARY KEY  (`fldNewsID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

/*Table structure for table `tblRacePits` */

CREATE TABLE `tblRacePits` (
  `fnTrackID` int(11) NOT NULL default '0',
  `fnFlagID` int(11) NOT NULL default '0',
  PRIMARY KEY  (`fnTrackID`,`fnFlagID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `tblRacers` */

CREATE TABLE `tblRacers` (
  `fldID` int(30) NOT NULL auto_increment,
  `fldName` varchar(20) NOT NULL default '',
  `fldPoints` int(10) NOT NULL default '0',
  PRIMARY KEY  (`fldID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

/*Table structure for table `tblRaceTrack` */

CREATE TABLE `tblRaceTrack` (
  `fnTrackID` int(11) NOT NULL auto_increment,
  `fnRaceID` int(11) NOT NULL default '0',
  `fnArenaTrackID` int(11) NOT NULL default '0',
  `fcTrackName` varchar(64) default NULL,
  `fcAllowedShips` varchar(16) default NULL,
  `fnXWarp` int(10) unsigned default '0',
  `fnYWarp` int(10) unsigned default '0',
  PRIMARY KEY  (`fnTrackID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `tblRaceWinners` */

CREATE TABLE `tblRaceWinners` (
  `arena` int(11) NOT NULL default '0',
  `trackWon` int(11) NOT NULL default '0',
  `name` varchar(32) NOT NULL default '',
  PRIMARY KEY  (`arena`,`trackWon`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
