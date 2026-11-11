---
section: library_basics
order: 439
---
# Console: reading and writing the terminal

The console is a program's line to the terminal: the screen the program is started in. Whatever you print appears there, and whatever you read is a line the player typed there. Most programs start by printing a question and reading the answer.

Console calls cost nothing. Printing does not end the line, but PrintLine does. A read waits until a line is typed, and a program that waits is parked.

Table: Console members

| Member | What it does | Language |
| --- | --- | --- |
| `Console.Print(text)` | Writes the text and leaves the line open. | Sigma and Sigma# |
| `Console.Print(char)` | Writes one character. | Sigma and Sigma#, version 2 |
| `Console.PrintLine(text)` | Writes the text and ends the line. | Sigma and Sigma# |
| `Console.Clear()` | Clears the lines the console holds. | Sigma and Sigma# |
| `Console.ReadLine()` | Waits for a line and gives it back as text. | Sigma and Sigma# |
| `Console.HasLine()` | Yes or no: is a typed line waiting? It never waits. | Sigma# only |
| `Console.Read()` | The next character as a number, the line break included. | Sigma and Sigma#, version 2 |
| `Console.Scan(out value)` | Reads a value of the variable's type, giving 1 when it works and 0 when the text is not one. | Sigma and Sigma#, version 2 |
| `Console.ReadInt()` | Reads the whole line as a whole number. | Sigma and Sigma# |
| `Console.ReadLong()` | Reads the whole line as a long whole number. | Sigma# only |
| `Console.ReadDouble()` | Reads the whole line as a decimal number. | Sigma# only |
| `Console.ReadBool()` | Reads the whole line as true or false. | Sigma and Sigma# |

After Read or Scan, the rest of the line waits for the next read. When Scan fails on a string variable, the variable becomes empty text.

ReadBool takes true, yes, y, on or 1 for true, and false, no, n, off or 0 for false. It ignores case and spaces, so you can type Yes or OFF.

The terminal keeps at most 16 typed lines waiting. Output is kept too: the last 200 lines, 4,096 characters at most for each line, and 64,000 characters in all. Older lines go first, and a line that is cut ends with a marker.

> [!DEFINE] Terminal
> The screen a program is started in: the machine's prompt or a window.

> [!DEFINE] Parked
> A program waiting on something outside itself. A parked program spends no credits while it waits.

## If something goes wrong

> [!PROBLEM] The program stops and says that the text is not a number.
> Type a whole number. To keep going on bad input, read a line with ReadLine and convert it with Convert.ToInt(text, fallback).

> [!PROBLEM] The program stops and says that the text is not true or false.
> ReadBool takes only true, yes, y, on or 1 for true, and false, no, n, off or 0 for false. Type one of those.

> [!PROBLEM] The program waits and nothing shows.
> It waits for a line. Type one in the terminal the program is in front of.

> [!PROBLEM] Old lines of output are gone.
> The console keeps only the last 200 lines. Print less, or write what you need to a file.

> [!PROBLEM] A line of output ends in a cut marker.
> A line keeps 4,096 characters. Split long text into several lines.

> [!SEE] jsc:sigma_runtime_stops
