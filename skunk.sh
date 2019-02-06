#!/usr/bin/env sh

CP=""

add_to_cp()
{
    if [ -n "$1" ]
    then
        if [ ! -e "$1" ]
        then
            echo "Warn: file not found: \`$1'" >&2
        fi
        CP="$CP${CP:+:}${1}"
    fi
}

real_me=$(realpath -- "$0")
me_dir=$(dirname -- "${real_me}")
maven_repo=$HOME/.m2/repository
o_jvm="-Xmx8g"

# commons-cli
add_to_cp "${maven_repo}"/commons-cli/commons-cli/1.3/commons-cli-1.3.jar

# commons-csv
add_to_cp "${maven_repo}"/org/apache/commons/commons-csv/1.4/commons-csv-1.4.jar

# commons-io
add_to_cp "${maven_repo}"/commons-io/commons-io/2.4/commons-io-2.4.jar

# xmlpull
add_to_cp "${maven_repo}"/xmlpull/xmlpull/1.1.3.1/xmlpull-1.1.3.1.jar

# xpp3_min
add_to_cp "${maven_repo}"/xpp3/xpp3_min/1.1.4c/xpp3_min-1.1.4c.jar

# xstream
add_to_cp "${maven_repo}"/com/thoughtworks/xstream/xstream/1.4.9/xstream-1.4.9.jar

# Log4j
add_to_cp "${maven_repo}"/log4j/log4j/1.2.14/log4j-1.2.14.jar
#add_to_cp "${maven_repo}"/org/apache/logging/log4j/log4j-api/2.1/log4j-api-2.1.jar

# Skunk class files
add_to_cp "${me_dir}"/target/classes

exec java ${o_jvm} -cp "$CP" de.ovgu.skunk.detection.main.Skunk "$@"
