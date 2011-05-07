#!/bin/bash
while true; do java -Xmx386m -cp bin/twcore.jar:libs/googleapi.jar:libs/aim.jar:libs/mysql-connector-java-5.0.7-bin.jar twcore.core.Start bin/setup.cfg; done
