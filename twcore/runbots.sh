#!/bin/bash
while true; do java -Xmx386m -cp bin/twcore.jar:libs/googleapi.jar:libs/tyrus-standalone-client-1.8.3.jar:libs/aim.jar:lib/mysql-connector-java-5.0.7-bin.jar:lib/gson.jar twcore.core.Start bin/setup.cfg; done
