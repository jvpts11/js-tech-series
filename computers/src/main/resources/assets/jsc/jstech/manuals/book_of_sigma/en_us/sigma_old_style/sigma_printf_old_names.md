---
section: sigma_old_style
order: 427
---
# printf and the old names

Sigma keeps a set of short names from an older style of programming, such as printf, strlen and puts. You call them with no type in front, and they work in Sigma and in Sigma#. The one you meet first is printf, which prints a text with values in it.

> [!DEFINE] Format string
> The text that printf prints, with holes in it where values go.

> [!DEFINE] Hole
> A place in a format string, marked with a percent sign and a letter, where a value is written.

printf takes a format string and then one value for each hole. It prints the text to the console and gives nothing back. The format string must be written in the call itself, in quotes, so that the compiler can check its holes against the values.

```sg Old.sg
using Standard.*;

namespace Old;

class Tool
{
    static void Main()
    {
        int n = 42;
        printf("n = %d, text = %s", n, "ok");
    }
}
```

This program prints n = 42, text = ok. The first hole takes the whole number n, and the second takes the text ok.

Table: The holes of a format string

| Hole | Wants | Version |
| --- | --- | --- |
| `%d, %i` | A whole number, an int or a long | 1 |
| `%u, %x, %X, %o` | A whole number | 2 |
| `%f` | A number | 1 |
| `%e, %E` | A number | 2 |
| `%s` | Text | 1 |
| `%c` | A char | 1 |
| `%%` | Nothing: it writes a percent sign | 1 |
| `l in front, as in %ld` | Nothing: it is accepted and means nothing | 1 |
| `ll, h, hh in front` | Nothing: accepted, and means nothing | 2 |
| `Flags, width, precision` | Flags such as - and 0, and a width or precision up to 999 | 2 |

The number of holes must match the number of values. Each hole also wants a kind of value: %d wants a whole number, %s wants text, and %c wants a char. The compiler checks all of this before the program runs.

sprintf takes the same format and values, and gives back the text instead of printing it. scanf reads one value from the console into a variable. Its format has exactly one hole and nothing else, and the variable is written with out, as in scanf("%d", out n). It gives back 1 when it read a value, and 0 when what was typed was not one. Every old name except printf needs version 2 of the project.

Each old name is one call of the library, and it gives the same value as the long way. strlen(text) gives the same as text.Length. A method or a variable of your own with the same name wins over the old name.

Table: Some old names and their long way

| Old name | Long way | Version |
| --- | --- | --- |
| `puts(text)` | `Console.PrintLine(text)` | 2 |
| `putchar(char)` | `Console.Print(char)` | 2 |
| `exit(int)` | `Program.Exit(int)` | 2 |
| `rand()` | `Random.Next(32768)` | 2 |
| `strlen(text)` | `text.Length` | 2 |
| `strcmp(a, b)` | `string.Compare(a, b)` | 2 |
| `atoi(text)` | `Convert.ToInt(text, 0)` | 2 |
| `toupper(char)` | `char.ToUpper(char)` | 2 |
| `fopen(path, mode)` | `File.Open(path, mode)` | 2 |

The [old names in the library](jsc:standard_functions) page lists every old name and the call it makes.

## If something goes wrong

> [!PROBLEM] The compiler says printf's format has to be written out where it is used, in quotes, so its holes can be read.
> Write the format string directly in the call, between double quotes.

> [!PROBLEM] The compiler says printf: the format has 2 holes and the call gives 1 value.
> Give one value for each hole, in the same order.

> [!PROBLEM] The compiler says printf: '%d' takes a whole number, and this is text.
> Use %s for text, or give a whole number to the hole.

> [!PROBLEM] The compiler says '%q' is no hole printf has.
> Use one of the holes in the table above.

> [!PROBLEM] The compiler says 'strlen' needs version 2; this project is version 1.
> Use a project that is held to version 2, or use printf, which every version has.

> [!SEE] jsc:sigma_strings_chars, jsc:sigma_library
