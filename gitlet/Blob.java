package gitlet;

import java.io.File;
import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Blob implements Serializable {
    public byte[] fileContent;
    public String fileName;
    public String fileID;


    public Blob(File file) {
        fileContent = gitlet.Utils.readContents(file);
        fileName = file.getName();
        fileID = gitlet.Utils.sha1(fileName, fileContent);
    }

    public String getID() {
        return fileID;
    }

}
