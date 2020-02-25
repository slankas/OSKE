#!/bin/bash


# Sleep 15 seconds to help "ensure" postgres database  is up
sleep 15s

BASE_DIR=/opt/collector
CONFIG_DIR=$BASE_DIR/config
COLLECTOR_CLASSPATH=$BASE_DIR'/lib/*:'$BASE_DIR/collector.jar

cd $CONFIG_DIR

#
# Initialize the database system
# The first script creates all of the tables.  We can just ignore all of these errors if the objects already exist
# The second script checks to see if there are entries already in the system user.  If so, we assume the system has already been initialized
export PGPASSWORD=MUST_CHANGE_PASSWORD
psql -U openke_user -h database -d openke_db -f database_setup.sql
psql -U openke_user -h database -d openke_db -f local_setup.sql

COLLECTOR_CLASSPATH=$BASE_DIR'/lib/*:'$BASE_DIR/collector.jar

java -Djava.util.logging.config.file=./logging.properties -Dlogback.configurationFile=./logging.xml -Xmx8g -classpath $COLLECTOR_CLASSPATH edu.ncsu.las.collector.JobCollector 
