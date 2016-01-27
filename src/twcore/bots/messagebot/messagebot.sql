-- phpMyAdmin SQL Dump
-- version 2.11.0
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Dec 23, 2007 at 02:47 PM
-- Server version: 5.0.45
-- PHP Version: 5.2.3

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

--
-- Database: `trench_TrenchWars`
--

-- --------------------------------------------------------

--
-- Table structure for table `tblBotNews`
--

CREATE TABLE `tblBotNews` (
  `fnID` int(10) NOT NULL auto_increment,
  `fcName` varchar(30) NOT NULL default '',
  `fcNews` varchar(250) NOT NULL default '',
  `fdTime` date NOT NULL default '0000-00-00',
  `fcURL` varchar(100) NOT NULL default '',
  PRIMARY KEY  (`fnID`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COMMENT='News blips to be displayed by MessageBot' AUTO_INCREMENT=35 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblChannel`
--

CREATE TABLE `tblChannel` (
  `fnChannelID` int(5) NOT NULL auto_increment,
  `fcChannelName` varchar(30) NOT NULL default '',
  `fcOwner` varchar(30) NOT NULL default '',
  `fnPrivate` int(5) NOT NULL default '0',
  PRIMARY KEY  (`fnChannelID`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=145 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblChannelUser`
--

CREATE TABLE `tblChannelUser` (
  `fnChannelID` int(5) NOT NULL default '0',
  `fcChannel` varchar(30) NOT NULL default '',
  `fcName` varchar(30) NOT NULL default '',
  `fnLevel` int(10) NOT NULL default '0',
  KEY `fnChannelID` (`fnChannelID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `tblMessageBotIgnore`
--

CREATE TABLE `tblMessageBotIgnore` (
  `fcIgnorer` varchar(30) NOT NULL default '',
  `fcIgnoree` varchar(30) NOT NULL default ''
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `tblMessageSystem`
--

CREATE TABLE `tblMessageSystem` (
  `fnID` int(20) NOT NULL auto_increment,
  `fcName` varchar(30) NOT NULL default '',
  `fcMessage` varchar(255) NOT NULL default '',
  `fcSender` varchar(30) NOT NULL default '',
  `fnRead` int(5) NOT NULL default '0',
  `fdTimeStamp` datetime NOT NULL default '0000-00-00 00:00:00',
  PRIMARY KEY  (`fnID`),
  KEY `fcName` (`fcName`,`fnRead`),
  KEY `fcSender` (`fcSender`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=164435 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblMessageToBot`
--

CREATE TABLE `tblMessageToBot` (
  `fnID` int(6) unsigned NOT NULL auto_increment,
  `fcSyncData` varchar(75) NOT NULL default '',
  PRIMARY KEY  (`fnID`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1627 ;
