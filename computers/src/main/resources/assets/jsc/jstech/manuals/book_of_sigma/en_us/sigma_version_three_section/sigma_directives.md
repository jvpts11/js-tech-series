---
section: sigma_version_three_section
order: 431
---
# \#if and the symbols

A directive is a line that starts with # and speaks to the compiler, not to the machine. With #if, a part of a file is compiled only when a symbol is defined. The lines that are left out are not read at all, so they may hold anything.

Table: The directives

| Written as | What it does |
| --- | --- |
| `#if SYMBOL` | Compiles the lines up to the next #elif, #else or #endif when the symbol is defined. A condition joins symbols, true and false with &&, \|\|, !, == and !=, and brackets. |
| `#elif SYMBOL` | Another condition, asked when the ones before it were false. |
| `#else` | The lines compiled when no condition was true. |
| `#endif` | Ends the #if. |
| `#define SYMBOL, #undef SYMBOL` | Defines a symbol, or takes one away, from that line to the end of the file. |
| `#error text, #warning text` | Stops the build with that text, or tells it and goes on. |

Table: The symbols the compiler defines

| Symbol | When |
| --- | --- |
| The instruction set, in capitals, as X86 for jsc:x86 | Always: the machine the listing is built for. |
| The era, in capitals, as VINTAGE | When the machine that builds knows its era. |
| `SIGMA_3` | In every build of version 3. |

Your project can define its own symbols too, with a define: line in its project file. Each directive stands on a line of its own.

> [!DEFINE] Directive
> A line that starts with #, which tells the compiler which lines to read.

> [!DEFINE] Symbol
> A name a directive asks about, which is defined or is not.

## If something goes wrong

> [!PROBLEM] The compiler says an #if is never closed with an #endif.
> Add the #endif where the part that depends on the symbol ends.

> [!PROBLEM] The compiler says a word is not a directive the language reads.
> Use one of the directives in the table.

> [!SEE] jsc:sigma_version_three
