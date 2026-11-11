---
section: frames_help_and_studio
order: 226
---
# Virtual Studio

Virtual Studio is the workshop for Sigma# programs, in one window. You open a solution or a folder, write the sources, build them, and start the program. The language itself is in the Book of Sigma ([](jsc:book_of_sigma)).

It is an installable program, for Frames XP or newer. Installing it needs 512 MB of free disk space, and it holds 256 MB of memory while it is open.

## Opening your work

Table: The Start Window cards

| Card | What it opens |
| --- | --- |
| Open a project or solution | A .sln file on this machine. |
| Open a local folder | Any folder of programs. |
| Open a file | One .sgs or .sg file, with no project. |

Virtual Studio opens .sln, .sgproj, .sgsproj, .sg, .sgs and .asm files.

## Building and starting

1. Press [[Ctrl]]+[[Shift]]+[[B]] to build the solution.
2. Read the result: it says Build succeeded, or Build failed with the number of errors.
3. Press [[F5]] to build the startup project and run it at the terminal.

```table
{"caption": "Keys in Virtual Studio", "heads": ["Key", "What it does"], "rows": [[{"literal": "Ctrl+Shift+N"}, {"words": "Opens the New Project wizard."}], [{"literal": "Ctrl+Shift+B"}, {"words": "Builds the solution."}], [{"literal": "Ctrl+S"}, {"words": "Saves the file."}], [{"literal": "Ctrl+F"}, {"words": "Finds text."}], [{"literal": "Ctrl+G"}, {"words": "Goes to a line."}], [{"literal": "F5"}, {"words": "Builds the startup project and runs it."}], [{"literal": "Ctrl+W"}, {"words": "Closes the current tab."}], [{"literal": "Ctrl+."}, {"words": "Implements an interface."}], [{"literal": "Ctrl+`"}, {"words": "Opens the terminal, ready for typing."}], [{"literal": "Ctrl+Space"}, {"words": "Shows the completions, if they are turned on."}]]}
```

The list of what can follow a name says what each call costs the program, before you write the line. The Book of Sigma explains the editors ([](jsc:sigma_editors)).

> [!DEFINE] Solution
> A folder that groups projects. It has a file that ends in .sln.

> [!DEFINE] Project
> One program's source files, and the project file that says what to build.

> [!DEFINE] Template
> A starting point for a new project, offered by the New Project wizard.

> [!DEFINE] Build
> Turning the sources into a program. Start builds the startup project, then runs it.

## If something goes wrong

> [!PROBLEM] Virtual Studio cannot be installed on Frames 95.
> It needs Frames XP or newer ([](jsc:frames_xp)).

> [!PROBLEM] The disk has less than 512 MB of free space when you install it.
> Free up 512 MB of disk space, then install it.

> [!PROBLEM] A balloon says "Low on memory".
> The memory is past nine tenths. Close other programs, or add memory, before the system runs out and goes down ([](jsc:frames_memory)).

> [!PROBLEM] The result says Build failed.
> Fix the errors the build reports, then press [[Ctrl]]+[[Shift]]+[[B]] again.

> [!SEE] jsc:book_of_sigma, jsc:sigma_editors
