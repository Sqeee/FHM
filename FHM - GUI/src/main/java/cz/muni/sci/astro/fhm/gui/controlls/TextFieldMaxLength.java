package cz.muni.sci.astro.fhm.gui.controlls;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;

/**
 * TextField with max length
 *
 * @author Jan Hlava, 395986
 */
public class TextFieldMaxLength extends TextField {
    private static final int DEFAULT_MAX_LENGTH = 8;
    private IntegerProperty maxLength;

    /**
     * Creates new instance of this component
     */
    public TextFieldMaxLength() {
        this("", DEFAULT_MAX_LENGTH);
    }

    /**
     * Creates new instance of this component
     *
     * @param text      initial text to show in component
     * @param maxLength max allowed length of this component
     * @throws IllegalArgumentException if maxLength is negative
     */
    public TextFieldMaxLength(String text, int maxLength) {
        super(text);
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength cannot be negative");
        }
        this.maxLength = new SimpleIntegerProperty(maxLength);
    }

    /**
     * Handles replacing text in given range with given text
     *
     * @param range range of text to replace
     * @param text  new text
     */
    @Override
    public void replaceText(IndexRange range, String text) {
        replaceText(range.getStart(), range.getEnd(), text);
    }

    /**
     * Handles replacing text in given range with given text
     *
     * @param start index where replacement starts
     * @param end   index where replacement ends
     * @param text  new text
     */
    @Override
    public void replaceText(int start, int end, String text) {
        if (text.isEmpty()) {
            super.replaceText(start, end, text);
        } else {
            super.replaceText(start, end, text.substring(0, Math.min(getMaxLength() - (getText().length() - end + start), text.length())));
        }
    }

    /**
     * Handles replacing selected text with given text
     *
     * @param replacement new text
     */
    @Override
    public void replaceSelection(String replacement) {
        if (replacement.isEmpty()) {
            super.replaceSelection(replacement);
        } else {
            super.replaceSelection(replacement.substring(0, Math.min(getMaxLength() - (getText().length() - getSelectedText().length()), replacement.length())));
        }
    }

    /**
     * Returns integer property of max length
     *
     * @return integer property of max length
     */
    public IntegerProperty maxLengthProperty() {
        return maxLength;
    }

    /**
     * Returns int value of max length property
     *
     * @return int value of max length property
     */
    public int getMaxLength() {
        return maxLength.get();
    }

    /**
     * Sets max length property
     *
     * @param maxLength new max length (cannot be negative)
     */
    public void setMaxLength(int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("max length cannot be negative");
        }
        this.maxLength.set(maxLength);
        setText(getText().substring(0, Math.min(getMaxLength(), getText().length())));
    }
}
