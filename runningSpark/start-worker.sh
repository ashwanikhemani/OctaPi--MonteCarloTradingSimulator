#!/bin/sh

. /start-common.sh

/spark/sbin/start-slave.sh spark://spark-master:7077
