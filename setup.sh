#!/usr/bin/env bash

export BOBCAT_HOME="/Users/judson/Old/Personal/Phd/bobcat"

# CPLEX Configuration
CPLEXPATH="/opt/IBM/ILOG/CPLEX_Studio125"
export CPLEXARCH="x86_darwin"

export JLPATH="${CPLEXPATH}/cplex/bin/${CPLEXARCH}:${CPLEXPATH}/lib:${CPLEXPATH}/lib/${CPLEXARCH}"

export JAVA_OPTIONS="-Djava.library.path=${JLPATH} -Xmx1024m"

alias java='java $JAVA_OPTIONS'

# run the cognitive radio topology control simulation
alias crtc='cd $BOBCAT_HOME; java -jar cognitive_radio_topology_control/target/cognitive_radio_topology_control-1.0-jar-with-dependencies.jar'
# Run the cognitive radio topology control simulation in debug mode
alias crtcd='cd $BOBCAT_HOME; java -jar cognitive_radio_topology_control/target/cognitive_radio_topology_control-1.0-jar-with-dependencies.jar -d -n 8 -v'
# Run the routing and channel selection simulation
alias rcs='cd $BOBCAT_HOME; java -jar routing_channel_selection/target/routing_channel_selection-1.0-jar-with-dependencies.jar'
# Run the beam scheduling simulation
alias bs='cd $BOBCAT_HOME; java -jar beam_scheduling/target/beam_scheduling-1.0-jar-with-dependencies.jar'
# Run the directional antenna simulation
alias da='cd $BOBCAT_HOME; java -jar directional_antennas/target/directional_antennas-1.0-jar-with-dependencies.jar'
