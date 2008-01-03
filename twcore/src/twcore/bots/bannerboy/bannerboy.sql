-- phpMyAdmin SQL Dump
-- version 2.11.0
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Jan 03, 2008 at 09:05 AM
-- Server version: 5.0.45
-- PHP Version: 5.2.3

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

--
-- Database: `trench_TrenchWars`
--

-- --------------------------------------------------------

--
-- Table structure for table `tblBanner`
--

CREATE TABLE `tblBanner` (
  `fnBannerID` int(10) unsigned NOT NULL auto_increment,
  `fnUserID` int(10) unsigned NOT NULL default '0',
  `fcBanner` varchar(192) NOT NULL default '',
  `fdDateFound` datetime NOT NULL default '0000-00-00 00:00:00',
  `fnArchived` tinyint(4) NOT NULL default '0',
  `fnVotes` int(10) unsigned NOT NULL default '0',
  `fnRating` int(10) unsigned NOT NULL default '0',
  `fnBannerCategoryID` mediumint(8) unsigned NOT NULL default '0',
  `fnDownloads` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`fnBannerID`),
  KEY `fcBanner` (`fcBanner`),
  KEY `fnUserID` (`fnUserID`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=100385 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblWore`
--

CREATE TABLE `tblWore` (
  `fnWoreID` int(10) unsigned NOT NULL auto_increment,
  `fnUserID` int(10) unsigned NOT NULL default '0',
  `fnBannerID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`fnWoreID`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=278575 ;
