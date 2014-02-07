#!/bin/bash
LOG_FILE="/var/log/dotcms/logs/restart.log"
echo "START: Blocking the remote publish on this machine." >> $LOG_FILE
iptables -A INPUT -p tcp --source-port 8443 -j REJECT
iptables -A INPUT -p tcp --desitnation-port 8443 -j REJECT
echo "END: Blocking the remote publish on this machine." >> $LOG_FILE
sleep 5
exit 0;
