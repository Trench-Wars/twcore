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

  $javadir/java -cp twcore.jar:googleapi.jar:mysql-connector-java-5.0.7-bin.jar:aim.jar:mail.jar:activation.jar twcore.core.Start setup.cfg >> ~/log&
  echo $! > $bothome/$botscript.pid

fi

exit 0
