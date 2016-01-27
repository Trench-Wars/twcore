-- phpMyAdmin SQL Dump
-- version 2.11.0
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Feb 11, 2008 at 04:23 AM
-- Server version: 5.0.45
-- PHP Version: 5.2.5

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

-- --------------------------------------------------------

--
-- Table structure for table `tblPointsData`
--

CREATE TABLE `tblPointsData` (
  `fldID` int(30) NOT NULL auto_increment,
  `fldName` int(30) NOT NULL default '0',
  `fldPoints` int(30) NOT NULL default '0',
  `fldReason` varchar(255) NOT NULL default '',
  `fldTime` varchar(20) NOT NULL default '',
  PRIMARY KEY  (`fldID`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblRacers`
--

CREATE TABLE `tblRacers` (
  `fldID` int(30) NOT NULL auto_increment,
  `fldName` varchar(20) NOT NULL default '',
  `fldPoints` int(10) NOT NULL default '0',
  PRIMARY KEY  (`fldID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 ;
