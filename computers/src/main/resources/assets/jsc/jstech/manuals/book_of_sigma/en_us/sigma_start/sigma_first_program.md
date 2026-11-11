---
section: sigma_start
order: 407
---
# Your first program

A program starts in one place, and the compiler must find that place. The simplest program prints one line and ends. On Frames, Virtual Studio can make a starter project for it with its wizard, and then one key runs it ([](jsc:virtual_studio)).

The wizard offers four kinds of project, and each comes in both languages. You pick the kind and the language, then give the project a name.

Table: The four kinds of project

| Kind | What it is |
| --- | --- |
| Console App | A program with a Main that runs at the terminal it was started from, prints as it goes, and is gone when it returns. |
| Script | A program that stays up. It is set up once, it is called again and again while it runs, and it is told when it is stopped. |
| Class Library | Classes that other projects share. It has no start of its own, and it compiles into the projects that use it. |
| Empty Project | A project with no files in it, for starting from nothing. |

The name may use only letters, digits and underscores, and it must not start with a digit. The name becomes the [namespace](jsc:sigma_namespaces_using), the class name, and the text the program prints.

The wizard writes the project file, NAME.sgsproj for Sigma# (NAME.sgproj for Sigma), and the source file, NAME.sgs for Sigma# (NAME.sg for Sigma). When you build, the listing goes to build/NAME.asm.

> [!DEFINE] project file
> The text file in a project folder that names the project and lists its source files and settings.

Here is the starter code of a Sigma# console app, in a project called Hello. The name Hello is used for the namespace, for the class, and for the text the program prints.

```sgs Hello.sgs
using System.IO.*;

namespace Hello;

class Hello {
    static void Main() {
        Console.PrintLine("Hello from Hello");
    }
}
```

The same program in Sigma looks like this. Sigma prints with printf, and you end the text with a line break yourself, written as a backslash and an n.

```sg Hello.sg
using Standard.*;

namespace Hello;

class Hello {
    static void Main() {
        printf("Hello from Hello\n");
    }
}
```

A program needs exactly one place to start: a class that implements IScript, or a class with a static Main. A Main is static, returns void or int, and takes nothing or the arguments as a string array. Each using line brings a library into the program, so its names can be written short.

> [!DEFINE] Main
> The method where a console program starts.

> [!DEFINE] IScript
> The interface a script implements, with the methods the runtime calls on it: OnInit, OnTick and OnDestroy.

> [!DEFINE] using
> A line at the top of a source file that brings a library into the program.

Returning an int from Main, or taking the arguments, needs Sigma# version 2. Each project holds its own version ([](jsc:sigma_projects)).

## Your first run in Virtual Studio

1. Open the New Project wizard with [[Ctrl]]+[[Shift]]+[[N]] in Virtual Studio.
2. Choose the Console App offer for Sigma#, and type a name, such as Hello.
3. Press [[F5]] to build the project and run it.
4. Read the line the program prints in the terminal.

The program prints Hello from Hello, and it is gone when Main returns. Compiling it on the command line is shown in [](jsc:sigma_compile).

## If something goes wrong

> [!PROBLEM] "a program needs exactly one place to start" appears, with the count of starts it found.
> Keep one class with a static Main, or one class that implements IScript, and remove the others.

> [!PROBLEM] The build prints an error that names a file and a place in it, such as hello.sgs(3,5): line 3, fifth character.
> Read the error and fix the line it names, then build again ([](jsc:sigma_errors)).

> [!SEE] jsc:sigma_compile, jsc:sigma_projects
