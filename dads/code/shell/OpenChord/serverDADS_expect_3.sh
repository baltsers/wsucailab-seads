#!/usr/bin/expect
set timeout 60

	spawn ./serverDADS.sh
	expect -re "oc >"
	send "joinN -port 8080 -bootstrap 10.99.1.191:4242  \r"
	expect -re "oc >"
for {set i 0} {$i<9999} {incr i 0}  {
	#spawn ./server.sh
	#expect -re "oc >"
	#send "joinN \r"
	#expect -re "oc >"
	send "insertN -key test -value test \r"
	expect -re "oc >"
	send "retrieveN -key test \r"
	expect -re "oc >"
	send "entriesN \r"
	expect -re "oc >"
	send "refsN \r"
	expect -re "oc >"	
	send "removeN -key test -value test \r"
	expect -re "oc >"
	
	#send "\003" 
	set seconds [exec sh -c {./RANDOMNUM.sh}]
    sleep $seconds
}
expect eof
