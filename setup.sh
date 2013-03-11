#!/usr/bin/env bash

# CPLEX Configuration
CPLEXPATH="/opt/IBM/ILOG/CPLEX_Studio125"
export CPLEXARCH="x86_win32"

export JLPATH="${CPLEXPATH}/cplex/bin/${CPLEXARCH}:${CPLEXPATH}/lib:${CPLEXPATH}/lib/${CPLEXARCH}"

# Setting Java Options
JAVA_OPTIONS="-Djava.library.path=${JLPATH}"
export JAVA_OPTIONS

alias java='java -d32 -Djava.library.path=${JLPATH}'

CPLEXPATH="/opt/IBM/ILOG/CPLEX_Studio125"
export CPLEXARCH="x86_win32"
export JLPATH="${CPLEXPATH}/cplex/bin/${CPLEXARCH}/:${CPLEXPATH}/cplex/lib/:${CPLEXPATH}/lib/${CPLEXARCH}/"

export JAVA="java -d32 -Djava.library.path=${JLPATH}"

# run the cognitive radio topology control simulation
alias crtc='$JAVA -jar cognitive_radio_topology_control/target/cognitive_radio_topology_control-1.0-jar-with-dependencies.jar'
# Run the cognitive radio topology control simulation in debug mode
alias crtcd='$JAVA -jar cognitive_radio_topology_control/target/cognitive_radio_topology_control-1.0-jar-with-dependencies.jar -d -n 8 -v'
# Run the routing and channel selection simulation
alias rcs='$JAVA -jar routing_channel_selection/target/routing_channel_selection-1.0-jar-with-dependencies.jar'
# Run the beam scheduling simulation
alias bs='$JAVA -jar beam_scheduling/target/beam_scheduling-1.0-jar-with-dependencies.jar'
# Run the directional antenna simulation
alias da='$JAVA -jar directional_antennas/target/directional_antennas-1.0-jar-with-dependencies.jar'
