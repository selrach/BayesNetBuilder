#!/bin/sh

# -------------------------------------------------------------------------------
# Shell script that starts BayesNetBuilder
# -------------------------------------------------------------------------------

# -------------------------------------------------------------------------------
# Log4j configuration file
# -------------------------------------------------------------------------------
ARGS=" -Dlog4j.configuration=log.properties "

# -------------------------------------------------------------------------------
# Memory extension, specific to Sun java
# -------------------------------------------------------------------------------
ARGS="$ARGS -Xms128M -Xmx1024M -XX:PermSize=128M -XX:MaxPermSize=512"


LOG_CONFIG="./configs"

# -------------------------------------------------------------------------------
# Jar files
# -------------------------------------------------------------------------------

JARS=

for jar in `ls . | grep jar`
do
  if [ -f $jar ]; then
    if [ -z $JARS ]; then
      JARS=$jar
    else
	  JARS="$JARS:$jar"
    fi
  fi
done


for jar in `ls ./lib | grep jar`
do
    if [ -z $JARS ]; then
      JARS="./lib/$jar"
    else
	  JARS="$JARS:./lib/$jar"
    fi
done


# -------------------------------------------------------------------------------
# BayesNetBuilder boot class
# -------------------------------------------------------------------------------
BOOT_CLASS=selrach.bnetbuilder.gui.NetworkBuilder

cmd="java -Djava.library.path=/usr/lib $ARGS -classpath .:$JARS:$LOG_CONFIG $BOOT_CLASS"
echo $cmd
$cmd


