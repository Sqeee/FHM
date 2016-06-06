package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsCardDateValue;
import cz.muni.sci.astro.fits.FitsCardDateValueUnknownFormatException;
import cz.muni.sci.astro.fits.FitsFile;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represents JD operation
 *
 * @author Jan Hlava, 395986
 */
public class OperationJD implements Operation {
    private String keyword;
    private String sourceKeyword;
    private FitsCardDateValue dateValue;
    private boolean updateCard;

    /**
     * Creates new instance of this class
     *
     * @param keyword       keyword of card, where the result will be stored, cannot be null
     * @param sourceKeyword from which keyword we take date time
     * @param dateTime      date time for computing julian day
     * @param updateCard    if in case that header unique keyword is already in header should be updated
     * @throws OperationIllegalArgumentException if argument has invalid value
     */
    public OperationJD(String keyword, String sourceKeyword, String dateTime, boolean updateCard) throws OperationIllegalArgumentException {
        if (keyword == null) {
            throw new OperationIllegalArgumentException("No keyword for computing julian day was specified.");
        } else if (sourceKeyword == null && dateTime == null) {
            throw new OperationIllegalArgumentException("Source keyword or date time value must be specified.");
        }
        if (dateTime != null) {
            try {
                dateValue = FitsCardDateValue.createFromDateString(dateTime);
            } catch (FitsCardDateValueUnknownFormatException exc) {
                throw new OperationIllegalArgumentException("Given date time value has unknown or bad format.");
            }
        }
        this.keyword = keyword;
        this.sourceKeyword = sourceKeyword;
        this.updateCard = updateCard;
    }

    /**
     * Executes JD operation
     *
     * @param file         file for executing operation
     * @param printerOK    method for printing OK things
     * @param printerError method for printing error things
     */
    @Override
    public void execute(FitsFile file, PrintOutputMethod printerOK, PrintOutputMethod printerError) {
        if (sourceKeyword != null) {
            List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(sourceKeyword);
            if (cardsWithKeyword.isEmpty()) {
                printerError.print("File \"", file.getFilename(), "\" does not contain source keyword ", sourceKeyword, ".");
                return;
            } else if (cardsWithKeyword.get(0).isDateValue()) {
                dateValue = cardsWithKeyword.get(0).getDateValue();
            } else {
                printerError.print("Keyword ", keyword, " in file \"", file.getFilename(), "\" has not date or time value.");
                return;
            }
        }
        printerOK.print("For file \"", file.getFilename(), "\" julian day is \"", Helpers.formatDouble(dateValue.getJulianDay()), "\".");
        List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
        if (cardsWithKeyword.isEmpty()) {
            List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
            FitsCard newCard = new FitsCard();
            newCard.setKeyword(keyword);
            newCard.setRValue(Helpers.formatDouble(dateValue.getJulianDay()));
            cards.add(Math.max(cards.size() - 1, 0), newCard);
            file.getHDU(0).getHeader().setCards(cards);
            printerOK.print("New card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been created and filled with julian day.");
        } else if (updateCard) {
            cardsWithKeyword.get(0).setRValue(Helpers.formatDouble(dateValue.getJulianDay()));
            printerOK.print("Card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been updated and filled with julian day.");
        } else {
            printerError.print("File \"", file.getFilename(), "\" is already containing keyword ", keyword, ", keyword is header unique and option for updating keywords was not specified.");
        }
    }

    /**
     * Returns string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner("; ", "Operation JD: ", ".");
        stringJoiner.add("keyword - \"" + keyword + "\"");
        if (sourceKeyword != null) {
            stringJoiner.add("source keyword - \"" + sourceKeyword + "\"");
        } else {
            stringJoiner.add("date time - \"" + dateValue + "\"");
        }
        if (updateCard) {
            stringJoiner.add("update card - yes");
        }
        return stringJoiner.toString();
    }
}
