#!/usr/bin/expect
set timeout 60
for {set i 0} {$i<9999} {incr i 0}  {
	spawn ./clientDADS.sh
	sleep 3
    set seconds [exec sh -c {./RANDOMNUM.sh}]
    puts "seconds: $seconds"
    sleep $seconds
	
	send "\003" 
}
expect eof
