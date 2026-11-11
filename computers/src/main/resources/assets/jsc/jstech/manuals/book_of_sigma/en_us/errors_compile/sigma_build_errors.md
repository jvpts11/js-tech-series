---
section: errors_compile
order: 490
---
# Build refusals

The compiler can also refuse a build for two reasons that have nothing to do with the mistakes of your text. The first is a feature the compiler does not build yet. The second is a target machine that runs only the oldest instruction set.

Table: Refusals while building

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S4011` | a string with holes that was never reduced is not built yet | The compiler does not build a text with values put inside it yet. Write the text another way. |
| `S4011` | a lambda inside another one that keeps its variable is not built yet | The compiler does not build a lambda nested in another one yet. Write it without the nesting. |
| `S4011` | a lambda that keeps a variable a loop declares is not built yet | The compiler does not build a lambda that keeps a loop's variable yet. Rewrite the lambda so it does not keep that variable. |
| `S4012` | IA-16 runs Σ only; write it in Σ, in a Σ project | A Sigma# program asked for the IA-16 instruction set, the oldest one. Write the program in Sigma, in a Sigma project. |

The message of S4012 is shown at line 1, column 1 of the file.

## If something goes wrong

> [!PROBLEM] It says a lambda is not built yet, or a text is not built yet.
> The compiler does not support that form yet. Write the lambda or the text another way.

> [!PROBLEM] It says IA-16 runs Σ only.
> Your Sigma# program targets the oldest instruction set, IA-16, which runs only Sigma. Write it in Sigma, in a Sigma project.

> [!SEE] jsc:sigma_compile_errors
