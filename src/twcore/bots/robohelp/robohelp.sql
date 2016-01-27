-- phpMyAdmin SQL Dump
-- version 2.6.1-rc1
-- http://www.phpmyadmin.net
-- 
-- Generation Time: Jul 28, 2007 at 06:17 PM
-- Server version: 4.1.14
-- PHP Version: 5.1.6
-- 

-- --------------------------------------------------------

-- 
-- Table structure for table `tblAdvert`
-- 

CREATE TABLE `tblAdvert` (
  `fnAdvertID` int(11) NOT NULL auto_increment,
  `fcUserName` varchar(30) NOT NULL default '',
  `fcEventName` varchar(50) NOT NULL default '',
  `fcAdvert` varchar(255) NOT NULL default '',
  `fdTime` datetime NOT NULL default '0000-00-00 00:00:00',
  PRIMARY KEY  (`fnAdvertID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

-- 
-- Table structure for table `tblCall`
-- 

CREATE TABLE `tblCall` (
  `fnCallID` int(11) unsigned NOT NULL auto_increment,
  `fcUserName` varchar(30) NOT NULL default '',
  `fnCount` int(11) NOT NULL default '0',
  `fdDate` date NOT NULL default '0000-00-00',
  `fnType` tinyint(1) NOT NULL default '0',
  `ftUpdated` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`fnCallID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

-- 
-- Table structure for table `tblWarnings`
-- 

CREATE TABLE `tblWarnings` (
  `name` varchar(30) NOT NULL default '',
  `warning` varchar(255) NOT NULL default '',
  `timeofwarning` date NOT NULL default '0000-00-00',
  `staffmember` varchar(30) default NULL,
  PRIMARY KEY  (`warning`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
        