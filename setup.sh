#!/usr/bin/env bash

# CPLEX Configuration
CPLEXPATH="/opt/ibm/ILOG/CPLEX_Studio_Academic123"
CPLEXPATH="/opt/cplex"
CPLEXARCH="x86_sles10_4.1"
CPLEXARCH="x86-64_sles10_4.1"
export CPLEXARCH="x86-64_darwin9_gcc4.0"

export JLPATH="${CPLEXPATH}/cplex/bin/${CPLEXARCH}:${CPLEXPATH}/lib:${CPLEXPATH}/lib/${CPLEXARCH}"

# Setting Java Options
JAVA_OPTIONS="-Djava.library.path=${JLPATH} -Xmx4096m"
export JAVA_OPTIONS="-Djava.library.path=${JLPATH} -Xmx2048m"

alias java='java $JAVA_OPTIONS'
#CMD="java $JAVA_OPTIONS -jar target/DirectionalAntennas-1.0-jar-with-dependencies.jar"
