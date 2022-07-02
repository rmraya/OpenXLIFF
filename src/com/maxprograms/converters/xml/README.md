# XLIFF Filter Configuration Files

The XML filter operates in two modes:

- `Generic mode`: extracts text from any XML element;
- `With Configuration File`: extracts text from selected elements and attributes, with the ability to ignore elements that are not required.

Configuration files are stored in `/xmlfilter` folder of OpenXLIFF distribution. They are written in XML format and their names should be the root element of the files to parse prefixed by `config_`, for example: `config_svg.xml`.

All configuration files must comply with the grammar defined in `configuration.dtd`, available at `/catalog/maxprograms` folder of OpenXLIFF distribution.

The root element of an XML configuration file is `<ini-file>`. It contains one or more `<tag>` elements.

The content of the `<tag>` element is the name of the element being configured. Four attributes are used to define how to process that element.

## Element Configuration Attributes

Attribute|Description
---------|-----------
hard-break|Set to `"segment"` if the element starts a new segment; set to `"inline"` if the element is an inline element; set to `"ignore"` if the element and its children should be ignored.
ctype|Description to use for inline elements in `<ph>` placeholder.
attributes|List of attributes that contain translatable text. Write attribute names separated with `";"`.
keep-format| Set to `"yes"` if you need to preserve white spaces. If not present, `"no"` is assumed.

### Attribute Values

Attribute|Value
---------|-----
hard-break|(segment\|inline\|ignore) #REQUIRED
ctype|(image\|pb\|lb\|x-bold\|x-entry\|x-font\|x-italic\|x-link\|x-underlined\|x-other) #IMPLIED
attributes|CDATA #IMPLIED
keep-format|(yes\|no) #IMPLIED

## Example

The configuration file for converting [SVG](https://en.wikipedia.org/wiki/Scalable_Vector_Graphics) images to XLIFF looks like this:

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ini-file PUBLIC "-//MAXPROGRAMS//Converters 2.0.0//EN" "configuration.dtd">
<ini-file>
    <tag hard-break="segment" keep-format="yes">text</tag>
    <tag ctype="x-font" hard-break="inline">tspan</tag>
    <tag ctype="x-other" hard-break="inline">tref</tag>
    <tag ctype="x-other" hard-break="inline">textPath</tag>
    <tag hard-break="segment">title</tag>
    <tag hard-break="segment">desc</tag>
    <tag attributes="xlink:title" ctype="x-link" hard-break="inline">a</tag>
</ini-file>
```
