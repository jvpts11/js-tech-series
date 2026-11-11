---
section: sigma_version_three_section
order: 428
---
# What version 3 brings

A project is held to a version of its language, which you choose in its settings. Version 3 is the newest. It brings the words and forms in the table, and a build held to version 2 refuses each of them where it is written, with the code S3057 or S2014.

Table: What version 3 brings

| What | Sigma | Sigma# |
| --- | --- | --- |
| const, labels and goto | Yes | Yes |
| comefrom, abstainfrom, reinstate, ignore and remember | Yes | Yes |
| \#if and the other directives | Yes | Yes |
| Pointers, sizeof and the calls of raw memory | Yes | Yes, in unsafe code |
| unsafe and stackalloc | No | Yes |
| try, catch, finally and throw | No | Yes |
| sealed, static classes, params, local functions and nameof | No | Yes |
| Tuples | No | Yes |
| Extension methods, operators and indexers | No | Yes |

A program written for version 2 still compiles in version 3, and to the very same listing. The new words are words only where the language reads them, so a program that calls a variable unsafe or a method sizeof keeps working. The words try, catch, finally, throw, goto, const, comefrom, abstainfrom and reinstate were kept by the versions before, so no program could use them as names.

A listing that uses an instruction of version 3, such as throw or a read through a pointer, is written in version 4 of the listing format, which an older runtime refuses. A program that uses none of them is written in the format it always was.

> [!DEFINE] Contextual word
> A word that means something only where the language expects it, and is a plain name everywhere else.

## If something goes wrong

> [!PROBLEM] The compiler says a word needs version 3 of the language.
> Hold the project to version 3 in its settings, or write the program without that word.

> [!SEE] jsc:sigma_constants_goto, jsc:sigma_exceptions, jsc:sigma_pointers
