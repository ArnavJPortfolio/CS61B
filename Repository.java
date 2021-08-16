package gitlet;

import java.io.File;
import java.util.*;


/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /** Name of the current head branch. */
    private String head;
    /** Staging Area. */
    private Stage stage;
    /** id of the commit of the current head branch. */
    private static String HEAD_COMMIT_ID;

    //TODO: Repository constructor needs javadoc
    public Repository() {
        if (Dir.GITLET.exists()) {
            head = Utils.readContentsAsString(Utils.join(Dir.BRANCHES, "HEAD.txt"));
            stage = new Stage();
            HEAD_COMMIT_ID = Utils.readContentsAsString(Utils.join(Dir.BRANCHES, head + ".txt"));
        }
    }

    //TODO: init() needs Javadoc
    public void init() {
        if (!Dir.GITLET.exists()) {
            // Directory Creation
            Dir.GITLET.mkdir();
            Dir.BRANCHES.mkdir();
            Dir.COMMITS.mkdir();
            Dir.BLOBS.mkdir();
            Dir.STAGE.mkdir();
            Dir.ADD.mkdir();
            Dir.REMOVE.mkdir();
            // Initial Commit Creation
            Commit initialCommit = new Commit(null, "initial commit", new Date(0));
            File initialCommitFile = Utils.join(Dir.COMMITS, initialCommit.getId() + ".txt");
            Utils.writeObject(initialCommitFile, initialCommit);
            // Branch Creation; by default HEAD is saved as master
            File masterBranchFile = Utils.join(Dir.BRANCHES, "master.txt");
            Utils.writeContents(masterBranchFile, initialCommit.getId());
            File HEADTrackerFile = Utils.join(Dir.BRANCHES, "HEAD.txt");
            Utils.writeContents(HEADTrackerFile, "master");

        } else {
            Utils.exitWithError("A Gitlet version-control system already exists in the current directory.");
        }
    }

    //TODO: commit method needs javadoc
    public void commit(String message) {
        if (message.isEmpty()) {
            Utils.exitWithError("Please enter a commit message.");
        }
        Commit newCommit = new Commit(HEAD_COMMIT_ID, message, null);
        newCommit.update(stage);
        File newCommitFile = Utils.join(Dir.COMMITS, newCommit.getId() + ".txt");
        Utils.writeObject(newCommitFile, newCommit);
        File HEADBranchFile = Utils.join(Dir.BRANCHES, head + ".txt");
        Utils.writeContents(HEADBranchFile, newCommit.getId());
    }

    public void branch(String name) {
        File newBranchFile = Utils.join(Dir.BRANCHES, name + ".txt");
        if (newBranchFile.exists()) {
            Utils.exitWithError("A branch with that name already exists.");
        }
        Utils.writeContents(newBranchFile, HEAD_COMMIT_ID);
    }

    public void rmBranch(String name) {
        if (head.equals(name)) {
            Utils.exitWithError("Cannot remove the current branch.");
        }
        File oldBranchFile = Utils.join(Dir.BRANCHES, name + ".txt");
        if (!oldBranchFile.exists()) {
            Utils.exitWithError("A branch with that name does not exist.");
        }
        oldBranchFile.delete();
    }


    // Source: https://stackoverflow.com/questions/49556021/convert-current-time-to-pst-and-compare-with-another-date
    public void log() {
        Commit commit = Utils.readObject(Utils.join(Dir.COMMITS, HEAD_COMMIT_ID + ".txt"), Commit.class);
        while (true) {
            commit.print();
            if (commit.getParentId() == null) { break; }
            File nextCommitFile = Utils.join(Dir.COMMITS, commit.getParentId() + ".txt");
            commit = Utils.readObject(nextCommitFile, Commit.class);
        }
    }

    public void globalLog() {
        List<String> commits = Utils.plainFilenamesIn(Dir.COMMITS);
        for (String commitString: commits) {
            Commit commit = Utils.readObject(Utils.join(Dir.COMMITS, commitString), Commit.class);
            commit.print();
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        for (String branch : Utils.plainFilenamesIn(Dir.BRANCHES)) {
            if (!branch.equals("HEAD.txt")) {
                String substring = branch.substring(0, branch.length() - 4);
                if (substring.equals(head)) {
                    System.out.println("*" + substring);
                } else {
                    System.out.println(substring);
                }
            }
        }
        HashSet<String> files = new HashSet<>();
        files.addAll(Utils.plainFilenamesIn(Dir.CWD));
        files.addAll(Utils.plainFilenamesIn(Dir.REMOVE));
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String file : Utils.plainFilenamesIn(Dir.ADD)) {
            System.out.println(file);
            files.remove(file);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String file : Utils.plainFilenamesIn(Dir.REMOVE)) {
            System.out.println(file);
            files.remove(file);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        ArrayList<String> trash = new ArrayList<>();
        for (String file : files) {
            if (modified(file)) {
                System.out.println(file);
                trash.add(file);
            }
        }

        System.out.println();
        Commit headCommit = returnCommit(HEAD_COMMIT_ID);
        HashMap<String, String> commitMap = headCommit.getTracking();
        System.out.println("=== Untracked Files ===");
        for (String file : files) {
            if (commitMap.get(file) == null & !trash.contains(file)) {
                System.out.println(file);
            }
        }
        System.out.println();
    }

    public void find(String message) {
        boolean found = false;
        List<String> commits = Utils.plainFilenamesIn(Dir.COMMITS);
        for (String commitString: commits) {
            Commit commit = Utils.readObject(Utils.join(Dir.COMMITS, commitString), Commit.class);
            if (commit.getMessage().equals(message)) {
                found = true;
                System.out.println(commit.getId());
            }
        }
        if (!found) {
            Utils.exitWithError("Found no commit with that message.");
        }
    }

    public void reset (String commitId) {
        Commit commit = returnCommit(commitId);
        if (commit == null) {
            Utils.exitWithError("No commit with that id exists.");
        }
        checkoutCommit(commit);
        File HEADBranchFile = Utils.join(Dir.BRANCHES, head + ".txt");
        Utils.writeContents(HEADBranchFile, commit.getId());
    }

    public void checkout(String commitId, String filename) {
        Commit commit = returnCommit(commitId);
        if (commit == null) {
            Utils.exitWithError("No commit with that id exists.");
        }
        checkoutFile(commit, filename);
    }

    public void checkoutBranch(String branchName) {
        if (branchName.equals(head)) {
            Utils.exitWithError("No need to checkout the current branch.");
        }
        File branch = null;
        for (String b : Utils.plainFilenamesIn(Dir.BRANCHES)) {
            if (!b.equals("HEAD.txt") & b.equals(branchName + ".txt")) {
                branch = Utils.join(Dir.BRANCHES, b);
            }
        }
        if (branch == null) {
            Utils.exitWithError("No such branch exists.");
        }

        String commitId = Utils.readContentsAsString(branch);
        File commitFile = Utils.join(Dir.COMMITS, commitId + ".txt");
        Commit commit = Utils.readObject(commitFile, Commit.class);
        checkoutCommit(commit);
        File HEADTrackerFile = Utils.join(Dir.BRANCHES, "HEAD.txt");
        Utils.writeContents(HEADTrackerFile, branchName);
    }

    private void checkoutCommit(Commit commit) {
        //TODO: big bug: if you create a file that has the same name as a file in the commit that you want to checkout it overwrites
        // it should error!
        Commit headCommit = returnCommit(HEAD_COMMIT_ID);
        for (String filename : headCommit.getTracking().keySet()) {
            if (modified(filename)) {
                Utils.exitWithError("There is an untracked file in the way;" +
                        " delete it, or add and commit it first.");
            }
        }
        for (String filename : headCommit.getTracking().keySet()) {
            Utils.join(Dir.CWD, filename).delete();
        }

        for (String filename : commit.getTracking().keySet()) {
            if (Utils.join(Dir.CWD, filename).exists()) {
                Utils.exitWithError("There is an untracked file in the way;" +
                        " delete it, or add and commit it first.");
            }
            checkoutFile(commit, filename);
        }
        stage.clear();
    }

    private void checkoutFile(Commit commit, String filename) {
        String BlobId = commit.getTracking().get(filename);
        if (BlobId == null) {
            Utils.exitWithError("File does not exist in that commit.");
        }

        Utils.join(Dir.ADD, filename).delete();
        Utils.join(Dir.REMOVE, filename).delete();

        File commitBlobFile = Utils.join(Dir.BLOBS, BlobId + ".txt");
        Blob commitBlob = Utils.readObject(commitBlobFile, Blob.class);

        File currFile = Utils.join(Dir.CWD, filename);
        Utils.writeContents(currFile, (Object) commitBlob.getContents());
    }

    //TODO: Needs Javadoc BTW THIS WORKS FOR ABBREViATED TOO
    private Commit returnCommit(String commitId) {
        String commitFilename = commitId + ".txt";
        if (commitId.length() < 40) {
            List<String> commits = Utils.plainFilenamesIn(Dir.COMMITS);
            for (String commit : commits) {
                if (commit.indexOf(commitId) == 0) {
                    commitFilename = commit;
                    break;
                }
            }
        }
        File commitFile = Utils.join(Dir.COMMITS, commitFilename);
        if (commitFile.exists()) {
            return Utils.readObject(commitFile, Commit.class);
        }
        return null;
    }


    //Returns true if the file in the CWD has been modified from the head commit
    //Returns false if the file doesn't exist in the CWD
    //Returns false if the file isn't being tracked in the head commit
    private boolean modified(String filename) {
        Commit headCommit = returnCommit(HEAD_COMMIT_ID);
        String BlobId = headCommit.getTracking().get(filename);
        if (BlobId == null) {
            return false;
        }
        File commitBlobFile = Utils.join(Dir.BLOBS, BlobId + ".txt");
        Blob commitBlob = Utils.readObject(commitBlobFile, Blob.class);
        File checkFile = Utils.join(Dir.CWD, filename);
        if (!checkFile.exists()) {
            return false;
        }
        if (Utils.readContentsAsString(checkFile).equals(commitBlob.getContents())) {
            return false;
        } else {
            return true;
        }
    }

    public void merge(String branchName) {
        if (!Utils.plainFilenamesIn(Dir.ADD).isEmpty() | !Utils.plainFilenamesIn(Dir.REMOVE).isEmpty()) {
            Utils.exitWithError("You have uncommitted changes.");
        }

        if (branchName.equals(head)) {
            Utils.exitWithError("Cannot merge a branch with itself.");
        }
        File branchFile = Utils.join(Dir.BRANCHES, branchName + ".txt");
        if (!branchFile.exists()) {
            Utils.exitWithError("A branch with that name does not exist.");
        }

        Commit headCommit = returnCommit(HEAD_COMMIT_ID);
        Commit givenCommit = returnCommit(Utils.readContentsAsString(branchFile));
        Commit splitCommit = splitPoint(headCommit, givenCommit);
        HashMap<String, String> headMap = headCommit.getTracking();
        HashMap<String, String> givenMap = givenCommit.getTracking();
        HashMap<String, String> splitMap = splitCommit.getTracking();

        if (splitCommit.equals(givenCommit)) {
            Utils.exitWithError("Given branch is an ancestor of the current branch.");
        }

        if (splitCommit.equals(headCommit)) {
            checkoutBranch(branchName);
            Utils.exitWithError("Current branch fast-forwarded.");
        }

        for (String filename : givenMap.keySet()) {
            File fileCWD = Utils.join(Dir.CWD, filename);
            String headBlobId = headMap.get(filename);
            String givenBlobId = givenMap.get(filename);
            String splitBlobId = splitMap.get(filename);

            if (headBlobId != null & givenBlobId != null & splitBlobId != null) {  // #1
                if(headBlobId.equals(splitBlobId) & !headBlobId.equals(givenBlobId)){
                    checkoutFile(givenCommit, filename);
                    File newFileAdd = Utils.join(Dir.ADD, filename);
                    Utils.writeContents(newFileAdd, Utils.readContents(fileCWD));
                }

                if (!headBlobId.equals(splitBlobId) & givenBlobId.equals(splitBlobId)) { // #2
                    continue;
                }

                if (headBlobId.equals(givenBlobId) & !headBlobId.equals(splitBlobId) & !givenBlobId.equals(splitBlobId)) { // #3
                    continue;
                }
            }


            if (splitBlobId == null & headBlobId == null) { // #5
                checkoutFile(givenCommit, filename);
                File newFileAdd = Utils.join(Dir.ADD, filename);
                Utils.writeContents(newFileAdd, Utils.readContents(fileCWD));
            }

            if (givenBlobId.equals(splitBlobId) & headBlobId == null) { // #7
                continue;
            }

            if(headBlobId != null & givenBlobId != null){
                if (!headBlobId.equals(givenBlobId)) {
                    File givenBlobFile = Utils.join(Dir.BLOBS, givenBlobId + ".txt");
                    Blob givenBlob = Utils.readObject(givenBlobFile, Blob.class);
                    conflict(fileCWD, givenBlob);
                }
            }
        }

        for (String filename: splitMap.keySet()) { //#6
            File fileCWD = Utils.join(Dir.CWD, filename);
            String headBlobId = headMap.get(filename);
            String givenBlobId = givenMap.get(filename);
            String splitBlobId = splitMap.get(filename);
            if (splitBlobId.equals(headBlobId) & givenBlobId == null) {
                File newFileRm = Utils.join(Dir.REMOVE, filename);
                Utils.writeContents(newFileRm, Utils.readContents(fileCWD));
                fileCWD.delete();
            }
        }
    }

    public void conflict(File fileCWD, Blob givenBlob) {
        String contents = "<<<<<<< HEAD\n";
        if (fileCWD.exists()) {
            contents = contents + Utils.readContentsAsString(fileCWD) + "\n";
        } else {
            contents = contents + '\n';
        }
        contents = contents + "=======\n";
        if (givenBlob != null) {
            contents = contents + givenBlob.getContents() + "\n";
        } else {
            contents = contents + "\n";
        }
        contents = contents + ">>>>>>>";
        Utils.writeContents(fileCWD, contents);
    }

    public Commit splitPoint(Commit a, Commit b) {
        Commit curr = a;
        HashSet<String> track = new HashSet<>();
        while (curr.getParentId() != null) {
            track.add(curr.getId());
            curr = returnCommit(curr.getParentId());
        }
        curr = b;
        while (curr.getParentId() != null) {
            if (track.contains(curr.getId())) {
                return curr;
            }
            curr = returnCommit(curr.getParentId());
        }
        return null;
    }

    public static String getHeadCommitId() {
        return HEAD_COMMIT_ID;
    }

    public Stage getStage() {
        return stage;
    }
}
