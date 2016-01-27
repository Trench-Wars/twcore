CREATE TABLE IF NOT EXISTS `tblPrezFreqWarps` (
  `fnID` smallint(6) NOT NULL AUTO_INCREMENT,
  `fcArena` varchar(15) NOT NULL,
  `fnFreq` smallint(4) NOT NULL,
  `fnX` smallint(6) NOT NULL,
  `fnY` smallint(6) NOT NULL,
  PRIMARY KEY (`fnID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;