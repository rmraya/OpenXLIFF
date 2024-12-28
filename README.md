# OpenXLIFF Filters

![OpenXLIFF FIlters logo](openxliff_128.png) 

An open source set of Java filters for creating, merging and validating XLIFF 1.2, 2.0 and 2.1 files.

With OpenXLIFF Filters you can create XLIFF files that don't use proprietary markup and are compatible with most CAT (Computer Asisted Translation) tools.

- **[Releases](https://github.com/rmraya/OpenXLIFF#releases)**
- **[Binary Downloads](https://www.maxprograms.com/products/openxliff.html)**
- **[Filters Configuration](https://github.com/rmraya/OpenXLIFF#filters-configuration)**
- **[Related Projects](https://github.com/rmraya/OpenXLIFF#related-projects)**
- **[Supported File Formats](https://github.com/rmraya/OpenXLIFF#supported-file-formats)**

## Features

- **[Convert Document to XLIFF](https://github.com/rmraya/OpenXLIFF#convert-document-to-xliff)**  
- **[Convert XLIFF to Original Format](https://github.com/rmraya/OpenXLIFF#convert-xliff-to-original-format)**
- **[XLIFF Validation](https://github.com/rmraya/OpenXLIFF#xliff-validation)**
- **[Translation Status Analysis](https://github.com/rmraya/OpenXLIFF#translation-status-analysis)**
- **[Join multiple XLIFF files](https://github.com/rmraya/OpenXLIFF#join-multiple-xliff-files)**
- **[Pseudo-translate XLIFF file](https://github.com/rmraya/OpenXLIFF#pseudo-translate-xliff-file)**
- **[Copy Source to Target](https://github.com/rmraya/OpenXLIFF#copy-source-to-target)**
- **[Approve All Segments](https://github.com/rmraya/OpenXLIFF#approve-all-segments)**
- **[Remove All Targets](https://github.com/rmraya/OpenXLIFF#remove-all-targets)**
- **[Export Approved Segments as TMX](https://github.com/rmraya/OpenXLIFF#export-approved-segments-as-tmx)**

## Filters Configuration

XML and JSON filters are configurable

- **[XML Filter Configuration](https://github.com/rmraya/OpenXLIFF/tree/master/src/com/maxprograms/converters/xml#readme)**
- **[JSON Filter Configuration](https://github.com/rmraya/OpenXLIFF/tree/master/src/com/maxprograms/converters/json#readme)**

## Related Projects

- **[XLIFF Manager](https://github.com/rmraya/XLIFFManager)** implements an easy-to-use user interface for creating, merging, validating, and manipulating XLIFF files in a graphical environment.
- **[Swordfish](https://github.com/rmraya/Swordfish)** uses OpenXLIFF Filters to extract translatable text from supported formats.
- **[RemoteTM](https://github.com/rmraya/RemoteTM)** uses OpenXLIFF Filters to handle all TMX processing.
- **[Stingray](https://github.com/rmraya/Stingray)** uses OpenXLIFF Filters for extracting the text to align from supported monolingual documents.
- **[XLIFF Validation](https://github.com/rmraya/XLIFFValidation)** web-based XLIFF Validation Service.
- **[Fluenta](https://github.com/rmraya/Fluenta)** a Translation Manager that uses OpenXLIFF Filters to generate XLIFF from DITA projects.
- **[JavaPM](https://github.com/rmraya/JavaPM)** a set of scripts for localizing Java `.properties` files using XLIFF.

## Supported Versions

| Version   | Supported          |
| --------- | ------------------ |
| Latest    | :white_check_mark: |
| Any other | :x:                |

## Releases

| Version | Comment | Release Date |
|:-------:|---------|:------------:|
|4.1.0 | Adjusted for [XLIFF Manager](https://github.com/rmraya/XLIFFManager) 8.0 | December 28th, 2024|
|4.0.0 | Updated [XMLJava](https://github.com/rmraya/XMLJava) to version 2.0.0 | December 15th, 2024|
|3.25.0 | Handled locked status in SDLXLIFF files | November 24th, 2024|
|3.24.0 | Added character count and improved JSON filter | November 14th, 2024|
|3.23.0 | Migrated `.bat` scripts to `.cmd` | October 24th, 2024|
|3.22.0 | Added option to ignore translatable SVG files when parsing DITA | September 26th, 2024|
|3.21.0 | Fixed attributes parsing in HTML and improved JSON filter | August 29th, 2024|
|3.20.0 | Improved region merging for Word documents | March 17th, 2024|
|3.19.0 | Moved Language Tags handling to [BCP47J](https://github.com/rmraya/BCP47J) | March 7th, 2024|
|3.18.0 | Fixed scope builder for DITA filter; removed Machine Translation engines | February 5th, 2024|
|3.17.0 | Improved extraction from Word text boxes; switched to Java 21 | January 6th, 2024|
|3.16.0 | Added catalog for XLIFF 2.2; adjusted ChatGPT models | October 29th, 2023|
|3.15.0 | Added new options to JSON filter configuration | September 13th, 2023|
|3.14.0 | Added option to generate XLIFF 2.1 | September 1st, 2023|
|3.13.0 | Added export as TMX scripts; allowed relative paths from command line | August 27th, 2023|
|3.12.0 | Improved JSON support and localization | August 16th, 2023|
|3.11.0 | Improved SVG handling and language sorting | July 31st, 2023|
|3.10.0 | Fixed TMX exports | July 10th, 2023|
|3.9.0 | Improved Machine Translation support and internal code | June 30th, 2023|
|3.8.0 | Improved localization handling | June 2nd, 2023|
|3.7.0 | Extracted "alt" text from Word images | May 15th, 2023|
|3.6.0 | Improved segmentation of XLIFF 2.0 | April 1st, 2023|
|3.5.0 | Improved HTML filter | March 15th, 2023|
|3.4.0 | Extracted localizable strings | February 27th, 2023|
|3.3.0 | Detected loops while processing @keyref in DITA | February 20th, 2023|
|3.2.0 | Improved file management | February 4th, 2023|
|3.1.0 | Improved DITA merge | January 24th, 2023|
|3.0.0 | Moved XML code to project [XMLJava](https://github.com/rmraya/XMLJava) | January 9th, 2023|
|2.13.0 | Ignored tracked changes from Oxygen XML Editor | December 22nd, 2022|
|2.12.0 | Added "xmlfilter" parameter to conversion options | December 5th, 2022|
|2.11.0 | Improved support for DITA from Astoria CMS | December 2nd, 2022|
|2.10.0 | Fixed DITA segmentation | November 22nd, 2022|
|2.9.1 | Fixed joining of XLIFF 2.0 files and improved PHP Array support | October 22nd, 2022|
|2.9.0 | Added support for PHP Arrays | October 21st, 2022|
|2.8.0 | Updated TXLF, JSON and DITA filters | October 8th, 2022|
|2.7.0 | Fixed resegmenter for XLIFF 2.0 | August 12th, 2022|
|2.6.0 | Converted HTML fragments in Excel & Word files to tags | July 16th, 2022|
|2.5.0 | Added configuration options to JSON filter; Added scripts to approve all segments; Updated language list | July 6th, 2022|
|2.4.2 | Improved support for Trados Studio Packages | June 18th, 2022|
|2.4.1 | Fixed conversion of third party XLIFF files | June 10th, 2022|
|2.4.0 | Added remove all targets; added feedback for [Fluenta](https://www.maxprograms.com/products/fluenta.html) on DITA filter | June 9th, 2022|
|2.3.0 | Added copy source to target; Fixed DITA conversion and merge | May 25th, 2022|
|2.2.0 | Added pseudo-translation | May 11th, 2022|
|2.1.0 | Updated dependencies and improved validation of XLIFF 2.x | April 21st, 2022|
|2.0.0 | Moved server code to [XLIFF Manager](https://github.com/rmraya/XLIFFManager) project| March 29th, 2022|
|1.17.5 | Updated DITA keyscope handling | March 18th, 2022|
|1.17.4 | Fixed handling of nested untranslatables in DITA; Improved XLIFF 2.0 support | March 6th, 2022|
|1.17.2 | Fixed support for FrameMaker MIF files | February 6th, 2022|
|1.17.1 | Improved support for DITA | February 5th, 2022|
|1.17.0 | Improved validation of XLIFF 2.0; Added SVG statistics for XLIFF 2.0 | December 1st, 2021|
|1.16.0 | Improved support for XLIFF 2.0; Switched to Java 17 | November 23rd, 2021|
|1.15.2 | MS Office and DITA fixes | November 13th, 2021|
|1.15.0 | Initial support for RELAX NG gammars | November 8th, 2021|
|1.14.0 | Improved segmentation for XLIFF 2.0 | October 3rd, 2021|
|1.13.0 | Improved DITA support | August 31st, 2021|
|1.12.7 | Improved round trip 1.2 -> 2.0 -> 1.2; Ignored untranslatable SVG in DITA maps | July 4th, 2021|
|1.12.6 | Improved validation; updated language management | June 19th, 2021|
|1.12.5 | Improved support for bilingual files | February 3rd, 2021|
|1.12.4 | Allowed concurrent access for [XLIFF Validation](https://github.com/rmraya/XLIFFValidation) | January 22nd, 2021|
|1.12.3 | Improved support for Trados Studio packages | January 1st, 2021|
|1.12.2 | Suppressed output for confirmed empty targets | December 7th, 2020|
|1.12.1 | Improved conversion of XLIFF 1.2 files | December 3rd, 2020|
|1.12.0 | Added support for Adobe InCopy ICML and SRT subtitles | November 23rd, 2020|
|1.11.1 | Fixed JSON encoding; fixed import of XLIFF matches | November 1st, 2020|
|1.11.0 | Added support for JSON files | September 25, 2020|
|1.10.1 | Fixed handling of TXLF files and improved XML catalog | September 5th, 2020|
|1.10.0 | Added conversion of 3rd party XLIFF; improved support for XLIFF 2.0; fixed issues with Trados Studio packages | August 25th, 2020|
|1.9.1 | Added improvements required by [Swordfish IV](https://github.com/rmraya/Swordfish). | August 13th, 2020|
|1.9.0 | Added 5 Machine Translation (MT) engines (Google, Microsoft Azure, DeepL, MyMemory & Yandex) | May 18th, 2020|
|1.8.4 | Improved catalog and other minor fixes | April 25th, 2020|
|1.8.3 | Fixed conversion of PO files | April 17th, 2020|
|1.8.2 | Switched to synchronized classes in XML package | April 10th, 2020|
|1.8.1 | Improved support for Trados Studio packages | April 3rd, 2020|
|1.8.0 | Implemented support for Trados Studio packages | March 29th, 2020|
|1.7.0 | Major code cleanup; Changed segmentation model for XLIFF 2.0 | January 1st, 2020|
|1.6.0 | Added support for XLIFF files from WPML WordPress Plugin | December 1st, 2019|
|1.5.2 | Improved segmenter performance | October 29th, 2019|
|1.5.1 | Fixed catalog on Windows | September 22nd, 2019|
|1.5.0 | Improved support for empty &lt;xref/> elements in DITA; improved support for XML catalogs | September 5th, 2019|
|1.4.2 | Added option to join XLIFF files; Fixed merge errors in XLIFF 2.0; added tool info to XLIFF files; cleaned DITA attributes on merging | August 14th, 2019|
|1.4.1 | Improved performance embedding skeleton; added Apache Ant building option; renamed module to 'openxliff' | July 25th, 2019|
|1.4.0 | Improved report of task results | July 17th, 2019|
|1.3.3 | Fixed merging of MS Office files from XLIFF 2.0 | July 5th, 2019|
|1.3.2 | Updated for Swordfish 3.4.3 | June 30th, 2019|
|1.3.1 | Updated for Swordfish 3.4-0 | April 30th, 2019|
|1.3.0 | Added option to export approved XLIFF segments as TMX | April 18th, 2019|
|1.2.1 | Improved validation of XLIFF 2.0 | April 6th, 2019|
|1.2.0 | Added Translation Status Analysis | March 3rd, 2019|
|1.1.0 | Incorporated XLIFFChecker code| November 20th, 2018|
|1.0.0 | Initial Release | November 12th, 2018|

## Supported File Formats

OpenXLIFF Filters can generate XLIFF 1.2 and XLIFF 2.0 from these formats:

- **General Documentation**
  - Adobe InCopy ICML
  - Adobe InDesign Interchange (INX)
  - Adobe InDesign IDML CS4, CS5, CS6 & CC
  - HTML
  - Microsoft Office (2007 and newer)
  - Microsoft Visio XML Drawings (2007 and newer)
  - MIF (Maker Interchange Format)
  - OpenOffice / LibreOffice / StarOffice
  - PHP Arrays
  - Plain Text
  - SDLXLIFF (Trados Studio)
  - SRT Subtitles
  - Trados Studio Packages (*.sdlppx)
  - TXML (GlobalLink/Wordfast PRO)
  - WPML XLIFF (WordPress Multilingual Plugin)
  - Wordfast/GlobalLink XLIFF (*.txlf)
  - XLIFF from Other Tools (.mqxliff, .txlf, .xliff, etc.)
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
  - PHP Arrays
  - PO (Portable Objects)
  - RC (Windows C/C++ Resources)
  - ResX (Windows .NET Resources)
  - TS (Qt Linguist translation source)

## Requirements

- JDK 21 or newer is required for compiling and building. Pre-built binaries already include everything you need to run all options.
- Apache Ant 1.10.13 or newer

## Building

- Checkout this repository.
- Point your JAVA_HOME variable to JDK 21
- Run `ant` to generate a binary distribution in `./dist`

### Steps for building

``` bash
  git clone https://github.com/rmraya/OpenXLIFF.git
  cd OpenXLIFF
  ant
```

A binary distribution will be created in `/dist` folder.

## Convert Document to XLIFF

You can use the library in your own Java code. Conversion to XLIFF is handled by the class `com.maxprograms.converters.Convert`.

If you use binaries from the command line, running `.\convert.cmd` or `./convert.sh` without parameters displays help for XLIFF generation.

```text
Usage:

convert.sh [-help] [-version] -file sourceFile -srcLang sourceLang 
        [-tgtLang targetLang] [-skl skeletonFile] [-xliff xliffFile] 
        [-type fileType] [-enc encoding] [-srx srxFile] [-catalog catalogFile] 
        [-divatal ditaval] [-config configFile] [-embed] [-paragraph] 
        [-xmlfilter folder] [-2.0] [-2.1] [-ignoretc] [-ignoresvg] [-charsets]
        [-types]

Where:

   -help:      (optional) display this help information and exit
   -version:   (optional) display version & build information and exit
   -file:      source file to convert
   -srcLang:   source language code
   -tgtLang:   (optional) target language code
   -xliff:     (optional) XLIFF file to generate
   -skl:       (optional) skeleton file to generate
   -type:      (optional) document type
   -enc:       (optional) character set code for the source file
   -srx:       (optional) SRX file to use for segmentation
   -catalog:   (optional) XML catalog to use for processing
   -ditaval:   (optional) conditional processing file to use when converting DITA maps
   -config:    (optional) configuration file to use when converting JSON documents
   -embed:     (optional) store skeleton inside the XLIFF file
   -paragraph: (optional) use paragraph segmentation
   -xmlfilter: (optional) folder containing configuration files for the XML filter
   -ignoretc:  (optional) ignore tracked changes from Oxygen XML Editor in XML files
   -ignoresvg: (optional) ignore translatable SVG files when parsing DITA maps
   -2.0:       (optional) generate XLIFF 2.0
   -2.1:       (optional) generate XLIFF 2.1
   -charsets:  (optional) display a list of available character sets and exit
   -types:     (optional) display a list of supported document types and exit
```

Only two parameters are absolutely required: `-file` and `-srcLang`. The library tries to automatically detect format and encoding and exits with an error message if it can't guess them. If automatic detection doesn't work, add `-type` and `-enc` parameters.

Character sets vary with the operating system. Run the conversion script with `-charsets` to get a list of character sets available in your OS.

By default, XLIFF and skeleton are generated in the folder where the source document is located. Extensions used for XLIFF and Skeleton are `.xlf` and `.skl`.

The `XML` type handles multiple document formats, like `XHTML`, `SVG` or `DocBook` files.

Default XML catalog and SRX file are provided. You can also use custom versions if required.

### Supported document types

```text
   INX = Adobe InDesign Interchange
   ICML = Adobe InCopy ICML
   IDML = Adobe InDesign IDML
   DITA = DITA Map
   HTML = HTML Page
   JS = JavaScript
   JSON = JSON
   JAVA = Java Properties
   MIF = MIF (Maker Interchange Format)
   OFF = Microsoft Office 2007 Document
   OO = OpenOffice Document
   PHPA = PHP Array
   PO = PO (Portable Objects)
   RC = RC (Windows C/C++ Resources)
   RESX = ResX (Windows .NET Resources)
   SDLPPX = Trados Studio Package
   SDLXLIFF = SDLXLIFF Document
   SRT = SRT Subtitle
   TEXT = Plain Text
   TS = TS (Qt Linguist translation source)
   TXLF = Wordfast/GlobalLink XLIFF
   TXML = TXML Document
   WPML = WPML XLIFF
   XLIFF = XLIFF Document
   XML = XML Document
   XMLG = XML (Generic)
```

## Convert XLIFF to Original Format

You can convert XLIFF files created with OpenXLIFF Filters to original format using class `com.maxprograms.converters.Merge` in your Java code.

If you use binaries from the command line, running `.\merge.cmd` or `./merge.sh` without parameters will display the information you need to merge an XLIFF file.

```text
Usage:

   merge.cmd [-help] [-version] -xliff xliffFile -target targetFile [-catalog catalogFile] [-unapproved] [-export]

Where:

   -help:       (optional) Display this help information and exit
   -version:    (optional) Display version & build information and exit
   -xliff:      XLIFF file to merge
   -target:     translated file or folder where to store translated files
   -catalog:    (optional) XML catalog to use for processing
   -unapproved: (optional) accept translations from unapproved segments
   -export:     (optional) generate TMX file from approved segments
   -getTarget:  (optional) display a potential target file name and exit
```

## XLIFF Validation

The original [XLIFFChecker code](http://sourceforge.net/p/xliffchecker/code/) supports XLIFF 1.0, 1.1 and 1.2. The new version incorporated in OpenXLIFF Filters also supports XLIFF 2.0.

Standard XML Schema validation does not detect the use of duplicated 'id' attributes, wrong language codes and other constraints written in the different XLIFF specifications.

All XLIFF 2.0 modules are validated using XML Schema validation in a first pass. Extra validation is then performed using Java code for XLIFF 2.0 Core and for Metadata, Matches and Glossary modules.

You can validate XLIFF files using your own Java code. Validation of XLIFF files is handled by the class `com.maxprograms.validation.XliffChecker`.

If you use binaries from the command line, running `.\xliffchecker.cmd` or `./xliffchecker.sh` without parameters displays help for XLIFF validation.

```text
Usage:

   xliffchecker.cmd [-help] -file xliffFile [-catalog catalogFile]

Where:

   -help:      (optional) Display this help information and exit
   -file:      XLIFF file to validate
   -catalog:   (optional) XML catalog to use for processing
```

### XLIFF Validation Service

You can validate your XLIFF files online at [https://dev.maxprograms.com/Validation/](https://dev.maxprograms.com/Validation/)

## Translation Status Analysis

This library lets you produce an HTML file with word counts and segment status statistics from an XLIFF file.  

If you use binaries from the command line, running `.\analysis.cmd` or `./analysis.sh` without parameters displays help for statistics generation.

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

## Join multiple XLIFF files

You can combine several XLIFF files into a larger one using class `com.maxprograms.converters.Join` from your Java code or using the provided scripts.

Running `.\join.cmd` or `./join.sh` without parameters displays help for joining files.

```text
Usage:

   join.sh [-help] -target targetFile -files file1,file2,file3...

 Where:

   -help:     (optional) Display this help information and exit
   -target:   combined output XLIFF file
```

The merge process automatically splits the files when converting back to original format.

## Pseudo-translate XLIFF file

You can pseudo-translate  all untranslated segments using class `com.maxprograms.converters.PseudoTranslation` from your Java code or using the provided scripts.

Running `.\pseudotranslate.cmd` or `./pseudotranslate.sh` without parameters displays help for pseudo-translating an XLIFF file.

```text
Usage:

   pseudotranslate.cmd [-help] -xliff xliffFile [-catalog catalogFile]

Where:

   -help:      (optional) Display this help information and exit
   -xliff:     XLIFF file to pseudo-translate
   -catalog:   (optional) XML catalog to use for processing
```

## Copy Source to Target

You can copy the content of `<source>` elements to new `<target>` elements for all untranslated segments using class `com.maxprograms.converters.CopySources` from your Java code or using the provided scripts.

Running `.\copysources.cmd` or `./copysources.sh` without parameters displays help for copying source to target in an XLIFF file.

```text
Usage:

   copysources.cmd [-help] -xliff xliffFile [-catalog catalogFile]

Where:

   -help:      (optional) Display this help information and exit
   -xliff:     XLIFF file to process
   -catalog:   (optional) XML catalog to use for processing
```

## Approve All Segments

You can set all `<trans-unit>` or `<segment>` elements as `approved` or `final` if they contain target text using class `com.maxprograms.converters.ApproveAll` from your Java code or using the provided scripts.

Running `.\approveall.cmd` or `./approveall.sh` without parameters displays help for approving or confirming all segments in an XLIFF file.

```text
Usage:

   approveall.cmd [-help] -xliff xliffFile [-catalog catalogFile]

Where:

   -help:      (optional) Display this help information and exit
   -xliff:     XLIFF file to process
   -catalog:   (optional) XML catalog to use for processing
```

## Remove All Targets

You can remove`<target>` elements from all `<segment>` or `<trans-unit>` elements using class `com.maxprograms.converters.RemoveTargets` from your Java code or using the provided scripts.

Running `.\removetargets.cmd` or `./removetargets.sh` without parameters displays help for removing targets from an XLIFF file.

```text
Usage:

   removetargets.cmd [-help] -xliff xliffFile [-catalog catalogFile]

Where:

   -help:      (optional) Display this help information and exit
   -xliff:     XLIFF file to process
   -catalog:   (optional) XML catalog to use for processing
```

## Export Approved Segments as TMX

You can export all aproved segments from an XLIFF file as TMX using class `com.maxprograms.converters.TmxExporter` from your Java code or using the provided scripts.

Running `.\exporttmx.cmd` or `./exporttmx.sh` without parameters displays help for exporting approved segments from an XLIFF file.

```text
Usage:

exporttmx.sh [-help] -xliff xliffFile [-tmx tmxFile] [-catalog catalogFile]

Where:

    -help:      (optional) Display this help information and exit
    -xliff:     XLIFF file to process
    -tmx:       (optional) TMX file to generate
    -catalog:   (optional) XML catalog to use for processing
```

If the optional `-tmx` parameter is not provided, the TMX file will be generated in the same folder as the XLIFF file and its name will be the same as the XLIFF file plus `.tmx`.
