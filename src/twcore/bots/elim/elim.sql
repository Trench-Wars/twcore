-- phpMyAdmin SQL Dump
-- version 3.3.2deb1ubuntu1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Feb 07, 2012 at 10:01 AM
-- Server version: 5.1.49
-- PHP Version: 5.3.2-1ubuntu4.11

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `trench_TrenchWars`
--

-- --------------------------------------------------------

--
-- Table structure for table `tblElim__Game`
--

CREATE TABLE IF NOT EXISTS `tblElim__Game` (
  `fnGameID` int(15) NOT NULL AUTO_INCREMENT,
  `fnShip` tinyint(10) NOT NULL DEFAULT '1',
  `fcWinner` varchar(40) NOT NULL,
  `fnSpecAt` tinyint(10) NOT NULL DEFAULT '10',
  `fnKills` smallint(10) NOT NULL DEFAULT '0',
  `fnDeaths` smallint(10) NOT NULL DEFAULT '1',
  `fnPlayers` int(10) NOT NULL DEFAULT '2',
  `fnRating` int(10) NOT NULL DEFAULT '300',
  `fdTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fnGameID`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=16828 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblElim__Player`
--

CREATE TABLE IF NOT EXISTS `tblElim__Player` (
  `fnIndex` bigint(20) NOT NULL AUTO_INCREMENT,
  `fnRank` bigint(20) NOT NULL DEFAULT '0',
  `fcName` varchar(30) NOT NULL,
  `fnShip` smallint(1) NOT NULL DEFAULT '1',
  `fnRating` int(5) NOT NULL DEFAULT '300',
  `fnAdjRating` int(5) NOT NULL DEFAULT '300',
  `fnAve` float NOT NULL DEFAULT '300',
  `fnWins` int(10) NOT NULL DEFAULT '0',
  `fnGames` int(10) NOT NULL DEFAULT '0',
  `fnKills` int(10) NOT NULL DEFAULT '0',
  `fnDeaths` int(10) NOT NULL DEFAULT '0',
  `fnShots` int(10) NOT NULL DEFAULT '1',
  `fnAim` decimal(5,2) NOT NULL DEFAULT '100.00',
  `fnKillJoys` int(10) NOT NULL DEFAULT '0',
  `fnKnockOuts` int(10) NOT NULL DEFAULT '0',
  `fnMultiKills` int(10) NOT NULL DEFAULT '0',
  `fnWinStreak` int(5) NOT NULL DEFAULT '0',
  `fnKillStreak` int(5) NOT NULL DEFAULT '0',
  `fnDeathStreak` int(5) NOT NULL DEFAULT '0',
  `fnTopKillStreak` int(5) NOT NULL DEFAULT '0',
  `fnTopDeathStreak` int(5) NOT NULL DEFAULT '0',
  `fnTopWinStreak` int(5) NOT NULL DEFAULT '0',
  `fnTopMultiKill` int(5) NOT NULL DEFAULT '0',
  `ftUpdated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fnIndex`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=8060 ;
