#!/bin/sh
# Authors: lnx & Sphonk
#
# Check whether the core is running or not. If not, start it.
#
# Crontab entry to run the check every 5 minute:
#    */5 * * * *   /home/bots/botcheck.sh >/dev/null 2>&1
#

bothome="/home/bots"
botdir="/home/bots/twbots"
botscript="Start"
javadir="/home/bots/java/current/bin"

botpid=`cat $bothome/$botscript.pid`

cd $botdir

if [ "$(ps x|grep $botpid|grep -v grep)" = "" ]; then

  $javadir/java -cp twcore.jar:twcore/misc/googleapi.jar:twcore/misc/mysql-connector-java-3.1.7-bin.jar twcore.core.Start setup.cfg >> ~/log&
  echo $! > $bothome/$botscript.pid

fi

exit 0
