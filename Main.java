package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {


        if (args.length == 0) {
            Utils.exitWithError("PLease enter a command.");
        }

        Repository repo = new Repository();
        Stage stage = repo.getStage();
        String firstArg = args[0];

        switch(firstArg) {
            case "init":
                Utils.validateNumArgs(args, 1);
                repo.init();
                break;
            case "add":
                checkInitialized();
                Utils.validateNumArgs(args, 2);
                stage.add(args[1]);
                break;
            case "rm":
                checkInitialized();
                Utils.validateNumArgs(args, 2);
                stage.rm(args[1]);
                break;
            case "commit":
                checkInitialized();
                Utils.validateNumArgs(args, 2);
                repo.commit(args[1]);
                break;
            case "log":
                checkInitialized();
                Utils.validateNumArgs(args, 1);
                repo.log();
                break;
            case "checkout":
                checkInitialized();
                if (args.length == 2) {
                    repo.checkoutBranch(args[1]);
                    break;
                }
                if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        Utils.exitWithError("Incorrect operands.");
                    }
                    repo.checkout(Repository.getHeadCommitId(), args[2]);
                    break;
                }
                if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        Utils.exitWithError("Incorrect operands.");
                    }
                    repo.checkout(args[1], args[3]);
                    break;
                }
            case "global-log":
                checkInitialized();
                Utils.validateNumArgs(args, 1);
                repo.globalLog();
                break;
            case "status":
                checkInitialized();
                Utils.validateNumArgs(args, 1);
                repo.status();
                break;
            case "find":
                checkInitialized();
                Utils.validateNumArgs(args, 2);
                repo.find(args[1]);
            case "branch":
                checkInitialized();
                Utils.validateNumArgs(args, 2);
                repo.branch(args[1]);
                break;
            case "rm-branch":
                checkInitialized();
                Utils.validateNumArgs(args, 2);
                repo.rmBranch(args[1]);
                break;
            case "reset":
                checkInitialized();
                Utils.validateNumArgs(args, 2);
                repo.reset(args[1]);
                break;
            case "merge":
                checkInitialized();
                Utils.validateNumArgs(args, 2);
                repo.merge(args[1]);
                break;
            default:
                Utils.exitWithError("No command with that name exists.");
        }
    }

    //TODO: checkInitialized method needs Javadoc
    public static void checkInitialized() {
        if (!Dir.GITLET.exists()) {
            Utils.exitWithError("Not in an initialized Gitlet directory.");
        }
    }
}
