package cz.muni.sci.astro.fits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Enum definitions of known Keywords
 *
 * @author Jan Hlava, 395986
 */
public enum FitsKeyword {
    SIMPLE("SIMPLE"),
    BITPIX("BITPIX"),
    NAXIS("NAXIS"),
    NAXISn("NAXISn"),
    EXTEND("EXTEND"),
    BSCALE("BSCALE"),
    BZERO("BZERO"),
    BUNIT("BUNIT"),
    PEDESTAL("PEDESTAL"),
    DATAMIN("DATAMIN"),
    DATAMAX("DATAMAX"),
    BLANK("BLANK"),
    SITENAME("SITENAME"),
    SITEALT("SITEALT"),
    SITELAT("SITELAT"),
    SITELONG("SITELONG"),
    TIMEZONE("TIMEZONE"),
    TELESCOP("TELESCOP"),
    TELFOCUS("TELFOCUS"),
    TELDIAM("TELDIAM"),
    TELFRAT("TELFRAT"),
    INSTRUME("INSTRUME"),
    CCDABG("CCDABG"),
    CCDFWC("CCDFWC"),
    CCDEGAIN("CDDEGAIN"),
    CCDRNOIS("CCDRNOIS"),
    CCDRTIME("CCDRTIME"),
    CCDSIZEn("CCDSIZEn"),
    PIXSIZEn("PIXSIZEn"),
    PIXSCALn("PIXSCALn"),
    BIASSECn("BIASSECn"),
    PROGRAM("PROGRAM"),
    ORIGIN("ORIGIN"),
    OWNER("OWNER"),
    OBSERVER("OBSERVER"),
    DATASEC("DATASEC"),
    BINNINGn("BINNINGn"),
    TEMP_SET("TEMP-SET"),
    TEMP_CCD("TEMP-CCD"),
    TEMP_OUT("TEMP-OUT"),
    TIMESYS("TIMESYS"),
    DATE_OBS("DATE-OBS"),
    IMAGETYP("IMAGETYP"),
    EXPTIME("EXPTIME"),
    FILTER("FILTER"),
    FLIPSTAT("FLIPSTAT"),
    MOUNTDEC("MOUNTDEC"),
    MOUNTRA("MOUNTRA"),
    NOTES("NOTES"),
    OBJECT("OBJECT"),
    CATNAME("CATNAME"),
    WDEC("WDEC"),
    WRA("WRA"),
    WEPOCH("WEPOCH"),
    CRPIXn("CRPIXn"),
    CRVALn("CRVALn"),
    DEC("DEC"),
    RA("RA"),
    EPOCH("EPOCH"),
    EQUINOX("EQUINOX"),
    RADECSYS("RADECSYS"),
    CTYPEn("CTYPEn"),
    CDELTn("CDELTn"),
    CROTAn("CROTAn"),
    CD1_1("CD1_1"),
    CD1_2("CD1_2"),
    CD2_1("CD2_1"),
    CD2_2("CD2_2"),
    SECPIX("SECPIX"),
    WCSSEP("WCSSEP"),
    WCSRFCAT("WCSRFCAT"),
    WCSIMCAT("WCSIMCAT"),
    WCSMATCH("WCSMATCH"),
    WCSNREF("WCSNREF"),
    WCSTOL("WCSTOL"),
    IMWCS("IMWCS"),
    COMMENT("COMMENT"),
    HISTORY("HISTORY"),
    CONTINUE("CONTINUE"),
    EMPTY(""),
    END("END"),
    CUSTOM("CUSTOM");

    private final String name;

    /**
     * Creates new instance of this enum with given name
     *
     * @param name name of keyword
     */
    FitsKeyword(String name) {
        this.name = name;
    }

    /**
     * Returns collection of mandatory keywords for given image type
     *
     * @param imageType image type for which we need collection of mandatory keywords
     * @return collection of mandatory keywords for given image type
     */
    public static List<FitsKeyword> getImageTypeMandatory(FitsImageType imageType) {
        List<FitsKeyword> keywords = new ArrayList<>();
        switch (imageType) {
            case UNKNOWN:
                Collections.addAll(keywords, SIMPLE, BITPIX, NAXIS, END); // IMAGETYP was removed because it can prevent from saving file where imagetyp is missing and no change with this keyword was made, it can be little confusing
                break;
            case LIGHT_FRAME:
                Collections.addAll(keywords, SIMPLE, BITPIX, NAXIS, EXTEND, BSCALE, BZERO, BUNIT, SITENAME, SITEALT, SITELAT, SITELONG, TIMEZONE, TELESCOP, TELFOCUS, TELDIAM, TELFRAT, INSTRUME, CCDABG, CCDEGAIN, CCDRNOIS, CCDRTIME, OWNER, TIMESYS, DATE_OBS, IMAGETYP, EXPTIME, FILTER, OBJECT, CATNAME, WDEC, WRA, WEPOCH, DEC, RA, EPOCH, EQUINOX, RADECSYS, CD1_1, CD1_2, CD2_1, CD2_2, SECPIX, WCSSEP, WCSRFCAT, WCSIMCAT, WCSMATCH, WCSNREF, WCSTOL, IMWCS);
                break;
            case FLAT_FIELD:
                Collections.addAll(keywords, SIMPLE, BITPIX, NAXIS, EXTEND, BSCALE, BZERO, BUNIT, SITENAME, SITEALT, SITELAT, SITELONG, TIMEZONE, TELESCOP, TELFOCUS, TELDIAM, TELFRAT, INSTRUME, CCDABG, CCDEGAIN, CCDRNOIS, CCDRTIME, OWNER, TIMESYS, DATE_OBS, IMAGETYP, EXPTIME, FILTER);
                break;
            case DARK_FRAME:
                Collections.addAll(keywords, SIMPLE, BITPIX, NAXIS, EXTEND, BSCALE, BZERO, BUNIT, SITENAME, SITEALT, SITELAT, SITELONG, TIMEZONE, TELESCOP, TELFOCUS, TELDIAM, TELFRAT, INSTRUME, CCDABG, CCDEGAIN, CCDRNOIS, CCDRTIME, OWNER, TIMESYS, DATE_OBS, IMAGETYP, EXPTIME);
                break;
            case BIAS_FRAME:
                Collections.addAll(keywords, SIMPLE, BITPIX, NAXIS, EXTEND, BSCALE, BZERO, BUNIT, SITENAME, SITEALT, SITELAT, SITELONG, TIMEZONE, TELESCOP, TELFOCUS, TELDIAM, TELFRAT, INSTRUME, CCDABG, CCDEGAIN, CCDRNOIS, CCDRTIME, OWNER, TIMESYS, DATE_OBS, IMAGETYP, EXPTIME);
                break;
            case OBJECT:
                Collections.addAll(keywords, SIMPLE, BITPIX, NAXIS, END);
                break;
        }
        return keywords;
    }

    /**
     * Returns collection of mandatory n type keywords for given image type
     *
     * @param imageType image type for which we need collection of mandatory n type keywords
     * @return collection of mandatory n type keywords for given image type
     */
    public static List<FitsKeyword> getImageTypeNMandatory(FitsImageType imageType) {
        List<FitsKeyword> keywords = new ArrayList<>();
        switch (imageType) {
            case UNKNOWN:
                keywords.add(NAXISn);
                break;
            case OBJECT:
                keywords.add(NAXISn);
                break;
            case LIGHT_FRAME:
                Collections.addAll(keywords, NAXISn, CCDSIZEn, PIXSIZEn, PIXSCALn, BINNINGn, CRPIXn, CRVALn, CTYPEn, CDELTn, CROTAn);
                break;
            case FLAT_FIELD:
                Collections.addAll(keywords, NAXISn, CCDSIZEn, PIXSIZEn, PIXSCALn, BINNINGn);
                break;
            case DARK_FRAME:
                Collections.addAll(keywords, NAXISn, CCDSIZEn, PIXSIZEn, BINNINGn);
                break;
            case BIAS_FRAME:
                Collections.addAll(keywords, NAXISn, CCDSIZEn, PIXSIZEn, BINNINGn);
                break;
        }
        return keywords;
    }

    /**
     * Returns string representation of enum
     *
     * @return string representation of enum
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns data type of this keyword
     *
     * @return data type of this keyword
     */
    public FitsKeywordsDataType getType() {
        switch (this) {
            case SIMPLE:
            case EXTEND:
            case CCDABG:
                return FitsKeywordsDataType.LOGICAL;
            case BITPIX:
            case NAXIS:
            case NAXISn:
            case PEDESTAL:
            case BLANK:
                return FitsKeywordsDataType.INT;
            case BSCALE:
            case BZERO:
            case DATAMIN:
            case DATAMAX:
            case SITEALT:
            case SITELAT:
            case SITELONG:
            case TIMEZONE:
            case TELFOCUS:
            case TELDIAM:
            case CCDFWC:
            case CCDEGAIN:
            case CCDRNOIS:
            case CCDRTIME:
            case CCDSIZEn:
            case PIXSIZEn:
            case PIXSCALn:
            case BINNINGn:
            case TEMP_SET:
            case TEMP_CCD:
            case TEMP_OUT:
            case EXPTIME:
            case MOUNTDEC:
            case MOUNTRA:
            case WEPOCH:
            case CRPIXn:
            case CRVALn:
            case EPOCH:
            case EQUINOX:
            case CDELTn:
            case CROTAn:
            case CD1_1:
            case CD1_2:
            case CD2_1:
            case CD2_2:
            case SECPIX:
            case WCSSEP:
            case WCSMATCH:
            case WCSNREF:
            case WCSTOL:
                return FitsKeywordsDataType.REAL;
            case BUNIT:
            case SITENAME:
            case TELESCOP:
            case TELFRAT:
            case INSTRUME:
            case BIASSECn:
            case PROGRAM:
            case ORIGIN:
            case OWNER:
            case OBSERVER:
            case DATASEC:
            case TIMESYS:
            case DATE_OBS:
            case IMAGETYP:
            case FILTER:
            case FLIPSTAT:
            case NOTES:
            case OBJECT:
            case CATNAME:
            case WDEC:
            case WRA:
            case DEC:
            case RA:
            case RADECSYS:
            case CTYPEn:
            case WCSRFCAT:
            case WCSIMCAT:
            case IMWCS:
                return FitsKeywordsDataType.LITERAL;
            case CUSTOM:
                return FitsKeywordsDataType.CUSTOM;
            case COMMENT:
            case HISTORY:
            case CONTINUE:
            case EMPTY:
            case END:
            default:
                return FitsKeywordsDataType.NONE;
        }
    }

    /**
     * Checks if this keyword is n type
     *
     * @return true if keyword is n type, otherwise false
     */
    public boolean isNKeyword() {
        switch (this) {
            case NAXISn:
            case CCDSIZEn:
            case PIXSIZEn:
            case PIXSCALn:
            case BIASSECn:
            case BINNINGn:
            case CRPIXn:
            case CRVALn:
            case CTYPEn:
            case CDELTn:
            case CROTAn:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns if must have n param as keyword NAXIS
     *
     * @return true if must have n param as keyword NAXIS, otherwise false
     */
    public boolean isNAsNAXIS() {
        switch (this) {
            case NAXISn:
            case CCDSIZEn:
            case PIXSIZEn:
            case PIXSCALn:
            case BINNINGn:
            case CRPIXn:
            case CRVALn:
            case CTYPEn:
            case CDELTn:
            case CROTAn:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true if this keyword should be unique in header
     *
     * @return true if this keyword should be unique in header, otherwise no
     */
    public boolean isUniqueHeaderKeyword() {
        switch (this) {
            case EMPTY:
            case COMMENT:
            case HISTORY:
            case CONTINUE:
                return false;
            default:
                return true;
        }
    }

    /**
     * Returns true if this keyword has physical unit
     *
     * @return true if this keyword has physical unit, otherwise false
     */
    public boolean isUnitKeyword() {
        switch (this) {
            case PEDESTAL:
            case DATAMIN:
            case DATAMAX:
            case SITEALT:
            case SITELAT:
            case SITELONG:
            case TIMEZONE:
            case TELDIAM:
            case TELFOCUS:
            case CCDFWC:
            case CCDEGAIN:
            case CCDRNOIS:
            case CCDRTIME:
            case PIXSIZEn:
            case PIXSCALn:
            case TEMP_SET:
            case TEMP_CCD:
            case TEMP_OUT:
            case EXPTIME:
            case MOUNTDEC:
            case MOUNTRA:
            case CRPIXn:
            case CRVALn:
            case CDELTn:
            case CROTAn:
            case SECPIX:
            case WCSSEP:
            case WCSTOL:
                return true;
            default:
                return false;
        }
    }
}
