package cz.muni.sci.astro.fhm.cli;

/**
 * Main class for CLI
 *
 * @author Jan Hlava, 395986
 */
public class App {
    /**
     * Launch CLI
     *
     * @param args array containing params from command line
     */
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("help")) {
            System.out.println("Call this program with one argument - filename of file which contains commands to execute.");
            System.out.println("Short description of available commands (for more info use help _command_ - for example help file):");
            for (Command command : Command.values()) {
                System.out.println(command + " - " + command.shortDescription());
            }
        } else if (args.length == 2 && args[0].equals("help")) {
            try {
                Command command = Command.valueOf(args[1].toUpperCase());
                System.out.println("Showing help for command " + command + ':');
                System.out.println(command.longDescription());
            } catch (IllegalArgumentException exc) {
                System.err.println("Cannot show help for unknown command " + args[1] + '.');
            }
        } else if (args.length != 1) {
            System.err.println("Wrong count of arguments - program needs 1 argument:");
            System.err.println("Filename of file, you want to process");
            System.err.println("Or use argument help to show info about syntax.");
            System.exit(1);
        } else {
            CommandsInterpreter interpreter = new CommandsInterpreter(args[0]);
            if (!interpreter.run()) {
                System.exit(2);
            } else {
                System.out.println("All commands has been successfully executed.");
            }
        }
    }
}
