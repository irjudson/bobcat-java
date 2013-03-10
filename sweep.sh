#!/usr/bin/env bash
#

export BOBCAT_HOME="/Users/ivan.judson/Personal/Phd/bobcat"

CPLEXPATH="/opt/ibm/ILOG/CPLEX_Studio_Academic123"
CPLEXPATH="/opt/cplex"
CPLEXARCH="x86_sles10_4.1"
CPLEXARCH="x86-64_sles10_4.1"
export CPLEXARCH="x86-64_darwin9_gcc4.0"
export JLPATH="${CPLEXPATH}/cplex/bin/${CPLEXARCH}:${CPLEXPATH}/lib:${CPLEXPATH}/lib/${CPLEXARCH}"

JAVA_OPTIONS="-Djava.library.path=${JLPATH} -Xmx4096m"
export JAVA_OPTIONS="-Djava.library.path=${JLPATH} -Xmx4096m"

export CRTC="java $JAVA_OPTIONS -jar cognitive_radio_topology_control/target/cognitive_radio_topology_control-1.0-jar-with-dependencies.jar"

SUBSCRIBERS="10 20 30 40 50 60 70 80 90"
SIZE="20000 30000 40000 50000 60000 70000 80000"
# CHANNELS="2 3 4 5 6 7 8 9 10"
APS="5 6 7 8 9"

ITERATIONS="1 2 3 4 5 6 7 8 9 10"

# 840 output files total

MSUB="50"
MSIZ="50000"
# MCHA="6"
MAP="7"

# 90 * 4 output files = 360 
echo -n "Running user simulation"
for s in $SUBSCRIBERS; do
    for i in $ITERATIONS; do
      OUTPUT1="mst.user.$s.$i.out"
      OUTPUT2="mst-uc.user.$s.$i.out"
      OUTPUT3="mst2.user.$s.$i.out"
      OUTPUT4="mst2-uc.user.$s.$i.out"
      while [ ! -e $OUTPUT1 ]; do
        $CRTC -n $s >& $OUTPUT1
        GOOD="`grep 'Couldn' $OUTPUT1`"
        if [ "x$GOOD" != "x" ]; then
          rm $OUTPUT1
        else
          SEED="`tail -1 $OUTPUT1  | awk -F, '{ print $2 }'`"
          $CRTC -k -s $SEED -n $s >& $OUTPUT2
          $CRTC -n $s -s $SEED -b >& $OUTPUT3
          $CRTC -k -n $s -s $SEED -b >& $OUTPUT4
        fi
        echo -n "."
        wait
      done
    done
done
echo "done!"

# 70 * 4 = 280
echo -n "Running size simulation"
for b in $SIZE; do
    for i in $ITERATIONS; do
      OUTPUT1="mst.size.$b.$i.out"
      OUTPUT2="mst-uc.size.$b.$i.out"
      OUTPUT3="mst2.size.$b.$i.out"
      OUTPUT4="mst2-uc.size.$b.$i.out"
      while [ ! -e $OUTPUT1 ]; do
        $CRTC -w $b >& $OUTPUT1
        GOOD="`grep 'Couldn' $OUTPUT1`"
        if [ "x$GOOD" != "x" ]; then
          rm $OUTPUT1
        else
          SEED="`tail -1 $OUTPUT1  | awk -F, '{ print $2 }'`"
          $CRTC -k -w $b -s $SEED >& $OUTPUT2
          $CRTC -w $b -s $SEED -b >& $OUTPUT3
          $CRTC -k -w $b -s $SEED -b >& $OUTPUT4
        fi
        echo -n "."
        wait
      done
    done
done
echo "done!"

# 50 * 4 = 200
echo -n "Running APS simulation"
for k in $APS; do
    for i in $ITERATIONS; do
      OUTPUT1="mst.ap.$k.$i.out"
      OUTPUT2="mst-uc.ap.$k.$i.out"
      OUTPUT3="mst2.ap.$k.$i.out"
      OUTPUT4="mst2-uc.ap.$k.$i.out"
      while [ ! -e $OUTPUT1 ]; do
        $CRTC -a $k >& $OUTPUT1
        GOOD="`grep 'Couldn' $OUTPUT1`"
        if [ "x$GOOD" != "x" ]; then
          rm $OUTPUT1
        else
          SEED="`tail -1 $OUTPUT1  | awk -F, '{ print $2 }'`"
          $CRTC -k -a $k -s $SEED >& $OUTPUT2
          $CRTC -a $k -s $SEED -b >& $OUTPUT3
          $CRTC -k -a $k -s $SEED -b >& $OUTPUT4
        fi
      done
      echo -n "."
      wait
    done
done
echo "done!"

# echo -n "Running channel simulation"
# for k in $CHANNELS; do
#     for i in $ITERATIONS; do
#       OUTPUT="mst.channel.$k.$i.out"
#       OUTPUT2="mst2.channel.$k.$i.out"
#       while [ ! -e $OUTPUT ]; do
#         $CRTC -c $k >& $OUTPUT
#         GOOD="`grep 'Couldn' $OUTPUT`"
#         if [ "x$GOOD" != "x" ]; then
#           rm $OUTPUT
#         else
#           SEED="`tail -1 $OUTPUT  | awk -F, '{ print $2 }'`"
#           $CRTC -c $k -s $SEED -b >& $OUTPUT2
#         fi
#         echo -n "."
#         wait
#       done
#     done
# done
# echo "done!"

exit 0
