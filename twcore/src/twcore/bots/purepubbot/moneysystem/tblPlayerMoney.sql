CREATE TABLE IF NOT EXISTS `tblPlayerMoney` (
  `fcName` varchar(23) NOT NULL,
  `fnMoney` int(11) NOT NULL,
  `ftUpdated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fcName`),
  KEY `fnMoney` (`fnMoney`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;