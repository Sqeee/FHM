package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsException;
import cz.muni.sci.astro.fits.FitsFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs multiple operation
 *
 * @author Jan Hlava, 395986
 */
public class MultipleOperationsRunner {
    private PrintOutputMethod printerOK;
    private PrintOutputMethod printerError;
    private List<Operation> operations;
    private List<List<String>> filesForOperations;

    /**
     * Creates new instances of this class
     *
     * @param printerOK    method for printing OK things
     * @param printerError method for printing error things
     */
    public MultipleOperationsRunner(PrintOutputMethod printerOK, PrintOutputMethod printerError) {
        this.printerOK = printerOK;
        this.printerError = printerError;
        operations = new ArrayList<>();
        filesForOperations = new ArrayList<>();
    }

    /**
     * Adds new card to files
     *
     * @param files        file names to adding card
     * @param keyword      keyword of new card, cannot be null
     * @param rvalue       real value of new card
     * @param ivalue       imaginary value of new card
     * @param comment      comment of new card
     * @param index        index where new card should be added, null means index is not specified, negative values means indexing from the end, indexing is from 1
     * @param afterKeyword after which keyword should be added, null means no after card is specified, if index is specified, then this must be null
     * @param updateCard   if in case that header unique keyword is already in header should be updated
     */
    public void addCard(List<String> files, String keyword, String rvalue, String ivalue, String comment, Integer index, String afterKeyword, boolean updateCard) {
        OperationAddCard operation;
        try {
            operation = new OperationAddCard(keyword, rvalue, ivalue, comment, index, afterKeyword, updateCard);
        } catch (OperationIllegalArgumentException exc) {
            printerError.print(exc.getMessage());
            return;
        }
        addOperationToQueue(operation, files);
    }

    /**
     * Removes card from files
     *
     * @param files   file names to removing card
     * @param keyword card with this keyword should be deleted, null means keyword is not specified, then for deleting will be used index
     * @param index   index from where card should be deleted, null means index is not specified, negative values means indexing from the end, indexing is from 1, if keyword param is specified, this param must be null, otherwise this param must be specified
     */
    public void removeCard(List<String> files, String keyword, Integer index) {
        OperationRemoveCard operation;
        try {
            operation = new OperationRemoveCard(keyword, index);
        } catch (OperationIllegalArgumentException exc) {
            printerError.print(exc.getMessage());
            return;
        }
        addOperationToQueue(operation, files);
    }

    /**
     * Changes values of card with given keyword
     *
     * @param files                   file names for changing card
     * @param keyword                 card with given keyword will be changed, cannot be null
     * @param newKeyword              new keyword, if it should not change use null
     * @param newRValue               new real value, if it should not change use null
     * @param newIValue               new imaginary value, if it should not change use null
     * @param newComment              new comment, if it should not change use null
     * @param deleteDuplicatedKeyword true if you want to delete card with same keyword (it must be unique header keyword) as new keyword, otherwise false
     */
    public void changeCard(List<String> files, String keyword, String newKeyword, String newRValue, String newIValue, String newComment, boolean deleteDuplicatedKeyword) {
        OperationChangeCard operation;
        try {
            operation = new OperationChangeCard(keyword, newKeyword, newRValue, newIValue, newComment, deleteDuplicatedKeyword);
        } catch (OperationIllegalArgumentException exc) {
            printerError.print(exc.getMessage());
            return;
        }
        addOperationToQueue(operation, files);
    }

    /**
     * Changes index of card with given keyword
     *
     * @param files   file names for changing index
     * @param keyword index of card with given keyword will be changed, cannot be null
     * @param index   new index, negative values means indexing from the end, indexing is from 1, cannot be null or has 0 value
     */
    public void changeIndexCard(List<String> files, String keyword, Integer index) {
        OperationChangeIndex operation;
        try {
            operation = new OperationChangeIndex(keyword, index);
        } catch (OperationIllegalArgumentException exc) {
            printerError.print(exc.getMessage());
            return;
        }
        addOperationToQueue(operation, files);
    }

    /**
     * Concatenates given values to card with given keyword
     *
     * @param files      file names for concatenation
     * @param keyword    keyword of card, where the result will be stored, cannot be null
     * @param values     list of values used for concatenation, every value has prefix identified value type (-s= for string, -k= for keyword values), cannot be null or empty, can contain only allowed prefixes
     * @param glue       glue string used between values, cannot be null
     * @param updateCard if in case that header unique keyword is already in header should be updated
     */
    public void concatenate(List<String> files, String keyword, List<String> values, String glue, boolean updateCard) {
        OperationConcatenate operation;
        try {
            operation = new OperationConcatenate(keyword, values, glue, updateCard);
        } catch (OperationIllegalArgumentException exc) {
            printerError.print(exc.getMessage());
            return;
        }
        addOperationToQueue(operation, files);
    }

    /**
     * Shifts date and time values in card with given keyword
     *
     * @param files     file names for shifting card
     * @param keyword   keyword of card, where the shifting will be done, cannot be null
     * @param intervals list of intervals used for shifting, every interval has prefix identified unit type (example -h= for hour, -y= for year), cannot be null or empty, it can contain only allowed prefixes
     */
    public void shift(List<String> files, String keyword, List<String> intervals) {
        OperationShift operation;
        try {
            operation = new OperationShift(keyword, intervals);
        } catch (OperationIllegalArgumentException exc) {
            printerError.print(exc.getMessage());
            return;
        }
        addOperationToQueue(operation, files);
    }

    /**
     * Computes julian day and saves to card with given keyword
     *
     * @param files         file names for computing julian day
     * @param keyword       keyword of card, where the result will be stored, cannot be null
     * @param sourceKeyword from which keyword we take date time
     * @param dateTime      date time for computing julian day
     * @param updateCard    if in case that header unique keyword is already in header should be updated
     */
    public void jd(List<String> files, String keyword, String sourceKeyword, String dateTime, boolean updateCard) {
        OperationJD operation;
        try {
            operation = new OperationJD(keyword, sourceKeyword, dateTime, updateCard);
        } catch (OperationIllegalArgumentException exc) {
            printerError.print(exc.getMessage());
            return;
        }
        addOperationToQueue(operation, files);
    }

    /**
     * Adds operation to queue
     *
     * @param operation operation which should be added to queue
     * @param files     files fo which operation should be executed
     */
    private void addOperationToQueue(Operation operation, List<String> files) {
        if (files.isEmpty()) {
            printerOK.print("Operation is ignored, because you are working with empty set of files.");
            return;
        }
        operations.add(operation);
        filesForOperations.add(new ArrayList<>(files));
        printerOK.print("Operation was added into operation queue.");
    }

    /**
     * Gets operations in queue
     *
     * @return operations in queue
     */
    public List<Operation> getOperations() {
        return operations;
    }

    /**
     * Returns files for given operation
     *
     * @param operation operation for which you want to know affected files
     * @return list of affected values, if operation is not in queue, then null is returned
     */
    public List<String> getFilesForOperation(Operation operation) {
        int index = operations.indexOf(operation);
        if (index >= 0) {
            return filesForOperations.get(index);
        }
        return null;
    }

    /**
     * Removes operation from queue
     *
     * @param operation operation which should be removed
     */
    public void removeOperation(Operation operation) {
        int index = operations.indexOf(operation);
        if (index >= 0) {
            operations.remove(index);
            filesForOperations.remove(index);
        }
    }

    /**
     * Swaps operations in queue
     *
     * @param operation1 first operation for swapping
     * @param operation2 second operation for swapping
     */
    public void swapOperations(Operation operation1, Operation operation2) {
        int index1 = operations.indexOf(operation1);
        int index2 = operations.indexOf(operation2);
        if (index1 >= 0 && index2 >= 0) {
            Operation operation = operations.set(index1, operation2);
            List<String> files = filesForOperations.set(index1, filesForOperations.get(index2));
            operations.set(index2, operation);
            filesForOperations.set(index2, files);
        }
    }

    /**
     * Executes operations
     */
    public void executeOperations() {
        if (operations.isEmpty()) {
            printerOK.print("No operations was set.");
        }
        Map<String, List<Operation>> fileOperations = new HashMap<>();
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            List<String> files = filesForOperations.get(i);
            for (String file : files) {
                if (fileOperations.containsKey(file)) {
                    fileOperations.get(file).add(operation);
                } else {
                    List<Operation> operations = new ArrayList<>();
                    operations.add(operation);
                    fileOperations.put(file, operations);
                }
            }
        }
        for (Map.Entry<String, List<Operation>> entry : fileOperations.entrySet()) {
            File file = new File(entry.getKey());
            FitsFile fitsFile;
            try {
                fitsFile = new FitsFile(file);
            } catch (FitsException exc) {
                printerError.print("File \"", file.getName(), "\" cannot be opened. Details: ", exc.getMessage(), ".");
                continue;
            }
            printerOK.print();
            printerOK.print("Executing operations on file \"", file.getName(), "\".");
            for (Operation operation : entry.getValue()) {
                operation.execute(fitsFile, printerOK, printerError);
            }
            List<String> problems = fitsFile.checkReadyToSave();
            if (problems.isEmpty()) {
                if (!fitsFile.saveFile()) {
                    printerError.print("Saving file \"", fitsFile.getFilename(), "\" failed.");
                } else {
                    printerOK.print("File \"", fitsFile.getFilename(), "\" was successfully saved.");
                }
            } else {
                printerError.print("Cannot save file \"", fitsFile.getFilename(), "\" due errors:");
                problems.forEach(printerError::print);
            }
            fitsFile.close();
        }
        operations.clear();
        filesForOperations.clear();
    }
}
