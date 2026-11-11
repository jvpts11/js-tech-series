---
section: errors_compile
order: 488
---
# Syntax errors

A syntax error is a mistake in the shape of the text. The compiler cannot read the line as a statement, a declaration or a value, because a piece is missing, extra or in the wrong place. The codes below are the mistakes you can see on the line they name.

The compiler cuts the text into tokens first. A token is one piece of text: a word, a number, a string or a symbol. A message shows a token in quotes, such as ';'. A token with no single spelling is shown by its kind: an integer, a long integer, a float, a double, a string, a character, a name, or the end of the file.

> [!DEFINE] token
> One piece of text that the compiler reads as a single unit: a word, a number, a string or a symbol.

Table: Codes while reading the text

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S1001` | the string was never closed before the end of the line | A string has no closing quote on its line. Close it on the same line. |
| `S1002` | the comment was never closed before the end of the file | A comment has no closing mark. Add the closing mark. |
| `S1003` | '<character>' does not begin anything the language knows | The character is not part of the language. Remove it or replace it. |
| `S1004` | '<text>' is not a number the language can read | A number has letters or a second dot stuck to it, or a value too big for its type. Write the number again. |
| `S1005` | a character literal holds exactly one character | Two single quotes hold no character, or more than one. Keep exactly one character between them. |
| `S1006` | '\\<character>' is not an escape the language knows | A backslash is followed by a character that is not an escape. Use an escape the language has, or remove the backslash. |
| `S1007` | <text> | An #error line stops the build with its text, and a #warning line tells it. Read the text: the file's author wrote it for you. |
| `S1008` | '#<word>' is not a directive the language reads | A line starts with # and a word that is no directive. Use #if, #elif, #else, #endif, #define, #undef, #error or #warning. |
| `S1009` | '<text>' is not a condition #<word> can read | The condition holds something other than symbols, true, false, !, &&, \|\|, ==, != and brackets. Write it with those. |
| `S1010` | \#<word> has no #if open before it | An #elif, #else or #endif stands with no #if before it. Add the #if, or remove the line. |
| `S1011` | this #if is never closed with an #endif | Add the #endif where the part the #if chooses ends. |
| `S1012` | this #if already had its #else, so #<word> comes too late | An #elif or a second #else comes after the #else. Move it before the #else. |

Table: Codes while reading the grammar

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S2001` | expected <token> but found <token> | The grammar needs one piece and finds another. Put the missing piece where the message says. |
| `S2002` | expected a type but found <token> | A type name must stand here. Write the name of a type. |
| `S2003` | expected an expression but found <token> | A value must stand here, for example between the empty braces of an interpolated string. Write the value. |
| `S2004` | expected a field, a method, a property or an event but found <token> | A line inside a type is none of these four. Write one of them, or remove the line. |
| `S2005` | expected a class, a struct, a record, an interface, an enum or a delegate but found <token> | Where a type should start, the line starts with something else. Start the type with one of these words. |
| `S2006` | only a call, an assignment, an increment, a decrement or a new object can be used as a statement | A line holds a value that does nothing, such as a + 1. Assign it, print it, or delete it. |
| `S2007` | '<word>' was given twice | The same modifier stands twice, such as public public. Keep one of them. |
| `S2008` | the left side of an assignment must be a variable, a field, a property or an element | The left side of the = sign must be a variable, a field, a property or an element. Put one of these there. |
| `S2009` | a using has to come before the namespace and the types | A using line sits too low in the file. Move it above the namespace. |
| `S2010` | a file declares one namespace on its own line, before its types; put more of them in blocks | The file has more than one namespace line. Keep one on its own line and write the others in blocks. |
| `S2011` | every type is in a namespace: put 'namespace Name;' at the top of the file | A type stands with no namespace. Write a namespace line at the top of the file. |
| `S2013` | this is nested more than 128 levels deep, which is more than the compiler reads; the rest of the file was not read | There are too many blocks inside blocks. Split the deep part into smaller parts. |
| `S2014` | '<word>' is kept for a later version of the language; for now, <alternative> | The file writes try, catch, finally, throw, goto, const, comefrom, abstainfrom or reinstate in a build held to version 2 or older. Version 3 brings them, and the versions before it kept them, so no name can be one of them. Hold the project to version 3, or write what the message says. Sigma never has try, catch, finally or throw. |
| `S2015` | a label marks a statement, and <token> is not one | A label stands at the end of a block, or before something that is not a statement. Put the label in front of a statement. |

## If something goes wrong

> [!PROBLEM] It says "expected ';' but found '}'".
> The line before the brace does not end with a semicolon. Add the semicolon at its end.

> [!PROBLEM] It says "only a call, an assignment, an increment, a decrement or a new object can be used as a statement".
> The line holds a value that does nothing. Assign it, print it, or delete it.

> [!PROBLEM] It says every type is in a namespace.
> Write a namespace line at the top of the file, with a name you choose.

> [!PROBLEM] It says this is nested more than 128 levels deep.
> Split the deep part into smaller parts. The rest of the file is not read until then.

> [!SEE] jsc:sigma_compile_errors
