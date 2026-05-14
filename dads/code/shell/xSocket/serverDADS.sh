#!/bin/bash
if [ $# -lt 0 ];then
	echo "Usage: $0 "
	exit 1
fi

source ./xs_global.sh
MAINCP=".:$subjectloc/ODDInstrumented:/home/xqfu/libs/soot-trunk.jar:/home/xqfu/DUA/bin:/home/xqfu/DADS.jar"
starttime=`date +%s%N | cut -b1-13`
java -cp ${MAINCP} XSocketServer
stoptime=`date +%s%N | cut -b1-13`
echo "StaticAnalysisTime for ${ver}${seed} elapsed: " `expr $stoptime - $starttime` milliseconds
