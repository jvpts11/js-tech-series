---
section: library_toolkit
order: 462
---
# StringBuilder and Regex: building and matching text

Joining texts with + makes a new text every time. When you build a long text a piece at a time, use a StringBuilder: each piece is added at the same cost, however long the text already is. Both types live in System.Text and came with Sigma# 3.

Table: StringBuilder members

| Member | What it does |
| --- | --- |
| `new StringBuilder(), StringBuilder.new()` | An empty builder. |
| `StringBuilder.Append(text)` | Adds the text at the end, and gives the builder back, so calls can follow one another. |
| `StringBuilder.AppendLine(text)` | Adds the text and a line break. |
| `StringBuilder.Clear()` | Empties the builder. |
| `StringBuilder.ToString()` | Everything added, as one text. |
| `StringBuilder.Length` | How many characters it holds. |

A pattern finds text by its shape: \\d+ is one or more digits. Regex matches a pattern written as Java and C# write them.

Table: Regex members

| Member | What it gives |
| --- | --- |
| `Regex.IsMatch(text, pattern)` | Yes when the pattern is found anywhere in the text. |
| `Regex.Match(text, pattern)` | The first match and its groups, the whole match first, or an empty list when there is none. |
| `Regex.Matches(text, pattern)` | Every match, in order. |
| `Regex.Replace(text, pattern, with)` | The text with every match replaced. $1 in the replacement stands for the first group. |
| `Regex.Split(text, pattern)` | The pieces between the matches. |

A pattern costs a credit for every 4096 characters it reads. Some patterns read the same text many times over: a pattern that would read it more than 256 times over stops the program instead.

```sgs Builder.sgs
StringBuilder report = new StringBuilder();
foreach (string line in File.Read("stock.txt").Split('\n')) {
    if (Regex.IsMatch(line, "^iron")) {
        report.AppendLine(line);
    }
}
Console.PrintLine(report.ToString());
```

> [!DEFINE] Pattern
> A text that describes the shape of the text to find, such as \\d+ for a run of digits.

## If something goes wrong

> [!PROBLEM] The program stops and says the text is not a pattern.
> Check the pattern's brackets and backslashes. In a text, write a backslash twice: "\\\\d".

> [!PROBLEM] The program stops and says the pattern read the text too many times over.
> Write a simpler pattern: a group repeated inside a repeated group is what reads without end.

> [!SEE] jsc:numbers_and_time, jsc:data_library
