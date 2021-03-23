import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ControllerRetrieveSnapshotHandler extends Thread {
    private Socket clientSocket;
    private String destBranch;
    Bank.InitBranch.Builder branchBuilder;

    public ControllerRetrieveSnapshotHandler(Socket socket, String name, Bank.InitBranch.Builder branchBuilderIn) {
        clientSocket = socket;
        destBranch = name;
        branchBuilder = branchBuilderIn;
    }

    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            Bank.BranchMessage branchMessage = Bank.BranchMessage.parseDelimitedFrom(inputStream);
            if (branchMessage.hasReturnSnapshot()) {
                List<Integer> channelStateList = branchMessage.getReturnSnapshot().getLocalSnapshot().getChannelStateList();
                List<String> branchNames = new ArrayList<>();
                for (int count = 0; count < branchBuilder.getAllBranchesCount(); count++) {
                    if (!branchBuilder.getAllBranches(count).getName().equals(destBranch)) {
                        branchNames.add(branchBuilder.getAllBranches(count).getName());
                    }
                }
                if (channelStateList.size() == branchNames.size()) {
                    System.out.println("-----------------------------------------------\n");
                    System.out.println("snapshot_id: " + branchMessage.getReturnSnapshot().getLocalSnapshot().getSnapshotId());
                    System.out.print(destBranch + ":" + branchMessage.getReturnSnapshot().getLocalSnapshot().getBalance() + ", ");

                    for (int branchCount = 0; branchCount < branchNames.size(); branchCount++) {
                        System.out.print(branchNames.get(branchCount) + "->" + destBranch + ": " + channelStateList.get(branchCount) + ", ");
                    }
                } else {
                    System.out.println("Snapshot Incomplete");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
