package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.List;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    private String message;
    /** Parent Commit of this Commit. */
    private final String parentId;
    /** Timestamp */
    private final Date timestamp;
    /** HashMap of files to track: Key = filename, Value = blob id */
    private HashMap<String, String> tracking;
    /** This Commit's hash */
    private final String id;


    //TODO: Commit constructor needs Javadoc
    //TODO: I don't like the last constructor ~ maybe try boolean original
    public Commit(String parentId, String message, Date timestamp) {
        this.parentId = parentId;
        this.message = message;

        if (timestamp == null) {
            timestamp = new java.util.Date();
        }
        this.timestamp = timestamp;

        if (!(parentId == null)) {
            File parentCommitFile = Utils.join(Dir.COMMITS, parentId + ".txt");
            Commit parentCommit = Utils.readObject(parentCommitFile, Commit.class);
            tracking = parentCommit.getTracking();
        } else {
            tracking = new HashMap<String, String>();
        }

        this.id = Utils.sha1((Object) Utils.serialize(this));
    }

    //TODO: update method needs javadoc
    public void update(Stage stage) {
        List<String> addFiles = Utils.plainFilenamesIn(Dir.ADD);
        List<String> removeFiles = Utils.plainFilenamesIn(Dir.REMOVE);
        if (addFiles.isEmpty() & removeFiles.isEmpty()) {
            Utils.exitWithError("No changes added to the commit.");
        }

        for (String filename : addFiles) {
            File addFile = Utils.join(Dir.ADD, filename);
            Blob newBlob = new Blob(addFile);
            tracking.put(filename, newBlob.getId());
            addFile.delete();
        }

        for (String filename : removeFiles) {
            File removeFile = Utils.join(Dir.REMOVE, filename);
            tracking.remove(filename);
            removeFile.delete();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Commit)) {
            return false;
        }
        return (((Commit) o).getId().equals(id));
    }

    public void print() {
        System.out.println("===");
        System.out.println("commit " + id);
        System.out.println("Date: " + Utils.formatTimestamp(timestamp));
        System.out.println(message);
        System.out.println();
    }

    public String getParentId() {
        return parentId;
    }

    public String getMessage() {
        return message;
    }

    public HashMap<String, String> getTracking() {
        return tracking;
    }

    public String getId() {
        return id;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
