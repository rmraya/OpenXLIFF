![alt text](https://maxprograms.com/images/openxliff_s.png "Open XLIFF Filters")

## Open XLIFF Filters

An open source set of Java filters for creating, merging and validating XLIFF 1.2 and 2.0 files.

Source code of Open XLIFF Filters is available under [Eclipse Public License 1.0](https://www.eclipse.org/org/documents/epl-v10.html).

With Open XLIFF Filters you can create XLIFF files that don't use proprietary markup.

**XLIFFChecker**, an open source XLIFF validation tool, is now part of Open XLIFF Filters. Its code has been ported to Java 11 and enhanced with support for XLIFF 2.0.
 - **[Releases](https://github.com/rmraya/OpenXLIFF#releases)**
 - **[Downloads](https://github.com/rmraya/OpenXLIFF#downloads)**
 - **[Supported File Formats](https://github.com/rmraya/OpenXLIFF#supported-file-formats)**
 - **[Converting Documents to XLIFF](https://github.com/rmraya/OpenXLIFF#converting-documents-to-xliff)**  
 - **[Converting XLIFF to Original Format](https://github.com/rmraya/OpenXLIFF#converting-xliff-to-original-format)**
 - **[XLIFF Validation](https://github.com/rmraya/OpenXLIFF#xliff-validation)** 
 - **[Translation Status Analysis](https://github.com/rmraya/OpenXLIFF#translation-status-analysis)**

Project 
**[XLIFF Manager](https://github.com/rmraya/XLIFFManager)** implements an easy to use UI for creating, merging and validating XLIFF files in a graphical environment. 

### Releases

Version | Comment | Release Date
--------|---------|-------------
1.3.1 | Updated for Swordfish 3.4-0 | April 30, 2019
1.3.0 | Added option to export approved XLIFF segments as TMX | April 18, 2019
1.2.1 | Improved validation of XLIFF 2.0 | April 6, 2019
1.2.0 | Added Translation Status Analysis | March 3, 2019
1.1.0 | Incorporated XLIFFChecker code| November 20, 2018
1.0.0 | Initial Release | November 12, 2018

### Downloads

Ready to use binaries for Windows, Linux and macOS are available for download at https://www.maxprograms.com/products/openxliff.html  

If you prefer to use a graphical interface instead of the command line, get installers for **XLIFF Manager** at https://www.maxprograms.com/products/xliffmanager.html

### Supported File Formats

Open XLIFF Filters can generate XLIFF 1.2 and XLIFF 2.0 from these formats:

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
  - TXML (GlobalLink/Wordfast PRO)   
- **XML Formats**
  - XML (Generic)
  - DITA 1.0, 1.1, 1.2 and 1.3
  - DocBook 3.x, 4.x and 5.x
  - SVG
  - Word 2003 ML
  - XHTML 
- **Software Development**
  - JavaScript
  - Java Properties
  - PO (Portable Objects)
  - RC (Windows C/C++ Resources)
  - ResX (Windows .NET Resources)
  - TS (Qt Linguist translation source)

### Requirements

- JDK 11 or newer is required for compiling and building. Pre-built binaries already include everything you need to run all options.

### Building

- Checkout this repository.
- Point your JAVA_HOME variable to JDK 11
- Use `buid.bat` or `build.sh` to generate a binary distribution in `./open_filters`

### Converting Documents to XLIFF

You can use the library in your own Java code. Conversion to XLIFF is handled by the class `com.maxprograms.converters.Convert`.

If you use binaries from the command line, running `.\convert.bat` or `./convert.sh` without parameters displays help for XLIFF generation. 


```
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
   IDML = Adobe InDesign IDML
   DITA = DITA Map
   HTML = HTML Page
   JS = JavaScript
   JAVA = Java Properties
   MIF = MIF (Maker Interchange Format)
   OFF = Microsoft Office 2007 Document
   OO = OpenOffice Document
   PO = PO (Portable Objects)
   RC = RC (Windows C/C++ Resources)
   RESX = ResX (Windows .NET Resources)
   SDLXLIFF = SDLXLIFF Document
   TEXT = Plain Text
   TS = TS (Qt Linguist translation source)
   TXML = TXML Document
   XML = XML Document
   XMLG = XML (Generic)
```
Only two parameters are absolutely required: `-file` and `-srcLang`. The library tries to automatically detect format and encoding and exits with an error message if it can't guess them. If automatic detection doesn't work, add `-type` and `-enc` parameters.

Character sets vary with the operating system. Run the conversion script with `-charsets` to get a list of character sets available in your OS.

By default, XLIFF and skeleton are generated in the folder where the source document is located. Extensions used for XLIFF and Skeleton are `.xlf` and `.skl`.

The `XML` type handles multiple document formats, like `XHTML`, `SVG` or `DocBook` files.

Default XML catalog and SRX file are provided. You can also use custom versions if required.

### Converting XLIFF to Original Format

You can convert XLIFF files created with Open XLIFF Filters to original format using class `com.maxprograms.converters.Merge` in your Java code.

If you use binaries from the command line, running `.\merge.bat` or `./merge.sh` without parameters will display the information you need to merge an XLIFF file.

```
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
### XLIFF Validation

The original [XLIFFChecker code](http://sourceforge.net/p/xliffchecker/code/) supports XLIFF 1.0, 1.1 and 1.2. The new version incorporated in Open XLIFF Filters also supports XLIFF 2.0.

Standard XML Schema validation does not detect the use of duplicated 'id' attributes, wrong language codes and other constraints written in the different XLIFF specifications.

All XLIFF 2.0 modules are validated using XML Schema validation in a first pass. Extra validation is then performed using Java code for XLIFF 2.0 Core and for Metadata, Matches and Glossary modules.

You can validate XLIFF files using your own Java code. Validation of XLIFF files is handled by the class `com.maxprograms.validation.XliffChecker`.

If you use binaries from the command line, running `.\xliffchecker.bat` or `./xliffchecker.sh` without parameters displays help for XLIFF validation. 

```
Usage:

   xliffchecker.bat [-help] -file xliffFile [-catalog catalogFile]

Where:

   -help:      (optional) Display this help information and exit
   -file:      XLIFF file to validate
   -catalog:   (optional) XML catalog to use for processing
```   

### Translation Status Analysis

This library lets you produce an HTML file with word counts and segment status statistics from an XLIFF file.  

If you use binaries from the command line, running `.\analysis.bat` or `./analysis.sh` without parameters displays help for statistics generation.

You can generate statistics using your own Java code. Statistics generation is handled by the class `com.maxprograms.stats.RepetitionAnalysis`.

```
Usage:

   analysis.sh [-help] -file xliffFile [-catalog catalogFile] 

Where:

   -help:      (optional) Display this help information and exit
   -file:      XLIFF file to analyze
   -catalog:   (optional) XML catalog to use for processing
```

The HTML report is generated in the folder where the XLIFF file is located and its name is the name of the XLIFF plus `.log.html`. 

