#!/bin/sh

. /start-common.sh

echo "$(hostname -i) spark-master" >> /etc/hosts

/spark/sbin/start-master.sh --ip spark-master --port 7077
