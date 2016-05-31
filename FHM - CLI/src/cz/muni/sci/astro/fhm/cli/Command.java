package cz.muni.sci.astro.fhm.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Enum definitions of known commands
 *
 * @author Jan Hlava, 395986
 */
public enum Command {
    FILE,
    DIR,
    CANCEL_FILTERS,
    FILTER_FILENAME,
    FILTER_KEYWORD,
    FILTER_KEYWORD_RVALUE,
    FILTER_KEYWORD_IVALUE,
    ADD_CARD,
    ADD_CARD_INDEX,
    ADD_CARD_AFTER_KEYWORD,
    REMOVE_CARD,
    REMOVE_CARD_INDEX,
    CHANGE_KEYWORD,
    CHANGE_RVALUE,
    CHANGE_IVALUE,
    CHANGE_COMMENT,
    CHANGE_INDEX,
    CONCATENATE,
    SHIFT,
    JD,
    JD_KEYWORD;

    private static final String PARAM_INFO_P = "-p - optional param, if it is used, it means apply this filter to already filtered files";
    private static final String PARAM_INFO_U = "-u - optional param, if it is used than existing keyword will be overwritten instead of throwing error, for non unique header keywords (for example empty card) is this option ignored and they are added, not updated";
    private static final String PARAM_INFO_ADD_KEYWORD = "keyword - keyword of added card";
    private static final String PARAM_INFO_ADD_RVALUE = "rvalue - real value of added card";
    private static final String PARAM_INFO_ADD_COMMENT = "comment - optional param, if it is used, it is comment of added card, if not, it is empty";
    private static final String PARAM_INFO_ADD_IVALUE = "ivalue - optional param (if you want use it, you must use previous comment param), if it is used, it is imaginary value of added card, if not, it is empty";
    private static final String PARAM_INFO_CHANGE_KEYWORD = "keyword - which card with this keyword should be changed";

    /**
     * Returns list of params for command
     *
     * @return list about params for command
     */
    public String paramList() {
        switch (this) {
            case FILE:
                return "file...";
            case DIR:
                return "directory";
            case CANCEL_FILTERS:
                return "";
            case FILTER_FILENAME:
                return "[-p] filter_string";
            case FILTER_KEYWORD:
                return "[-p] keyword...";
            case FILTER_KEYWORD_RVALUE:
                return "[-p] keyword rvalue";
            case FILTER_KEYWORD_IVALUE:
                return "[-p] keyword ivalue";
            case ADD_CARD:
                return "[-u] keyword rvalue [comment [ivalue]]";
            case ADD_CARD_INDEX:
                return "[-u] index keyword rvalue [comment [ivalue]]";
            case ADD_CARD_AFTER_KEYWORD:
                return "[-u] after_keyword keyword rvalue [comment [ivalue]]";
            case REMOVE_CARD:
                return "keyword";
            case REMOVE_CARD_INDEX:
                return "index";
            case CHANGE_KEYWORD:
                return "[-d] old_keyword new_keyword";
            case CHANGE_RVALUE:
                return "keyword rvalue";
            case CHANGE_IVALUE:
                return "keyword ivalue";
            case CHANGE_COMMENT:
                return "keyword comment";
            case CHANGE_INDEX:
                return "keyword index";
            case CONCATENATE:
                return "[-u] keyword (-s string | -k keyword_value)...";
            case SHIFT:
                return "keyword [-y year | -m month | -d day | -h hour | -min minute | -s second | -ms millisecond | -mics microsecond]+";
            case JD:
                return "[-u] keyword date_time";
            case JD_KEYWORD:
                return "[-u] keyword source_keyword";
            default:
                return "";
        }
    }

    /**
     * Returns params with description
     *
     * @return params with description
     */
    public List<String> paramsInfo() {
        List<String> result = new ArrayList<>();
        switch (this) {
            case FILE:
                result.add("file... - one or more files which you want to process");
                break;
            case DIR:
                result.add("directory - directory from which you want to process files");
                break;
            case CANCEL_FILTERS:
                break;
            case FILTER_FILENAME:
                result.add(PARAM_INFO_P);
                result.add("filter_string - string used to filtration filenames");
                break;
            case FILTER_KEYWORD:
                result.add(PARAM_INFO_P);
                result.add("keyword - one or more keywords, which should be stored in files");
                break;
            case FILTER_KEYWORD_RVALUE:
                result.add(PARAM_INFO_P);
                result.add("keyword - keyword, which should be stored in files");
                result.add("rvalue - real value, which given keyword should have stored");
                break;
            case FILTER_KEYWORD_IVALUE:
                result.add(PARAM_INFO_P);
                result.add("keyword - keyword, which should be stored in files");
                result.add("ivalue - imaginary value, which given keyword should have stored");
                break;
            case ADD_CARD:
                result.add(PARAM_INFO_U);
                result.add(PARAM_INFO_ADD_KEYWORD);
                result.add(PARAM_INFO_ADD_RVALUE);
                result.add(PARAM_INFO_ADD_COMMENT);
                result.add(PARAM_INFO_ADD_IVALUE);
                break;
            case ADD_CARD_INDEX:
                result.add(PARAM_INFO_U);
                result.add("index - index where new card should be inserted, index can be negative (position is counted from the end), index starts at 1 or -1, index 0 is invalid");
                result.add(PARAM_INFO_ADD_KEYWORD);
                result.add(PARAM_INFO_ADD_RVALUE);
                result.add(PARAM_INFO_ADD_COMMENT);
                result.add(PARAM_INFO_ADD_IVALUE);
                break;
            case ADD_CARD_AFTER_KEYWORD:
                result.add(PARAM_INFO_U);
                result.add("after_keyword - after which keyword a new card should be inserted");
                result.add(PARAM_INFO_ADD_KEYWORD);
                result.add(PARAM_INFO_ADD_RVALUE);
                result.add(PARAM_INFO_ADD_COMMENT);
                result.add(PARAM_INFO_ADD_IVALUE);
                break;
            case REMOVE_CARD:
                result.add("keyword - which keyword deleted card should contain");
                break;
            case REMOVE_CARD_INDEX:
                result.add("index - from which index card should be deleted, index can be negative (position is counted from the end), index starts at 1 or -1, index 0 is invalid");
                break;
            case CHANGE_KEYWORD:
                result.add("-d - optional param, if it is used than existing keyword will be deleted instead of throwing error, for non unique header keywords (for example empty card) is this option ignored and they are updated");
                result.add("old_keyword - which keyword should be changed");
                result.add("new_keyword - new value of keyword");
                break;
            case CHANGE_RVALUE:
                result.add(PARAM_INFO_CHANGE_KEYWORD);
                result.add("rvalue - new real value");
                break;
            case CHANGE_IVALUE:
                result.add(PARAM_INFO_CHANGE_KEYWORD);
                result.add("ivalue - new imaginary value");
                break;
            case CHANGE_COMMENT:
                result.add(PARAM_INFO_CHANGE_KEYWORD);
                result.add("comment - new comment value");
                break;
            case CHANGE_INDEX:
                result.add(PARAM_INFO_CHANGE_KEYWORD);
                result.add("index - new index of card, index can be negative (position is counted from the end), index starts at 1 or -1, index 0 is invalid");
                break;
            case CONCATENATE:
                result.add(PARAM_INFO_U);
                result.add("keyword - to which keyword should be concatenation result added, if it is new keyword in primary HDU, it will be added before END keyword, if it is already used, you should use -u param");
                result.add("-s string - string used to concatenation, just replace string with your string, if string contains spaces, you must use form with \": \"-s string with spaces\", this param can be used any number of times");
                result.add("-s keyword_value - keyword's value used to concatenation, just replace keyword_value with keyword, example: -k NAXIS, this param can be used any number of times");
                break;
            case SHIFT:
                result.add("keyword - datetime value in card with this keyword will be shifted");
                result.add("-y year - number for shifting years, just replace year with number, it is optional param");
                result.add("-m month - number for shifting months, just replace month with number, it is optional param");
                result.add("-d day - number for shifting days, just replace day with number, it is optional param");
                result.add("-h hour - number for shifting hours, just replace hour with number, it is optional param");
                result.add("-min minute - number for shifting minutes, just replace minute with number, it is optional param");
                result.add("-s second - number for shifting seconds, just replace second with number, it is optional param");
                result.add("-ms millisecond - number for shifting milliseconds, just replace millisecond with number, it is optional param");
                result.add("-mics microsecond - number for shifting microseconds, just replace microsecond with number, it is optional param");
                break;
            case JD:
                result.add(PARAM_INFO_U);
                result.add("keyword - to which keyword will be computed julian day stored");
                result.add("date_time - date time value as input for computing julian day");
                break;
            case JD_KEYWORD:
                result.add(PARAM_INFO_U);
                result.add("keyword - to which keyword will be computed julian day stored");
                result.add("source_keyword - from which keyword value will be computed julian day");
                break;
        }
        return result;
    }

    /**
     * Returns short description of command
     *
     * @return short description of command
     */
    public String shortDescription() {
        switch (this) {
            case FILE:
                return "Loads given files for processing.";
            case DIR:
                return "Loads files from given directory for processing.";
            case CANCEL_FILTERS:
                return "Cancels all used filters.";
            case FILTER_FILENAME:
                return "Filters opened files by their filenames.";
            case FILTER_KEYWORD:
                return "Filters opened files by containing keywords.";
            case FILTER_KEYWORD_RVALUE:
                return "Filters opened files by containing keyword with given real value.";
            case FILTER_KEYWORD_IVALUE:
                return "Filters opened files by containing keyword with given imaginary value.";
            case ADD_CARD:
                return "Adds new card with given values to primary HDU.";
            case ADD_CARD_INDEX:
                return "Adds new card with given values to primary HDU on given index.";
            case ADD_CARD_AFTER_KEYWORD:
                return "Adds new card with given values to primary HDU after given keyword.";
            case REMOVE_CARD:
                return "Removes card with given keyword from primary HDU.";
            case REMOVE_CARD_INDEX:
                return "Removes card from given index from primary HDU.";
            case CHANGE_KEYWORD:
                return "Changes old keyword to new keyword in primary HDU";
            case CHANGE_RVALUE:
                return "Changes real value in card with given keyword in primary HDU";
            case CHANGE_IVALUE:
                return "Changes imaginary value in card with given keyword in primary HDU";
            case CHANGE_COMMENT:
                return "Changes comment value in card with given keyword in primary HDU";
            case CHANGE_INDEX:
                return "Changes index of card in primary HDU";
            case CONCATENATE:
                return "Concatenates strings and keyword's values and adds result of concatenation to given keyword in primary HDU";
            case SHIFT:
                return "Shifts date and time values in given keywords in primary HDU by given date and time interval";
            case JD:
                return "Computes julian day from given date and time and stores it into card with given keyword in primary HDU";
            case JD_KEYWORD:
                return "Computes julian day from value of source keyword and stores it into card with given keyword in primary HDU";
            default:
                return "";
        }
    }

    /**
     * Returns long description of command
     *
     * @return long description of command
     */
    public String longDescription() {
        StringJoiner description = new StringJoiner(System.lineSeparator());
        description.add(shortDescription());
        if (!paramList().isEmpty()) {
            description.add("Params: " + paramList());
            paramsInfo().forEach(description::add);
        } else {
            description.add("No params.");
        }
        return description.toString();
    }

    /**
     * Returns pattern for matching command syntax
     *
     * @return pattern for matching command syntax
     */
    public String pattern() {
        switch (this) {
            case FILE:
                return "(?:(FILE)((?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\")))+))";
            case DIR:
                return "(?:(DIR)( (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))))";
            case CANCEL_FILTERS:
                return "(?:(CANCEL_FILTERS))";
            case FILTER_FILENAME:
                return "(?:(FILTER_FILENAME)((?! -p\\z)(?: -p)? (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))))";
            case FILTER_KEYWORD:
                return "(?:(FILTER_KEYWORD)((?! -p\\z)(?: -p)?(?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\")))+))";
            case FILTER_KEYWORD_RVALUE:
                return "(?:(FILTER_KEYWORD_RVALUE)((?! -p (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -p)?(?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2}))";
            case FILTER_KEYWORD_IVALUE:
                return "(?:(FILTER_KEYWORD_IVALUE)((?! -p (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -p)?(?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2}))";
            case ADD_CARD:
                return "(?:(ADD_CARD)((?! -u (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -u)?( (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2,4}))";
            case ADD_CARD_INDEX:
                return "(?:(ADD_CARD_INDEX)((?! -u -?\\d+ (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -u)?( -?\\d+(?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2,4})))";
            case ADD_CARD_AFTER_KEYWORD:
                return "(?:(ADD_CARD_AFTER_KEYWORD)((?! -u (?:(?:[\\S&&[^\"]]{2,})|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -u)?( (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){3,5}))";
            case REMOVE_CARD:
                return "(?:(REMOVE_CARD)( (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))))";
            case REMOVE_CARD_INDEX:
                return "(?:(REMOVE_CARD_INDEX)( -?\\d+))";
            case CHANGE_KEYWORD:
                return "(?:(CHANGE_KEYWORD)((?! -d (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -d)?(?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2}))";
            case CHANGE_RVALUE:
                return "(?:(CHANGE_RVALUE)((?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2}))";
            case CHANGE_IVALUE:
                return "(?:(CHANGE_IVALUE)((?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2}))";
            case CHANGE_COMMENT:
                return "(?:(CHANGE_COMMENT)((?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2}))";
            case CHANGE_INDEX:
                return "(?:(CHANGE_INDEX)( (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\")) -?\\d+))";
            case CONCATENATE:
                return "(?:(CONCATENATE)((?! -u (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -u)?(?: (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))(?: (?:(?:-[sk] [\\S&&[^\"]]+)|(?:-[sk] \"(?:[\\S &&[^\"]]|\\\\\")*\")))+)))";
            case SHIFT:
                return "(?:(SHIFT)( (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))(?: (?:(?:-[ymdhs](?:in|s|ics)? -?\\d+)|(?:-[ymdhs](?:in|s|ics)? \"-?\\d+\")))+))";
            case JD:
                return "(?:(JD)((?:(?! -u (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -u)? (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2}))";
            case JD_KEYWORD:
                return "(?:(JD_KEYWORD)((?:(?! -u (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))\\z)(?: -u)? (?:(?:[\\S&&[^\"]]+)|(?:\"(?:[\\S &&[^\"]]|\\\\\")*\"))){2}))";
            default:
                return "(?: XXX )";
        }
    }
}
