#!/usr/bin/env sh

SKUNK_CP=""

add_to_cp()
{
    if [ -n "$1" ]
    then
	SKUNK_CP="$SKUNK_CP${SKUNK_CP:+:}${1}"
    fi
}

skunk_dir=$(dirname "$0")/../..

LOG4J_PATH="${skunk_dir}"/metricminer2/lib/apache-log4j-2.6-bin

# commons-csv
add_to_cp "${skunk_dir}"/Skunk/VARISCAN/lib/commons-csv-1.1.jar

# commons-io
add_to_cp "${skunk_dir}"/Skunk/VARISCAN/lib/commons-io-2.4.jar

# xmlpull
add_to_cp "${skunk_dir}"/Skunk/VARISCAN/lib/xmlpull-1.1.3.1.jar

# xpp3_min
add_to_cp "${skunk_dir}"/Skunk/VARISCAN/lib/xpp3_min-1.1.4c.jar

# xstream
add_to_cp "${skunk_dir}"/Skunk/VARISCAN/lib/xstream-1.4.8.jar

# commons-cli
add_to_cp "${skunk_dir}"/Skunk/VARISCAN/lib/commons-cli-1.3.1/commons-cli-1.3.1.jar

# log4j-api
add_to_cp $LOG4J_PATH/log4j-api-2.6.jar

# log4j-core
add_to_cp $LOG4J_PATH/log4j-core-2.6.jar

# log4j-1.2-api
add_to_cp $LOG4J_PATH/log4j-1.2-api-2.6.jar

# Skunk classes
add_to_cp "${skunk_dir}"/Skunk/VARISCAN/bin

exec java -cp $SKUNK_CP de.ovgu.skunk.detection.main.Skunk "$@"