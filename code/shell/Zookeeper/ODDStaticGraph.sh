#!/bin/bash
if [ $# -lt 0 ];then
	echo "Usage: $0 "
	exit 1
fi

source ./zk_global.sh
ROOT=/home/xqfu/
#DRIVERCLASS=ChatServer.core.MainServer
#subjectloc=$subjectloc/


#MAINCP=".:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:/home/xqfu/libs/soot-trunk8.jar:/home/xqfu/libs/DUA3.jar:/home/xqfu/libs/mcia8.jar:$$subjectloc/dist/classes"
# MAINCP=".:/home/xqfu/libs/soot-trunk.jar:/home/xqfu/DUA/bin:/home/xqfu/SEADS.jar"
MAINCP=".:$ROOT/SEADS.jar:$ROOT/banderaCommons.jar:$ROOT/banderaToolFramework.jar:$ROOT/commons-cli-1.3.1.jar:$ROOT/commons-io-1.4.jar:$ROOT/commons-lang-2.1.jar:$ROOT/commons-logging-1.2.jar:$ROOT/commons-pool-1.2.jar:$ROOT/trove-2.1.0.jar:$ROOT/xmlenc-0.52.jar:/home/xqfu/DUA/bin:$ROOT/jibx-run-1.1.3.jar:$ROOT/libs/soot-trunk.jar"
echo $MAINCP
rm -R ODDSGInstr -f
mkdir -p ODDSGInstr


SOOTCP=".:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:$subjectloc/build/classes:$ROOT/SEADS.jar"


echo $SOOTCP
OUTDIR=$subjectloc/ODDStaticGraph
rm -R $OUTDIR -f
mkdir -p $OUTDIR

starttime=`date +%s%N | cut -b1-13`
	#-sclinit \
	#-wrapTryCatch \
	#-debug \
	#-dumpJimple \
	#-statUncaught \
	#-ignoreRTECD \
	#-exInterCD \
	#-main-class ScheduleClass -entry:ScheduleClass \
	
java -Xmx100g -ea -cp ${MAINCP} ODD.ODDStaticGraph \
	-w -cp ${SOOTCP} \
	-p cg verbose:false,implicit-entry:false -p cg.spark verbose:false,on-fly-cg:true,rta:false \
	-f c -d "$OUTDIR" -brinstr:off -duainstr:off \
   	-duaverbose \
	-slicectxinsens \
   	-brinstr:off -duainstr:off  \
	-process-dir $subjectloc/build/classes \
	-allowphantom \
	-wrapTryCatch \
    -intraCD \
    -interCD \
    -exInterCD \
    -serializeVTG \
	-dumpFunctionList \
    1>ODDSGInstr/instr.out 2>ODDSGInstr/instr.err

stoptime=`date +%s%N | cut -b1-13`
echo "StaticAnalysisTime for ${ver}${seed} elapsed: " `expr $stoptime - $starttime` milliseconds

echo "Running finished."
exit 0


# hcai vim :set ts=4 tw=4 tws=4

