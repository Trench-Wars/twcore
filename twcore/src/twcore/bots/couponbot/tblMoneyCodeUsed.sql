CREATE TABLE IF NOT EXISTS `tblMoneyCodeUsed` (
  `fnMoneyCodeUserId` int(11) NOT NULL AUTO_INCREMENT,
  `fnMoneyCodeId` int(11) NOT NULL,
  `fcName` varchar(23) NOT NULL COMMENT 'Who used this code',
  `fbCancelled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1 if code cancelled (money must has been removed if cancelled)',
  `fdCreated` datetime NOT NULL,
  `ftUpdated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fnMoneyCodeUserId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;