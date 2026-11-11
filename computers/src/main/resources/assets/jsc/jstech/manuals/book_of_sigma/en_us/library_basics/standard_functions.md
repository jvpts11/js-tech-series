---
section: library_basics
order: 438
---
# The old names: printf, rand and the rest

Programs written in the old style call bare names such as printf, rand and strlen. Sigma and Sigma# still accept them. Each bare name is a call on a library member, so puts(s) does the same as Console.PrintLine(s).

printf is version 1, the calls of raw memory are version 3, and every other name on this page is version 2, so an older build refuses it. Each table row names the member the old name calls, so you can read that member's entry in this book.

Table: Printing and reading

| Old name | Library call |
| --- | --- |
| `printf(format, ...)` | Console.Print, filled from the values after the format. |
| `sprintf(format, ...)` | Gives the text back instead of printing it. |
| `puts(text)` | Console.PrintLine: prints and ends the line. |
| `putchar(char)` | Console.Print for one character. |
| `gets()` | Console.ReadLine: waits for a line. |
| `getchar()` | Console.Read: the next character as a number. |
| `scanf(format, out ...)` | Console.Scan, one value per hole. |

Table: Numbers and text

| Old name | Library call |
| --- | --- |
| `exit(status)` | Program.Exit: ends the program with the status. |
| `abs, min, max` | Math.Abs, Math.Min and Math.Max. |
| `floor, sqrt, pow` | Math.Floor, Math.Sqrt and Math.Pow. |
| `atoi(text)` | Convert.ToInt(text, 0): 0 when the text is not a number. |
| `atof(text)` | Convert.ToDouble. |
| `itoa(value, base)` | Convert.ToString(number, base). |
| `srand(seed)` | Random.Seed: starts the sequence again. |
| `rand()` | Random.Next(32768): a number from 0 to 32767. |
| `strlen(text)` | The length of the text, as string.Length. |
| `strstr(text, part)` | The place of the part inside the text, as IndexOf. |
| `strcmp(a, b)` | string.Compare: the order of two texts. |
| `strcpy(out dest, src)` | Puts the text into dest. |
| `strcat(ref dest, src)` | Adds the text to the end of dest. |
| `toupper, tolower` | The upper or lower case of one character. |
| `isdigit, isalpha, isspace` | char.IsDigit, char.IsLetter and char.IsWhiteSpace. |

Table: Files and the machine

| Old name | Library call |
| --- | --- |
| `remove(path)` | File.Delete: gives true or false. |
| `rename(from, to)` | File.Move: gives true or false. |
| `fopen(path, mode)` | File.Open: gives a FILE, or null. |
| `fclose(file)` | FILE.Close(). |
| `fgets(out line, file)` | FILE.ReadLine. |
| `fputs(text, file)` | FILE.Write with the text. |
| `fprintf(file, format, ...)` | FILE.Write with the formatted text. |
| `fscanf(file, format, ...)` | FILE.Scan. |
| `fgetc(file)` | FILE.Read(), the next character as a number. |
| `fputc(char, file)` | FILE.Write with one character. |
| `feof(file)` | FILE.AtEnd. |
| `rewind(file)` | FILE.Seek(0): back to the start. |
| `fseek(file, place)` | FILE.Seek with the place. |
| `ftell(file)` | FILE.Position: the place now. |
| `bus(name)` | Bus.Named: the bus with that name. |
| `redstone(name)` | Redstone.Named: the Redstone Interface with that name. |
| `craftInterface(name)` | CraftInterface.Named. |
| `craftRouter(name)` | CraftRouter.Named. |

The formats of printf, sprintf, scanf, fprintf and fscanf work the same way. A hole starts with a percent sign and a letter: %d for a whole number, %f for a decimal number, %s for text, %c for a character and %% for a percent sign.

The compiler knows %d, %i, %u, %x, %X, %o, %f, %e, %E, %s, %c and %%. A version 1 hole takes only the plain letters d, i, f, s and c, with nothing between the percent sign and the letter. Flags, widths and precisions came in version 2.

> [!DEFINE] Bare function
> An old-style name, such as printf or rand, that the library answers with a member you can also call by its full name.

> [!DEFINE] Hole
> A place in a format text, starting with a percent sign, that is filled with a value.

> [!DEFINE] FILE
> A file the program has opened, which it works on a piece at a time.

> [!DEFINE] Null
> No value at all: the program holds nothing, and a call that gives null answers with nothing.

Table: Raw memory, in version 3

| Old name | Library call |
| --- | --- |
| `malloc(bytes)` | Memory.Alloc: a new block of raw memory. |
| `calloc(count, size)` | Memory.AllocZeroed: a block for count values of that size. |
| `realloc(p, bytes)` | Memory.Resize: the block at a new size. |
| `free(p)` | Memory.Free: gives the block back. |
| `memcpy(dest, src, bytes)` | Memory.Copy, in the same order. |
| `memset(p, value, bytes)` | Memory.Fill, in the same order. |
| `memcmp(a, b, bytes)` | Memory.Compare: below zero, zero or above zero. |

## If something goes wrong

> [!PROBLEM] The compiler says a hole is not one printf has.
> Use one of the holes listed above, such as %d for a whole number or %s for text.

> [!PROBLEM] The compiler says a width or a precision cannot come from a value here.
> Write the number into the format, as in %5d, instead of a star.

> [!PROBLEM] The compiler says the format must be written out.
> Put the format between quotes in the call, not in a variable.

> [!PROBLEM] The program stops and says that the hole is not one this machine's printf can write.
> Use a hole from the list above.

> [!SEE] jsc:sigma_printf_old_names, jsc:sigma_methods_out_ref
