import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Controller {
    Bank.InitBranch.Builder branchBuilder = Bank.InitBranch.newBuilder();
    public static int branchTotalAmount = 0;
    public File file = null;
    public BufferedReader bufferedReader = null;
    public int branchInitAmt = 0;
    static HashMap<String, Socket> branchDetails = new HashMap<String, Socket>();

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("./controller.sh 4000 branches.txt\n");
            System.exit(0);
        }
        try {
            Controller controller = new Controller();
            branchTotalAmount = Integer.valueOf(args[0]);
            String branchFileName = args[1];
            controller.initializeBranchInfo(branchTotalAmount, branchFileName);
            controller.sendInitSnapshotToBranches();
        } catch (NumberFormatException e) {
            System.out.println("Error: Enter an Integer value for bank balance");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * creates a file object and buffer reader
     *
     * @param fileNameIn file name
     */
    private void fileReaderBranch(String fileNameIn) {
        try {
            file = new File(fileNameIn);
            if (!file.exists()) {
                System.out.println("File does not exit" + fileNameIn + "!!");
                System.exit(0);
            }
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            System.err.print("File not found");
            e.printStackTrace();
            System.exit(0);
        }
    }


    /**
     * parse the branches.txt and set respective details for the branches
     *
     * @param amount
     * @param fileName
     */
    private void initializeBranchInfo(int amount, String fileName) {
        int totalnoOfBranches = 0;
        String line = "";
        fileReaderBranch(fileName);
        try {
            while ((line = bufferedReader.readLine()) != null) {
                String[] lineArr = line.split(" ");
                String branchName = lineArr[0];
                String ipAddress = lineArr[1];
                int port = Integer.parseInt(lineArr[2]);
                setbranchBuilder(branchName, ipAddress, port);
                totalnoOfBranches++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int branchInitAmount = amount / totalnoOfBranches;
        System.out.println("Each branch is Initialed with balance   " + branchInitAmount);
        branchBuilder.setBalance(branchInitAmount);
        Bank.BranchMessage.Builder branchMsgBuilder = Bank.BranchMessage.newBuilder();
        branchMsgBuilder.setInitBranch(branchBuilder);
        //create channel for each of the branchs to listen and save the socket to a data structure
        Socket branchClientSocket = null;
        for(Bank.InitBranch.Branch branch : branchBuilder.getAllBranchesList()) {
            try {
                branchClientSocket = new Socket(branch.getIp(), branch.getPort());
                OutputStream outputStream = branchClientSocket.getOutputStream();
                branchMsgBuilder.build().writeDelimitedTo(outputStream);
                branchDetails.put(branch.getName(), branchClientSocket);
            }catch (UnknownHostException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Sets each branch details
     *
     * @param branchNameIn
     * @param branchIpAddressIn
     * @param branchPortIn
     */
    public void setbranchBuilder(String branchNameIn, String branchIpAddressIn, int branchPortIn) {
        try {
            Bank.InitBranch.Branch.Builder newbranch = Bank.InitBranch.Branch.newBuilder();
            newbranch.setName(branchNameIn);
            newbranch.setIp(branchIpAddressIn);
            newbranch.setPort(branchPortIn);
            branchBuilder.addAllBranches(newbranch);
        } catch (Exception exception) {
            System.err.println("Error in setting the branch detials");
            exception.printStackTrace();
        }
    }

    /**
     * function to send intial snapshot messages
     */
    private void sendInitSnapshotToBranches() {

        Thread initSnapshot = new Thread(){

            public void run() {

                int snapshotId = 1;

                while(true) {
                    // send snapshot message every 1 second
                    threadSleep(1000);

                    // selecting a random branch for init snapshot
                    String name = branchBuilder.getAllBranches(getRandomBranchNumber()).getName();

                    // configure init snapshot
                    Bank.InitSnapshot.Builder initSnapshotBuilder = null;
                    initSnapshotBuilder = Bank.InitSnapshot.newBuilder();
                    initSnapshotBuilder.setSnapshotId(snapshotId);

                    Bank.BranchMessage.Builder branchMesssageBuilder = null;
                    branchMesssageBuilder = Bank.BranchMessage.newBuilder();
                    branchMesssageBuilder.setInitSnapshot(initSnapshotBuilder);

                    try {
                        Socket socket = Controller.branchDetails.get(name);
                        OutputStream outputStream = socket.getOutputStream();

                        //send init snapshot message
                        branchMesssageBuilder.build().writeDelimitedTo(outputStream);

                        // waiting for snapshot to complete
                        threadSleep(2000);

                        //retrieve snapshot
                        branchMesssageBuilder = Bank.BranchMessage.newBuilder();
                        Bank.RetrieveSnapshot.Builder retrieveSnapshotBuilder = null;
                        retrieveSnapshotBuilder = Bank.RetrieveSnapshot.newBuilder();
                        retrieveSnapshotBuilder.setSnapshotId(snapshotId);

                        branchMesssageBuilder.setRetrieveSnapshot(retrieveSnapshotBuilder);

                        for(Bank.InitBranch.Branch branch : branchBuilder.getAllBranchesList()) {
                            socket = Controller.branchDetails.get(branch.getName());
                            outputStream = socket.getOutputStream();
                            branchMesssageBuilder.build().writeDelimitedTo(outputStream);
                            ControllerRetrieveSnapshotHandler controllerRetrieveSnapshotHandler = new ControllerRetrieveSnapshotHandler(socket, branch.getName(), branchBuilder);
                            controllerRetrieveSnapshotHandler.start();
                        }

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    snapshotId += 1;
                }
            }
        };

        initSnapshot.start();
    }

    /**
     * function to put thread to sleep
     * @param time
     */
    public void threadSleep(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * function to get a random branch number
     * @return
     */
    public int getRandomBranchNumber(){
        return ThreadLocalRandom.current().nextInt(0, branchBuilder.getAllBranchesCount());
    }

}



