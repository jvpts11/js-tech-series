---
section: examples_language
order: 512
---
# Old names

Old names are short, familiar functions, such as printf, puts and strlen, that older programs use. Each one is a short form of a call the library already has. For example, puts gives the same result as Console.PrintLine.

A method or a variable of your own program with the same name wins. The program calls its own method, not the old name. This program needs version 2, because it uses old names that version 2 adds, and its Main takes arguments.

## The program

```sgs OldNames.sgs
using System.*;
using System.IO.*;
namespace Corpus;
class OldNamesCorpus {
    static int Main(string[] args) {
        puts("How many ingots?");
        putchar('>');
        Console.Print(' ');
        string line = gets();
        int letter = getchar();
        int count;
        int scanned = scanf("%d", out count) + Console.Scan(out count) + letter;
        string built;
        strcpy(out built, line);
        strcat(ref built, "!");
        FILE notes = fopen("notes.txt", "a+");
        string held;
        bool gotLine = fgets(out held, notes) && !feof(notes) && notes.ReadLine(out held) && notes.AtEnd;
        fputs(built, notes);
        fputc('.', notes);
        notes.Write("x");
        notes.Write('y');
        int where = fgetc(notes) + notes.Read() + ftell(notes) + notes.Position + notes.Scan(out count);
        rewind(notes);
        fseek(notes, 2);
        notes.Seek(1);
        fprintf(notes, "%05d\n", where);
        int more = fscanf(notes, "%d", out count);
        fclose(notes);
        FILE again = File.Open("notes.txt", "r");
        again.Close();
        bool moved = remove("old.txt") || rename("a.txt", "b.txt") || File.Move("b.txt", "c.txt");
        double asked = atof(line);
        int n = abs(-3) + min(4, 5) + max(1, 2);
        double root = sqrt(pow(3, 2)) + floor(2.5) + abs(-0.5) + min(1.5, 2) + max(0.5, 1);
        srand(1987);
        int lucky = rand();
        int length = strlen(line);
        int at = strstr(line, "ingot");
        string said = sprintf("%s: %d, %f", itoa(n), lucky + length + at, root + asked);
        printf("%s\n", said);
        printf("%-12s|%5d|%05.2f|%#x|%e|%u\n", said, n, root, lucky, asked, length);
        int order = strcmp(line, said) + string.Compare(said, line) + atoi(line) + Convert.ToInt(line, -1);
        char first = toupper('a');
        char last = tolower(char.ToUpper('z'));
        bool kinds = isdigit(first) || isalpha(last) || isspace(' ') || char.ToLower(first) == 'a'
                || char.IsDigit('1') || char.IsLetter('b') || char.IsWhiteSpace('\t');
        puts(itoa(order, 16) + Convert.ToString(lucky, 2) + first + last + kinds);
        if (args.Length > 1) {
            exit(0);
        }
        return args.Length;
    }
}
```

## Input and output

Table: Old names for printing and reading

| Old name | The long way it calls |
| --- | --- |
| `printf` | `Console.Print, with a formatted text` |
| `puts` | `Console.PrintLine` |
| `putchar` | `Console.Print, with one character` |
| `gets` | `Console.ReadLine` |
| `getchar` | `Console.Read` |
| `scanf` | `Console.Scan, with one value` |
| `exit` | `Program.Exit` |

## Numbers and text

Table: Old names for numbers and text

| Old name | The long way it calls |
| --- | --- |
| `abs, min, max` | `Math.Abs, Math.Min, Math.Max` |
| `sqrt, pow, floor` | `Math.Sqrt, Math.Pow, Math.Floor` |
| `atoi` | `Convert.ToInt, which gives 0 for text that is not a number` |
| `atof` | `Convert.ToDouble` |
| `itoa` | `Convert.ToString` |
| `sprintf` | `the formatted text, as a value` |
| `srand, rand` | `Random.Seed, and Random.Next(32768)` |
| `strlen` | `the Length of the text` |
| `strstr` | `IndexOf of the text` |
| `strcmp` | `string.Compare` |
| `strcpy` | `the out place takes the text` |
| `strcat` | `the ref place gets the text added on` |
| `toupper, tolower` | `Char.ToUpper, Char.ToLower` |
| `isdigit, isalpha, isspace` | `Char.IsDigit, Char.IsLetter, Char.IsWhiteSpace` |

## Files

Table: Old names for files

| Old name | The long way it calls |
| --- | --- |
| `fopen` | `File.Open, which gives a FILE` |
| `fclose` | `Close of the file` |
| `fgets` | `ReadLine of the file` |
| `fputs, fprintf` | `Write of the text` |
| `fscanf` | `Scan of the file` |
| `fgetc` | `Read of the file` |
| `fputc` | `Write of one character` |
| `feof` | `AtEnd of the file` |
| `rewind` | `Seek to 0` |
| `fseek` | `Seek to the given position` |
| `ftell` | `Position of the file` |
| `remove` | `File.Delete` |
| `rename` | `File.Move` |

The text of a printf format holds holes, and a value fills each hole. A hole is a percent sign and a letter, and a number written into the hole sets its width.

Table: The holes of a format

| Hole | Takes |
| --- | --- |
| `%d, %i, %u, %x, %X, %o` | A whole number. |
| `%f, %e, %E` | A number of type double. |
| `%s` | Text. |
| `%c` | A character. |
| `%%` | A percent sign, written as itself. |

> [!DEFINE] Bare function
> A function written with no type or object in front of its name, such as puts(...).

> [!DEFINE] Format string
> A text with holes, such as %d, that the values of a call fill in order.

> [!DEFINE] Hole
> A place in a format string that one value fills.

> [!DEFINE] FILE
> A file that the program has open, from fopen.

> [!DEFINE] Out
> A variable that a method writes, and that the call hands to the method.

> [!DEFINE] Ref
> A variable that a method reads and writes, and that the call hands to the method.

## If something goes wrong

> [!PROBLEM] The program is built at version 1, and a name that needs version 2 is refused with code S3057. The message names the name, the language and the version it needs.
> Build with --lang 2, or take the name out of the program. See ([](jsc:sigma_examples_intro)).

> [!PROBLEM] The format of printf is kept in a variable, and the compiler refuses it with code S3053.
> Write the format in quotes inside the call, where it is used.

> [!PROBLEM] The format has more holes than the call has values, and the compiler refuses it with code S3055.
> Give one value for each hole, in order.

> [!PROBLEM] A value does not match its hole, such as text for a %d, and the compiler refuses it with code S3056.
> Change the value, or the letter of the hole, so that they match.

> [!PROBLEM] scanf is written with two holes, or without out, and the compiler refuses it.
> Write scanf with one hole and nothing around it, and the variable with out, as scanf("%d", out n) does.

> [!PROBLEM] Your method leaves an out argument empty on one way out, and the compiler refuses it with code S3037.
> Give the out argument a value before every return.

> [!SEE] jsc:sigma_printf_old_names, jsc:sigma_methods_out_ref, jsc:sigma_library
