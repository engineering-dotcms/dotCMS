#!/bin/bash
LOG_FILE="/var/log/dotcms/logs/restart.log"
echo "I'm in restart..." >> $LOG_FILE
/usr/bin/sudo /sbin/service JBossWCMProd start < /usr/local/bin/parameters >> $LOG_FILE
sleep 10
echo "Done!" >> $LOG_FILE
exit 0;
