#!/bin/bash
# Note: This file needs to run with superuser/administrative privileges

# Necessary setting to allow ElasticSearch to run within docker
sysctl -w vm.max_map_count=262144

# Start docker components
docker-compose -f docker-compose-dev.yml up -d

# Sleep 10 seconds to help "ensure" postgres database  is up, then check readiness
#sleep 10s

# Wait for PostgreSQL to be ready
#while : ; do
#  docker exec database pg_isready >& /dev/null
#  [[ $? -ne 0 ]] || break
#done
RETRIES=30
until docker exec -i database psql -U openke_user -d openke_db -c "select 1" > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo "Waiting for postgres server to start, $((RETRIES)) remaining attempts..."
    RETRIES=$((RETRIES-=1))
    sleep 2
done

if [ "$RETRIES" -eq "0" ]; then
   echo "Exiting, database not available";
   exit;
fi

# Find yourself
BASEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Create the Collector/system_properties.json doesn't exist
# Note: The collector daemon and web application must be configured to "start" / have the
#       current working directory of $BASEDIR/../Collector
#       They both use this system_properties file - primarily for the database connection as
#       well as to define where data is stored locally
if [ ! -f $BASEDIR/../Collector/system_properties.json ]; then
  cp $BASEDIR/../Collector/system_properties.json.development $BASEDIR/../Collector/system_properties.json
fi

#
# Initialize the database system
# The first script creates all of the tables.  We can just ignore all of these errors if the objects already exist
# The second script checks to see if there are entries already in the system user.  If so, we assume the system has already been initialized
# These commands currently do not require a password as they are assumed to be run on the same machine/server as the
# database server.  If this assumption is not correct, then this script will need to be altered.
cat $BASEDIR/../Collector/sql/database_setup.sql | docker exec -i database psql -U openke_user -d openke_db -f - > /dev/null 2>&1
cat $BASEDIR/../Collector/sql/local_setup_development.sql | docker exec -i database psql -U openke_user -d openke_db -f - > /dev/null 2>&1

echo "Development environment ready"
