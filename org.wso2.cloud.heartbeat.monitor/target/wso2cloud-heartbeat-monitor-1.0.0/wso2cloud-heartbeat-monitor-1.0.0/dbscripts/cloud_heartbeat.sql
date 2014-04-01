-- --------------------------------------------------------
--
-- Create Database: `cloud_heartbeat`
--
CREATE DATABASE cloud_heartbeat;
USE cloud_heartbeat;
-- --------------------------------------------------------

--
-- Table structure for table `FAILURE_DETAIL`
--

CREATE TABLE IF NOT EXISTS `FAILURE_DETAIL` (
  `TIMESTAMP` bigint(20) NOT NULL,
  `SERVICE` varchar(50)  NOT NULL,
  `TEST` varchar(50)  NOT NULL,
  `DETAIL` text ,
  `DATETIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`TIMESTAMP`,`SERVICE`,`TEST`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `LIVE_STATUS`
--

CREATE TABLE IF NOT EXISTS `LIVE_STATUS` (
  `TIMESTAMP` bigint(20) NOT NULL,
  `SERVICE` varchar(50)  NOT NULL,
  `TEST` varchar(50)  NOT NULL,
  `STATUS` tinyint(1) NOT NULL,
  `DATETIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`TIMESTAMP`,`SERVICE`,`TEST`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `HISTORY_NOTES`
--

CREATE TABLE IF NOT EXISTS `HISTORY_NOTES` (
  `SERVICE` varchar(50) NOT NULL,
  `DATETIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `NOTE` text NOT NULL,
  PRIMARY KEY (`SERVICE`,`DATETIME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `HISTORY`
-- summarized tests' data
--

CREATE TABLE IF NOT EXISTS `HISTORY` (
  `SERVICE` varchar(50) NOT NULL,
  `DATE` date NOT NULL,
  `STATUS` enum('NORMAL','DISRUPTION','DOWN','NA') NOT NULL,
  KEY `SERVICE` (`SERVICE`,`DATE`),
  KEY `DATE` (`DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

