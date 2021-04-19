# OpenXLIFF Filters

<img src="https://maxprograms.com/images/openxliff_trim.png" style="float:left; padding: 0 10px 0 10px ">  

An open source set of Java filters for creating, merging and validating XLIFF 1.2 and 2.0 files.

With OpenXLIFF Filters you can create XLIFF files that don't use proprietary markup and are compatible with most CAT (Computer Asisted Translation) tools.

**XLIFFChecker**, an open source XLIFF validation tool, is now part of OpenXLIFF Filters. Its code has been ported to Java 11 and enhanced with support for XLIFF 2.0.

- **[Releases](https://github.com/rmraya/OpenXLIFF#releases)**
- **[Related Projects](https://github.com/rmraya/OpenXLIFF#related-projects)**
- **[Downloads](https://github.com/rmraya/OpenXLIFF#downloads)**
- **[Supported File Formats](https://github.com/rmraya/OpenXLIFF#supported-file-formats)**
- **[Converting Documents to XLIFF](https://github.com/rmraya/OpenXLIFF#converting-documents-to-xliff)**  
- **[Converting XLIFF to Original Format](https://github.com/rmraya/OpenXLIFF#converting-xliff-to-original-format)**
- **[XLIFF Validation](https://github.com/rmraya/OpenXLIFF#xliff-validation)**
- **[XLIFF Validation Service](https://github.com/rmraya/OpenXLIFF#xliff-validation-service)**
- **[Translation Status Analysis](https://github.com/rmraya/OpenXLIFF#translation-status-analysis)**
- **[Joining multiple XLIFF files](https://github.com/rmraya/OpenXLIFF#joining-multiple-xliff-files)**

## Related Projects

- **[XLIFF Manager](https://github.com/rmraya/XLIFFManager)** implements an easy to use free UI for creating, merging and validating XLIFF files in a graphical environment.
- **[Swordfish IV](https://github.com/rmraya/Swordfish)** uses OpenXLIFF Filters to extract translatable text from supported formats and manage XML documents.
- **[RemoteTM](https://github.com/rmraya/RemoteTM)** uses OpenXLIFF Filters to handle all TMX processing.
- **[Stingray](https://github.com/rmraya/Stingray)** uses OpenXLIFF Filters for extracting the text to align from supported monolingual documents.
- **[TMXEditor](https://github.com/rmraya/TMXEditor)** relies on OpenXLIFF Filters XML support for processing TMX files.
- **[XLIFF Validation](https://github.com/rmraya/XLIFFValidation)** web-based XLIFF Validation Service.

## Releases

Version | Comment | Release Date
--------|---------|-------------
1.12.5 | Improved support for bilingual files | February 3rd, 2021
1.12.4 | Allowed concurrent access for [XLIFF Validation](https://github.com/rmraya/XLIFFValidation) | January 22nd, 2021
1.12.3 | Improved support for Trados Studio packages | January 1st, 2021
1.12.2 | Suppressed output for confirmed empty targets | December 7th, 2020
1.12.1 | Improved conversion of XLIFF 1.2 files | December 3rd, 2020
1.12.0 | Added support for Adobe InCopy ICML and SRT subtitles | November 23rd, 2020
1.11.1 | Fixed JSON encoding; fixed import of XLIFF matches | November 1st, 2020
1.11.0 | Added support for JSON files | September 25, 2020
1.10.1 | Fixed handling of TXLF files and improved XML catalog | September 5th, 2020
1.10.0 | Added conversion of 3rd party XLIFF; improved support for XLIFF 2.0; fixed issues with Trados Studio packages | August 25th. 2020
1.9.1 | Added improvements required by [Swordfish IV](https://github.com/rmraya/Swordfish). | August 13th, 2020
1.9.0 | Added 5 Machine Translation (MT) engines (Google, Microsoft Azure, DeepL, MyMemory & Yandex) | May 18th, 2020
1.8.4 | Improved catalog and other minor fixes | April 25th. 2020
1.8.3 | Fixed conversion of PO files | April 17th, 2020
1.8.2 | Switched to synchronized classes in XML package | April 10th, 2020
1.8.1 | Improved support for Trados Studio packages | April 3rd, 2020
1.8.0 | Implemented support for Trados Studio packages | March 29th, 2020
1.7.0 | Major code cleanup; Changed segmentation model for XLIFF 2.0 | January 1st, 2020
1.6.0 | Added support for XLIFF files from WPML WordPress Plugin | December 1st, 2019
1.5.2 | Improved segmenter performance | October 29th, 2019
1.5.1 | Fixed catalog on Windows | September 22nd, 2019
1.5.0 | Improved support for empty &lt;xref/> elements in DITA; improved support for XML catalogs | September 5th, 2019
1.4.2 | Added option to join XLIFF files; Fixed merge errors in XLIFF 2.0; added tool info to XLIFF files; cleaned DITA attributes on merging | August 14th, 2019
1.4.1 | Improved performance embedding skeleton; added Apache Ant building option; renamed module to 'openxliff' | July 25th, 2019
1.4.0 | Improved report of task results | July 17th, 2019
1.3.3 | Fixed merging of MS Office files from XLIFF 2.0 | July 5th, 2019
1.3.2 | Updated for Swordfish 3.4.3 | June 30th, 2019
1.3.1 | Updated for Swordfish 3.4-0 | April 30th, 2019
1.3.0 | Added option to export approved XLIFF segments as TMX | April 18th, 2019
1.2.1 | Improved validation of XLIFF 2.0 | April 6th, 2019
1.2.0 | Added Translation Status Analysis | March 3rd, 2019
1.1.0 | Incorporated XLIFFChecker code| November 20th, 2018
1.0.0 | Initial Release | November 12th, 2018

## Downloads

Ready to use binaries for Windows, Linux and macOS are available for download at [https://www.maxprograms.com/products/openxliff.html](https://www.maxprograms.com/products/openxliff.html)  

If you prefer to use a graphical interface instead of the command line, get installers for **XLIFF Manager** at [https://www.maxprograms.com/products/xliffmanager.html](https://www.maxprograms.com/products/xliffmanager.html)

## Supported File Formats

OpenXLIFF Filters can generate XLIFF 1.2 and XLIFF 2.0 from these formats:

- **General Documentation**
  - Adobe InDesign Interchange (INX)
  - Adobe InDesign IDML CS4, CS5, CS6 & CC
  - HTML
  - Microsoft Office (2007 and newer)
  - Microsoft Visio XML Drawings (2007 and newer)
  - MIF (Maker Interchange Format)
  - OpenOffice / LibreOffice / StarOffice
  - Plain Text
  - SDLXLIFF (Trados Studio)
  - SRT Subtitles
  - Trados Studio Packages (*.sdlppx)
  - TXML (GlobalLink/Wordfast PRO)
  - WPML XLIFF (WordPress Multilingual Plugin)
- **XML Formats**
  - XML (Generic)
  - DITA 1.0, 1.1, 1.2 and 1.3
  - DocBook 3.x, 4.x and 5.x
  - SVG
  - Word 2003 ML
  - XHTML
- **Software Development**
  - JavaScript
  - JSON
  - Java Properties
  - PO (Portable Objects)
  - RC (Windows C/C++ Resources)
  - ResX (Windows .NET Resources)
  - TS (Qt Linguist translation source)

## Requirements

- JDK 11 or newer is required for compiling and building. Pre-built binaries already include everything you need to run all options.
- Apache Ant 1.10.6 or newer

## Building

- Checkout this repository.
- Point your JAVA_HOME variable to JDK 11
- Run `ant` to generate a binary distribution in `./dist`

## Converting Documents to XLIFF

You can use the library in your own Java code. Conversion to XLIFF is handled by the class `com.maxprograms.converters.Convert`.

If you use binaries from the command line, running `.\convert.bat` or `./convert.sh` without parameters displays help for XLIFF generation.

```text
Usage:

   convert.bat [-help] [-version] -file sourceFile -srcLang sourceLang [-tgtLang targetLang]
               [-skl skeletonFile] [-xliff xliffFile] [-type fileType] [-enc encoding]
               [-srx srxFile] [-catalog catalogFile] [-divatal ditaval] [-embed] [-paragraph]
               [-2.0] [-charsets]

Where:

   -help:      (optional) Display this help information and exit
   -version:   (optional) Display version & build information and exit
   -file:      source file to convert
   -srgLang:   source language code
   -tgtLang:   (optional) target language code
   -xliff:     (optional) XLIFF file to generate
   -skl:       (optional) skeleton file to generate
   -type:      (optional) document type
   -enc:       (optional) character set code for the source file
   -srx:       (optional) SRX file to use for segmentation
   -catalog:   (optional) XML catalog to use for processing
   -ditaval:   (optional) conditional processing file to use when converting DITA maps
   -embed:     (optional) store skeleton inside the XLIFF file
   -paragraph: (optional) use paragraph segmentation
   -2.0:       (optional) generate XLIFF 2.0
   -charsets:  (optional) display a list of available character sets and exit

Document Types

   INX = Adobe InDesign Interchange
   ICML = Adobe InCopy ICML
   IDML = Adobe InDesign IDML
   DITA = DITA Map
   HTML = HTML Page
   JS = JavaScript
   JAVA = Java Properties
   JSON = JSON
   MIF = MIF (Maker Interchange Format)
   OFF = Microsoft Office 2007 Document
   OO = OpenOffice Document
   PO = PO (Portable Objects)
   RC = RC (Windows C/C++ Resources)
   RESX = ResX (Windows .NET Resources)
   SDLPPX = Trados Studio Package
   SDLXLIFF = SDLXLIFF Document
   SRT = SRT Subtitles
   TEXT = Plain Text
   TS = TS (Qt Linguist translation source)
   TXML = TXML Document
   WPML = WPML XLIFF
   XLIFF = XLIFF Document
   XML = XML Document
   XMLG = XML (Generic)
```

Only two parameters are absolutely required: `-file` and `-srcLang`. The library tries to automatically detect format and encoding and exits with an error message if it can't guess them. If automatic detection doesn't work, add `-type` and `-enc` parameters.

Character sets vary with the operating system. Run the conversion script with `-charsets` to get a list of character sets available in your OS.

By default, XLIFF and skeleton are generated in the folder where the source document is located. Extensions used for XLIFF and Skeleton are `.xlf` and `.skl`.

The `XML` type handles multiple document formats, like `XHTML`, `SVG` or `DocBook` files.

Default XML catalog and SRX file are provided. You can also use custom versions if required.

## Converting XLIFF to Original Format

You can convert XLIFF files created with OpenXLIFF Filters to original format using class `com.maxprograms.converters.Merge` in your Java code.

If you use binaries from the command line, running `.\merge.bat` or `./merge.sh` without parameters will display the information you need to merge an XLIFF file.

```text
Usage:

   merge.bat [-help] [-version] -xliff xliffFile -target targetFile [-catalog catalogFile] [-unapproved] [-export]

Where:

   -help:       (optional) Display this help information and exit
   -version:    (optional) Display version & build information and exit
   -xliff:      XLIFF file to merge
   -target:     translated file or folder where to store translated files
   -catalog:    (optional) XML catalog to use for processing
   -unapproved: (optional) accept translations from unapproved segments
   -export:     (optional) generate TMX file from approved segments
```

## XLIFF Validation

The original [XLIFFChecker code](http://sourceforge.net/p/xliffchecker/code/) supports XLIFF 1.0, 1.1 and 1.2. The new version incorporated in OpenXLIFF Filters also supports XLIFF 2.0.

Standard XML Schema validation does not detect the use of duplicated 'id' attributes, wrong language codes and other constraints written in the different XLIFF specifications.

All XLIFF 2.0 modules are validated using XML Schema validation in a first pass. Extra validation is then performed using Java code for XLIFF 2.0 Core and for Metadata, Matches and Glossary modules.

You can validate XLIFF files using your own Java code. Validation of XLIFF files is handled by the class `com.maxprograms.validation.XliffChecker`.

If you use binaries from the command line, running `.\xliffchecker.bat` or `./xliffchecker.sh` without parameters displays help for XLIFF validation.

```text
Usage:

   xliffchecker.bat [-help] -file xliffFile [-catalog catalogFile]

Where:

   -help:      (optional) Display this help information and exit
   -file:      XLIFF file to validate
   -catalog:   (optional) XML catalog to use for processing
```

## XLIFF Validation Service

You can validate your XLIFF files online at [https://dev.maxprograms.com/Validation/](https://dev.maxprograms.com/Validation/)

## Translation Status Analysis

This library lets you produce an HTML file with word counts and segment status statistics from an XLIFF file.  

If you use binaries from the command line, running `.\analysis.bat` or `./analysis.sh` without parameters displays help for statistics generation.

You can generate statistics using your own Java code. Statistics generation is handled by the class `com.maxprograms.stats.RepetitionAnalysis`.

```text
Usage:

   analysis.sh [-help] -file xliffFile [-catalog catalogFile]

Where:

   -help:      (optional) Display this help information and exit
   -file:      XLIFF file to analyze
   -catalog:   (optional) XML catalog to use for processing
```

The HTML report is generated in the folder where the XLIFF file is located and its name is the name of the XLIFF plus `.log.html`.

## Joining multiple XLIFF files

You can combine several XLIFF files into a larger one using the class `com.maxprograms.converters.Join` from your Java code or using the provided scripts.

Running `.\join.bat` or `./join.sh` without parameters displays help for joining files.

```text
Usage:

   join.sh [-help] -target targetFile -files file1,file2,file3...

 Where:

   -help:     (optional) Display this help information and exit
   -target:   combined output XLIFF file
```

The merge process automatically splits the files when converting back to original format.
