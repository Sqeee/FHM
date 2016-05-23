package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsCardDateValue;
import cz.muni.sci.astro.fits.FitsCardDateValueUnknownFormatException;
import cz.muni.sci.astro.fits.FitsFile;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.StringJoiner;

/**
 * Runs multiple operation
 *
 * @author Jan Hlava, 395986
 */
public class MultipleOperationsRunner
{
    public static final String PARAM_CONCATENATION_STRING_PREFIX = "-s ";
    public static final String PARAM_CONCATENATION_KEYWORD_VALUE_PREFIX = "-k ";
    public static final String PARAM_SHIFTING_YEAR_PREFIX = "-y ";
    public static final String PARAM_SHIFTING_MONTH_PREFIX = "-m ";
    public static final String PARAM_SHIFTING_DAY_PREFIX = "-d ";
    public static final String PARAM_SHIFTING_HOUR_PREFIX = "-h ";
    public static final String PARAM_SHIFTING_MINUTE_PREFIX = "-min ";
    public static final String PARAM_SHIFTING_SECOND_PREFIX = "-s ";
    public static final String PARAM_SHIFTING_MILLISECOND_PREFIX = "-ms ";
    public static final String PARAM_SHIFTING_MICROSECOND_PREFIX = "-mics ";

    private PrintOutputMethod printerOK;
    private PrintOutputMethod printerError;

    /**
     * Creates new instances of this class
     *
     * @param printerOK method for printing OK things
     * @param printerError method for printing error things
     */
    public MultipleOperationsRunner(PrintOutputMethod printerOK, PrintOutputMethod printerError)
    {
        this.printerOK = printerOK;
        this.printerError = printerError;
    }

    /**
     * Adds new card to files
     *
     * @param files files to adding card
     * @param keyword keyword of new card
     * @param rvalue real value of new card
     * @param ivalue imaginary value of new card, can be unspecified (null)
     * @param comment comment of new card, can be unspecified (null)
     * @param index index where new card should be added, null means index is not specified, negative values means indexing from the end, indexing is from 1
     * @param afterKeyword after which keyword should be added, null means no after card is specified, if index is specified, then this must be null
     * @param updateCard if in case that header unique keyword is already in header should be updated
     */
    public void addCard(List<FitsFile> files, String keyword, String rvalue, String ivalue, String comment, Integer index, String afterKeyword, boolean updateCard)
    {
        if (index != null && index == 0)
        {
            printerError.print("Index 0 is invalid.");
            return;
        }
        else if (index != null && afterKeyword != null)
        {
            printerError.print("Only index or after keyword can be specified, not both.");
            return;
        }
        else if (keyword == null)
        {
            printerError.print("Keyword must be specified.");
            return;
        }
        checkIsWorkingWithZeroFiles(files);
        if (rvalue == null)
        {
            rvalue = "";
        }
        if (ivalue == null)
        {
            ivalue = "";
        }
        if (comment == null)
        {
            comment = "";
        }
        for (FitsFile file : files)
        {
            FitsCard newCard = new FitsCard();
            newCard.setKeyword(keyword);
            newCard.setRValue(rvalue);
            newCard.setComment(comment);
            newCard.setIValue(ivalue);
            List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
            FitsCard deletedCard = null;
            Integer newIndex = index;
            if (!updateCard && !cardsWithKeyword.isEmpty() && cardsWithKeyword.get(0).getKeyword().isUniqueHeaderKeyword())
            {
                printerError.print("File \"", file.getFilename(), "\" is already containing keyword ", keyword, ", keyword is header unique and option for updating instead of adding was not specified.");
                continue;
            }
            else if (updateCard && !cardsWithKeyword.isEmpty() && cardsWithKeyword.get(0).getKeyword().isUniqueHeaderKeyword())
            {
                deletedCard = cardsWithKeyword.get(0);
            }
            List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
            if (newIndex != null && Math.abs(newIndex) != 1 && Math.abs(newIndex) > cards.size()) // if no cards are in file, then size is 0 and is bigger than index 1, but index 1 is correct - I want to give new card to first place
            {
                printerError.print("File \"", file.getFilename(), "\" does not have index ", Integer.toString(newIndex), ", maximum index is ", Integer.toString(cards.size()), ".");
                continue;
            }
            if (afterKeyword != null)
            {
                cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(afterKeyword);
                if (!cardsWithKeyword.isEmpty())
                {
                    newIndex = cards.indexOf(cardsWithKeyword.get(0)) + 2; // I want after (+1) and indexing is shifted by +1
                }
                else
                {
                    printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", afterKeyword, ".");
                    continue;
                }
            }
            else if (newIndex == null)
            {
                newIndex = -2;
            }
            if (newIndex < 0)
            {
                newIndex += cards.size() + 1; // after adding card, end index will be shifted -> max index 10, I want -1 -> 9, but if I add new card at 9, original card on index 9 will be shifted to index 10 and new card will on index -2 instead -1 -> it is reason for +1
                newIndex = Math.max(0, newIndex); // fix index in case, that HDU has all cards deleted
            }
            else
            {
                newIndex--; // fix different indexing -> java starts with 0, users with 1
            }
            if (deletedCard != null && index != null && index < 0 && cards.indexOf(deletedCard) + 1 > index) // fix indexing from end in case, that deleted card is between end and index
            {
                newIndex--;
            }
            cards.add(newIndex, newCard);
            if (deletedCard != null)
            {
                cards.remove(deletedCard);
                printerOK.print("Original card with keyword ", keyword, " was deleted from file \"", file.getFilename(), "\".");
            }
            file.getHDU(0).getHeader().setCards(cards);
            printerOK.print("New card with keyword ", keyword, " was added to file \"", file.getFilename(), "\".");
        }
    }

    /**
     * Removes card from files
     *
     * @param files files to removing card
     * @param keyword card with this keyword should be deleted, null means keyword is not specified, then for deleting will be used index
     * @param index index from where card should be deleted, null means index is not specified, negative values means indexing from the end, indexing is from 1, if keyword param is specified, this param must be null, otherwise this param must be specified
     */
    public void removeCard(List<FitsFile> files, String keyword, Integer index)
    {
        if (index != null && index == 0)
        {
            printerError.print("Index 0 is invalid.");
            return;
        }
        else if (index == null && keyword == null)
        {
            printerError.print("For deleting must be specified index or keyword.");
            return;
        }
        else if (index != null && keyword != null)
        {
            printerError.print("For deleting cannot be specified both params.");
            return;
        }
        checkIsWorkingWithZeroFiles(files);
        for (FitsFile file : files)
        {
            Integer newIndex = index;
            List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
            if (newIndex != null && Math.abs(newIndex) > cards.size())
            {
                printerError.print("File \"", file.getFilename(), "\" does not have index ", Integer.toString(newIndex), ", maximum index is ", Integer.toString(cards.size()), ".");
                continue;
            }
            else if (keyword != null)
            {
                List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
                if (cardsWithKeyword.isEmpty())
                {
                    printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", keyword, ".");
                    continue;
                }
                else
                {
                    newIndex = cards.indexOf(cardsWithKeyword.get(0)) + 1; // Indexing is shifted by +1
                }
            }
            if (newIndex < 0)
            {
                newIndex += cards.size(); // convert end index to classical index
            }
            else
            {
                newIndex--; // fix different indexing -> java starts with 0, users with 1
            }
            FitsCard deletedCard = cards.remove(newIndex.intValue());
            printerOK.print("Card with keyword ", deletedCard.getKeywordName(), " was deleted from file \"", file.getFilename(), "\".");
            file.getHDU(0).getHeader().setCards(cards);
        }
    }

    /**
     * Changes values of card with given keyword
     *
     * @param files files for changing card
     * @param keyword card with given keyword will be changed
     * @param newKeyword new keyword, if it should not change use null
     * @param newRValue new real value, if it should not change use null
     * @param newIValue new imaginary value, if it should not change use null
     * @param newComment new comment, if it should not change use null
     * @param deleteDuplicatedKeyword true if you want to delete card with same keyword (it must be unique header keyword) as new keyword, otherwise false
     */
    public void changeCard(List<FitsFile> files, String keyword, String newKeyword, String newRValue, String newIValue, String newComment, boolean deleteDuplicatedKeyword)
    {
        if (keyword == null)
        {
            printerError.print("Keyword must be specified.");
            return;
        }
        checkIsWorkingWithZeroFiles(files);
        for (FitsFile file : files)
        {
            FitsCard changedCard;
            List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
            if (cardsWithKeyword.isEmpty())
            {
                printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", keyword, ".");
                continue;
            }
            else
            {
                changedCard = cardsWithKeyword.get(0);
            }
            if (newKeyword != null)
            {
                cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(newKeyword);
                if (!cardsWithKeyword.isEmpty() && cardsWithKeyword.get(0).getKeyword().isUniqueHeaderKeyword())
                {
                    if (deleteDuplicatedKeyword)
                    {
                        List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
                        cards.remove(cardsWithKeyword.get(0));
                        file.getHDU(0).getHeader().setCards(cards);
                    }
                    else
                    {
                        printerError.print("File \"", file.getFilename(), "\" is already containing keyword ", newKeyword, ", keyword is header unique and option for deleting duplicated keywords was not specified.");
                        continue;
                    }
                }
                changedCard.setKeyword(newKeyword);
            }
            if (newRValue != null)
            {
                changedCard.setRValue(newRValue);
            }
            if (newIValue != null)
            {
                changedCard.setIValue(newIValue);
            }
            if (newComment != null)
            {
                changedCard.setComment(newComment);
            }
            printerOK.print("Values of card with keyword ", keyword, " in file \"", file.getFilename(), "\" was changed.");
        }
    }

    /**
     * Changes index of card with given keyword
     *
     * @param files files for changing index
     * @param keyword index of card with given keyword will be changed
     * @param index new index, negative values means indexing from the end, indexing is from 1
     */
    public void changeIndexCard(List<FitsFile> files, String keyword, Integer index)
    {
        if (keyword == null)
        {
            printerError.print("Keyword must be specified.");
            return;
        }
        else if (index == null)
        {
            printerError.print("Index must be specified.");
            return;
        }
        else if (index == 0)
        {
            printerError.print("0 index is invalid.");
            return;
        }
        checkIsWorkingWithZeroFiles(files);
        for (FitsFile file : files)
        {
            int newIndex = index;
            List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
            List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
            if (cardsWithKeyword.isEmpty())
            {
                printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", keyword, ".");
                continue;
            }
            else if (Math.abs(newIndex) > cards.size())
            {
                printerError.print("File \"", file.getFilename(), "\" does not have index ", Integer.toString(newIndex), ", maximum index is ", Integer.toString(cards.size()), ".");
                continue;
            }
            FitsCard card = cardsWithKeyword.get(0);
            cards.remove(card);
            if (newIndex < 0)
            {
                newIndex += cards.size() + 1; // after adding card, end index will be shifted -> max index 10, I want -1 -> 9, but if I add new card at 9, original card on index 9 will be shifted to index 10 and new card will on index -2 instead -1 -> it is reason for +1
            }
            else
            {
                newIndex--; // fix different indexing -> java starts with 0, users with 1
            }
            cards.add(newIndex, card);
            file.getHDU(0).getHeader().setCards(cards);
            printerOK.print("Index of card with keyword ", keyword, " in file \"", file.getFilename(), "\" was changed.");
        }
    }

    /**
     * Concatenates given values to card with given keyword
     *
     * @param files files for concatenation
     * @param keyword keyword of card, where the result will be stored
     * @param values list of values used for concatenation, every value has prefix identified value type (-s= for string, -k= for keyword values)
     * @param glue glue string used between values
     * @param updateCard if in case that header unique keyword is already in header should be updated
     */
    public void concatenate(List<FitsFile> files, String keyword, List<String> values, String glue, boolean updateCard)
    {
        if (keyword == null)
        {
            printerError.print("No keyword for concatenation was specified.");
            return;
        }
        else if (values == null || values.isEmpty())
        {
            printerError.print("No values for concatenation was specified.");
            return;
        }
        for (String value : values)
        {
            if (!value.startsWith(PARAM_CONCATENATION_STRING_PREFIX) && !value.startsWith(PARAM_CONCATENATION_KEYWORD_VALUE_PREFIX))
            {
                printerError.print("Unknown concatenation value - ", value, ".");
                return;
            }
        }
        checkIsWorkingWithZeroFiles(files);
        for (FitsFile file : files)
        {
            String concatenation = getConcatenation(file, values, glue);
            if (concatenation == null)
            {
                continue;
            }
            printerOK.print("For file \"", file.getFilename(), "\" concatenated value is \"", concatenation, "\".");
            List<FitsCard> cards = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
            if (cards.isEmpty())
            {
                cards = file.getHDU(0).getHeader().getCards();
                FitsCard newCard = new FitsCard();
                newCard.setKeyword(keyword);
                newCard.setRValue(concatenation);
                cards.add(Math.max(cards.size() - 1, 0), newCard);
                file.getHDU(0).getHeader().setCards(cards);
                printerOK.print("New card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been created and filled with concatenation value.");
            }
            else if (updateCard)
            {
                cards.get(0).setRValue(concatenation);
                printerOK.print("Card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been updated and filled with concatenation value.");
            }
            else
            {
                printerError.print("File \"", file.getFilename(), "\" is already containing keyword ", keyword, ", keyword is header unique and option for updating keywords was not specified.");
            }
        }
    }

    /**
     * Shifts date and time values in card with given keyword
     *
     * @param files files for shifting card
     * @param keyword keyword of card, where the shifting will be done
     * @param intervals list of intervals used for shifting, every interval has prefix identified unit type (example -h= for hour, -y= for year)
     */
    public void shift(List<FitsFile> files, String keyword, List<String> intervals)
    {
        if (keyword == null)
        {
            printerError.print("No keyword for shifting was specified.");
            return;
        }
        else if (intervals == null || intervals.isEmpty())
        {
            printerError.print("No intervals for shifting was specified.");
            return;
        }
        for (String interval : intervals)
        {
            if (!interval.startsWith(PARAM_SHIFTING_YEAR_PREFIX) && !interval.startsWith(PARAM_SHIFTING_MONTH_PREFIX) && !interval.startsWith(PARAM_SHIFTING_DAY_PREFIX) && !interval.startsWith(PARAM_SHIFTING_HOUR_PREFIX) && !interval.startsWith(PARAM_SHIFTING_MINUTE_PREFIX) && !interval.startsWith(PARAM_SHIFTING_SECOND_PREFIX) && !interval.startsWith(PARAM_SHIFTING_MILLISECOND_PREFIX) && !interval.startsWith(PARAM_SHIFTING_MICROSECOND_PREFIX))
            {
                printerError.print("Unknown shifting interval - ", interval, ".");
                return;
            }
        }
        checkIsWorkingWithZeroFiles(files);
        for (FitsFile file : files)
        {
            List<FitsCard> cards = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
            if (cards.isEmpty())
            {
                printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", keyword, ".");
            }
            else if (cards.get(0).isDateValue())
            {
                FitsCardDateValue dateValue = cards.get(0).getDateValue();
                for (String interval : intervals)
                {
                    int edgeIndex = interval.indexOf(' ') + 1;
                    int value = Integer.parseInt(interval.substring(edgeIndex));
                    switch (interval.substring(0, edgeIndex))
                    {
                        case PARAM_SHIFTING_YEAR_PREFIX:
                            dateValue.doShift(value, ChronoUnit.YEARS);
                            break;
                        case PARAM_SHIFTING_MONTH_PREFIX:
                            dateValue.doShift(value, ChronoUnit.MONTHS);
                            break;
                        case PARAM_SHIFTING_DAY_PREFIX:
                            dateValue.doShift(value, ChronoUnit.DAYS);
                            break;
                        case PARAM_SHIFTING_HOUR_PREFIX:
                            dateValue.doShift(value, ChronoUnit.HOURS);
                            break;
                        case PARAM_SHIFTING_MINUTE_PREFIX:
                            dateValue.doShift(value, ChronoUnit.MINUTES);
                            break;
                        case PARAM_SHIFTING_SECOND_PREFIX:
                            dateValue.doShift(value, ChronoUnit.SECONDS);
                            break;
                        case PARAM_SHIFTING_MILLISECOND_PREFIX:
                            dateValue.doShift(value, ChronoUnit.MILLIS);
                            break;
                        case PARAM_SHIFTING_MICROSECOND_PREFIX:
                            dateValue.doShift(value, ChronoUnit.MICROS);
                            break;
                        default:
                            printerError.print("Unknown interval ", interval.substring(0, edgeIndex), ".");
                            break;
                    }
                }
                printerOK.print("Date and time value of card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been shifted.");
            }
            else
            {
                printerError.print("Keyword ", keyword, " in file \"", file.getFilename(), "\" has not date or time value.");
            }
        }
    }

    /**
     * Computes julian day and saves to card with given keyword
     *
     * @param files files for computing julian day
     * @param keyword keyword of card, where the result will be stored
     * @param sourceKeyword from which keyword we take date time
     * @param dateTime date time for computing julian day
     * @param updateCard if in case that header unique keyword is already in header should be updated
     */
    public void jd(List<FitsFile> files, String keyword, String sourceKeyword, String dateTime, boolean updateCard)
    {
        if (keyword == null)
        {
            printerError.print("No keyword for computing julian day was specified.");
            return;
        }
        else if (sourceKeyword == null && dateTime == null)
        {
            printerError.print("Source keyword or date time value must be specified.");
            return;
        }
        FitsCardDateValue dateValue = null;
        if (dateTime != null)
        {
            try
            {
                dateValue = FitsCardDateValue.createFromDateString(dateTime);
            }
            catch (FitsCardDateValueUnknownFormatException exc)
            {
                printerError.print("Given date time value has unknown or bad format.");
                return;
            }
        }
        checkIsWorkingWithZeroFiles(files);
        for (FitsFile file : files)
        {
            if (sourceKeyword != null)
            {
                List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(sourceKeyword);
                if (cardsWithKeyword.isEmpty())
                {
                    printerError.print("File \"", file.getFilename(), "\" does not contain source keyword ", sourceKeyword, ".");
                    continue;
                }
                else if (cardsWithKeyword.get(0).isDateValue())
                {
                    dateValue = cardsWithKeyword.get(0).getDateValue();
                }
                else
                {
                    printerError.print("Keyword ", keyword, " in file \"", file.getFilename(), "\" has not date or time value.");
                    continue;
                }
            }
            printerOK.print("For file \"", file.getFilename(), "\" julian day is \"", Helpers.formatDouble(dateValue.getJulianDay()), "\".");
            List<FitsCard> cardsWithKeyword = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
            if (cardsWithKeyword.isEmpty())
            {
                List<FitsCard> cards = file.getHDU(0).getHeader().getCards();
                FitsCard newCard = new FitsCard();
                newCard.setKeyword(keyword);
                newCard.setRValue(Helpers.formatDouble(dateValue.getJulianDay()));
                cards.add(Math.max(cards.size() - 1, 0), newCard);
                file.getHDU(0).getHeader().setCards(cards);
                printerOK.print("New card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been created and filled with julian day.");
            }
            else if (updateCard)
            {
                cardsWithKeyword.get(0).setRValue(Helpers.formatDouble(dateValue.getJulianDay()));
                printerOK.print("Card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been updated and filled with julian day.");
            }
            else
            {
                printerError.print("File \"", file.getFilename(), "\" is already containing keyword ", keyword, ", keyword is header unique and option for updating keywords was not specified.");
            }
        }
    }

    /**
     * Returns concatenation from given values and file
     *
     * @param file file from which we want card values
     * @param values list of values used for concatenation, every value has prefix identified value type (-s= for string, -k= for keyword values)
     * @param glue string used between values
     * @return concatenation from given values and file
     */
    private String getConcatenation(FitsFile file, List<String> values, String glue)
    {
        if (glue == null)
        {
            glue = "";
        }
        StringJoiner stringJoiner = new StringJoiner(glue);
        for (String value : values)
        {
            if (value.startsWith(PARAM_CONCATENATION_STRING_PREFIX))
            {
                stringJoiner.add(value.substring(PARAM_CONCATENATION_STRING_PREFIX.length()));
            }
            else
            {
                List<FitsCard> cardsWithKeywords = file.getHDU(0).getHeader().getCardsWithKeyword(value.substring(PARAM_CONCATENATION_STRING_PREFIX.length()));
                if (cardsWithKeywords.isEmpty())
                {
                    printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", value.substring(PARAM_CONCATENATION_STRING_PREFIX.length()), ", which should be used for concatenation.");
                    return null;
                }
                else
                {
                    stringJoiner.add(cardsWithKeywords.get(0).getValue());
                }
            }
        }
        return stringJoiner.toString();
    }

    /**
     * Checks if we are working with zero files
     *
     * @param files files for check
     */
    private void checkIsWorkingWithZeroFiles(List<FitsFile> files)
    {
        if (files.isEmpty())
        {
            printerOK.print("Command is ignored, because you are working with empty set of files.");
        }
    }
}
