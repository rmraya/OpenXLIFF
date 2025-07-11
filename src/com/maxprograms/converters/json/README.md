# JSON Filter Configuration Files

Configuration files for JSON filter are defined in a JSON file that contains three arrays and six optional boolean keys:

- `traslatable`: array of JSON objects that define translatable keys
- `ignorable`: array of strings listing ignorable keys
- `htmlIgnore`: array of strings listing HTML 5 custom elements that should not be converted to inline tags
- `parseEntities`: boolean value indicating whether HTML entitites should be converted to Unicode characters. Default: `false`
- `trimTags`: send initial/trailing tags to skeleton when possible. Default: `true`
- `mergeTags`: merge adjacent tags. Default: `true`
- `rawSegmentation`: treat source and target as plain text for segmentation. Default: `false`
- `exportHTML`: treat target as containg HTML on merge. Default: `false`
- `preserveSpaces`: preserve spaces found in the parsed text. Default: `false`

Configuration files must be written using UTF-8 character set without a byte order mark (BOM).

## Translatable Object Fields

- `sourceKey`: key for the value to use as source text
- `targetKey`: (optional) key for the value to use as target text
- `idKey`: (optional) key of the value to use as segment ID
- `resnameKey`: (optional) key of the value to use in the `resname` attribute in the generated `<trans-unit>` element
- `noteKey`: (optional) key for values to be extracted as segment notes
- `replicateNotes`: (optional) boolean indicating whether to include notes in all segments when there is more than one
- `approvedKey`: (optional) key for values containing approval status for the segment.

### Notes

- Key `targetKey` is ignored if the target language is not specified when calling the JSON filter.
- When `idKey` is used, the corresponding values must be unique within the JSON file.
- If multiple segments are generated from a single `sourceKey`, notes harvested from `noteKey` are only added to the first segment.
- if the number of segments generated from `sourceKey` is different from the number of segments generated from `targetKey`, only one `<trans-unit>` element is generated with source and target unsegmented.
- When `idKey` is present and multiple segments are generated, a suffix based on the segment count is added to the `id` attribute.
- Tags from `<source>` or `<target>` are sent to skeleton when `trimTags` is `true` and:
  - There is just one tag and it is at the beginning
  - There is just one tag and it is at the end
  - There are only two tags, one at the beginning and one at the end
- When `approvedKey` is present, the generated segment is marked as approved if:
  - The segment contains a `<target>` element
  - The value of `approvedKey` object is a boolean `true`
  - The value of `approvedKey` object is the string `yes` (case insensitive, `Yes` or `YES` are also valid)

### Example

 ``` json
{
    "translatable": [
        {
            "sourceKey": "original_text",
            "targetKey": "current_translation"
        },
        {
            "sourceKey": "source_text",
            "idKey": "textblock_id",
            "targetKey": "target_text",
            "resnameKey": "item_label",
            "noteKey": "comments",
            "replicateNotes": true,
            "approvedKey": "approved"
        }
    ],
    "ignorable": [
        "original_id",
        "key"
    ],
    "htmlIgnore": [
        "myTitle", 
        "footNote"
    ],
    "parseEntities": true,
    "trimTags": false,    
    "mergeTags": false,
    "rawSegmentation": true,
    "exportHTML" : true
}
 ```

## JSON Processing

 1. The JSON filter configuration parser reads the `translatable` array and makes a list of possible `sourceKey` values. A list of keys to ignore is built from the `ignorable` array.
 2. The filter reads the JSON file and iterates over all available objects and their descendants
 3. If an object that has a key in the `sourceKey` list is found, a new segment is created and its value is used as source text. If the object has other keys defined in the configuration, they are used as target, id, resname attribute or note as indicated. Attribute `approved` is set to `yes` when `approvedKey` is present and conditions are met.
 4. If an object contains a key that matches `noteKey`, a list of text strings is created from its content and all descendents. Each found string is added as a `<note>` element in the active segment.
 5. All other remaining key/value pairs in the object are checked. If a value contains text, it is extracted as a new segment wihout target or special attributes. To prevent creation of unwanted segments, add the corresponding keys to the `ignorable` list.
 6. By default, the filter converts the opening and closing tags of HTML 5 elements to XLIFF inline tags. To prevent this, add the element names to the `htmlIgnore` list.
  