#!/bin/bash

cd /usr/local/collector/config/
java  -Xmx2g -classpath "/usr/local/collector/lib/collector.jar" edu.ncsu.las.collector.Collector 2>&1 | rotatelogs /iscsi/collector_drone/logs/collect.log 86400
