package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsFile;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represents change card index operation
 *
 * @author Jan Hlava, 395986
 */
public class OperationChangeIndex implements Operation {
    private String keyword;
    private Integer index;

    /**
     * Creates new instance of this class
     *
     * @param keyword index of card with given keyword will be changed, cannot be null
     * @param index   new index, negative values means indexing from the end, indexing is from 1, cannot be null or has 0 value
     * @throws OperationIllegalArgumentException if argument has invalid value
     */
    public OperationChangeIndex(String keyword, Integer index) throws OperationIllegalArgumentException {
        if (keyword == null) {
            throw new OperationIllegalArgumentException("Keyword must be specified.");
        } else if (index == null) {
            throw new OperationIllegalArgumentException("Index must be specified.");
        } else if (index == 0) {
            throw new OperationIllegalArgumentException("0 index is invalid.");
        }
        this.keyword = keyword;
        this.index = index;
    }

    /**
     * Executes change card index operation
     *
     * @param file         file for executing operation
     * @param printerOK    method for printing OK things
     * @param printerError method for printing error things
     */
    @Override
    public void execute(FitsFile file, PrintOutputMethod printerOK, PrintOutputMethod printerError) {
        int newIndex = index;
        List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
        List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
        if (cardsWithKeyword.isEmpty()) {
            printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", keyword, ".");
            return;
        } else if (Math.abs(newIndex) > cards.size()) {
            printerError.print("File \"", file.getFilename(), "\" does not have index ", Integer.toString(newIndex), ", maximum index is ", Integer.toString(cards.size()), ".");
            return;
        }
        FitsCard card = cardsWithKeyword.get(0);
        cards.remove(card);
        if (newIndex < 0) {
            newIndex += cards.size() + 1; // after adding card, end index will be shifted -> max index 10, I want -1 -> 9, but if I add new card at 9, original card on index 9 will be shifted to index 10 and new card will on index -2 instead -1 -> it is reason for +1
        } else {
            newIndex--; // fix different indexing -> java starts with 0, users with 1
        }
        cards.add(newIndex, card);
        file.getHDU(0).getHeader().setCards(cards);
        printerOK.print("Index of card with keyword ", keyword, " in file \"", file.getFilename(), "\" was changed.");
    }

    /**
     * Returns string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner("; ", "Operation change index of card: ", ".");
        stringJoiner.add("keyword - \"" + keyword + "\"");
        stringJoiner.add("index - " + index);
        return stringJoiner.toString();
    }
}
