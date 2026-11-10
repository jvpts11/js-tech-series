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
 * The pages of the language chapter of the Book of Sigma about version 3 of the languages: what it brings, and a page
 * for each part of it (computers/docs/design/sigma.md).
 */
final class SigmaVersionThreePages {

    private SigmaVersionThreePages() {
    }

    /** Declares the section of version 3 and its entries, in reading order, at the end of the language chapter. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_version_three_section").titled("Version 3")
                .in(chapter).register();
        overview(guide, section);
        constantsAndJumps(guide, section);
        intercalWords(guide, section);
        directives(guide, section);
        smallForms(guide, section);
        tuples(guide, section);
        extensionsAndOperators(guide, section);
        exceptions(guide, section);
        pointers(guide, section);
    }

    private static void overview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_version_three", section).titled("What version 3 brings")
                .paragraph("A project is held to a version of its language, which you choose in its settings. Version"
                        + " 3 is the newest. It brings the words and forms in the table, and a build held to"
                        + " version 2 refuses each of them where it is written, with the code S3057 or S2014.")
                .table("What version 3 brings", "What", "Sigma", "Sigma#")
                .row(GuideCell.words("const, labels and goto"), GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("comefrom, abstainfrom, reinstate, ignore and remember"),
                        GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("#if and the other directives"), GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("Pointers, sizeof and the calls of raw memory"), GuideCell.words("Yes"),
                        GuideCell.words("Yes, in unsafe code"))
                .row(GuideCell.words("unsafe and stackalloc"), GuideCell.words("No"), GuideCell.words("Yes"))
                .row(GuideCell.words("try, catch, finally and throw"), GuideCell.words("No"), GuideCell.words("Yes"))
                .row(GuideCell.words("sealed, static classes, params, local functions and nameof"),
                        GuideCell.words("No"), GuideCell.words("Yes"))
                .row(GuideCell.words("Tuples"), GuideCell.words("No"), GuideCell.words("Yes"))
                .row(GuideCell.words("Extension methods, operators and indexers"), GuideCell.words("No"),
                        GuideCell.words("Yes"))
                .paragraph("A program written for version 2 still compiles in version 3, and to the very same"
                        + " listing. The new words are words only where the language reads them, so a program that"
                        + " calls a variable unsafe or a method sizeof keeps working. The words try, catch, finally,"
                        + " throw, goto, const, comefrom, abstainfrom and reinstate were kept by the versions before,"
                        + " so no program could use them as names.")
                .paragraph("A listing that uses an instruction of version 3, such as throw or a read through a"
                        + " pointer, is written in version 4 of the listing format, which an older runtime refuses."
                        + " A program that uses none of them is written in the format it always was.")
                .define("Contextual word", "A word that means something only where the language expects it, and is a"
                        + " plain name everywhere else.")
                .ifSomethingGoesWrong(
                        "The compiler says a word needs version 3 of the language.",
                        "Hold the project to version 3 in its settings, or write the program without that word.")
                .seeAlso("jsc:sigma_constants_goto", "jsc:sigma_exceptions", "jsc:sigma_pointers")
                .register();
    }

    private static void constantsAndJumps(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_constants_goto", section).titled("const, labels and goto")
                .paragraph("const declares a value the compiler knows, which never changes. You write const before"
                        + " the type of a local or a field, and give it its value at once: const int Size = 16;. The"
                        + " value is made of literals, other consts, enum values and operators. A const field is"
                        + " static, and every place that reads it is written with the value itself.")
                .paragraph("A label names a statement. You write a name and a colon in front of it, as in done:."
                        + " Then goto done; jumps to that statement. In Sigma#, goto case 3; and goto default; jump"
                        + " to an arm of the switch around them. A jump may leave blocks and loops, but it never goes"
                        + " into a block, and nothing jumps out of a finally.")
                .listing("Retry.sg",
                        "namespace Tools;",
                        "",
                        "class Retry",
                        "{",
                        "    const int Tries = 3;",
                        "",
                        "    static void Main()",
                        "    {",
                        "        int left = Tries;",
                        "    again:",
                        "        left--;",
                        "        printf(\"%d left\\n\", left);",
                        "        if (left > 0)",
                        "        {",
                        "            goto again;",
                        "        }",
                        "    }",
                        "}")
                .paragraph("This program prints 2 left, 1 left and 0 left. A goto costs what a jump of a loop"
                        + " costs, and a const costs nothing at all: the listing holds its value.")
                .define("Label", "A name and a colon in front of a statement, which a goto jumps to.")
                .ifSomethingGoesWrong(
                        "The compiler says the const's value is one known when compiling.",
                        "Give the const a value made of literals, other consts and operators, with no call and no"
                                + " variable in it.",
                        "The compiler says a jump may leave a block, never enter one.",
                        "Move the label out of the block, to the block the goto stands in or one around it.")
                .seeAlso("jsc:sigma_intercal_words", "jsc:sigma_control_flow")
                .register();
    }

    private static void intercalWords(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_intercal_words", section)
                .titled("comefrom, abstainfrom, reinstate, ignore and remember")
                .paragraph("These five words come from INTERCAL, a language written to be strange. You can use them"
                        + " in Sigma and in Sigma#, and each costs one instruction.")
                .table("The five words", "Written as", "What it does")
                .row(GuideCell.code("comefrom label;"), GuideCell.words("When the statement the label marks ends,"
                        + " the program goes on after the comefrom instead. A label takes one comefrom."))
                .row(GuideCell.code("abstainfrom label;"), GuideCell.words("Switches off the code from the statement"
                        + " the label marks to the end of its block: from then on it is skipped. Switching off twice"
                        + " is the same as once."))
                .row(GuideCell.code("reinstate label;"), GuideCell.words("Switches that code on again."))
                .row(GuideCell.code("ignore x;"), GuideCell.words("Every later write to x vanishes, though it still"
                        + " costs its instructions. x is a local, a field or a static field."))
                .row(GuideCell.code("remember x;"), GuideCell.words("Writes to x count again."))
                .paragraph("What is switched off and what is ignored belongs to the running program, and is saved"
                        + " with it when the world is put away. Both start clean each time the program starts.")
                .listing("Odd.sg",
                        "namespace Tools;",
                        "",
                        "class Odd",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        int count = 1;",
                        "        ignore count;",
                        "        count = 99;",
                        "        remember count;",
                        "        abstainfrom loud;",
                        "        {",
                        "        loud:",
                        "            printf(\"never printed\\n\");",
                        "        }",
                        "        printf(\"%d\\n\", count);",
                        "    }",
                        "}")
                .paragraph("This program prints \"1\": the write of 99 vanished, and the block from the label loud to"
                        + " its end was switched off before it was reached.")
                .ifSomethingGoesWrong(
                        "The compiler says the label already has a comefrom.",
                        "Keep one comefrom for each label.",
                        "The compiler says only a local, a field or a static field can be ignored.",
                        "Ignore a variable or a field, not an element of an array or a value.")
                .seeAlso("jsc:sigma_constants_goto")
                .register();
    }

    private static void directives(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_directives", section).titled("#if and the symbols")
                .paragraph("A directive is a line that starts with # and speaks to the compiler, not to the machine."
                        + " With #if, a part of a file is compiled only when a symbol is defined. The lines that are"
                        + " left out are not read at all, so they may hold anything.")
                .table("The directives", "Written as", "What it does")
                .row(GuideCell.code("#if SYMBOL"), GuideCell.words("Compiles the lines up to the next #elif, #else"
                        + " or #endif when the symbol is defined. A condition joins symbols, true and false with"
                        + " &&, ||, !, == and !=, and brackets."))
                .row(GuideCell.code("#elif SYMBOL"), GuideCell.words("Another condition, asked when the ones before"
                        + " it were false."))
                .row(GuideCell.code("#else"), GuideCell.words("The lines compiled when no condition was true."))
                .row(GuideCell.code("#endif"), GuideCell.words("Ends the #if."))
                .row(GuideCell.code("#define SYMBOL, #undef SYMBOL"), GuideCell.words("Defines a symbol, or takes one"
                        + " away, from that line to the end of the file."))
                .row(GuideCell.code("#error text, #warning text"), GuideCell.words("Stops the build with that text,"
                        + " or tells it and goes on."))
                .table("The symbols the compiler defines", "Symbol", "When")
                .row(GuideCell.words("The instruction set, in capitals, as X86 for jsc:x86"),
                        GuideCell.words("Always: the machine the listing is built for."))
                .row(GuideCell.words("The era, in capitals, as VINTAGE"),
                        GuideCell.words("When the machine that builds knows its era."))
                .row(GuideCell.code("SIGMA_3"), GuideCell.words("In every build of version 3."))
                .paragraph("Your project can define its own symbols too, with a define: line in its project file."
                        + " Each directive stands on a line of its own.")
                .define("Directive", "A line that starts with #, which tells the compiler which lines to read.")
                .define("Symbol", "A name a directive asks about, which is defined or is not.")
                .ifSomethingGoesWrong(
                        "The compiler says an #if is never closed with an #endif.",
                        "Add the #endif where the part that depends on the symbol ends.",
                        "The compiler says a word is not a directive the language reads.",
                        "Use one of the directives in the table.")
                .seeAlso("jsc:sigma_version_three")
                .register();
    }

    private static void smallForms(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_small_forms", section).titled("sealed, static classes, params, local functions and nameof")
                .paragraph("Version 3 brings five small forms of C# to Sigma#. Each saves you some writing, and none"
                        + " of them changes how the program runs.")
                .table("The small forms", "Written as", "What it means")
                .row(GuideCell.code("sealed class Tool"), GuideCell.words("No class stands on Tool. On an override,"
                        + " sealed says that no class further down replaces the method again."))
                .row(GuideCell.code("static class Tools"), GuideCell.words("A class of static members only, which"
                        + " is never made with new."))
                .row(GuideCell.code("int Sum(params int[] values)"), GuideCell.words("The last parameter takes its"
                        + " values one by one, as in Sum(1, 2, 3), or an array as it is."))
                .row(GuideCell.code("int Twice(int n) => n * 2;"), GuideCell.words("A local function: a method"
                        + " written inside a method, which reads the method's locals as a lambda does."))
                .row(GuideCell.code("nameof(count)"), GuideCell.words("The text of the last name written, here"
                        + " count, known when compiling. Renaming count and forgetting the text is caught."))
                .listing("Small.sgs",
                        "using System.*;",
                        "",
                        "namespace Tools;",
                        "",
                        "static class Small",
                        "{",
                        "    static int Sum(params int[] values)",
                        "    {",
                        "        int total = 0;",
                        "        for (int i = 0; i < values.Length; i++)",
                        "        {",
                        "            total += values[i];",
                        "        }",
                        "        return total;",
                        "    }",
                        "",
                        "    static void Main()",
                        "    {",
                        "        int Twice(int n) => n * 2;",
                        "        int sum = Sum(1, 2, 3);",
                        "        Console.PrintLine(nameof(sum) + \" = \" + Twice(sum));",
                        "    }",
                        "}")
                .paragraph("This program prints sum = 12.")
                .define("Local function", "A method written inside another method, which only that method calls.")
                .ifSomethingGoesWrong(
                        "The compiler says params goes on the last parameter.",
                        "Move the params parameter to the end of the list.",
                        "The compiler says a member of a static class has to be static as well.",
                        "Write static on the member, or take static off the class.")
                .seeAlso("jsc:sigma_methods_out_ref", "jsc:sigma_classes")
                .register();
    }

    private static void tuples(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_tuples", section).titled("Tuples")
                .paragraph("A tuple holds a few values together without a type of its own. You write its type as the"
                        + " types of its places in brackets, (int count, string name), and a tuple itself as its values"
                        + " in brackets, (3, \"iron\"). A tuple has from two to eight places.")
                .paragraph("You read a place by its name, or by Item1, Item2 and so on. A tuple is copied as a struct"
                        + " is: storing one or handing it over hands over a copy, and nobody disposes of one.")
                .paragraph("A tuple can be taken apart into variables. var (n, s) = Find(); declares two new"
                        + " variables, and (a, b) = (b, a); swaps two that already exist: every value is read before"
                        + " any place is written.")
                .listing("Stock.sgs",
                        "using System.*;",
                        "",
                        "namespace Tools;",
                        "",
                        "class Stock",
                        "{",
                        "    static (int count, string name) Most()",
                        "    {",
                        "        return (64, \"iron\");",
                        "    }",
                        "",
                        "    static void Main()",
                        "    {",
                        "        var (count, name) = Most();",
                        "        Console.PrintLine(name + \": \" + count);",
                        "        (int, int) pair = (1, 2);",
                        "        (pair.Item1, pair.Item2) = (pair.Item2, pair.Item1);",
                        "        Console.PrintLine(\"\" + pair.Item1);",
                        "    }",
                        "}")
                .paragraph("This program prints \"iron: 64\", and then \"2\".")
                .define("Tuple", "A few values held together, each in a place of its own, with no type written for"
                        + " them.")
                .ifSomethingGoesWrong(
                        "The compiler says a tuple has from two to eight places.",
                        "Keep between two and eight values, or write a struct for more.",
                        "The compiler says a value cannot be taken apart into that many places.",
                        "Write as many places as the tuple has.")
                .seeAlso("jsc:sigma_classes")
                .register();
    }

    private static void extensionsAndOperators(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_extensions_operators", section).titled("Extension methods, operators and indexers")
                .paragraph("An extension method adds a call to a type you did not write. It is a static method of a"
                        + " static class at the top of a namespace, and its first parameter is written with this, as"
                        + " in static int Doubled(this int n). Any program that brings in that namespace with using"
                        + " can then write count.Doubled() for an int count. A method the type has of its own, with"
                        + " the same name, wins.")
                .paragraph("Your own types can give the operators a meaning. An operator is a public static method"
                        + " written with the word operator and its sign: public static Money operator +(Money a, Money"
                        + " b). The comparisons come in pairs, so a type that declares == declares != as well, and <"
                        + " goes with >.")
                .paragraph("An indexer lets you read and write an object with square brackets, as an array. You write"
                        + " this[...] with a get and a set: public int this[int i] { get { ... } set { ... } }. An"
                        + " indexer takes one value in its brackets.")
                .listing("Money.sgs",
                        "using System.*;",
                        "",
                        "namespace Shop;",
                        "",
                        "struct Money",
                        "{",
                        "    public long Cents;",
                        "    public static Money operator +(Money a, Money b)",
                        "    {",
                        "        Money sum = new Money();",
                        "        sum.Cents = a.Cents + b.Cents;",
                        "        return sum;",
                        "    }",
                        "}",
                        "",
                        "static class Prices",
                        "{",
                        "    public static Money Cents(this long n)",
                        "    {",
                        "        Money made = new Money();",
                        "        made.Cents = n;",
                        "        return made;",
                        "    }",
                        "}",
                        "",
                        "class Till",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        long price = 150;",
                        "        long tip = 25;",
                        "        Money total = price.Cents() + tip.Cents();",
                        "        Console.PrintLine(\"\" + total.Cents);",
                        "    }",
                        "}")
                .paragraph("This program prints \"175\".")
                .define("Extension method", "A static method that reads as a call on the value of its first"
                        + " parameter.")
                .define("Indexer", "The methods behind square brackets on an object of your own type.")
                .ifSomethingGoesWrong(
                        "The compiler says a type that declares == declares != as well.",
                        "Write the partner of every comparison you declare, taking the same values.",
                        "The call of an extension method finds nothing.",
                        "Bring in the namespace of its static class with using.")
                .seeAlso("jsc:sigma_classes", "jsc:sigma_namespaces_using")
                .register();
    }

    private static void exceptions(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_exceptions", section).titled("Exceptions: try, catch, finally and throw")
                .paragraph("From version 3, Sigma# has exceptions. throw stops the step it is in and throws an"
                        + " exception, an object of the type Exception or of a class standing on it. The program then"
                        + " looks for a catch that takes it, first in the method that threw and then in each method"
                        + " that called it. Sigma has no exceptions.")
                .paragraph("A try protects a block. Each catch after it names a type of exception and takes the ones"
                        + " of that type, or of a type standing on it; the first that fits runs. A finally runs"
                        + " whichever way the block is left: at its end, by a return, a break or a jump, or by an"
                        + " exception. A throw written alone in a catch throws on the exception that catch took.")
                .table("The exceptions the language has", "Type", "Thrown when")
                .row(GuideCell.code("NullReferenceException"), GuideCell.words("A name that holds nothing is used"
                        + " as if it held an object."))
                .row(GuideCell.code("ObjectDisposedException"), GuideCell.words("An object is used after its"
                        + " dispose."))
                .row(GuideCell.code("DivideByZeroException"), GuideCell.words("A whole number is divided by zero."))
                .row(GuideCell.code("IndexOutOfRangeException"), GuideCell.words("An index is outside its array."))
                .row(GuideCell.code("ArgumentException, ArgumentOutOfRangeException"), GuideCell.words("A call is"
                        + " handed a value it cannot take."))
                .row(GuideCell.code("InvalidOperationException"), GuideCell.words("A step the program cannot take"
                        + " now, such as a cast to a type the object is not."))
                .row(GuideCell.code("FormatException"), GuideCell.words("Text is not the number it was read as."))
                .row(GuideCell.code("IOException, FileNotFoundException"), GuideCell.words("A file cannot be read or"
                        + " written, or is not there."))
                .row(GuideCell.code("NetworkException"), GuideCell.words("The computer is not on a network."))
                .row(GuideCell.code("AccessViolationException"), GuideCell.words("A read or a write through a pointer"
                        + " lands outside every block of raw memory."))
                .paragraph("Every exception has a Message, the File and the Line it was thrown at, a Trace of the"
                        + " calls it went through, and the Inner exception it was made with. A halt of the language,"
                        + " such as a division by zero, is thrown as its exception in a program that uses exceptions;"
                        + " running out of memory and calling too deep are thrown nothing.")
                .paragraph("An exception that nothing catches stops your program, which writes its message and its"
                        + " trace to the console. When a program is stopped with sigma stop, the Task Manager or kill,"
                        + " its finally blocks run first; kill -9 and a machine that loses power run nothing. A throw"
                        + " costs 50 instructions, and 5 more for each call it leaves.")
                .listing("Parse.sgs",
                        "using System.*;",
                        "",
                        "namespace Tools;",
                        "",
                        "class Parse",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        try",
                        "        {",
                        "            int n = Convert.ToInt(\"seven\");",
                        "            Console.PrintLine(\"\" + n);",
                        "        }",
                        "        catch (FormatException failed)",
                        "        {",
                        "            Console.PrintLine(\"not a number: \" + failed.Message);",
                        "        }",
                        "        finally",
                        "        {",
                        "            Console.PrintLine(\"done\");",
                        "        }",
                        "    }",
                        "}")
                .define("Exception", "An object thrown when a step fails, which a catch somewhere along the calls can"
                        + " take and handle.")
                .define("Finally", "A block that runs whichever way the block it follows is left.")
                .ifSomethingGoesWrong(
                        "The compiler says a catch before this one already takes every exception of that type.",
                        "Put the catch of the narrower type first, and the one of Exception last.",
                        "The compiler says nothing leaves a finally but its own end.",
                        "Take the return, break or jump out of the finally, and do it after the try.",
                        "The program stops and says an exception was thrown and nothing caught it.",
                        "Catch it where the program can go on, or check the value before the step.")
                .seeAlso("jsc:sigma_no_exceptions", "jsc:sigma_runtime_stops")
                .register();
    }

    private static void pointers(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_pointers", section).titled("Pointers and raw memory")
                .paragraph("A pointer holds the address of a value in raw memory: bytes the program asked for, with no"
                        + " object around them. int* is a pointer to an int. A pointer points at an int, a long, a"
                        + " byte, a struct that holds only values, or at nothing in particular with void*. The type"
                        + " byte, a whole number from 0 to 255, comes with pointers.")
                .paragraph("In Sigma#, pointers are held only in unsafe code: a method written unsafe, or a block"
                        + " written unsafe { ... }. A Sigma file is unsafe throughout, as C was.")
                .table("What a pointer does", "Written as", "What it means")
                .row(GuideCell.code("&x"), GuideCell.words("The address of x: a local, a parameter, or a place a"
                        + " pointer reaches. From then on, x lives in raw memory until its method returns."))
                .row(GuideCell.code("*p"), GuideCell.words("The value p points at, to read or to write."))
                .row(GuideCell.code("p->Count"), GuideCell.words("A field of the struct p points at."))
                .row(GuideCell.code("p[i]"), GuideCell.words("The value i places after the one p points at."))
                .row(GuideCell.code("p + n, p - n, p++"), GuideCell.words("The pointer moved by n values of what it"
                        + " points at."))
                .row(GuideCell.code("p - q"), GuideCell.words("How many values lie between two pointers, as a long."))
                .row(GuideCell.code("p == q, p < q"), GuideCell.words("Compares two addresses. null points at"
                        + " nothing."))
                .row(GuideCell.code("(byte*) p"), GuideCell.words("The same address, read as another pointer."))
                .row(GuideCell.code("stackalloc int[n]"), GuideCell.words("Room for n values, freed when the method"
                        + " returns."))
                .row(GuideCell.code("sizeof(Cell)"), GuideCell.words("How many bytes a value of the type takes."))
                .table("The calls of raw memory", "Sigma#", "Sigma", "What it does")
                .row(GuideCell.code("Memory.Alloc(bytes)"), GuideCell.code("malloc(bytes)"),
                        GuideCell.words("A new block of that many bytes."))
                .row(GuideCell.code("Memory.AllocZeroed(count, size)"), GuideCell.code("calloc(count, size)"),
                        GuideCell.words("A new block for count values of that size."))
                .row(GuideCell.code("Memory.Resize(p, bytes)"), GuideCell.code("realloc(p, bytes)"),
                        GuideCell.words("A block of the new size, holding what fits of the old one, which is"
                                + " freed."))
                .row(GuideCell.code("Memory.Free(p)"), GuideCell.code("free(p)"),
                        GuideCell.words("Gives the block back. Freeing null does nothing."))
                .row(GuideCell.code("Memory.Copy(dest, src, bytes)"), GuideCell.code("memcpy(dest, src, bytes)"),
                        GuideCell.words("Copies bytes from src to dest."))
                .row(GuideCell.code("Memory.Fill(p, value, bytes)"), GuideCell.code("memset(p, value, bytes)"),
                        GuideCell.words("Sets every byte to the value."))
                .row(GuideCell.code("Memory.Compare(a, b, bytes)"), GuideCell.code("memcmp(a, b, bytes)"),
                        GuideCell.words("Below zero, zero or above zero, by the first byte that differs."))
                .paragraph("Every block of raw memory starts as zeros and knows its size. A read or a write outside"
                        + " every live block, past the end of one, in one already freed, or through null, is a"
                        + " violation. In Sigma# it throws an AccessViolationException. In Sigma the program dies with"
                        + " its system's own words, such as Segmentation fault (core dumped) on Linux and the BSDs.")
                .paragraph("The bytes count exactly in the program's memory. A read or a write through a pointer"
                        + " costs one instruction, Alloc and Free cost what new and dispose cost, and Copy, Fill and"
                        + " Compare cost one more for every 64 bytes.")
                .listing("Cells.sg",
                        "namespace Tools;",
                        "",
                        "struct Cell",
                        "{",
                        "    public int Value;",
                        "    public Cell* Next;",
                        "}",
                        "",
                        "class Cells",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        Cell* first = null;",
                        "        for (int i = 1; i <= 3; i++)",
                        "        {",
                        "            Cell* made = calloc(1, sizeof(Cell));",
                        "            made->Value = i;",
                        "            made->Next = first;",
                        "            first = made;",
                        "        }",
                        "        int total = 0;",
                        "        for (Cell* at = first; at != null; at = at->Next)",
                        "        {",
                        "            total += at->Value;",
                        "        }",
                        "        printf(\"%d\\n\", total);",
                        "    }",
                        "}")
                .paragraph("This program prints \"6\". A struct with a pointer field is written in Sigma, which is"
                        + " unsafe throughout; in Sigma#, a pointer lives in the locals and parameters of unsafe code.")
                .define("Pointer", "A value that holds the address of another value in raw memory.")
                .define("Raw memory", "Bytes a program asked for, with no object around them, read and written"
                        + " through pointers.")
                .ifSomethingGoesWrong(
                        "The compiler says a pointer is held only in unsafe code.",
                        "Write unsafe on the method, or put the lines in an unsafe block.",
                        "The program stops with Segmentation fault, or throws an AccessViolationException.",
                        "A pointer read past the end of its block, or a block already freed. Check the index and"
                                + " the size you asked for, and free each block once, after its last use.")
                .seeAlso("jsc:sigma_exceptions", "jsc:sigma_dispose")
                .register();
    }
}
