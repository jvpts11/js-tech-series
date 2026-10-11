/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The getting-started pages of The Book of Sigma: the two languages and which one to choose, installing their
 * compilers and runtime, the first program, compiling, running, the editors and projects. The pages assume nothing
 * about the mod or about programming.
 */
final class SigmaStartPages {

    private SigmaStartPages() {
    }

    /** Declares the section and the entries of the getting-started pages, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_start").titled("Getting started").in(chapter)
                .register();
        twoLanguages(guide, section);
        installing(guide, section);
        firstProgram(guide, section);
        compiling(guide, section);
        running(guide, section);
        editors(guide, section);
        projects(guide, section);
    }

    private static void twoLanguages(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_two_languages", section).titled("Two languages, one family")
                .paragraph("Sigma and Sigma# are the two programming languages of the computers here. They are one"
                        + " family: everything written in Sigma is also a valid Sigma# program. You learn one, and"
                        + " you can read the other.")
                .define("Sigma", "The smaller of the two programming languages. Its source files end in .sg, and its"
                        + " compiler is scc.")
                .define("Sigma#", "The full programming language, which accepts everything Sigma accepts. Its source"
                        + " files end in .sgs, and its compiler is sgsc.")
                .table("The two languages", "Language", "Compiler", "Source file ending")
                .row(GuideCell.words("Sigma"), GuideCell.code("scc"), GuideCell.code(".sg"))
                .row(GuideCell.words("Sigma#"), GuideCell.code("sgsc"), GuideCell.code(".sgs"))
                .paragraph("Sigma keeps a [class](jsc:sigma_classes), a struct, an array, a loop and a call, and"
                        + " nothing else of the advanced kind. Sigma# adds what Sigma leaves out, such as threads,"
                        + " collections, interfaces and generics. A program written in Sigma compiles unchanged in"
                        + " Sigma#.")
                .paragraph("Which one should you choose? Your computer decides first. A Vintage computer can hold"
                        + " only the Sigma compiler, so Sigma is its only language. A Legacy computer or later holds"
                        + " the whole toolchain, and then Sigma# gives you the full library.")
                .paragraph("A compiler turns a source file into a listing: the assembly text that a computer runs,"
                        + " in a file that ends in .asm. Each listing is built for one instruction set.")
                .define("compiler", "A program that turns source text into a listing. When the source has an error,"
                        + " it prints the errors and writes nothing.")
                .define("listing", "The assembly text a compiler writes, in a file that ends in .asm.")
                .define("source file", "A text file that holds a program in Sigma or Sigma#, written before it is"
                        + " compiled.")
                .define("project", "A folder that holds the source files of one program, and the settings that build"
                        + " it.")
                .paragraph("The runtime runs the listings. It is a package of its own, and once it is installed it"
                        + " gives the computer the sigma command ([](jsc:sigma_installing)).")
                .define("runtime", "The program that runs listings on a computer, and that gives the computer the"
                        + " sigma command.")
                .paragraph("Each computer runs one instruction set (ISA): the basic operations its processor knows."
                        + " A listing is built for one set, and a computer runs it only if its processor knows that"
                        + " set. This mod has three: IA-16, x86 and x86-64.")
                .define("instruction set (ISA)", "The basic operations a processor knows. A listing is built for one"
                        + " set, and a computer runs it only if its processor knows that set.")
                .table("The three instruction sets", "Name", "Width", "Runs listings built for")
                .row(GuideCell.code("IA-16"), GuideCell.words("16 bits"), GuideCell.words("IA-16 only"))
                .row(GuideCell.code("x86"), GuideCell.words("32 bits"), GuideCell.words("x86 and IA-16"))
                .row(GuideCell.code("x86-64"), GuideCell.words("64 bits"),
                        GuideCell.words("x86-64, x86 and IA-16"))
                .paragraph("A program built for the oldest instruction set keeps working on every newer one. That is"
                        + " why IA-16 listings run everywhere, and x86-64 listings run only on x86-64 machines.")
                .paragraph("Every computer also has an era, its hardware generation: Vintage, Legacy, Transition,"
                        + " Standard, Advanced, Exa or Singularity. The era decides which instruction set the"
                        + " computer has, and which software it can install. The eras are explained in"
                        + " [](jsc:eras).")
                .define("era", "The hardware generation of a computer. The era decides its instruction set and which"
                        + " software it can install.")
                .table("The instruction set of each era", "Era", "Instruction set")
                .row(GuideCell.words("Vintage"), GuideCell.code("IA-16"))
                .row(GuideCell.words("Legacy"), GuideCell.code("x86"))
                .row(GuideCell.words("Transition, Standard, Advanced, Exa and Singularity"),
                        GuideCell.code("x86-64"))
                .paragraph("When you do not choose an instruction set, the build takes the oldest one the language"
                        + " runs on: IA-16 for Sigma, and x86 for Sigma#. So a Sigma# program built by default runs"
                        + " from a Legacy computer up, and never on a Vintage one.")
                .paragraph("Sigma has a smaller library than Sigma#. Its classes live in the namespace Standard, with"
                        + " Console, File, Math and Convert among them. The chapter [](jsc:sigma_library) lists what"
                        + " each one does.")
                .table("What Sigma leaves out", "Missing in Sigma", "What the compiler suggests you write")
                .row(GuideCell.words("Interfaces"),
                        GuideCell.words("A class whose methods are virtual, and build on it instead."))
                .row(GuideCell.words("Records"), GuideCell.words("A struct with the same fields."))
                .row(GuideCell.words("Abstract classes"),
                        GuideCell.words("A class with virtual methods that do nothing, and build on that."))
                .row(GuideCell.words("Abstract methods"),
                        GuideCell.words("Give it a body that does nothing, and write it virtual."))
                .row(GuideCell.words("Delegates"),
                        GuideCell.words("Call the method by name, or keep the object and call it through that."))
                .row(GuideCell.words("Events"),
                        GuideCell.words("Keep the object that wants telling, and call a method on it."))
                .row(GuideCell.words("Properties"), GuideCell.words("A field, or a method that gives the value back."))
                .row(GuideCell.words("Generics, such as List<T>"),
                        GuideCell.words("An array, or a class that holds exactly what you need."))
                .row(GuideCell.words("List"), GuideCell.words("An array, and a count of your own beside it."))
                .row(GuideCell.words("Map"), GuideCell.words("Two arrays, or one array searched for the key."))
                .ifSomethingGoesWrong(
                        "The build stops with \"IA-16 runs Σ only; write it in Σ, in a Σ project\".",
                        "Write the program in Sigma, in a Sigma project. Or set its platform target to x86 or x86-64.",
                        "A listing is refused: \"built for x86; this machine is IA-16\".",
                        "The computer is a Vintage one, which runs only IA-16 listings. Build with scc, which"
                                + " builds for IA-16, or run the listing on a Legacy computer or later.")
                .seeAlso("jsc:sigma_language")
                .register();
    }

    private static void installing(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_installing", section).titled("Installing the compilers and the runtime")
                .paragraph("Sigma and Sigma# do not come with the computer. You install three packages: the"
                        + " Σ Compiler (scc), the Σ# Compiler (sgsc) and the Sigma Runtime (sigma). A"
                        + " package is installed by its name, the way every program is ([](jsc:programs)).")
                .define("Σ Compiler", "The package scc, which turns your Sigma source into a listing.")
                .define("Σ# Compiler", "The package sgsc, which turns your Sigma# source into a listing.")
                .define("Sigma Runtime", "The package sigma, which runs your listings on the computer.")
                .paragraph("The Sigma compiler is the smallest of the three. It is the only one a Vintage computer"
                        + " can hold, because the Sigma# compiler and the runtime need a Legacy computer or later.")
                .table("The three packages", "Package", "Name", "Earliest era", "Disk and memory")
                .row(GuideCell.code("scc"), GuideCell.words("Σ Compiler"), GuideCell.words("Vintage"),
                        GuideCell.words("4 MB of disk, 2 MB of memory while it runs"))
                .row(GuideCell.code("sgsc"), GuideCell.words("Σ# Compiler"), GuideCell.words("Legacy"),
                        GuideCell.words("8 MB of disk, 16 MB of memory while it runs"))
                .row(GuideCell.code("sigma"), GuideCell.words("Sigma Runtime"), GuideCell.words("Legacy"),
                        GuideCell.words("12 MB of disk, 24 MB of memory while it runs"))
                .paragraph("On Frames, the edition matters too. Frames 95 can install the Sigma compiler only: the"
                        + " Sigma# compiler and the runtime need Frames XP or later.")
                .paragraph("The packages come from the Mirror, the network's package repository. It is a service on a"
                        + " Standard Mainframe, so your computer must be on a network whose Mainframe runs the"
                        + " Mirror ([](jsc:mirror)).")
                .table("Installing from the Mirror", "System", "Command to type")
                .row(GuideCell.words("Frames"), GuideCell.code("pckmgr install <name>"))
                .row(GuideCell.words("Debian and Ubuntu"), GuideCell.code("apt install <name>"))
                .row(GuideCell.words("Fedora"), GuideCell.code("dnf install <name>"))
                .row(GuideCell.words("Arch"), GuideCell.code("pacman -S <name>"))
                .row(GuideCell.words("Gentoo, which builds each package from source"),
                        GuideCell.code("emerge <name>"))
                .row(GuideCell.words("FreeBSD"), GuideCell.code("pkg install <name>"))
                .paragraph("The name is the package's own name, such as scc or sgsc. On MC-DOS, MC-NET and UNIX System"
                        + " V, programs install from install media only, not from the Mirror ([](jsc:programs)).")
                .subheading("Installing on Frames")
                .steps("Open the Command Prompt.", "Type pckmgr install scc and press [[Enter]].",
                        "Type pckmgr install sgsc and press [[Enter]].",
                        "Type pckmgr install sigma and press [[Enter]].")
                .paragraph("Once a package is installed, its command appears at the prompt. The prompt is the line"
                        + " where you type a command, and the system answers there. Each command word is a verb: a"
                        + " word the system knows, such as install or run.")
                .define("prompt", "The line where you type a command, and where the system answers it.")
                .define("verb", "A command word the system knows, such as install or run.")
                .paragraph("Until a package is installed, its verbs do not exist at the prompt. The runtime adds the"
                        + " sigma verb, and the packaging tool sgpack comes with it ([](jsc:sigma_sharing)).")
                .ifSomethingGoesWrong(
                        "\"sgsc needs Legacy hardware or later\" appears.",
                        "Install the Sigma compiler, scc, on this Vintage computer. Or use a Legacy computer or later.",
                        "\"unable to locate package sgsc\" appears.",
                        "The Mirror does not offer the package to this computer. Check that a Mainframe runs the"
                                + " Mirror. On Frames, search for the name with pckmgr search.",
                        "\"could not resolve mirror:// - connect this computer to a network whose Mainframe runs the"
                                + " Mirror service\" appears.",
                        "Connect this computer to a network whose Mainframe runs the Mirror service.",
                        "\"this system installs programs from install media, not a package manager\" appears.",
                        "Use Frames, Linux or FreeBSD for this package, since they have a package manager.",
                        "\"unmet requirements (hardware or free disk space)\" appears.",
                        "The computer lacks the era or the free disk space the package needs. Free some disk space,"
                                + " or use a computer of a newer era.",
                        "\"this verb needs a package name\" appears after pckmgr install.",
                        "Type the name of the package after install, for example pckmgr install scc.")
                .seeAlso("jsc:programs", "jsc:mirror")
                .register();
    }

    private static void firstProgram(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_first_program", section).titled("Your first program")
                .paragraph("A program starts in one place, and the compiler must find that place. The simplest"
                        + " program prints one line and ends. On Frames, Virtual Studio can make a starter project for"
                        + " it with its wizard, and then one key runs it ([](jsc:virtual_studio)).")
                .paragraph("The wizard offers four kinds of project, and each comes in both languages. You pick the"
                        + " kind and the language, then give the project a name.")
                .table("The four kinds of project", "Kind", "What it is")
                .row(GuideCell.words("Console App"),
                        GuideCell.words("A program with a Main that runs at the terminal it was started from, prints"
                                + " as it goes, and is gone when it returns."))
                .row(GuideCell.words("Script"),
                        GuideCell.words("A program that stays up. It is set up once, it is called again and again"
                                + " while it runs, and it is told when it is stopped."))
                .row(GuideCell.words("Class Library"),
                        GuideCell.words("Classes that other projects share. It has no start of its own, and it"
                                + " compiles into the projects that use it."))
                .row(GuideCell.words("Empty Project"), GuideCell.words("A project with no files in it, for starting"
                        + " from nothing."))
                .paragraph("The name may use only letters, digits and underscores, and it must not start with a digit."
                        + " The name becomes the [namespace](jsc:sigma_namespaces_using), the class name, and the"
                        + " text the program prints.")
                .paragraph("The wizard writes the project file, NAME.sgsproj for Sigma# (NAME.sgproj for Sigma), and"
                        + " the source file, NAME.sgs for Sigma# (NAME.sg for Sigma). When you build, the listing goes"
                        + " to build/NAME.asm.")
                .define("project file", "The text file in a project folder that names the project and lists its"
                        + " source files and settings.")
                .paragraph("Here is the starter code of a Sigma# console app, in a project called Hello. The name"
                        + " Hello is used for the namespace, for the class, and for the text the program prints.")
                .listing("Hello.sgs",
                        "using System.IO.*;",
                        "",
                        "namespace Hello;",
                        "",
                        "class Hello {",
                        "    static void Main() {",
                        "        Console.PrintLine(\"Hello from Hello\");",
                        "    }",
                        "}")
                .paragraph("The same program in Sigma looks like this. Sigma prints with printf, and you end the"
                        + " text with a line break yourself, written as a backslash and an n.")
                .listing("Hello.sg",
                        "using Standard.*;",
                        "",
                        "namespace Hello;",
                        "",
                        "class Hello {",
                        "    static void Main() {",
                        "        printf(\"Hello from Hello" + "\\" + "n\");",
                        "    }",
                        "}")
                .paragraph("A program needs exactly one place to start: a class that implements IScript, or a class"
                        + " with a static Main. A Main is static, returns void or int, and takes nothing or the"
                        + " arguments as a string array. Each using line brings a library into the program, so its"
                        + " names can be written short.")
                .define("Main", "The method where a console program starts.")
                .define("IScript", "The interface a script implements, with the methods the runtime calls on it:"
                        + " OnInit, OnTick and OnDestroy.")
                .define("using", "A line at the top of a source file that brings a library into the program.")
                .paragraph("Returning an int from Main, or taking the arguments, needs Sigma# version 2. Each project"
                        + " holds its own version ([](jsc:sigma_projects)).")
                .subheading("Your first run in Virtual Studio")
                .steps("Open the New Project wizard with [[Ctrl]]+[[Shift]]+[[N]] in Virtual Studio.",
                        "Choose the Console App offer for Sigma#, and type a name, such as Hello.",
                        "Press [[F5]] to build the project and run it.",
                        "Read the line the program prints in the terminal.")
                .paragraph("The program prints Hello from Hello, and it is gone when Main returns. Compiling it on the"
                        + " command line is shown in [](jsc:sigma_compile).")
                .ifSomethingGoesWrong(
                        "\"a program needs exactly one place to start\" appears, with the count of starts it found.",
                        "Keep one class with a static Main, or one class that implements IScript, and remove the"
                                + " others.",
                        "The build prints an error that names a file and a place in it, such as hello.sgs(3,5): line"
                                + " 3, fifth character.",
                        "Read the error and fix the line it names, then build again ([](jsc:sigma_errors)).")
                .seeAlso("jsc:sigma_compile", "jsc:sigma_projects")
                .register();
    }

    private static void compiling(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_compile", section).titled("Compiling with sgsc and scc")
                .paragraph("Compiling turns your source file into a listing that a computer can run. You compile on"
                        + " the command line with the compiler of your language: sgsc for Sigma#, and scc for Sigma.")
                .table("The two compilers", "Command", "Language", "Instruction set by default")
                .row(GuideCell.code("sgsc"), GuideCell.words("Sigma#"), GuideCell.code("x86"))
                .row(GuideCell.code("scc"), GuideCell.words("Sigma"), GuideCell.code("IA-16"))
                .paragraph("Both commands take the same options. Type the command with no file, and it shows its"
                        + " usage line.")
                .session("jsc:guide/terminal/bash", "Ubuntu")
                .typed("player@pc:~$ ", "sgsc")
                .answer("usage: sgsc <file.sgs> [more.sgs ...] [-o <out.asm>] [--arch <isa>] [--lang <version>]")
                .table("The options of sgsc and scc", "Option", "What it does")
                .row(GuideCell.code("-o <file>"),
                        GuideCell.words("Names the listing it writes; without it, the listing takes the first source"
                                + " file's name with .asm in place of its ending."))
                .row(GuideCell.code("--arch <isa>"),
                        GuideCell.words("Builds for an instruction set: IA-16, x86 or x86-64, in any letter case;"
                                + " without it, the language's default applies."))
                .row(GuideCell.code("--lang <version>"),
                        GuideCell.words("Holds the build to a language version, 1 or 2; without it, the newest"
                                + " version the installed compiler knows is used."))
                .paragraph("The full page of each compiler follows.")
                .command(CommandGuidePages.id(Platform.LINUX, "sgsc"))
                .command(CommandGuidePages.id(Platform.LINUX, "scc"))
                .paragraph("A successful compile prints the compiler's name and version first, then what it wrote."
                        + " Here it compiles a file called hello.sgs:")
                .session("jsc:guide/terminal/bash", "Ubuntu")
                .typed("player@pc:~$ ", "sgsc hello.sgs")
                .answer("Σ# Compiler 2.0", "sgsc: wrote hello.asm", "run it with: sigma run hello.asm")
                .paragraph("Compiling does not run the program. The last line says how to run it, with the sigma"
                        + " command ([](jsc:sigma_run)).")
                .table("How an error reads", "Part", "What it means")
                .row(GuideCell.words("hello.sgs"), GuideCell.words("The source file that has the error."))
                .row(GuideCell.words("(3,5)"), GuideCell.words("The line and the column where the problem is."))
                .row(GuideCell.words("error CODE: message"),
                        GuideCell.words("The kind of message, a code that names the problem, and its words."))
                .paragraph("A warning looks the same, but it says warning instead of error, and it is shown in dim"
                        + " text. When the build fails, the compiler prints the number of errors last, such as"
                        + " \"sgsc: 1 error, nothing was written\". You get no listing from a failed build.")
                .ifSomethingGoesWrong(
                        "\"sgsc: 1 error, nothing was written\" appears, or the same with more errors.",
                        "Read each error above it, fix the line it names, and compile again.",
                        "The compiler prints the command's name and a read error when a source file is missing.",
                        "Check the file's name, and that the file is in the folder you named.",
                        "The usage line appears, with the options listed.",
                        "Give at least one source file.",
                        "\"IA-16 runs Σ only; write it in Σ and build it with scc\" appears.",
                        "Build the Sigma program with scc, or choose x86 or x86-64 with --arch.",
                        "\"no instruction set is called\" appears, followed by the names there are.",
                        "Use one of the names IA-16, x86 or x86-64.",
                        "\"'3' is not a version of Σ#; there are 1 to 2\" appears.",
                        "Use 1 or 2 after --lang.",
                        "\"this compiler knows Σ# 1 at most; Σ# 2 needs a newer one\" appears.",
                        "Update the compiler package, or use a lower version after --lang.")
                .seeAlso("jsc:sigma_errors", "jsc:sigma_run")
                .register();
    }

    private static void running(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_run", section).titled("Running programs")
                .paragraph("A listing is ready to run, and the sigma command runs it. The runtime starts the program,"
                        + " keeps it in its list of running programs, and lets you stop it again.")
                .table("The sigma command", "Command", "What it does")
                .row(GuideCell.code("sigma run <file> [--heap <n>M] [arguments]"),
                        GuideCell.words("Starts a program. The word start does the same."))
                .row(GuideCell.code("sigma stop <id>"),
                        GuideCell.words("Stops a running program. The word kill does the same."))
                .row(GuideCell.code("sigma ps"),
                        GuideCell.words("Lists the running programs. The word list does the same, and so does sigma"
                                + " alone."))
                .subheading("Running a program")
                .steps("Compile the source file, or have a listing ready.",
                        "Type sigma run hello.asm and press [[Enter]].",
                        "Type sigma ps to see the program in the list.",
                        "Type sigma stop 3 with the id from the list to stop it.")
                .table("The files sigma run takes", "File ending", "What happens")
                .row(GuideCell.code(".asm"), GuideCell.words("A listing. The runtime starts it."))
                .row(GuideCell.code(".sgs or .sg"),
                        GuideCell.words("A source file. The runtime compiles it first, then runs it."))
                .row(GuideCell.words("Any other ending"),
                        GuideCell.words("Refused: \"hello.txt is not a Σ# program\"."))
                .paragraph("When you run a source file, the runtime compiles it first. It uses the newest language"
                        + " version and the default instruction set of its language. The settings of a project do not"
                        + " apply to that compile.")
                .paragraph("Before a listing starts, the runtime makes three checks. It checks that"
                        + " the listing fits the computer's instruction set. It checks the listing's assembly version,"
                        + " which is refused when it is too old or too new. And it checks that the memory of the"
                        + " program fits in the free memory of the computer.")
                .paragraph("A running program gets its own memory, called the heap. The heap is measured in megabytes,"
                        + " and you set it with --heap.")
                .define("heap", "The memory a running program is given, measured in megabytes.")
                .table("The heap", "Setting", "Value")
                .row(GuideCell.words("Default"), GuideCell.amount(1, "MB"))
                .row(GuideCell.words("Largest possible"),
                        GuideCell.words("64 MB. A larger request is cut to 64 MB, with no message."))
                .row(GuideCell.words("Writing it"),
                        GuideCell.words("--heap 16M, --heap 16MB and --heap 16 all mean 16 megabytes."))
                .paragraph("A value that is not a number counts as zero, and zero gives the default. A --heap that is"
                        + " the last word is not a setting: it goes to the program as an argument.")
                .paragraph("Everything after the file, except --heap and its number, goes to the program. A Sigma"
                        + " program reads these words with Program.Args ([](jsc:programs_and_processes)). A Sigma#"
                        + " program can take them as a string array in Main, if its project is version 2.")
                .table("What sigma ps shows", "Column", "What it shows")
                .row(GuideCell.words("id"), GuideCell.words("The number that sigma stop takes."))
                .row(GuideCell.words("name"),
                        GuideCell.words("The program's name, cut to 20 characters, with a ~ when it is cut."))
                .row(GuideCell.words("state"),
                        GuideCell.words("running, parked (it waits for something outside it), finished, halted"
                                + " (it stopped on a mistake), or input (it waits for the keyboard)."))
                .row(GuideCell.words("memory"),
                        GuideCell.words("The memory the program holds, out of the heap it was given, in KB (or in"
                                + " bytes when it is under 1,024)."))
                .paragraph("With no program running, the list says so. Here is that answer:")
                .session("jsc:guide/terminal/bash", "Ubuntu")
                .typed("player@pc:~$ ", "sigma ps")
                .answer("no Σ# programs are running")
                .paragraph("Stopping a program answers stopped with its id, such as \"stopped 3\". An id that is not"
                        + " running answers \"sigma: nothing is running as 3\".")
                .paragraph("The full page of the sigma command follows.")
                .command(CommandGuidePages.id(Platform.LINUX, "sigma"))
                .ifSomethingGoesWrong(
                        "\"hello.asm: 16 MB will not fit in 8 MB of free memory\" appears.",
                        "Lower the --heap value, stop another program with sigma stop, or free memory on the computer.",
                        "\"built for x86; this machine is IA-16\" appears.",
                        "Build with scc, or run the listing on a Legacy computer or later.",
                        "A message names the listing's assembly version and the version this runtime reads.",
                        "Compile the source file again, with the compiler that is installed.",
                        "\"hello.txt is not a Σ# program\" appears.",
                        "Run a file that ends in .asm, .sgs or .sg.",
                        "\"hello.sgs does not compile\" appears.",
                        "Compile the source with sgsc or scc, and read the errors it names.")
                .seeAlso("jsc:sigma_running", "jsc:sigma_compile")
                .register();
    }

    private static void editors(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_editors", section).titled("The editors")
                .paragraph("You write a Sigma program in a text editor. Five programs open Sigma code: the Editor, vim,"
                        + " Virtual Studio, Virtual Studio Code and Exposure. They differ in where they run and how"
                        + " much they help you.")
                .table("Where each editor runs", "Program", "Where it runs", "Disk and memory")
                .row(GuideCell.words("Editor"),
                        GuideCell.words("The desktops of Frames, Linux, FreeBSD and UNIX"),
                        GuideCell.words("Comes preinstalled"))
                .row(GuideCell.words("vim"), GuideCell.words("The terminal of every system"),
                        GuideCell.words("8 MB of disk, 4 MB of memory"))
                .row(GuideCell.words("Virtual Studio"), GuideCell.words("Frames only"),
                        GuideCell.words("512 MB of disk, 256 MB of memory"))
                .row(GuideCell.words("Virtual Studio Code"), GuideCell.words("Frames, Linux and FreeBSD"),
                        GuideCell.words("128 MB of disk, 48 MB of memory"))
                .row(GuideCell.words("Exposure"), GuideCell.words("Frames, Linux and FreeBSD"),
                        GuideCell.words("192 MB of disk, 64 MB of memory"))
                .paragraph("vim takes over the terminal it was started from, so it works where there is no desktop."
                        + " Virtual Studio, Virtual Studio Code and Exposure are installed like any other program"
                        + " ([](jsc:programs)). All three need a Legacy computer or later. On Frames, they also need"
                        + " Frames XP or later.")
                .paragraph("Virtual Studio is the whole Sigma# workshop in one window, and it runs on Frames only. It"
                        + " works with projects and solutions, and you can read about it in [](jsc:virtual_studio).")
                .define("solution", "A folder that groups several projects, with one file that lists them.")
                .table("Keys of Virtual Studio", "Key", "What it does")
                .row(GuideCell.code("F5"),
                        GuideCell.words("Start: builds and runs the open file, or the startup project when a solution"
                                + " is open."))
                .row(GuideCell.code("Ctrl+Shift+B"),
                        GuideCell.words("Builds each project of the solution that makes a listing, in turn, and shows"
                                + " the output in the Output tab; with no solution, builds the open file."))
                .row(GuideCell.code("Ctrl+Shift+N"), GuideCell.words("Opens the New Project wizard."))
                .row(GuideCell.code("Ctrl+S"), GuideCell.words("Saves the file."))
                .row(GuideCell.code("Ctrl+F"), GuideCell.words("Finds text in the file."))
                .row(GuideCell.code("Ctrl+G"), GuideCell.words("Goes to a line."))
                .row(GuideCell.code("Ctrl+W"), GuideCell.words("Closes the tab."))
                .paragraph("Virtual Studio Code is a light editor with the terminal built in, so you can write,"
                        + " compile and run without leaving it. It runs on Frames, Linux and FreeBSD.")
                .define("Virtual Studio Code", "A light editor with a terminal built in, where you write, compile"
                        + " and run a program in one window.")
                .table("Keys of Virtual Studio Code", "Key", "What it does")
                .row(GuideCell.code("F5"),
                        GuideCell.words("Run File: builds the file, then types sigma run at the terminal, with the"
                                + " listing in the build folder."))
                .row(GuideCell.code("Ctrl+Shift+B"),
                        GuideCell.words("Build File: saves the file, then types the compiler's command, which writes"
                                + " its listing to the build folder beside the file."))
                .row(GuideCell.code("Ctrl+P"), GuideCell.words("Goes to a file by its name."))
                .row(GuideCell.code("Ctrl+S"), GuideCell.words("Saves the file."))
                .paragraph("Exposure compiles every program in a folder at once, so a change shows what else it broke."
                        + " It offers nothing while you type, and that is its trade.")
                .define("Exposure", "An editor that compiles every program in a folder at once, so you see what"
                        + " else your change broke.")
                .table("Keys of Exposure", "Key", "What it does")
                .row(GuideCell.code("F5"),
                        GuideCell.words("Surveys the folder: it asks for every program in it at once and lists the"
                                + " problems."))
                .row(GuideCell.code("F6"),
                        GuideCell.words("Saves, compiles and runs the file at the terminal, with the listing written"
                                + " beside the source file."))
                .paragraph("The window of Exposure shows PROBLEMS, a count of the problems the survey found, and WHAT"
                        + " THE COMPILER SAID, the compiler's own words about the problem. The Rebuild all button"
                        + " compiles everything again.")
                .table("Which editor opens which file", "File", "Opened first by")
                .row(GuideCell.code(".sgs and .sg"),
                        GuideCell.words("Virtual Studio Code, then Virtual Studio, Exposure and the Editor"))
                .row(GuideCell.code(".asm"),
                        GuideCell.words("The Sigma runtime, which runs it, then Virtual Studio Code and the others"))
                .row(GuideCell.code(".sgsproj, .sgproj and .sln"), GuideCell.words("Virtual Studio, then the Editor"))
                .row(GuideCell.code(".txt, .cfg, .csv, .cmd and .log"), GuideCell.words("The Editor"))
                .ifSomethingGoesWrong(
                        "Virtual Studio Code says the file is not a program to build.",
                        "Open a file that ends in .sg or .sgs, then press [[Ctrl]]+[[Shift]]+[[B]] or [[F5]].",
                        "\"No project to start: none builds a listing\" appears in Virtual Studio.",
                        "Choose a startup project in the solution, one that builds a listing.",
                        "Exposure says the file is not a program to run.",
                        "Open a Sigma source file, which ends in .sg or .sgs.")
                .seeAlso("jsc:sigma_projects", "jsc:sigma_compile")
                .register();
    }

    private static void projects(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_projects", section).titled("Projects and versions")
                .paragraph("A project keeps the files of one program together, with the settings that build it. When"
                        + " you make a project, its settings live in the project file, a text file in the project"
                        + " folder. The file of Sigma# is named NAME.sgsproj, and the file of Sigma is named"
                        + " NAME.sgproj.")
                .paragraph("Each line of the file holds one setting, as a key, a colon and a value. A line written in"
                        + " another way is skipped.")
                .table("What a project file holds", "Key", "What it holds")
                .row(GuideCell.code("name"), GuideCell.words("The project's name."))
                .row(GuideCell.code("kind"), GuideCell.words("One of console, script, library or empty."))
                .row(GuideCell.code("language"),
                        GuideCell.words("jsc:sigma for Sigma, or jsc:sigma_sharp for Sigma#."))
                .row(GuideCell.code("source"),
                        GuideCell.words("One line for each source file, relative to the project folder."))
                .row(GuideCell.code("reference"), GuideCell.words("One line for each library project it uses."))
                .row(GuideCell.code("entry"),
                        GuideCell.words("The listing it builds, such as build/NAME.asm. Empty for a library."))
                .row(GuideCell.code("platform"),
                        GuideCell.words("The instruction set it builds for, such as jsc:x86."))
                .row(GuideCell.code("langversion"),
                        GuideCell.words("The language version it is held to. Written only when set. Without it, the"
                                + " project follows the installed compiler."))
                .paragraph("The kind tells the compiler what to build. A console program runs and returns. A script"
                        + " stays up. A library holds shared classes and has no start of its own. An empty project"
                        + " has nothing in it yet.")
                .paragraph("A solution keeps its projects together, one folder for each project. Its file lists each"
                        + " project by its path, and it names the startup project, the one that Start runs. When the"
                        + " startup project is empty, Start uses the first project that can run.")
                .paragraph("A project builds for one instruction set, which you choose in the popup named Platform"
                        + " target.")
                .define("langversion", "The language version a project is held to. Version 1 is the first language,"
                        + " and version 2 is the current one.")
                .define("held", "The version a build really uses: the lower of the project's langversion and the"
                        + " version of the installed compiler.")
                .paragraph("Sigma and Sigma# share their version numbers. Sigma 2 is the subset of Sigma# 2, so a"
                        + " project has a single number for both.")
                .subheading("Setting the language version")
                .steps("Open the Properties popup of the project in Virtual Studio.",
                        "Find the row Language version.",
                        "Press the Default button to follow the installed compiler, or press a numbered button to"
                                + " hold the project to that version.")
                .paragraph("Under the buttons, Virtual Studio explains the choice. \"Default follows the installed"
                        + " compiler (sgsc 2.0).\" is the first hint. \"A lower version refuses what came after it:\""
                        + " is the second. After you choose a number, the popup says \"Written to the project as"
                        + " langversion\" with that number.")
                .paragraph("Choosing Default removes the langversion line from the project file. Your build then uses"
                        + " the installed compiler's version. With a number, your build uses the lower of that number"
                        + " and the installed compiler's version.")
                .paragraph("On the command line the project file is not read. The option --lang sets the version for"
                        + " one compile ([](jsc:sigma_compile)). The sigma run command on a source file uses the newest"
                        + " version, not the project's.")
                .paragraph("Version 1 is the first release. Version 2 added the types for the network, the machines and"
                        + " the windows, such as Bus, CraftInterface, Table and Chart. It added members to older types,"
                        + " such as Console.Read and File.Move. It also lets Main return an int and take the arguments,"
                        + " and the smaller Sigma gained Random.")
                .ifSomethingGoesWrong(
                        "\"'puts' needs Σ# 2; this project is Σ# 1\" appears.",
                        "Set the project's Language version to Default, or to version 2.",
                        "\"this compiler knows Σ# 1 at most; Σ# 2 needs a newer one\" appears.",
                        "Update the compiler package, or use a lower version after --lang.",
                        "\"'3' is not a version of Σ#; there are 1 to 2\" appears.",
                        "Use 1 or 2 after --lang.")
                .seeAlso("jsc:sigma_sharing", "jsc:sigma_compile")
                .register();
    }
}
