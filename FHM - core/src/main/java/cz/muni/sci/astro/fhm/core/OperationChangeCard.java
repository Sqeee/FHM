package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsFile;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represents change card operation
 *
 * @author Jan Hlava, 395986
 */
public class OperationChangeCard implements Operation {
    private String keyword;
    private String newKeyword;
    private String newRValue;
    private String newIValue;
    private String newComment;
    private boolean deleteDuplicatedKeyword;

    /**
     * Creates new instance of this class
     *
     * @param keyword                 card with given keyword will be changed, cannot be null
     * @param newKeyword              new keyword, if it should not change use null
     * @param newRValue               new real value, if it should not change use null
     * @param newIValue               new imaginary value, if it should not change use null
     * @param newComment              new comment, if it should not change use null
     * @param deleteDuplicatedKeyword true if you want to delete card with same keyword (it must be unique header keyword) as new keyword, otherwise false
     * @throws OperationIllegalArgumentException if argument has invalid value
     */
    public OperationChangeCard(String keyword, String newKeyword, String newRValue, String newIValue, String newComment, boolean deleteDuplicatedKeyword) throws OperationIllegalArgumentException {
        if (keyword == null) {
            throw new OperationIllegalArgumentException("Keyword must be specified.");
        }
        this.keyword = keyword;
        this.newKeyword = newKeyword;
        this.newRValue = newRValue;
        this.newIValue = newIValue;
        this.newComment = newComment;
        this.deleteDuplicatedKeyword = deleteDuplicatedKeyword;
    }

    /**
     * Executes change card operation
     *
     * @param file         file for executing operation
     * @param printerOK    method for printing OK things
     * @param printerError method for printing error things
     */
    @Override
    public void execute(FitsFile file, PrintOutputMethod printerOK, PrintOutputMethod printerError) {
        FitsCard changedCard;
        List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
        if (cardsWithKeyword.isEmpty()) {
            printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", keyword, ".");
            return;
        } else {
            changedCard = cardsWithKeyword.get(0);
        }
        if (newKeyword != null) {
            cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(newKeyword);
            if (!cardsWithKeyword.isEmpty() && cardsWithKeyword.get(0).getKeyword().isUniqueHeaderKeyword()) {
                if (deleteDuplicatedKeyword) {
                    List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
                    cards.remove(cardsWithKeyword.get(0));
                    file.getHDU(0).getHeader().setCards(cards);
                } else {
                    printerError.print("File \"", file.getFilename(), "\" is already containing keyword ", newKeyword, ", keyword is header unique and option for deleting duplicated keywords was not specified.");
                    return;
                }
            }
            changedCard.setKeyword(newKeyword);
        }
        if (newRValue != null) {
            changedCard.setRValue(newRValue);
        }
        if (newIValue != null) {
            changedCard.setIValue(newIValue);
        }
        if (newComment != null) {
            changedCard.setComment(newComment);
        }
        printerOK.print("Values of card with keyword ", keyword, " in file \"", file.getFilename(), "\" was changed.");
    }

    /**
     * Returns string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner("; ", "Operation change card: ", ".");
        stringJoiner.add("keyword - \"" + keyword + "\"");
        if (newKeyword != null) {
            stringJoiner.add("new keyword - \"" + newKeyword + "\"");
        }
        if (newRValue != null) {
            stringJoiner.add("real value - \"" + newRValue + "\"");
        }
        if (newIValue != null) {
            stringJoiner.add("imaginary value - \"" + newIValue + "\"");
        }
        if (newComment != null) {
            stringJoiner.add("comment - \"" + newComment + "\"");
        }
        if (deleteDuplicatedKeyword) {
            stringJoiner.add("delete duplicated card - yes");
        }
        return stringJoiner.toString();
    }
}
