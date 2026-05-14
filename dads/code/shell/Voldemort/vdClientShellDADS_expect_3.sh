#!/usr/bin/expect
set timeout 60

	spawn ./vdClientShellDADS.sh
	sleep 199
	expect -re "> "
for {set i 0} {$i<9999} {incr i 0}  {
	send "put 'hello" "world' \r"
	sleep 1
	expect -re "> "
	send "get 'hello' \r"
	sleep 1
	expect -re "> "
	send "delete 'hello' \r"
	sleep 1
	expect -re "> "
	send "get 'hello' \r"
	sleep 1
	expect -re "> "
	
	#send "\003" 
	set seconds [exec sh -c {./RANDOMNUM.sh}]
    sleep $seconds
}
expect eof
