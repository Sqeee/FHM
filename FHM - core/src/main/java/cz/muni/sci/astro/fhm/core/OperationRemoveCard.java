package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsFile;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represents remove card operation
 *
 * @author Jan Hlava, 395986
 */
public class OperationRemoveCard implements Operation {
    private String keyword;
    private Integer index;

    /**
     * Creates new instance of this class
     *
     * @param keyword card with this keyword should be deleted, null means keyword is not specified, then for deleting will be used index
     * @param index   index from where card should be deleted, null means index is not specified, negative values means indexing from the end, indexing is from 1, if keyword param is specified, this param must be null, otherwise this param must be specified
     * @throws OperationIllegalArgumentException if argument has invalid value
     */
    public OperationRemoveCard(String keyword, Integer index) throws OperationIllegalArgumentException {
        if (index != null && index == 0) {
            throw new OperationIllegalArgumentException("Index 0 is invalid.");
        } else if (index == null && keyword == null) {
            throw new OperationIllegalArgumentException("For deleting must be specified index or keyword.");
        } else if (index != null && keyword != null) {
            throw new OperationIllegalArgumentException("For deleting cannot be specified both params.");
        }
        this.keyword = keyword;
        this.index = index;
    }

    /**
     * Executes add card operation
     *
     * @param file         file for executing operation
     * @param printerOK    method for printing OK things
     * @param printerError method for printing error things
     */
    @Override
    public void execute(FitsFile file, PrintOutputMethod printerOK, PrintOutputMethod printerError) {
        Integer newIndex = index;
        List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
        if (newIndex != null && Math.abs(newIndex) > cards.size()) {
            printerError.print("File \"", file.getFilename(), "\" does not have index ", Integer.toString(newIndex), ", maximum index is ", Integer.toString(cards.size()), ".");
            return;
        } else if (keyword != null) {
            List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
            if (cardsWithKeyword.isEmpty()) {
                printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", keyword, ".");
                return;
            } else {
                newIndex = cards.indexOf(cardsWithKeyword.get(0)) + 1; // Indexing is shifted by +1
            }
        }
        if (newIndex < 0) {
            newIndex += cards.size(); // convert end index to classical index
        } else {
            newIndex--; // fix different indexing -> java starts with 0, users with 1
        }
        FitsCard deletedCard = cards.remove(newIndex.intValue());
        printerOK.print("Card with keyword ", deletedCard.getKeywordName(), " was deleted from file \"", file.getFilename(), "\".");
        file.getHDU(0).getHeader().setCards(cards);
    }

    /**
     * Returns string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner("; ", "Operation remove card: ", ".");
        if (keyword != null) {
            stringJoiner.add("keyword - \"" + keyword + "\"");
        } else {
            stringJoiner.add("index - " + index);
        }
        return stringJoiner.toString();
    }
}
