> **Historical Project — PhD Thesis Work** — Wireless networking simulation toolkit (Java). Part of Ivan R. Judson's doctoral research. Preserved as a research reference.

# Bobcat (Java)

Wireless networking simulation toolkit written in Java. Developed to support doctoral thesis research on directional antenna networks and beam scheduling algorithms. The Python version lives in [bobcat](https://github.com/irjudson/bobcat).

bobcat
======

Wireless Networking Simulation Toolkit

to build this:

git clone the repo
install/configure cplex
mvn2 package

to use this:

- Install CPLEX 
- Edit the setup.sh to indicate the path to the code and the path to CPLEX
- . ./setup.sh
- mvn2 package (to build it)
- run 'wscd' for verbose debugging


 Yet another thing we could possibly consider is a second backup path.  
 Another would be to pick a root node r (gateway) and provide capacity to a set of subnodes {s1,..sk} (say we have a 2-hop WMN).
 We could then find the cheapest way to provision all of the (r,si) connections.