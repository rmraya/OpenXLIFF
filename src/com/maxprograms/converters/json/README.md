# JSON Filter Configuration Files

Configuration files for JSON filter are defined in a JSON file with these fields:

  - `traslatable`: array of JSON objects that define translatable keys
  - `ignorable`: array of strings listing ignorable keys 
## Translatable Object Fields:

 - `sourceKey`: key for the value to use as source text
 - `targetKey`: (optional) key for the value to use as target text
 - `idKey`: (optional) key of the value to use as segment ID
 - `resnameKey`: (optional) key of the value to use in the `resname` attribute in the generated `<trans-unit>` element
 - `noteKey`: (optional) key for values to be extracted as segment notes

 *Important:* when `idKey` is used, the corresponding values must be unique within the JSON file.

 ### Example

 ``` json
 {
    "translatable": [{
        "sourceKey": "text",
        "targetKey": "translation",
        "idKey": "key",
        "resnameKey": "label",
        "noteKey": "comment_text"
    },{
        "sourceKey": "original_text",
        "idKey": "key",
        "targetKey": "current_translation"
    },{
        "sourceKey": "name"
    }],
    "ignorable":[]
 }
 ```

 ## JSON Processing 

 1. The JSON filter configuraton parser reads the `translatable` array and makes a list of `sourceKey` values. A list of keys to ignore is built from the `ignorable` array.
 2. The filter reads the JSON file and iterates over all available objects and their descendants
 3. If an object that has a key in the `sourceKey` list is found, a new segment is created and its value uis used as source text. If the object has other keys defined in the configuration, they are used as target, id, resnae attribute or note as indicated.
 

  