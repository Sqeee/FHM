package cz.muni.sci.astro.fits;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Represents date value in FITS cards
 *
 * @author Jan Hlava, 395986
 */
public class FitsCardDateValue {
    private static final String[] acceptableFormats = {
            "uuuu-MM-dd'T'HH:mm:ss", "uuuu-MM-dd'T'H:mm:ss",
            "uuuu-MM-dd HH:mm:ss", "uuuu-MM-dd H:mm:ss",
            "MM/dd/uuuu HH:mm:ss", "MM/dd/uuuu H:mm:ss",
            "MM/dd/uuuu'T'HH:mm:ss", "MM/dd/uuuu'T'H:mm:ss",
            "uuuu-MM-dd", "MM/dd/yyyy", "HH:mm:ss", "H:mm:ss"
    };
    private static final int MAX_FRACTION_DIGITS = 9;

    private FitsCard owner;
    private LocalDateTime date;
    private LocalDateTime originDate;
    private Double originJD;
    private DateTimeFormatter formatter;
    private int formatIndex;

    /**
     * Creates new date value with now time and default format -> 2015-04-10T16:47:54.415
     */
    public FitsCardDateValue() {
        date = LocalDateTime.now();
        originDate = date.withYear(date.getYear()); // creates copy
        formatIndex = 0;
        formatter = createDateTimeFormatter(acceptableFormats[0]);
    }

    /**
     * Creates new date value from given text representation of date
     *
     * @param text text representation of date
     * @return new date value created form given text representation of date
     * @throws FitsCardDateValueUnknownFormatException throws when given text has unknown date format
     */
    public static FitsCardDateValue createFromDateString(String text) throws FitsCardDateValueUnknownFormatException {
        FitsCardDateValue newDateValue = new FitsCardDateValue();
        DateTimeFormatter formatter;
        for (String format : acceptableFormats) {
            formatter = createDateTimeFormatter(format);
            try {
                if (format.contains("u") && format.contains("m")) {
                    newDateValue.date = LocalDateTime.parse(text, formatter);
                } else if (format.contains("u")) {
                    newDateValue.date = LocalDate.parse(text, formatter).atTime(0, 0);
                } else {
                    newDateValue.date = LocalTime.parse(text, formatter).atDate(LocalDate.of(-4713, 11, 25));
                }
                newDateValue.formatter = formatter;
                newDateValue.formatIndex = getAcceptableFormats().indexOf(format);
                newDateValue.originDate = newDateValue.date.withYear(newDateValue.date.getYear());
                return newDateValue;
            } catch (DateTimeParseException ignored) {
            }
        }
        // if no value was not returned, throws exception
        throw new FitsCardDateValueUnknownFormatException(text + " has unknown date format");
    }

    /**
     * Creates new date vale from given julian day, converting is made by algorithm described on https://en.wikipedia.org/wiki/Julian_day#Julian_or_Gregorian_calendar_from_Julian_day_number
     *
     * @param julianDay julian day for conversion
     * @return new data value created from given julian day
     */
    public static FitsCardDateValue createFromJulianDay(double julianDay) {
        FitsCardDateValue newDateValue = new FitsCardDateValue();
        newDateValue.originJD = julianDay;
        double jdn = Math.floor(julianDay + 0.5);
        double fraction = (julianDay + 0.5) % 1;
        double f = jdn + 1401 + Math.floor((Math.floor((4 * jdn + 274277) / 146097) * 3) / 4) - 38;
        double e = 4 * f + 3;
        double g = Math.floor((e % 1461) / 4);
        double h = g * 5 + 2;
        int day = (int) Math.floor((h % 153) / 5) + 1;
        int month = (int) ((Math.floor(h / 153) + 2) % 12) + 1;
        int year = (int) Math.floor(e / 1461) - 4716 + ((14 - month) / 12);
        if (year < 1) //skip year 0
        {
            year -= 1;
        }
        int hour = (int) (fraction * 24);
        fraction = fraction * 24 - hour;
        int minute = (int) (fraction * 60);
        fraction = fraction * 60 - minute;
        int second = (int) (fraction * 60);
        fraction = fraction * 60 - second;
        int nano = (int) (fraction * 1000000000);
        newDateValue.date = LocalDateTime.parse("2016-01-01T00:00:00").withDayOfMonth(day).withMonth(month).withYear(year).withHour(hour).withMinute(minute).withSecond(second).withNano(nano);
        if (newDateValue.date.getNano() == 999999999) // Fix some flooring problems
        {
            newDateValue.date = newDateValue.date.plusNanos(1);
        }
        newDateValue.originDate = newDateValue.date.withYear(newDateValue.date.getYear()); // creates copy
        return newDateValue;
    }

    /**
     * Returns list of acceptable formats
     *
     * @return list of acceptable formats
     */
    public static List<String> getAcceptableFormats() {
        return Arrays.asList(acceptableFormats);
    }

    /**
     * Returns date time formatter created from given format
     *
     * @param format format for new date time formatter
     * @return date time formatter created from given format
     */
    private static DateTimeFormatter createDateTimeFormatter(String format) {
        return new DateTimeFormatterBuilder().appendPattern(format).appendFraction(ChronoField.NANO_OF_SECOND, 0, MAX_FRACTION_DIGITS, true).toFormatter();
    }

    /**
     * Parses JD string and returns FitsCardDateValue
     *
     * @param jdString string containing JD
     * @return FitsCardDateValue from parsed JD string
     * @throws NumberFormatException throws it, if string is not parsable as JD
     */
    public static FitsCardDateValue parseJDString(String jdString) throws NumberFormatException {
        Number jd;
        if (jdString.contains(".")) {
            jd = Double.parseDouble(jdString);
        } else {
            jd = Integer.parseInt(jdString);
        }
        if (jd.doubleValue() >= 0.0) {
            return createFromJulianDay(jd.doubleValue());
        } else {
            throw new NumberFormatException("JD should not be negative.");
        }
    }

    /**
     * Do date shift
     *
     * @param value    value to be shifted
     * @param interval interval to be shifted
     */
    public void doShift(int value, ChronoUnit interval) {
        date = date.plus(value, interval);
        updateValueInOwner();
    }

    /**
     * Returns Julian Day, converting is made by algorithm described on https://en.wikipedia.org/wiki/Julian_day#Converting_Julian_or_Gregorian_calendar_date_to_Julian_day_number
     *
     * @return Julian Day
     */
    public double getJulianDay() {
        if (date.equals(originDate) && originJD != null) // if date was not changed, show original JD - the reason for this is, that computed value can be affected by flooring
        {
            return originJD;
        }
        int a = (14 - date.getMonthValue()) / 12;
        int year = date.getYear();
        if (year < 0) // Fix skipping year 0
        {
            year += 1;
        }
        int y = year + 4800 - a;
        int m = date.getMonthValue() + 12 * a - 3;
        int jdn = date.getDayOfMonth() + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        return jdn + (date.getHour() - 12) / 24.0 + date.getMinute() / 1440.0 + (date.getSecond()) / 86400.0 + (date.getNano() / 86400000000000.0);
    }

    /**
     * Returns date value
     *
     * @return date value
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Returns string representation of this date
     *
     * @return string representation of this date
     */
    @Override
    public String toString() {
        return date.format(formatter);
    }

    /**
     * Checks if date value was created from classical date
     *
     * @return true if date value was created from classical date, otherwise false
     */
    public boolean isCreatedFromClassicDate() {
        return originJD == null;
    }

    /**
     * Returns string representation of format
     *
     * @return string representation of format
     */
    public String getFormat() {
        return acceptableFormats[formatIndex];
    }

    /**
     * Sets format of date
     *
     * @param format format of date
     * @throws FitsCardDateValueUnknownFormatException throws exception in case of given format is not in accepted formats
     */
    public void setFormat(String format) throws FitsCardDateValueUnknownFormatException {
        List<String> acceptedFormats = getAcceptableFormats();
        if (acceptedFormats.contains(format)) {
            formatter = createDateTimeFormatter(format);
            formatIndex = acceptedFormats.indexOf(format);
            updateValueInOwner();
        } else {
            throw new FitsCardDateValueUnknownFormatException("Unknown format");
        }
    }

    /**
     * Sets owner of this date time value
     *
     * @param owner card to which belongs this date time value
     */
    public void setOwner(FitsCard owner) {
        this.owner = owner;
    }

    /**
     * Updates value in owner to be up to date
     */
    private void updateValueInOwner() {
        if (owner != null) {
            if (isCreatedFromClassicDate()) {
                owner.setRValue(toString());
            } else {
                owner.setRValue(Double.toString(getJulianDay()));
            }
        }
    }
}
