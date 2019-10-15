package gitlet;


import java.io.Serializable;
import java.util.TreeMap;

public class Commit implements Serializable {
    private String commitID;
    private String parentID;
    private String logMessage;
    private String timeStamp;
    private TreeMap<String, String> fileMap;
    private String branchName;

    public Commit(String CID, String PID, String logM, String timeS, String bSymbol,
                  TreeMap<String, String> files) {
        commitID = CID;
        parentID = PID;
        logMessage = logM;
        timeStamp = timeS;
        branchName = bSymbol;
        fileMap = files;
    }

    public TreeMap<String, String> getFileMap() {
        return fileMap;
    }

    public String getBranchSymbol() {
        return branchName;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public String getParentID() {
        return parentID;
    }

    public String getCommitID() {
        return commitID;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        String NEWLINE = System.getProperty("line.separator");

        sb.append("===").append(NEWLINE);
        sb.append("Commit ").append(commitID).append(NEWLINE);
        sb.append(timeStamp).append(NEWLINE);
        sb.append(logMessage).append(NEWLINE);
        return sb.toString();
    }
}
