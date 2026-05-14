#!/bin/bash
if [ $# -lt 0 ];then
	echo "Usage: $0 "
	exit 1
fi

source ./xs_global.sh
#ROOT=/home/xqfu/
#DRIVERCLASS=ChatServer.core.MainServer
subjectloc=$subjectloc


#MAINCP=".:/opt/jdk1.8.0_101/jre/lib/rt.jar:/home/xqfu/libs/soot-trunk8.jar:/home/xqfu/libs/DUA3.jar:/home/xqfu/libs/mcia8.jar:$$subjectloc/bin"
MAINCP=".:/home/xqfu/libs/soot-trunk.jar:/home/xqfu/DUA/bin:/home/xqfu/SEADS.jar"
echo $MAINCP
rm -R out-Instr -f
mkdir -p out-Instr


SOOTCP=".:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar:$subjectloc/java/bin:$subjectloc/bin:/home/xqfu/SEADS.jar"


echo $SOOTCP
OUTDIR=$subjectloc/SEADSInstrumented
rm -R $OUTDIR
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
java -Xmx100g -ea -cp ${MAINCP} ODD.ODDInst \
	-w -cp ${SOOTCP} \
	-p cg verbose:false,implicit-entry:false -p cg.spark verbose:false,on-fly-cg:true,rta:false \
	-f c -d "$OUTDIR" -brinstr:off -duainstr:off \
   	-brinstr:off -duainstr:off  \
	-process-dir $subjectloc/java/bin \
	-process-dir $subjectloc/bin \
    -allowphantom \
	-dumpFunctionList \
	-serializeVTG \
#	 1>out-DTInstr/instr.out 2>out-DTInstr/instr.err

cp version.txt $subjectloc/SEADSInstrumented/org/xsocket/. -f
stoptime=`date +%s%N | cut -b1-13`
echo "StaticAnalysisTime elapsed: " `expr $stoptime - $starttime` milliseconds

echo "Running finished."
exit 0


# hcai vim :set ts=4 tw=4 tws=4

