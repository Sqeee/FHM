package cz.muni.sci.astro.fits;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents FITS file structure
 *
 * @author Jan Hlava, 395986
 */
public class FitsFile {
    public static final int KEYWORD_LENGTH = 8;
    public static final int CARD_LENGTH = 80;
    public static final int BLOCK_LENGTH = 2880;
    public static final int CARD_BLOCK_ENTRIES = BLOCK_LENGTH / CARD_LENGTH;
    public static final String BLANK_CARD_ENTRY = "                                                                                ";
    public static final String END_CARD_ENTRY = "END                                                                             ";
    public static final String CONTINUE_CARD_PREFIX = "CONTINUE  ";

    private File file;
    private List<FitsHeaderDataUnit> HDUs;
    private RandomAccessFile raf;

    /**
     * Creates FITS file from given file
     *
     * @param file FITS file
     * @throws FitsException            if file has invalid format
     * @throws NullPointerException     if file is null
     * @throws IllegalArgumentException if file is not found
     */
    public FitsFile(File file) throws FitsException {
        if (file == null) {
            throw new NullPointerException("file is null");
        } else if (!file.exists()) {
            throw new FitsFileException("file does not exists");
        } else if (!isFitsFile(file)) {
            throw new FitsFileException("file is not FITS file");
        }
        this.file = file;
        HDUs = new ArrayList<>(1);
        try {
            raf = new RandomAccessFile(file, "rw");
            while (true) {
                HDUs.add(new FitsHeaderDataUnit(raf));
            }
        } catch (FileNotFoundException exc) {
            throw new IllegalArgumentException("File not found", exc);
        } catch (FitsException exc) {
            if (HDUs.isEmpty()) {
                throw new FitsFileException("No valid FITS file: " + exc.getMessage(), exc);
            }
        }
    }

    /**
     * Tests given file if is FITS file (checks existence of SIMPLE keyword and blank or END keyword at the end of header)
     *
     * @param file file to test
     * @return true if file is FITS file, otherwise false (test is simple, true does not mean that file is valid FITS file)
     */
    public static boolean isFitsFile(File file) {
        byte[] card = new byte[CARD_LENGTH];
        int read_bytes;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            read_bytes = raf.read(card);
            String strCard = new String(card);
            if (read_bytes != CARD_LENGTH || !strCard.startsWith("SIMPLE  = ")) {
                return false;
            }
            long headers = 1;
            while (read_bytes != -1) {
                raf.seek(headers * BLOCK_LENGTH - CARD_LENGTH);
                read_bytes = raf.read(card);
                strCard = new String(card);
                if (read_bytes != CARD_LENGTH) {
                    raf.close();
                    return false;
                } else if (strCard.equals(BLANK_CARD_ENTRY) || strCard.equals(END_CARD_ENTRY)) {
                    raf.close();
                    return true;
                }
                headers++;
            }
            raf.close();
            return false;
        } catch (IOException exc) {
            return false;
        }
    }

    /**
     * Returns count of HDUs in this FITS file
     *
     * @return count of HDUs in this FITS file
     */
    public int getCountHDUs() {
        return HDUs.size();
    }

    /**
     * Returns filename of this FITS file
     *
     * @return filename of this FITS file, if file is closed, returns empty String
     */
    public String getFilename() {
        return file != null ? file.getName() : "";
    }

    /**
     * Returns HDU with given index
     *
     * @param index index of HDU to return
     * @return HDU with given index
     * @throws IndexOutOfBoundsException if index is out of range (index < 0 || index >= getCountHDUs())
     */
    public FitsHeaderDataUnit getHDU(int index) {
        if (HDUs.isEmpty()) {
            return null;
        } else if (index < 0 || index >= HDUs.size()) {
            throw new IndexOutOfBoundsException("Index is out of range");
        }
        return HDUs.get(index);
    }

    /**
     * Returns cards from FITS file
     *
     * @return cards from FITS file
     */
    public List<FitsCard> getCards() {
        List<FitsCard> result = new ArrayList<>();
        for (FitsHeaderDataUnit hdu : HDUs) {
            result.addAll(hdu.getHeader().getCards());
        }
        return result;
    }

    /**
     * Returns cards with given keyword from FITS file
     *
     * @param keyword keyword, which should cards contains
     * @return cards with given keyword from FITS file
     */
    public List<FitsCard> getCardsWithKeyword(String keyword) {
        List<FitsCard> result = new ArrayList<>();
        for (FitsHeaderDataUnit hdu : HDUs) {
            result.addAll(hdu.getHeader().getCardsWithKeyword(keyword));
        }
        return result;
    }

    /**
     * Closes FITS file
     */
    public void closeFile() {
        try {
            raf.close();
        } catch (IOException ignored) {
        } finally {
            file = null;
            raf = null;
            HDUs.clear();
        }
    }

    /**
     * Try to save FITS file (first, you need setup new cards in HDUs). It creates backup in case of failure (return value is false)
     *
     * @return true if saving was successful, false if saving failed
     */
    public boolean saveFile() {
        try {
            Path backupPath;
            if (file.getParent() != null) {
                backupPath = Paths.get(file.getParent(), file.getName() + ".old");
            } else {
                backupPath = Paths.get(file.getName() + ".old");
            }
            Files.copy(file.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES).toFile();
            RandomAccessFile backup = new RandomAccessFile(backupPath.toFile(), "r");
            long beginHDUPosition = 0;
            boolean changedEndHDUPosition = false;
            long EndHDUPositionBeforeSaving;
            for (FitsHeaderDataUnit hdu : HDUs) {
                EndHDUPositionBeforeSaving = hdu.getHDUEndPosition();
                if (!hdu.saveHDU(backup, beginHDUPosition, changedEndHDUPosition)) {
                    backup.close();
                    return false;
                }
                beginHDUPosition = hdu.getHDUEndPosition();
                changedEndHDUPosition = beginHDUPosition != EndHDUPositionBeforeSaving;
            }
            backup.close();
            Files.delete(backupPath);
            return true;
        } catch (IOException exc) {
            return false;
        }
    }

    /**
     * Returns problems blocking to save file
     *
     * @return problems blocking to save file
     */
    public List<String> checkReadyToSave() {
        List<String> problems = new ArrayList<>();
        for (FitsHeaderDataUnit hdu : HDUs) {
            problems.addAll(hdu.getHeader().checkSaveHeader());
        }
        return problems;
    }

    /**
     * Checks if given object is equal to this object
     *
     * @param object object for equality comparison
     * @return true if object and this are equaled, otherwise false
     */
    @Override
    public boolean equals(Object object) {
        return object instanceof FitsFile && getFilename().equals(object);
    }

    /**
     * Returns hash code of this fits file
     *
     * @return hash code of this fits file
     */
    @Override
    public int hashCode() {
        return getFilename().hashCode();
    }
}
