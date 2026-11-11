---
section: errors_reading
order: 486
---
# Reading an error line

When you compile a Sigma program, the compiler checks every file of the program and lists each mistake it finds. A compile is one run of the compiler over your files, and the result is either a listing the machine can run or a list of mistakes. Each mistake takes one line, and that line tells you where it is, what kind it is and what it is about.

Table: Parts of an error line

| Part | What it tells you |
| --- | --- |
| The file name | The source file that holds the mistake, under the name you saved it. |
| (12,9) | The line and the column, both counted from 1 at the top left of the file. |
| error | The kind of line. Every line the compiler gives you is an error. |
| `S2001` | The error code. It names the kind of mistake, and it is the same in every language. |
| The message | What the mistake is, in words, with the names it found in it. |

For example, a mistake in a file called Monitor.sgs can read like this: "Monitor.sgs(12,9): error S2001: expected ';' but found '}'". The compiler expected a semicolon at line 12, column 9, and found a closing brace instead.

A mistake in a listing has the same shape, but no file name: "(4,1): error A4003: 'nonsense' is not an instruction". Read it the same way. The listing errors have their own page: [listing errors](jsc:sigma_listing_errors).

Read the list from the top and fix the first line first. A mistake early in a file can cause the lines after it, so the rest of the list can change when you fix it. The list is sorted by file, then by line, then by column.

The compiler lists at most 100 mistakes for each file. When a file has more, the last line of the list reads "too many errors; the rest were not reported", and you fix the ones above it before you compile again.

The editor's problem list, see [the editors](jsc:sigma_editors), groups the problems by file. The file with the most problems comes first, so the list starts with the file that needs the most work.

> [!DEFINE] compile
> Turning the source text of a program into a listing that a machine can run.

> [!DEFINE] listing
> The text file the compiler writes from a program, with the steps the machine runs. Its file name ends in asm.

## If something goes wrong

> [!PROBLEM] The message says "Monitor.sgs does not compile" when you start the program.
> The file failed to compile, so it has no listing to run. Open it in the editor, fix its first problem and compile it again.

> [!SEE] jsc:sigma_compile_errors
