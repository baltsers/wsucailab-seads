#!/usr/bin/expect
set timeout 60
spawn ./clientDADS.sh
sleep 3  
send "User \r"
expect "Password:"
send "Pass \r"
expect "Login successful!"
sleep 1
for {set i 0} {$i<9999} {incr i 0}  {
    set sentence [exec sh -c {./RandomSentence.sh}]
    puts "sentence: $sentence"
    send "$sentence \r"

    set seconds [exec sh -c {./RANDOMNUM.sh}]
    puts "seconds: $seconds"
    expect "received data from "
    sleep $seconds
}
expect eof
