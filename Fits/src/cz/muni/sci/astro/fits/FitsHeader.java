package cz.muni.sci.astro.fits;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents FITS header structure
 *
 * @author Jan Hlava, 395986
 */
public class FitsHeader
{
    private List<FitsCard> cards;

    /**
     * Creates new Header from given RandomAccessFile
     *
     * @param raf RandomAccessFile instance of FITS file with file pointer set to the beginning of Header
     * @throws FitsFileException if cannot read Header
     * @throws FitsCardBadFormatException if Card format is invalid
     */
    public FitsHeader(RandomAccessFile raf) throws FitsException
    {
        cards = new ArrayList<>();
        byte[] bytes = new byte[FitsFile.BLOCK_LENGTH];
        String card;
        try
        {
            while (true)
            {
                raf.readFully(bytes);
                for (int i = 0; i < FitsFile.CARD_BLOCK_ENTRIES; i++)
                {
                    card = new String(bytes, i * FitsFile.CARD_LENGTH, FitsFile.CARD_LENGTH);
                    FitsCard fitsCardNew = new FitsCard(card);
                    if (fitsCardNew.getKeyword() != FitsKeyword.CONTINUE)
                    {
                        cards.add(fitsCardNew);
                        if (fitsCardNew.getKeyword() == FitsKeyword.END)
                        {
                            return;
                        }
                    }
                    // Value from CONTINUE keyword is attached to previous card
                    else if (!cards.isEmpty())
                    {
                        FitsCard fitsCard = cards.get(cards.size() - 1);
                        if (fitsCard.getDataType() == FitsKeywordsDataType.LITERAL)
                        {
                            String value = fitsCard.getRValueString();
                            String comment = fitsCard.getComment();
                            if (value.endsWith("&"))
                            {
                                fitsCard.setRValue(value.substring(0, value.length() - 1) + fitsCardNew.getRValueString());
                                fitsCard.incrementCountContinue();
                                fitsCard.setComment(comment.substring(0, comment.length()) + fitsCardNew.getComment());
                            }
                            else
                            {
                                throw new FitsCardBadFormatException("Keyword Continue needs card which ends with &");
                            }
                        }
                        else
                        {
                            throw new FitsCardBadFormatException("Keyword Continue needs card with literal data type");
                        }
                    }
                    else
                    {
                        throw new FitsCardBadFormatException("Keyword Continue cannot be first");
                    }
                }
            }
        }
        catch (IOException exc)
        {
            throw new FitsFileException("Cannot read fits header", exc);
        }
    }

    /**
     * Returns cards from this Header
     *
     * @return cards from this Header
     */
    public List<FitsCard> getCards()
    {
        return cards;
    }

    /**
     * Returns cards with given keyword from HDU
     *
     * @param keyword keyword, which should cards contains
     * @return cards with given keyword from HDU
     */
    public List<FitsCard> getCardsWithKeyword(String keyword)
    {
        return cards.stream().filter(card -> card.getKeywordName().equals(keyword)).collect(Collectors.toList());
    }

    /**
     * Returns JavaScript representation of cards from this Header
     *
     * @return JavaScript representation of cards from this Header
     */
    public String getJavaScriptKeywordsAndValues()
    {
        return cards.stream().map(FitsCard::toStringJavaScript).collect(Collectors.joining());
    }

    /**
     * Sets new list of cards
     *
     * @param cards list of cards, which should be set
     */
    public void setCards(List<FitsCard> cards)
    {
        this.cards = cards;
    }

    /**
     * Checks if header can be saved and returns list of saving problems
     *
     * @return list of saving problems
     */
    public List<String> checkSaveHeader()
    {
        List<String> problems = new ArrayList<>();
        for (FitsCard card : cards)
        {
            problems.addAll(card.validate());
        }
        if (cards.size() < 4)
        {
            problems.add("Header must have at least 4 cards - SIMPLE, BITPIX, NAXIS and END.");
        }
        else
        {
            Boolean bitpixPositive = null;
            int naxis = -1;
            if (cards.get(0).getKeyword() != FitsKeyword.SIMPLE)
            {
                problems.add("First keyword in the header must be SIMPLE.");
            }
            if (cards.get(1).getKeyword() != FitsKeyword.BITPIX)
            {
                problems.add("Second keyword in the header must be BITPIX.");
            }
            else if (cards.get(1).getDataType() == FitsKeywordsDataType.INT)
            {
                bitpixPositive = (int)cards.get(1).getRValue() > 0;
            }
            if (cards.get(2).getKeyword() != FitsKeyword.NAXIS)
            {
                problems.add("Third keyword in the header must be NAXIS.");
            }
            else if (cards.get(2).getDataType() == FitsKeywordsDataType.INT && (int)cards.get(2).getRValue() >= 0)
            {
                naxis = (int)cards.get(2).getRValue();
                if (naxis > 0 && !cards.get(3).getKeywordName().equals(FitsKeyword.NAXISn.toString().replace("n", "1")))
                {
                    problems.add("If count of axises is bigger than zero, then after keyword " + cards.get(2).getKeywordName() + " must be keyword " + FitsKeyword.NAXISn.toString().replace("n", "1") + '.');
                }
            }
            if (cards.get(cards.size() - 1).getKeyword() != FitsKeyword.END)
            {
                problems.add("Last keyword in the header must be END.");
            }
            FitsImageType imageType = getFitsImageType();
            Set<String> keywordNames = new HashSet<>();
            Set<FitsKeyword> mandatoryKeywordsMissing = new HashSet<>(FitsKeyword.getImageTypeMandatory(imageType));
            Set<FitsKeyword> mandatoryNKeywordsMissing = new HashSet<>(FitsKeyword.getImageTypeNMandatory(imageType));
            for (int i = 0; i < cards.size(); i++)
            {
                FitsCard card = cards.get(i);
                mandatoryKeywordsMissing.remove(card.getKeyword());
                mandatoryNKeywordsMissing.remove(card.getKeyword());
                if (card.getKeyword().isUniqueHeaderKeyword() && keywordNames.contains(card.getKeywordName()))
                {
                    problems.add("Keyword " + card.getKeywordName() + " is header unique keyword, but there are more keywords in the header.");
                }
                else
                {
                    keywordNames.add(card.getKeywordName());
                }
                if (card.getKeyword() == FitsKeyword.BLANK && bitpixPositive != Boolean.TRUE)
                {
                    problems.add("Keyword " + card.getKeywordName() + " can be used only if keyword " + FitsKeyword.BITPIX.toString() + " has positive value.");
                }
                else if (card.getKeyword().isNAsNAXIS() && card.getNParam() > naxis)
                {
                    problems.add("Count of axises is " + naxis + " so keyword " + card.getKeywordName() + " cannot be used.");
                }
                if (card.getKeyword().isNKeyword() && card.getNParam() > 1)
                {
                    if (i <= 0 || cards.get(i - 1).getKeyword() != card.getKeyword() || cards.get(i - 1).getNParam() + 1 != card.getNParam())
                    {
                        problems.add("Keyword " + card.getKeywordName() + " is not properly ordered, n keywords must start at 1 and continually increment.");
                    }
                }
            }
            problems.addAll(mandatoryKeywordsMissing.stream().map(keyword -> "Keyword " + keyword.toString() + " is mandatory and it is not in header.").collect(Collectors.toList()));
            problems.addAll(mandatoryNKeywordsMissing.stream().map(keyword -> "Keywords " + keyword.toString() + " are mandatory and they are not in header.").collect(Collectors.toList()));
        }
        return problems;
    }

    /**
     * Returns type of image
     *
     * @return type of image
     */
    public FitsImageType getFitsImageType()
    {
        List<FitsCard> cardsImageType = getCardsWithKeyword("IMAGETYP");
        if (cardsImageType.isEmpty())
        {
            return FitsImageType.UNKNOWN;
        }
        try
        {
            return FitsImageType.valueOf(cardsImageType.get(0).getRValueString().replace(" ", "_").toUpperCase());
        }
        catch (IllegalArgumentException exc)
        {
            return FitsImageType.UNKNOWN;
        }
    }

    /**
     * Returns save representation of header
     *
     * @return save representation of header
     */
    public byte[] getSaveRepresentation()
    {
        if (!checkSaveHeader().isEmpty())
        {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder(cards.size() * FitsFile.CARD_LENGTH);
        for (FitsCard card : cards)
        {
            stringBuilder.append(card.getSaveRepresentation());
        }
        int cardsMissing = (FitsFile.CARD_BLOCK_ENTRIES - ((stringBuilder.length() / FitsFile.CARD_LENGTH) % FitsFile.CARD_BLOCK_ENTRIES)) % FitsFile.CARD_BLOCK_ENTRIES;
        for (int i = 0; i < cardsMissing; i++)
        {
            stringBuilder.append(FitsFile.BLANK_CARD_ENTRY);
        }
        return stringBuilder.toString().getBytes();
    }
}
