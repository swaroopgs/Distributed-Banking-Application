import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
public class ControllerHandler extends Thread {
    private Socket clientSocket;
    public Branch branchServer;

    public ControllerHandler(Socket socketIn, Branch branchServerIn) {
        clientSocket = socketIn;
        branchServer = branchServerIn;
    }

    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            Bank.BranchMessage.Builder branchMsgBuilder  = Bank.BranchMessage.newBuilder();
            Bank.BranchMessage branchMessage = null;
            while( (branchMessage = Bank.BranchMessage.parseDelimitedFrom(inputStream)) != null){

                if(branchMessage.hasInitSnapshot()) {
                    branchServer.initiateSnapshot(branchMessage.getInitSnapshot().getSnapshotId());
                }
                if(branchMessage.hasRetrieveSnapshot()) {
                    branchMsgBuilder  = Bank.BranchMessage.newBuilder();
                    Bank.ReturnSnapshot returnSnapshot = branchServer.returnSnapshotBuilder(branchMessage.getRetrieveSnapshot().getSnapshotId());
                    branchMsgBuilder.setReturnSnapshot(returnSnapshot);
                    branchMsgBuilder.build().writeDelimitedTo(clientSocket.getOutputStream());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
