---
section: library_toolkit
order: 463
---
# Json, Csv, Hash and Base64: data in text

Texts often carry data: a JSON document, a CSV table, a code. These types read them, all in System.Text, and came with Sigma# 3. Reading costs what its instructions cost.

Table: Json and JsonValue members

| Member | What it gives |
| --- | --- |
| `Json.Parse(text)` | The document as a JsonValue. A text that is not JSON stops the program with a FormatException. |
| `JsonValue.Get(name)` | An object's member, or null. You can write value[name] instead. |
| `JsonValue.At(number)` | An array's element, counted from 0. You can write value[number] instead. |
| `JsonValue.Has(name)` | Yes when the object has that member. |
| `JsonValue.Keys()` | The names of an object's members, in order. |
| `JsonValue.AsString()` | A text, a number, true or false, or null, written as text. |
| `JsonValue.AsNumber()` | A number, or a text that writes one. |
| `JsonValue.AsBool()` | true or false. |
| `JsonValue.Kind` | object, array, string, number, bool or null. |
| `JsonValue.Count` | How many members or elements it holds, or the length of a text. |

A JsonValue is read only. Build the text of a document yourself, with a StringBuilder.

```sgs Prices.sgs
JsonValue prices = Json.Parse(File.Read("prices.json"));
double iron = prices["items"][0]["price"].AsNumber();
```

Table: Csv, Hash and Base64 members

| Member | What it gives |
| --- | --- |
| `Csv.Parse(text)` | The rows of a CSV table, each a list of its fields. A field in quotes may hold commas and line breaks. |
| `Csv.Write(rows)` | The rows as CSV, a line each, quoting the fields that need it. |
| `Hash.Sha256(text)` | The text's SHA-256 fingerprint, in hexadecimal. |
| `Hash.Crc32(text)` | The text's CRC-32 checksum, as a number. |
| `Base64.Encode(text)` | The text written in Base64. |
| `Base64.Decode(text)` | The text that Base64 holds. What is not Base64 stops the program with a FormatException. |

> [!DEFINE] JSON
> A way of writing data as text: objects of named members, arrays, texts, numbers, true, false and null.

> [!DEFINE] CSV
> A table written as text: a row on each line, its fields parted by commas.

## If something goes wrong

> [!PROBLEM] The program stops and says the text is not JSON.
> Check the document at the character it names. Every text in JSON is written in double quotes.

> [!PROBLEM] Reading a member gives null.
> The object has no member of that name. Check it with Has, or list them with Keys.

> [!SEE] jsc:text_tools_library, jsc:files_and_file_handles
