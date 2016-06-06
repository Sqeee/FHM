package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsFile;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represents concatenating operation
 *
 * @author Jan Hlava, 395986
 */
public class OperationConcatenate implements Operation {
    public static final String PREFIX_STRING = "-s ";
    public static final String PREFIX_KEYWORD_VALUE = "-k ";

    private String keyword;
    private List<String> values;
    private String glue;
    private boolean updateCard;

    /**
     * Creates new instance of this class
     *
     * @param keyword    keyword of card, where the result will be stored, cannot be null
     * @param values     list of values used for concatenation, every value has prefix identified value type (-s= for string, -k= for keyword values), cannot be null or empty, can contain only allowed prefixes
     * @param glue       glue string used between values
     * @param updateCard if in case that header unique keyword is already in header should be updated
     * @throws OperationIllegalArgumentException if argument has invalid value
     */
    public OperationConcatenate(String keyword, List<String> values, String glue, boolean updateCard) throws OperationIllegalArgumentException {
        if (keyword == null) {
            throw new OperationIllegalArgumentException("No keyword for concatenation was specified.");
        } else if (values == null || values.isEmpty()) {
            throw new OperationIllegalArgumentException("No values for concatenation was specified.");
        }
        for (String value : values) {
            if (!value.startsWith(PREFIX_STRING) && !value.startsWith(PREFIX_KEYWORD_VALUE)) {
                throw new OperationIllegalArgumentException("Unknown concatenation value - " + value + ".");
            }
        }
        this.keyword = keyword;
        this.values = values;
        this.glue = glue;
        this.updateCard = updateCard;
    }

    /**
     * Executes concatenating operation
     *
     * @param file         file for executing operation
     * @param printerOK    method for printing OK things
     * @param printerError method for printing error things
     */
    @Override
    public void execute(FitsFile file, PrintOutputMethod printerOK, PrintOutputMethod printerError) {
        String concatenation = getConcatenation(file, values, glue, printerError);
        if (concatenation == null) {
            return;
        }
        printerOK.print("For file \"", file.getFilename(), "\" concatenated value is \"", concatenation, "\".");
        List<FitsCard> cards = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
        if (cards.isEmpty()) {
            cards = file.getHDU(0).getHeader().getCards();
            FitsCard newCard = new FitsCard();
            newCard.setKeyword(keyword);
            newCard.setRValue(concatenation);
            cards.add(Math.max(cards.size() - 1, 0), newCard);
            file.getHDU(0).getHeader().setCards(cards);
            printerOK.print("New card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been created and filled with concatenation value.");
        } else if (updateCard) {
            cards.get(0).setRValue(concatenation);
            printerOK.print("Card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been updated and filled with concatenation value.");
        } else {
            printerError.print("File \"", file.getFilename(), "\" is already containing keyword ", keyword, ", keyword is header unique and option for updating keywords was not specified.");
        }
    }

    /**
     * Returns concatenation from given values and file
     *
     * @param file         file from which we want card values
     * @param values       list of values used for concatenation, every value has prefix identified value type (-s= for string, -k= for keyword values)
     * @param glue         string used between values
     * @param printerError method for printing error things
     * @return concatenation from given values and file
     */
    private String getConcatenation(FitsFile file, List<String> values, String glue, PrintOutputMethod printerError) {
        if (glue == null) {
            glue = "";
        }
        StringJoiner stringJoiner = new StringJoiner(glue);
        for (String value : values) {
            if (value.startsWith(PREFIX_STRING)) {
                stringJoiner.add(value.substring(PREFIX_STRING.length()));
            } else {
                List<FitsCard> cardsWithKeywords = file.getHDU(0).getHeader().getCardsWithKeyword(value.substring(PREFIX_KEYWORD_VALUE.length()));
                if (cardsWithKeywords.isEmpty()) {
                    printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", value.substring(PREFIX_KEYWORD_VALUE.length()), ", which should be used for concatenation.");
                    return null;
                } else {
                    stringJoiner.add(cardsWithKeywords.get(0).getValue());
                }
            }
        }
        return stringJoiner.toString();
    }

    /**
     * Returns string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner("; ", "Operation concatenate: ", ".");
        stringJoiner.add("keyword - \"" + keyword + "\"");
        values.forEach(stringJoiner::add);
        if (updateCard) {
            stringJoiner.add("update card - yes");
        }
        return stringJoiner.toString();
    }
}
