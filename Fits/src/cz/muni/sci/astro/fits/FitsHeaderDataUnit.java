package cz.muni.sci.astro.fits;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * Represents FITS Header Data Unit structure
 *
 * @author Jan Hlava, 395986
 */
public class FitsHeaderDataUnit
{
    private final FitsHeader header;
    private final RandomAccessFile headerFile;
    private long headerFileBeginOffset;
    private long headerFileEndOffset;
    private long dataUnitLength;

    /**
     * Creates new HDU from given RandomAccessFile
     *
     * @param raf RandomAccessFile instance of FITS file with file pointer set to the beginning of HDU
     * @throws FitsFileException if cannot get file header offset
     */
    public FitsHeaderDataUnit(RandomAccessFile raf) throws FitsException
    {
        headerFile = raf;
        try
        {
            headerFileBeginOffset = raf.getFilePointer();
            header = new FitsHeader(raf);
            headerFileEndOffset = raf.getFilePointer();
            int bitpix = Math.abs((int) header.getCardsWithKeyword("BITPIX").get(0).getRValue()) / 8;
            int countNaxis = (int) header.getCardsWithKeyword("NAXIS").get(0).getRValue();
            dataUnitLength = 1;
            for (int i = 1; i <= countNaxis; i++)
            {
                dataUnitLength *= (int) header.getCardsWithKeyword("NAXIS" + i).get(0).getRValue();
            }
            dataUnitLength *= bitpix;
            dataUnitLength += ((FitsFile.BLOCK_LENGTH - (dataUnitLength % FitsFile.BLOCK_LENGTH)) % FitsFile.BLOCK_LENGTH);
            raf.seek(headerFileEndOffset + dataUnitLength);
        }
        catch (IOException exc)
        {
            throw new FitsFileException("Cannot get file header offset", exc);
        }
    }

    /**
     * Returns header from this HDU
     *
     * @return header from this HDU
     */
    public FitsHeader getHeader()
    {
        return header;
    }

    /**
     * Returns type of image in this HDU
     *
     * @return type of image in this HDU
     */
    public FitsImageType getFitsImageType()
    {
        List<FitsCard> cardsImageType = header.getCardsWithKeyword("IMAGETYP");
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
     * Returns position in FitsFile, where HDU ends (possible next HDU starts this + 1)
     *
     * @return position in FitsFile, where HDU ends (possible next HDU starts this + 1)
     */
    public long getHDUEndPosition()
    {
        return headerFileEndOffset + dataUnitLength - 1;
    }

    /**
     * Tries to save HDU
     *
     * @param copy copy of FITS file
     * @param beginHDUPosition new begin hdu position
     * @param changedEndHDUPosition if end HDU position was changed
     * @return true if saving was successful, false if saving failed
     */
    public boolean saveHDU(RandomAccessFile copy, long beginHDUPosition, boolean changedEndHDUPosition)
    {
        byte[] headerText = header.getSaveRepresentation();
        byte[] copyBytes = new byte[FitsFile.BLOCK_LENGTH];
        if (changedEndHDUPosition)
        {
            headerFileEndOffset += (beginHDUPosition - headerFileBeginOffset);
        }
        if (headerText != null)
        {
            try
            {
                headerFile.seek(beginHDUPosition);
                headerFile.write(headerText);
                long endHDUPosition = headerFile.getFilePointer();
                if (headerFileEndOffset != endHDUPosition)
                {
                    copy.seek(headerFileEndOffset);
                    while (copy.getFilePointer() + FitsFile.BLOCK_LENGTH <= copy.length())
                    {
                        copy.readFully(copyBytes);
                        headerFile.write(copyBytes);
                    }
                    headerFile.setLength(headerFile.getFilePointer());
                }
                headerFileBeginOffset = beginHDUPosition;
                headerFileEndOffset = endHDUPosition;
            }
            catch (IOException exc)
            {
                return false;
            }
            return true;
        }
        return false;
    }
}
