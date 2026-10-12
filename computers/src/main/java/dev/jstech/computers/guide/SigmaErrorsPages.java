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
 * Pages of the Book of Sigma that explain every error a Sigma program can meet while it is compiled and while it
 * runs: how to read an error line, the compile errors by code, the listing errors and the stops.
 */
final class SigmaErrorsPages {

    private SigmaErrorsPages() {
    }

    /** Declares the sections and entries of the Sigma errors, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        errorsReading(guide, chapter);
        errorsCompile(guide, chapter);
        errorsListing(guide, chapter);
        errorsRunning(guide, chapter);
    }

    private static void errorsReading(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("errors_reading").titled("Reading errors")
                .in(chapter).register();
        guide.page("sigma_reading_an_error", section).titled("Reading an error line")
                .paragraph("When you compile a Sigma program, the compiler checks every file of the program and lists"
                        + " each mistake it finds. A compile is one run of the compiler over your files, and the"
                        + " result is either a listing the machine can run or a list of mistakes. Each mistake takes"
                        + " one line, and that line tells you where it is, what kind it is and what it is about.")
                .table("Parts of an error line", "Part", "What it tells you")
                .row(GuideCell.words("The file name"),
                        GuideCell.words("The source file that holds the mistake, under the name you saved it."))
                .row(GuideCell.words("(12,9)"),
                        GuideCell.words("The line and the column, both counted from 1 at the top left of the file."))
                .row(GuideCell.words("error"),
                        GuideCell.words("The kind of line. Every line the compiler gives you is an error."))
                .row(GuideCell.code("S2001"),
                        GuideCell.words("The error code. It names the kind of mistake, and it is the same in every"
                                + " language."))
                .row(GuideCell.words("The message"),
                        GuideCell.words("What the mistake is, in words, with the names it found in it."))
                .paragraph("For example, a mistake in a file called Monitor.sgs can read like this:"
                        + " \"Monitor.sgs(12,9): error S2001: expected ';' but found '}'\". The compiler expected a"
                        + " semicolon at line 12, column 9, and found a closing brace instead.")
                .paragraph("A mistake in a listing has the same shape, but no file name: \"(4,1): error A4003:"
                        + " 'nonsense' is not an instruction\". Read it the same way. The listing errors have their"
                        + " own page: [listing errors](jsc:sigma_listing_errors).")
                .paragraph("Read the list from the top and fix the first line first. A mistake early in a file can"
                        + " cause the lines after it, so the rest of the list can change when you fix it. The list"
                        + " is sorted by file, then by line, then by column.")
                .paragraph("The compiler lists at most 100 mistakes for each file. When a file has more, the last"
                        + " line of the list reads \"too many errors; the rest were not reported\", and you fix the"
                        + " ones above it before you compile again.")
                .paragraph("The editor's problem list, see [the editors](jsc:sigma_editors), groups the problems by"
                        + " file. The file with the most problems comes first, so the list starts with the file that"
                        + " needs the most work.")
                .define("compile", "Turning the source text of a program into a listing that a machine can run.")
                .define("listing", "The text file the compiler writes from a program, with the steps the machine"
                        + " runs. Its file name ends in asm.")
                .ifSomethingGoesWrong("The message says \"Monitor.sgs does not compile\" when you start the program.",
                        "The file failed to compile, so it has no listing to run. Open it in the editor, fix its"
                                + " first problem and compile it again.")
                .seeAlso("jsc:sigma_compile_errors")
                .register();
    }

    private static void errorsCompile(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("errors_compile").titled("When it is built")
                .in(chapter).register();
        guide.page("sigma_compile_errors", section).titled("How the compiler reads a file")
                .paragraph("The compiler does not check a file all at once. It goes through a few steps in order, and"
                        + " each step can find mistakes. A mistake in one step can hide the mistakes of the steps"
                        + " after it, so the list can change as you fix things.")
                .table("The steps of a build", "Step", "What it checks")
                .row(GuideCell.words("Reading the text"),
                        GuideCell.words("The characters and the words of each file, one piece at a time."))
                .row(GuideCell.words("Grammar"),
                        GuideCell.words("The shape of each line: its brackets, its keywords and its punctuation."))
                .row(GuideCell.words("Language rules"),
                        GuideCell.words("Whether every feature the file uses exists in its language level."))
                .row(GuideCell.words("Meaning"),
                        GuideCell.words("What the names and the types in the file stand for."))
                .row(GuideCell.words("Listing"),
                        GuideCell.words("The listing is written only when no step found a mistake."))
                .paragraph("The compiler checks meaning only when the earlier steps found no mistake at all. So a"
                        + " file with a syntax error shows its syntax errors first, and its meaning errors appear"
                        + " after you have fixed them. The editor checks a file while you type, and it is looser: it"
                        + " can report meaning errors while a line is still broken. Treat those as less certain, and"
                        + " check them again once the syntax errors are gone.")
                .table("The two language levels", "Level", "Mark", "Source file ends in", "What it has")
                .row(GuideCell.words("Sigma"), GuideCell.words("Σ"), GuideCell.code("sg"),
                        GuideCell.words("The smaller subset, which the oldest machines take."))
                .row(GuideCell.words("Sigma#"), GuideCell.words("Σ#"), GuideCell.code("sgs"),
                        GuideCell.words("Everything the language has."))
                .paragraph("The compiler builds a program for the oldest instruction set that runs every instruction"
                        + " the program uses. If you ask for a newer instruction set, the compiler keeps your choice.")
                .paragraph("The compiler reads at most 128 levels of nesting, which means blocks inside blocks inside"
                        + " blocks. Deeper than that, it reports code S2013 and does not read the rest of the file.")
                .define("syntax error", "A mistake in the shape of the text: a missing bracket, a word where a"
                        + " symbol belongs, or a line that is not finished.")
                .define("build", "One run of the compiler over a program: it gives a listing, or the messages when"
                        + " the program has mistakes.")
                .define("meaning", "What the names and the types of a program stand for, checked once its shape is"
                        + " right.")
                .define("instruction set", "The machine instructions a computer's processor runs. Each computer has"
                        + " one, and a program is built for one.")
                .ifSomethingGoesWrong("The list changed after you fixed one line, and new mistakes appeared.",
                        "That is expected: the steps hide each other's mistakes. Fix the first line of the new list,"
                                + " then compile again.",
                        "The compiler gives you a list of mistakes and no listing.",
                        "The listing is written only when no mistake is left. Fix every line in the list, then"
                                + " compile again.")
                .seeAlso("jsc:sigma_syntax_errors", "jsc:sigma_semantic_errors", "jsc:sigma_build_errors")
                .register();
        guide.page("sigma_syntax_errors", section).titled("Syntax errors")
                .paragraph("A syntax error is a mistake in the shape of the text. The compiler cannot read the line"
                        + " as a statement, a declaration or a value, because a piece is missing, extra or in the"
                        + " wrong place. The codes below are the mistakes you can see on the line they name.")
                .paragraph("The compiler cuts the text into tokens first. A token is one piece of text: a word, a"
                        + " number, a string or a symbol. A message shows a token in quotes, such as ';'. A token"
                        + " with no single spelling is shown by its kind: an integer, a long integer, a float, a"
                        + " double, a string, a character, a name, or the end of the file.")
                .define("token", "One piece of text that the compiler reads as a single unit: a word, a number, a"
                        + " string or a symbol.")
                .table("Codes while reading the text", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S1001"),
                        GuideCell.words("the string was never closed before the end of the line"),
                        GuideCell.words("A string has no closing quote on its line. Close it on the same line."))
                .row(GuideCell.code("S1002"),
                        GuideCell.words("the comment was never closed before the end of the file"),
                        GuideCell.words("A comment has no closing mark. Add the closing mark."))
                .row(GuideCell.code("S1003"),
                        GuideCell.words("'<character>' does not begin anything the language knows"),
                        GuideCell.words("The character is not part of the language. Remove it or replace it."))
                .row(GuideCell.code("S1004"),
                        GuideCell.words("'<text>' is not a number the language can read"),
                        GuideCell.words("A number has letters or a second dot stuck to it, or a value too big for its"
                                + " type. Write the number again."))
                .row(GuideCell.code("S1005"),
                        GuideCell.words("a character literal holds exactly one character"),
                        GuideCell.words("Two single quotes hold no character, or more than one. Keep exactly one"
                                + " character between them."))
                .row(GuideCell.code("S1006"),
                        GuideCell.words("'\\<character>' is not an escape the language knows"),
                        GuideCell.words("A backslash is followed by a character that is not an escape. Use an"
                                + " escape the language has, or remove the backslash."))
                .table("Codes while reading the grammar", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S2001"),
                        GuideCell.words("expected <token> but found <token>"),
                        GuideCell.words("The grammar needs one piece and finds another. Put the missing piece where"
                                + " the message says."))
                .row(GuideCell.code("S2002"),
                        GuideCell.words("expected a type but found <token>"),
                        GuideCell.words("A type name must stand here. Write the name of a type."))
                .row(GuideCell.code("S2003"),
                        GuideCell.words("expected an expression but found <token>"),
                        GuideCell.words("A value must stand here, for example between the empty braces of an"
                                + " interpolated string. Write the value."))
                .row(GuideCell.code("S2004"),
                        GuideCell.words("expected a field, a method, a property or an event but found <token>"),
                        GuideCell.words("A line inside a type is none of these four. Write one of them, or remove"
                                + " the line."))
                .row(GuideCell.code("S2005"),
                        GuideCell.words("expected a class, a struct, a record, an interface, an enum or a delegate"
                                + " but found <token>"),
                        GuideCell.words("Where a type should start, the line starts with something else. Start the"
                                + " type with one of these words."))
                .row(GuideCell.code("S2006"),
                        GuideCell.words("only a call, an assignment, an increment, a decrement or a new object can"
                                + " be used as a statement"),
                        GuideCell.words("A line holds a value that does nothing, such as a + 1. Assign it, print it,"
                                + " or delete it."))
                .row(GuideCell.code("S2007"),
                        GuideCell.words("'<word>' was given twice"),
                        GuideCell.words("The same modifier stands twice, such as public public. Keep one of them."))
                .row(GuideCell.code("S2008"),
                        GuideCell.words("the left side of an assignment must be a variable, a field, a property or"
                                + " an element"),
                        GuideCell.words("The left side of the = sign must be a variable, a field, a property or an"
                                + " element. Put one of these there."))
                .row(GuideCell.code("S2009"),
                        GuideCell.words("a using has to come before the namespace and the types"),
                        GuideCell.words("A using line sits too low in the file. Move it above the namespace."))
                .row(GuideCell.code("S2010"),
                        GuideCell.words("a file declares one namespace on its own line, before its types; put more"
                                + " of them in blocks"),
                        GuideCell.words("The file has more than one namespace line. Keep one on its own line and"
                                + " write the others in blocks."))
                .row(GuideCell.code("S2011"),
                        GuideCell.words("every type is in a namespace: put 'namespace Name;' at the top of the file"),
                        GuideCell.words("A type stands with no namespace. Write a namespace line at the top of the"
                                + " file."))
                .row(GuideCell.code("S2013"),
                        GuideCell.words("this is nested more than 128 levels deep, which is more than the compiler"
                                + " reads; the rest of the file was not read"),
                        GuideCell.words("There are too many blocks inside blocks. Split the deep part into smaller"
                                + " parts."))
                .row(GuideCell.code("S2014"),
                        GuideCell.words("'<word>' is kept for a later version of the language; for now,"
                                + " <alternative>"),
                        GuideCell.words("The file writes try, catch, finally, throw, goto or const. A later version"
                                + " of the language brings them, so they are kept already, and no name can be one"
                                + " of them. Until then, write what the message says: there are no exceptions, so"
                                + " a mistake halts the program and a call that can fail answers instead."))
                .ifSomethingGoesWrong("It says \"expected ';' but found '}'\".",
                        "The line before the brace does not end with a semicolon. Add the semicolon at its end.",
                        "It says \"only a call, an assignment, an increment, a decrement or a new object can be used"
                                + " as a statement\".",
                        "The line holds a value that does nothing. Assign it, print it, or delete it.",
                        "It says every type is in a namespace.",
                        "Write a namespace line at the top of the file, with a name you choose.",
                        "It says this is nested more than 128 levels deep.",
                        "Split the deep part into smaller parts. The rest of the file is not read until then.")
                .seeAlso("jsc:sigma_compile_errors")
                .register();
        guide.page("sigma_semantic_errors", section).titled("Meaning errors")
                .paragraph("A meaning error is a line whose shape is right but whose names or types do not fit."
                        + " The compiler checks meaning once the shape of the file is clean, so these codes usually"
                        + " appear after the syntax errors are gone. The editor's check while you type is the"
                        + " exception.")
                .paragraph("The words in angle brackets stand for what the compiler found in your file: a name, a"
                        + " type, a member of a type, a number or a token. Your message shows the real words in"
                        + " their place.")
                .define("hole", "A place inside a text where a value is put in: the braces of an interpolated"
                        + " string, or a marker of a printf format that the format fills with a value.")
                .table("Names and declarations", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S2012"),
                        GuideCell.words("'<name>' names a namespace; write 'using <name>.*;' to bring in everything"
                                + " in it, or name one of its types"),
                        GuideCell.words("A namespace was named where a type is needed. Write the using line with a"
                                + " star, or name one type of the namespace."))
                .row(GuideCell.code("S3001"),
                        GuideCell.words("'<name>' does not name anything here"),
                        GuideCell.words("The name is not declared where you use it. Check its spelling, or declare"
                                + " it above this line."))
                .row(GuideCell.code("S3002"),
                        GuideCell.words("'<name>' is already declared here"),
                        GuideCell.words("Two things have the same name in one place. Give one of them another"
                                + " name."))
                .row(GuideCell.code("S3040"),
                        GuideCell.words("'<name>' is in <namespace>; add 'using <namespace>.*;' or 'using"
                                + " <namespace>.<name>;' at the top of the file"),
                        GuideCell.words("The name lives in a namespace you did not bring in. Add the using line the"
                                + " message gives, at the top of the file."))
                .row(GuideCell.code("S3059"),
                        GuideCell.words("'<name>' already stands behind '<name>', so it cannot also stand in front"
                                + " of it"),
                        GuideCell.words("Two types each name the other as their base, in a circle. Remove the base"
                                + " that closes the circle."))
                .row(GuideCell.code("S3060"),
                        GuideCell.words("'<name>' is <private or protected> to '<type>', so it cannot be reached"
                                + " from here"),
                        GuideCell.words("The member is private to its type, which is what a member written with no"
                                + " word of access is, or protected to it and the types that stand on it. Mark it"
                                + " public, or reach it from inside its type."))
                .row(GuideCell.code("S3031"),
                        GuideCell.words("only a class or an interface can stand behind the colon, and '<name>' is"
                                + " neither"),
                        GuideCell.words("A type takes a base after its colon that is not a class or an interface."
                                + " Name a class or an interface there."))
                .row(GuideCell.code("S3041"),
                        GuideCell.words("a struct can implement interfaces but stands on no class, and '<name>' is a"
                                + " class"),
                        GuideCell.words("A struct names a class as its base, but a struct can only implement"
                                + " interfaces. Remove the class, or make the type a class."))
                .row(GuideCell.code("S3015"),
                        GuideCell.words("'<name>' has no base class"),
                        GuideCell.words("The program uses a base-class member where its type has no base class."
                                + " Check the type, or remove that use."))
                .row(GuideCell.code("S3057"),
                        GuideCell.words("'<name>' needs <level> <version>; this project is <level> <version>"),
                        GuideCell.words("A name is newer than the language version your project holds to. Raise the"
                                + " project's version, or do not use the name."))
                .row(GuideCell.code("S3028"),
                        GuideCell.words("'<name>' does not name a type"),
                        GuideCell.words("This message is no longer shown, so you should not meet it. If you do,"
                                + " write the name of a type in that place."))
                .table("Types and conversions", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S3003"),
                        GuideCell.words("cannot convert '<type>' to '<type>'"),
                        GuideCell.words("A value of one type is used where another type is needed, with no safe"
                                + " conversion. Write a value of the right type."))
                .row(GuideCell.code("S3019"),
                        GuideCell.words("'<name>' takes <count> type arguments"),
                        GuideCell.words("A generic type got the wrong number of types. Give it the count the message"
                                + " says."))
                .row(GuideCell.code("S3039"),
                        GuideCell.words("an enum's number has to be written as a number"),
                        GuideCell.words("An enum value gives a number that is not written as a plain number. Write"
                                + " the number itself."))
                .row(GuideCell.code("S3009"),
                        GuideCell.words("a condition is a bool, not '<type>'"),
                        GuideCell.words("An if or a loop tests something that is not true or false. Write a test that"
                                + " gives true or false."))
                .row(GuideCell.code("S3022"),
                        GuideCell.words("only an object can be disposed, not '<type>'"),
                        GuideCell.words("Dispose works on objects only. Dispose an object, not a plain value."))
                .row(GuideCell.code("S3042"),
                        GuideCell.words("only an object can be locked, not '<type>'"),
                        GuideCell.words("Lock works on objects only. Lock an object."))
                .row(GuideCell.code("S3023"),
                        GuideCell.words("foreach walks an array or a list, not '<type>'"),
                        GuideCell.words("A foreach loop needs an array or a list. Give it one of them."))
                .table("Operators, calls and members", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S3007"),
                        GuideCell.words("'<operator>' cannot be applied to '<type>' and '<type>'"),
                        GuideCell.words("The two values do not work together with this operator. Use values of a"
                                + " type the operator takes."))
                .row(GuideCell.code("S3008"),
                        GuideCell.words("'<operator>' cannot be applied to '<type>'"),
                        GuideCell.words("The operator does not take this type. Use a value of a type it takes."))
                .row(GuideCell.code("S3024"),
                        GuideCell.words("'<type>' cannot be indexed by '<type>'"),
                        GuideCell.words("The index is the wrong kind of value for this type. Give an index of the"
                                + " kind it takes."))
                .row(GuideCell.code("S3025"),
                        GuideCell.words("'<name>' is not something that can be called"),
                        GuideCell.words("You called a name that is not a method or a delegate. Check the spelling,"
                                + " or call a method."))
                .row(GuideCell.code("S3026"),
                        GuideCell.words("'<type>' cannot be made with new"),
                        GuideCell.words("new was used with a type that cannot be made that way. Make an object of a"
                                + " type that can."))
                .row(GuideCell.code("S3050"),
                        GuideCell.words("'<type>' is abstract, so it cannot be made with new"),
                        GuideCell.words("An abstract type has no objects of its own. Make an object of a type that"
                                + " is not abstract."))
                .row(GuideCell.code("S3005"),
                        GuideCell.words("no version of '<name>' takes those arguments"),
                        GuideCell.words("The values you give fit no version of the method. Give values of the types"
                                + " one version takes."))
                .row(GuideCell.code("S3006"),
                        GuideCell.words("the call to '<name>' fits more than one version of it"),
                        GuideCell.words("More than one version fits the values. Give a value of a type that fits only"
                                + " one version."))
                .row(GuideCell.code("S3004"),
                        GuideCell.words("'<type>' has no member called '<name>'"),
                        GuideCell.words("The name is not a member of that type. Check the spelling, or use a member"
                                + " the type has."))
                .row(GuideCell.code("S3020"),
                        GuideCell.words("'<member>' belongs to the type, not to one of its objects"),
                        GuideCell.words("Write the type's name before the dot, not the name of one of its objects."))
                .row(GuideCell.code("S3021"),
                        GuideCell.words("'<member>' belongs to an object, not to the type"),
                        GuideCell.words("A member of an object is used through the type's name. Use an object of the"
                                + " type."))
                .row(GuideCell.code("S3014"),
                        GuideCell.words("'<name>' is not available in a static member"),
                        GuideCell.words("A static member has no object, so this value does not exist there. Use a"
                                + " value that does not need an object."))
                .row(GuideCell.code("S3033"),
                        GuideCell.words("'<name>' is a method: call it, or hand it over where a delegate of its shape"
                                + " is wanted"),
                        GuideCell.words("A method is used as a value without its brackets. Call it, or give it where"
                                + " a delegate is expected."))
                .row(GuideCell.code("S3027"),
                        GuideCell.words("this lambda does not have the shape '<shape>' takes"),
                        GuideCell.words("The lambda's values do not match the delegate it is given to. Make it take"
                                + " the values the delegate takes."))
                .table("Loops, switches and returns", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S3029"),
                        GuideCell.words("this switch already has that label"),
                        GuideCell.words("Two cases of the switch have the same label. Keep one of them."))
                .row(GuideCell.code("S3010"),
                        GuideCell.words("'break' only means something inside a loop or a switch"),
                        GuideCell.words("break stands outside any loop or switch. Move it inside one, or delete it."))
                .row(GuideCell.code("S3011"),
                        GuideCell.words("'continue' only means something inside a loop"),
                        GuideCell.words("continue stands outside any loop. Move it inside a loop, or delete it."))
                .row(GuideCell.code("S3012"),
                        GuideCell.words("a method that gives back '<type>' must return a value"),
                        GuideCell.words("The method ends without giving back a value. Return a value of that type."))
                .row(GuideCell.code("S3013"),
                        GuideCell.words("a method that gives back nothing cannot return a value"),
                        GuideCell.words("The method returns a value, but it gives back nothing. Remove the value"
                                + " after return."))
                .table("Out, ref and events", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S3034"),
                        GuideCell.words("'<name>' is filled in by the method, so the argument is written with out"),
                        GuideCell.words("The method fills in this argument. Write out before it."))
                .row(GuideCell.code("S3035"),
                        GuideCell.words("'<name>' is read by the method, so the argument cannot be written with out"),
                        GuideCell.words("The method only reads this argument. Remove out."))
                .row(GuideCell.code("S3036"),
                        GuideCell.words("an out argument is exactly '<type>', with no conversion on the way"),
                        GuideCell.words("The out variable must be exactly the type the method fills. Declare it with"
                                + " that type."))
                .row(GuideCell.code("S3037"),
                        GuideCell.words("'<name>' is filled in by this method, so it must be given a value before"
                                + " every way out"),
                        GuideCell.words("One way out of the method leaves the out value empty. Give it a value before"
                                + " every return."))
                .row(GuideCell.code("S3038"),
                        GuideCell.words("'<name>' is not somewhere a method can write"),
                        GuideCell.words("The method can only write into a place. Give it a place, not a plain value."))
                .row(GuideCell.code("S3030"),
                        GuideCell.words("an event can only be raised inside the type that declares it"),
                        GuideCell.words("The event is raised from outside its type. Raise it inside the type."))
                .row(GuideCell.code("S3032"),
                        GuideCell.words("an event's type must be a delegate, and '<type>' is not one"),
                        GuideCell.words("An event needs a delegate type. Give it a delegate."))
                .row(GuideCell.code("S3058"),
                        GuideCell.words("'ref' is written only for strcat, which joins onto the text '<name>' holds;"
                                + " a method fills in with out"),
                        GuideCell.words("ref is only for joining text. Use out for a method that fills a value in."))
                .table("Overrides, abstract and modifiers", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S3016"),
                        GuideCell.words("'<name>' is readonly, so it can only be written where it is declared or in a"
                                + " constructor"),
                        GuideCell.words("A readonly value is written only where it is declared or in a constructor."
                                + " Move the write there."))
                .row(GuideCell.code("S3043"),
                        GuideCell.words("'<method>' is already on '<type>', so write it with override to replace it"),
                        GuideCell.words("The method replaces one from a base type. Write override before it."))
                .row(GuideCell.code("S3044"),
                        GuideCell.words("'<method>' on '<type>' is not virtual, so nothing can replace it; mark it"
                                + " virtual there"),
                        GuideCell.words("The base method cannot be replaced. Mark it virtual in its own type."))
                .row(GuideCell.code("S3045"),
                        GuideCell.words("'<method>' is written with override but nothing above '<type>' has it"),
                        GuideCell.words("override names a method that no base type has. Remove override, or check the"
                                + " name."))
                .row(GuideCell.code("S3046"),
                        GuideCell.words("'<method>' has no body, so '<type>' has to be declared abstract"),
                        GuideCell.words("A method without a body makes its type abstract. Declare the type abstract."))
                .row(GuideCell.code("S3047"),
                        GuideCell.words("'<method>' is abstract, so it is written with a semicolon and no body"),
                        GuideCell.words("An abstract method ends with a semicolon and has no body. Remove the body."))
                .row(GuideCell.code("S3048"),
                        GuideCell.words("'<method>' has no body, so it is written with abstract"),
                        GuideCell.words("A method without a body must be abstract. Write abstract before it."))
                .row(GuideCell.code("S3049"),
                        GuideCell.words("'<type>' is not abstract, so it has to give '<method>' a body"),
                        GuideCell.words("A type that is not abstract must give a body to each abstract member it"
                                + " has. Give the method a body."))
                .row(GuideCell.code("S3018"),
                        GuideCell.words("'<type>' says it is a '<kind>' but does not have '<member>'"),
                        GuideCell.words("The type says it implements something but misses one of its members. Write"
                                + " the missing member."))
                .row(GuideCell.code("S3051"),
                        GuideCell.words("'<name>' cannot be written with <modifier>"),
                        GuideCell.words("The modifier does not fit this kind of member. Remove it."))
                .table("The entry point and the subset", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S3017"),
                        GuideCell.words("a program needs exactly one place to start, either a class that implements"
                                + " IScript or a class with a static Main, and this one has <count>"),
                        GuideCell.words("The program has no place to start, or more than one. Keep exactly one class"
                                + " that implements IScript or has a static Main."))
                .row(GuideCell.code("S3052"),
                        GuideCell.words("Sigma has no <feature>; <alternative>"),
                        GuideCell.words("A Sigma file uses a feature only Sigma# has. Write the file in Sigma#, or use"
                                + " the alternative the message names."))
                .table("Printf formats", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S3053"),
                        GuideCell.words("<name>'s format has to be written out where it is used, in quotes, so its"
                                + " holes can be read"),
                        GuideCell.words("A printf format is kept in a name. Write the format as text in quotes, where"
                                + " it is used."))
                .row(GuideCell.code("S3054"),
                        GuideCell.words("<name>: <problem>"),
                        GuideCell.words("The format text is wrong. The second part of the message says what is wrong"
                                + " with it."))
                .row(GuideCell.code("S3055"),
                        GuideCell.words("<name>: the format has <count> and the call gives <count>"),
                        GuideCell.words("The format has a different number of holes than the call has values. Give"
                                + " one value for each hole."))
                .row(GuideCell.code("S3056"),
                        GuideCell.words("<name>: '%<letter>' takes <type>, and this is <type>"),
                        GuideCell.words("A hole takes one kind of value, and the call gives another. Give a value of"
                                + " the kind the hole takes."))
                .ifSomethingGoesWrong("It says '<name>' does not name anything here.",
                        "The name is spelled wrong, or it is declared after this line. Check its spelling, or"
                                + " declare it above.",
                        "It says cannot convert '<type>' to '<type>'.",
                        "Write the value as the type the message names, or change the type of the place it goes.",
                        "It says no version of '<name>' takes those arguments.",
                        "Give the values the method takes, in the order it takes them.",
                        "It says a program needs exactly one place to start.",
                        "Keep exactly one class that implements IScript or has a static Main.")
                .seeAlso("jsc:sigma_compile_errors")
                .register();
        guide.page("sigma_build_errors", section).titled("Build refusals")
                .paragraph("The compiler can also refuse a build for two reasons that have nothing to do with the"
                        + " mistakes of your text. The first is a feature the compiler does not build yet. The second"
                        + " is a target machine that runs only the oldest instruction set.")
                .table("Refusals while building", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("S4011"),
                        GuideCell.words("a string with holes that was never reduced is not built yet"),
                        GuideCell.words("The compiler does not build a text with values put inside it yet. Write the"
                                + " text another way."))
                .row(GuideCell.code("S4011"),
                        GuideCell.words("a lambda inside another one that keeps its variable is not built yet"),
                        GuideCell.words("The compiler does not build a lambda nested in another one yet. Write it"
                                + " without the nesting."))
                .row(GuideCell.code("S4011"),
                        GuideCell.words("a lambda that keeps a variable a loop declares is not built yet"),
                        GuideCell.words("The compiler does not build a lambda that keeps a loop's variable yet."
                                + " Rewrite the lambda so it does not keep that variable."))
                .row(GuideCell.code("S4012"),
                        GuideCell.words("IA-16 runs Σ only; write it in Σ, in a Σ project"),
                        GuideCell.words("A Sigma# program asked for the IA-16 instruction set, the oldest one. Write"
                                + " the program in Sigma, in a Sigma project."))
                .paragraph("The message of S4012 is shown at line 1, column 1 of the file.")
                .ifSomethingGoesWrong("It says a lambda is not built yet, or a text is not built yet.",
                        "The compiler does not support that form yet. Write the lambda or the text another way.",
                        "It says IA-16 runs Σ only.",
                        "Your Sigma# program targets the oldest instruction set, IA-16, which runs only Sigma. Write it"
                                + " in Sigma, in a Sigma project.")
                .seeAlso("jsc:sigma_compile_errors")
                .register();
    }

    private static void errorsListing(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("errors_listing").titled("Listing errors")
                .in(chapter).register();
        guide.page("sigma_listing_errors", section).titled("Listing errors")
                .paragraph("A listing is the text file the compiler writes for a program. The machine reads the listing"
                        + " when the program starts, and a listing problem is a line it cannot read. A listing problem"
                        + " stops the program from starting, and the machine shows you the first problem only.")
                .paragraph("The machine shows the program's name, a colon, and then the problem line. For example:"
                        + " \"<program>: (4,1): error A4003: 'nonsense' is not an instruction\". A listing problem"
                        + " always shows column 1.")
                .paragraph("Listing problems come from three causes: a listing you edited by hand, a listing made by"
                        + " another compiler version, and a machine that does not run the instruction set"
                        + " the program was built for. A listing that the editor builds cleanly shows no listing"
                        + " problem.")
                .paragraph("The runtime reads listing versions 2 and 3. A listing with no version line gives A4001,"
                        + " and the reader stops there. A version line with no number is read as the newest version."
                        + " After a version problem, the reader reads no further, so that one problem is the only one"
                        + " shown.")
                .define("label", "A name written in front of a line of a method, which a jump can point to.")
                .define("directive", "A line that starts with a dot, such as .arch, .field or .method, and names a"
                        + " part of the program.")
                .define("operand", "The thing written after an instruction's name, such as a number or a name.")
                .define("place", "One numbered slot in a method, where it keeps one value while it runs.")
                .define("slots line", "The line that says how many places a method has.")
                .table("Problems in the text of the listing", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("A4001"),
                        GuideCell.words("the listing has to begin with a version line"),
                        GuideCell.words("The first line is not a version line. Compile the source again."))
                .row(GuideCell.code("A4002"),
                        GuideCell.words("this runtime reads assembly version <version>, and this listing is version"
                                + " <version>"),
                        GuideCell.words("The listing came from a newer compiler than this runtime reads, so it needs a"
                                + " runtime that reads its version."))
                .row(GuideCell.code("A4003"),
                        GuideCell.words("'<name>' is not an instruction"),
                        GuideCell.words("The name is not an instruction of the listing format. Check the spelling, or"
                                + " compile the source again."))
                .row(GuideCell.code("A4004"),
                        GuideCell.words("'<name>' needs something after it"),
                        GuideCell.words("An instruction needs an operand and has none. Write the operand, or compile"
                                + " the source again."))
                .row(GuideCell.code("A4005"),
                        GuideCell.words("'<name>' takes nothing after it"),
                        GuideCell.words("An instruction has an operand it does not take. Remove the operand."))
                .row(GuideCell.code("A4006"),
                        GuideCell.words("'<text>' is not what '<name>' takes"),
                        GuideCell.words("The operand is the wrong kind of value for that instruction or directive."))
                .row(GuideCell.code("A4007"),
                        GuideCell.words("'<name>' is not a line this format has"),
                        GuideCell.words("A line starts with a dot and names no line the format has. Check the"
                                + " spelling."))
                .row(GuideCell.code("A4008"),
                        GuideCell.words("an instruction has to be inside a method"),
                        GuideCell.words("An instruction line stands outside any method. Put it inside a method."))
                .row(GuideCell.code("A4009"),
                        GuideCell.words("'<name>' has to be inside a type"),
                        GuideCell.words("A directive stands outside any type. Put it inside a type."))
                .row(GuideCell.code("A4010"),
                        GuideCell.words("nothing in this method is labelled '<name>'"),
                        GuideCell.words("A jump points to a label that the method does not have. Check the spelling,"
                                + " or add the label."))
                .row(GuideCell.code("A4012"),
                        GuideCell.words("this listing is assembly version <version> and this runtime reads version"
                                + " <version> and later: compile its source again"),
                        GuideCell.words("The listing is older than the runtime reads. Compile the source again with"
                                + " the current compiler."))
                .row(GuideCell.code("A4016"),
                        GuideCell.words("this method has no slots line, so it has no body"),
                        GuideCell.words("The method does not say how many places it has. Compile the source again."))
                .row(GuideCell.code("A4017"),
                        GuideCell.words("place <place> does not exist: this method has <count>"),
                        GuideCell.words("An instruction uses a place the method does not have. Use a place inside the"
                                + " count the message gives."))
                .row(GuideCell.code("A4018"),
                        GuideCell.words("'<name>' labels more than one line of this method"),
                        GuideCell.words("Two lines of one method have the same label. Keep one label for each line."))
                .table("Problems with the program and the machine", "Code", "Message", "What it means and the fix")
                .row(GuideCell.code("A4013"),
                        GuideCell.words("nothing answers '<name>'"),
                        GuideCell.words("A call, a new object or a field names something the machine does not have."
                                + " Check the spelling of the name."))
                .row(GuideCell.code("A4014"),
                        GuideCell.words("'<name>' can be read but not written"),
                        GuideCell.words("The program writes to a property that can only be read. Read its value, and"
                                + " do not write to it."))
                .row(GuideCell.code("A4015"),
                        GuideCell.words("built for <instruction set>; this machine is <instruction set>"),
                        GuideCell.words("The program was built for another instruction set than this machine runs, so"
                                + " build it for this machine's instruction set or run it on a machine that runs"
                                + " that set."))
                .paragraph("The problem with A4015 is different. The listing is fine, and the machine is the wrong"
                        + " one. The names that A4013 refers to are the ones described in"
                        + " [the library](jsc:sigma_library).")
                .ifSomethingGoesWrong("The machine shows an A4001 problem, a missing version line.",
                        "A listing edited by hand can lose its version line. Compile the source again.",
                        "The machine shows an A4002 problem, a listing newer than the runtime reads.",
                        "The listing needs a runtime that reads its version. Use a runtime that does.",
                        "The machine shows an A4012 problem, a listing older than the runtime reads.",
                        "Compile the source again with the current compiler.",
                        "The machine shows an A4015 problem, which says the program was built for another instruction"
                                + " set.",
                        "Build the program for this machine's instruction set, or run it on a machine that runs it.")
                .seeAlso("jsc:sigma_runtime_stops")
                .register();
    }

    private static void errorsRunning(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("errors_running").titled("While it runs")
                .in(chapter).register();
        guide.page("sigma_runtime_stops", section).titled("The stops while a program runs")
                .paragraph("A program can stop while it runs, when it meets an error it cannot go on from. This book"
                        + " calls that kind of stop a halt, and there are 13 of them. The program ends its run at"
                        + " once.")
                .define("halt", "An error during a program's run, which stops the program at once and writes one"
                        + " message to its console.")
                .paragraph("When a halt happens, the program's threads stop and the program lets go of its locks."
                        + " The message is all you see of the stop. Most messages do not say which line of the program"
                        + " caused it.")
                .paragraph("The table below numbers the 13 stops, so you can find each one. The game shows you only"
                        + " the message, not the number. In this chapter a program's memory is counted in megabytes"
                        + " (MB).")
                .table("The 13 stops", "Stop", "What causes it", "What to do")
                .row(GuideCell.words("1"),
                        GuideCell.words("A division or a remainder by zero, with whole numbers or with real numbers."),
                        GuideCell.words("Check the number you divide by before the division."))
                .row(GuideCell.words("2"),
                        GuideCell.words("A value is nothing where an object, a list, a map, an array, a thread or a"
                                + " process is needed."),
                        GuideCell.words("Make sure the value is set before you use it."))
                .row(GuideCell.words("3"),
                        GuideCell.words("An object was disposed and then used again."),
                        GuideCell.words("Do not use an object after it is disposed."))
                .row(GuideCell.words("4"),
                        GuideCell.words("The program holds more memory than it was given."),
                        GuideCell.words("Make it hold fewer or smaller values, or give it more memory when it starts,"
                                + " up to 64 MB."))
                .row(GuideCell.words("5"),
                        GuideCell.words("A conversion gets a text or a value of the wrong kind."),
                        GuideCell.words("Check the text you convert: a number conversion needs a number, and a true or"
                                + " false conversion needs true or false."))
                .row(GuideCell.words("6"),
                        GuideCell.words("A number, a place or a length is outside the range it may have."),
                        GuideCell.words("Use a value inside the limit the message gives."))
                .row(GuideCell.words("7"),
                        GuideCell.words("A name is wrong for that kind of value: the value has no such member."),
                        GuideCell.words("Use a member that the value has. The message names the value and the member."))
                .row(GuideCell.words("8"),
                        GuideCell.words("A program calls the network, but the computer is not on one."),
                        GuideCell.words("Join the computer to a network first."))
                .row(GuideCell.words("9"),
                        GuideCell.words("The thread lets go of a lock it does not hold."),
                        GuideCell.words("Take the lock before you let it go."))
                .row(GuideCell.words("10"),
                        GuideCell.words("The machine cannot start the program: no runner exists for that kind of file,"
                                + " there is not enough free memory, or the file cannot be read or started."),
                        GuideCell.words("The message says why. Compile a source file first, free some memory on the"
                                + " machine, or check the file."))
                .row(GuideCell.words("11"),
                        GuideCell.words("A value given to a component breaks a rule, or a computer is powered off."),
                        GuideCell.words("The message gives the rule. Keep the value within it, and check the file or"
                                + " the computer."))
                .row(GuideCell.words("12"),
                        GuideCell.words("A method calls itself without end, so the calls go too deep."),
                        GuideCell.words("Give the recursion an end, or use a loop. The message names the method."))
                .row(GuideCell.words("13"),
                        GuideCell.words("The runtime itself failed while it ran a step of the program."),
                        GuideCell.words("This is not your program's fault. Report the line from the server log."))
                .paragraph("In the messages below, the words in angle brackets stand for the names and the numbers the"
                        + " game fills in.")
                .table("The messages of the stops", "Stop", "The message as the player sees it")
                .row(GuideCell.words("1"), GuideCell.words("divided by zero"))
                .row(GuideCell.words("1"), GuideCell.words("took the remainder of a division by zero"))
                .row(GuideCell.words("2"), GuideCell.words("there is no handler to call"))
                .row(GuideCell.words("2"), GuideCell.words("there is no list here"))
                .row(GuideCell.words("2"), GuideCell.words("there is no map here"))
                .row(GuideCell.words("2"), GuideCell.words("there is no array here"))
                .row(GuideCell.words("2"), GuideCell.words("there is no object to write <field> on"))
                .row(GuideCell.words("2"), GuideCell.words("there is nothing here to reach into"))
                .row(GuideCell.words("2"), GuideCell.words("there is no body to run on the thread"))
                .row(GuideCell.words("2"), GuideCell.words("there is no handler to call for <name>"))
                .row(GuideCell.words("2"), GuideCell.words("there is no <thing> here to <action>"))
                .row(GuideCell.words("2"), GuideCell.words("there is no process here"))
                .row(GuideCell.words("2"), GuideCell.words("there is no thread here"))
                .row(GuideCell.words("3"), GuideCell.words("this was disposed and cannot be used"))
                .row(GuideCell.words("4"),
                        GuideCell.words("out of memory: <live> of <total> bytes are live and <asked> more were asked"
                                + " for"))
                .row(GuideCell.words("4"),
                        GuideCell.words("line <line> holds <bytes> bytes, one such part for each line that holds"
                                + " memory, joined by semicolons"))
                .row(GuideCell.words("5"), GuideCell.words("this is not a <type>"))
                .row(GuideCell.words("5"), GuideCell.words("there is nothing here to make a <type>"))
                .row(GuideCell.words("5"), GuideCell.words("'<text>' is not a number"))
                .row(GuideCell.words("5"), GuideCell.words("'<text>' is not true or false"))
                .row(GuideCell.words("6"),
                        GuideCell.words("there is no base <base>; a number is written in a base from 2 to 36"))
                .row(GuideCell.words("6"), GuideCell.words("an array cannot have <count> places"))
                .row(GuideCell.words("6"), GuideCell.words("a program may have <count> windows open at once"))
                .row(GuideCell.words("6"),
                        GuideCell.words("there is no place <place> to start from in a string of <length>"))
                .row(GuideCell.words("6"),
                        GuideCell.words("there are no <count> characters from place <place> in a string of <length>"))
                .row(GuideCell.words("6"), GuideCell.words("a window holds at most <count> widgets"))
                .row(GuideCell.words("6"),
                        GuideCell.words("a window holds at most <count> widgets, counting the ones inside rows and"
                                + " columns"))
                .row(GuideCell.words("6"), GuideCell.words("a row or a column holds at most <count> widgets"))
                .row(GuideCell.words("6"), GuideCell.words("a list holds at most <count> rows"))
                .row(GuideCell.words("6"), GuideCell.words("a canvas holds at most <count> strokes"))
                .row(GuideCell.words("6"), GuideCell.words("a menu holds at most <count> entries"))
                .row(GuideCell.words("6"), GuideCell.words("a tab view holds at most <count> pages"))
                .row(GuideCell.words("6"), GuideCell.words("a table has at most <count> columns"))
                .row(GuideCell.words("6"), GuideCell.words("a status bar shows at most <count> sections"))
                .row(GuideCell.words("7"), GuideCell.words("the runtime does not answer for <name>"))
                .row(GuideCell.words("7"), GuideCell.words("there is no <name> to call"))
                .row(GuideCell.words("7"), GuideCell.words("<name> has no body to run"))
                .row(GuideCell.words("7"), GuideCell.words("<name> has no <member>"))
                .row(GuideCell.words("7"), GuideCell.words("there is no <member> to read here"))
                .row(GuideCell.words("7"), GuideCell.words("<operator> does not apply to a real number"))
                .row(GuideCell.words("7"), GuideCell.words("<name> has no <member> to run"))
                .row(GuideCell.words("7"),
                        GuideCell.words("this computer has no desktop to open a window on"))
                .row(GuideCell.words("7"), GuideCell.words("there is no <name> to run on the thread"))
                .row(GuideCell.words("8"), GuideCell.words("this computer is not on a network"))
                .row(GuideCell.words("9"), GuideCell.words("this thread is letting go of a lock it does not hold"))
                .row(GuideCell.words("10"),
                        GuideCell.words("<name>: nothing installed runs a program of this kind (compile a source file"
                                + " first)"))
                .row(GuideCell.words("10"),
                        GuideCell.words("<name>: <size> MB will not fit in <free> MB of free memory"))
                .row(GuideCell.words("10"), GuideCell.words("<name> cannot run programs"))
                .row(GuideCell.words("11"), GuideCell.words("a component's value nests at most <count> deep"))
                .row(GuideCell.words("11"), GuideCell.words("a component's value has at most <count> parts"))
                .row(GuideCell.words("11"), GuideCell.words("a component's value weighs at most <count> bytes"))
                .row(GuideCell.words("11"), GuideCell.words("a component holds values, not handlers"))
                .row(GuideCell.words("11"), GuideCell.words("a component holds values, and a <type> is not one"))
                .row(GuideCell.words("11"),
                        GuideCell.words("a value that holds itself cannot be handed to a component"))
                .row(GuideCell.words("11"), GuideCell.words("'<name>' is closed"))
                .row(GuideCell.words("11"),
                        GuideCell.words("'<name>' was opened with \"<mode>\", which does not read"))
                .row(GuideCell.words("11"),
                        GuideCell.words("'<name>' was opened with \"<mode>\", which does not write"))
                .row(GuideCell.words("11"), GuideCell.words("'<text>' is no printf hole this machine writes"))
                .row(GuideCell.words("11"), GuideCell.words("<name> is powered off"))
                .row(GuideCell.words("11"), GuideCell.words("<name> does not take programs from other computers"))
                .row(GuideCell.words("12"),
                        GuideCell.words("calls went <count> deep calling <name>: a method may be calling itself without"
                                + " end"))
                .row(GuideCell.words("13"), GuideCell.words("the runtime could not carry this out (<exception>)"))
                .paragraph("Stops 10 and 11 can also show a message that the machine writes itself, for a bus, a"
                        + " crafting interface, a file or an [IQL](jsc:iql_getting_started) query. IQL, the Item Query"
                        + " Language, is the network's query language. Those messages come from the part of the"
                        + " machine that ran the step, so they are not in the table.")
                .paragraph("Stop 13 is the one that is not your program's fault. The runtime failed, and a line goes to"
                        + " the server log with the line of the program where it happened.")
                .table("Limits", "Limit", "Amount")
                .row(GuideCell.words("Calls deep on one thread"), GuideCell.words("1,024 frames"))
                .row(GuideCell.words("Stack places in one thread"), GuideCell.words("1,048,576 places"))
                .row(GuideCell.words("Memory of one program, by default"), GuideCell.amount(1, "MB"))
                .row(GuideCell.words("Memory of one program, at most"), GuideCell.amount(64, "MB"))
                .row(GuideCell.words("Nesting of a value given to a component"), GuideCell.words("16 levels"))
                .row(GuideCell.words("Parts of a value given to a component"), GuideCell.words("4,096 parts"))
                .row(GuideCell.words("Size of a value given to a component"), GuideCell.words("32,768 bytes"))
                .row(GuideCell.words("Windows open at once in one program"), GuideCell.words("8 windows"))
                .row(GuideCell.words("Widgets in one window"), GuideCell.words("256 widgets"))
                .paragraph("The stack places are counted as the frames times the places each frame uses. The memory"
                        + " you ask for is kept between 1 MB and 64 MB.")
                .define("frame", "One active method call on a thread. A thread keeps one frame for each call that"
                        + " has not returned yet.")
                .ifSomethingGoesWrong("It says divided by zero.",
                        "Check the number you divide by, before the division.",
                        "It says out of memory.",
                        "Make the program hold fewer or smaller values, or give it more memory when it starts, up to"
                                + " 64 MB.",
                        "It says calls went deep, and a method may be calling itself without end.",
                        "Give the calling method an end, or use a loop instead.",
                        "It says a text is not a number, or not true or false.",
                        "The text you convert is not a number, nor true or false. Check the input.",
                        "It says a number, a place or a length is out of range, such as \"a window holds at most 256"
                                + " widgets\".",
                        "Use a value inside the limit the message gives.",
                        "It says there is no such member, or the runtime does not answer for a name.",
                        "The name is wrong for that kind of value. Use a member the value has.",
                        "It says this computer is not on a network.",
                        "Join the computer to a network first.",
                        "It says this thread is letting go of a lock it does not hold.",
                        "Take the lock before you let it go.",
                        "It says nothing installed runs a program of this kind.",
                        "Compile the source file first, so that a listing exists to run.",
                        "It says the runtime could not carry this out.",
                        "This is not your program's fault. Report the line from the server log.")
                .seeAlso("jsc:sigma_listing_errors", "jsc:sigma_compile_errors", "jsc:sigma_running")
                .register();
    }
}
