package gitlet;


import java.io.*;
import java.nio.file.Paths;


public class Command {

    public static final String WORK_DIR = Paths.get(".").toAbsolutePath().normalize().toString();
    private static final String PATH = WORK_DIR + "/.gitlet/commitTree";
    private static CommitTree commitTree;

    public static void excute(String[] args){

        String first = args[0];
        int size = args.length;
        if (first.equals("init")) {
            init();
            return;
        }
        commitTree = (CommitTree)Utils.deserialize(PATH);

        switch (first) {
            case "add":
                if (size >= 2) {
                    add(args, size);
                } else {
                    System.out.println("Please enter the file name you want to add");
                }
                break;
            case "commit":
                if (size >= 2) {
                    commitTree.createCommit(args[1]);
                } else {
                    System.out.println("Please enter the message you want to commit");
                }
                break;
            case "rm":
                commitTree.remove(args[1]);
                break;
            case "log":
                commitTree.log();
                break;
            case "global-log":
                commitTree.globalLog();
                break;
            case "find":
                commitTree.find(args[1]);
                break;
            case "status":
                commitTree.status();
                break;
            case "checkout":
                switch (size) {
                    case 3:
                        if (args[1].equals("--")) {
                            commitTree.checkoutFile(args[2]);
                        } else {
                            System.out.println("Incorrect operands.");
                        }
                        break;
                    case 4:
                        if (args[2].equals("--")) {
                            commitTree.checkoutCommitFile(args[1], args[3]);
                        } else {
                            System.out.println("Incorrect operands.");
                        }
                        break;
                    case 2:
                        commitTree.checkoutBranch(args[1]);
                        break;
                    default:
                        System.out.println("Wrong arguments");
                        break;
                }
                break;
            case "branch":
                commitTree.createBranch(args[1]);
                break;
            case "rm-branch":
                commitTree.deleteBranch(args[1]);
                break;
            case "reset":
                commitTree.reset(args[1]);
                break;
            case "merge":
            //    commitTree.merge(args[1]);
                break;
            default:
                System.out.println(first
                        + " is not a gitlet command, please enter a valid command again.");
                break;
        }
        Utils.serialize(PATH, commitTree);

    }

    public static void init() {
        File gitlet = new File(WORK_DIR + "/.gitlet");
        if (gitlet.exists()) {
            System.out.println("A gitlet version-control system already exists in the current directory.");
        } else {
            commitTree = new CommitTree();
            Utils.serialize(PATH, commitTree);
        }
    }

    public static void add(String[] args, int size){
        //Deserialize Staging area
        for(int i = 1; i < size; i++){
            File file = new File(WORK_DIR + "/" + args[i]);
            if (!file.exists()) {
                System.out.println("File does not exist.");
                return;
            }
            Blob blobby = new Blob(file);
            commitTree.add(args[i], blobby);
        }
    }


}
