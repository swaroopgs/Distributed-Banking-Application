import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
public class BranchHandler extends Thread {
    private Socket clientSocket;
    private Branch branchServer;
    private String requestedBranch;

    public BranchHandler(Socket socket, Branch server, String name) {
        clientSocket = socket;
        branchServer = server;
        requestedBranch = name;
    }

    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            Bank.BranchMessage branchMessage = null;
            while( (branchMessage = Bank.BranchMessage.parseDelimitedFrom(inputStream)) != null){
                if(branchMessage.hasTransfer()) {
                    int amount = branchMessage.getTransfer().getAmount();
                    branchServer.addAmountBranchBalance(amount);
                }
                if(branchMessage.hasMarker()) {
                    //if has a mearher message then receive the message
                    branchServer.receiveingMarkerMessage(branchMessage.getMarker().getSnapshotId(), requestedBranch);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
