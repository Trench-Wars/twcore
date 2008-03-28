-- --------------------------------------------------------

--
-- Table structure for table `tblDistensionPlayer`
--

CREATE TABLE `tblDistensionPlayer` (
  `fnID` mediumint(8) unsigned NOT NULL auto_increment,
  `fcName` varchar(25) NOT NULL default '',
  `fnArmyID` smallint(5) unsigned NOT NULL default '0',
  `fnTime` smallint(5) NOT NULL default '0',
  `fnBattlesWon` mediumint(8) unsigned NOT NULL default '0',
  `fcBanned` enum('n','y') NOT NULL default 'n',
  `fcSendKillMsg` enum('n','y') NOT NULL default 'y',
  `fcShip1` enum('n','y') NOT NULL default 'y',
  `fcShip2` enum('n','y') NOT NULL default 'n',
  `fcShip3` enum('n','y') NOT NULL default 'n',
  `fcShip4` enum('n','y') NOT NULL default 'n',
  `fcShip5` enum('n','y') NOT NULL default 'y',
  `fcShip6` enum('n','y') NOT NULL default 'n',
  `fcShip7` enum('n','y') NOT NULL default 'n',
  `fcShip8` enum('n','y') NOT NULL default 'n',
  `fcShip9` enum('n','y') NOT NULL default 'n',
  PRIMARY KEY  (`fnID`),
  UNIQUE KEY `fcName` (`fcName`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COMMENT='Player data for the progressive RPG-style war game, Distensi' AUTO_INCREMENT=640 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblDistensionShip`
--

CREATE TABLE `tblDistensionShip` (
  `fnPlayerID` mediumint(8) unsigned NOT NULL default '0',
  `fnShipNum` tinyint(3) unsigned NOT NULL default '0',
  `fnRank` tinyint(3) unsigned default '0',
  `fnRankPoints` int(11) NOT NULL default '0',
  `fnUpgradePoints` tinyint(3) unsigned default '0',
  `fnStat1` tinyint(3) unsigned NOT NULL default '0',
  `fnStat2` tinyint(3) unsigned NOT NULL default '0',
  `fnStat3` tinyint(3) unsigned NOT NULL default '0',
  `fnStat4` tinyint(3) unsigned NOT NULL default '0',
  `fnStat5` tinyint(3) unsigned NOT NULL default '0',
  `fnStat6` tinyint(3) unsigned NOT NULL default '0',
  `fnStat7` tinyint(3) unsigned NOT NULL default '0',
  `fnStat8` tinyint(3) unsigned NOT NULL default '0',
  `fnStat9` tinyint(3) unsigned NOT NULL default '0',
  `fnStat10` tinyint(3) unsigned NOT NULL default '0',
  `fnStat11` tinyint(3) unsigned NOT NULL default '0',
  `fnStat12` tinyint(3) unsigned NOT NULL default '0',
  `fnStat13` tinyint(3) unsigned NOT NULL default '0',
  `fnStat14` tinyint(3) unsigned NOT NULL default '0',
  `fnStat15` tinyint(3) unsigned NOT NULL default '0',
  PRIMARY KEY  (`fnPlayerID`,`fnShipNum`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COMMENT='Distension (''RPG''-like pub) ship settings';

-- --------------------------------------------------------

--
-- Table structure for table `tblDistensionArmy`
--

CREATE TABLE `tblDistensionArmy` (
  `fnArmyID` mediumint(8) unsigned NOT NULL default '0',
  `fcArmyName` varchar(40) NOT NULL default '',
  `fnNumPilots` smallint(5) unsigned NOT NULL default '0',
  `fcDefaultArmy` enum('y','n') NOT NULL default 'n',
  `fcPrivateArmy` enum('y','n') NOT NULL default 'n',
  `fcPassword` varchar(8) NOT NULL default '',
  PRIMARY KEY  (`fnArmyID`),
  UNIQUE KEY `fnArmyName` (`fcArmyName`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `tblDistensionNews`
--

CREATE TABLE `tblDistensionNews` (
  `fnPostID` mediumint(8) unsigned NOT NULL auto_increment,
  `fcTitle` varchar(40) NOT NULL default '',
  `fdDate` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `ftBody` text NOT NULL,
  `fcAuthorName` varchar(40) NOT NULL default '',
  PRIMARY KEY  (`fnPostID`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=2 ;

-- --------------------------------------------------------

--
-- Table structure for table `tblDistensionUpgrade`
--

CREATE TABLE `tblDistensionUpgrade` (
  `fnShipNum` smallint(5) unsigned NOT NULL,
  `fnUpgradeNum` smallint(5) unsigned NOT NULL,
  `fcDesc` varchar(30) NOT NULL,
  `fnPrizeNum` smallint(6) NOT NULL,
  `fnPointsReq` varchar(50) NOT NULL default '0' COMMENT 'Comma-separated integers',
  `fnRankReq` varchar(50) NOT NULL default '0' COMMENT 'Comma-separated integers',
  KEY `fnShipNum` (`fnShipNum`,`fnUpgradeNum`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COMMENT='Stores static information on each upgrade';
