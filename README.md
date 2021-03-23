Assignment 3: The Snapshot Algorithm

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

Completion status: Completed


