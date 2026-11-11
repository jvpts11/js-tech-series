---
section: sigma_data
order: 423
---
# Text and characters

Text is a string: a row of characters between double quotes. A string is a reference type, so it can be null. A char is one character, written between single quotes, and inside the machine it is its number.

You join text with +. When one side is text, the result is text, so "n = " + 5 gives n = 5. Two texts are equal with == when they hold the same characters. Text cannot be compared with < or >: to put texts in order, use string.Compare, which needs version 2 of the project. It gives back a number below zero, zero or above zero.

Table: Members of text

| Member | Gives back | What it does |
| --- | --- | --- |
| `Length` | An int | How many characters the text holds. It is a property, so write no brackets. |
| `Substring(start)` | Text | The text from the place start to the end. |
| `Substring(start, length)` | Text | A piece of the text, of the length you give. |
| `IndexOf(text)` | An int | The place of the first match, counted from 0, or -1 when there is none. |
| `Contains, StartsWith, EndsWith` | A bool | Whether the text holds, starts with or ends with the piece you give. |
| `ToUpper(), ToLower(), Trim()` | Text | A new text in capitals, in small letters, or without spaces at the ends. |
| `Replace(old, new)` | Text | A new text with every old piece changed to new. |
| `Split(char)` | A List of text, Sigma# only | The pieces between the character you give. |
| `string.Format(format, value)` | Text | Builds a text from a format and the values for its holes. |
| `string.Compare(a, b)` | An int | Puts two texts in order: below zero, zero or above zero. |

Text cannot be indexed like an array: text[0] is refused. To take the first character as text, use Substring(0, 1).

A char has its own tests: char.IsDigit(c), char.IsLetter(c), char.IsWhiteSpace(c), char.ToUpper(c) and char.ToLower(c). They need version 2 of the project. The older names, such as toupper, are on the [old names page](jsc:sigma_printf_old_names).

A char in arithmetic becomes a number. 'A' + 1 is an int, not a character.

```sg Names.sg
string name = "Ada Lovelace";
int first = name.IndexOf(" ");
string given = name.Substring(0, first);     // "Ada"
bool long_name = name.Length > 8;
string shout = given.ToUpper();              // "ADA"
int order = string.Compare("apple", "banana"); // below zero
```

> [!DEFINE] Index
> The place of an item in a row, counted from 0, so the first item is at place 0.

## If something goes wrong

> [!PROBLEM] The compiler says 'string' cannot be indexed by 'int'.
> Use Substring to take a piece of the text instead.

> [!SEE] jsc:sigma_list_map, jsc:sigma_printf_old_names
