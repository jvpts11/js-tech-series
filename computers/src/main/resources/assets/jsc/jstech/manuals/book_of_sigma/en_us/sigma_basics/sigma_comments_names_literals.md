---
section: sigma_basics
order: 412
---
# Comments, names and literals

A program is a list of statements. A statement is one instruction, such as storing a number in a variable or calling a method. Most statements end with a semicolon. Before you write one, you need the small pieces that statements are built from: comments, names, keywords and literals.

A comment is a note for the people who read the program, and the machine skips it. A line comment starts with two slashes and runs to the end of the line. A block comment starts with a slash and a star, and it ends at the first star and slash after that. Block comments do not nest: you cannot put one inside another.

A name is a word you choose for a variable, a method or a type. It starts with a letter or an underscore, and then holds letters, digits or underscores. Any letter counts, so accented and Greek letters are allowed in names. The keywords below are reserved: you cannot use them as names.

Table: Keywords, by group

| Group | Keywords |
| --- | --- |
| Types | `int long float double bool char string object void var` |
| Declarations | `namespace using class interface enum struct record delegate event` |
| Modifiers and actions | `new dispose this base static virtual override abstract public private protected internal readonly` |
| Statements | `if else while for foreach do switch case default break continue return lock in out` |
| Values and tests | `true false null is as` |
| Version 3's own, kept by the versions before it | `try catch finally throw goto const comefrom abstainfrom reinstate` |
| Words only where version 3 reads them | `sealed params nameof operator unsafe stackalloc sizeof byte ignore remember` |

Sigma refuses some of these words, even though the language knows them. The table "What Sigma has, and what Sigma# adds" shows which features each language has. A build held to version 2 refuses the words of version 3, and none of the kept ones can be a name. The words of the last row are words only where the language reads them, so a program may still use them as names.

A literal is a value written out in the program, such as the number 42, the number 0.5, true, or a text in double quotes. Each literal has a type, and the table shows which one. Numbers are written in plain decimal digits. A number with an exponent, such as 1e5, or with underscores, such as 1_000, is not a number the language can read.

Table: Literals

| Written as | Type | What it means |
| --- | --- | --- |
| `42` | `int` | A whole number, up to 2,147,483,647. |
| `42L` | `long` | A whole number with an L after it, for bigger numbers. |
| `0.5` | `double` | A number with a dot. Without a suffix, it is a double. |
| `3.14f` | `float` | A number with an f after it. |
| `2d` | `double` | A whole number with a d after it, written as a double. |
| `true, false` | `bool` | The two truth values. |
| `'A'` | `char` | Exactly one character, between single quotes. |
| `"text"` | `string` | Any text between double quotes, on one line. |
| `null` | none | Nothing. It fits any type that holds objects. |

Inside a text or a character, a backslash starts an escape. The letter after the backslash says what goes into the text, as the table shows.

Table: Escapes

| Letter after a backslash | Puts in |
| --- | --- |
| `n` | A new line. |
| `t` | A tab. |
| `r` | A carriage return. |
| `0` | The zero character. |
| `\` | A backslash. |
| `"` | A double quote. |
| `'` | A single quote. |

A text must close on the line where it starts. A text can also have holes filled from values, written with a dollar sign in front of the quotes, and that form belongs to Sigma# only.

Table: What Sigma has, and what Sigma# adds

| Feature | Sigma | Sigma# |
| --- | --- | --- |
| Basic types and literals: int, long, float, double, bool, char, string | Yes | Yes |
| Arrays | Yes | Yes |
| if, while, do, for, switch, break, continue and return | Yes | Yes |
| dispose | Yes | Yes |
| Type tests with is and as, and casts | Yes | Yes |
| class, struct and enum | Yes | Yes |
| virtual, override and the Script base class | Yes | Yes |
| abstract classes and methods | No | Yes |
| interface and record | No | Yes |
| delegate, event, property and lambda | No | Yes |
| var, which reads the type from the value | No | Yes |
| foreach and lock | No | Yes |
| List, Map and types written with angle brackets | No | Yes |
| Texts with holes, written with a dollar sign | No | Yes |
| using System and the System library | No | Yes |
| Old names, such as printf | Yes | Yes |
| Version 3: const, goto, the INTERCAL words, #if and pointers | Yes | Yes |
| Version 3: exceptions, tuples, unsafe and the small forms of C# | No | Yes |

Table: Errors in names and literals

| Error | Message | What to do |
| --- | --- | --- |
| `S1001` | the string was never closed before the end of the line | Close the text with a double quote on the same line. |
| `S1002` | the comment was never closed before the end of the file | Add the star and the slash that end the block comment. |
| `S1003` | '#' does not begin anything the language knows | Remove the character, or put it inside a text. |
| `S1004` | '1e5' is not a number the language can read | Write the number in plain decimal digits. |
| `S1005` | a character literal holds exactly one character | Keep one character between the single quotes. |
| `S1006` | '\\x' is not an escape the language knows | Use one of the escapes in the table above. |

> [!DEFINE] Statement
> One instruction of a program, such as an assignment or a call. Most statements end with a semicolon.

> [!DEFINE] Literal
> A value written out in the program itself, such as 42 or a text in quotes.

> [!DEFINE] Keyword
> A word the language reserves for its own use, such as if or int. A keyword cannot be a name.

> [!DEFINE] Type
> The kind of a value: whole numbers, decimal numbers, truth values, characters, text or objects. The type says what a variable may hold.

> [!DEFINE] Variable
> A named place that holds a value, which the program can read and change.

> [!DEFINE] Method
> A named piece of code that the program runs each time you call it by name.

## If something goes wrong

> [!PROBLEM] The compiler says the string was never closed before the end of the line.
> Put the closing double quote on the same line as the opening one.

> [!PROBLEM] The compiler says '1e5' is not a number the language can read.
> Write the number in plain decimal digits, such as 100000 for 1e5.

> [!SEE] jsc:sigma_examples, jsc:sigma_syntax_errors
