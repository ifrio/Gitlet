package gitlet;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.TreeMap;


public class StagingArea implements Serializable {
    //Stores Blobs
 //   private TreeMap<String, String> tracked; //change to mapped
    private TreeMap<String, String> removed; // "              "
    private TreeMap<String, String> staged;
    private static String WORK_DIR = Command.WORK_DIR;


    public StagingArea(){
       // tracked = new TreeMap<>();
        removed = new TreeMap<>();
        staged = new TreeMap<>();
    }

    public static StagingArea reInstantateStagingArea(){
        StagingArea obj;
        File inFile = new File(WORK_DIR + "/.gitlet/StagingArea");
        try {
            ObjectInputStream inp = new ObjectInputStream(new FileInputStream(inFile));
            obj = (StagingArea) inp.readObject();
            inp.close();
            return obj;
        } catch (IOException | ClassNotFoundException excp) {
            return new StagingArea();
        }
    }

    public boolean findFile(String name, File directory) {
        File[] list = directory.listFiles();
        for (File fil : list) {
            if (name.equalsIgnoreCase(fil.getName())) {
                return true;
            }
        }
        return false;
    }

    public static final String StagedPathName(String blobID) {
        /* creates the path name to store in directory */
        return WORK_DIR+ ".gitlet/.staged/" + blobID;
    }

    public void add(String FileName, Blob blobObj) {

        /* checks if the file is already in the .staged directory */
        if (removed.containsKey(FileName)) {
            removed.remove(FileName);
            return;
        }

        if (findFile(FileName, new File(WORK_DIR + "/.gitlet/.staged"))) {
            String deleteID = staged.get(FileName);
            deletefromstaged(deleteID);
        }

        String blobID = blobObj.fileID;
        /* copying from .blobs --> .staged */
        staged.put(FileName, blobID);
        // tracked.put(FileName, blobID);
        //File source = new File(WORK_DIR + "/.gitlet/.blobs/" + blobID);
        Utils.serialize(WORK_DIR + "/.gitlet/.blobs/" + blobID, blobObj);
        // renaming the file and moving it to a new location (COPYING)
        Utils.serialize(WORK_DIR + "/.gitlet/.staged/" + blobID, blobObj);
        // source.renameTo(new File(WORK_DIR + "/.gitlet/.staged/" + blobID));
    }

    public void deletefromstaged(String blobID) {
        File file = new File(StagedPathName(blobID));
        file.delete();
    }

    /* If the file isn’t tracked by the current commit but it is staged,
     unstage the file and do nothing else. */
    public void unstage(String fileName) {

        //removing from stage directory
        deletefromstaged(staged.get(fileName));

        //removed from staged map
        staged.remove(fileName, staged.get(fileName));
    }

    public void clearStage() {
        File stagedFile = new File(WORK_DIR + "/.gitlet/.staged");
        File[] list = stagedFile.listFiles();
        for (File fil : list) {
            fil.delete();
        }
        staged.clear();
        removed.clear();
    }


    public TreeMap<String, String> getRemoved() {
        return removed;
    }

    public TreeMap<String, String> getStaged() {

        return staged;
    }

    public void addRemovedFile(String fileName, String fileID) {
        removed.put(fileName, fileID);
    }

}
