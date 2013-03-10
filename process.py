#!/usr/bin/env python
#
#
import sys
import csv
import simplestats
from nested_dict import *

MCHAN = 6
MSIZE = 50000.0
MUSERS = 50
MAP = 9


data = nested_dict(5, list)
data_reader = csv.reader(open(sys.argv[1], 'r'))

for row in data_reader:
    variation, seed, width, height, nodes, users, channels, aps, cost = row

    seed = int(seed)
    width = float(width)
    height = float(height)
    nodes = int(nodes)
    users = int(users)
    channels = int(channels)
    aps = int(aps)
    cost = float(cost)

    data[width][nodes][channels][aps][variation].append(cost)

# Scenario 1: vary size of the network
mst = []
mst2 = []
idxs = []
for width in sorted(data.keys()):
    mst.append(str(simplestats.mean(data[width][MUSERS][MCHAN][0]['mst-vc'])))
    mst2.append(str(simplestats.mean(data[width][MUSERS][MCHAN][0]['mst2-vc'])))
    idxs.append(int(width/1000))

print("""
figure(1);
set(1, \"defaulttextfontname\", \"Times-Roman\");
set(1, \"defaultaxesfontname\", \"Times-Roman\");
set(1, \"defaulttextfontsize\", 19);
set(1, \"defaultaxesfontsize\", 19);

X = %(idx)s;
MST = [ %(mst)s ];
MST2 = [ %(mst2)s ];

plot(X,MST,'r','LineWidth',3);
hold on;
plot(X,MST2,'b','LineWidth',3);

xlabel('Region sidelength (km)');
ylabel('Cost');
legend('MST Routing','MST + Backup', \"Location\", \"SouthEast\")
set(gca, 'XTickMode', 'manual', 'XTick', X);
fixAxes;
hold off;
print -dpdf scenario1-uc.pdf
print -deps scenario1-uc.eps
""" % {'idx': sorted(idxs), 'mst': ", ".join(mst), 'mst2': ", ".join(mst2)})

# Scenario 2: vary # of nodes in {6, 8, 10, 12, 14}
mst = []
mst2 = []
for nodes in sorted(data[MSIZE].keys()):
    mst.append(str(simplestats.mean(data[MSIZE][nodes][MCHAN][0]['mst-vc'])))
    mst2.append(str(simplestats.mean(data[MSIZE][nodes][MCHAN][0]['mst2-vc'])))

print("""
figure(2);
set(2, \"defaulttextfontname\", \"Times-Roman\");
set(2, \"defaultaxesfontname\", \"Times-Roman\");
set(2, \"defaulttextfontsize\", 19);
set(2, \"defaultaxesfontsize\", 19);

X = %(idx)s;
MST = [ %(mst)s ];
MST2 = [ %(mst2)s ];

plot(X,MST,'r','LineWidth',3);
hold on;
plot(X,MST2,'b','LineWidth',3);

xlabel('Number of Nodes');
ylabel('Cost');
legend('MST Routing','MST + Backup', \"Location\", \"SouthEast\")
set(gca, 'XTickMode', 'manual', 'XTick', X);
fixAxes;
hold off;
print -dpdf scenario2-uc.pdf
print -deps scenario2-uc.eps
""" % {'idx': sorted(data[MSIZE].keys()), 'mst': ", ".join(mst), 'mst2': ", ".join(mst2)})

# Scenario 3: vary the number of aps
mst = []
mst2 = []
idxs = []
for aps in sorted(data[MSIZE][MUSERS][MCHAN].keys()):
    if aps in [5, 6, 7, 8, 9, 10, 11, 12, 13]:
        mst.append(str(simplestats.mean(data[MSIZE][MUSERS][MCHAN][aps]['mst-vc'])))
        mst2.append(str(simplestats.mean(data[MSIZE][MUSERS][MCHAN][aps]['mst2-vc'])))
        idxs.append(aps)

print("""
figure(3);
set(3, \"defaulttextfontname\", \"Times-Roman\");
set(3, \"defaultaxesfontname\", \"Times-Roman\");
set(3, \"defaulttextfontsize\", 19);
set(3, \"defaultaxesfontsize\", 19);

X = %(idx)s;
MST = [ %(mst)s ];
MST2 = [ %(mst2)s ];

plot(X,MST,'r','LineWidth',3);
hold on;
plot(X,MST2, 'b','LineWidth',3);

xlabel('Number of Access Points');
ylabel('Cost');
legend('MST Routing','MST + Backup', \"Location\", \"SouthEast\")
set(gca, 'XTickMode', 'manual', 'XTick', X);
fixAxes;
hold off;
print -dpdf scenario3-uc.pdf
print -deps scenario3-uc.eps
""" % {'idx': sorted(idxs), 'mst': ", ".join(mst), 'mst2': ", ".join(mst2)})
