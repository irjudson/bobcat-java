#!/usr/bin/env bash

# run the cognitive radio topology control simulation
alias crtc='java -Xmx1024m -jar cognitive_radio_topology_control/target/cognitive_radio_topology_control-1.0-jar-with-dependencies.jar'
# Run the cognitive radio topology control simulation in debug mode
alias crtcd='java -Xmx1024m -jar cognitive_radio_topology_control/target/cognitive_radio_topology_control-1.0-jar-with-dependencies.jar -d -n 8 -v'
# Run the routing and channel selection simulation
alias rcs='java -Xmx1024m -jar routing_channel_selection/target/routing_channel_selection-1.0-jar-with-dependencies.jar'
# Run the beam scheduling simulation
alias bs='java -Xmx1024m -jar beam_scheduling/target/beam_scheduling-1.0-jar-with-dependencies.jar'
# Run the directional antenna simulation
alias da='java -Xmx1024m -jar directional_antennas/target/directional_antennas-1.0-jar-with-dependencies.jar'
