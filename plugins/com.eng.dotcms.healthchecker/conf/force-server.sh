#!/bin/bash

service JBossWCMProd stop
sleep 5
sh ./restart_server.sh
exit 0;
