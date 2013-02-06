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
