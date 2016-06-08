package cz.muni.sci.astro.fits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents FITS Card Image structure
 *
 * @author Jan Hlava, 395986
 */
public class FitsCard {
    private static final String LOGICAL_STRING_FALSE = "F";
    private static final String LOGICAL_STRING_TRUE = "T";
    private static final char LOGICAL_CHAR_FALSE = 'F';
    private static final char LOGICAL_CHAR_TRUE = 'T';
    private static final int RVALUE_START_INDEX = 10;
    private static final int IVALUE_START_INDEX = 30;
    private static final int COMMENT_START_INDEX = 50;
    private static final int VALUE_LENGTH = IVALUE_START_INDEX - RVALUE_START_INDEX;
    private static final int CONTINUE_VALUE_LENGTH = FitsFile.CARD_LENGTH - RVALUE_START_INDEX;
    private static final String COMMENT_SEPARATOR = " / ";
    private static final String LITERAL_MARK = "'";
    private static final String CONTINUED_VALUE_END_MARK = "&";
    private static final int VALUE_LENGTH_RESERVED = 2 * LITERAL_MARK.length() + CONTINUED_VALUE_END_MARK.length();
    private static final String TRAIL_END = "\\s+$";

    private FitsKeyword keyword;
    private Object rValue;
    private Object iValue;
    private int nParam;
    private String comment;
    private String customName;
    private FitsCardDateValue dateValue;
    private int countContinue;

    /**
     * Creates new empty card
     */
    public FitsCard() {
        try {
            createNewFormEntry(FitsFile.BLANK_CARD_ENTRY);
        } catch (FitsCardBadFormatException ignored) // I created new card from valid blank entry, so I can ignore it
        {
        }
    }

    /**
     * Creates new Card from given entry
     *
     * @param entry Card String (80 bytes long)
     * @throws FitsCardBadFormatException if entry has bad format
     */
    public FitsCard(String entry) throws FitsCardBadFormatException {
        createNewFormEntry(entry);
    }

    /**
     * Creates new card from given entry
     *
     * @param entry Card String (80 bytes long)
     */
    private void createNewFormEntry(String entry) throws FitsCardBadFormatException {
        countContinue = 0;
        setKeyword(entry.substring(0, FitsFile.KEYWORD_LENGTH + 1));
        switch (keyword.getType()) {
            case LOGICAL:
                processLogical(entry);
                break;
            case INT:
                processInt(entry);
                break;
            case REAL:
                processReal(entry);
                break;
            case LITERAL:
                if (entry.charAt(RVALUE_START_INDEX) != '\'' || !entry.substring(RVALUE_START_INDEX + 1).contains(LITERAL_MARK)) {
                    throw new FitsCardBadFormatException("Bad format of fits card (keyword " + keyword.toString().replace("CUSTOM", customName) + " is literal type and value musts start and end with apostrophe ')");
                }
                processLiteral(entry);
                break;
            case CUSTOM:
                if (entry.charAt(RVALUE_START_INDEX) == '\'' && entry.substring(RVALUE_START_INDEX + 1).contains(LITERAL_MARK)) {
                    processLiteral(entry);
                } else if (entry.substring(RVALUE_START_INDEX, IVALUE_START_INDEX).contains(".")) {
                    processReal(entry);
                } else if (entry.charAt(IVALUE_START_INDEX - 1) == LOGICAL_CHAR_TRUE || entry.charAt(IVALUE_START_INDEX - 1) == LOGICAL_CHAR_FALSE) {
                    processLogical(entry);
                } else {
                    processInt(entry);
                }
                break;
            case NONE:
                if (keyword == FitsKeyword.COMMENT || keyword == FitsKeyword.HISTORY) {
                    rValue = entry.substring(FitsFile.KEYWORD_LENGTH, FitsFile.CARD_LENGTH);
                } else if (keyword == FitsKeyword.EMPTY) {
                    setComment(entry.substring(entry.indexOf("/ ", FitsFile.KEYWORD_LENGTH) + 2));
                } else if (keyword == FitsKeyword.CONTINUE) {
                    int begin_value_index;
                    int end_value_index;
                    begin_value_index = entry.indexOf(LITERAL_MARK, RVALUE_START_INDEX);
                    if (begin_value_index == -1) {
                        throw new FitsCardBadFormatException("Bad format of fits card (keyword CONTINUE value musts start with apostrophe ')");
                    }
                    end_value_index = begin_value_index;
                    while (true) {
                        end_value_index = entry.indexOf(LITERAL_MARK, end_value_index + 1);
                        if (end_value_index != -1 && end_value_index < entry.length() - 1 && entry.charAt(end_value_index + 1) == '\'') {
                            end_value_index++;
                        } else {
                            break;
                        }
                    }
                    if (end_value_index == -1) {
                        throw new FitsCardBadFormatException("Bad format of fits card (keyword CONTINUE value musts end with apostrophe ')");
                    }
                    rValue = entry.substring(begin_value_index + 1, end_value_index);
                    processComment(entry, end_value_index);
                }
                break;
        }
        computeDateValue();
    }

    /**
     * Process Card entry - Integer data type
     *
     * @param entry Card String (80 bytes long)
     * @throws FitsCardBadFormatException if entry has bad format or entry does not contain Integer data type
     */
    private void processInt(String entry) throws FitsCardBadFormatException {
        try {
            rValue = Integer.parseInt(entry.substring(RVALUE_START_INDEX, IVALUE_START_INDEX).trim());
            if (!entry.substring(IVALUE_START_INDEX, COMMENT_START_INDEX).trim().isEmpty() && !entry.substring(IVALUE_START_INDEX, COMMENT_START_INDEX).contains("/")) {
                iValue = Integer.parseInt(entry.substring(IVALUE_START_INDEX, COMMENT_START_INDEX).trim());
            }
            processComment(entry, IVALUE_START_INDEX);
        } catch (NumberFormatException exc) {
            String value;
            if (rValue != null) {
                value = entry.substring(IVALUE_START_INDEX, COMMENT_START_INDEX).trim();
            } else {
                value = entry.substring(RVALUE_START_INDEX, IVALUE_START_INDEX).trim();
            }
            throw new FitsCardBadFormatException("Bad format of fits card (keyword " + getKeywordName() + " is integer, but \"" + value + "\" is not integer)", exc);
        }
    }

    /**
     * Process Card entry - Literal data type
     *
     * @param entry Card String (80 bytes long)
     * @throws FitsCardBadFormatException if entry has bad format
     */
    private void processLiteral(String entry) throws FitsCardBadFormatException {
        int end_value_index = RVALUE_START_INDEX;
        while (true) {
            end_value_index = entry.indexOf(LITERAL_MARK, end_value_index + 1);
            if (end_value_index != -1 && end_value_index < entry.length() - 1 && entry.charAt(end_value_index + 1) == '\'') {
                end_value_index++;
            } else {
                break;
            }
        }
        if (end_value_index == -1) {
            throw new FitsCardBadFormatException("Bad format of fits card (keyword " + getKeywordName() + " is literal type and value musts end with apostrophe ')");
        }
        rValue = entry.substring(RVALUE_START_INDEX + 1, end_value_index).replaceAll(TRAIL_END, "");
        processComment(entry, end_value_index + 1);
    }

    /**
     * Process Card entry - Logical data type
     *
     * @param entry Card String (80 bytes long)
     * @throws FitsCardBadFormatException if entry has bad format
     */
    private void processLogical(String entry) throws FitsCardBadFormatException {
        if (entry.charAt(IVALUE_START_INDEX - 1) == LOGICAL_CHAR_TRUE) {
            rValue = true;
        } else if (entry.charAt(IVALUE_START_INDEX - 1) == LOGICAL_CHAR_FALSE) {
            rValue = false;
        } else {
            throw new FitsCardBadFormatException("Bad format of fits card (keyword " + getKeywordName() + " is logical type, valid values are T or F, not '" + entry.charAt(IVALUE_START_INDEX - 1) + "')");
        }
        processComment(entry, IVALUE_START_INDEX);
    }

    /**
     * Process Card entry - Real data type
     *
     * @param entry Card String (80 bytes long)
     * @throws FitsCardBadFormatException if entry has bad format or entry does not contain Real data type
     */
    private void processReal(String entry) throws FitsCardBadFormatException {
        try {
            rValue = Double.parseDouble(entry.substring(RVALUE_START_INDEX, IVALUE_START_INDEX).trim());
            if (!entry.substring(IVALUE_START_INDEX, COMMENT_START_INDEX).trim().isEmpty() && !entry.substring(IVALUE_START_INDEX, COMMENT_START_INDEX).contains("/")) {
                iValue = Double.parseDouble(entry.substring(IVALUE_START_INDEX, COMMENT_START_INDEX).trim());
            }
            processComment(entry, IVALUE_START_INDEX);
        } catch (NumberFormatException exc) {
            String value;
            if (rValue != null) {
                value = entry.substring(IVALUE_START_INDEX, COMMENT_START_INDEX).trim();
            } else {
                value = entry.substring(RVALUE_START_INDEX, IVALUE_START_INDEX).trim();
            }
            throw new FitsCardBadFormatException("Bad format of fits card (keyword " + getKeywordName() + " is real number, but \"" + value + "\" is not real number)", exc);
        }
    }

    /**
     * Returns processed value (for example for "T" returns boolean true)
     *
     * @param value string value to process
     * @return processed value (for example for "T" returns boolean true)
     */
    private Object processValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }
        try {
            String valueWithoutFD = value.trim().replaceAll("f|F|d|D", "s"); // Not parse values like 1D as number, but as String
            return Double.parseDouble(valueWithoutFD);
        } catch (NumberFormatException ignored) {
        }
        switch (value) {
            case LOGICAL_STRING_TRUE:
                return true;
            case LOGICAL_STRING_FALSE:
                return false;
            default:
                return value.replaceAll(TRAIL_END, "");
        }
    }

    /**
     * Process Card entry - comment
     *
     * @param entry      Card String (80 bytes long)
     * @param startIndex index where comment section should starts
     */
    public void processComment(String entry, int startIndex) {
        if (entry.substring(startIndex).contains("/ ")) {
            setComment(entry.substring(entry.indexOf("/ ", startIndex) + 2));
        } else if (entry.substring(startIndex).contains(" /")) {
            setComment(entry.substring(entry.indexOf(" /", startIndex) + 2));
        }
    }

    /**
     * Returns cards keyword
     *
     * @return cards keyword (warning: returns CUSTOM if keyword is not predefined, returns n keywords with N at the end)
     */
    public FitsKeyword getKeyword() {
        return keyword;
    }

    /**
     * Sets keyword and all according values
     *
     * @param keywordEntry entry containing keyword name
     */
    public void setKeyword(String keywordEntry) {
        keywordEntry += "        =".substring(Math.min(keywordEntry.length(), 9)); // processing values needs = at the end
        nParam = -1;
        customName = null;
        try {
            keyword = FitsKeyword.valueOf(keywordEntry.substring(0, FitsFile.KEYWORD_LENGTH).replaceAll(TRAIL_END, "").replace('-', '_'));
        } catch (IllegalArgumentException exc) {
            if (keywordEntry.substring(0, 5).equals("NAXIS") && keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH + 1).matches("\\d+[ ]{0,2}=")) {
                nParam = Integer.parseInt(keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH).replaceAll(TRAIL_END, ""));
                keyword = FitsKeyword.NAXISn;
            } else if (keywordEntry.substring(0, 7).equals("CCDSIZE") && keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH + 1).matches("\\d=")) {
                nParam = Integer.parseInt(keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH));
                keyword = FitsKeyword.CCDSIZEn;
            } else if (keywordEntry.substring(0, 7).equals("PIXSIZE") && keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH + 1).matches("\\d=")) {
                nParam = Integer.parseInt(keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH));
                keyword = FitsKeyword.PIXSIZEn;
            } else if (keywordEntry.substring(0, 7).equals("PIXSCAL") && keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH + 1).matches("\\d=")) {
                nParam = Integer.parseInt(keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH));
                keyword = FitsKeyword.PIXSCALn;
            } else if (keywordEntry.substring(0, 7).equals("BIASSEC") && keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH + 1).matches("\\d=")) {
                nParam = Integer.parseInt(keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH));
                keyword = FitsKeyword.BIASSECn;
            } else if (keywordEntry.substring(0, 7).equals("BINNING") && keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH + 1).matches("\\d=")) {
                nParam = Integer.parseInt(keywordEntry.substring(7, FitsFile.KEYWORD_LENGTH));
                keyword = FitsKeyword.BINNINGn;
            } else if (keywordEntry.substring(0, 5).equals("CRPIX") && keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH + 1).matches("\\d+[ ]{0,2}=")) {
                nParam = Integer.parseInt(keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH).replaceAll(TRAIL_END, ""));
                keyword = FitsKeyword.CRPIXn;
            } else if (keywordEntry.substring(0, 5).equals("CRVAL") && keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH + 1).matches("\\d+[ ]{0,2}=")) {
                nParam = Integer.parseInt(keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH).replaceAll(TRAIL_END, ""));
                keyword = FitsKeyword.CRVALn;
            } else if (keywordEntry.substring(0, 5).equals("CTYPE") && keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH + 1).matches("\\d+[ ]{0,2}=")) {
                nParam = Integer.parseInt(keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH).replaceAll(TRAIL_END, ""));
                keyword = FitsKeyword.CTYPEn;
            } else if (keywordEntry.substring(0, 5).equals("CDELT") && keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH + 1).matches("\\d+[ ]{0,2}=")) {
                nParam = Integer.parseInt(keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH).replaceAll(TRAIL_END, ""));
                keyword = FitsKeyword.CDELTn;
            } else if (keywordEntry.substring(0, 5).equals("CROTA") && keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH + 1).matches("\\d+[ ]{0,2}=")) {
                nParam = Integer.parseInt(keywordEntry.substring(5, FitsFile.KEYWORD_LENGTH).replaceAll(TRAIL_END, ""));
                keyword = FitsKeyword.CROTAn;
            } else if (keywordEntry.substring(0, FitsFile.KEYWORD_LENGTH).trim().isEmpty()) {
                keyword = FitsKeyword.EMPTY;
            } else {
                keyword = FitsKeyword.CUSTOM;
            }
        }
        if (keyword.getType() == FitsKeywordsDataType.CUSTOM) {
            customName = keywordEntry.substring(0, FitsFile.KEYWORD_LENGTH).replaceAll(TRAIL_END, "");
        }
    }

    /**
     * Returns cards keyword name
     *
     * @return cards keyword name
     */
    public String getKeywordName() {
        if (keyword.getType() != FitsKeywordsDataType.CUSTOM) {
            String name = keyword.toString();
            if (name.endsWith("n")) {
                name = name.replace("n", Integer.toString(nParam));
            }
            return name;
        } else {
            return customName;
        }
    }

    /**
     * Returns n param of Card
     *
     * @return n param of Card
     */
    public int getNParam() {
        return nParam;
    }

    /**
     * Return rValue (real part) of Card
     *
     * @return rValue (real part) of Card
     */
    public Object getRValue() {
        return rValue;
    }

    /**
     * Sets rValue (real part) to Card
     *
     * @param value value to be set
     */
    public void setRValue(String value) {
        rValue = processValue(value);
        computeDateValue();
    }

    /**
     * Return string representation of rValue (real part) of Card
     *
     * @return string representation of rValue (real part) of Card
     */
    public String getRValueString() {
        if (rValue == null) {
            return "";
        } else if (rValue instanceof Boolean) {
            if ((boolean) rValue) {
                return LOGICAL_STRING_TRUE;
            } else {
                return LOGICAL_STRING_FALSE;
            }
        } else {
            return rValue.toString();
        }
    }

    /**
     * Returns count of how many keywords CONTINUE were used to combine this keyword
     *
     * @return count of how many keywords CONTINUE were used to combine this keyword
     */
    public int getCountContinue() {
        return countContinue;
    }

    /**
     * Increments count of used CONTINUE keyword
     */
    public void incrementCountContinue() {
        countContinue++;
    }

    /**
     * Returns comment of Card
     *
     * @return comment of Card
     */
    public String getComment() {
        if (comment != null) {
            return comment;
        } else {
            return "";
        }
    }

    /**
     * Sets comment to Card
     *
     * @param comment value to be set
     */
    public void setComment(String comment) {
        if (comment != null) {
            this.comment = comment.replaceAll(TRAIL_END, "");
        } else {
            this.comment = "";
        }
    }

    /**
     * Returns iValue (imaginary part) of Card
     *
     * @return iValue (imaginary part) of Card
     */
    public Object getIValue() {
        return iValue;
    }

    /**
     * Sets iValue (imaginary part) to Card
     *
     * @param value value to be set
     */
    public void setIValue(String value) {
        iValue = processValue(value);
    }

    /**
     * Returns string representation of iValue (imaginary part) of Card
     *
     * @return string representation of iValue (imaginary part) of Card
     */
    public String getIValueString() {
        return iValue != null ? iValue.toString() : "";
    }

    /**
     * Returns data type of Card
     *
     * @return data type of Card
     */
    public FitsKeywordsDataType getDataType() {
        if (keyword.getType() == FitsKeywordsDataType.NONE) {
            return FitsKeywordsDataType.NONE;
        } else if (rValue instanceof String) {
            return FitsKeywordsDataType.LITERAL;
        } else if (rValue instanceof Integer) {
            return FitsKeywordsDataType.INT;
        } else if (rValue instanceof Double) {
            return FitsKeywordsDataType.REAL;
        } else if (rValue instanceof Boolean) {
            return FitsKeywordsDataType.LOGICAL;
        } else if (iValue instanceof Integer) {
            return FitsKeywordsDataType.INT;
        } else if (iValue instanceof Double) {
            return FitsKeywordsDataType.REAL;
        } else {
            return FitsKeywordsDataType.LITERAL;
        }
    }

    /**
     * Returns value (includes real and imaginary part) of Card
     *
     * @return value (includes real and imaginary part) of Card
     */
    public String getValue() {
        FitsKeywordsDataType dataType = getDataType();
        if (dataType == FitsKeywordsDataType.REAL || dataType == FitsKeywordsDataType.INT) {
            if (iValue == null) {
                return rValue.toString();
            } else {
                return '(' + rValue.toString() + ", " + iValue.toString() + ')';
            }
        } else if (dataType == FitsKeywordsDataType.LITERAL) {
            return (String) rValue;
        } else if (keyword == FitsKeyword.COMMENT || keyword == FitsKeyword.HISTORY) {
            return (String) rValue;
        } else if (dataType == FitsKeywordsDataType.NONE) {
            return "";
        } else if (rValue instanceof Boolean) {
            if ((Boolean) rValue) {
                return LOGICAL_STRING_TRUE;
            } else {
                return LOGICAL_STRING_FALSE;
            }
        } else // if thanks partial or bad editing is not datatype set correctly
        {
            return rValue != null ? rValue.toString() : "";
        }
    }

    /**
     * Returns escaped value of card
     *
     * @return escaped value of card
     */
    private String getEscapedValue() {
        if (getDataType() == FitsKeywordsDataType.LITERAL) {
            return getRValueString().replace(LITERAL_MARK, "''");
        } else {
            return getValue();
        }
    }

    /**
     * Returns data type of given value
     *
     * @param value value to get data type
     * @return data type of given value
     */
    public FitsKeywordsDataType getDataTypeOfValue(String value) {
        try {
            Integer.parseInt(value);
            return FitsKeywordsDataType.INT;
        } catch (NumberFormatException ignored) {
        }
        try {
            Double.parseDouble(value);
            return FitsKeywordsDataType.REAL;
        } catch (NumberFormatException ignored) {
        }
        if (value.equals(LOGICAL_STRING_TRUE) || value.equals(LOGICAL_STRING_FALSE)) {
            return FitsKeywordsDataType.LOGICAL;
        } else {
            return FitsKeywordsDataType.LITERAL;
        }
    }

    /**
     * Checks if contains date value
     *
     * @return true if contains date value, otherwise false
     */
    public boolean isDateValue() {
        return dateValue != null;
    }

    /**
     * Checks if contains classical date value
     *
     * @return true if contains classical date value, otherwise false
     */
    public boolean isClassicDateValue() {
        return (isDateValue() && dateValue.isCreatedFromClassicDate());
    }

    /**
     * Checks if contains julian day date value
     *
     * @return true if contains julian day date value, otherwise false
     */
    public boolean isJulianDayDateValue() {
        return (isDateValue() && !dateValue.isCreatedFromClassicDate());
    }

    /**
     * Returns JavaScript representation of cards from this Card
     *
     * @return JavaScript representation of cards from this Card
     */
    public String toStringJavaScript() {
        String keywordName = getKeywordName();
        if (keywordName.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(keywordName.replace("-", "__"));
        FitsKeywordsDataType dataType = getDataType();
        if (keyword.getType() != FitsKeywordsDataType.NONE) {
            result.append(" = ");
        } else {
            result.append(" = ''");
        }
        if (dataType == FitsKeywordsDataType.LITERAL || dataType == FitsKeywordsDataType.LOGICAL) {
            result.append(LITERAL_MARK).append(getValue().replace(LITERAL_MARK, "\\'")).append(LITERAL_MARK);
        } else if (keyword.getType() != FitsKeywordsDataType.NONE) {
            result.append(getValue());
        }
        return result.append(';').toString();
    }

    /**
     * Validates card and returns problems
     *
     * @return problems after validating
     */
    public List<String> validate() {
        String problemPrefix;
        List<String> problems = new ArrayList<>();
        String keywordName = getKeywordName();
        FitsKeywordsDataType keywordDataType = keyword.getType();
        FitsKeywordsDataType realDataType = getDataType();
        FitsKeywordsDataType imaginaryDataType = getDataTypeOfValue(getIValueString());
        if (keywordDataType != FitsKeywordsDataType.NONE && keywordDataType != FitsKeywordsDataType.CUSTOM) {
            if (realDataType != keywordDataType) {
                problems.add(linkProblem("Keyword ", keywordName, " has ", realDataType.toString(), " data type, but it should have ", keywordDataType.toString(), " data type."));
            }
        }
        if (realDataType != FitsKeywordsDataType.NONE && iValue != null && !getIValueString().isEmpty() && realDataType != imaginaryDataType && (realDataType == FitsKeywordsDataType.INT || realDataType == FitsKeywordsDataType.REAL)) {
            problems.add(linkProblem("Keyword ", keywordName, " has ", realDataType.toString(), " data type of real value, but imaginary value has ", imaginaryDataType.toString(), " data type."));
        }
        if ((realDataType == FitsKeywordsDataType.INT || realDataType == FitsKeywordsDataType.REAL) && getRValueString().length() > VALUE_LENGTH) {
            problems.add(linkProblem("Keyword ", keywordName, " and its real value exceeds maximal length limit - ", String.valueOf(VALUE_LENGTH), "."));
        }
        if ((imaginaryDataType == FitsKeywordsDataType.INT || realDataType == FitsKeywordsDataType.REAL) && getIValueString().length() > VALUE_LENGTH) {
            problems.add(linkProblem("Keyword ", keywordName, " and its imaginary value exceeds maximal length limit - ", String.valueOf(VALUE_LENGTH), "."));
        }
        if (keyword.isUnitKeyword() && (getComment() == null || getComment().isEmpty())) {
            problems.add(linkProblem("Keyword ", keywordName, " is keyword with physical unit, but comment is empty."));
        }
        if (!getIValueString().isEmpty() && !getRValueString().isEmpty() && (realDataType == FitsKeywordsDataType.LITERAL || realDataType == FitsKeywordsDataType.LOGICAL)) {
            problems.add(linkProblem("Keyword ", keywordName, " has ", realDataType.toString(), " data type, so it cannot have imaginary value."));
        } else if (!getIValueString().isEmpty() && getRValueString().isEmpty()) {
            problems.add(linkProblem("Keyword ", keywordName, " has imaginary value, but real value is empty."));
        }
        if (realDataType == FitsKeywordsDataType.LOGICAL && getComment().length() + COMMENT_SEPARATOR.length() > FitsFile.CARD_LENGTH - IVALUE_START_INDEX) {
            problems.add(linkProblem("Keyword ", keywordName, " has too long comment (", String.valueOf(getComment().length()), " chars), max allowed chars are ", String.valueOf(FitsFile.CARD_LENGTH - IVALUE_START_INDEX - COMMENT_SEPARATOR.length()), "."));
        } else if (realDataType == FitsKeywordsDataType.INT || realDataType == FitsKeywordsDataType.REAL) {
            int commentStart = COMMENT_START_INDEX;
            if (getIValueString().isEmpty()) {
                commentStart = IVALUE_START_INDEX;
            }
            if (getComment().length() + COMMENT_SEPARATOR.length() > FitsFile.CARD_LENGTH - commentStart) {
                problems.add(linkProblem("Keyword ", keywordName, " has too long comment (", String.valueOf(getComment().length()), " chars), max allowed chars are ", String.valueOf(FitsFile.CARD_LENGTH - commentStart - COMMENT_SEPARATOR.length()), "."));
            }
        }
        if (keyword.isNKeyword() && nParam < 1) {
            problems.add(linkProblem("Keyword ", keywordName, " cannot have n param lesser than 1."));
        }
        if (!isStringContainingOnlyAllowedCharacters(getValue())) {
            problems.add(linkProblem("Value '", getValue(), "' in keyword ", keywordName, " contains characters with ordinal value less than 32 or bigger than 126."));
        }
        if (!isStringContainingOnlyAllowedCharacters(getComment())) {
            problems.add(linkProblem("Comment '", getComment(), "' in keyword ", keywordName, " contains characters with ordinal value less than 32 or bigger than 126."));
        }
        switch (keyword) {
            case CUSTOM:
                if (!customName.replaceAll("[A-Z0-9-_]", "").isEmpty()) {
                    problems.add(linkProblem("Keyword ", customName, " contains forbidden characters (allowed: uppercase letters A to Z, the digits 0 to 9, the hyphen, and the underscore character)."));
                }
                if (customName.length() > FitsFile.KEYWORD_LENGTH) {
                    problems.add(linkProblem("Keyword ", customName, " exceeds max length (8)."));
                }
                break;
            case EMPTY:
                problemPrefix = "Empty keyword can have only comment, not value - ";
                if (rValue != null && !getRValueString().isEmpty()) {
                    problems.add(linkProblem(problemPrefix, "real value is '", getRValueString(), "'."));
                }
                if (iValue != null && !getIValueString().isEmpty()) {
                    problems.add(linkProblem(problemPrefix, "imaginary value is '", getIValueString(), "'."));
                }
                if (getComment().length() + COMMENT_SEPARATOR.length() > FitsFile.CARD_LENGTH - IVALUE_START_INDEX) {
                    problems.add(linkProblem("Keyword ", keywordName, " has too long comment (", String.valueOf(getComment().length()), " chars), max allowed chars are ", String.valueOf(FitsFile.CARD_LENGTH - IVALUE_START_INDEX - COMMENT_SEPARATOR.length()), "."));
                }
                break;
            case END:
                problemPrefix = "END keyword must be empty - ";
                if (rValue != null && !getRValueString().isEmpty()) {
                    problems.add(linkProblem(problemPrefix, "real value is '", getRValueString(), "'."));
                }
                if (iValue != null && !getIValueString().isEmpty()) {
                    problems.add(linkProblem(problemPrefix, "imaginary value is '", getIValueString(), "'."));
                }
                if (comment != null && !comment.isEmpty()) {
                    problems.add(linkProblem(problemPrefix, "comment is '", comment, "'."));
                }
                break;
            case HISTORY:
            case COMMENT:
                problemPrefix = keyword + " keyword must be empty - ";
                if (iValue != null && !getIValueString().isEmpty()) {
                    problems.add(linkProblem(problemPrefix, "imaginary value is '", getIValueString(), "'."));
                }
                if (comment != null && !comment.isEmpty()) {
                    problems.add(linkProblem(problemPrefix, "comment is '", comment, "'."));
                }
                break;
            case CONTINUE:
                if (iValue != null && !getIValueString().isEmpty()) {
                    problems.add(linkProblem("CONTINUE keyword must be empty - imaginary value is '", getIValueString(), "'."));
                }
                break;
            case BITPIX:
                int[] bitpixValues = {-64, -32, 8, 16, 32};
                if (realDataType == FitsKeywordsDataType.INT && Arrays.stream(bitpixValues).noneMatch(i -> i == (int) rValue)) {
                    StringJoiner stringJoiner = new StringJoiner(", ");
                    Arrays.stream(bitpixValues).forEach(i -> stringJoiner.add(Integer.toString(i)));
                    problems.add(linkProblem("BITPIX keyword has invalid real value ", getRValueString(), ", valid values are: ", stringJoiner.toString(), "."));
                }
                break;
            case NAXIS:
                if (realDataType == FitsKeywordsDataType.INT && ((int) rValue < 0 || (int) rValue > 999)) {
                    problems.add(linkProblem("NAXIS keyword must have real value in range of 0-999, but it has value ", getRValueString(), "."));
                }
                break;
            case NAXISn:
            case PEDESTAL:
                if (realDataType == FitsKeywordsDataType.INT && (int) rValue < 0) {
                    problems.add(linkProblem(keywordName, " keyword can not have real value negative."));
                }
                break;
        }
        return problems;
    }

    /**
     * Checks if given string contains only allowed characters
     *
     * @param string string to check
     * @return true if string contains only allowed characters, otherwise false
     */
    private boolean isStringContainingOnlyAllowedCharacters(String string) {
        if (string == null) {
            return true;
        }
        for (int ch : string.toCharArray()) {
            if (ch < 32 || ch > 126) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns string of card for saving FITS header
     *
     * @return string of card for saving FITS header, if card is not valid, returns null
     */
    public String getSaveRepresentation() {
        if (validate().isEmpty()) {
            StringBuilder result = new StringBuilder(FitsFile.BLANK_CARD_ENTRY);
            String value;
            int commentStarts;
            int continueCreated = 0;
            result.insert(0, getKeywordName());
            FitsKeywordsDataType dataType = getDataType();
            if (dataType != FitsKeywordsDataType.NONE) {
                result.setCharAt(8, '=');
            }
            if (keyword == FitsKeyword.COMMENT || keyword == FitsKeyword.HISTORY) {
                result.insert(FitsFile.KEYWORD_LENGTH, rValue);
            } else if (dataType == FitsKeywordsDataType.INT || dataType == FitsKeywordsDataType.REAL) {
                value = getRValueString();
                result.insert(IVALUE_START_INDEX - value.length(), value);
                value = getIValueString();
                if (value.isEmpty()) {
                    commentStarts = IVALUE_START_INDEX;
                } else {
                    result.insert(COMMENT_START_INDEX - value.length(), value);
                    commentStarts = COMMENT_START_INDEX;
                }
                value = getComment();
                if (value != null && !value.isEmpty()) {
                    result.insert(commentStarts, COMMENT_SEPARATOR + value);
                }
            } else if (dataType == FitsKeywordsDataType.LOGICAL) {
                if ((boolean) rValue) {
                    result.setCharAt(IVALUE_START_INDEX - 1, LOGICAL_CHAR_TRUE);
                } else {
                    result.setCharAt(IVALUE_START_INDEX - 1, LOGICAL_CHAR_FALSE);
                }
                value = getComment();
                if (value != null && !value.isEmpty()) {
                    result.insert(IVALUE_START_INDEX, COMMENT_SEPARATOR + value);
                }
            } else if (dataType == FitsKeywordsDataType.LITERAL || keyword == FitsKeyword.CONTINUE) {
                value = getEscapedValue();
                String commentContinue = getComment();
                int commentSeparatorNeeded = 0;
                if (!commentContinue.isEmpty()) {
                    commentSeparatorNeeded = 1;
                }
                while (true) {
                    int valueLength = value.length() + 2 * LITERAL_MARK.length();
                    int commentLength = COMMENT_SEPARATOR.length() * commentSeparatorNeeded + commentContinue.length();
                    int maxLength = valueLength + commentLength;
                    if (maxLength <= CONTINUE_VALUE_LENGTH) {
                        value = LITERAL_MARK + value + LITERAL_MARK;
                        if (!commentContinue.isEmpty()) {
                            value = value + COMMENT_SEPARATOR + commentContinue;
                        }
                        if (continueCreated != 0) {
                            result.insert(FitsFile.CARD_LENGTH * continueCreated, FitsFile.CONTINUE_CARD_PREFIX);
                        }
                        result.insert(RVALUE_START_INDEX + FitsFile.CARD_LENGTH * continueCreated, value);
                        break;
                    } else {
                        int partValueEndIndex;
                        int partCommentEndIndex = 0;
                        if (commentLength == 0) {
                            partValueEndIndex = value.lastIndexOf(' ', FitsFile.CARD_LENGTH - VALUE_LENGTH_RESERVED - 1) + 1;
                            if (partValueEndIndex == 0) {
                                partValueEndIndex = CONTINUE_VALUE_LENGTH - VALUE_LENGTH_RESERVED;
                            }
                        } else {
                            double ratioValue = ((double) (valueLength + CONTINUED_VALUE_END_MARK.length())) / maxLength;
                            double ratioComment = ((double) commentLength) / maxLength;
                            int maxValueLength = (int) (ratioValue * CONTINUE_VALUE_LENGTH) - VALUE_LENGTH_RESERVED;
                            int maxCommentLength = (int) (ratioComment * CONTINUE_VALUE_LENGTH) - COMMENT_SEPARATOR.length();
                            partValueEndIndex = value.lastIndexOf(' ', maxValueLength) + 1; // This I want with last space
                            if (partValueEndIndex == 0) {
                                partValueEndIndex = maxValueLength;
                            }
                            partCommentEndIndex = commentContinue.lastIndexOf(' ', maxCommentLength); //This I do not want with last space - I remove end trailing spaces
                            if (partCommentEndIndex == 0) {
                                partCommentEndIndex = maxCommentLength;
                            }
                        }
                        StringBuilder partValue = new StringBuilder(FitsFile.BLANK_CARD_ENTRY);
                        String insert;
                        int indexInsert = 0;
                        if (continueCreated != 0) {
                            insert = FitsFile.CONTINUE_CARD_PREFIX;
                            partValue.insert(indexInsert, insert);
                            indexInsert += insert.length();
                        }
                        insert = LITERAL_MARK;
                        partValue.insert(indexInsert, insert);
                        indexInsert += insert.length();
                        insert = value.substring(0, partValueEndIndex);
                        value = value.substring(partValueEndIndex);
                        partValue.insert(indexInsert, insert);
                        indexInsert += insert.length();
                        insert = CONTINUED_VALUE_END_MARK;
                        partValue.insert(indexInsert, insert);
                        indexInsert += insert.length();
                        insert = LITERAL_MARK;
                        partValue.insert(indexInsert, insert);
                        indexInsert += insert.length();
                        if (partCommentEndIndex != 0) {
                            insert = COMMENT_SEPARATOR;
                            partValue.insert(indexInsert, insert);
                            indexInsert += insert.length();
                            insert = commentContinue.substring(0, partCommentEndIndex);
                            commentContinue = commentContinue.substring(partCommentEndIndex);
                            partValue.insert(indexInsert, insert);
                        }
                        partValue.setLength(FitsFile.CARD_LENGTH);
                        if (continueCreated != 0) {
                            result.insert(FitsFile.CARD_LENGTH * continueCreated, partValue.toString());
                        } else {
                            result.insert(RVALUE_START_INDEX, partValue.toString());
                        }
                        continueCreated++;
                        if (commentContinue.isEmpty()) {
                            commentSeparatorNeeded = 0;
                        }
                    }
                }
            } else if (keyword == FitsKeyword.EMPTY && getComment() != null && !getComment().isEmpty()) {
                result.insert(IVALUE_START_INDEX, COMMENT_SEPARATOR + getComment());
            }
            result.setLength(FitsFile.CARD_LENGTH + continueCreated * FitsFile.CARD_LENGTH);
            return result.toString();
        } else {
            return null;
        }
    }

    /**
     * Computes date value
     */
    private void computeDateValue() {
        dateValue = null;
        if (rValue instanceof Double || rValue instanceof Integer) {
            if (((Number) rValue).doubleValue() >= 0.0) // valid julian days for used algorithm are only non negative
            {
                dateValue = FitsCardDateValue.createFromJulianDay(((Number) rValue).doubleValue());
                dateValue.setOwner(this);
            }
        } else if (rValue instanceof String) {
            try {
                dateValue = FitsCardDateValue.createFromDateString((String) rValue);
                dateValue.setOwner(this);
            } catch (FitsCardDateValueUnknownFormatException ignored) {
            }
        }
    }

    /**
     * Returns date value of this card
     *
     * @return date value of this card
     */
    public FitsCardDateValue getDateValue() {
        return dateValue;
    }

    /**
     * Returns linked parts of problem to string
     *
     * @param parts parts of problem
     * @return linked parts of problem to string
     */
    private String linkProblem(String... parts) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : parts) {
            stringBuilder.append(part);
        }
        return stringBuilder.toString();
    }
}
