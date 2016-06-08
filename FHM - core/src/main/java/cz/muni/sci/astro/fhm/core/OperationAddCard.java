package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsFile;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represents add card operation
 *
 * @author Jan Hlava, 395986
 */
public class OperationAddCard implements Operation {
    private String keyword;
    private String rvalue;
    private String ivalue;
    private String comment;
    private Integer index;
    private String afterKeyword;
    private boolean updateCard;

    /**
     * Creates new instance of this class
     *
     * @param keyword      keyword of new card, cannot be null
     * @param rvalue       real value of new card
     * @param ivalue       imaginary value of new card
     * @param comment      comment of new card
     * @param index        index where new card should be added, null means index is not specified, negative values means indexing from the end, indexing is from 1
     * @param afterKeyword after which keyword should be added, null means no after card is specified, if index is specified, then this must be null
     * @param updateCard   if in case that header unique keyword is already in header should be updated
     * @throws OperationIllegalArgumentException if argument has invalid value
     */
    public OperationAddCard(String keyword, String rvalue, String ivalue, String comment, Integer index, String afterKeyword, boolean updateCard) throws OperationIllegalArgumentException {
        if (index != null && index == 0) {
            throw new OperationIllegalArgumentException("Index 0 is invalid.");
        } else if (index != null && afterKeyword != null) {
            throw new OperationIllegalArgumentException("Only index or after keyword can be specified, not both.");
        } else if (keyword == null) {
            throw new OperationIllegalArgumentException("Keyword must be specified.");
        } else if (keyword.length() > FitsFile.KEYWORD_LENGTH) {
            throw new OperationIllegalArgumentException("Keyword exceeds max length (" + FitsFile.KEYWORD_LENGTH + ").");
        }
        if (rvalue == null) {
            rvalue = "";
        }
        if (ivalue == null) {
            ivalue = "";
        }
        if (comment == null) {
            comment = "";
        }
        FitsCard card = new FitsCard();
        card.setKeyword(keyword);
        card.setRValue(rvalue);
        card.setIValue(ivalue);
        card.setComment(comment);
        List<String> problems = card.validate();
        if (!problems.isEmpty()) {
            throw new OperationIllegalArgumentException(String.join("\n", problems));
        }
        this.keyword = keyword;
        this.rvalue = rvalue;
        this.ivalue = ivalue;
        this.comment = comment;
        this.index = index;
        this.afterKeyword = afterKeyword;
        this.updateCard = updateCard;
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
        FitsCard newCard = new FitsCard();
        newCard.setKeyword(keyword);
        newCard.setRValue(rvalue);
        newCard.setComment(comment);
        newCard.setIValue(ivalue);
        List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
        FitsCard deletedCard = null;
        Integer newIndex = index;
        if (!updateCard && !cardsWithKeyword.isEmpty() && cardsWithKeyword.get(0).getKeyword().isUniqueHeaderKeyword()) {
            printerError.print("File \"", file.getFilename(), "\" is already containing keyword ", keyword, ", keyword is header unique and option for updating instead of adding was not specified.");
            return;
        } else if (updateCard && !cardsWithKeyword.isEmpty() && cardsWithKeyword.get(0).getKeyword().isUniqueHeaderKeyword()) {
            deletedCard = cardsWithKeyword.get(0);
        }
        List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
        if (newIndex != null && Math.abs(newIndex) != 1 && Math.abs(newIndex) > cards.size()) // if no cards are in file, then size is 0 and is bigger than index 1, but index 1 is correct - I want to give new card to first place
        {
            printerError.print("File \"", file.getFilename(), "\" does not have index ", Integer.toString(newIndex), ", maximum index is ", Integer.toString(cards.size()), ".");
            return;
        }
        if (afterKeyword != null) {
            cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(afterKeyword);
            if (!cardsWithKeyword.isEmpty()) {
                newIndex = cards.indexOf(cardsWithKeyword.get(0)) + 2; // I want after (+1) and indexing is shifted by +1
            } else {
                printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", afterKeyword, ".");
                return;
            }
        } else if (newIndex == null) {
            newIndex = -2;
        }
        if (newIndex < 0) {
            newIndex += cards.size() + 1; // after adding card, end index will be shifted -> max index 10, I want -1 -> 9, but if I add new card at 9, original card on index 9 will be shifted to index 10 and new card will on index -2 instead -1 -> it is reason for +1
            newIndex = Math.max(0, newIndex); // fix index in case, that HDU has all cards deleted
        } else {
            newIndex--; // fix different indexing -> java starts with 0, users with 1
        }
        if (deletedCard != null && index != null && index < 0 && cards.indexOf(deletedCard) + 1 > index) // fix indexing from end in case, that deleted card is between end and index
        {
            newIndex--;
        }
        cards.add(newIndex, newCard);
        if (deletedCard != null) {
            cards.remove(deletedCard);
            printerOK.print("Original card with keyword ", keyword, " was deleted from file \"", file.getFilename(), "\".");
        }
        file.getHDU(0).getHeader().setCards(cards);
        printerOK.print("New card with keyword ", keyword, " was added to file \"", file.getFilename(), "\".");
    }

    /**
     * Returns string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner("; ", "Operation add card: ", ".");
        stringJoiner.add("keyword - \"" + keyword + "\"");
        if (!rvalue.isEmpty()) {
            stringJoiner.add("real value - \"" + rvalue + "\"");
        }
        if (!ivalue.isEmpty()) {
            stringJoiner.add("imaginary value - \"" + ivalue + "\"");
        }
        if (!comment.isEmpty()) {
            stringJoiner.add("comment - \"" + comment + "\"");
        }
        if (index != null) {
            stringJoiner.add("index - " + index);
        } else if (afterKeyword != null) {
            stringJoiner.add("after keyword - \"" + afterKeyword + "\"");
        }
        if (updateCard) {
            stringJoiner.add("update card - yes");
        }
        return stringJoiner.toString();
    }
}
