package gitlet;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gitlet.Utils.join;

public class Stage implements Serializable {
    /**
     * Location of the persistence file for Stage
     */
    public static final File STAGE_FILE = Utils.join(Dir.STAGE, "STAGE.txt");

    /**
     * Adds the file from the working directory to the staging area.
     * Throws error if the file doesn't exist in the working directory.
     * If the file is already in the staging area, this deletes the previous Blob and adds the new one.
     * If the file is staged for removal, un-stage it for removal.
     * If the file doesn't exist in the CWD but exists in remove, un-stage it for removal and restore
     *
     * @param filename the name of the file in the working directory to be added to the staging area
     */
    public void add(String filename) {
        File fileCWD = Utils.join(Dir.CWD, filename);
        File fileRemove = Utils.join(Dir.REMOVE, filename);
        if (!fileCWD.exists() | fileRemove.exists()) {
            if (fileRemove.exists()) {
                Utils.writeContents(fileCWD, Utils.readContents(fileRemove));
                fileRemove.delete();
                return;
            } else {
                Utils.exitWithError("File does not exist.");
            }
        }
        File HEADCommitFile = Utils.join(Dir.COMMITS, Repository.getHeadCommitId() + ".txt");
        Commit HEADCommit = Utils.readObject(HEADCommitFile, Commit.class);

        String prevBlobId = HEADCommit.getTracking().get(filename);
        //Checks if the file is the same as the file in the current commit
        if (prevBlobId != null) {
            File prevBlobFile = Utils.join(Dir.BLOBS, prevBlobId + ".txt");
            Blob prevBlob = Utils.readObject(prevBlobFile, Blob.class);
            if (prevBlob.getContents().equals(Utils.readContentsAsString(fileCWD))) {
                File prevFileAdd = Utils.join(Dir.ADD, filename);
                prevFileAdd.delete();
                return;
            }
        }

        File prevFileAdd = Utils.join(Dir.ADD, filename);
        prevFileAdd.delete();
        File prevFileRm = Utils.join(Dir.REMOVE, filename);
        prevFileRm.delete();

        File newFileAdd = Utils.join(Dir.ADD, filename);
        Utils.writeContents(newFileAdd, Utils.readContents(fileCWD));
    }


    /**
     * Removes the file associated with the given filename
     * from the ADD directory in the staging area
     * and if the file is tracked in the COMMITS directory
     * it stages the associated file within the REMOVE directory.
     *
     * @param filename Name of the file in the working directory to be unstaged.
     */
    public void rm(String filename) {
        File fileCWD = Utils.join(Dir.CWD, filename);

        File HEADCommitFile = Utils.join(Dir.COMMITS, Repository.getHeadCommitId() + ".txt");
        Commit HEADCommit = Utils.readObject(HEADCommitFile, Commit.class);
        File prevFileAdd = Utils.join(Dir.ADD, filename);

        if (!fileCWD.exists()) {
            String blobId = HEADCommit.getTracking().get(filename);
            if (blobId != null) {
                File blobFile = Utils.join(Dir.BLOBS, blobId + ".txt");
                Blob blob = Utils.readObject(blobFile, Blob.class);
                File newFileRm = Utils.join(Dir.REMOVE, filename);
                Utils.writeContents(newFileRm, blob.getContents());
                prevFileAdd.delete();
                return;
            } else {
                Utils.exitWithError("File does not exist.");
            }
        }
        if (HEADCommit.getTracking().containsKey(filename)) {
            File newFileRm = Utils.join(Dir.REMOVE, filename);
            Utils.writeContents(newFileRm, Utils.readContents(fileCWD));
            fileCWD.delete();
            prevFileAdd.delete();
        } else {
            if (!prevFileAdd.delete()) {
                Utils.exitWithError("No reason to remove the file.");
            }
        }
    }

    /** Clears the staging area. DANGEROUS */
    public void clear () {
        for (File file : Dir.ADD.listFiles()) {
            file.delete();
        }
        for (File file : Dir.REMOVE.listFiles()) {
            file.delete();
        }
    }
}
