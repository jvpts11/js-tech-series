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
 * The pages of the chapter on finding help: the commands that tell you what another command does, and the one that
 * lists them all. The chapter is shared by the Unix books, so every page says which systems it applies to.
 */
final class FindingHelpPages {

    private FindingHelpPages() {
    }

    /** Declares the sections and entries of finding help, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("help_at_the_prompt").titled("Help at the prompt")
                .in(chapter).register();
        overview(guide, section);
        helpCommand(guide, section);
        manPages(guide, section);
        whatisAndApropos(guide, section);
        infoKeys(guide, section);
        listcmd(guide, section);
    }

    private static void overview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("finding_help_overview", section).titled("Where to look").icon(ComputersGuide.MANUAL)
                .paragraph("Forgot the name of a command, or the option you need? You can ask the computer at the"
                        + " prompt, without leaving the terminal. Five tools answer the question what a command"
                        + " does, and one tool lists every command the computer has.")
                .paragraph("Each tool works on some systems and not on others. The same pages appear in every book"
                        + " that holds these chapters, so each page says which systems it applies to.")
                .table("Where each tool works", "Tool", "Where it works", "What it gives you")
                .row(GuideCell.code("help"), GuideCell.words("MC-DOS, MC-NET, Frames, Linux, UNIX System V"),
                        GuideCell.words("A command list, or one command's page; full screen on MC-DOS and"
                                + " MC-NET."))
                .row(GuideCell.code("man"), GuideCell.words("Linux, UNIX System V, FreeBSD"),
                        GuideCell.words("A command's manual page, and the entries of the manuals."))
                .row(GuideCell.code("whatis"), GuideCell.words("Linux, UNIX System V, FreeBSD"),
                        GuideCell.words("One line that says what a command is for."))
                .row(GuideCell.code("apropos"), GuideCell.words("Linux, UNIX System V, FreeBSD"),
                        GuideCell.words("The commands whose name or summary holds a word you give."))
                .row(GuideCell.code("info"), GuideCell.words("Linux"),
                        GuideCell.words("The manuals as linked screens, read with keys."))
                .row(GuideCell.code("listcmd"), GuideCell.words("Every system, when the server allows it"),
                        GuideCell.words("Every command and program this computer has."))
                .table("Which tool to start with", "You want", "Start with")
                .row(GuideCell.words("How a command is used, when you know its name"),
                        GuideCell.words("help <command> where help exists; man <command> on UNIX systems"))
                .row(GuideCell.words("What a command is for, in one line"), GuideCell.code("whatis <command>"))
                .row(GuideCell.words("A command, found by what it does"), GuideCell.code("apropos <word>"))
                .row(GuideCell.words("Every manual and command, read as linked screens"), GuideCell.code("info"))
                .row(GuideCell.words("Every command and program this computer has"), GuideCell.code("listcmd"))
                .paragraph("Most systems answer an unknown word with \"command not found:\" and the word, then a"
                        + " hint to type help. UNIX System V answers with the word and \"not found\" only, and"
                        + " FreeBSD, which has no help command, points you to apropos and man intro.")
                .define("command", "A word typed at the prompt that makes the system do one job, such as ls, help"
                        + " or man.")
                .define("manual page", "The short written page that explains one command: its name, its form,"
                        + " what it does and its options.")
                .ifSomethingGoesWrong("The reply is \"command not found: info\", or on FreeBSD \"info: not found. Try:"
                                + " apropos <word>, or man intro\".",
                        "info is on Linux only. On UNIX System V and FreeBSD, use man <command>; on MC-DOS,"
                                + " MC-NET and Frames, use help <command>.",
                        "The reply is \"command not found: listcmd\".",
                        "The server has the switch off. Read the listcmd page ([](jsc:listcmd)) to see how it is"
                                + " turned on.")
                .seeAlso("jsc:help_on_a_computer", "jsc:shell")
                .register();
    }

    private static void helpCommand(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("help_command", section).titled("The help command").icon(ComputersGuide.MANUAL)
                .paragraph("help is the command that tells you what the other commands do. Type help on its own,"
                        + " and it lists the commands this computer has. Type help and a command's name, and it"
                        + " prints that command's page: its form, what it does and its options.")
                .paragraph("On MC-DOS and MC-NET, help takes the whole screen, and you move through the page with"
                        + " the keys below. On Frames, Linux and UNIX System V it prints the page as text. The Help"
                        + " windows of the desktop are a different thing ([](jsc:help_on_a_computer)).")
                .subheading("Using help")
                .steps("Type help and press [[Enter]] to list the commands this computer has.",
                        "Type help, a space and a name, such as help ls on Linux, to read that command's page.",
                        "On MC-DOS or MC-NET, press [[Esc]] when you are done, to return to the prompt.")
                .paragraph("On MC-DOS and MC-NET, the top line of the full screen reads, for example, \"MC-DOS Help:"
                        + " dir\". The keys are the ones below.")
                .table("Keys of the full-screen help", "Key", "What it does")
                .row(GuideCell.code("Tab"), GuideCell.words("Picks the next button or link."))
                .row(GuideCell.code("Shift+Tab"), GuideCell.words("Picks the button or link before."))
                .row(GuideCell.code("Enter"), GuideCell.words("Follows the picked link, or picks a menu item."))
                .row(GuideCell.code("Up, Down"), GuideCell.words("Scroll one line."))
                .row(GuideCell.code("Page Up, Page Down"), GuideCell.words("Move one page."))
                .row(GuideCell.code("Home, End"), GuideCell.words("Go to the start or the end of the topic."))
                .row(GuideCell.code("F10 or Alt+F"), GuideCell.words("Opens the File menu."))
                .row(GuideCell.code("Alt+S"), GuideCell.words("Opens the Search menu."))
                .row(GuideCell.code("F3"), GuideCell.words("Repeats the last find."))
                .row(GuideCell.code("Alt+C, Alt+N, Alt+B, Alt+I"), GuideCell.words("Show the contents, the next"
                        + " topic, the topic before, and the index."))
                .row(GuideCell.code("Escape"), GuideCell.words("Closes an open menu or search, or leaves the help"
                        + " when none is open."))
                .row(GuideCell.code("Alt+F4"), GuideCell.words("Leaves the help."))
                .paragraph("To search, press [[Alt]]+[[S]], then press [[Enter]] on the first item of the Search"
                        + " menu, the one named Find with three dots after it. The screen asks \"Find what:\". Type"
                        + " the word and press [[Enter]].")
                .paragraph("The second item, \"Repeat Last Find\", searches again. When the word is not in the text,"
                        + " the screen says \"Match not found.\"")
                .command(CommandGuidePages.id(Platform.LINUX, "help"))
                .define("topic", "A subject the full-screen help opens on, such as a command's name or a title in its"
                        + " contents.")
                .define("link", "A word in the help text that you pick with Tab and follow with Enter.")
                .ifSomethingGoesWrong("The reply is \"no such command:\" and the name you typed.",
                        "The name is not a command of this computer. Type help on its own to list the commands it"
                                + " has.",
                        "The reply is \"help: not found. Try: apropos <word>, or man intro\".",
                        "FreeBSD has no help command. Type apropos and a word, or man intro for the introduction.",
                        "The full screen does not close.",
                        "Press Escape once for each open menu or search, then once more to leave the help.",
                        "The reply is \"Match not found.\"",
                        "Check the spelling of the word, or try a shorter word.")
                .seeAlso("jsc:man_pages", "jsc:help_on_a_computer")
                .register();
    }

    private static void manPages(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("man_pages", section).titled("The man command").icon(ComputersGuide.MANUAL)
                .paragraph("man shows the manual page of a command. It works on the UNIX systems: Linux, UNIX System V"
                        + " and FreeBSD. On MC-DOS, MC-NET and Frames, use help instead.")
                .paragraph("When the manuals of the series hold a page for the command, man opens that page in the"
                        + " pager. When they hold none, man prints the command's own page on the terminal, in"
                        + " capitals, such as LS(1).")
                .paragraph("On UNIX and FreeBSD, man also reads the entries of the manuals, which you can open by"
                        + " name. An entry named by its last part, such as graphics-cards, opens in the pager as a"
                        + " page of section 7. The entry on graphics cards is one of them"
                        + " ([](jsc:graphics_cards)).")
                .subheading("Using man")
                .steps("Type man and a command's name, such as man ls on Linux.",
                        "Read the page, then press q to return to the prompt.",
                        "On UNIX or FreeBSD, type man graphics-cards to read the entry on graphics cards.")
                .table("Keys in the pager", "Key", "What it does")
                .row(GuideCell.code("Space"), GuideCell.words("Moves one page down."))
                .row(GuideCell.code("b"), GuideCell.words("Moves one page up."))
                .row(GuideCell.code("j or Down"), GuideCell.words("Moves one line down."))
                .row(GuideCell.code("k or Up"), GuideCell.words("Moves one line up."))
                .row(GuideCell.code("Page Down, Page Up"), GuideCell.words("Move one page each way."))
                .row(GuideCell.code("g or Home"), GuideCell.words("Goes to the top of the page."))
                .row(GuideCell.code("G or End"), GuideCell.words("Goes to the end of the page."))
                .row(GuideCell.code("/"), GuideCell.words("Starts a search for the text you type."))
                .row(GuideCell.code("n"), GuideCell.words("Finds the next match of the search."))
                .row(GuideCell.code("q or Escape"), GuideCell.words("Gives the terminal back."))
                .paragraph("The line at the foot shows the page's name with (7), the line you are on, and \"(press q"
                        + " to quit)\".")
                .paragraph("When you type a word after man, it looks for a match in this order, and stops at the first"
                        + " match it finds.")
                .table("The order man follows", "Step", "What man shows")
                .row(GuideCell.words("A command of this system, with a page in the manuals"),
                        GuideCell.words("That page, opened in the pager."))
                .row(GuideCell.words("An entry of the manuals, on UNIX and FreeBSD"),
                        GuideCell.words("The entry, opened in the pager."))
                .row(GuideCell.words("A command of this system, with no page in the manuals"),
                        GuideCell.words("The command's own page, printed on the terminal in capitals, such as LS(1)."))
                .row(GuideCell.words("A topic such as intro, on FreeBSD"),
                        GuideCell.words("The topic's page, printed on the terminal as INTRO(1)."))
                .row(GuideCell.words("Nothing of these"),
                        GuideCell.words("\"No manual entry for\" and the word you typed."))
                .command(CommandGuidePages.id(Platform.LINUX, "man"))
                .define("section", "The number in brackets after a page's name: (1) for a command, and (7) for a"
                        + " manual entry.")
                .define("pager", "A screen that shows a long text one screenful at a time, with keys to move"
                        + " through it.")
                .ifSomethingGoesWrong("The reply is \"No manual entry for graphics-cards\" on Linux.",
                        "On Linux, man reads only commands. Read the entry with info ([](jsc:info_keys)) instead.",
                        "The reply is \"No manual entry for graphics\".",
                        "Type the whole name, graphics-cards. A part of the name finds nothing.",
                        "The reply is \"What manual page do you want?\".",
                        "You typed man with no name. Type man and a command's name, such as man ls on Linux.",
                        "The hint under it suggests \"man intro\", and that gives \"No manual entry for intro\".",
                        "The topic intro exists on FreeBSD only. On Linux and UNIX System V, use help or apropos.")
                .seeAlso("jsc:whatis_and_apropos", "jsc:info_keys", "jsc:graphics_cards")
                .register();
    }

    private static void whatisAndApropos(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("whatis_and_apropos", section).titled("whatis and apropos").icon(ComputersGuide.MANUAL)
                .paragraph("whatis gives the one line that says what a command is for. It is the quickest answer, and"
                        + " it tells you whether the full page is worth reading.")
                .paragraph("apropos is for when you know the job, not the name. It searches the name and the"
                        + " one-line summary of every command this computer has, and prints the commands that"
                        + " match.")
                .paragraph("The summaries are in English, whichever language the game shows you. Search with English"
                        + " words, such as network or disk.")
                .subheading("Using them")
                .steps("Type whatis and a command's name, such as whatis ls.",
                        "Type apropos and a word, such as apropos network, to find commands by their job.",
                        "Type man and the name of a command you want to read in full.")
                .table("What whatis prints", "Part of the line", "What it tells you")
                .row(GuideCell.words("The name, such as ls"), GuideCell.words("The command this line is about."))
                .row(GuideCell.code("(1)"), GuideCell.words("The section. Every command's page is in section 1."))
                .row(GuideCell.words("A hyphen and the summary"),
                        GuideCell.words("What the command is for, in a few words."))
                .paragraph("Everything you type after apropos is the search. apropos network card looks for the whole"
                        + " phrase, network card, not for two words. Only the commands this computer has are"
                        + " searched.")
                .command(CommandGuidePages.id(Platform.LINUX, "whatis"))
                .command(CommandGuidePages.id(Platform.LINUX, "apropos"))
                .define("one-line summary", "The short description a command gives of itself in one line, which"
                        + " whatis and apropos read.")
                .ifSomethingGoesWrong("The reply ends with \"nothing appropriate.\" after the word you typed.",
                        "No name or summary holds that word. Try a shorter word, or an English word from a summary,"
                                + " such as disk.",
                        "The reply is \"whatis what?\".",
                        "You typed whatis with no name. Type whatis and a command's name, such as whatis ls.",
                        "The reply is \"apropos what?\".",
                        "You typed apropos with no word. Type apropos and the word to search for, such as apropos"
                                + " network.",
                        "apropos finds nothing for a word in another language.",
                        "The search reads the English summaries only. Type the English word.")
                .seeAlso("jsc:man_pages", "jsc:help_command")
                .register();
    }

    private static void infoKeys(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("info_keys", section).titled("info and its keys").icon(ComputersGuide.MANUAL)
                .paragraph("info is the Linux tool for the manuals. Where man shows one page, info shows the manuals"
                        + " as linked screens. Each chapter, section and entry of a manual is a node, and the"
                        + " commands of this computer sit beside them.")
                .paragraph("info takes the whole terminal, as the help screens do. Press q to leave it and get your"
                        + " prompt back. info exists on Linux only, so on UNIX System V and FreeBSD, use man.")
                .subheading("Using info")
                .steps("Type info and press [[Enter]] to open the directory, the list of every manual and command.",
                        "Type info, a space and a name, such as info graphics-cards, to open that entry directly.",
                        "Press [[Tab]] to pick a link, then [[Enter]] to follow it.",
                        "Press q to leave info.")
                .paragraph("The directory is the first screen. It lists every manual, with one menu line for the"
                        + " commands of this computer. Press d at any time to return to it.")
                .table("Keys in info", "Key", "What it does")
                .row(GuideCell.code("q"), GuideCell.words("Quits info."))
                .row(GuideCell.code("n, p"), GuideCell.words("Go to the next node, or to the previous one."))
                .row(GuideCell.code("u"), GuideCell.words("Goes to the node above this one."))
                .row(GuideCell.code("t"), GuideCell.words("Goes to the top node of the manual."))
                .row(GuideCell.code("d"), GuideCell.words("Goes to the directory of every manual."))
                .row(GuideCell.code("l"), GuideCell.words("Goes back to the node you read last."))
                .row(GuideCell.code("h"), GuideCell.words("Shows the keys."))
                .row(GuideCell.code("m"), GuideCell.words("Asks for a menu item by its name. Press Enter after it."))
                .row(GuideCell.code("s or /"), GuideCell.words("Asks for a search text, which Enter runs; Escape or"
                        + " Ctrl+G cancels."))
                .row(GuideCell.code("Space, Page Down"), GuideCell.words("Move one screen down."))
                .row(GuideCell.code("Backspace, Page Up"), GuideCell.words("Move one screen up."))
                .row(GuideCell.code("Up, Down"), GuideCell.words("Move one line."))
                .row(GuideCell.code("Tab, Shift+Tab"), GuideCell.words("Pick the next or the previous link."))
                .row(GuideCell.code("Enter"), GuideCell.words("Follows the picked link."))
                .command(CommandGuidePages.id(Platform.LINUX, "info"))
                .define("node", "One piece of an info manual, a chapter, a section or an entry, read as one screen.")
                .ifSomethingGoesWrong("The reply is \"command not found: info\" on the other systems, and on FreeBSD"
                        + " \"info: not found. Try: apropos <word>, or man intro\".",
                        "info is on Linux only. Use man <command> on UNIX System V and FreeBSD, or help <command>"
                                + " on MC-DOS, MC-NET and Frames.",
                        "The reply is \"No menu item\" and a name, after m.",
                        "The name must match a menu item of this node. Press d or t to go elsewhere, or pick a link"
                                + " with Tab.",
                        "The reply is \"No 'Next' pointer for this node.\", or the same for Prev or Up.",
                        "This node has no neighbour in that direction. Use n, p, u or l to go somewhere else.",
                        "The reply is \"No previous nodes.\"",
                        "You have not read another node yet, so there is nothing to go back to. Read a node with n"
                                + " first.",
                        "The reply is \"Search failed:\" followed by the text you searched for.",
                        "The text is not in this manual. Try a shorter word.")
                .seeAlso("jsc:man_pages", "jsc:help_command")
                .register();
    }

    private static void listcmd(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("listcmd", section).titled("The listcmd command").icon(ComputersGuide.MANUAL)
                .paragraph("listcmd lists everything this computer can run: its commands, whichever system they belong"
                        + " to, and the programs installed on it. It prints one list, and nothing is hidden from it.")
                .paragraph("Normally listcmd does not exist, because each system teaches its own commands in its own"
                        + " way. The server owner can switch it on for every computer, if you would rather read one"
                        + " list.")
                .subheading("Turning it on")
                .steps("Open jscomputers-server.toml in the serverconfig folder of the world.",
                        "Set prompt.list_commands to true.",
                        "Save the file.")
                .paragraph("In single-player, it is the same file, in the serverconfig folder of your world.")
                .table("Where the switch is", "Setting", "Value")
                .row(GuideCell.words("File"), GuideCell.code("jscomputers-server.toml"))
                .row(GuideCell.words("Folder"), GuideCell.words("serverconfig, inside the world's folder"))
                .row(GuideCell.words("Key"), GuideCell.code("prompt.list_commands"))
                .row(GuideCell.words("Default"), GuideCell.code("false"))
                .table("The two parts of the list", "Heading", "What it lists")
                .row(GuideCell.code("COMMANDS"), GuideCell.words("One row per command this computer has, with its"
                        + " one-line summary."))
                .row(GuideCell.code("PROGRAMS"), GuideCell.words("One row per installed program, with its name and its"
                        + " id."))
                .paragraph("Each computer lists only what it has. The commands depend on its system, and the programs"
                        + " depend on what is installed on it. When the switch is on, listcmd also appears in the"
                        + " list that help prints, like any other command.")
                .command(CommandGuidePages.id(Platform.LINUX, "listcmd"))
                .ifSomethingGoesWrong("The reply is \"command not found: listcmd\".",
                        "The switch is off on this server, so the command does not exist. Ask the server owner to set"
                                + " prompt.list_commands to true.",
                        "The list is shorter on one computer than on another.",
                        "Each computer lists only what it has. A program appears under PROGRAMS once it is installed"
                                + " on that computer.")
                .seeAlso("jsc:help_command", "jsc:programs")
                .register();
    }
}
