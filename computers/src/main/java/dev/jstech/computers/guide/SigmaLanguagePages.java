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
 * The pages of the language chapter of the Book of Sigma: the statements, the types, the methods, the classes, the
 * namespaces, the entry points of a program, the text and the collections, the failures and the old-style names.
 */
final class SigmaLanguagePages {

    private SigmaLanguagePages() {
    }

    /** Declares the sections and entries of the language chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        basics(guide, chapter);
        methods(guide, chapter);
        objects(guide, chapter);
        organising(guide, chapter);
        data(guide, chapter);
        failures(guide, chapter);
        oldStyle(guide, chapter);
    }

    private static void basics(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_basics").titled("The basics").in(chapter)
                .register();
        commentsNamesLiterals(guide, section);
        typesAndVariables(guide, section);
        operators(guide, section);
        controlFlow(guide, section);
    }

    private static void commentsNamesLiterals(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_comments_names_literals", section).titled("Comments, names and literals")
                .paragraph("A program is a list of statements. A statement is one instruction, such as storing a"
                        + " number in a variable or calling a method. Most statements end with a semicolon. Before"
                        + " you write one, you need the small pieces that statements are built from: comments,"
                        + " names, keywords and literals.")
                .paragraph("A comment is a note for the people who read the program, and the machine skips it. A"
                        + " line comment starts with two slashes and runs to the end of the line. A block comment"
                        + " starts with a slash and a star, and it ends at the first star and slash after that. Block"
                        + " comments do not nest: you cannot put one inside another.")
                .paragraph("A name is a word you choose for a variable, a method or a type. It starts with a letter"
                        + " or an underscore, and then holds letters, digits or underscores. Any letter counts, so"
                        + " accented and Greek letters are allowed in names. The keywords below are reserved: you"
                        + " cannot use them as names.")
                .table("Keywords, by group", "Group", "Keywords")
                .row(GuideCell.words("Types"),
                        GuideCell.code("int long float double bool char string object void var"))
                .row(GuideCell.words("Declarations"),
                        GuideCell.code("namespace using class interface enum struct record delegate event"))
                .row(GuideCell.words("Modifiers and actions"),
                        GuideCell.code("new dispose this base static virtual override abstract public private"
                                + " protected internal readonly"))
                .row(GuideCell.words("Statements"),
                        GuideCell.code("if else while for foreach do switch case default break continue return"
                                + " lock in out"))
                .row(GuideCell.words("Values and tests"), GuideCell.code("true false null is as"))
                .row(GuideCell.words("Kept for a later version"), GuideCell.code("try catch finally throw goto const"))
                .paragraph("Sigma refuses some of these words, even though the language knows them. The table"
                        + " \"What Sigma has, and what Sigma# adds\" shows which features each language has. The"
                        + " words kept for a later version are refused in both languages until that version brings"
                        + " them, and none of them can be a name.")
                .paragraph("A literal is a value written out in the program, such as the number 42, the number 0.5,"
                        + " true, or a text in double quotes. Each literal has a type, and the table shows which one."
                        + " Numbers are written in plain decimal digits. A number with an exponent, such as 1e5, or"
                        + " with underscores, such as 1_000, is not a number the language can read.")
                .table("Literals", "Written as", "Type", "What it means")
                .row(GuideCell.code("42"), GuideCell.code("int"),
                        GuideCell.words("A whole number, up to 2,147,483,647."))
                .row(GuideCell.code("42L"), GuideCell.code("long"),
                        GuideCell.words("A whole number with an L after it, for bigger numbers."))
                .row(GuideCell.code("0.5"), GuideCell.code("double"),
                        GuideCell.words("A number with a dot. Without a suffix, it is a double."))
                .row(GuideCell.code("3.14f"), GuideCell.code("float"),
                        GuideCell.words("A number with an f after it."))
                .row(GuideCell.code("2d"), GuideCell.code("double"),
                        GuideCell.words("A whole number with a d after it, written as a double."))
                .row(GuideCell.code("true, false"), GuideCell.code("bool"),
                        GuideCell.words("The two truth values."))
                .row(GuideCell.code("'A'"), GuideCell.code("char"),
                        GuideCell.words("Exactly one character, between single quotes."))
                .row(GuideCell.code("\"text\""), GuideCell.code("string"),
                        GuideCell.words("Any text between double quotes, on one line."))
                .row(GuideCell.code("null"), GuideCell.words("none"),
                        GuideCell.words("Nothing. It fits any type that holds objects."))
                .paragraph("Inside a text or a character, a backslash starts an escape. The letter after the"
                        + " backslash says what goes into the text, as the table shows.")
                .table("Escapes", "Letter after a backslash", "Puts in")
                .row(GuideCell.code("n"), GuideCell.words("A new line."))
                .row(GuideCell.code("t"), GuideCell.words("A tab."))
                .row(GuideCell.code("r"), GuideCell.words("A carriage return."))
                .row(GuideCell.code("0"), GuideCell.words("The zero character."))
                .row(GuideCell.code("\\"), GuideCell.words("A backslash."))
                .row(GuideCell.code("\""), GuideCell.words("A double quote."))
                .row(GuideCell.code("'"), GuideCell.words("A single quote."))
                .paragraph("A text must close on the line where it starts. A text can also have holes filled from"
                        + " values, written with a dollar sign in front of the"
                        + " quotes, and that form belongs to Sigma# only.")
                .table("What Sigma has, and what Sigma# adds", "Feature", "Sigma", "Sigma#")
                .row(GuideCell.words("Basic types and literals: int, long, float, double, bool, char, string"),
                        GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("Arrays"), GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("if, while, do, for, switch, break, continue and return"),
                        GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("dispose"), GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("Type tests with is and as, and casts"), GuideCell.words("Yes"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("class, struct and enum"), GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("virtual, override and the Script base class"), GuideCell.words("Yes"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("abstract classes and methods"), GuideCell.words("No"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("interface and record"), GuideCell.words("No"), GuideCell.words("Yes"))
                .row(GuideCell.words("delegate, event, property and lambda"), GuideCell.words("No"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("var, which reads the type from the value"), GuideCell.words("No"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("foreach and lock"), GuideCell.words("No"), GuideCell.words("Yes"))
                .row(GuideCell.words("List, Map and types written with angle brackets"), GuideCell.words("No"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("Texts with holes, written with a dollar sign"), GuideCell.words("No"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("using System and the System library"), GuideCell.words("No"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("Old names, such as printf"), GuideCell.words("Yes"), GuideCell.words("Yes"))
                .table("Errors in names and literals", "Error", "Message", "What to do")
                .row(GuideCell.code("S1001"),
                        GuideCell.words("the string was never closed before the end of the line"),
                        GuideCell.words("Close the text with a double quote on the same line."))
                .row(GuideCell.code("S1002"),
                        GuideCell.words("the comment was never closed before the end of the file"),
                        GuideCell.words("Add the star and the slash that end the block comment."))
                .row(GuideCell.code("S1003"),
                        GuideCell.words("'#' does not begin anything the language knows"),
                        GuideCell.words("Remove the character, or put it inside a text."))
                .row(GuideCell.code("S1004"),
                        GuideCell.words("'1e5' is not a number the language can read"),
                        GuideCell.words("Write the number in plain decimal digits."))
                .row(GuideCell.code("S1005"),
                        GuideCell.words("a character literal holds exactly one character"),
                        GuideCell.words("Keep one character between the single quotes."))
                .row(GuideCell.code("S1006"),
                        GuideCell.words("'\\x' is not an escape the language knows"),
                        GuideCell.words("Use one of the escapes in the table above."))
                .define("Statement", "One instruction of a program, such as an assignment or a call. Most statements"
                        + " end with a semicolon.")
                .define("Literal", "A value written out in the program itself, such as 42 or a text in quotes.")
                .define("Keyword", "A word the language reserves for its own use, such as if or int. A keyword cannot"
                        + " be a name.")
                .define("Type", "The kind of a value: whole numbers, decimal numbers, truth values, characters, text"
                        + " or objects. The type says what a variable may hold.")
                .define("Variable", "A named place that holds a value, which the program can read and change.")
                .define("Method", "A named piece of code that the program runs each time you call it by name.")
                .ifSomethingGoesWrong(
                        "The compiler says the string was never closed before the end of the line.",
                        "Put the closing double quote on the same line as the opening one.",
                        "The compiler says '1e5' is not a number the language can read.",
                        "Write the number in plain decimal digits, such as 100000 for 1e5.")
                .seeAlso("jsc:sigma_examples", "jsc:sigma_syntax_errors")
                .register();
    }

    private static void typesAndVariables(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_types_variables", section).titled("Types and variables")
                .paragraph("A variable is a named place that holds a value. Its type says what kind of value may go in"
                        + " it, and the compiler refuses anything else. You declare a variable by writing its type,"
                        + " then its name, and then, if you want, a first value.")
                .paragraph("Each declaration names exactly one variable. Write int total = 0; to declare one; a line"
                        + " such as int a, b; is refused.")
                .table("Built-in types", "Type", "Holds", "Example")
                .row(GuideCell.code("int"), GuideCell.words("A whole number, in 32 bits."),
                        GuideCell.code("int count = 42;"))
                .row(GuideCell.code("long"), GuideCell.words("A whole number, in 64 bits, for bigger numbers."),
                        GuideCell.code("long big = 42L;"))
                .row(GuideCell.code("float"), GuideCell.words("A decimal number, shorter than a double."),
                        GuideCell.code("float f = 1.5f;"))
                .row(GuideCell.code("double"), GuideCell.words("A decimal number, with more precision."),
                        GuideCell.code("double half = 0.5;"))
                .row(GuideCell.code("bool"), GuideCell.words("true or false."), GuideCell.code("bool ok = true;"))
                .row(GuideCell.code("char"), GuideCell.words("One character. Inside the machine, its number."),
                        GuideCell.code("char letter = 'A';"))
                .row(GuideCell.code("string"), GuideCell.words("Text, which is a reference type."),
                        GuideCell.code("string text = \"hi\";"))
                .row(GuideCell.code("object"), GuideCell.words("A value of any type."),
                        GuideCell.code("object anything = 5;"))
                .paragraph("Types fall into two groups, and the group decides what happens when you copy a value. A"
                        + " value type, such as int, bool or char, is copied whenever you store it or hand it over."
                        + " A reference type, such as text or an object of a class, is shared: two names can point to"
                        + " the same object, so a change through one name shows through the other.")
                .paragraph("A text is a reference type, so it can hold null, the empty value.")
                .paragraph("Numbers go into bigger number types without asking. An int goes into a long, a float or a"
                        + " double. The other way needs a cast: you write the new type in brackets in front of the"
                        + " value, as in (int) 2.5. The cast tells the compiler that you accept the"
                        + " change. Without the cast, the line is refused.")
                .listing("Converting.sg",
                        "int total = 0;",
                        "long big = total;          // widening: fine",
                        "int back = (int) big;      // narrowing needs the cast",
                        "double avg = total / 2.0;  // int divided by double gives a double")
                .paragraph("The word null is the empty value of a reference type. It fits any type that holds objects,"
                        + " such as text or an object of a class, but it does not fit an int.")
                .paragraph("Sigma# has the word var, which reads the type from the value: var x = 5; makes x an int."
                        + " Sigma refuses var, and you write the type out instead.")
                .define("Class", "A named blueprint for objects: it lists the fields and methods that each object has.")
                .define("Object", "A value made from a class with new. It holds its own fields and runs its own"
                        + " methods.")
                .define("Reference type", "A type whose values are objects that can be shared, so two names can point"
                        + " to one object.")
                .define("Value type", "A type whose values are copied whenever they are stored or handed over.")
                .define("Null", "The empty value of a reference type, which points to no object.")
                .ifSomethingGoesWrong(
                        "The compiler says cannot convert 'double' to 'int'.",
                        "Use a cast if losing the fraction is what you want, or declare the variable as a double."
                                + " The error is explained in [the meaning errors](jsc:sigma_semantic_errors).",
                        "The compiler says 'total' does not name anything here.",
                        "Check the spelling, and check that you declared the variable above the line that uses it.",
                        "The compiler refuses var.",
                        "Sigma has no var: write the type out, as in int x = 5;")
                .seeAlso("jsc:sigma_operators", "jsc:sigma_classes")
                .register();
    }

    private static void operators(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_operators", section).titled("Operators and their order")
                .paragraph("An operator is a symbol that combines values: + adds two numbers, and == asks whether two"
                        + " values are equal. A piece of code that gives back a value, such as 2 + 3 or a name, is an"
                        + " expression. You join expressions with operators to make bigger expressions.")
                .paragraph("When one line has several operators, precedence decides which one is worked out first."
                        + " Multiplication binds tighter than addition, so 2 + 3 * 4 is 14, not 20. Parentheses"
                        + " override the order: (2 + 3) * 4 is 20.")
                .table("Operator precedence", "Group", "Operators, loosest group first")
                .row(GuideCell.words("Assignment"),
                        GuideCell.code("= += -= *= /= %= &= |= ^= <<= >>= (right to left)"))
                .row(GuideCell.words("Conditional"), GuideCell.code("?:"))
                .row(GuideCell.words("Or"), GuideCell.code("||"))
                .row(GuideCell.words("And"), GuideCell.code("&&"))
                .row(GuideCell.words("Bitwise or"), GuideCell.code("|"))
                .row(GuideCell.words("Bitwise exclusive or"), GuideCell.code("^"))
                .row(GuideCell.words("Bitwise and"), GuideCell.code("&"))
                .row(GuideCell.words("Equality"), GuideCell.code("== !="))
                .row(GuideCell.words("Comparison and type tests"),
                        GuideCell.code("< <= > >= is as"))
                .row(GuideCell.words("Shifts"), GuideCell.code("<< >>"))
                .row(GuideCell.words("Addition"), GuideCell.code("+ -"))
                .row(GuideCell.words("Multiplication"), GuideCell.code("* / %"))
                .row(GuideCell.words("Unary, before a value"),
                        GuideCell.code("! - + ~ ++ -- and casts"))
                .row(GuideCell.words("Member, call, index and after a value"),
                        GuideCell.code(". () [] ++ --"))
                .paragraph("The arithmetic operators are + for adding, - for subtracting, * for multiplying, / for"
                        + " dividing and % for the remainder. When both sides of / are whole numbers, the result is a"
                        + " whole number: the fraction is cut off, so 7 / 2 is 3, and 7 % 2 is 1. Dividing by zero, or"
                        + " taking a remainder with zero, stops the program with a message.")
                .paragraph("The + operator joins text. When one side is text, the result is text, so \"y = \" + y"
                        + " puts the number after the words. The comparison operators < <= > >= work on numbers only."
                        + " To put two texts in order, use string.Compare, as the text page explains.")
                .table("What == compares", "Kind of value", "What it compares")
                .row(GuideCell.words("Numbers"), GuideCell.words("Their values."))
                .row(GuideCell.words("bool"), GuideCell.words("Whether both are true, or both are false."))
                .row(GuideCell.words("Text"), GuideCell.words("The characters, one after another."))
                .row(GuideCell.words("Struct and record"), GuideCell.words("Each field, one after another."))
                .row(GuideCell.words("Class object"),
                        GuideCell.words("Whether both names point to the same object, not what it holds."))
                .table("Logical and bitwise operators", "Operators", "Work on")
                .row(GuideCell.code("&& || !"), GuideCell.words("bool values only."))
                .row(GuideCell.code("& | ^"), GuideCell.words("bool values, or whole numbers."))
                .row(GuideCell.code("<< >>"), GuideCell.words("Whole numbers only."))
                .row(GuideCell.code("~"), GuideCell.words("Whole numbers only."))
                .row(GuideCell.code("++ --"), GuideCell.words("Numbers only, before or after the name."))
                .paragraph("The ++ operator adds one, and -- takes one away. A line can hold only the operator, such as"
                        + " count++ on its own. Inside a bigger expression, the form after the name gives the old"
                        + " value, and the form before the name gives the new one.")
                .paragraph("The compound forms join an operator to an assignment. total += 5; does the same as"
                        + " total = total + 5;, and the same goes for the other compound forms.")
                .paragraph("A shift moves the bits of the number on its left by the count on its right, and gives a"
                        + " number of the kind on its left. It takes only the low bits of the count, as C# does: five"
                        + " for an int and six for a long. So 1 << 32 is 1, and 1 << 33 is 2.")
                .listing("Operators.sg",
                        "int x = 3;",
                        "int y = x * 2 + 1;        // 7",
                        "bool ok = y > 5 && y != 9;",
                        "int shifted = 1 << 4;     // 16",
                        "string label = \"y = \" + y;")
                .define("Expression", "A piece of code that gives back a value, such as 2 + 3 or a name.")
                .define("Operator", "A symbol that combines values, such as + or ==.")
                .define("Precedence", "The rule that decides which operator is worked out first when a line has"
                        + " several.")
                .ifSomethingGoesWrong(
                        "The compiler says '<' cannot be applied to 'string' and 'string'.",
                        "Compare texts with string.Compare, which gives a number to put in order.",
                        "The compiler says \"a condition is a bool, not 'int'\".",
                        "An if, a while or a for needs a true or false test. Write if (count > 0) instead of"
                                + " if (count).",
                        "The compiler says \"only a call, an assignment, an increment, a decrement or a new object can"
                                + " be used as a statement\".",
                        "Store the value in a variable, or pass it to a method, so the line does something.",
                        "The compiler says \"the left side of an assignment must be a variable, a field, a property or"
                                + " an element\".",
                        "Put a name on the left of the equals sign, such as total = 5;")
                .seeAlso("jsc:sigma_control_flow", "jsc:sigma_strings_chars")
                .register();
    }

    private static void controlFlow(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_control_flow", section).titled("Statements: if, loops, switch, break and return")
                .paragraph("Some statements hold other statements inside them. They decide whether the inside runs, or"
                        + " how many times it runs. Most of them test a condition: an expression that is true or"
                        + " false.")
                .paragraph("A block can stand wherever one statement can. A variable declared inside a block exists"
                        + " only until the closing brace.")
                .table("Control statements", "Statement", "Form", "Language")
                .row(GuideCell.code("if"), GuideCell.code("if (condition) statement else statement"),
                        GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("while"), GuideCell.code("while (condition) statement"),
                        GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("do"), GuideCell.code("do statement while (condition);"),
                        GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("for"), GuideCell.code("for (start; condition; step) statement"),
                        GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("foreach"), GuideCell.code("foreach (Type name in array) statement"),
                        GuideCell.words("Sigma# only"))
                .row(GuideCell.code("switch"), GuideCell.code("switch (value) with case and default labels"),
                        GuideCell.words("Sigma and Sigma#"))
                .row(GuideCell.code("lock"), GuideCell.code("lock (object) statement"),
                        GuideCell.words("Sigma# only"))
                .paragraph("The if statement runs its statement when the condition is true. The optional else runs"
                        + " its statement when the condition is false. A loop repeats a statement: while checks the"
                        + " condition before each run, and do checks it after each run. The for loop keeps a counter."
                        + " For example, for (int i = 0; i < 3; i++) runs three times, with i at 0, 1 and 2.")
                .paragraph("The foreach statement runs its statement once for each item of an array or a list, in"
                        + " order. It gives each item to a variable of your choice. Sigma refuses foreach and asks you"
                        + " to write a for over the array's Length instead.")
                .paragraph("The switch statement compares a value with each case label, and runs the section whose"
                        + " label matches. A section does not stop by itself. Without a break or a return at its end,"
                        + " the program goes on into the next section. Put break at the end of each section unless you"
                        + " want that.")
                .listing("Counting.sg",
                        "for (int i = 0; i < 3; i++)",
                        "{",
                        "    switch (i)",
                        "    {",
                        "        case 0:",
                        "            printf(\"zero\");",
                        "            break;",
                        "        default:",
                        "            printf(\"%d\", i);",
                        "            break;",
                        "    }",
                        "}")
                .paragraph("The break statement leaves the nearest loop or switch. The continue statement skips to the"
                        + " next turn of the nearest loop. The return statement ends a method. A method that gives"
                        + " back a value must return one on every way out, so the code cannot reach the end of the"
                        + " method without a return.")
                .paragraph("The lock statement, in Sigma# only, takes an object, and a number cannot be locked."
                        + " How locks are used with threads is explained in"
                        + " [Threads and locks](jsc:threads_and_locks).")
                .define("Block", "A group of statements between a pair of braces, which counts as one statement.")
                .define("Loop", "A statement that repeats another statement, such as while, do or for.")
                .define("Condition", "An expression that must give a true or false value, such as count > 0.")
                .define("Array", "A fixed row of values of one type, numbered from 0. You write its type with brackets,"
                        + " such as int[].")
                .define("List", "A growable row of values of one type, from the List type of Sigma#. Its items are"
                        + " numbered from 0 as well.")
                .ifSomethingGoesWrong(
                        "The compiler says 'break' only means something inside a loop or a switch.",
                        "Move the break inside a loop or a switch, or remove it.",
                        "The compiler says 'continue' only means something inside a loop.",
                        "Move the continue inside a loop, or remove it.",
                        "The compiler says a method that gives back 'int' must return a value.",
                        "End the method with a return that gives a value, such as return total;",
                        "The compiler says a method that gives back nothing cannot return a value.",
                        "Remove the value after return, or change the method so that it gives back a value of some"
                                + " type, such as int.")
                .seeAlso("jsc:sigma_methods_out_ref", "jsc:sigma_dispose")
                .register();
    }

    private static void methods(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_functions").titled("Methods").in(chapter)
                .register();
        methodsOutRef(guide, section);
    }

    private static void methodsOutRef(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_methods_out_ref", section).titled("Methods, out and ref")
                .paragraph("A method is a named piece of code that you call by name, often with values to work on. Its"
                        + " header says what the method gives back, its name and its parameters. Its body holds the"
                        + " statements that run. A method that gives nothing back has void as its result type.")
                .table("The forms of a method", "Form", "Meaning")
                .row(GuideCell.code("static int Twice(int x)"),
                        GuideCell.words("A static method that gives back an int, from one parameter."))
                .row(GuideCell.code("void Show(string text)"),
                        GuideCell.words("A method that gives back nothing."))
                .row(GuideCell.code("bool Split(int total, out int half)"),
                        GuideCell.words("A method with an out parameter, which the method fills in."))
                .paragraph("A static method belongs to the class itself. You call it through the name of the class, as"
                        + " Maths.Twice(4). A method without static belongs to one object, and you call it on that"
                        + " object, as till.Show(\"hi\").")
                .listing("Maths.sg",
                        "class Maths",
                        "{",
                        "    static int Twice(int x) { return x * 2; }",
                        "",
                        "    static bool Split(int total, out int half)",
                        "    {",
                        "        if (total % 2 != 0)",
                        "        {",
                        "            half = 0;",
                        "            return false;",
                        "        }",
                        "        half = total / 2;",
                        "        return true;",
                        "    }",
                        "}")
                .paragraph("The values you pass to a method are its arguments. The method receives them in its"
                        + " parameters, which are the names in its header. A value type argument is copied, so the"
                        + " method cannot change the caller's variable. An object is passed as the same object, so the"
                        + " method can change what the caller sees.")
                .paragraph("Several methods can share one name when their parameters differ. These are overloads."
                        + " The compiler picks the version whose parameters fit the arguments. A call that fits no"
                        + " version is refused, and so is a call that fits two versions equally well.")
                .paragraph("An out parameter lets a method give a second value back. The header writes out in front of"
                        + " the parameter, and the method must fill it in before every way out. The caller writes out"
                        + " in front of an existing variable of exactly the parameter's type.")
                .listing("Caller.sg",
                        "int half;",
                        "bool even = Maths.Split(10, out half);",
                        "printf(\"%d\", half);")
                .paragraph("The keyword ref is written only for strcat, which joins text onto the text that a variable"
                        + " holds. A method that needs to fill in a value uses out instead. A method may also call"
                        + " itself: each call takes room on the stack of calls, and a chain more than 1,024 calls deep"
                        + " stops the program, as the [runtime stops](jsc:sigma_runtime_stops) page explains.")
                .define("Static", "Written on a method or a field, it means the member belongs to the class, not to one"
                        + " object.")
                .define("Parameter", "A name in the header of a method, which receives the value passed to it.")
                .define("Argument", "A value you pass to a method when you call it.")
                .define("Overload", "One of several methods with the same name, told apart by their parameters.")
                .ifSomethingGoesWrong(
                        "The compiler says no version of 'Twice' takes those arguments.",
                        "Check the number of values in the call against the parameters in the header.",
                        "The compiler says the call to 'Twice' fits more than one version of it.",
                        "Give the arguments exact types, so that only one overload fits.",
                        "The compiler says 'half' is filled in by the method, so the argument is written with out.",
                        "Write out in front of the variable in the call.",
                        "The compiler says 'half' is filled in by this method, so it must be given a value before"
                                + " every way out.",
                        "Set the out parameter before each return, as Split does before it returns.",
                        "The compiler says an out argument is exactly 'int', with no conversion on the way.",
                        "Declare the variable with exactly the type of the parameter.",
                        "The compiler says 'ref' is written only for strcat.",
                        "Write out instead, so the method fills in a value that the caller then reads.")
                .seeAlso("jsc:sigma_classes", "jsc:sigma_delegates_events")
                .register();
    }

    private static void objects(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_objects").titled("Objects").in(chapter).register();
        classesStructsEnums(guide, section);
        inheritance(guide, section);
        interfacesAndRecords(guide, section);
        delegatesEventsAndLambdas(guide, section);
    }

    private static void classesStructsEnums(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_classes", section).titled("Classes, structs and enums")
                .paragraph("A class is a blueprint, and an object is a thing made from it. You declare a class with the"
                        + " word class, and you make an object of it with new. A program declares its types at the"
                        + " top of a namespace, and each type can hold fields, constructors and methods.")
                .table("The kinds of type", "Kind", "How its values behave", "Sigma", "Sigma#")
                .row(GuideCell.words("class"), GuideCell.words("Each value is an object, and names share it."),
                        GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("struct"),
                        GuideCell.words("Each value is copied when stored or handed over, and compared by its fields."),
                        GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("enum"), GuideCell.words("A list of named numbers, such as Low and High."),
                        GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.words("interface"), GuideCell.words("A list of methods that a class must write."),
                        GuideCell.words("No"), GuideCell.words("Yes"))
                .row(GuideCell.words("record"),
                        GuideCell.words("A class-like type made of readonly fields, compared by its values."),
                        GuideCell.words("No"), GuideCell.words("Yes"))
                .row(GuideCell.words("delegate"), GuideCell.words("A named shape of method, which a value can hold."),
                        GuideCell.words("No"), GuideCell.words("Yes"))
                .paragraph("A struct is a value type. Its values are copied when you store them or hand them over, so a"
                        + " change to the copy does not change the original. An enum is a list of named numbers. Each"
                        + " name is a value of the enum type, and you write Level.Mid to use one. The first name gets"
                        + " the number 0, and each name after it gets the next number, unless you write its number"
                        + " yourself.")
                .listing("Shop.sg",
                        "namespace Shop;",
                        "",
                        "enum Level { Low, Mid = 5, High }     // Low is 0, Mid is 5, High is 6",
                        "",
                        "struct Money { public int Cents; }     // copied when stored",
                        "",
                        "class Till",
                        "{",
                        "    private int sales;",
                        "    public Till(int start) { sales = start; }",
                        "    public void Add(int cents) { sales += cents; }",
                        "    public int Total() { return sales; }",
                        "}")
                .paragraph("A constructor has the name of its class and gives nothing back. It runs when you make an"
                        + " object with new, and it sets the object up. Till till = new Till(100); makes a Till that"
                        + " starts with 100 cents. A class with no constructor gets an empty one. A constructor can"
                        + " hand over to another constructor of its own class with the keyword this, or to the base"
                        + " class with the keyword base.")
                .paragraph("You reach a member with a dot. An object's member is written with the object's name, as"
                        + " till.Total(). A static member is written with the type's name, as Level.Mid for an enum"
                        + " value. The keyword this means the current object, and base means the base class. A field"
                        + " marked readonly can be written only where it is declared or in a constructor. The word"
                        + " const is kept for a later version of the language: for now, write a static readonly"
                        + " field.")
                .paragraph("The words public, private, protected and internal say who may reach a member, as in C#."
                        + " A public or an internal member is reached from anywhere in the program. A private one is"
                        + " reached only from inside its own type and the types nested in it, and a member written"
                        + " with no word of access is private. A protected one is reached from its own type and from"
                        + " the types that stand on it. Reaching a member from where its word does not let you is the"
                        + " error S3060.")
                .paragraph("Sigma# has interfaces, records and delegates as well, which are explained in"
                        + " [Interfaces and records](jsc:sigma_interfaces_records) and"
                        + " [Properties, events, delegates and lambdas](jsc:sigma_delegates_events).")
                .define("Struct", "A value type, declared with the word struct, which is copied whenever it is"
                        + " stored or"
                        + " handed over.")
                .define("Enum", "A type whose values are a list of names, and each name has a number.")
                .define("Field", "A variable declared inside a class or a struct, which belongs to each object.")
                .define("Constructor", "A special method that runs when you make an object with new, named after its"
                        + " class.")
                .define("Namespace", "A named group of types, such as Shop. A file puts its types in one namespace"
                        + " with a namespace line.")
                .define("Interface", "A list of method signatures that a class promises to write.")
                .define("Base class", "The class that another class inherits from, written after a colon.")
                .define("Record", "A class-like type made of readonly fields, which is compared by its values.")
                .define("Delegate", "A named shape of method. A value of a delegate type can hold any method of that"
                        + " shape.")
                .ifSomethingGoesWrong(
                        "The compiler says 'Till' has no member called 'Count'.",
                        "Check the spelling, and check that the class declares that member.",
                        "The compiler says 'Add' belongs to an object, not to the type.",
                        "Call it on an object, such as till.Add(50).",
                        "You mark a field readonly, then write it in a method: the compiler says 'count' is readonly,"
                                + " so it can only be written where it is declared or in a constructor.",
                        "Write it in the constructor, or leave out readonly.",
                        "The compiler says an enum's number has to be written as a number.",
                        "Write the number as a plain whole number, such as Mid = 5.",
                        "The compiler says 'Add' is already declared here.",
                        "Rename one of the two methods, or give the second one different parameters.")
                .seeAlso("jsc:sigma_inheritance", "jsc:sigma_interfaces_records")
                .register();
    }

    private static void inheritance(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_inheritance", section).titled("Inheritance: virtual, override and abstract")
                .paragraph("Inheritance lets one class build on another. A class that inherits from a base class"
                        + " has all"
                        + " of the base class's members, and it can add new ones or replace some of them. You write the"
                        + " base class after a colon, as in class Car : Vehicle. A class has one base class at most.")
                .paragraph("A method that a derived class may replace must be marked virtual in the base class. The"
                        + " derived class replaces it with override, and keeps the same parameters. Without override, a"
                        + " method with the same name and parameters as one above it is refused, so a method never gets"
                        + " hidden by accident.")
                .listing("Garage.sg",
                        "namespace Garage;",
                        "",
                        "class Vehicle",
                        "{",
                        "    public virtual string Name() { return \"vehicle\"; }",
                        "}",
                        "",
                        "class Car : Vehicle",
                        "{",
                        "    public override string Name() { return \"car\"; }",
                        "}")
                .table("Words of inheritance", "Word", "Sigma", "Sigma#")
                .row(GuideCell.code("virtual"), GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.code("override"), GuideCell.words("Yes"), GuideCell.words("Yes"))
                .row(GuideCell.code("abstract"), GuideCell.words("No"), GuideCell.words("Yes"))
                .paragraph("An abstract method has no body. It says that each class deriving from this one must write"
                        + " one. A method marked abstract needs an abstract class, and an abstract class cannot be made"
                        + " with new. Sigma has virtual and override, but no abstract classes and no abstract methods."
                        + " Its compiler refuses them and suggests a class with virtual methods that do nothing.")
                .paragraph("To call the version of a method that you replaced, write base.Name(args). It calls the"
                        + " version in the base class. A struct cannot have a base class, and a static method cannot be"
                        + " virtual, override or abstract.")
                .define("Inheritance", "A way for a class to take on the members of another class, and then add to them"
                        + " or replace some of them.")
                .define("Virtual", "Marks a method that a derived class may replace.")
                .define("Override", "Marks a method that replaces a virtual method of the base class.")
                .define("Abstract", "Marks a method with no body, or a class that cannot be made with new, which needs"
                        + " a derived class to finish it.")
                .ifSomethingGoesWrong(
                        "The compiler says only a class or an interface can stand behind the colon.",
                        "Name a class as the base class. Interfaces after the colon are Sigma# only.",
                        "The compiler says 'Name' is already on 'Vehicle', so write it with override to replace it.",
                        "Add override in front of the method.",
                        "The compiler says 'Name' on 'Vehicle' is not virtual, so nothing can replace it.",
                        "Add virtual to the method in the base class.",
                        "The compiler says 'Name' is written with override but nothing above it has it.",
                        "Remove override, or check that the name and the parameters match the method above.",
                        "The compiler says 'Draw' has no body, so 'Shape' has to be declared abstract.",
                        "In Sigma#, declare the class abstract, or give the method a body.",
                        "The compiler says 'Shape' is abstract, so it cannot be made with new.",
                        "Make an object of a class that derives from it, and give that class the bodies it needs.")
                .seeAlso("jsc:sigma_entry_points", "jsc:sigma_classes")
                .register();
    }

    private static void interfacesAndRecords(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_interfaces_records", section).titled("Interfaces and records")
                .paragraph("This page describes Sigma#. Sigma has neither interfaces nor records. Its compiler refuses"
                        + " the word interface and suggests a class with virtual methods instead, and it refuses the"
                        + " word record and suggests a struct with the same fields.")
                .paragraph("An interface lists the methods that a class must write. It holds method signatures only: a"
                        + " result type, a name and parameters, ending in a semicolon, with no body. A class says"
                        + " that it"
                        + " writes an interface with a colon, and then writes every method of it as a public method."
                        + " An interface can stand behind another interface after a colon too.")
                .listing("Shapes.sgs",
                        "namespace Shapes;",
                        "",
                        "interface IShape",
                        "{",
                        "    int Area();",
                        "}",
                        "",
                        "class Square : IShape",
                        "{",
                        "    public int Side;",
                        "    public int Area() { return Side * Side; }",
                        "}")
                .paragraph("IScript is a built-in interface that makes a program stay up. The"
                        + " [entry points page](jsc:sigma_entry_points) explains it.")
                .paragraph("A record is a class-like type made of readonly fields. You write its fields in brackets"
                        + " after its name. record Point(int X, int Y); declares a Point with two fields, X and Y. Each"
                        + " field is public and readonly, and the record gets one constructor that takes the fields in"
                        + " order. The record also writes ToString for you, which prints its fields by name."
                        + " Two records"
                        + " with the same fields are equal, because a record is compared by its values.")
                .table("Records and interfaces, in short", "Kind", "What it holds", "Copied when stored")
                .row(GuideCell.words("Interface"), GuideCell.words("Method signatures only, with no fields."),
                        GuideCell.words("Not applicable: it holds no values."))
                .row(GuideCell.words("Record"), GuideCell.words("readonly fields, set by the constructor."),
                        GuideCell.words("No: it is shared, as every class is."))
                .ifSomethingGoesWrong(
                        "The compiler says 'Square' says it is a 'IShape' but does not have 'Area'.",
                        "Write every method the interface lists, as a public method.",
                        "Sigma refuses the word interface and suggests a class with virtual methods.",
                        "In Sigma, write a class whose methods are virtual, and derive from it, as the Sigma page"
                                + " on inheritance shows.",
                        "Sigma refuses the word record and suggests a struct with the same fields.",
                        "In Sigma, write a struct with the same fields, or use Sigma# for a record.")
                .seeAlso("jsc:sigma_inheritance", "jsc:sigma_delegates_events")
                .register();
    }

    private static void delegatesEventsAndLambdas(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_delegates_events", section).titled("Properties, events, delegates and lambdas")
                .paragraph("Sigma has none of the features on this page. Sigma# has all of them. Sigma refuses"
                        + " lambdas as well, so this page is for Sigma#.")
                .paragraph("A property is a named value of an object that runs code when it is read or written. You"
                        + " write it with get and set accessors, as in int Size { get; set; }. A property with only get"
                        + " can be read but not written. Sigma refuses properties: write a field, or a method"
                        + " that gives"
                        + " the value back.")
                .paragraph("A delegate is a named shape of method. A value of a delegate type can hold any method"
                        + " of that"
                        + " shape, and you call the value as if it were a method. The line delegate int Pick(int a, int"
                        + " b); declares a shape that takes two ints and gives back an int. The System namespace has"
                        + " three ready-made delegates: Action takes nothing and gives nothing back, Action<T>"
                        + " takes one"
                        + " value and gives nothing back, and Func<T, R> takes one value and gives one back.")
                .listing("Pick.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "",
                        "namespace Shop;",
                        "",
                        "delegate int Pick(int a, int b);",
                        "",
                        "class Chooser",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        Pick biggest = (int a, int b) => a > b ? a : b;",
                        "        Console.PrintLine(\"\" + biggest(3, 9));",
                        "    }",
                        "}")
                .paragraph("A class declares an event with the word event and a delegate type, as in event Action"
                        + " Changed;. Other code subscribes with += and unsubscribes with -=. Only the class that"
                        + " declares the event can raise it, by calling it.")
                .paragraph("A lambda is a short method written in place. x => x * 2 takes one value, and"
                        + " () => { statements }"
                        + " takes none, with a block of statements for its body. The shape comes from the delegate that"
                        + " the lambda is given to, so its parameters and its result must match that delegate. A lambda"
                        + " can use the local variables of the method it sits in, and it can use this.")
                .listing("Count.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "",
                        "namespace Shop;",
                        "",
                        "class Till",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        int kept = 0;",
                        "        Action count = () => {",
                        "            kept++;",
                        "            Console.PrintLine(\"\" + kept);",
                        "        };",
                        "        count();",
                        "    }",
                        "}")
                .define("Event", "A way for an object to announce that something happened. Other code subscribes to it"
                        + " with += and unsubscribes with -=.")
                .define("Lambda", "A short method written in place, which takes its shape from the delegate it is given"
                        + " to.")
                .ifSomethingGoesWrong(
                        "The compiler says an event's type must be a delegate.",
                        "Give the event a delegate type, such as Action.",
                        "The compiler says an event can only be raised inside the type that declares it.",
                        "Call a method of the class that declares the event, and let that method raise it.",
                        "The compiler says this lambda does not have the shape 'Pick' takes.",
                        "Give the lambda the same parameters and the same result as the delegate.",
                        "The compiler says a lambda that keeps a variable a loop declares is not built yet.",
                        "Declare the variable before the loop starts, so the lambda keeps a variable of its own.",
                        "The compiler says 'Ring' is a method: call it, or hand it over where a delegate of its shape"
                                + " is wanted.",
                        "Write Ring() with its brackets, or hand the method to a delegate of its shape.")
                .seeAlso("jsc:sigma_methods_out_ref", "jsc:sigma_namespaces_using")
                .register();
    }

    private static void organising(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_organising").titled("Organising a program")
                .in(chapter).register();
        namespacesAndUsing(guide, section);
        entryPoints(guide, section);
    }

    private static void namespacesAndUsing(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_namespaces_using", section).titled("Namespaces and using")
                .paragraph("Every type of a program belongs to a namespace, which is a name that groups types. A file"
                        + " declares its namespace with one line before its types, such as namespace Shop;. Every type"
                        + " in the file then belongs to that namespace, so two programs can each have a type called"
                        + " Till without a clash. When you write a program, the first lines tell the compiler where"
                        + " the types it uses come from.")
                .paragraph("The library is grouped into namespaces as well. A program that wants a library type writes"
                        + " a using line at the top of the file, before the namespace line. using System.IO.*; brings"
                        + " in everything in System.IO, and using System.IO.Console; brings in one type only.")
                .table("Namespaces you will meet", "Namespace", "Holds")
                .row(GuideCell.code("Standard"),
                        GuideCell.words("The library of Sigma, which Sigma# also has: Console, File, Math, Script and"
                                + " more."))
                .row(GuideCell.code("System"), GuideCell.words("Action, Func and IScript."))
                .row(GuideCell.code("System.IO"), GuideCell.words("Console and File."))
                .row(GuideCell.code("System.Utils"), GuideCell.words("Math and Random."))
                .row(GuideCell.code("System.Collections"), GuideCell.words("List and Map."))
                .row(GuideCell.code("System.Machine"), GuideCell.words("Other parts of the library. The"
                        + " [library chapter](jsc:sigma_library) describes them."))
                .paragraph("A using line for a whole namespace is written with a star, such as using System.IO.*;. A"
                        + " line that names one type has no star. A program written in Sigma writes using Standard.*;"
                        + " to reach the library of Sigma.")
                .paragraph("A type that the program declares itself wins over a library type with the same name. A"
                        + " full name, such as System.IO.Console.PrintLine(\"hi\"), works in Sigma# without a using"
                        + " line, and Sigma refuses full names.")
                .table("The start of a new project", "Project", "Header lines")
                .row(GuideCell.words("Sigma# console program"),
                        GuideCell.code("using System.IO.*; then namespace, then the class"))
                .row(GuideCell.words("Sigma# script"),
                        GuideCell.code("using System.*; and using System.IO.*; then namespace, then the class"))
                .row(GuideCell.words("Sigma console program"), GuideCell.code("using Standard.*; then namespace"))
                .row(GuideCell.words("Sigma script"), GuideCell.code("using Standard.*; then namespace"))
                .listing("Ping.sgs",
                        "using System.IO.*;",
                        "",
                        "namespace Garage.Tools;",
                        "",
                        "class Ping",
                        "{",
                        "    static void Main() { Console.PrintLine(\"up\"); }",
                        "}")
                .ifSomethingGoesWrong(
                        "The compiler says 'Console' is in System.IO; add 'using System.IO.*;' or"
                                + " 'using System.IO.Console;' at the top of the file.",
                        "Add that using line at the top of the file.",
                        "The compiler says 'System.IO' names a namespace.",
                        "Write 'using System.IO.*;' to bring in everything in it, or name one of its types.",
                        "The compiler says a using has to come before the namespace and the types.",
                        "Move the using line above the namespace line.",
                        "The compiler refuses using System in Sigma.",
                        "Write using Standard.*; instead, which holds everything that Sigma has.")
                .seeAlso("jsc:sigma_entry_points", "jsc:sigma_library")
                .register();
    }

    private static void entryPoints(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_entry_points", section).titled("Where a program starts: Main and resident programs")
                .paragraph("Every program has exactly one place to start. When you run a program, the machine runs"
                        + " that place first. There"
                        + " are two kinds of start. A console program runs a method named Main once and ends."
                        + " A resident"
                        + " program stays up after it starts, and runs code each game tick until it is stopped.")
                .define("Entry point", "The place where a program starts running.")
                .define("Console program", "A program that runs its Main method once, and ends when Main returns.")
                .define("Resident program", "A program that starts and then stays up, running its methods each game"
                        + " tick until it is stopped.")
                .table("The two kinds of program", "Kind", "Starts with", "Ends when")
                .row(GuideCell.words("Console program"), GuideCell.code("static void Main()"),
                        GuideCell.words("Main returns."))
                .row(GuideCell.words("Resident program"),
                        GuideCell.words("A class that stands on Script, or, in Sigma#, a class that implements"
                                + " IScript."),
                        GuideCell.words("It calls Program.Exit, or the program is removed."))
                .paragraph("A class that both stands on Script and implements IScript is a resident program, because"
                        + " writing the interface is a deliberate act. A file with no start place is refused, and so"
                        + " is a file with more than one.")
                .table("The forms of Main", "Form", "Version", "Meaning")
                .row(GuideCell.code("static void Main()"), GuideCell.words("1"),
                        GuideCell.words("Runs once, takes nothing and gives nothing back."))
                .row(GuideCell.code("static int Main()"), GuideCell.words("2"),
                        GuideCell.words("Gives back an exit code, a whole number, when it ends."))
                .row(GuideCell.code("static void Main(string[] args)"), GuideCell.words("2"),
                        GuideCell.words("Receives the arguments that the program was started with."))
                .row(GuideCell.code("static int Main(string[] args)"), GuideCell.words("2"),
                        GuideCell.words("Receives the arguments, and gives back an exit code."))
                .paragraph("Main must be static, in the class that holds it. A project is held to the version it names,"
                        + " so a form of Main that a later version adds is refused in an earlier project.")
                .paragraph("A resident program has three methods that the machine calls. OnInit runs once when the"
                        + " program starts. OnTick runs once per game tick, and a new tick waits until the last OnTick"
                        + " has finished, so a slow OnTick never piles up. OnDestroy runs when the program is stopped,"
                        + " if the machine still has time to run it. In Sigma#, write public void for each of them"
                        + " in an"
                        + " IScript class. In Sigma, write public override void for the ones you use, in a class that"
                        + " stands on Script.")
                .paragraph("A console program in Sigma looks like this, with one line that prints when it runs.")
                .listing("Greeter.sg",
                        "using Standard.*;",
                        "",
                        "namespace Hello;",
                        "",
                        "class Greeter",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        printf(\"Hello\");",
                        "    }",
                        "}")
                .paragraph("A resident program in Sigma stands on Script, and it overrides only the methods it needs."
                        + " Each game tick, the machine calls OnTick, as the [credits page](jsc:credits_per_tick)"
                        + " explains.")
                .listing("Light.sg",
                        "using Standard.*;",
                        "",
                        "namespace Lamp;",
                        "",
                        "class Light : Script",
                        "{",
                        "    public override void OnTick()",
                        "    {",
                        "        // runs every game tick",
                        "    }",
                        "}")
                .ifSomethingGoesWrong(
                        "The compiler says the program needs exactly one place to start, and this one has none.",
                        "Add a class with a static method named Main, or a class that stands on Script, or implements"
                                + " IScript in Sigma#.",
                        "The compiler says 'Main' needs version 2; this project is version 1.",
                        "Write the Main that your project's version has, or change the version the project names.")
                .seeAlso("jsc:sigma_namespaces_using", "jsc:sigma_inheritance")
                .register();
    }

    private static void data(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_data").titled("Text and collections").in(chapter)
                .register();
        stringsAndChars(guide, section);
        listsAndMaps(guide, section);
    }

    private static void stringsAndChars(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_strings_chars", section).titled("Text and characters")
                .paragraph("Text is a string: a row of characters between double quotes. A string is a reference type,"
                        + " so it can be null. A char is one character, written between single quotes, and inside the"
                        + " machine it is its number.")
                .paragraph("You join text with +. When one side is text, the result is text, so \"n = \" + 5 gives"
                        + " n = 5. Two texts are equal with == when they hold the same characters. Text cannot be"
                        + " compared with < or >: to put texts in order, use string.Compare, which needs version 2 of"
                        + " the project. It gives back a number below zero, zero or above zero.")
                .table("Members of text", "Member", "Gives back", "What it does")
                .row(GuideCell.code("Length"), GuideCell.words("An int"),
                        GuideCell.words("How many characters the text holds. It is a property, so write no brackets."))
                .row(GuideCell.code("Substring(start)"), GuideCell.words("Text"),
                        GuideCell.words("The text from the place start to the end."))
                .row(GuideCell.code("Substring(start, length)"), GuideCell.words("Text"),
                        GuideCell.words("A piece of the text, of the length you give."))
                .row(GuideCell.code("IndexOf(text)"), GuideCell.words("An int"),
                        GuideCell.words("The place of the first match, counted from 0, or -1 when there is none."))
                .row(GuideCell.code("Contains, StartsWith, EndsWith"), GuideCell.words("A bool"),
                        GuideCell.words("Whether the text holds, starts with or ends with the piece you give."))
                .row(GuideCell.code("ToUpper(), ToLower(), Trim()"), GuideCell.words("Text"),
                        GuideCell.words("A new text in capitals, in small letters, or without spaces at the ends."))
                .row(GuideCell.code("Replace(old, new)"), GuideCell.words("Text"),
                        GuideCell.words("A new text with every old piece changed to new."))
                .row(GuideCell.code("Split(char)"), GuideCell.words("A List of text, Sigma# only"),
                        GuideCell.words("The pieces between the character you give."))
                .row(GuideCell.code("string.Format(format, value)"), GuideCell.words("Text"),
                        GuideCell.words("Builds a text from a format and the values for its holes."))
                .row(GuideCell.code("string.Compare(a, b)"), GuideCell.words("An int"),
                        GuideCell.words("Puts two texts in order: below zero, zero or above zero."))
                .paragraph("Text cannot be indexed like an array: text[0] is refused. To take the first character as"
                        + " text, use Substring(0, 1).")
                .paragraph("A char has its own tests: char.IsDigit(c), char.IsLetter(c), char.IsWhiteSpace(c),"
                        + " char.ToUpper(c) and char.ToLower(c). They need version 2 of the project. The older names,"
                        + " such as toupper, are on the [old names page](jsc:sigma_printf_old_names).")
                .paragraph("A char in arithmetic becomes a number. 'A' + 1 is an int, not a character.")
                .listing("Names.sg",
                        "string name = \"Ada Lovelace\";",
                        "int first = name.IndexOf(\" \");",
                        "string given = name.Substring(0, first);     // \"Ada\"",
                        "bool long_name = name.Length > 8;",
                        "string shout = given.ToUpper();              // \"ADA\"",
                        "int order = string.Compare(\"apple\", \"banana\"); // below zero")
                .define("Index", "The place of an item in a row, counted from 0, so the first item is at place 0.")
                .ifSomethingGoesWrong(
                        "The compiler says 'string' cannot be indexed by 'int'.",
                        "Use Substring to take a piece of the text instead.")
                .seeAlso("jsc:sigma_list_map", "jsc:sigma_printf_old_names")
                .register();
    }

    private static void listsAndMaps(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_list_map", section).titled("Lists and maps")
                .paragraph("An array holds a fixed number of values of one type, numbered from 0. You make one with new"
                        + " and its length: new int[4] makes an array of four ints, with the indexes 0 to 3."
                        + " Length tells"
                        + " how many places the array has. An index outside the array stops the program with a message,"
                        + " as the [runtime stops page](jsc:sigma_runtime_stops) explains. Arrays are in Sigma and in"
                        + " Sigma#.")
                .paragraph("A List is a growable row of values of one type, written List<int> for a list of ints. You"
                        + " make one with new List<int>(), and each Add puts one more item at the end. Sigma# only.")
                .table("Members of List", "Member", "Gives back", "What it does")
                .row(GuideCell.code("Count"), GuideCell.words("An int"),
                        GuideCell.words("How many items the list holds."))
                .row(GuideCell.code("Add(item)"), GuideCell.words("Nothing"),
                        GuideCell.words("Puts the item at the end."))
                .row(GuideCell.code("Insert(index, item)"), GuideCell.words("Nothing"),
                        GuideCell.words("Puts the item in the place index, moving the later items along."))
                .row(GuideCell.code("RemoveAt(index)"), GuideCell.words("Nothing"),
                        GuideCell.words("Takes away the item at the place index."))
                .row(GuideCell.code("Remove(item)"), GuideCell.words("A bool"),
                        GuideCell.words("Takes away an item that matches, and tells whether it found one."))
                .row(GuideCell.code("Clear()"), GuideCell.words("Nothing"), GuideCell.words("Takes away every item."))
                .row(GuideCell.code("Contains(item)"), GuideCell.words("A bool"),
                        GuideCell.words("Whether the item is in the list."))
                .row(GuideCell.code("IndexOf(item)"), GuideCell.words("An int"),
                        GuideCell.words("The place of the item, or -1 when it is not there."))
                .row(GuideCell.code("Get(index), Set(index, item)"), GuideCell.words("An item, or nothing"),
                        GuideCell.words("Reads or changes the item at the place index."))
                .row(GuideCell.code("Sort()"), GuideCell.words("Nothing"), GuideCell.words("Puts the items in order."))
                .paragraph("A Map holds pairs: a key, and the value that belongs to it. Map<string, int> makes a map"
                        + " from text keys to int values. Put adds a pair, and the key finds the value again. A map"
                        + " has no Clear member. Sigma# only.")
                .define("Key", "The part of a pair that finds its value in a map, such as the name of a fruit.")
                .define("Map", "A collection of pairs: each key leads to one value. Written Map<K, V> in Sigma#.")
                .table("Members of Map", "Member", "Gives back", "What it does")
                .row(GuideCell.code("Count"), GuideCell.words("An int"),
                        GuideCell.words("How many pairs the map holds."))
                .row(GuideCell.code("Put(key, value)"), GuideCell.words("Nothing"),
                        GuideCell.words("Adds the pair, or changes the value of a key that is already there."))
                .row(GuideCell.code("Get(key)"), GuideCell.words("The value"),
                        GuideCell.words("The value of the key. A missing key gives back null and does not stop"
                                + " the program."))
                .row(GuideCell.code("ContainsKey(key)"), GuideCell.words("A bool"),
                        GuideCell.words("Whether the key is in the map."))
                .row(GuideCell.code("TryGet(key, out value)"), GuideCell.words("A bool"),
                        GuideCell.words("Whether the key is there, and its value through out."))
                .row(GuideCell.code("Remove(key)"), GuideCell.words("A bool"),
                        GuideCell.words("Takes the pair away, and tells whether the key was there."))
                .row(GuideCell.code("Keys(), Values()"), GuideCell.words("A List"),
                        GuideCell.words("Every key, or every value, in a List."))
                .paragraph("To find out whether a key is there before you read it, use ContainsKey, or use TryGet,"
                        + " which tells you and gives the value back through out in one call. A map is indexed with"
                        + " the key in brackets, as in count[\"apple\"].")
                .listing("Stock.sgs",
                        "using System.*;",
                        "using System.Collections.*;",
                        "using System.IO.*;",
                        "",
                        "namespace Inventory;",
                        "",
                        "class Stock",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        Map<string, int> count = new Map<string, int>();",
                        "        count.Put(\"apple\", 3);",
                        "        int got;",
                        "        if (count.TryGet(\"apple\", out got))",
                        "        {",
                        "            Console.PrintLine(\"\" + got);",
                        "        }",
                        "        List<string> names = count.Keys();",
                        "        foreach (string name in names)",
                        "        {",
                        "            Console.PrintLine(name);",
                        "        }",
                        "    }",
                        "}")
                .paragraph("A foreach statement walks a List or an array, one item after another. A map cannot"
                        + " be walked"
                        + " with foreach: walk its Keys() or its Values() instead, which are Lists.")
                .paragraph("Sigma refuses the List and Map types and any type written with angle brackets. In Sigma,"
                        + " use an array with a count of your own beside it, or use two arrays for pairs.")
                .ifSomethingGoesWrong(
                        "The program stops and says \"there is no place 3 in a list of 3\".",
                        "The index must run from 0 up to one less than Count. Check it before you read the item.",
                        "The program stops and says \"there is no place 4 in an array of 4\".",
                        "Use the indexes from 0 up to the length minus one.",
                        "The compiler says foreach walks an array or a list, not a Map.",
                        "Walk Keys() or Values() instead, as in foreach (string name in count.Keys()).",
                        "The compiler refuses List in Sigma, and says to use an array, and a count of your own"
                                + " beside it.",
                        "Use an array, with a count of your own beside it, as Sigma allows.")
                .seeAlso("jsc:sigma_control_flow", "jsc:sigma_no_exceptions")
                .register();
    }

    private static void failures(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_failures").titled("When things fail").in(chapter)
                .register();
        noExceptions(guide, section);
        dispose(guide, section);
    }

    private static void noExceptions(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_no_exceptions", section).titled("No exceptions: Try forms and halts")
                .paragraph("Many languages report a failed step with an exception, which another part of the program"
                        + " can catch and handle. This language has no exceptions. It has no try, catch, throw or"
                        + " finally, and the compiler refuses those words as the start of a statement. When you read"
                        + " a number from text in Sigma#, use a Try form first, so the program does not stop.")
                .define("Exception", "A signal that a step failed, which a program can catch and handle somewhere"
                        + " else.")
                .paragraph("Instead, a program can ask first with a Try form, or it can let the problem stop it. A Try"
                        + " form is a library call that says whether it worked, and gives its result back through out."
                        + " Sigma's own Convert and File offer fewer calls, and the Try forms are not among them. The"
                        + " [numbers page](jsc:numbers_and_time) and the [files page](jsc:files_and_file_handles) show"
                        + " what Sigma offers.")
                .define("Try form", "A library call that gives back true or false for success, and writes its result"
                        + " through out.")
                .table("The Try forms", "Call", "Gives back")
                .row(GuideCell.code("Convert.TryInt(text, out int)"), GuideCell.words("A bool: whether the text is a"
                        + " whole number, and the number through out"))
                .row(GuideCell.code("Convert.TryLong(text, out long)"), GuideCell.words("A bool: whether the text is a"
                        + " long, and the value through out"))
                .row(GuideCell.code("Convert.TryDouble(text, out double)"),
                        GuideCell.words("A bool: whether the text is"
                        + " a decimal number, and the value through out"))
                .row(GuideCell.code("Convert.TryBool(text, out bool)"), GuideCell.words("A bool: whether the text is"
                        + " true or false, and the value through out"))
                .row(GuideCell.code("File.TryRead(path, out string)"),
                        GuideCell.words("A bool: whether the file could be"
                        + " read, and its text through out"))
                .row(GuideCell.code("Map.TryGet(key, out value)"),
                        GuideCell.words("A bool: whether the key is there, and"
                        + " its value through out"))
                .paragraph("A halt is different. The program stops at once, writes a message to its console,"
                        + " and does not"
                        + " go on. Nothing catches a halt. The full list of messages is in the"
                        + " [runtime stops page](jsc:sigma_runtime_stops).")
                .table("Halts that the language itself causes", "The message begins with", "Caused by")
                .row(GuideCell.words("divided by zero"), GuideCell.words("A division with a divisor of zero."))
                .row(GuideCell.words("took the remainder of a division by zero"),
                        GuideCell.words("A remainder with a divisor of zero."))
                .row(GuideCell.words("there is nothing here to reach into"),
                        GuideCell.words("A name that holds nothing is used as if it held an object."))
                .row(GuideCell.words("there is no place 4 in an array of 4"),
                        GuideCell.words("An index that is outside the array."))
                .row(GuideCell.words("this was disposed and cannot be used"),
                        GuideCell.words("An object used again after its dispose."))
                .row(GuideCell.words("out of memory"),
                        GuideCell.words("The program asks for more memory than it may use."))
                .paragraph("A fault is different again. The machine itself failed on one instruction, and only that one"
                        + " program stops. The message says that the runtime could not carry the step out.")
                .define("Fault", "A failure of the machine itself on one instruction, which stops only that program.")
                .listing("Reader.sgs",
                        "using System.*;",
                        "using System.Utils.*;",
                        "using System.IO.*;",
                        "",
                        "namespace Parse;",
                        "",
                        "class Reader",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        string text = \"42\";",
                        "        int n;",
                        "        if (Convert.TryInt(text, out n))",
                        "        {",
                        "            Console.PrintLine(\"\" + n);",
                        "        }",
                        "        else",
                        "        {",
                        "            Console.PrintLine(\"not a number\");",
                        "        }",
                        "    }",
                        "}")
                .ifSomethingGoesWrong(
                        "The program stops and says there is nothing here to reach into.",
                        "Check that the name holds an object before you use it, for example by creating the object"
                                + " with new first.",
                        "The program stops and says this was disposed and cannot be used.",
                        "Do not use an object after you dispose of it. See [Freeing objects with dispose]"
                                + "(jsc:sigma_dispose).",
                        "The program stops and says divided by zero.",
                        "Check that the divisor is not zero before you divide.",
                        "The compiler refuses try.",
                        "Neither language has try: check the value before the step, or use a Try form in Sigma#.")
                .seeAlso("jsc:sigma_runtime_stops", "jsc:sigma_dispose")
                .register();
    }

    private static void dispose(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_dispose", section).titled("Freeing objects with dispose")
                .paragraph("A program makes objects with new, and each object takes some of the memory the program may"
                        + " use. This language has no garbage collector. Here, an object stays until your program"
                        + " disposes of it, or until the program ends.")
                .define("Garbage collector", "A part of a machine that finds objects nothing uses any more, and frees"
                        + " them. This language has none.")
                .paragraph("The dispose statement says that you have finished with an object. You write the word, then"
                        + " the name of the object, as in dispose scratch;. The name must hold an object: dispose of an"
                        + " enum value is refused.")
                .listing("Work.sg",
                        "namespace Work;",
                        "",
                        "class Scratch",
                        "{",
                        "    public int Size;",
                        "    public Scratch(int size) { Size = size; }",
                        "}",
                        "",
                        "class Work",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        Scratch scratch = new Scratch(4096);",
                        "        dispose scratch;",
                        "    }",
                        "}")
                .paragraph("After dispose, the object cannot be used again. A later use stops the program"
                        + " with the message "
                        + "\"this was disposed and cannot be used.\" Memory is counted to the byte: each new takes"
                        + " bytes"
                        + " from the memory your program may use, and a program that asks for more stops with an out of"
                        + " memory message.")
                .ifSomethingGoesWrong(
                        "The compiler says only an object can be disposed, not 'Level'.",
                        "Dispose only objects made with new. A value of an enum is not an object.",
                        "The program stops and says this was disposed and cannot be used.",
                        "Make a new object with new when you need one again.",
                        "The program stops with an out of memory message.",
                        "Dispose of the objects you no longer need, so that the program holds less.")
                .seeAlso("jsc:sigma_no_exceptions", "jsc:sigma_runtime_stops")
                .register();
    }

    private static void oldStyle(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sigma_old_style").titled("The old style").in(chapter)
                .register();
        printfAndOldNames(guide, section);
    }

    private static void printfAndOldNames(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_printf_old_names", section).titled("printf and the old names")
                .paragraph("Sigma keeps a set of short names from an older style of programming, such as printf, strlen"
                        + " and puts. You call them with no type in front, and they work in Sigma and in Sigma#. The"
                        + " one you meet first is printf, which prints a text with values in it.")
                .define("Format string", "The text that printf prints, with holes in it where values go.")
                .define("Hole", "A place in a format string, marked with a percent sign and a letter, where a value is"
                        + " written.")
                .paragraph("printf takes a format string and then one value for each hole. It prints the text to the"
                        + " console and gives nothing back. The format string must be written in the call itself, in"
                        + " quotes, so that the compiler can check its holes against the values.")
                .listing("Old.sg",
                        "using Standard.*;",
                        "",
                        "namespace Old;",
                        "",
                        "class Tool",
                        "{",
                        "    static void Main()",
                        "    {",
                        "        int n = 42;",
                        "        printf(\"n = %d, text = %s\", n, \"ok\");",
                        "    }",
                        "}")
                .paragraph("This program prints n = 42, text = ok. The first hole takes the whole number n,"
                        + " and the second"
                        + " takes the text ok.")
                .table("The holes of a format string", "Hole", "Wants", "Version")
                .row(GuideCell.code("%d, %i"), GuideCell.words("A whole number, an int or a long"),
                        GuideCell.words("1"))
                .row(GuideCell.code("%u, %x, %X, %o"), GuideCell.words("A whole number"), GuideCell.words("2"))
                .row(GuideCell.code("%f"), GuideCell.words("A number"), GuideCell.words("1"))
                .row(GuideCell.code("%e, %E"), GuideCell.words("A number"), GuideCell.words("2"))
                .row(GuideCell.code("%s"), GuideCell.words("Text"), GuideCell.words("1"))
                .row(GuideCell.code("%c"), GuideCell.words("A char"), GuideCell.words("1"))
                .row(GuideCell.code("%%"), GuideCell.words("Nothing: it writes a percent sign"), GuideCell.words("1"))
                .row(GuideCell.code("l in front, as in %ld"),
                        GuideCell.words("Nothing: it is accepted and means nothing"),
                        GuideCell.words("1"))
                .row(GuideCell.code("ll, h, hh in front"), GuideCell.words("Nothing: accepted, and means nothing"),
                        GuideCell.words("2"))
                .row(GuideCell.code("Flags, width, precision"),
                        GuideCell.words("Flags such as - and 0, and a width or precision up to 999"),
                        GuideCell.words("2"))
                .paragraph("The number of holes must match the number of values. Each hole also wants a kind of value:"
                        + " %d wants a whole number, %s wants text, and %c wants a char."
                        + " The compiler checks all of this"
                        + " before the program runs.")
                .paragraph("sprintf takes the same format and values, and gives back the text instead of printing it."
                        + " scanf reads one value from the console into a variable. Its format has exactly one hole and"
                        + " nothing else, and the variable is written with out, as in scanf(\"%d\", out n). It gives"
                        + " back 1 when it read a value, and 0 when what was typed was not one. Every old name except"
                        + " printf needs version 2 of the project.")
                .paragraph("Each old name is one call of the library, and it gives the same value as the long way."
                        + " strlen(text) gives the same as text.Length. A method or a variable of your own"
                        + " with the same"
                        + " name wins over the old name.")
                .table("Some old names and their long way", "Old name", "Long way", "Version")
                .row(GuideCell.code("puts(text)"), GuideCell.code("Console.PrintLine(text)"), GuideCell.words("2"))
                .row(GuideCell.code("putchar(char)"), GuideCell.code("Console.Print(char)"), GuideCell.words("2"))
                .row(GuideCell.code("exit(int)"), GuideCell.code("Program.Exit(int)"), GuideCell.words("2"))
                .row(GuideCell.code("rand()"), GuideCell.code("Random.Next(32768)"), GuideCell.words("2"))
                .row(GuideCell.code("strlen(text)"), GuideCell.code("text.Length"), GuideCell.words("2"))
                .row(GuideCell.code("strcmp(a, b)"), GuideCell.code("string.Compare(a, b)"), GuideCell.words("2"))
                .row(GuideCell.code("atoi(text)"), GuideCell.code("Convert.ToInt(text, 0)"), GuideCell.words("2"))
                .row(GuideCell.code("toupper(char)"), GuideCell.code("char.ToUpper(char)"), GuideCell.words("2"))
                .row(GuideCell.code("fopen(path, mode)"), GuideCell.code("File.Open(path, mode)"), GuideCell.words("2"))
                .paragraph("The [old names in the library](jsc:standard_functions) page lists every old name"
                        + " and the call"
                        + " it makes.")
                .ifSomethingGoesWrong(
                        "The compiler says printf's format has to be written out where it is used, in quotes, so its"
                                + " holes can be read.",
                        "Write the format string directly in the call, between double quotes.",
                        "The compiler says printf: the format has 2 holes and the call gives 1 value.",
                        "Give one value for each hole, in the same order.",
                        "The compiler says printf: '%d' takes a whole number, and this is text.",
                        "Use %s for text, or give a whole number to the hole.",
                        "The compiler says '%q' is no hole printf has.",
                        "Use one of the holes in the table above.",
                        "The compiler says 'strlen' needs version 2; this project is version 1.",
                        "Use a project that is held to version 2, or use printf, which every version has.")
                .seeAlso("jsc:sigma_strings_chars", "jsc:sigma_library")
                .register();
    }
}
