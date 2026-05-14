#!/bin/bash
if [ $# -lt 0 ];then
	echo "Usage: $0 "
	exit 1
fi
source ./vd_global.sh
ROOT=/home/xqfu
#MAINCP="$ROOT/DistEA/DUAForensics.jar:$ROOT/DistTaint3.jar:$ROOT/libs/soot-trunk.jar"
MAINCP="$ROOT/DUA/bin:$ROOT/SEADS.jar:$ROOT/libs/soot-trunk.jar"
SOOTCP=".:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:$subjectloc/dist/classes:$subjectloc/dist/testclasses:$ROOT/SEADS.jar"

suffix="voldemort"

LOGDIR=out-ODDSG
mkdir -p $LOGDIR
logout=$LOGDIR/instrSG-$suffix.out
logerr=$LOGDIR/instrSG-$suffix.err

OUTDIR=$subjectloc/ODDStaticGraph
mkdir -p $OUTDIR

starttime=`date +%s%N | cut -b1-13`

	#-allowphantom \
   	#-duaverbose \
	#-wrapTryCatch \
	#-dumpJimple \
	#-statUncaught \
	#-perthread \
	#-syncnio \
	#-main-class $DRIVERCLASS \
	#-entry:$DRIVERCLASS \
	#-syncnio \
	#-syncnio \
	#-main-class $DRIVERCLASS \
	#-entry:$DRIVERCLASS \
	
java -Xmx400g -ea -cp ${MAINCP} ODD.ODDStaticGraph \
	-w -cp $SOOTCP -p cg verbose:false,implicit-entry:false \
	-p cg.spark verbose:false,on-fly-cg:true,rta:false -f c \
	-d $OUTDIR \
	-brinstr:off -duainstr:off \
	-allowphantom \
	-wrapTryCatch \
            -interCD \
            -interCD \
            -exInterCD \
        -duaverbose   \
        -serializeVTG \
		-dumpFunctionList \
	-slicectxinsens \
          -process-dir $subjectloc/dist/classes \
		  -process-dir $subjectloc/dist/testclasses \
	 1> $logout 2> $logerr

cp $subjectloc/ODDStaticGraph/staticVtg.dat .

stoptime=`date +%s%N | cut -b1-13`
echo "StaticAnalysisTime for $suffix elapsed: " `expr $stoptime - $starttime` milliseconds

echo "Running finished."
exit 0


# hcai vim :set ts=4 tw=4 tws=4

