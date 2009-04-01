-- phpMyAdmin SQL Dump
-- version 2.11.0
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Apr 01, 2009 at 05:24 PM
-- Server version: 5.0.45
-- PHP Version: 5.2.8

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

-- --------------------------------------------------------

--
-- Table structure for table `tblElimCasualRecs`
--

CREATE TABLE `tblElimCasualRecs` (
  `fnIndex` mediumint(50) NOT NULL auto_increment,
  `fcUserName` varchar(40) default NULL,
  `fnKills` int(10) NOT NULL default '0',
  `fnDeaths` int(10) NOT NULL default '0',
  `fnGameType` int(10) NOT NULL default '0',
  PRIMARY KEY  (`fnIndex`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=13989 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblElimGame`
--

CREATE TABLE `tblElimGame` (
  `fnGameID` mediumint(9) NOT NULL auto_increment,
  `fnGameType` tinyint(4) NOT NULL default '0',
  `fcWinnerName` varchar(80) NOT NULL default '0',
  `fnWinnerKills` int(10) NOT NULL default '0',
  `fnWinnerDeaths` int(10) NOT NULL default '0',
  `fnShipType` tinyint(10) NOT NULL default '0',
  `fnDeaths` tinyint(10) NOT NULL default '0',
  `fnNumPlayers` int(10) NOT NULL default '0',
  `fnAvgRating` int(10) NOT NULL default '0',
  `fdPlayed` datetime NOT NULL default '0000-00-00 00:00:00',
  PRIMARY KEY  (`fnGameID`),
  KEY `fdGamePlayed` (`fcWinnerName`),
  KEY `fnGameTypeID` (`fnGameType`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 PACK_KEYS=0 AUTO_INCREMENT=22946 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblElimJavsRecs`
--

CREATE TABLE `tblElimJavsRecs` (
  `fnIndex` bigint(80) NOT NULL auto_increment,
  `fnUserID` mediumint(40) NOT NULL,
  `fnKills` int(10) NOT NULL,
  `fnDeaths` int(10) NOT NULL,
  PRIMARY KEY  (`fnIndex`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COMMENT='Rows are deleted as players draw information.' AUTO_INCREMENT=76260 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblElimPlayer`
--

CREATE TABLE `tblElimPlayer` (
  `fnIndex` bigint(20) NOT NULL auto_increment,
  `fnRank` bigint(20) NOT NULL default '0',
  `fcUserName` varchar(50) NOT NULL default '',
  `fnGameType` int(3) NOT NULL default '0',
  `fnRating` int(5) NOT NULL default '0',
  `fnGamesWon` int(10) NOT NULL default '0',
  `fnGamesPlayed` int(10) NOT NULL default '0',
  `fnKills` int(10) NOT NULL default '0',
  `fnDeaths` int(10) NOT NULL default '0',
  `fnShots` int(10) NOT NULL default '1',
  `fnSB` int(10) NOT NULL default '0',
  `fnPE` int(10) NOT NULL default '0',
  `fnDK` int(10) NOT NULL default '0',
  `fnAve` int(5) NOT NULL default '300',
  `fnAim` decimal(5,2) NOT NULL default '100.00',
  `fnCKS` int(5) NOT NULL default '0',
  `fnCLS` int(5) NOT NULL default '0',
  `fnCWS` int(5) NOT NULL default '0',
  `fnBKS` int(5) NOT NULL default '0',
  `fnWLS` int(5) NOT NULL default '0',
  `fnBWS` int(5) NOT NULL default '0',
  `fnBDK` int(5) NOT NULL default '0',
  `fnElim` int(3) NOT NULL default '0',
  `fnSpecWhenOut` int(3) NOT NULL default '0',
  `ftUpdated` datetime NOT NULL default '0000-00-00 00:00:00',
  PRIMARY KEY  (`fnIndex`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=5611 ;
