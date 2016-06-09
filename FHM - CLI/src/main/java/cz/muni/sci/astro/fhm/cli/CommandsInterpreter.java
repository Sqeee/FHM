package cz.muni.sci.astro.fhm.cli;

import cz.muni.sci.astro.fhm.core.MultipleOperationsRunner;
import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsException;
import cz.muni.sci.astro.fits.FitsFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprets commands in given file
 *
 * @author Jan Hlava, 395986
 */
public class CommandsInterpreter {
    private static final int COMMENT_GROUP = 1;
    private static final Pattern PARAM_SEPARATOR = Pattern.compile("(?: (?:([\\S&&[^\"]]+)|(?:\"((?:\\\\\"|[\\S &&[^\"]])*)\")))");
    private static final Pattern PARAM_VALUE_SEPARATOR = Pattern.compile("(?: (?:(-[a-z]+ [\\S&&[^\"]]+)|(?:(-[a-z]+ \"(?:(?:\\\\\"|[\\S &&[^\"]])*)\"))))");
    private static final String COMMENT_CHAR = "#";
    private static final String PARAM_USE_PREVIOUS_FILTER = "-p";
    private static final String PARAM_UPDATE_CARD = "-u";
    private static final String PARAM_DELETE_DUPLICATED_CARD = "-d";
    private static final int PARAM_ADD_KEYWORD_INDEX = 0;
    private static final int PARAM_ADD_RVALUE_INDEX = 1;
    private static final int PARAM_ADD_IVALUE_INDEX = 3;
    private static final int PARAM_ADD_COMMENT_INDEX = 2;

    private final Pattern commandValidatorPattern;
    private final String filename;
    private final List<String> commands;
    private final List<String> openedFiles;
    private final List<String> filteredFiles;
    private Matcher matcherParam;
    private boolean allOK = true;
    private MultipleOperationsRunner operationsRunner;

    /**
     * Creates new instances of this class
     *
     * @param filename filename of file to process
     */
    public CommandsInterpreter(String filename) {
        this.filename = filename;
        StringJoiner regex = new StringJoiner("|", "\\A(?:(?:(" + COMMENT_CHAR + ").*)|", ")\\z");
        for (Command command : Command.values()) {
            regex.add(command.pattern());
        }
        commandValidatorPattern = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
        commands = new ArrayList<>();
        openedFiles = new ArrayList<>();
        filteredFiles = new ArrayList<>();
        operationsRunner = new MultipleOperationsRunner(this::printOK, this::printError);
    }

    /**
     * Runs command interpreter, reads commands from file and executes them
     *
     * @return true if all commands were successfully interpreted, otherwise false
     */
    public boolean run() {
        String line;
        int lineNumber = 1;
        try (BufferedReader scanner = new BufferedReader(new FileReader(filename))) {
            while ((line = scanner.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }
                Matcher commandValidator = commandValidatorPattern.matcher(line);
                if (commandValidator.matches()) {
                    if (commandValidator.group(COMMENT_GROUP) != null) {
                        lineNumber++;
                        continue;
                    }
                    commands.add(line);
                } else {
                    int indexEndCommand = line.indexOf(' ');
                    if (indexEndCommand == -1) {
                        indexEndCommand = line.length();
                    }
                    try {
                        Command command = Command.valueOf(line.substring(0, indexEndCommand).toUpperCase());
                        printError("On line ", Integer.toString(lineNumber), " is bad usage of command - ", command.toString(), ". Do not forget to enclose values with spaces into \"\". Right params: ");
                        printError(command.toString(), " ", command.paramList());
                        printError("Your command: ", line);
                    } catch (IllegalArgumentException exc) {
                        printError("On line ", Integer.toString(lineNumber), " is unknown command - ", line.substring(0, indexEndCommand), ".");
                    }
                }
                lineNumber++;
            }
        } catch (IOException exc) {
            printError("Cannot read file with commands. Details: ", exc.getMessage());
            return false;
        }
        if (allOK) {
            processCommands();
        }
        return allOK;
    }

    /**
     * Processes and executes commands
     */
    private void processCommands() {
        for (String command : commands) {
            printOK("Command: ", command);
            List<String> params = new ArrayList<>();
            boolean modifyParam;
            matcherParam = PARAM_SEPARATOR.matcher(command);
            int commandEndIndex = command.indexOf(' ');
            if (commandEndIndex == -1) {
                commandEndIndex = command.length();
            }
            switch (Command.valueOf(command.substring(0, commandEndIndex).toUpperCase())) {
                case FILE:
                    handleOpenedFilesBeforeOpen();
                    while (matcherParam.find()) {
                        openFile(getParam());
                    }
                    break;
                case DIR:
                    handleOpenedFilesBeforeOpen();
                    matcherParam.find();
                    openDir(getParam());
                    break;
                case CANCEL_FILTERS:
                    printOK("All used filters canceled.");
                    filteredFiles.clear();
                    filteredFiles.addAll(openedFiles);
                    break;
                case FILTER_FILENAME:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_USE_PREVIOUS_FILTER);
                    filterFilename(getParam(), modifyParam);
                    break;
                case FILTER_KEYWORD:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_USE_PREVIOUS_FILTER);
                    do {
                        params.add(getParam());
                    } while (matcherParam.find());
                    filterKeywords(params, modifyParam);
                    break;
                case FILTER_KEYWORD_RVALUE:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_USE_PREVIOUS_FILTER);
                    params.add(getParam());
                    matcherParam.find();
                    filterKeywordRValue(params.get(0), getParam(), modifyParam);
                    break;
                case FILTER_KEYWORD_IVALUE:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_USE_PREVIOUS_FILTER);
                    params.add(getParam());
                    matcherParam.find();
                    filterKeywordIValue(params.get(0), getParam(), modifyParam);
                    break;
                case ADD_CARD:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_UPDATE_CARD);
                    params.add(getParam());
                    while (matcherParam.find()) {
                        params.add(getParam());
                    }
                    for (int i = params.size(); i <= 4; i++) {
                        params.add(null);
                    }
                    operationsRunner.addCard(filteredFiles, params.get(PARAM_ADD_KEYWORD_INDEX), params.get(PARAM_ADD_RVALUE_INDEX), params.get(PARAM_ADD_IVALUE_INDEX), params.get(PARAM_ADD_COMMENT_INDEX), null, null, modifyParam);
                    break;
                case ADD_CARD_INDEX:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_UPDATE_CARD);
                    int index = Integer.parseInt(getParam());
                    while (matcherParam.find()) {
                        params.add(getParam());
                    }
                    for (int i = params.size(); i <= 4; i++) {
                        params.add(null);
                    }
                    operationsRunner.addCard(filteredFiles, params.get(PARAM_ADD_KEYWORD_INDEX), params.get(PARAM_ADD_RVALUE_INDEX), params.get(PARAM_ADD_IVALUE_INDEX), params.get(PARAM_ADD_COMMENT_INDEX), index, null, modifyParam);
                    break;
                case ADD_CARD_AFTER_KEYWORD:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_UPDATE_CARD);
                    String afterCard = getParam();
                    while (matcherParam.find()) {
                        params.add(getParam());
                    }
                    for (int i = params.size(); i <= 4; i++) {
                        params.add(null);
                    }
                    operationsRunner.addCard(filteredFiles, params.get(PARAM_ADD_KEYWORD_INDEX), params.get(PARAM_ADD_RVALUE_INDEX), params.get(PARAM_ADD_IVALUE_INDEX), params.get(PARAM_ADD_COMMENT_INDEX), null, afterCard, modifyParam);
                    break;
                case REMOVE_CARD:
                    matcherParam.find();
                    operationsRunner.removeCard(filteredFiles, getParam(), null);
                    break;
                case REMOVE_CARD_INDEX:
                    matcherParam.find();
                    operationsRunner.removeCard(filteredFiles, null, Integer.parseInt(getParam()));
                    break;
                case CHANGE_KEYWORD:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_DELETE_DUPLICATED_CARD);
                    do {
                        params.add(getParam());
                    } while (matcherParam.find());
                    operationsRunner.changeCard(filteredFiles, params.get(0), params.get(1), null, null, null, modifyParam);
                    break;
                case CHANGE_RVALUE:
                    while (matcherParam.find()) {
                        params.add(getParam());
                    }
                    operationsRunner.changeCard(filteredFiles, params.get(0), null, params.get(1), null, null, false);
                    break;
                case CHANGE_IVALUE:
                    while (matcherParam.find()) {
                        params.add(getParam());
                    }
                    operationsRunner.changeCard(filteredFiles, params.get(0), null, null, params.get(1), null, false);
                    break;
                case CHANGE_COMMENT:
                    while (matcherParam.find()) {
                        params.add(getParam());
                    }
                    operationsRunner.changeCard(filteredFiles, params.get(0), null, null, null, params.get(1), false);
                    break;
                case CHANGE_INDEX:
                    matcherParam.find();
                    params.add(getParam());
                    matcherParam.find();
                    index = Integer.parseInt(getParam());
                    operationsRunner.changeIndexCard(filteredFiles, params.get(0), index);
                    break;
                case CONCATENATE:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_UPDATE_CARD);
                    String keyword = getParam();
                    matcherParam.usePattern(PARAM_VALUE_SEPARATOR);
                    while (matcherParam.find()) {
                        params.add(getParamAndValue());
                    }
                    operationsRunner.concatenate(filteredFiles, keyword, params, "", modifyParam);
                    break;
                case SHIFT:
                    matcherParam.find();
                    keyword = getParam();
                    matcherParam.usePattern(PARAM_VALUE_SEPARATOR);
                    while (matcherParam.find()) {
                        params.add(getParamAndValue());
                    }
                    operationsRunner.shift(filteredFiles, keyword, params);
                    break;
                case JD:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_UPDATE_CARD);
                    keyword = getParam();
                    matcherParam.find();
                    operationsRunner.jd(filteredFiles, keyword, null, getParam(), modifyParam);
                    break;
                case JD_KEYWORD:
                    matcherParam.find();
                    modifyParam = getUsedSpecialParam(PARAM_UPDATE_CARD);
                    keyword = getParam();
                    matcherParam.find();
                    operationsRunner.jd(filteredFiles, keyword, getParam(), null, modifyParam);
                    break;
            }
            printOK();
        }
        printOK("Executing operations on exit:");
        executeOperations();
    }

    /**
     * Opens given filename and store it into collection
     *
     * @param filename filename of file, which should be opened
     */
    private void openFile(String filename) {
        if (filename.indexOf(".\\") == 0) {
            filename = filename.substring(2);
        }
        try (FitsFile ignored = new FitsFile(new File(filename))) {
            if (!openedFiles.contains(filename)) {
                openedFiles.add(filename);
                filteredFiles.add(filename);
                printOK("File \"", filename, "\" was opened.");
            } else {
                printError("File \"", filename, "\" is already opened.");
            }
        } catch (FitsException exc) {
            printError("File \"", filename, "\" cannot be opened. Details: ", exc.getMessage(), ".");
        }
    }

    /**
     * Opens files in given dir
     *
     * @param directory dir containing desired files
     */
    private void openDir(String directory) {
        printOK("Loading files in directory \"", directory, "\".");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(directory))) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && FitsFile.isFitsFile(file.toFile())) {
                    openFile(file.toString());
                } else if (!Files.isDirectory(file)) {
                    printOK("File \"", file.getFileName().toString(), "\" was skipped, because it is not Fits file.");
                }
            }
            if (openedFiles.isEmpty()) {
                printOK("Directory is empty.");
            }
        } catch (IOException exc) {
            if (exc instanceof NoSuchFileException) {
                printError("Loading directory \"", directory, "\" failed because directory was not found.");
            } else {
                printError("Loading directory \"", directory, "\" failed. Details: ", exc.getMessage(), ".");
            }
        }
    }

    /**
     * Filters opened files by given filter
     *
     * @param filter             filter to filter files
     * @param usePreviousFilters if should we used previous files
     */
    private void filterFilename(String filter, boolean usePreviousFilters) {
        printOK("Filtering files by filename: ", filter);
        List<String> files = new ArrayList<>();
        if (usePreviousFilters) {
            files.addAll(filteredFiles);
        } else {
            files.addAll(openedFiles);
        }
        filteredFiles.clear();
        PathMatcher filenameMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filter);
        for (String filename : files) {
            File file = new File(filename);
            if (filenameMatcher.matches(Paths.get(file.getName()))) {
                filteredFiles.add(filename);
            } else {
                printOK("File \"", file.getName(), "\" does not match filter.");
            }
        }
        printFilteringResult();
    }

    /**
     * Filters opened files by containment keywords
     *
     * @param keywords           list of keywords, which should be contained in files
     * @param usePreviousFilters if should we used previous files
     */
    private void filterKeywords(List<String> keywords, boolean usePreviousFilters) {
        StringJoiner stringJoiner = new StringJoiner(", ", "Filtering files by keywords: ", "");
        keywords.forEach(stringJoiner::add);
        printOK(stringJoiner.toString());
        List<String> files = new ArrayList<>();
        if (usePreviousFilters) {
            files.addAll(filteredFiles);
        } else {
            files.addAll(openedFiles);
        }
        filteredFiles.clear();
        for (String filename : files) {
            try (FitsFile fitsFile = new FitsFile(new File(filename))) {
                boolean containsAll = true;
                for (String keyword : keywords) {
                    if (fitsFile.getCardsWithKeyword(keyword).isEmpty()) {
                        containsAll = false;
                        break;
                    }
                }
                if (containsAll) {
                    filteredFiles.add(filename);
                } else {
                    printOK("File \"", fitsFile.getFilename(), "\" does not match filter.");
                }
            } catch (FitsException ignored) {
            } // Check for Fits file has been already done
        }
        printFilteringResult();
    }

    /**
     * Filters opened files by containment keywords with given real value
     *
     * @param keyword            keyword which should be contained in files
     * @param rValue             real value which should have given keyword
     * @param usePreviousFilters if should we used previous files
     */
    private void filterKeywordRValue(String keyword, String rValue, boolean usePreviousFilters) {
        printOK("Filtering files by containment of keyword ", keyword, " and real value \"", rValue, "\".");
        List<String> files = new ArrayList<>();
        if (usePreviousFilters) {
            files.addAll(filteredFiles);
        } else {
            files.addAll(openedFiles);
        }
        filteredFiles.clear();
        for (String filename : files) {
            try (FitsFile fitsFile = new FitsFile(new File(filename))) {
                List<FitsCard> cards = fitsFile.getCardsWithKeyword(keyword);
                if (!cards.isEmpty() && cards.get(0).getRValueString().equals(rValue)) {
                    filteredFiles.add(filename);
                } else {
                    printOK("File \"", fitsFile.getFilename(), "\" does not match filter.");
                }
            } catch (FitsException ignored) {
            } // Check for Fits file has been already done
        }
        printFilteringResult();
    }

    /**
     * Filters opened files by containment keywords with given imaginary value
     *
     * @param keyword            keyword which should be contained in files
     * @param iValue             imaginary value which should have given keyword
     * @param usePreviousFilters if should we used previous files
     */
    private void filterKeywordIValue(String keyword, String iValue, boolean usePreviousFilters) {
        printOK("Filtering files by containment of keyword ", keyword, " and imaginary value \"", iValue, "\".");
        List<String> files = new ArrayList<>();
        if (usePreviousFilters) {
            files.addAll(filteredFiles);
        } else {
            files.addAll(openedFiles);
        }
        filteredFiles.clear();
        for (String filename : files) {
            try (FitsFile fitsFile = new FitsFile(new File(filename))) {
                List<FitsCard> cards = fitsFile.getCardsWithKeyword(keyword);
                if (!cards.isEmpty() && cards.get(0).getIValueString().equals(iValue)) {
                    filteredFiles.add(filename);
                } else {
                    printOK("File \"", fitsFile.getFilename(), "\" does not match filter.");
                }
            } catch (FitsException ignored) {
            } // Check for Fits file has been already done
        }
        printFilteringResult();
    }

    /**
     * Returns param from regex operation
     *
     * @return param from regex operation
     */
    private String getParam() {
        if (matcherParam.group(1) != null) {
            return matcherParam.group(1);
        } else {
            return matcherParam.group(2);
        }
    }

    /**
     * Returns param with value from regex operation
     *
     * @return param with value from regex operation
     */
    private String getParamAndValue() {
        if (matcherParam.group(1) != null) {
            return matcherParam.group(1);
        } else {
            String result = matcherParam.group(2).replaceFirst("\"", "");
            return result.substring(0, result.length() - 1);
        }
    }

    /**
     * Returns if is used given param
     *
     * @param param which param should be used
     * @return true if is used given param, otherwise false
     */
    private boolean getUsedSpecialParam(String param) {
        if (getParam().equals(param)) {
            matcherParam.find();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Executes operations
     */
    private void executeOperations() {
        operationsRunner.executeOperations();
        openedFiles.clear();
        filteredFiles.clear();
    }

    /**
     * Handles opened files before opening new ones
     */
    private void handleOpenedFilesBeforeOpen() {
        if (!openedFiles.isEmpty()) {
            printOK("Executing operations on opened files before opening new ones.");
            executeOperations();
        }
    }

    /**
     * Prints empty message to output
     */
    private void printOK() {
        System.out.println();
    }

    /**
     * Prints message to output
     *
     * @param partsMessage parts of message which should be printed
     */
    private void printOK(String... partsMessage) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : partsMessage) {
            stringBuilder.append(part);
        }
        System.out.println(stringBuilder.toString());
    }

    /**
     * Prints error message to output
     *
     * @param partsMessage parts of message which should be printed
     */
    private void printError(String... partsMessage) {
        allOK = false;
        StringBuilder stringBuilder = new StringBuilder("ERROR: ");
        for (String part : partsMessage) {
            stringBuilder.append(part);
        }
        System.out.println(stringBuilder.toString());
    }

    /**
     * Prints result of filtering
     */
    private void printFilteringResult() {
        if (!filteredFiles.isEmpty()) {
            StringJoiner stringJoiner = new StringJoiner("\", \"", "Result of applying filter: \"", "\"");
            filteredFiles.forEach(file -> stringJoiner.add(new File(file).getName()));
            printOK(stringJoiner.toString());
        } else {
            printOK("Result of applying filter is empty.");
        }
    }
}
