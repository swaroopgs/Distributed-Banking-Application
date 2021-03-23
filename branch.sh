#!/bin/bash +vx
LIB_PATH=$"lib/protobuf-java-3.7.0.jar"
#port
java -classpath bin:$LIB_PATH Branch $1 $2 $3
