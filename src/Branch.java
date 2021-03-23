import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Branch {
    public HashMap<Integer, HashMap<String, Integer>> incomingChannelStatesMap = new HashMap<>();
    //ConcurrentHashMap is used  beacuse it is thread safe
    public ConcurrentHashMap<Integer, Map<String, Integer>> finalMap = new ConcurrentHashMap<>();

    static int branchBalance = 0;
    int bankbranchInitialBalance = 0;
    static String branchIPAddress;
    static String branchName;
    static int branchPort;
    Bank.BranchMessage allBranchDetails;
    static HashMap<String, Socket> branchSockets = new HashMap<>();
    static int time=0;
    static int totalTcpestablishedConnections = 0;
    static int flag = 0;
    private final Object balanceLock = new Object();

    private ConcurrentHashMap<Integer, Integer> branchSnapID = new ConcurrentHashMap<>();
    public static void main(String[] args) {

        Branch branchServer = new Branch();
        ServerSocket serverSocket = null;

        if (args.length != 3) {
            System.out.println("Usage: ./branch branch1 9090 100\n");

            System.exit(0);
        }

        try {
            time = Integer.valueOf(args[2]);
            if(!(time >1 && time<5000)){
                System.out.println("Usage: t must be in number between 1 and 5000.\n");
                System.exit(0);
            }
            branchIPAddress = InetAddress.getLocalHost().getHostAddress();
            branchName = args[0];
            branchPort = Integer.valueOf(args[1]);
            serverSocket = new ServerSocket(branchPort);
            System.out.println("*************" + branchName + " Server Started " + branchIPAddress + " " + branchPort + "*********");
            Bank.BranchMessage branchMsg = null;
            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();
            branchMsg = Bank.BranchMessage.parseDelimitedFrom(inputStream);
            if (branchMsg.hasInitBranch()) {
                // setup tcp connections with initial and the branch details
                branchServer.initializeBranchDetailandsetTCPConnectionsBranch(branchMsg);
            }

            ControllerHandler controllerHandler = new ControllerHandler(socket, branchServer);
            controllerHandler.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                if(flag == 0) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String branchName = in.readLine();
                    branchSockets.put(branchName, socket);
                    totalTcpestablishedConnections+=1;
                    BranchHandler branchrHandler = new BranchHandler(socket,branchServer, branchName);
                    branchrHandler.start();
                    if(branchServer.checksyncTCPConnections() == true){
                        branchServer.transferMoneyToAllBranches();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
    private void initializeBranchDetailandsetTCPConnectionsBranch(Bank.BranchMessage branchMsgIn) {
        bankbranchInitialBalance = branchMsgIn.getInitBranch().getBalance();
        branchBalance = bankbranchInitialBalance;
        allBranchDetails = branchMsgIn;
        System.out.println("branchanme ::" +branchName + " initial amount " + branchBalance);
        System.out.println("*****" +"\n");
        int branchindex = 0;
        Socket socket = null;
        for (branchindex = 0; branchindex < allBranchDetails.getInitBranch().getAllBranchesCount(); branchindex++) {
            String bankbranchName=allBranchDetails.getInitBranch().getAllBranches(branchindex).getName();
            if (bankbranchName.equals(branchName)) {
                branchindex++;
                break;
            }
        }

        for (int count = branchindex; count < allBranchDetails.getInitBranch().getAllBranchesCount(); count++) {
          try {
                socket = new Socket(allBranchDetails.getInitBranch().getAllBranches(count).getIp(), allBranchDetails.getInitBranch().getAllBranches(count).getPort());
                PrintWriter outrequest = new PrintWriter(socket.getOutputStream(), true);
                outrequest.println(branchName);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            branchSockets.put(allBranchDetails.getInitBranch().getAllBranches(count).getName(), socket);
            totalTcpestablishedConnections ++;
            //Start listening for messages and then start money transfer
            new BranchHandler(socket, this, allBranchDetails.getInitBranch().getAllBranches(count).getName()).start();
            if(checksyncTCPConnections() == true){
                transferMoneyToAllBranches();
            }
        }

    }

    public boolean checksyncTCPConnections() {
        if ((allBranchDetails.getInitBranch().getAllBranchesCount() - 1) == totalTcpestablishedConnections) {
            flag = 1;
            return  true;
        }
        return false;
    }

    private void transferMoneyToAllBranches() {
        Thread branchTransactions = new Thread() {
            public void run() {
                System.out.println("Transfer money initiated");
                while (true) {
                    // the thread is slept for time 0- t which is uniformllyrandomlly varied
                    //Transfer amount
                    long sleep = (long) (Math.random() * (time));
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    String branch = getRandomBranchName();
                    Socket clientSocket;
                    try {
                        Bank.Transfer.Builder transferMessageBuilder = Bank.Transfer.newBuilder();
                        int amountToTransfer = getAmountToDebit();
                        if (amountToTransfer > 0) {
                            transferMessageBuilder.setAmount(amountToTransfer);
                            Bank.BranchMessage.Builder branchMessageBuilder = Bank.BranchMessage.newBuilder();
                            branchMessageBuilder.setTransfer(transferMessageBuilder);
                            clientSocket = branchSockets.get(branch);
                            branchMessageBuilder.build().writeDelimitedTo(clientSocket.getOutputStream());
                            if(time >= 1000)
                            System.out.println("Transfering amount of " + amountToTransfer + "to " + branch + "and branchBalance is " + branchBalance);
                        }
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        branchTransactions.start();
    }

    public String getRandomBranchName() {
        int randomIndex = ThreadLocalRandom.current().nextInt(0, allBranchDetails.getInitBranch().getAllBranchesCount());
        while (allBranchDetails.getInitBranch().getAllBranches(randomIndex).getName().equals(branchName)) {
            randomIndex = ThreadLocalRandom.current().nextInt(0, allBranchDetails.getInitBranch().getAllBranchesCount());
        }
        return allBranchDetails.getInitBranch().getAllBranches(randomIndex).getName();
    }

    public int getAmountToDebit() {
        int minMoney = (int) (bankbranchInitialBalance*5/100);
        int maxMoney = (int) (bankbranchInitialBalance*10/100);
        int amountToDebit = ThreadLocalRandom.current().nextInt(minMoney, maxMoney + 1);
        synchronized (balanceLock) {
            if ((branchBalance - amountToDebit) > 0) {
                if(time >= 1000)
                System.out.println(" Transfer Before Balance --- " + branchBalance);
                branchBalance = branchBalance - amountToDebit;
            } else
                amountToDebit = 0;
        }
        return amountToDebit;
    }

    public void addAmountBranchBalance(int amountIn) {
        synchronized (balanceLock) {
            if(time >= 1000)
            System.out.print("Received =" + branchBalance + "+" + amountIn);
            branchBalance +=amountIn;
        }
        if(time >= 1000)
        System.out.print(" = " + branchBalance+"\n");
    }

    public void initiateSnapshot(int snapshotId) {
        branchSnapID.put(snapshotId, branchBalance);
        if(incomingChannelStatesMap.get(snapshotId) == null) {
            HashMap<String, Integer> incomingChannelMap = new HashMap<>();
            Iterator loop = branchSockets.entrySet().iterator();
            while (loop.hasNext()) {
                Map.Entry pair = (Map.Entry)loop.next();
                incomingChannelMap.put((String) pair.getKey(), -1);
            }
            incomingChannelStatesMap.put(snapshotId, incomingChannelMap);
        }
        //set the snapshot message
        Bank.Marker.Builder markerMessage = Bank.Marker.newBuilder();
        markerMessage.setSnapshotId(snapshotId);
        sendMarkerMessagesToAllBranch(markerMessage);
    }


    public void sendMarkerMessagesToAllBranch(Bank.Marker.Builder markerMessageIn) {
        Bank.BranchMessage.Builder branchMesssageBuilder  = Bank.BranchMessage.newBuilder();
        branchMesssageBuilder.setMarker(markerMessageIn);
        // each of the branch sockets saved send marker message to all
        Iterator iterator = branchSockets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry branchSocketpair = (Map.Entry)iterator.next();
            Socket socketBranch = branchSockets.get(branchSocketpair.getKey());
            OutputStream outputRequest;
            try {
                outputRequest = socketBranch.getOutputStream();
                branchMesssageBuilder.build().writeDelimitedTo(outputRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void receiveingMarkerMessage(int snapshotId, String fromBranch) {
        // first marker message already received If Key is present,
        if(incomingChannelStatesMap.get(snapshotId) != null){
           Map<String, Integer> incomingChannel;
            if(finalMap.get(snapshotId) == null) {
                incomingChannel = new HashMap<>();
                finalMap.put(snapshotId, incomingChannel);
            }
            if(incomingChannelStatesMap.get(snapshotId) != null) {
                int recordedBalance = incomingChannelStatesMap.get(snapshotId).get(fromBranch);
                finalMap.get(snapshotId).put(fromBranch, recordedBalance);
            }

        }else {
            branchSnapID.put(snapshotId, branchBalance);
            if(incomingChannelStatesMap.get(snapshotId) == null) {
              //  recordFinalChannelStateAndStopRecording(snapshotId,fromBranch);
                HashMap<String, Integer> incomingChannel = new HashMap<>();
                Iterator loop = branchSockets.entrySet().iterator();
                while (loop.hasNext()) {
                    Map.Entry pair = (Map.Entry)loop.next();
                    incomingChannel.put((String) pair.getKey(), -1);
                }
                incomingChannelStatesMap.put(snapshotId, incomingChannel);
            }
            incomingChannelStatesMap.get(snapshotId).put(fromBranch, 0);
           // recordFinalChannelStateAndStopRecording(snapshotId,fromBranch);
            Map<String, Integer> incomingChannel;
            if(finalMap.get(snapshotId) == null) {
                incomingChannel = new HashMap<>();
                finalMap.put(snapshotId, incomingChannel);
            }
            if(incomingChannelStatesMap.get(snapshotId) != null) {
                int recordedBalance = incomingChannelStatesMap.get(snapshotId).get(fromBranch);
                finalMap.get(snapshotId).put(fromBranch, recordedBalance);
            }
            Bank.Marker.Builder markerMessage = Bank.Marker.newBuilder();
            markerMessage.setSnapshotId(snapshotId);
            sendMarkerMessagesToAllBranch(markerMessage);
        }
    }


    public Bank.ReturnSnapshot returnSnapshotBuilder(int snapshotId) {
        ArrayList<Integer> branchList = new ArrayList<>();
        Bank.ReturnSnapshot.Builder returnMsgBuilder = Bank.ReturnSnapshot.newBuilder();
        Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapShotBuilder = Bank.ReturnSnapshot.LocalSnapshot.newBuilder();
        localSnapShotBuilder.setSnapshotId(snapshotId);
        //Wait till snapshot finishes
        while(finalMap.get(snapshotId) == null) {
            try{
                Thread.sleep(1000L);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        while(finalMap.get(snapshotId).size() != branchSockets.size()){
            try{
                Thread.sleep(1000L);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        localSnapShotBuilder.setBalance(branchSnapID.get(snapshotId));
        Map<String, Integer> channelMap = finalMap.get(snapshotId);
        for(Bank.InitBranch.Branch branch : allBranchDetails.getInitBranch().getAllBranchesList()) {
            if(channelMap.get(branch.getName()) != null) {
                if(channelMap.get(branch.getName()) == -1)
                    branchList.add(0);
                else
                    branchList.add(channelMap.get(branch.getName()));
            }
        }
        localSnapShotBuilder.addAllChannelState(branchList);
        returnMsgBuilder.setLocalSnapshot(localSnapShotBuilder);
        return returnMsgBuilder.build();
    }

}


