-- phpMyAdmin SQL Dump
-- version 2.8.0.1
-- http://www.phpmyadmin.net
-- 
-- Host: localhost
-- Generation Time: Jan 22, 2007 at 08:46 AM
-- Server version: 4.1.11
-- PHP Version: 5.1.6
-- 
-- Triviabot SQL
-- This is a SQL dump with 11 sample questions for use with the triviabot
-- 

-- --------------------------------------------------------

-- 
-- Table structure for table `tblQuestion`
-- 

CREATE TABLE `tblQuestion` (
  `fnQuestionID` int(10) unsigned NOT NULL auto_increment,
  `fnQuestionTypeID` mediumint(9) NOT NULL default '0',
  `fcQuestion` varchar(255) NOT NULL default '',
  `fcAnswer` varchar(255) NOT NULL default '',
  `fnTimesUsed` int(11) NOT NULL default '0',
  PRIMARY KEY  (`fnQuestionID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=11 ;

-- 
-- Dumping data for table `tblQuestion`
-- 

INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (1, 1, 'Who was the first emperor of the Roman Empire?', 'August', 9);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (2, 5, 'What is the Capital of Belarus?', 'Minsk', 9);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (3, 5, 'When you walk down the famous Champs Elysees, in what city will you find yourself?', 'Paris', 9);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (4, 5, 'What U.S. island has a park called \\''The Battery\\'' at its southern tip?', 'Manhattan Island|Manhattan', 9);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (5, 5, 'How many Provinces (States) does Canada have?', 'ten|10', 9);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (6, 5, 'What body of water separates Turkey allowing this country to be on two continents, Europe and Asia?', 'Bosporus Strait', 9);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (7, 5, 'Of what Country is Oslo the Capital?', 'Norway', 9);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (8, 5, 'The Tiber river flows through which City?', 'Rome', 9);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (9, 5, 'The Canary Basin is located in which Ocean?', 'Atlantic Ocean|Atlantic', 8);
INSERT INTO `tblQuestion` (`fnQuestionID`, `fnQuestionTypeID`, `fcQuestion`, `fcAnswer`, `fnTimesUsed`) VALUES (10, 1, 'In what year did the Soviet Union explode its first atomic bomb?', '1949', 9);

-- --------------------------------------------------------

-- 
-- Table structure for table `tblQuestionType`
-- 

CREATE TABLE `tblQuestionType` (
  `fnQuestionTypeID` mediumint(8) unsigned NOT NULL auto_increment,
  `fcQuestionTypeName` varchar(50) NOT NULL default '',
  `fnNumberUsed` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`fnQuestionTypeID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=19 ;

-- 
-- Dumping data for table `tblQuestionType`
-- 

INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (1, 'History', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (2, 'Sports', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (3, 'Subspace', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (4, 'Current Events', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (5, 'Geography', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (6, 'Movies/TV', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (7, 'Music', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (8, 'General', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (9, 'Science', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (10, 'Religions and Mythology', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (11, 'The Simpsons', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (12, 'Literature', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (13, 'Art', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (14, 'Popular Culture', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (15, 'Popular Culture', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (16, 'Pop Culture', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (17, 'Pop Culture', 0);
INSERT INTO `tblQuestionType` (`fnQuestionTypeID`, `fcQuestionTypeName`, `fnNumberUsed`) VALUES (18, 'Pop Culture', 0);

-- --------------------------------------------------------

-- 
-- Table structure for table `tblUserTriviaStats`
-- 

CREATE TABLE IF NOT EXISTS `tblUserTriviaStats` (
  `fcUserName` varchar(35) NOT NULL DEFAULT '',
  `fnPoints` int(11) DEFAULT NULL,
  `fnPlayed` int(11) DEFAULT NULL,
  `fnWon` int(11) DEFAULT NULL,
  `fnPossible` int(11) DEFAULT NULL,
  `fnRating` int(11) DEFAULT NULL,
  PRIMARY KEY (`fcUserName`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
