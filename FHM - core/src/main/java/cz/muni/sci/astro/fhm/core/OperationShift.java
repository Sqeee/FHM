package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsCardDateValue;
import cz.muni.sci.astro.fits.FitsFile;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents shift operation
 *
 * @author Jan Hlava, 395986
 */
public class OperationShift implements Operation {
    public static final String PREFIX_YEAR = "-y ";
    public static final String PREFIX_MONTH = "-m ";
    public static final String PREFIX_DAY = "-d ";
    public static final String PREFIX_HOUR = "-h ";
    public static final String PREFIX_MINUTE = "-min ";
    public static final String PREFIX_SECOND = "-s ";
    public static final String PREFIX_MILLISECOND = "-ms ";
    public static final String PREFIX_MICROSECOND = "-mics ";

    private String keyword;
    private List<String> intervals;

    /**
     * Creates new instance of this class
     *
     * @param keyword   keyword of card, where the shifting will be done, cannot be null
     * @param intervals list of intervals used for shifting, every interval has prefix identified unit type (example -h= for hour, -y= for year), cannot be null or empty, it can contain only allowed prefixes
     * @throws OperationIllegalArgumentException if argument has invalid value
     */
    public OperationShift(String keyword, List<String> intervals) throws OperationIllegalArgumentException {
        if (keyword == null) {
            throw new OperationIllegalArgumentException("No keyword for shifting was specified.");
        } else if (intervals == null || intervals.isEmpty()) {
            throw new OperationIllegalArgumentException("No intervals for shifting was specified.");
        } else if (keyword.length() > FitsFile.KEYWORD_LENGTH) {
            throw new OperationIllegalArgumentException("Keyword exceeds max length (" + FitsFile.KEYWORD_LENGTH + ").");
        }
        for (String interval : intervals) {
            if (!interval.startsWith(PREFIX_YEAR) && !interval.startsWith(PREFIX_MONTH) && !interval.startsWith(PREFIX_DAY) && !interval.startsWith(PREFIX_HOUR) && !interval.startsWith(PREFIX_MINUTE) && !interval.startsWith(PREFIX_SECOND) && !interval.startsWith(PREFIX_MILLISECOND) && !interval.startsWith(PREFIX_MICROSECOND)) {
                throw new OperationIllegalArgumentException("Unknown shifting interval - " + interval + ".");
            }
        }
        FitsCard card = new FitsCard();
        card.setKeyword(keyword);
        switch (card.getKeyword().getType()) {
            case INT:
                card.setRValue("0");
                break;
            case REAL:
                card.setRValue("0.0");
                break;
            case LITERAL:
            case CUSTOM:
                card.setRValue("2016-01-01 00:00:00");
                break;
            default:
                throw new OperationIllegalArgumentException("Keyword " + keyword + " has " + card.getKeyword().getType() + " data type and it cannot be shifted.");
        }
        List<String> problems = card.validate();
        if (!problems.isEmpty()) {
            throw new OperationIllegalArgumentException(String.join("\n", problems));
        }
        this.keyword = keyword;
        this.intervals = intervals;
    }

    /**
     * Executes shift operation
     *
     * @param file         file for executing operation
     * @param printerOK    method for printing OK things
     * @param printerError method for printing error things
     */
    @Override
    public void execute(FitsFile file, PrintOutputMethod printerOK, PrintOutputMethod printerError) {
        List<FitsCard> cards = file.getHDU(0).getHeader().getCardsWithKeyword(keyword);
        if (cards.isEmpty()) {
            printerError.print("File \"", file.getFilename(), "\" does not contain keyword ", keyword, ".");
        } else if (cards.get(0).isDateValue()) {
            FitsCardDateValue dateValue = cards.get(0).getDateValue();
            for (String interval : intervals) {
                int edgeIndex = interval.indexOf(' ') + 1;
                int value = Integer.parseInt(interval.substring(edgeIndex));
                switch (interval.substring(0, edgeIndex)) {
                    case PREFIX_YEAR:
                        dateValue.doShift(value, ChronoUnit.YEARS);
                        break;
                    case PREFIX_MONTH:
                        dateValue.doShift(value, ChronoUnit.MONTHS);
                        break;
                    case PREFIX_DAY:
                        dateValue.doShift(value, ChronoUnit.DAYS);
                        break;
                    case PREFIX_HOUR:
                        dateValue.doShift(value, ChronoUnit.HOURS);
                        break;
                    case PREFIX_MINUTE:
                        dateValue.doShift(value, ChronoUnit.MINUTES);
                        break;
                    case PREFIX_SECOND:
                        dateValue.doShift(value, ChronoUnit.SECONDS);
                        break;
                    case PREFIX_MILLISECOND:
                        dateValue.doShift(value, ChronoUnit.MILLIS);
                        break;
                    case PREFIX_MICROSECOND:
                        dateValue.doShift(value, ChronoUnit.MICROS);
                        break;
                    default:
                        printerError.print("Unknown interval ", interval.substring(0, edgeIndex), ".");
                        break;
                }
            }
            printerOK.print("Date and time value of card with keyword ", keyword, " in file \"", file.getFilename(), "\" has been shifted.");
        } else {
            printerError.print("Keyword ", keyword, " in file \"", file.getFilename(), "\" has not date or time value.");
        }
    }

    /**
     * Returns string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner("; ", "Operation shift: ", ".");
        stringJoiner.add("keyword - \"" + keyword + "\"");
        intervals.forEach(stringJoiner::add);
        return stringJoiner.toString();
    }
}
