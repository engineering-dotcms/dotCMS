#!/bin/bash
LOG_FILE="/var/log/dotcms/logs/restart.log"
echo "Start sleeping 5 sec" >> $LOG_FILE
sleep 5
/usr/bin/sudo /sbin/service JBossWCMProd stop
echo "JBoss stopped..." >> $LOG_FILE
sleep 10
echo "...restarting" >> $LOG_FILE
/bin/bash /usr/local/bin/restart_server.sh
sleep 5
/bin/bash /usr/local/bin/allow-remote-publish.sh
exit 0;
