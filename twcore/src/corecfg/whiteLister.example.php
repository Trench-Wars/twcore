<?php
$user = "user";
$pass = "pass";
$database = "database";

//Run this script after updating racism.cfg
$db = mysql_connect ("localhost", $user, $pass);
mysql_select_db($database, $db);
$handle = fopen("racism.cfg", 'r');
if ($handle){
    while (!feof($handle)) {
		$word = fgets($handle, 4096);
		if($word == "")continue;
		$query = "SELECT * FROM tblBoggle_Dict WHERE entry LIKE '%".trim(strtolower($word))."%' AND entry != '%".trim(strtolower($word))."%'";
		$result = mysql_query($query) or die(mysql_error());
		if(mysql_num_rows($result) == 0)
			echo "Nothing found for ".trim($word).".\n";
		while($row = mysql_fetch_assoc($result)){
			$result2 = mysql_query("SELECT fcWord FROM tblWhiteList WHERE fcWord = '".trim(strtolower($row['entry']))."'") or die(mysql_error());
			if(mysql_num_rows($result2) == 0){
				echo "Inserting ".$row['entry']."\n";
				mysql_query("INSERT INTO tblWhiteList (fcWord) VALUES ('".trim(strtolower($row['entry']))."')") or die(mysql_error());
			}
		}
    }
    fclose($handle);
}
?>