#!/bin/bash
LOG_FILE="/var/log/dotcms/logs/restart.log"
echo "START: Allowing the remote publish on this machine." >> $LOG_FILE
iptables -D INPUT -p tcp --source-port 8443 -j ACCEPT
iptables -D INPUT -p tcp --desitnation-port 8443 -j ACCEPT
echo "END: Allowing the remote publish on this machine." >> $LOG_FILE
sleep 5
exit 0;
