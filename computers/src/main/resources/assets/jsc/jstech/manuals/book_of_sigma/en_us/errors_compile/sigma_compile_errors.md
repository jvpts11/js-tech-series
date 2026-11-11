---
section: errors_compile
order: 487
---
# How the compiler reads a file

The compiler does not check a file all at once. It goes through a few steps in order, and each step can find mistakes. A mistake in one step can hide the mistakes of the steps after it, so the list can change as you fix things.

Table: The steps of a build

| Step | What it checks |
| --- | --- |
| Reading the text | The characters and the words of each file, one piece at a time. |
| Grammar | The shape of each line: its brackets, its keywords and its punctuation. |
| Language rules | Whether every feature the file uses exists in its language level. |
| Meaning | What the names and the types in the file stand for. |
| Listing | The listing is written only when no step found a mistake. |

The compiler checks meaning only when the earlier steps found no mistake at all. So a file with a syntax error shows its syntax errors first, and its meaning errors appear after you have fixed them. The editor checks a file while you type, and it is looser: it can report meaning errors while a line is still broken. Treat those as less certain, and check them again once the syntax errors are gone.

Table: The two language levels

| Level | Mark | Source file ends in | What it has |
| --- | --- | --- | --- |
| Sigma | Σ | `sg` | The smaller subset, which the oldest machines take. |
| Sigma# | Σ# | `sgs` | Everything the language has. |

The compiler builds a program for the oldest instruction set that runs every instruction the program uses. If you ask for a newer instruction set, the compiler keeps your choice.

The compiler reads at most 128 levels of nesting, which means blocks inside blocks inside blocks. Deeper than that, it reports code S2013 and does not read the rest of the file.

> [!DEFINE] syntax error
> A mistake in the shape of the text: a missing bracket, a word where a symbol belongs, or a line that is not finished.

> [!DEFINE] build
> One run of the compiler over a program: it gives a listing, or the messages when the program has mistakes.

> [!DEFINE] meaning
> What the names and the types of a program stand for, checked once its shape is right.

> [!DEFINE] instruction set
> The machine instructions a computer's processor runs. Each computer has one, and a program is built for one.

## If something goes wrong

> [!PROBLEM] The list changed after you fixed one line, and new mistakes appeared.
> That is expected: the steps hide each other's mistakes. Fix the first line of the new list, then compile again.

> [!PROBLEM] The compiler gives you a list of mistakes and no listing.
> The listing is written only when no mistake is left. Fix every line in the list, then compile again.

> [!SEE] jsc:sigma_syntax_errors, jsc:sigma_semantic_errors, jsc:sigma_build_errors
