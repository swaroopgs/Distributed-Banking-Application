The Snapshot Algorithm
-------------------------

Name: Swaroop Gowdra Shanthakumar

Programming Language used
-------------------------
Java

Instructions to compile and run the application
-----------------------------------------------

1. To generate Bank class, use the following command
```
bash
export PATH=/home/yaoliu/src_code/local/bin:$PATH
protoc --java_out=./ bank.proto
```
2. To compile:
Navigate to project folder and then run the following command
```make```

3. To run between 2 branches (Add more branches in branches.txt to run it between more branches): 
```
./branch.sh branch1 9090 |*time interval*|
./branch.sh branch2 9091 |*time interval*|
./controller.sh |*total amount*| branches.txt
```

Implementation details
----------------------

1. The Controller reads the ip addresses and port numbers from the given file and divides the initial amount equally with all the branches and initializes them.
2. Upon initialization each branch waits until it is completely initialized before starting to transfer money.
3. Random money transfer
	a. Used TimerTask and Timer classes.
	b. Each time a random interval of time and a random branch are determined and then a transfer is scheduled.
4. Updating of the balance is synchronized so that only one thread would update the balance at a given instant of time.
5. When branch receives marker (if first marker) then it, 
	a. temporarily stalls money transfers.
	b. records its local state.
	c. sends markers to all the other branches.
	d. records the time at which the markers were sent.
	e. restarts money transfers. 
6. When branch receives money,
	a. Checks whether money arrived after recording local state and before the receipt of the marker from the corresponding branch.
		If yes then it records the receipt as the incoming channel state.
	b. Update balance.


