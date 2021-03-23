LIB_PATH=lib/protobuf-java-3.7.0.jar
all: clean
	mkdir bin
	javac -classpath $(LIB_PATH) -d bin Bank.java src/Branch.java src/BranchHandler.java src/ControllerHandler.java src/ControllerRetrieveSnapshotHandler.java src/Controller.java

clean:
	rm -rf bin
