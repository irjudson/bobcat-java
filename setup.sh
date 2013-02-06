#!/usr/bin/env bash

export BOBCAT_HOME="/Users/ivan.judson/Personal/Phd/bobcat"

# CPLEX Configuration
CPLEXPATH="/opt/ibm/ILOG/CPLEX_Studio_Academic123"
CPLEXPATH="/opt/cplex"
CPLEXARCH="x86_sles10_4.1"
CPLEXARCH="x86-64_sles10_4.1"
export CPLEXARCH="x86-64_darwin9_gcc4.0"

export JLPATH="${CPLEXPATH}/cplex/bin/${CPLEXARCH}:${CPLEXPATH}/lib:${CPLEXPATH}/lib/${CPLEXARCH}"

# Setting Java Options
JAVA_OPTIONS="-Djava.library.path=${JLPATH} -Xmx4096m"
export JAVA_OPTIONS="-Djava.library.path=${JLPATH} -Xmx4096m"

alias java='java $JAVA_OPTIONS'

# run the cognitive radio topology control simulation
alias crtc='cd $BOBCAT_HOME; java -jar whitespace_short_circuit/target/whitespace_short_circuit-1.0-jar-with-dependencies.jar'
# Run the cognitive radio topology control simulation in debug mode
alias crtcd='cd $BOBCAT_HOME; java -jar whitespace_short_circuit/target/whitespace_short_circuit-1.0-jar-with-dependencies.jar -d -n 8 -v'
# Run the routing and channel selection simulation
alias rcs='cd $BOBCAT_HOME; java -jar routing_channel_selection/target/routing_channel_selection-1.0-jar-with-dependencies.jar'
# Run the beam scheduling simulation
alias bs='cd $BOBCAT_HOME; java -jar beam_scheduling/target/beam_scheduling-1.0-jar-with-dependencies.jar'
# Run the directional antenna simulation
alias da='cd $BOBCAT_HOME; java -jar directional_antennas/target/directional_antennas-1.0-jar-with-dependencies.jar'
