# OpenXLIFF Filters Localization

Localizing OpenXLIFF Filters requires processing 2 types of files:

1. Java `.properties` files
2. Language files in XML format

## Localization of Java .properties

[JavaPM](https://www.maxprograms.com/products/javapm.html) is used to generate XLIFF from `/src` folder.

Use a command like this to generate XLIFF:

```bash
/path-to-Javapm/createxliff.sh -src /path-to-OpenXLIFF/src -xliff yourXliffFile.xlf -srcLang en -tgtLang fr -enc UTF-8
```

OpenXLIFF Filters .properties are encoded in UTF-8; translated versions must be generated using UTF-8 character set.

## Localization of language files

Two XML files contains the list of languages used by OpenXLIFF Filters. These files are located in `/src/com/maxprograms/languages/` folder.

The XML files that need translations are:

- languageList.xml
- extendedLanguageList.xml

Use `convert.bat` or `convert.sh` to generate XLIFF from these files:

```bash
dist/convert.sh -embed -type XMLG -srcLang en -file /path-to-OpenXLIFF/src/com/maxprograms/languages/languageList.xml -xliff /path-to-OpenXLIFF/i18n/languageList.xml.xlf 

dist/convert.sh -embed -type XMLG -srcLang en -file /path-to-OpenXLIFF/src/com/maxprograms/languages/extendedLanguageList.xml-xliff /path-to-OpenXLIFF/i18n/extendedLanguageList.xml.xlf
```

You can find sample XLIFF 2.0 files for .properties and languages in `/i18n` folder.
