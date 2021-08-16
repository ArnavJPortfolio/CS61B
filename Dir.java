package gitlet;

import java.io.File;

import static gitlet.Utils.join;
//TODO: Directories Class needs Javadoc
public class Dir {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET = Utils.join(CWD, ".gitlet");

    /** The .gitlet/branches directory. */
    public static final File BRANCHES = Utils.join(GITLET, "branches");

    /** The .gitlet/commits directory. */
    public static final File COMMITS = Utils.join(GITLET, "commits");

    /** The .gitlet/blobs directory. */
    public static final File BLOBS = Utils.join(GITLET, "blobs");

    /** The .gitlet/stage directory. */
    public static final File STAGE = Utils.join(GITLET, "stage");

    /** The .gitlet/stage/add directory. */
    public static final File ADD = Utils.join(STAGE, "add");

    /** The .gitlet/stage/remove directory. */
    public static final File REMOVE = Utils.join(STAGE, "remove");

}
