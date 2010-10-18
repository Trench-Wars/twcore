CREATE TABLE IF NOT EXISTS `tblMoneyCode` (
  `fnMoneyCodeId` int(11) NOT NULL AUTO_INCREMENT,
  `fcCode` varchar(32) NOT NULL,
  `fcDescription` varchar(255) NOT NULL,
  `fnMoney` int(11) NOT NULL,
  `fcCreatedBy` varchar(23) NOT NULL,
  `fnUsed` int(11) NOT NULL DEFAULT '0',
  `fnMaxUsed` int(11) NOT NULL DEFAULT '1',
  `fdStartAt` datetime NOT NULL,
  `fdEndAt` datetime NOT NULL,
  `fbEnabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=not action, 1=active',
  `fdCreated` datetime NOT NULL,
  `ftUpdated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fnMoneyCodeId`),
  UNIQUE KEY `fcCode` (`fcCode`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;
