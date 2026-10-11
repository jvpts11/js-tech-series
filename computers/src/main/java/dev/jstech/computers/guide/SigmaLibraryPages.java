/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Pages of the library chapter of the Book of Sigma: the names a program can call, grouped by what they touch
 * (the basics, programs, the network, devices and windows).
 */
final class SigmaLibraryPages {

    private SigmaLibraryPages() {
    }

    /** Declares the sections and entries of the library chapter of the Book of Sigma, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        libraryBasics(guide, chapter);
        libraryPrograms(guide, chapter);
        libraryNetwork(guide, chapter);
        libraryMachine(guide, chapter);
        libraryWindows(guide, chapter);
    }

    private static void libraryBasics(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("library_basics").titled("The basics")
                .in(chapter).register();
        guide.page("library_overview", section).titled("The library, and the two languages")
                .paragraph("A program reaches the machine through the library. The library is a set of named types,"
                        + " and each type has members you call, such as the terminal, files, the clock and the"
                        + " network.")
                .paragraph("Sigma and Sigma# both use the library, but they do not reach the same part of it. Sigma"
                        + " is the smaller language, and it reaches only the library's Standard namespace. Sigma# is"
                        + " the whole language, and it reaches every System namespace, such as System.IO and"
                        + " System.Sound.")
                .paragraph("Every name in Standard is a type Sigma# also has, under the namespace it has always"
                        + " had. A using line, explained in [](jsc:sigma_namespaces_using), brings a namespace in.")
                .subheading("Bringing names into a program")
                .steps("Write using Standard.*; at the top of the file, to bring in everything in Standard.",
                        "Below the using line, write namespace, then a name of your own and a semicolon, such as"
                                + " namespace MyProgram;.",
                        "Write the entry point of your kind of program, as the table below shows.")
                .table("Where a program starts", "Kind of program", "What you write")
                .row(GuideCell.words("A console program"), GuideCell.code("static void Main()"))
                .row(GuideCell.words("A script"), GuideCell.words("A class that extends Script, with OnInit,"
                        + " OnTick or OnDestroy written in it."))
                .paragraph("Script is a base class in Standard. Its three methods do nothing until your class"
                        + " writes its own.")
                .table("The two languages", "Language", "Names it reaches", "Compiler", "Source file")
                .row(GuideCell.words("Sigma"), GuideCell.words("Standard"), GuideCell.code("scc"),
                        GuideCell.code(".sg"))
                .row(GuideCell.words("Sigma#"), GuideCell.words("Every System namespace, such as System.IO"),
                        GuideCell.code("sgsc"), GuideCell.code(".sgs"))
                .paragraph("A version is the level of the language a build is held to. There are two versions, 1"
                        + " and 2, and version 2 is the newest. A build that asks for no version is held to the"
                        + " newest one.")
                .paragraph("This book marks each name that came in version 2. A version 1 build refuses those"
                        + " names, so a program that uses them needs the newest version.")
                .subheading("Names Sigma refuses")
                .paragraph("Sigma refuses some names that Sigma# has. The compiler names the replacement it expects"
                        + " you to use.")
                .table("Names Sigma refuses", "Name", "What to use instead")
                .row(GuideCell.code("List"), GuideCell.words("An array, and a count of your own beside it."))
                .row(GuideCell.code("Map"), GuideCell.words("Two arrays, or one array you search for the key."))
                .row(GuideCell.code("Thread"), GuideCell.words("Nothing: these machines run one thing at a time."))
                .paragraph("A member that Sigma does not offer is refused too. The compiler lists the members the"
                        + " type does have, in alphabetical order, so you can pick one of them.")
                .define("Namespace", "A group of types with a dotted name, such as System.IO. A using line brings it"
                        + " in, so its types can be written by bare name.")
                .define("Library", "The set of types and their members that a program can call, such as Console,"
                        + " File and Network.")
                .define("Member", "A method, value or event that belongs to a type.")
                .define("Version", "The level of the language a build is held to: 1 is the first level, and 2 is"
                        + " the newest.")
                .define("Subset", "The smaller language, Sigma, which offers fewer names than Sigma#.")
                .ifSomethingGoesWrong(
                        "The compiler says a name is in a namespace you did not bring in, and it asks for a using"
                                + " line.",
                        "Add the using line it names at the top of the file: using System.IO.*; for everything in"
                                + " the namespace, or using System.IO.<name>; for one type.",
                        "The compiler says a name is a namespace and refuses it alone.",
                        "Write .* after the namespace to bring in everything in it.",
                        "The compiler says Sigma has no System library, and that everything Sigma has is in"
                                + " Standard.",
                        "Write using Standard.*; and use the names it holds, or write the program in Sigma#.",
                        "The compiler refuses List, Map or Thread.",
                        "Use the replacement in the table of names Sigma refuses, or write the program in Sigma#, which"
                                + " has all of them. Threads have their own page ([](jsc:threads_and_locks)).",
                        "The compiler refuses a member that Sigma does not offer.",
                        "Use one of the members the compiler lists in its message.",
                        "A version 1 build refuses a name that came in version 2.",
                        "Use the newest version of the compiler, or write the program without that name.")
                .seeAlso("jsc:sigma_first_program", "jsc:sigma_reading_an_error")
                .register();

        guide.page("standard_functions", section).titled("The old names: printf, rand and the rest")
                .paragraph("Programs written in the old style call bare names such as printf, rand and strlen. Sigma"
                        + " and Sigma# still accept them. Each bare name is a call on a library member, so puts(s)"
                        + " does the same as Console.PrintLine(s).")
                .paragraph("printf is version 1. Every other name on this page is version 2, so a version 1 build"
                        + " refuses it. Each table row names the member the old name calls, so you can read that"
                        + " member's entry in this book.")
                .table("Printing and reading", "Old name", "Library call")
                .row(GuideCell.code("printf(format, ...)"), GuideCell.words("Console.Print, filled from the values"
                        + " after the format."))
                .row(GuideCell.code("sprintf(format, ...)"), GuideCell.words("Gives the text back instead of"
                        + " printing it."))
                .row(GuideCell.code("puts(text)"), GuideCell.words("Console.PrintLine: prints and ends the line."))
                .row(GuideCell.code("putchar(char)"), GuideCell.words("Console.Print for one character."))
                .row(GuideCell.code("gets()"), GuideCell.words("Console.ReadLine: waits for a line."))
                .row(GuideCell.code("getchar()"), GuideCell.words("Console.Read: the next character as a number."))
                .row(GuideCell.code("scanf(format, out ...)"), GuideCell.words("Console.Scan, one value per hole."))
                .table("Numbers and text", "Old name", "Library call")
                .row(GuideCell.code("exit(status)"), GuideCell.words("Program.Exit: ends the program with the"
                        + " status."))
                .row(GuideCell.code("abs, min, max"), GuideCell.words("Math.Abs, Math.Min and Math.Max."))
                .row(GuideCell.code("floor, sqrt, pow"), GuideCell.words("Math.Floor, Math.Sqrt and Math.Pow."))
                .row(GuideCell.code("atoi(text)"), GuideCell.words("Convert.ToInt(text, 0): 0 when the text is not"
                        + " a number."))
                .row(GuideCell.code("atof(text)"), GuideCell.words("Convert.ToDouble."))
                .row(GuideCell.code("itoa(value, base)"), GuideCell.words("Convert.ToString(number, base)."))
                .row(GuideCell.code("srand(seed)"), GuideCell.words("Random.Seed: starts the sequence again."))
                .row(GuideCell.code("rand()"), GuideCell.words("Random.Next(32768): a number from 0 to 32767."))
                .row(GuideCell.code("strlen(text)"), GuideCell.words("The length of the text, as string.Length."))
                .row(GuideCell.code("strstr(text, part)"), GuideCell.words("The place of the part inside the text,"
                        + " as IndexOf."))
                .row(GuideCell.code("strcmp(a, b)"), GuideCell.words("string.Compare: the order of two texts."))
                .row(GuideCell.code("strcpy(out dest, src)"), GuideCell.words("Puts the text into dest."))
                .row(GuideCell.code("strcat(ref dest, src)"), GuideCell.words("Adds the text to the end of dest."))
                .row(GuideCell.code("toupper, tolower"), GuideCell.words("The upper or lower case of one character."))
                .row(GuideCell.code("isdigit, isalpha, isspace"), GuideCell.words("char.IsDigit, char.IsLetter and"
                        + " char.IsWhiteSpace."))
                .table("Files and the machine", "Old name", "Library call")
                .row(GuideCell.code("remove(path)"), GuideCell.words("File.Delete: gives true or false."))
                .row(GuideCell.code("rename(from, to)"), GuideCell.words("File.Move: gives true or false."))
                .row(GuideCell.code("fopen(path, mode)"), GuideCell.words("File.Open: gives a FILE, or null."))
                .row(GuideCell.code("fclose(file)"), GuideCell.words("FILE.Close()."))
                .row(GuideCell.code("fgets(out line, file)"), GuideCell.words("FILE.ReadLine."))
                .row(GuideCell.code("fputs(text, file)"), GuideCell.words("FILE.Write with the text."))
                .row(GuideCell.code("fprintf(file, format, ...)"), GuideCell.words("FILE.Write with the formatted"
                        + " text."))
                .row(GuideCell.code("fscanf(file, format, ...)"), GuideCell.words("FILE.Scan."))
                .row(GuideCell.code("fgetc(file)"), GuideCell.words("FILE.Read(), the next character as a number."))
                .row(GuideCell.code("fputc(char, file)"), GuideCell.words("FILE.Write with one character."))
                .row(GuideCell.code("feof(file)"), GuideCell.words("FILE.AtEnd."))
                .row(GuideCell.code("rewind(file)"), GuideCell.words("FILE.Seek(0): back to the start."))
                .row(GuideCell.code("fseek(file, place)"), GuideCell.words("FILE.Seek with the place."))
                .row(GuideCell.code("ftell(file)"), GuideCell.words("FILE.Position: the place now."))
                .row(GuideCell.code("bus(name)"), GuideCell.words("Bus.Named: the bus with that name."))
                .row(GuideCell.code("redstone(name)"), GuideCell.words("Redstone.Named: the Redstone Interface with"
                        + " that name."))
                .row(GuideCell.code("craftInterface(name)"), GuideCell.words("CraftInterface.Named."))
                .row(GuideCell.code("craftRouter(name)"), GuideCell.words("CraftRouter.Named."))
                .paragraph("The formats of printf, sprintf, scanf, fprintf and fscanf work the same way. A hole"
                        + " starts with a percent sign and a letter: %d for a whole number, %f for a decimal number,"
                        + " %s for text, %c for a character and %% for a percent sign.")
                .paragraph("The compiler knows %d, %i, %u, %x, %X, %o, %f, %e, %E, %s, %c and %%. A version 1 hole"
                        + " takes only the plain letters d, i, f, s and c, with nothing between the percent sign"
                        + " and the letter. Flags, widths and precisions came in version 2.")
                .define("Bare function", "An old-style name, such as printf or rand, that the library answers with a"
                        + " member you can also call by its full name.")
                .define("Hole", "A place in a format text, starting with a percent sign, that is filled with a"
                        + " value.")
                .define("FILE", "A file the program has opened, which it works on a piece at a time.")
                .define("Null", "No value at all: the program holds nothing, and a call that gives null answers with"
                        + " nothing.")
                .ifSomethingGoesWrong(
                        "The compiler says a hole is not one printf has.",
                        "Use one of the holes listed above, such as %d for a whole number or %s for text.",
                        "The compiler says a width or a precision cannot come from a value here.",
                        "Write the number into the format, as in %5d, instead of a star.",
                        "The compiler says the format must be written out.",
                        "Put the format between quotes in the call, not in a variable.",
                        "The program stops and says that the hole is not one this machine's"
                                + " printf can write.",
                        "Use a hole from the list above.")
                .seeAlso("jsc:sigma_printf_old_names", "jsc:sigma_methods_out_ref")
                .register();

        guide.page("sigma_console", section).titled("Console: reading and writing the terminal")
                .paragraph("The console is a program's line to the terminal: the screen the program is started in."
                        + " Whatever you print appears there, and whatever you read is a line the player typed"
                        + " there. Most programs start by printing a question and reading the answer.")
                .paragraph("Console calls cost nothing. Printing does not end the line, but PrintLine does. A"
                        + " read waits until a line is typed, and a program that waits is parked.")
                .table("Console members", "Member", "What it does", "Language")
                .row(GuideCell.code("Console.Print(text)"), GuideCell.words("Writes the text and leaves the line"
                        + " open."), GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("Console.Print(char)"), GuideCell.words("Writes one character."),
                        GuideCell.words("Sigma and Sigma#, version 2"))
                .row(GuideCell.code("Console.PrintLine(text)"), GuideCell.words("Writes the text and ends the line."),
                        GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("Console.Clear()"), GuideCell.words("Clears the lines the console holds."),
                        GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("Console.ReadLine()"), GuideCell.words("Waits for a line and gives it back as"
                        + " text."), GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("Console.HasLine()"), GuideCell.words("Yes or no: is a typed line waiting? It"
                        + " never waits."), GuideCell.words("Sigma# only"))
                .row(GuideCell.code("Console.Read()"), GuideCell.words("The next character as a number, the line"
                        + " break included."), GuideCell.words("Sigma and Sigma#, version 2"))
                .row(GuideCell.code("Console.Scan(out value)"), GuideCell.words("Reads a value of the variable's"
                        + " type, giving 1 when it works and 0 when the text is not one."),
                        GuideCell.words("Sigma and Sigma#, version 2"))
                .row(GuideCell.code("Console.ReadInt()"), GuideCell.words("Reads the whole line as a whole number."),
                        GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("Console.ReadLong()"), GuideCell.words("Reads the whole line as a long whole"
                        + " number."), GuideCell.words("Sigma# only"))
                .row(GuideCell.code("Console.ReadDouble()"), GuideCell.words("Reads the whole line as a decimal"
                        + " number."), GuideCell.words("Sigma# only"))
                .row(GuideCell.code("Console.ReadBool()"), GuideCell.words("Reads the whole line as true or false."),
                        GuideCell.words("Sigma and Sigma#"))
                .paragraph("After Read or Scan, the rest of the line waits for the next read. When Scan fails on a"
                        + " string variable, the variable becomes empty text.")
                .paragraph("ReadBool takes true, yes, y, on or 1 for true, and false, no, n, off or 0 for false. It"
                        + " ignores case and spaces, so you can type Yes or OFF.")
                .paragraph("The terminal keeps at most 16 typed lines waiting. Output is kept too: the last 200"
                        + " lines, 4,096 characters at most for each line, and 64,000 characters in all. Older lines"
                        + " go first, and a line that is cut ends with a marker.")
                .define("Terminal", "The screen a program is started in: the machine's prompt or a window.")
                .define("Parked", "A program waiting on something outside itself. A parked program spends no credits"
                        + " while it waits.")
                .ifSomethingGoesWrong(
                        "The program stops and says that the text is not a number.",
                        "Type a whole number. To keep going on bad input, read a line with ReadLine and convert it"
                                + " with Convert.ToInt(text, fallback).",
                        "The program stops and says that the text is not true or false.",
                        "ReadBool takes only true, yes, y, on or 1 for true, and false, no, n, off or 0 for false. Type"
                                + " one of those.",
                        "The program waits and nothing shows.",
                        "It waits for a line. Type one in the terminal the program is in front of.",
                        "Old lines of output are gone.",
                        "The console keeps only the last 200 lines. Print less, or write what you need to a file.",
                        "A line of output ends in a cut marker.",
                        "A line keeps 4,096 characters. Split long text into several lines.")
                .seeAlso("jsc:sigma_runtime_stops")
                .register();

        guide.page("files_and_file_handles", section).titled("Files: whole files and files you open")
                .paragraph("A file is a named piece of data on a disk. Its path is its full name: the drive letter"
                        + " that names the disk, then the folders, then the file's own name.")
                .paragraph("There are two ways to work on a file. File works on a whole file at once: it reads all"
                        + " of it, or writes all of it. File.Open gives you a FILE instead, which you read and write"
                        + " a piece at a time until you close it.")
                .subheading("Whole files")
                .table("File members", "Member", "What it does")
                .row(GuideCell.code("File.Exists(path)"), GuideCell.words("Yes when the file can be read."))
                .row(GuideCell.code("File.Read(path)"), GuideCell.words("The whole text of the file. Stops the program"
                        + " when the file is missing."))
                .row(GuideCell.code("File.TryRead(path, out text)"), GuideCell.words("True and the text when the file"
                        + " is read. False and empty text otherwise."))
                .row(GuideCell.code("File.Write(path, text)"), GuideCell.words("Replaces the file with the text. False"
                        + " when the write did not happen."))
                .row(GuideCell.code("File.Append(path, text)"), GuideCell.words("Adds the text to the end of the file."
                        + " False when the add did not happen."))
                .row(GuideCell.code("File.Delete(path)"), GuideCell.words("Removes the file. False when refused."))
                .row(GuideCell.code("File.MkDir(path)"), GuideCell.words("Makes a folder. False when refused."))
                .row(GuideCell.code("File.List(path)"), GuideCell.words("The names in a folder, as a list."))
                .row(GuideCell.code("File.Open(path, mode)"), GuideCell.words("Opens a file to work on it piece by"
                        + " piece: gives a FILE, or null. Version 2."))
                .row(GuideCell.code("File.Move(from, to)"), GuideCell.words("Renames the file. False when refused."
                        + " Version 2."))
                .paragraph("Exists, Read and TryRead only look at a file. Write, Append, Delete, MkDir and Move"
                        + " change the disk, and each of them answers true when the change happened.")
                .subheading("A missing file")
                .paragraph("File.Read stops the program when the file is missing or cannot be read. The console then"
                        + " shows the file's message, such as \"<path>: file not found\". Use File.TryRead when you"
                        + " want to ask first and carry on either way.")
                .subheading("Files you open: FILE")
                .paragraph("A FILE is a file the program has opened. It came in version 2, and all its members are"
                        + " version 2 too. The program works on its own copy of the text, and nothing reaches the disk"
                        + " until you call Close, or until the program ends with the file still open.")
                .table("FILE members", "Member", "What it does")
                .row(GuideCell.code("FILE.Close()"), GuideCell.words("Writes the text to the disk, if it changed, and"
                        + " closes the file."))
                .row(GuideCell.code("FILE.ReadLine(out line)"), GuideCell.words("Gives the next line. False at the end,"
                        + " with an empty line."))
                .row(GuideCell.code("FILE.Read()"), GuideCell.words("The next character as a number, or -1 at the"
                        + " end."))
                .row(GuideCell.code("FILE.Scan(out value)"), GuideCell.words("Reads one value of the variable's type."))
                .row(GuideCell.code("FILE.Write(text)"), GuideCell.words("Writes the text into the file."))
                .row(GuideCell.code("FILE.Write(char)"), GuideCell.words("Writes one character."))
                .row(GuideCell.code("FILE.Seek(place)"), GuideCell.words("Moves to a place, counted in characters from"
                        + " the start and kept between 0 and the length of the file."))
                .row(GuideCell.code("FILE.Position"), GuideCell.words("The place now, in characters from the start."))
                .row(GuideCell.code("FILE.AtEnd"), GuideCell.words("Yes when the place is at the end of the file."))
                .subheading("Modes")
                .paragraph("The mode says how a file is opened. The letters b and t are ignored, so rb means r.")
                .table("Modes of File.Open", "Mode", "What it does")
                .row(GuideCell.code("r"), GuideCell.words("Reads a file that exists. Gives null when it is missing."))
                .row(GuideCell.code("w"), GuideCell.words("Writes a file, and starts it empty."))
                .row(GuideCell.code("a"), GuideCell.words("Adds text at the end of the file."))
                .row(GuideCell.code("r+"), GuideCell.words("Reads and writes a file that exists."))
                .row(GuideCell.code("w+"), GuideCell.words("Writes a file that starts empty, and reads it too."))
                .row(GuideCell.code("a+"), GuideCell.words("Adds at the end, and reads too."))
                .warning("Opening a file with w starts it empty, so the text it held is replaced by what the program"
                        + " writes.")
                .define("Mode", "The way a file is opened: reading, writing, adding at the end, or both with a plus"
                        + " sign.")
                .ifSomethingGoesWrong(
                        "The program stops and says that the file was not found.",
                        "Check the path, or use File.TryRead to ask first without stopping.",
                        "Write or Append gives false, and the file's message says there is not enough free space"
                                + " on the disk.",
                        "Free some space on the disk, or write less text.",
                        "Write gives false, and the message says the file is read-only.",
                        "Choose another path. A file ending in .dat is read-only, and the message names the Network"
                                + " Interactor for items.",
                        "Write gives false, and the message says the file name is not valid for this filesystem.",
                        "Use a name that this drive accepts.",
                        "Delete gives false for a .dat file.",
                        "Use the Network Interactor, as the message says. .dat files cannot be deleted from here.",
                        "Move gives false, and the message says a file of that name is already there.",
                        "Choose a new name, or delete the file that is there first.",
                        "Move gives false: a file on another machine is copied, not moved.",
                        "Read the file with File.Read, write it with File.Write, then delete the original with"
                                + " File.Delete.",
                        "MkDir gives false: the drive has no folders.",
                        "Make the folder on a drive that supports folders.",
                        "Open gives null.",
                        "Check that the mode is one listed above, that the path is not blank, and that a file read"
                                + " in mode r exists. A program may also hold only so many files at once: 8 on a"
                                + " Vintage machine, 20 on a Legacy machine and 64 on every other era. Close a file"
                                + " you no longer need.",
                        "The program stops and says that the file was opened with \"r\", which does not write.",
                        "Open the file in a mode that writes, such as w, a or r+.",
                        "The program stops and says that the file was opened with \"w\", which does not read.",
                        "Open the file in a mode that reads, such as r, r+ or a+.",
                        "The program stops and says that the file is closed.",
                        "Open the file again with File.Open before you use it.")
                .seeAlso("jsc:disks", "jsc:drives")
                .register();

        guide.page("numbers_and_time", section).titled("Math, Convert, Random and Time")
                .paragraph("Programs compute with numbers, and the library gives you the common sums and conversions in"
                        + " Math and Convert. Random gives you numbers you cannot predict, and Time tells the program"
                        + " what the world's clock says. Calls to these cost nothing.")
                .subheading("Math")
                .table("Math members", "Member", "What it does")
                .row(GuideCell.code("Math.Abs(number)"), GuideCell.words("The size of a number, without its sign."))
                .row(GuideCell.code("Math.Min(a, b)"), GuideCell.words("The smaller of two numbers."))
                .row(GuideCell.code("Math.Max(a, b)"), GuideCell.words("The larger of two numbers."))
                .row(GuideCell.code("Math.Floor(number)"), GuideCell.words("The whole number at or below a decimal"
                        + " number."))
                .row(GuideCell.code("Math.Ceil(number)"), GuideCell.words("The whole number at or above a decimal"
                        + " number. Sigma# only."))
                .row(GuideCell.code("Math.Round(number)"), GuideCell.words("The nearest whole number. Sigma# only."))
                .row(GuideCell.code("Math.Sqrt(number)"), GuideCell.words("The square root of a decimal number."))
                .row(GuideCell.code("Math.Pow(number, power)"), GuideCell.words("A number raised to a power."))
                .row(GuideCell.code("Math.Clamp(value, low, high)"), GuideCell.words("The value, kept between low and"
                        + " high. Sigma# only."))
                .paragraph("Sigma offers Abs, Min, Max, Floor, Sqrt and Pow. The other members here are Sigma# only.")
                .subheading("Convert")
                .table("Convert members", "Member", "What it does")
                .row(GuideCell.code("Convert.ToInt(text)"), GuideCell.words("The whole number in the text. Stops the"
                        + " program when the text is not a number."))
                .row(GuideCell.code("Convert.ToInt(text, fallback)"), GuideCell.words("The whole number in the text, or"
                        + " the fallback when the text is not a number. Version 2."))
                .row(GuideCell.code("Convert.ToLong(text)"), GuideCell.words("A long whole number. Sigma# only."))
                .row(GuideCell.code("Convert.ToFloat(text)"), GuideCell.words("A short decimal number. Sigma# only."))
                .row(GuideCell.code("Convert.ToDouble(text)"), GuideCell.words("The decimal number in the text."))
                .row(GuideCell.code("Convert.ToBool(text)"), GuideCell.words("True or false, from the text."))
                .row(GuideCell.code("Convert.ToString(value)"), GuideCell.words("The text of a value."))
                .row(GuideCell.code("Convert.ToString(number, base)"), GuideCell.words("The number as text in a base"
                        + " from 2 to 36. Version 2."))
                .row(GuideCell.code("Convert.TryInt(text, out value)"), GuideCell.words("True and the number when the"
                        + " text is a number, false otherwise. Sigma# only."))
                .row(GuideCell.code("Convert.TryLong, Convert.TryDouble, Convert.TryBool"), GuideCell.words("The "
                        + "same as TryInt, for"
                        + " the other kinds. Sigma# only."))
                .paragraph("A base is how many different digits a number is written with. Base 10 is the ordinary way,"
                        + " and base 16 is hexadecimal. Base 10 keeps the minus sign. Every other base writes the 32 "
                        + "bits of"
                        + " the number, so -1 in base 16 is ffffffff.")
                .paragraph("Convert.ToBool takes true, yes, y, on or 1 for true, and false, no, n, off or 0 for false,"
                        + " in any case.")
                .subheading("Random")
                .table("Random members", "Member", "What it does")
                .row(GuideCell.code("Random.Next(bound)"), GuideCell.words("A whole number from 0 up to the bound, not"
                        + " including the bound."))
                .row(GuideCell.code("Random.NextDouble()"), GuideCell.words("A decimal number from 0 up to 1, not"
                        + " including 1. Sigma# only."))
                .row(GuideCell.code("Random.Seed(number)"), GuideCell.words("Starts the sequence again from that"
                        + " number."))
                .paragraph("Random is in Sigma# from version 1, and in Sigma from version 2. Each program has its own"
                        + " sequence, and seeding it with the same number gives the same sequence again.")
                .subheading("Time")
                .table("Time members", "Member", "What it does")
                .row(GuideCell.code("Time.Tick"), GuideCell.words("The world's tick count, as a long whole number."))
                .row(GuideCell.code("Time.DayTime"), GuideCell.words("The time of day in ticks, from 0 up to 23,999."
                        + " Sigma# only."))
                .row(GuideCell.code("Time.Day"), GuideCell.words("The day number of the world's clock."))
                .row(GuideCell.code("Time.Ticks(seconds)"), GuideCell.words("The number of ticks in that many seconds."
                        + " Sigma# only."))
                .paragraph("A tick is one step of the world's clock, and the game runs 20 ticks each second. Time.Ticks"
                        + " is the one place the library turns seconds into ticks. The credits a program gets each tick"
                        + " are explained in [](jsc:credits_per_tick).")
                .define("Bound", "The number a whole-number draw stays below. Random.Next(6) gives 0 to 5.")
                .ifSomethingGoesWrong(
                        "The program stops and says that the text is not a number.",
                        "Give a fallback as the second value of Convert.ToInt, or check the text first with"
                                + " Convert.TryInt.",
                        "The program stops and says there is no base in that number.",
                        "Use a base from 2 to 36, such as 10 or 16.",
                        "Random.Next always gives 0.",
                        "Give a bound of 1 or more. A bound of 6 gives numbers from 0 to 5.")
                .seeAlso("jsc:sigma_types_variables", "jsc:sigma_runtime_stops")
                .register();

        guide.page("computer_info", section).titled("Computer: what the machine is")
                .paragraph("A program can ask the machine it runs on what it is: its name, its processor, its system,"
                        + " its memory and its disks. The answers are pictures taken when you ask, not views that"
                        + " update by themselves.")
                .paragraph("You read the machine through Computer, a built-in type whose members you read"
                        + " directly. Write Computer.Name, not the name of a variable that holds a computer.")
                .table("Computer members", "Member", "What it gives")
                .row(GuideCell.code("Computer.Name"), GuideCell.words("The name of the machine, as text."))
                .row(GuideCell.code("Computer.Cpu"), GuideCell.words("The processor (CPU), as a CpuInfo record."
                        + " Sigma# only."))
                .row(GuideCell.code("Computer.Os"), GuideCell.words("The system, as an OsInfo record. Sigma# only."))
                .row(GuideCell.code("Computer.RamMb"), GuideCell.words("The memory in megabytes (MB), as a whole"
                        + " number."))
                .row(GuideCell.code("Computer.FreeRamMb"), GuideCell.words("The memory free now, in megabytes (MB)."
                        + " Sigma# only."))
                .row(GuideCell.code("Computer.Online"), GuideCell.words("Yes or no."))
                .row(GuideCell.code("Computer.Disks()"), GuideCell.words("A list of DiskInfo records. Sigma# only."))
                .row(GuideCell.code("Computer.Programs()"), GuideCell.words("The names of the programs. Sigma# only."))
                .row(GuideCell.code("Computer.Processes()"), GuideCell.words("A list of ProcessInfo records. Sigma#"
                        + " only."))
                .paragraph("Name, RamMb and Online work in Sigma too. The rest are Sigma# only.")
                .subheading("The records")
                .paragraph("A record is a value with fixed fields, and you read each field by its name. These are the"
                        + " records the Computer members give back.")
                .table("Records of Computer", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("CpuInfo"), GuideCell.code("CpuInfo.Mhz, CpuInfo.Cores, CpuInfo.Era, "
                        + "CpuInfo.Architecture"),
                        GuideCell.words("Mhz: the fastest clock of all the cores, in megahertz (MHz). Cores: all the"
                                + " cores together. Era: the era of the first processor. Architecture: the instruction"
                                + " set it runs."))
                .row(GuideCell.code("DiskInfo"), GuideCell.code("DiskInfo.Mount, DiskInfo.UsedMb, DiskInfo.CapacityMb"),
                        GuideCell.words("Mount: the drive letter. UsedMb and CapacityMb: megabytes used and in all."))
                .row(GuideCell.code("OsInfo"), GuideCell.code("OsInfo.Id, OsInfo.Name"),
                        GuideCell.words("The system's id and its name."))
                .row(GuideCell.code("ProcessInfo"), GuideCell.code("ProcessInfo.Id, ProcessInfo.Name, "
                        + "ProcessInfo.State, ProcessInfo.HeldBytes"),
                        GuideCell.words("The process number, its name, its state as text, and the bytes of memory it"
                                + " holds."))
                .paragraph("The era names are vintage, legacy, transition, standard, advanced, exa and singularity,"
                        + " written in lower case. A machine has seven eras to choose from, and an era is not the same"
                        + " thing as a hardware tier.")
                .define("Architecture", "The instruction set a processor runs. A program built for one runs on that"
                        + " processor and on every newer one.")
                .define("Mount", "The drive letter a disk is reached by.")
                .define("Property", "A value that belongs to an object or a record, and that you read by its name,"
                        + " such as Computer.Name.")
                .ifSomethingGoesWrong(
                        "A value looks out of date.",
                        "Ask again. Each read takes a new picture of the machine.",
                        "A Sigma program does not see Computer.Cpu.",
                        "Use Sigma#, which offers it, or use the members Sigma offers here.")
                .seeAlso("jsc:the_systems", "jsc:processors", "jsc:eras")
                .register();
    }

    private static void libraryPrograms(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("library_programs").titled("Programs")
                .in(chapter).register();
        guide.page("programs_and_processes", section).titled("Programs, processes and messages")
                .paragraph("A program can start other programs, ask which programs run on its machine, and hear from"
                        + " them. A program you start becomes a process ([](jsc:running_programs)), with its own "
                        + "number,"
                        + " its own memory and its own work to do.")
                .paragraph("Program is the type for the program itself and for the programs it starts. Program.Start"
                        + " gives you a handle: a value that refers to the new process by its number.")
                .table("Program members", "Member", "What it does")
                .row(GuideCell.code("Program.SetName(name)"), GuideCell.words("Names the program. The machine's process"
                        + " list shows that name. A program without a name gets one from the runtime."))
                .row(GuideCell.code("Program.Name"), GuideCell.words("The name the program has, as text."))
                .row(GuideCell.code("Program.Args"), GuideCell.words("The arguments the program was started with, as a"
                        + " list of text."))
                .row(GuideCell.code("Program.Exit(status)"), GuideCell.words("Ends the program with that status."))
                .row(GuideCell.code("Program.DroppedEvents"), GuideCell.words("How many events the program lost, "
                        + "because"
                        + " its queue was full. Sigma# only."))
                .row(GuideCell.code("Program.Start(path)"), GuideCell.words("Starts the program at the path, with"
                        + " optional arguments and priority, and gives a Process. Sigma# only."))
                .row(GuideCell.code("Program.Current"), GuideCell.words("The program itself, as a Process. Sigma#"
                        + " only."))
                .row(GuideCell.code("Program.Shell(line)"), GuideCell.words("Runs one line at this machine's own "
                        + "prompt,"
                        + " waits for it to end, and gives back the lines it printed. Sigma# only."))
                .row(GuideCell.code("Program.OnMessage(handler)"), GuideCell.words("Sets the handler that runs for each"
                        + " message other programs send this one. Sigma# only."))
                .paragraph("Program.Start compiles a file of source (.sg or .sgs) before it starts it. A compiled"
                        + " file runs as it is.")
                .paragraph("Start takes the path, then optional arguments, then an optional priority. The priority"
                        + " is a name: low, medium or high, in any case. Any other"
                        + " name, or none, gives medium. A low program gets a turn only every other round, and a high"
                        + " program runs like a medium one.")
                .paragraph("A program started from another program gets 1 megabyte (MB) of memory, its heap. The most"
                        + " a heap can be is 64 MB.")
                .subheading("Process")
                .paragraph("A Process is a handle to a program on a machine. You get one from Start, from Current, and"
                        + " from Processes on another machine.")
                .table("Process members", "Member", "What it does")
                .row(GuideCell.code("Process.Id"), GuideCell.words("The number of the process."))
                .row(GuideCell.code("Process.Name"), GuideCell.words("The name of the process, as text."))
                .row(GuideCell.code("Process.Host"), GuideCell.words("Empty on this machine, and the host name of a"
                        + " machine on the network for a remote one. Sigma# only."))
                .row(GuideCell.code("Process.Running"), GuideCell.words("Yes while the process runs."))
                .row(GuideCell.code("Process.ExitCode"), GuideCell.words("The status the process ended with."))
                .row(GuideCell.code("Process.Wait()"), GuideCell.words("Waits until the process ends."))
                .row(GuideCell.code("Process.Wait(ticks)"), GuideCell.words("Waits at most that many ticks. Gives "
                        + "yes when"
                        + " the process ended in that time."))
                .row(GuideCell.code("Process.Kill()"), GuideCell.words("Stops the process."))
                .row(GuideCell.code("Process.Output()"), GuideCell.words("The lines the process printed, as a list."))
                .row(GuideCell.code("Process.Send(id, text)"), GuideCell.words("Sends a message to the process with "
                        + "that"
                        + " number. Gives false when its queue has no room."))
                .paragraph("A wait ends early when the program that waits, or the one it waits for, ends. Times are in"
                        + " ticks, and a tick is explained in [](jsc:credits_per_tick).")
                .subheading("Messages and events")
                .paragraph("A message is text one program sends to another. The receiver gets it as a ProcessMessage,"
                        + " with the number of the sender (ProcessMessage.From), the text (ProcessMessage.Text) and "
                        + "the tick it was sent (ProcessMessage.Tick).")
                .paragraph("A program does not act on a message at the moment it arrives. Each message, each click, and"
                        + " each change to a watched total waits in a queue as an event. The program runs the handlers"
                        + " in the order the events arrived.")
                .table("Process states", "State", "What it means")
                .row(GuideCell.code("running"), GuideCell.words("The process has turns to run."))
                .row(GuideCell.code("parked"), GuideCell.words("The process waits for something outside itself."))
                .row(GuideCell.code("finished"), GuideCell.words("The process ended."))
                .row(GuideCell.code("halted"), GuideCell.words("An error stopped the process."))
                .paragraph("A program keeps at most 256 waiting calls. An event that finds no room is dropped and"
                        + " counted in Program.DroppedEvents. A message is the exception: it is refused, and its"
                        + " sender is told. Closing a window always gets in.")
                .define("Handle", "A value that refers to a process by its number.")
                .define("Heap","The memory a program has for its own values. 1 MB by default when another program"
                        + " starts it.")
                .define("Handler", "The part of your program that runs when an event arrives.")
                .define("Event", "Something the program is told about: a message, a click, or a change it watches.")
                .ifSomethingGoesWrong(
                        "Program.Start stops the program: nothing installed runs a file of that kind.",
                        "Start a compiled program, or compile the source file first.",
                        "Program.Start stops the program: the program needs more memory than is free.",
                        "Close a program to free memory, then start it again.",
                        "The start stops with the compiler's text, because the source file does not compile.",
                        "Read the error the compiler gives, and fix the line it names. The errors are explained in"
                                + " [](jsc:sigma_reading_an_error).",
                        "Process.Output or Process.ExitCode stops the program: there is no process here.",
                        "The process has ended, so it is no longer on the machine. Read its output while the process"
                                + " is still running.",
                        "Process.Send gives false.",
                        "The receiving program has no room in its queue. Send again later.",
                        "The program stops with an error, and its files close.",
                        "Read the error in the console. The program's work stops with that error.")
                .seeAlso("jsc:running_programs", "jsc:threads_and_locks", "jsc:sigma_runtime_stops")
                .register();

        guide.page("threads_and_locks", section).titled("Threads and lock")
                .paragraph("A thread is a line of work inside one program. Threads of one program share its memory,"
                        + " and only one of them runs at a time: the machine gives each a turn in turn.")
                .paragraph("Thread exists only in Sigma#. Sigma has no threads, and it has no lock statement either.")
                .table("Thread members", "Member", "What it does")
                .row(GuideCell.code("Thread.Start(action)"), GuideCell.words("Starts a new thread that runs the method"
                        + " the action points at."))
                .row(GuideCell.code("Thread.Current"), GuideCell.words("The thread that is running this part of your "
                        + "program."))
                .row(GuideCell.code("Thread.Sleep(ticks)"), GuideCell.words("Stops the thread for that many ticks."))
                .row(GuideCell.code("Thread.Yield()"), GuideCell.words("Gives the turn to the next thread."))
                .row(GuideCell.code("Thread.Id"), GuideCell.words("The number of the thread."))
                .row(GuideCell.code("Thread.Running"), GuideCell.words("Yes while the thread runs."))
                .row(GuideCell.code("Thread.Join()"), GuideCell.words("Waits until the thread ends."))
                .row(GuideCell.code("Thread.Join(ticks)"), GuideCell.words("Waits at most that many ticks. Gives yes "
                        + "when"
                        + " the thread ended in that time."))
                .row(GuideCell.code("Thread.Stop()"), GuideCell.words("Stops the thread."))
                .paragraph("A thread runs a few instructions at a time, and 64 at most before another thread of the"
                        + " same program has its turn. Sleep and Join count ticks, and 20 ticks make one second.")
                .subheading("Lock")
                .paragraph("A lock stops two threads from running the same steps on one object at once. The lock"
                        + " statement takes the object's lock while its body runs, and lets go when the body ends.")
                .steps("Write lock, then the object in brackets.", "Write the statement the lock covers after the"
                        + " brackets.")
                .paragraph("Only an object can be locked. A thread that finds the lock held by another thread waits,"
                        + " and asks again when the lock is free. A thread that already holds a lock can take it again,"
                        + " and then it must let go as many times as it took it.")
                .define("Thread", "A line of work inside one program, sharing its memory with the program's other"
                        + " threads.")
                .define("Lock", "A claim on an object, so that only one thread at a time runs the steps that hold it.")
                .ifSomethingGoesWrong(
                        "The program stops and says there is no body to run on the thread.",
                        "Give Thread.Start a method with a body of steps to run.",
                        "The program stops and says there is no thread here.",
                        "The thread already ended, or it was never started here. Check Thread.Running first.",
                        "The program stops and says that this thread is letting go of a lock it does not hold.",
                        "Use the lock statement, which takes and lets go of the lock in turn.",
                        "The compiler says only an object can be locked.",
                        "Lock an object, such as a value of a class you write, not a plain number.",
                        "The compiler refuses Thread or lock in Sigma.",
                        "Use Sigma#, or write the program so that its steps run one after another.")
                .seeAlso("jsc:sigma_delegates_events", "jsc:sigma_runtime_stops", "jsc:running_programs")
                .register();
    }

    private static void libraryNetwork(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("library_network").titled("The network")
                .in(chapter).register();
        guide.page("network_watches", section).titled("The network: what it holds, and being told when it changes")
                .paragraph("The network is every computer and device joined by data cable to one Mainframe, the machine"
                        + " that runs the network's operations. A program on a machine that is on the network can ask"
                        + " what the network holds, and it can ask to be told when that changes.")
                .paragraph("A machine that is on no network cannot ask any of this. Only Network.Online and"
                        + " Network.Current answer there, and every other call stops the program.")
                .table("Network members", "Member", "What it does")
                .row(GuideCell.code("Network.Online"), GuideCell.words("Yes when the machine is on a network."))
                .row(GuideCell.code("Network.Current"), GuideCell.words("The short id of the network the machine is on,"
                        + " or null when it is on none."))
                .row(GuideCell.code("Network.Capacity"), GuideCell.words("What the network's servers can hold, as a "
                        + "long"
                        + " whole number."))
                .row(GuideCell.code("Network.Used"), GuideCell.words("What the network's servers hold, as a long whole"
                        + " number."))
                .row(GuideCell.code("Network.Total(item)"), GuideCell.words("The number of that item on the whole"
                        + " network."))
                .row(GuideCell.code("Network.Types()"), GuideCell.words("The names of the items the network holds, as a"
                        + " list."))
                .row(GuideCell.code("Network.Find(item)"), GuideCell.words("A list of HoldingInfo, one for each server"
                        + " that holds the item."))
                .row(GuideCell.code("Network.Servers()"), GuideCell.words("A list of ServerInfo, one for each server."))
                .row(GuideCell.code("Network.Watch(item, handler)"), GuideCell.words("Calls the handler when the "
                        + "network"
                        + " total of the item changes. Gives a Subscription."))
                .row(GuideCell.code("Network.WatchBelow(item, threshold, handler)"), GuideCell.words("Calls the handler"
                        + " when the total falls to the threshold or below it. Gives a Subscription."))
                .row(GuideCell.code("Network.WatchAbove(item, threshold, handler)"), GuideCell.words("Calls the handler"
                        + " when the total rises to the threshold or above it. Gives a Subscription."))
                .row(GuideCell.code("Network.Computers()"), GuideCell.words("A list of the other computers on the "
                        + "network,"
                        + " as RemoteComputer values."))
                .row(GuideCell.code("Network.Computer(host)"), GuideCell.words("The computer with that host name, or "
                        + "null"
                        + " when there is none."))
                .paragraph("Every member of Network is available in Sigma#"
                        + " only. Total counts every server that holds the item.")
                .subheading("What the records hold")
                .table("Records of Network", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("HoldingInfo"), GuideCell.code("HoldingInfo.Server, HoldingInfo.Quantity"),
                        GuideCell.words("The name of a server, and how much of the item it holds."))
                .row(GuideCell.code("ServerInfo"), GuideCell.code("ServerInfo.Name, ServerInfo.Stored, "
                        + "ServerInfo.Capacity"),
                        GuideCell.words("The name of a server, what it holds, and what it can hold."))
                .row(GuideCell.code("StockEvent"), GuideCell.code("StockEvent.Item, StockEvent.Total, "
                        + "StockEvent.Previous"),
                        GuideCell.words("The item, its new total, and its total before the change."))
                .row(GuideCell.code("Subscription"), GuideCell.code("Subscription.Id, Subscription.Item"),
                        GuideCell.words("The number of the watch, and the item it watches."))
                .subheading("Watches")
                .paragraph("A watch is kept by your program, not by the machine. Once each tick the machine is asked "
                        + "for"
                        + " the totals of everything being watched, and the watches that fire run in the order you set"
                        + " them.")
                .paragraph("A watch does not fire on its first reading. A program that starts with a low total is not"
                        + " told that it fell. WatchBelow fires once when the total goes to the threshold or below,"
                        + " after it was above. It rearms when the total goes back above, so it can fire again."
                        + " WatchAbove works the other way round.")
                .paragraph("The library has no call that stops a watch. Watches are kept when the world is saved and"
                        + " loaded again.")
                .define("Network total","The sum of an item over every server on the network.")
                .define("Threshold", "The number a watch is set at. Below and above fire on crossing it, not while the"
                        + " total stays there.")
                .define("Rearm", "A watch is ready to fire again once the total has gone back to the other side of the"
                        + " threshold.")
                .ifSomethingGoesWrong(
                        "The program stops and says this computer is not on a network.",
                        "Check Network.Online first, and call the other members only when it says yes.",
                        "Network.Current gives null.",
                        "The machine is on no network. Check Network.Online first.",
                        "Network.Computer gives null.",
                        "No computer on the network has that host name. Look at the names in Network.Computers.",
                        "A watch does not fire after you set it.",
                        "A watch fires on a change after its first reading. Wait for the total to change.")
                .seeAlso("jsc:what_a_network_is", "jsc:first_network", "jsc:network_identity")
                .register();

        guide.page("iql_library", section).titled("Iql: the network's language from a program")
                .paragraph("IQL is the network's query language. One IQL statement asks for something, or changes what"
                        + " the network holds and does. The language itself is in [](jsc:iql), and the book Learning "
                        + "IQL"
                        + " teaches it from zero in [](jsc:what_iql_is).")
                .paragraph("Iql lets a program send those statements itself, instead of you typing them at a prompt. "
                        + "Every"
                        + " Iql call needs a network with a Mainframe. Only Sigma# programs can use it.")
                .table("Iql members", "Member", "What it does")
                .row(GuideCell.code("Iql.Run(statement)"), GuideCell.words("Runs one statement and gives an "
                        + "IqlResult. A"
                        + " refused statement does not stop the program."))
                .row(GuideCell.code("Iql.Query(statement)"), GuideCell.words("Runs a statement and gives its rows as "
                        + "a list"
                        + " of maps. A refused statement stops the program."))
                .row(GuideCell.code("Iql.Exec(statement)"), GuideCell.words("Runs a statement and gives an IqlResult."))
                .row(GuideCell.code("Iql.Exec(statement, values)"), GuideCell.words("The same, with the values "
                        + "passed to a"
                        + " stored statement."))
                .row(GuideCell.code("Iql.RunFile(path)"), GuideCell.words("Runs each statement in the file and gives an"
                        + " IqlResult."))
                .paragraph("Exec puts EXEC in front of your statement, and adds each value after the statement, one"
                        + " after another. Use it to pass values to a stored statement.")
                .paragraph("RunFile reads the file first. When the file cannot be read, the program stops with the "
                        + "file's"
                        + " message.")
                .subheading("What the results hold")
                .table("Records of Iql", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("IqlResult"), GuideCell.code("IqlResult.Ok, IqlResult.Message, IqlResult.Rows"),
                        GuideCell.words("Ok: yes when the network took the statement. Message: the network's answer."
                                + " Rows: the result lines."))
                .paragraph("Each row is a map with three keys: name, quantity and detail. The name is the item or thing"
                        + " the row is about, quantity is its count, and detail says more about it.")
                .define("Row", "One line of a result, given as a map of name, quantity and detail.")
                .ifSomethingGoesWrong(
                        "The program stops and says this computer is not on a network with a Mainframe.",
                        "Build a network with a Mainframe, and check Mainframe.Online before you call Iql.",
                        "Query stops the program with the engine's text.",
                        "Use Run or Exec instead. They give Ok false and the message, and the program goes on.",
                        "Ok is false, and the message says the statement was refused.",
                        "Read the message. It is the network's answer, and the verbs are explained in [](jsc:iql).",
                        "RunFile stops the program with the file's message.",
                        "Check the path with File.Exists before you call RunFile.")
                .seeAlso("jsc:iql", "jsc:what_iql_is", "jsc:mainframes")
                .register();

        guide.page("mainframe_stats", section).titled("Mainframe: what the orchestrator has done")
                .paragraph("The Mainframe keeps count of the work its network has done: how many of each kind it "
                        + "did, how"
                        + " long each one waited and ran, and how much each one moved.")
                .paragraph("If you want to know whether a Mainframe runs on the network, ask Mainframe.Online. Its"
                        + " other members need a"
                        + " network, and they are Sigma# only.")
                .table("Mainframe members", "Member", "What it does")
                .row(GuideCell.code("Mainframe.Online"), GuideCell.words("Yes when a Mainframe runs on the network."))
                .row(GuideCell.code("Mainframe.PeakToday"), GuideCell.words("The most Operations running at once in "
                        + "the last"
                        + " day. Zero when there is no Mainframe."))
                .row(GuideCell.code("Mainframe.Stats(kind)"), GuideCell.words("The work of one kind, as a WorkStat."))
                .row(GuideCell.code("Mainframe.Work()"), GuideCell.words("The work of every kind the network did, as "
                        + "a list"
                        + " of WorkStat."))
                .paragraph("A kind of work the network has not done reads as zeros. Stats reads one kind; Work lists "
                        + "the"
                        + " kinds it has done.")
                .subheading("What a WorkStat holds")
                .table("Fields of WorkStat", "Field", "What it holds")
                .row(GuideCell.code("WorkStat.Type"), GuideCell.words("The kind of work, in lower case."))
                .row(GuideCell.code("WorkStat.Count"), GuideCell.words("How many of that kind the network did."))
                .row(GuideCell.code("WorkStat.AverageWait"), GuideCell.words("The average wait of each one, as a "
                        + "whole number."))
                .row(GuideCell.code("WorkStat.AverageRun"), GuideCell.words("The average run of each one, as a whole "
                        + "number."))
                .row(GuideCell.code("WorkStat.ShortfallPercent"), GuideCell.words("A whole number of percent."))
                .row(GuideCell.code("WorkStat.Moved"), GuideCell.words("How much of the item moved, as a long whole "
                        + "number."))
                .ifSomethingGoesWrong(
                        "The program stops and says this computer is not on a network.",
                        "Check Network.Online first.",
                        "Mainframe.PeakToday is 0.",
                        "There is no Mainframe on the network. Check Mainframe.Online.",
                        "Stats gives zeros for a kind of work.",
                        "The network has not done that kind yet. Use Mainframe.Work to see the kinds it has done.")
                .seeAlso("jsc:mainframes", "jsc:operations_catalogue", "jsc:states_and_queues")
                .register();

        guide.page("remote_computers", section).titled("Other computers on the network")
                .paragraph("A program can start a program on another computer of the same network, and ask that "
                        + "computer"
                        + " what it runs. The other computer is a RemoteComputer, and you get one from Network.Computer"
                        + " or Network.Computers.")
                .paragraph("The computer must be on the same network. A host name that matches several machines is"
                        + " refused, so use the node id or the full host name instead.")
                .define("Host name", "The name a machine answers to on the network.")
                .define("Node id", "The machine's own identifier. Use it in place of a host name that matches"
                        + " several machines.")
                .table("Fields of RemoteComputer", "Field", "What it holds")
                .row(GuideCell.code("RemoteComputer.Host"), GuideCell.words("The host name."))
                .row(GuideCell.code("RemoteComputer.Name"), GuideCell.words("The name of the computer."))
                .row(GuideCell.code("RemoteComputer.Type"), GuideCell.words("Mainframe, Computer, Crafting Computer, "
                        + "Personal "
                        + "Computer or"
                        + " Cluster Management Computer."))
                .row(GuideCell.code("RemoteComputer.Os"), GuideCell.words("The system the computer runs."))
                .row(GuideCell.code("RemoteComputer.Online"), GuideCell.words("Yes when the computer is on."))
                .table("Calls on a RemoteComputer", "Call", "What it does")
                .row(GuideCell.code("RemoteComputer.Start(path)"), GuideCell.words("Starts a program on that "
                        + "computer, as "
                        + "Program.Start"
                        + " does, and gives a Process; Start also takes (path, args) or (path, args, priority)."))
                .row(GuideCell.code("RemoteComputer.Shell(line)"), GuideCell.words("Runs a line at that computer's "
                        + "prompt, and "
                        + "gives back"
                        + " the lines it printed."))
                .row(GuideCell.code("RemoteComputer.Send(id, text)"), GuideCell.words("Sends a message to a process "
                        + "on that computer."
                        + " Gives false when it cannot."))
                .row(GuideCell.code("RemoteComputer.Processes()"), GuideCell.words("The processes on that computer, "
                        + "as a list of "
                        + "Process."))
                .table("Refusals from another computer", "Message", "What it means")
                .row(GuideCell.words("The computer cannot run programs"), GuideCell.words("That machine does not run"
                        + " programs."))
                .row(GuideCell.words("The computer does not take programs from other computers"),
                        GuideCell.words("That machine refuses programs sent from others."))
                .row(GuideCell.words("The computer is powered off"), GuideCell.words("That machine is off."))
                .row(GuideCell.words("No such computer on this network"), GuideCell.words("No machine of that name "
                        + "is on"
                        + " this network."))
                .ifSomethingGoesWrong(
                        "The program stops and says the computer is powered off.",
                        "Turn the computer on, then call it again.",
                        "The program stops and says the computer cannot run programs.",
                        "Choose another computer on the network for the program.",
                        "The program stops and says the computer does not take programs from other computers.",
                        "Start the program on another computer, or run it on that computer yourself.",
                        "Network.Computer gives null.",
                        "No computer of that name is on the network. Look at the names in Network.Computers.",
                        "The host name matches several machines.",
                        "Use the node id, or the full host name.")
                .seeAlso("jsc:remote_access", "jsc:what_a_network_is")
                .register();

        guide.page("operations_library", section).titled("Operations from a program")
                .paragraph("An Operation ([](jsc:what_operations_are)) is one job the network does. A SELECT brings an"
                        + " item from the network to this"
                        + " machine, an INSERT sends items the other way, and a CRAFT asks the network to make an item."
                        + " Operations is the type a program uses to ask for these jobs, and to look at the jobs in "
                        + "flight.")
                .paragraph("Every Operations call needs a network, so check Network.Online first when you are not sure."
                        + " A request the network cannot take does not stop the"
                        + " program: Ok is false, and the message says why. The kinds of Operation are in"
                        + " [](jsc:operations_catalogue).")
                .table("Operations members", "Member", "What it does")
                .row(GuideCell.code("Operations.Pull(item, amount)"), GuideCell.words("Asks the network to bring the "
                        + "item"
                        + " to this machine's storage (SELECT)."))
                .row(GuideCell.code("Operations.Push(item, amount)"), GuideCell.words("Sends items from this machine's"
                        + " storage to the network (INSERT)."))
                .row(GuideCell.code("Operations.Craft(item, amount)"), GuideCell.words("Asks the network to make the "
                        + "item"
                        + " (CRAFT)."))
                .row(GuideCell.code("Operations.Cancel(id)"), GuideCell.words("Cancels the Operation with that id."))
                .row(GuideCell.code("Operations.Reprioritise(id, priority)"), GuideCell.words("Changes how urgently the"
                        + " Operation runs."))
                .row(GuideCell.code("Operations.Update(item, action, amount)"), GuideCell.words("Asks for an UPDATE "
                        + "with"
                        + " the action, such as SMELT. Added in version 2."))
                .row(GuideCell.code("Operations.Get(id)"), GuideCell.words("The OperationInfo of the Operation with "
                        + "that id,"
                        + " or nothing when it is no longer in flight."))
                .row(GuideCell.code("Operations.List()"), GuideCell.words("Every Operation in flight, as a list of"
                        + " OperationInfo."))
                .paragraph("Pull, Push, Craft, Cancel, Reprioritise and Update give an AskResult. Get and List give"
                        + " OperationInfo. Cancel accepts the start of an id, provided that start is at least as long "
                        + "as the"
                        + " short form of the id. Get needs the whole id, and ignores case.")
                .subheading("What the results hold")
                .table("Records of Operations", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("AskResult"), GuideCell.code("AskResult.Ok, AskResult.Message"),
                        GuideCell.words("Ok: yes when the network took the request. Message: the network's answer, in"
                                + " English."))
                .row(GuideCell.code("OperationInfo"), GuideCell.code("OperationInfo.Id, OperationInfo.Type, "
                        + "OperationInfo.Item, OperationInfo.Moved, OperationInfo.Requested, OperationInfo.Status, "
                        + "OperationInfo.Priority"),
                        GuideCell.words("Id: the id to use. Type and Status: lower-case text. Item: the item. Moved and"
                                + " Requested: how much has moved, and how much was asked for."))
                .table("Priorities of an Operation", "Level", "Ways to write it")
                .row(GuideCell.words("Low"), GuideCell.code("low"))
                .row(GuideCell.words("Medium low"), GuideCell.code("medium_low"))
                .row(GuideCell.words("Medium"), GuideCell.code("medium, or normal"))
                .row(GuideCell.words("Medium high"), GuideCell.code("medium_high"))
                .row(GuideCell.words("High"), GuideCell.code("high"))
                .paragraph("The five Operation levels in the table are not the same scale as a program's priority,"
                        + " which is written as low, medium or high.")
                .subheading("Messages the network answers with")
                .table("Messages of an AskResult", "Message", "What it means")
                .row(GuideCell.words("unknown item: <item>"), GuideCell.words("The network does not know the item."))
                .row(GuideCell.words("the network has no running Mainframe"), GuideCell.words("No Mainframe runs on the"
                        + " network."))
                .row(GuideCell.words("this computer holds no <item>"), GuideCell.words("A push of an item this "
                        + "machine does"
                        + " not hold."))
                .row(GuideCell.words("nothing to push"), GuideCell.words("The push has no items to send."))
                .row(GuideCell.words("no pattern crafts <item>"), GuideCell.words("No pattern makes that item."))
                .row(GuideCell.words("the network has no room for <item>"), GuideCell.words("The network's storage "
                        + "has no"
                        + " room for the item."))
                .row(GuideCell.words("no operation <id> in flight (see 'ops')"),
                        GuideCell.words("No Operation with that id is running."))
                .row(GuideCell.words("no such priority: <name>"), GuideCell.words("The priority name is not one of the"
                        + " levels above."))
                .ifSomethingGoesWrong(
                        "The program stops and says this computer is not on a network.",
                        "Check Network.Online first.",
                        "AskResult says no pattern crafts the item.",
                        "Add a pattern for the item in the Pattern Studio ([](jsc:pattern_studio)).",
                        "AskResult says the network has no running Mainframe.",
                        "Start a Mainframe on the network, then ask again.",
                        "AskResult says the network has no room for the item.",
                        "Add storage to the network, such as a storage server ([](jsc:storage_servers)).",
                        "Operations.Get gives nothing.",
                        "The Operation has finished or was cancelled. Use Operations.List to see what is running.")
                .seeAlso("jsc:what_operations_are", "jsc:operations_catalogue", "jsc:states_and_queues")
                .register();
    }

    private static void libraryMachine(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("library_machine").titled("Devices")
                .in(chapter).register();
        guide.page("buses_library", section).titled("Buses from a program")
                .paragraph("A bus is a block that moves items between the network and a machine. Its settings say which"
                        + " items it handles, how many it keeps and moves, and when it works. The bus itself is "
                        + "explained"
                        + " in [](jsc:buses).")
                .paragraph("If you want to set a bus, Bus.Named finds it by its name on the network. It gives null "
                        + "when no"
                        + " bus of that name is"
                        + " on the network. Bus came in version 2.")
                .table("Bus members", "Member", "What it does")
                .row(GuideCell.code("Bus.Named(name)"), GuideCell.words("The bus with that name, or null when there is"
                        + " none."))
                .row(GuideCell.code("Bus.Name"), GuideCell.words("The name of the bus."))
                .row(GuideCell.code("Bus.On()"), GuideCell.words("Turns the bus on."))
                .row(GuideCell.code("Bus.Off()"), GuideCell.words("Turns the bus off."))
                .row(GuideCell.code("Bus.Continuous()"), GuideCell.words("Sets the mode in which the bus runs."))
                .row(GuideCell.code("Bus.OnDemand()"), GuideCell.words("Sets the other mode, on demand."))
                .row(GuideCell.code("Bus.ReadWrite()"), GuideCell.words("Sets the bus to move items both ways."))
                .row(GuideCell.code("Bus.ReadOnly()"), GuideCell.words("Sets the bus to take items out only."))
                .row(GuideCell.code("Bus.WriteOnly()"), GuideCell.words("Sets the bus to put items in only."))
                .row(GuideCell.code("Bus.Only(items)"), GuideCell.words("The bus handles only the items listed."))
                .row(GuideCell.code("Bus.AllBut(items)"), GuideCell.words("The bus handles every item except the ones"
                        + " listed."))
                .row(GuideCell.code("Bus.Tag(tag)"), GuideCell.words("Adds a tag to the bus."))
                .row(GuideCell.code("Bus.Keep(number)"), GuideCell.words("How many of an item the bus keeps in place."))
                .row(GuideCell.code("Bus.Max(number)"), GuideCell.words("The most of an item the bus moves at once."))
                .row(GuideCell.code("Bus.Priority(number)"), GuideCell.words("Which bus goes first."))
                .row(GuideCell.code("Bus.Fuzzy(yes or no)"), GuideCell.words("The fuzzy setting of the bus."))
                .row(GuideCell.code("Bus.WhenStock(item, number)"), GuideCell.words("A condition on the stock of an "
                        + "item,"
                        + " explained in [](jsc:bus_conditions)."))
                .row(GuideCell.code("Bus.WhenStockTag(tag, number)"), GuideCell.words("The same condition, on the stock"
                        + " of a tag."))
                .row(GuideCell.code("Bus.Between(from, to)"), GuideCell.words("The hours of the day the bus works, "
                        + "from 0"
                        + " to 23."))
                .row(GuideCell.code("Bus.After(bus)"), GuideCell.words("Names another bus that this bus comes after, "
                        + "as a"
                        + " condition."))
                .row(GuideCell.code("Bus.Item(name)"), GuideCell.words("The settings of one item on this bus, as a"
                        + " BusItem."))
                .paragraph("Each setting gives the bus back, so settings can follow one another. For example,"
                        + " bus(\"Ore in\").Keep(16).Max(64) keeps 16 of the item and moves at most 64 at once.")
                .paragraph("Only and AllBut take the items separated by commas. Blank items are dropped. Between takes"
                        + " hours from 0 to 23.")
                .paragraph("A setting goes through the network's own language. When the bus cannot take it, the program"
                        + " stops with the engine's answer. Each setting is marked with the program that made it: the"
                        + " class the program starts from, or Sigma when it has none.")
                .subheading("A BusItem")
                .paragraph("A BusItem is one item of a bus. Its fields are BusItem.Bus and BusItem.Item, the names "
                        + "of the bus and the"
                        + " item. Its settings are BusItem.Keep and BusItem.Max, and they work as the same settings "
                        + "on the"
                        + " bus do.")
                .ifSomethingGoesWrong(
                        "The program stops and says there is no bus with that name on this network.",
                        "Check the name in the bus's window. Bus.Named gives null when the bus is not on the network.",
                        "The program stops and says the bus cannot be set so.",
                        "The bus's era or its kind has no such setting. Choose another setting, or use another bus.",
                        "The program stops and says the item is not in the filter of the bus.",
                        "Add the item to the bus's filter, or set the bus to take every item.",
                        "The program stops and says there is no item or fluid with that name.",
                        "Check the name of the item. Names are the ones the network uses.",
                        "The program stops and says the hour is not an hour of the day.",
                        "Use hours from 0 to 23.")
                .seeAlso("jsc:buses", "jsc:bus_conditions", "jsc:bus_filters_and_tags")
                .register();

        guide.page("crafting_library", section).titled("Crafting interfaces and routers from a program")
                .paragraph("A crafting interface holds patterns and asks the network for crafts. A crafting router, "
                        + "on the"
                        + " same crafting cable, decides which inputs it carries, by its filter. The machines "
                        + "themselves"
                        + " are explained in [](jsc:crafting_interfaces) and [](jsc:pattern_encoders).")
                .table("CraftInterface members", "Member", "What it does")
                .row(GuideCell.code("CraftInterface.Named(name)"), GuideCell.words("The interface with that name, or "
                        + "null"
                        + " when there is none."))
                .row(GuideCell.code("CraftInterface.Name"), GuideCell.words("The name of the interface."))
                .row(GuideCell.code("CraftInterface.Exclusive(yes or no)"), GuideCell.words("The exclusive setting "
                        + "of the"
                        + " interface."))
                .row(GuideCell.code("CraftInterface.MaxJobs(number)"), GuideCell.words("The most jobs the interface "
                        + "takes"
                        + " on."))
                .row(GuideCell.code("CraftInterface.Pause()"), GuideCell.words("Pauses the interface."))
                .row(GuideCell.code("CraftInterface.Resume()"), GuideCell.words("Resumes the interface."))
                .row(GuideCell.code("CraftInterface.Route(pattern, input, router)"), GuideCell.words("Sends one "
                        + "input of a"
                        + " pattern through one router of the interface's own cable. A null router routes that input by"
                        + " the routers' filters."))
                .table("CraftRouter members", "Member", "What it does")
                .row(GuideCell.code("CraftRouter.Named(name)"), GuideCell.words("The router with that name, or null "
                        + "when"
                        + " there is none."))
                .row(GuideCell.code("CraftRouter.Name"), GuideCell.words("The name of the router."))
                .row(GuideCell.code("CraftRouter.Only(items)"), GuideCell.words("The router takes only the items "
                        + "listed."))
                .row(GuideCell.code("CraftRouter.AllBut(items)"), GuideCell.words("The router takes every item "
                        + "except the"
                        + " ones listed."))
                .row(GuideCell.code("CraftRouter.Tag(tag)"), GuideCell.words("Adds a tag to the router."))
                .row(GuideCell.code("CraftRouter.Any()"), GuideCell.words("Clears the filter, so the router takes "
                        + "anything."))
                .row(GuideCell.code("CraftRouter.Fuzzy(yes or no)"), GuideCell.words("The fuzzy setting of the "
                        + "router."))
                .paragraph("The interface and the router are found on the machine's crafting network. The name your "
                        + "program"
                        + " finds them by is the name their windows give, and a window can rename them.")
                .paragraph("Each setting goes through the same engine as the bus settings. A refused setting stops the"
                        + " program with the engine's text. Each setting is recorded under the program's class name, or"
                        + " under Sigma when the program has none.")
                .ifSomethingGoesWrong(
                        "The program stops and says there is no Crafting Interface with that name on this network.",
                        "Check the name in the interface's window.",
                        "The program stops and says there is no Crafting Input Router with that name on this network.",
                        "Check the name in the router's window.",
                        "The program stops and says the interface holds no pattern with that name.",
                        "Check the pattern's name in the interface's window.",
                        "The program stops and says the input is not an input of the pattern.",
                        "Use one of the inputs that the pattern holds.",
                        "The program stops and says there is no router with that name on the cable of the interface.",
                        "Use a router on the same cable as the interface.")
                .seeAlso("jsc:crafting_interfaces", "jsc:pattern_encoders", "jsc:asking_for_a_craft")
                .register();

        guide.page("redstone_library", section).titled("Redstone Interfaces from a program")
                .paragraph("A Redstone Interface reads redstone signal for a machine, or emits it, and it is linked "
                        + "to the"
                        + " machine by cable. Your program reads and sets it through Redstone. The block itself is "
                        + "explained"
                        + " in [](jsc:redstone_interfaces).")
                .define("Redstone signal", "A strength from 0 to 15: strength 0 is off, and strength 15 is the"
                        + " strongest.")
                .define("Live", "Powered and linked to the machine. A live interface shows its strength; one that is"
                        + " not live shows strength 0.")
                .table("Redstone members", "Member", "What it does")
                .row(GuideCell.code("Redstone.Named(name)"), GuideCell.words("The interface with that name, or null "
                        + "when no"
                        + " interface linked to this machine answers to it, ignoring capitals."))
                .row(GuideCell.code("Redstone.Name"), GuideCell.words("The name of the interface."))
                .row(GuideCell.code("Redstone.In()"), GuideCell.words("Sets the interface to read the signal. It stops"
                        + " emitting."))
                .row(GuideCell.code("Redstone.Out(strength)"), GuideCell.words("Sets the interface to emit that "
                        + "strength,"
                        + " from 0 to 15."))
                .row(GuideCell.code("Redstone.Level()"), GuideCell.words("The strength the interface shows now."))
                .paragraph("In sets the interface to read, and Out sets it to emit. The last one you call wins. Each"
                        + " setting is marked with the name of the program that made it, or with Sigma when that"
                        + " program has no name of its own.")
                .paragraph("Level shows the strength that is emitted while the interface is live, or the strength it "
                        + "reads"
                        + " when it does not emit. It gives 0 when the interface is not live.")
                .paragraph("An interface with no name of its own answers to Redstone Interface. A name is up to 32"
                        + " characters long, and two interfaces of one machine cannot share a name.")
                .ifSomethingGoesWrong(
                        "The program stops and says the number is not a redstone strength.",
                        "Give Out a whole number from 0 to 15.",
                        "The program stops and says no Redstone Interface with that name is linked to this machine.",
                        "Link the interface to the machine with cable, and check its name in its window.",
                        "Redstone.Named gives null.",
                        "No enabled interface linked to this machine has that name. Check the name in its window.",
                        "Redstone.Level gives 0.",
                        "The interface is not live. Check that it is powered and linked to this machine.")
                .seeAlso("jsc:redstone_interfaces", "jsc:disable_device")
                .register();

        guide.page("sound_library", section).titled("Sound: beeps, tunes, recordings and speakers")
                .paragraph("A machine makes sound through the speaker in its case, and through the sound cards you fit."
                        + " Sound is the type for the machine as a whole, and Speaker is one speaker you can name. The"
                        + " hardware is explained in [](jsc:sound_cards) and [](jsc:speakers).")
                .define("Hertz", "The number of times a sound wave repeats each second. A higher number is a higher "
                        + "pitch.")
                .define("Voice", "One sound that the sound card can play at once.")
                .define("Recording", "A song file on the machine's disk.")
                .define("Note", "A named pitch: a letter from A to G, an optional sharp or flat, and an octave from "
                        + "0 to 8.")
                .table("Sound members", "Member", "What it does")
                .row(GuideCell.code("Sound.Beep(hertz, millis)"), GuideCell.words("Beeps at that pitch, for that many"
                        + " milliseconds (ms)."))
                .row(GuideCell.code("Sound.Tones(tune)"), GuideCell.words("Plays a tune of notes. Gives true when it "
                        + "plays."))
                .row(GuideCell.code("Sound.Play(path)"), GuideCell.words("Plays a recording from the machine's disk. "
                        + "Gives"
                        + " true when it plays."))
                .row(GuideCell.code("Sound.Stop()"), GuideCell.words("Stops the recordings the programs are playing."))
                .table("Speaker members", "Member", "What it does")
                .row(GuideCell.code("Speaker.Named(name)"), GuideCell.words("The speaker with that name, linked to this"
                        + " machine, or null when there is none."))
                .row(GuideCell.code("Speaker.Name"), GuideCell.words("The name of the speaker."))
                .row(GuideCell.code("Speaker.Play(path)"), GuideCell.words("Plays a recording out of that speaker "
                        + "alone."
                        + " Gives true when it plays."))
                .paragraph("Beep comes out of the speaker in the machine's case. A new beep cuts off the one before "
                        + "it. Its"
                        + " pitch stays between 20 and 20,000 hertz (Hz), and its length between 1 and 60,000 "
                        + "milliseconds"
                        + " (ms).")
                .subheading("Tunes")
                .paragraph("A tune is written as items separated by spaces or commas. A tune such as C4:250 E4 G4"
                        + " C4+E4+G4:500 plays three notes and then a chord.")
                .table("Items of a tune", "Item", "What it means")
                .row(GuideCell.code("C4"), GuideCell.words("A note: a letter from A to G, then its octave from 0 to "
                        + "8."))
                .row(GuideCell.code("F#3, Bb5"), GuideCell.words("A sharp with #, or a flat with b, in a note."))
                .row(GuideCell.code("440"), GuideCell.words("A pitch in hertz, above 0 and up to 20000."))
                .row(GuideCell.code("R"), GuideCell.words("A rest: silence for the item's length."))
                .row(GuideCell.code("C4+E4+G4"), GuideCell.words("Notes played together as a chord, up to 16 notes."))
                .row(GuideCell.code(":250"), GuideCell.words("A length in milliseconds, from 1 to 60,000, after the "
                        + "item."))
                .paragraph("An item with no length lasts as long as the item before it. The first item lasts 250"
                        + " milliseconds. A tune holds at most 1,024 items.")
                .paragraph("Each note of a chord takes a voice of the sound card. A machine with no sound card plays "
                        + "only"
                        + " the first note of each chord, through the speaker in its case.")
                .subheading("Recordings")
                .paragraph("Play reads a recording from the machine's disks. It gives false when the file is not a"
                        + " recording, when the machine plays no recordings, when the volume is 0, or when the "
                        + "server does"
                        + " not keep the recording. A new Play replaces the recording the programs were playing.")
                .paragraph("A sound comes out at the machine's volume. A sound that finds no voice free takes the "
                        + "voice of"
                        + " the oldest sound.")
                .paragraph("A speaker is found by its exact name. A speaker with no name cannot be found this way.")
                .ifSomethingGoesWrong(
                        "The program stops and says that the item is not a note.",
                        "Write each item as a note, a pitch in hertz, or R for a rest.",
                        "Sound.Tones gives false.",
                        "The machine's sound is off, or the machine makes no sound. Check that its sound is on.",
                        "Sound.Play gives false.",
                        "Check that the file is a recording, that the volume is above 0, and that the server keeps the"
                                + " recording.",
                        "Speaker.Named gives null.",
                        "No speaker linked to this machine has that exact name. Check the name, because it must match"
                                + " exactly.")
                .seeAlso("jsc:sound_cards", "jsc:speakers")
                .register();

        guide.page("gateway_library", section).titled("Gateways: talking to ComputerCraft computers")
                .paragraph("A Gateway links the network to ComputerCraft computers on the wire. A program can list "
                        + "those"
                        + " computers and their peripherals, call a method of a peripheral, switch a computer on, "
                        + "off or"
                        + " reboot it, and send it a message. The block is explained in [](jsc:network_gateway).")
                .table("Gateway members", "Member", "What it does")
                .row(GuideCell.code("Gateway.Online"), GuideCell.words("Yes when this machine has a Gateway that is "
                        + "online."))
                .row(GuideCell.code("Gateway.Current"), GuideCell.words("The name of the Gateway the program chose."))
                .row(GuideCell.code("Gateway.Names()"), GuideCell.words("The names of the Gateways, as a list of "
                        + "text."))
                .row(GuideCell.code("Gateway.Select(name)"), GuideCell.words("Chooses the Gateway that later calls "
                        + "use, by"
                        + " name, in any case."))
                .row(GuideCell.code("Gateway.Computers()"), GuideCell.words("The ComputerCraft computers, as a list of"
                        + " CcComputer."))
                .row(GuideCell.code("Gateway.Peripherals()"), GuideCell.words("The peripherals, as a list of"
                        + " CcPeripheral."))
                .row(GuideCell.code("Gateway.Call(peripheral, method, args)"), GuideCell.words("Asks a peripheral to "
                        + "run a"
                        + " method, with up to three arguments after the two names, and gives back what it"
                        + " returns."))
                .row(GuideCell.code("Gateway.TurnOn(id)"), GuideCell.words("Switches on the computer with that "
                        + "number."))
                .row(GuideCell.code("Gateway.Shutdown(id)"), GuideCell.words("Switches off the computer with that "
                        + "number."))
                .row(GuideCell.code("Gateway.Reboot(id)"), GuideCell.words("Reboots the computer with that number."))
                .row(GuideCell.code("Gateway.Send(id, text)"), GuideCell.words("Sends a message to that computer."))
                .row(GuideCell.code("Gateway.OnMessage(handler)"), GuideCell.words("Sets the handler that runs for each"
                        + " message from a ComputerCraft computer."))
                .table("Records of the Gateway", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("CcComputer"), GuideCell.code("CcComputer.Id, CcComputer.Name, CcComputer.Label, "
                        + "CcComputer.Online"),
                        GuideCell.words("The number, name and label of the computer, and whether it is on."))
                .row(GuideCell.code("CcPeripheral"), GuideCell.code("CcPeripheral.Name, CcPeripheral.Type, "
                        + "CcPeripheral.Methods"),
                        GuideCell.words("The name and type of the peripheral, and the names of its methods."))
                .row(GuideCell.code("GatewayMessage"), GuideCell.code("GatewayMessage.From, GatewayMessage.Text, "
                        + "GatewayMessage.Tick"),
                        GuideCell.words("The number of the sender, the text, and the tick it arrived."))
                .paragraph("The Gateway a call uses is the one the program chose with Select. If the program chose "
                        + "none,"
                        + " the first Gateway the machine lists is used.")
                .paragraph("Send gives your message the event name jsc_message on the ComputerCraft side. A call "
                        + "across a"
                        + " Gateway costs more than most things a program does, and each argument adds to its cost.")
                .paragraph("Every call is written in the Gateway's log: called when it went through, and the refusal"
                        + " text when it did not.")
                .ifSomethingGoesWrong(
                        "The program stops and says this computer has no Gateway.",
                        "Use a machine that has a Gateway.",
                        "The program stops and says this computer has no Gateway with that name.",
                        "Check the name with Gateway.Names.",
                        "The program stops and says the machine has no ComputerCraft side, and asks whether "
                        + "ComputerCraft"
                                + " is installed.",
                        "Install ComputerCraft, then start the game again.",
                        "Gateway.Call stops the program with the error text from ComputerCraft.",
                        "The peripheral refused the call. Check the method name and the arguments in "
                        + "Gateway.Peripherals.")
                .seeAlso("jsc:network_gateway", "jsc:computercraft_peripheral")
                .register();
    }

    private static void libraryWindows(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("library_windows").titled("Windows")
                .in(chapter).register();
        guide.page("windows_and_layout", section).titled("Windows, rows and columns")
                .paragraph("A window is a box on the desktop, with a title and one widget inside it. A program makes a"
                        + " Window, puts a widget in it, and shows it. The taskbar lists the open windows. The screens"
                        + " that show them are explained in [](jsc:monitors) and [](jsc:desktops).")
                .define("Widget", "One part of a window, such as a button, a label or a list.")
                .define("Weight", "A row's or column's rule for sharing spare room among its widgets.")
                .table("Window members", "Member", "What it does")
                .row(GuideCell.code("Window.new(title, width, height)"), GuideCell.words("Makes a window with that "
                        + "title"
                        + " and size. Sigma# only."))
                .row(GuideCell.code("Window.Title"), GuideCell.words("The title on the window."))
                .row(GuideCell.code("Window.Content"), GuideCell.words("The widget the window holds."))
                .row(GuideCell.code("Window.Open"), GuideCell.words("Yes while the window is shown."))
                .row(GuideCell.code("Window.Show()"), GuideCell.words("Shows the window on the desktop and in the "
                        + "taskbar."))
                .row(GuideCell.code("Window.Close()"), GuideCell.words("Closes the window."))
                .row(GuideCell.code("Window.Add(widget, x, y, width, height)"), GuideCell.words("Places a widget at an"
                        + " exact place in the window."))
                .row(GuideCell.code("Window.OnClose"), GuideCell.words("The event that runs when the window closes."))
                .subheading("Rows and columns")
                .paragraph("A row puts its widgets side by side, and a column stacks them. A row shares its width "
                        + "among its"
                        + " widgets, and a column shares its height.")
                .table("Row and Column members", "Member", "What it does")
                .row(GuideCell.code("Row.new()"), GuideCell.words("Makes an empty row. Column.new() makes an empty "
                        + "column."))
                .row(GuideCell.code("Row.Add(widget)"), GuideCell.words("Adds a widget at its own size."))
                .row(GuideCell.code("Row.Add(widget, weight)"), GuideCell.words("Adds a widget that shares the room "
                        + "left over, by its weight (1 or more)."))
                .row(GuideCell.code("Row.Spacing"), GuideCell.words("The gap between the widgets, 4 by default."))
                .row(GuideCell.code("Row.Clear()"), GuideCell.words("Removes the widgets from the row."))
                .row(GuideCell.code("Column.Add"), GuideCell.words("The same as Row.Add, for a column."))
                .row(GuideCell.code("Column.Spacing"), GuideCell.words("The same as Row.Spacing, for a column."))
                .row(GuideCell.code("Column.Clear()"), GuideCell.words("Removes the widgets from the column."))
                .paragraph("MessageBox.Show(title, text) opens a window the machine makes for a title and one line of"
                        + " text. Sigma# only.")
                .subheading("Widgets in general")
                .paragraph("Every widget has Widget.Visible and Widget.Enabled, which are yes or no, and "
                        + "Widget.Width and Widget.Height, which are"
                        + " whole numbers. A width or height of 0 takes what the widget needs. Any other value stays"
                        + " between 8 and the most the window allows.")
                .table("Limits of windows and widgets", "Thing", "Most")
                .row(GuideCell.words("Open windows for one program"), GuideCell.words("8"))
                .row(GuideCell.words("Widgets in a window, counting those in rows and columns"), GuideCell.words("256"))
                .row(GuideCell.words("Widgets in one row or column"), GuideCell.words("256"))
                .row(GuideCell.words("Window width"), GuideCell.words("60 to 640, 240 by default"))
                .row(GuideCell.words("Window height"), GuideCell.words("60 to 360, 160 by default"))
                .ifSomethingGoesWrong(
                        "The program stops and says a program may have 8 windows open at once.",
                        "Close a window you no longer need with Window.Close(), before you show another.",
                        "The program stops and says a window holds at most 256 widgets.",
                        "Use fewer widgets. Widgets inside rows and columns count too.",
                        "The program stops and says a row or a column cannot hold itself.",
                        "Do not put a row or a column inside itself, or inside a row or column that it holds.",
                        "The program stops and says a widget cannot hold itself.",
                        "Give each widget one place in the window, and never put it inside itself.",
                        "The program stops and says a dialog opens over a window that is open.",
                        "Show the window first, then open the dialog over it.",
                        "The program stops and says this window already has a dialog open over it.",
                        "Close the open dialog first, then open the next one.",
                        "The program stops and says a value is not a program's to write.",
                        "Write only the values the program may write, such as the ones it made.",
                        "The program stops and says \"a <kind> component does not take that value\".",
                        "Check the kind of widget. Each kind takes only its own values.")
                .seeAlso("jsc:monitors", "jsc:desktops", "jsc:widgets_basic")
                .register();

        guide.page("widgets_basic", section).titled("Labels, buttons, boxes and bars")
                .paragraph("A widget is one part of a window. The simplest ones show text, and the others take what the"
                        + " player types or clicks. Making a widget costs nothing. Changing a value that the player "
                        + "sees"
                        + " costs credits, see [](jsc:call_costs).")
                .table("Basic widgets", "Widget", "Makes", "Values", "Events")
                .row(GuideCell.code("Label"), GuideCell.code("Label.new(), Label.new(text)"),
                        GuideCell.code("Label.Text"), GuideCell.words("none"))
                .row(GuideCell.code("Button"), GuideCell.code("Button.new(), Button.new(text)"),
                        GuideCell.code("Button.Text"), GuideCell.code("Button.OnClick"))
                .row(GuideCell.code("TextBox"), GuideCell.code("TextBox.new(), TextBox.new(text)"),
                        GuideCell.code("TextBox.Text"), GuideCell.code("TextBox.OnChange, TextBox.OnSubmit"))
                .row(GuideCell.code("CheckBox"), GuideCell.code("CheckBox.new(), CheckBox.new(text, checked)"),
                        GuideCell.code("CheckBox.Text, CheckBox.Checked"), GuideCell.code("CheckBox.OnToggle"))
                .row(GuideCell.code("ProgressBar"), GuideCell.code("ProgressBar.new(), ProgressBar.new(least, most)"),
                        GuideCell.code("ProgressBar.Value, ProgressBar.Least, "
                        + "ProgressBar.Most"), GuideCell.words("none"))
                .paragraph("An event tells your program that something happened, and it comes with no argument. "
                        + "OnChange"
                        + " runs on every change to a text box. OnSubmit runs when the player presses Enter in the "
                        + "box.")
                .paragraph("A text box keeps at most 256 characters of text.")
                .ifSomethingGoesWrong(
                        "The text box stops taking characters.",
                        "Shorten the text, or split it between two boxes.",
                        "The program does not react when the player presses Enter.",
                        "Listen for OnSubmit, which runs when the player presses Enter in the box.")
                .seeAlso("jsc:windows_and_layout", "jsc:widgets_lists")
                .register();

        guide.page("widgets_lists", section).titled("Lists, choices, tables, trees and menus")
                .paragraph("Lists and choices hold many entries, and the player picks one. The Selected value tells "
                        + "your"
                        + " program which one is picked. On a RadioGroup, a ComboBox or a TabView the count starts "
                        + "at 1,"
                        + " and 0 means nothing is picked on a RadioGroup or a ComboBox.")
                .define("Node", "An entry in a tree. A node can be the child of another node.")
                .define("Section", "A part of a status bar, with its own text.")
                .table("Lists and choices", "Widget", "Makes", "Members")
                .row(GuideCell.code("ListBox"), GuideCell.code("ListBox.new()"),
                        GuideCell.code("ListBox.Add(text), ListBox.Add(text, right text), ListBox.Clear, "
                        + "ListBox.Count, ListBox.Selected, ListBox.OnSelect"))
                .row(GuideCell.code("RadioGroup"), GuideCell.code("RadioGroup.new()"),
                        GuideCell.code("RadioGroup.Add, RadioGroup.Clear, RadioGroup.Count, RadioGroup.Selected, "
                        + "RadioGroup.Across, RadioGroup.OnSelect"))
                .row(GuideCell.code("ComboBox"), GuideCell.code("ComboBox.new()"),
                        GuideCell.code("ComboBox.Add, ComboBox.Clear, ComboBox.Count, ComboBox.Selected, "
                        + "ComboBox.OnSelect"))
                .row(GuideCell.code("TabView"), GuideCell.code("TabView.new()"),
                        GuideCell.code("TabView.Add(title, page), TabView.Clear, TabView.Count, TabView.Selected, "
                        + "TabView.OnSelect"))
                .paragraph("RadioGroup.Across is yes to lay the choices out across instead of down. A ComboBox is a"
                        + " drop-down and has no Across. TabView.Selected runs from 1 to the count, and the first "
                        + "page added"
                        + " is the one shown first. RadioGroup, ComboBox and TabView are version 2.")
                .table("Tables and trees", "Widget", "Makes", "Members")
                .row(GuideCell.code("Table"), GuideCell.code("Table.new()"),
                        GuideCell.code("Table.AddColumn, Table.AddRow, Table.Clear, Table.Cell, Table.Count, "
                        + "Table.Selected, Table.OnSelect"))
                .row(GuideCell.code("TreeView"), GuideCell.code("TreeView.new()"),
                        GuideCell.code("TreeView.Add, TreeView.Clear, TreeView.NodeText, TreeView.Count, "
                        + "TreeView.Selected, TreeView.OnSelect"))
                .paragraph("Table.AddRow takes a list of texts, or one to four texts. Table.Cell(row, column) gives "
                        + "the text"
                        + " in that place. TreeView.Add gives the number of the node it made, and TreeView.Add(text, "
                        + "parent)"
                        + " puts the node under another. TreeView.NodeText gives the text of a node by its number.")
                .table("Menus and bars", "Widget", "Makes", "Members")
                .row(GuideCell.code("MenuBar"), GuideCell.code("MenuBar.new()"),
                        GuideCell.code("MenuBar.Add(menu, entry), MenuBar.Clear, MenuBar.Picked, MenuBar.PickedMenu, "
                        + "MenuBar.OnPick"))
                .row(GuideCell.code("ContextMenu"), GuideCell.code("ContextMenu.new(owner)"),
                        GuideCell.code("ContextMenu.Add, ContextMenu.Clear, ContextMenu.Picked, ContextMenu.OnPick"))
                .row(GuideCell.code("StatusBar"), GuideCell.code("StatusBar.new(), StatusBar.new(text)"),
                        GuideCell.code("StatusBar.Text, StatusBar.Add, StatusBar.Clear"))
                .paragraph("A ContextMenu opens with the other mouse button on the widget it was made for. A menu "
                        + "holds 64"
                        + " entries.")
                .table("Other widgets", "Widget", "Makes", "Members")
                .row(GuideCell.code("TextArea"), GuideCell.code("TextArea.new()"), GuideCell.code("TextArea.Text, "
                        + "TextArea.OnChange"))
                .row(GuideCell.code("NumberBox"), GuideCell.code("NumberBox.new()"),
                        GuideCell.code("NumberBox.Value, NumberBox.Least, NumberBox.Most, NumberBox.Step, "
                        + "NumberBox.OnChange"))
                .row(GuideCell.code("Slider"), GuideCell.code("Slider.new()"),
                        GuideCell.code("Slider.Value, Slider.Least, Slider.Most, Slider.Step, Slider.OnChange"))
                .row(GuideCell.code("GroupBox"), GuideCell.code("GroupBox.new()"), GuideCell.code("GroupBox.Text, "
                        + "GroupBox.Content"))
                .row(GuideCell.code("ScrollView"), GuideCell.code("ScrollView.new()"),
                        GuideCell.code("ScrollView.Content"))
                .paragraph("TextArea keeps at most 4,096 characters. A list, a table, a tree or an item picker holds "
                        + "at most"
                        + " 1,024 rows. A tab view holds 64 pages, a table 8 columns, and a status bar 4 sections.")
                .ifSomethingGoesWrong(
                        "The program stops and says a list holds at most 1024 rows.",
                        "Clear the list, or show fewer rows.",
                        "The program stops and says a menu holds at most 64 entries.",
                        "Use fewer entries, or split them between two menus.",
                        "The program stops and says a tab view holds at most 64 pages.",
                        "Show fewer pages, or put some of them in another window.",
                        "The program stops and says a table has at most 8 columns.",
                        "Use 8 columns or fewer.",
                        "The program stops and says a status bar shows at most 4 sections.",
                        "Use 4 sections or fewer.",
                        "The program stops and says there is no node with that number in this tree.",
                        "Use a number that TreeView.Add gave you.",
                        "The program stops and says there is no row and column with those numbers in this table.",
                        "Check the row and column numbers against Table.Count first.")
                .seeAlso("jsc:widgets_basic", "jsc:windows_and_layout")
                .register();

        guide.page("widgets_pictures_and_data", section).titled("Canvas, images, charts, logs, items and components")
                .paragraph("Some widgets show pictures, numbers and records. A Canvas is an area you draw on, an "
                        + "Image shows"
                        + " a picture from the disk, and a Chart plots numbers. Each colour argument is a whole "
                        + "number.")
                .define("Stroke", "One drawing command on a canvas.")
                .define("Component", "A part that a mod adds to a window, with a kind name. It holds values, not "
                        + "handlers.")
                .table("Pictures, charts and logs", "Widget", "Makes", "Members")
                .row(GuideCell.code("Canvas"), GuideCell.code("Canvas.new(), Canvas.new(width, height)"),
                        GuideCell.code("Canvas.Clear(colour), Canvas.FillRect, Canvas.DrawLine, Canvas.DrawText, "
                        + "Canvas.SetPixel, Canvas.ClickX, Canvas.ClickY, "
                        + "Canvas.OnClick"))
                .row(GuideCell.code("Image"), GuideCell.code("Image.new(), "
                        + "Image.new(path)"), GuideCell.code("Image.Path"))
                .row(GuideCell.code("Chart"), GuideCell.code("Chart.new(), Chart.new(least, most)"),
                        GuideCell.code("Chart.Text, Chart.Least, Chart.Most, Chart.Add(number), Chart.Clear, "
                        + "Chart.Count"))
                .row(GuideCell.code("LogView"), GuideCell.code("LogView.new()"), GuideCell.code("LogView.Add(text), "
                        + "LogView.Clear, "
                        + "LogView.Count"))
                .paragraph("A chart holds 120 points, and when it is full the oldest"
                        + " point goes. A log view holds 256 lines, and the oldest line goes first. Image, Chart, "
                        + "LogView"
                        + " and the widgets below are version 2.")
                .table("Items and components", "Widget", "Makes", "Members")
                .row(GuideCell.code("ItemSlot"), GuideCell.code("ItemSlot.new(), ItemSlot.new(item, amount)"),
                        GuideCell.code("ItemSlot.Item, ItemSlot.Amount, ItemSlot.OnClick"))
                .row(GuideCell.code("ItemPicker"), GuideCell.code("ItemPicker.new()"),
                        GuideCell.code("ItemPicker.Add(item), ItemPicker.Clear, ItemPicker.Count, "
                        + "ItemPicker.Selected, ItemPicker.Picked, ItemPicker.OnSelect"))
                .row(GuideCell.code("OperationView"), GuideCell.code("OperationView.new()"),
                        GuideCell.code("OperationView.Show(OperationInfo), OperationView.Text, OperationView.State, "
                        + "OperationView.Done, OperationView.Total, OperationView.Computer"))
                .row(GuideCell.code("GenericComponent"), GuideCell.code("GenericComponent.new(kind)"),
                        GuideCell.code("GenericComponent.Kind, GenericComponent.Data, GenericComponent.OnAction"))
                .paragraph("An ItemSlot shows an item the way the game shows it. An ItemPicker shows a grid of "
                        + "items, with a"
                        + " search line above it. An OperationView shows one Operation as it stands, with its text, "
                        + "state,"
                        + " done and total amounts, and computer.")
                .paragraph("A GenericComponent is a kind a mod adds. Its Data is a copy of the value you give it, and"
                        + " OnAction gives you a ComponentAction, with its ComponentAction.Name and "
                        + "ComponentAction.Value. A component's value "
                        + "weighs at"
                        + " most 32 kilobytes (KB), nests at most 16 levels deep, and has at most 4,096 parts.")
                .ifSomethingGoesWrong(
                        "The canvas stops taking strokes.",
                        "A canvas holds 4,096 strokes at most. Clear it, and draw fewer strokes.",
                        "The program stops and says a component holds values, not handlers.",
                        "Give the component a value, and listen for its actions with OnAction.",
                        "The program stops and says the component's value weighs too much.",
                        "Keep the value under 32 kilobytes (KB), or split it into several components.",
                        "The program stops and says a value that holds itself cannot be handed to a component.",
                        "Build a value that does not contain itself.")
                .seeAlso("jsc:monitors", "jsc:operations_catalogue")
                .register();

        guide.page("dialogs_and_file_choice", section).titled("Dialogs: picking a file to open or save")
                .paragraph("A dialog is a small window that asks the player something. It opens over another window, "
                        + "its"
                        + " owner window, and it closes when the player answers it.")
                .paragraph("Your program makes the dialog, shows it over a window that is open, and reads the path"
                        + " the player picked.")
                .define("Dialog", "A small window that asks the player something, and opens over another window.")
                .define("Owner window", "The window a dialog sits over.")
                .table("Dialog members", "Member", "What it does")
                .row(GuideCell.code("OpenFileDialog.new(), OpenFileDialog.new(title)"),
                        GuideCell.words("Makes a dialog that asks the player to open a file. Version 2."))
                .row(GuideCell.code("SaveFileDialog.new(), SaveFileDialog.new(title)"),
                        GuideCell.words("Makes a dialog that asks the player where to save a file. Version 2."))
                .row(GuideCell.code("OpenFileDialog.Title, SaveFileDialog.Title"), GuideCell.words("The title of the "
                        + "dialog."))
                .row(GuideCell.code("OpenFileDialog.Path, SaveFileDialog.Path"), GuideCell.words("The path the "
                        + "player picked."))
                .row(GuideCell.code("OpenFileDialog.Filter, SaveFileDialog.Filter"), GuideCell.words("The filter "
                        + "text of the"
                        + " dialog."))
                .row(GuideCell.code("OpenFileDialog.Show(owner), "
                        + "SaveFileDialog.Show(owner)"), GuideCell.words("Shows the dialog over the owner "
                        + "window."))
                .row(GuideCell.code("SaveFileDialog.Name"), GuideCell.words("The file name the dialog suggests."))
                .row(GuideCell.code("OpenFileDialog.OnChoose, SaveFileDialog.OnChoose"), GuideCell.words("The event "
                        + "that runs"
                        + " when the player chooses a file."))
                .row(GuideCell.code("OpenFileDialog.OnCancel, SaveFileDialog.OnCancel"), GuideCell.words("The event "
                        + "that runs"
                        + " when the player cancels."))
                .row(GuideCell.code("MessageBox.Show(title, text)"), GuideCell.words("A window with a title and one "
                        + "line of"
                        + " text. Sigma# only."))
                .paragraph("A window holds one dialog at a time.")
                .ifSomethingGoesWrong(
                        "The program stops and says \"a dialog opens over a window that is open\", because the owner"
                                + " window is not open.",
                        "Show the owner window first, then open the dialog over it.",
                        "The program stops and says this window already has a dialog open over it.",
                        "Close the open dialog first, then open the next one.")
                .seeAlso("jsc:monitors", "jsc:windows_and_layout")
                .register();
    }
}
