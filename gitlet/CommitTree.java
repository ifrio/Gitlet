package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

public class CommitTree implements Serializable {
    // private Map<String, String> allCommits; //optional
    private String head;
    private TreeMap<String, String> branches;
    private TreeMap<String, String> splitPoints;
    private StagingArea stagingArea;

    private static String WORK_DIR = Command.WORK_DIR;
    public static final String DIR = "/.gitlet/.commits/";


    //gitlet init will create a CommitTree object
    public CommitTree(){
        //initial directories
        new File(WORK_DIR + "/.gitlet").mkdirs();
        new File(WORK_DIR + "/.gitlet/.staged").mkdirs();
        new File(WORK_DIR + "/.gitlet/.blobs").mkdirs();
        new File(WORK_DIR + "/.gitlet/.commits").mkdirs();

        stagingArea = new StagingArea();
        Utils.serialize(WORK_DIR + "/.gitlet/StagingArea", stagingArea);

        String currTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String message = "initial commit";
        String cid = gitlet.Utils.sha1(message, currTime);
        branches = new TreeMap<>();
        splitPoints = new TreeMap<>();
        head = "master";
        branches.put("master", cid);

        Commit commit = new Commit(cid, null, message, currTime, head, new TreeMap<>());
        Utils.serialize(WORK_DIR + DIR + cid, commit);
    }

    public void add(String FileName, Blob blob) {
        String currCID = branches.get(head);
        Commit commit = (Commit)Utils.deserialize(WORK_DIR + DIR + currCID);
        if (commit.getFileMap().containsKey(FileName)
                && commit.getFileMap().get(FileName).equals(blob.fileID)){
            return;
        }
        stagingArea = StagingArea.reInstantateStagingArea();
        stagingArea.add(FileName, blob);
        Utils.serialize(WORK_DIR + "/.gitlet/StagingArea", stagingArea);
    }

    public void createBranch(String branchName) {
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
        }

        String commitID = branches.get(head);
        String splitKey = getSplitKey(head, branchName);
        branches.put(branchName, commitID);
        head = branchName;
        splitPoints.put(splitKey, commitID);
    }

    public String getSplitKey(String branch1, String branch2) {
        if (branch1.compareTo(branch2) < 0) {
            return branch1 + branch2;
        } else {
            return branch2 + branch1;
        }
    }

    public void deleteBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        if (head.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        }

        branches.remove(branchName);
    }


    public void createCommit(String logM) {
        //Open staging area
        stagingArea = StagingArea.reInstantateStagingArea();

        TreeMap<String, String> added = stagingArea.getStaged();
        TreeMap<String, String> removed = stagingArea.getRemoved();


        if (added.isEmpty() && removed.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        if (logM == null || logM.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }

        String newCID;
        String pCID = branches.get(head);
        Commit newCommit;
        Commit parent = (Commit)Utils.deserialize(WORK_DIR + DIR + pCID);
        String currTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        TreeMap<String, String> fileMap = parent.getFileMap();

        //Trust the staging area already check the qualification of files
        //update files
        for (String file : added.keySet()) {
            if (fileMap.containsKey(file)) {
                fileMap.replace(file, added.get(file));
            } else {
                fileMap.put(file, added.get(file));
            }
        }

        //remove files
        for (String rmFile : removed.keySet()) {
            fileMap.remove(rmFile);
        }

        newCID = Utils.sha1(fileMap.toString(), pCID, logM, currTime);
        newCommit = new Commit(newCID, pCID, logM, currTime, head, fileMap);

        branches.replace(head, newCID);
        stagingArea.clearStage();

        //serialize the new commit and Staging Area
        Utils.serialize(WORK_DIR + DIR + newCID, newCommit);
        Utils.serialize(WORK_DIR + "/.gitlet/StagingArea", stagingArea);
    }

    public void log() {
        String currCID = branches.get(head);
        while (currCID != null) {
            Commit commit = (Commit)Utils.deserialize(WORK_DIR + DIR + currCID);
            System.out.println(commit);
            currCID = commit.getParentID();
        }
    }

    public void globalLog() {
        File allCommits = new File(WORK_DIR + DIR);
        for (String fileName : allCommits.list()){
            Commit commit = (Commit)Utils.deserialize(WORK_DIR + DIR + fileName);
            System.out.println(commit);
        }
    }


    public void status() {
        System.out.println("=== Branches ===");
        stagingArea = stagingArea.reInstantateStagingArea();
        for (String b : branches.keySet()) {
            if (b == head) {
                System.out.println("*" + b);
            } else {
                System.out.println(b);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        for (String fileName : stagingArea.getStaged().keySet()) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String fileName : stagingArea.getRemoved().keySet()) {
            System.out.println(fileName);
        }
        System.out.println();

        /*
        Modifications Not Staged For commit. Optional.
         */
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        /*
        Untracked Files. Optional.
         */
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }


    public void reset(String commitID) {
        File file = new File(WORK_DIR + DIR + commitID);
        if (!file.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit currCommit = (Commit)Utils.deserialize(WORK_DIR + DIR + branches.get(head));
        Commit resetCommit = (Commit)Utils.deserialize(WORK_DIR + DIR + commitID);
        stagingArea = StagingArea.reInstantateStagingArea();

        if (checkUntrackedFileInTheWay(currCommit, resetCommit)) {
            System.out.println("There is an untracked file in the way; delete it or add it first.");
            return;
        }

        for (String fileName : currCommit.getFileMap().keySet()) {
            if (!checkFileInCommit(fileName, resetCommit)) {
                gitlet.Utils.restrictedDelete(fileName);
            }
        }

        for (String fileName : resetCommit.getFileMap().keySet()) {
            checkout(resetCommit, fileName);
        }

        branches.replace(head, commitID);
        stagingArea.clearStage();
        Utils.serialize(WORK_DIR + "/.gitlet/StagingArea", stagingArea);
    }


    public void find(String message) {
        // Get all commit
        File dir = new File(WORK_DIR + DIR);
        File[] directoryListing = dir.listFiles();
        boolean found = false;
        Commit eachCommit;
        for (File fileCommit : directoryListing) {
            eachCommit = (Commit)Utils.deserialize(WORK_DIR + DIR + fileCommit.getName());
            if (eachCommit.getLogMessage().equals(message)) {  // Check if it's the message we need.
                System.out.println(eachCommit.getCommitID());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void remove(String fileName) {
        Commit commit = (Commit)Utils.deserialize(WORK_DIR + DIR + branches.get(head));
        stagingArea = StagingArea.reInstantateStagingArea();
        TreeMap<String, String> staged = stagingArea.getStaged();
        TreeMap<String, String> fileMap = commit.getFileMap();

        if (fileMap.containsKey(fileName)) {
            stagingArea.addRemovedFile(fileName, fileMap.get(fileName));
            Utils.restrictedDelete(WORK_DIR + "/" + fileName);
            if (staged.containsKey(fileName)) {
                stagingArea.unstage(fileName);
            }
        } else if (staged.containsKey(fileName)) {
            stagingArea.unstage(fileName);
        } else {
            System.out.println("No reason to remove the file.");
            return;
        }

        Utils.serialize(WORK_DIR + "/.gitlet/StagingArea", stagingArea);
    }


    public void checkoutFile(String FileName) {//checkout case 1
        String currCID = branches.get(head);
        checkoutCommitFile(currCID, FileName);

    }

    public void checkoutCommitFile(String commitID, String FileName) { //checkout case 2
        if (!checkIDCommit(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit commit = (Commit)Utils.deserialize(WORK_DIR + DIR + commitID);
        if (!checkFileInCommit(FileName, commit)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        checkout(commit, FileName);

    }

    public void checkoutBranch(String branchName) { //checkout case 3
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (branchName == branches.get(head)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        Commit currCommit = (Commit)Utils.deserialize(WORK_DIR + DIR + branches.get(head));
        Commit commitBranch = (Commit)Utils.deserialize(WORK_DIR + DIR + branches.get(branchName));
        if (checkUntrackedFileInTheWay(currCommit, commitBranch)){
            System.out.println("There is an untracked file in the way; delete it or add it first.");
            return;
        }
        for (String FileName : currCommit.getFileMap().keySet()) {
            if (!checkFileInCommit(FileName, commitBranch)) {
                gitlet.Utils.restrictedDelete(FileName);
            }
        }
        for (String FileName : commitBranch.getFileMap().keySet()) {
            checkout(commitBranch, FileName);
        }

        head = branchName;
        stagingArea = StagingArea.reInstantateStagingArea();
        stagingArea.clearStage();
        Utils.serialize(WORK_DIR + "/.gitlet/StagingArea", stagingArea);
    }

    public void checkout(Commit commit, String FileName) {//the general code which write/rewrite a file
        gitlet.Utils.restrictedDelete(FileName);
        String BlobID = commit.getFileMap().get(FileName);
        Blob fileBlob = (Blob)Utils.deserialize(WORK_DIR + "/.gitlet/.blobs/" + BlobID);
        File newFile = new File(WORK_DIR + "/" + FileName);
        gitlet.Utils.writeContents(newFile, fileBlob.fileContent);
    }

    //check if a file exits in a commit
    public boolean checkFileInCommit(String FileName, Commit commit) {
        return commit.getFileMap().containsKey(FileName);
    }

    //check if a file has been removed so it is untracked even if it is in the latest commit
    public boolean checkRemoved (String FileName){
        stagingArea = StagingArea.reInstantateStagingArea();
        return stagingArea.getRemoved().containsKey(FileName);
    }

    public boolean checkIDCommit(String IDCommit) { //check if it exists a commit whit that ID
        File commitFile = new File(WORK_DIR + DIR + IDCommit);
        return commitFile.exists();
    }

    //check if there is any file untracked in our latest commit (currCommit) which would be overwritten
    public boolean checkUntrackedFileInTheWay (Commit currCommit, Commit commit){
        for (String FileName : commit.getFileMap().keySet()) {
            if (checkFileInCommit(FileName, currCommit) && checkRemoved(FileName)){
                return true;
            }
        }
        return false;
    }
}
