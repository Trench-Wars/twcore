#!/bin/sh
# Author: lnx, 2005
#
# A simple script to that can be used to 
#  a) make it easier to update from CVS, or 
#  b) automatize the update from CVS
#

cvspath='/home/bots/cvs/bin/'
cvs=$cvspath'cvs -d :pserver:twcore_read@zux.sjr.fi:/twcore'

case "$1" in
     login)
                $cvs login
            ;;
     checkout)
                cd ~/
                $cvs checkout twcore
            ;;
     *)
             echo "[login|checkout]"   
             ;;
esac
