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
 * The pages of the editors that take the terminal (vim, emacs, vi and ee): how each one is started, its keys,
 * the messages it shows and how to leave it.
 */
final class TerminalEditorsPages {

    private TerminalEditorsPages() {
    }

    /** Declares the section and the entries of the editors at the terminal, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("the_editors").titled("The editors")
                .in(chapter).register();
        editorsOverview(guide, section);
        vim(guide, section);
        emacs(guide, section);
        vi(guide, section);
        ee(guide, section);
    }

    private static void editorsOverview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("editors_overview", section).titled("Editors at the prompt")
                .paragraph("A file holds text: a note, a setting or a program. To change the text you need an"
                        + " editor. At the prompt, the editor takes over the whole screen while you work, and gives"
                        + " the screen back when you leave. The prompt is where you type commands"
                        + " ([](jsc:shell_prompt)).")
                .paragraph("Four editors take the screen: [vim](jsc:vim), [emacs](jsc:emacs), [vi](jsc:vi) and"
                        + " [ee](jsc:ee). Each one has its own keys, and its entry lists them.")
                .paragraph("The arrow keys move the caret.")
                .define("Caret", "The cursor in the text: the place where the next letter goes.")
                .paragraph("You start an editor by typing its name, a space, and the name of the file. The file does"
                        + " not have to exist. The editor opens an empty file under that name, and the file is made"
                        + " when you first save it.")
                .steps("Type the name of the editor, a space and the name of the file, then press [[Enter]].",
                        "Write or change the text, with the keys of that editor.",
                        "Save the file, then leave the editor with that editor's keys.")
                .table("Editors and the systems they run on", "Editor", "Systems it runs on",
                        "Comes with the system")
                .row(GuideCell.code("vim"),
                        GuideCell.words("MC-DOS, Frames, Linux, FreeBSD and UNIX System V"),
                        GuideCell.words("No, you install it"))
                .row(GuideCell.code("emacs"),
                        GuideCell.words("MC-DOS, Frames, Linux, FreeBSD and UNIX System V"),
                        GuideCell.words("No, you install it"))
                .row(GuideCell.code("vi"), GuideCell.words("FreeBSD and UNIX System V"), GuideCell.words("Yes"))
                .row(GuideCell.code("ee"), GuideCell.words("FreeBSD"), GuideCell.words("Yes"))
                .paragraph("To install vim or emacs, see [](jsc:programs). Until an editor is installed, the prompt"
                        + " does not know its name, so install it before you type the name.")
                .table("Free disk space needed to install an editor", "Editor", "Free disk space needed")
                .row(GuideCell.code("vim"), GuideCell.amount(8, "MB"))
                .row(GuideCell.code("emacs"), GuideCell.amount(24, "MB"))
                .ifSomethingGoesWrong(
                        "The prompt says \"command not found: vim\" and \"type 'help' to list commands\".",
                        "The editor is not installed on this computer. Install it ([](jsc:programs)), then type"
                                + " its name again.",
                        "On FreeBSD the same problem looks like \"vim: not found. Try: apropos <word>, or man"
                                + " intro\".",
                        "The cause and the cure are the same.",
                        "You type ee on Linux or MC-DOS, and the prompt says the command is not found.",
                        "ee comes only with FreeBSD ([](jsc:the_systems)). On other systems, use another editor.",
                        "You type the name of an editor and nothing else, and the prompt says \"usage: vim"
                                + " <file>\".",
                        "Type the name of a file after the editor's name, with a space between them.")
                .seeAlso("jsc:programs", "jsc:the_systems")
                .register();
    }

    private static void vim(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("vim", section).titled("Vim")
                .paragraph("Vim works in modes. A mode decides what a key does: in one mode a letter is typed into"
                        + " the file, and in another the same letter is a command that moves the caret or deletes"
                        + " text.")
                .paragraph("The Vim here is a modern copy of the editor. It is not all of Vim. It has what you need"
                        + " to write and change a program: moving, typing, deleting, copying a line, undoing and a few"
                        + " colon commands. It has no search and no split windows.")
                .paragraph("The bottom row shows the name of the file in quotes, with [+] after it when the text has"
                        + " changes that are not saved. In insert mode it also says -- INSERT --.")
                .define("Mode", "The state that decides what a key does: normal, insert or command.")
                .subheading("The three modes")
                .paragraph("You start in normal mode. Press [[Esc]] to return to normal mode from insert mode or"
                        + " from command mode.")
                .table("The modes", "Mode", "What the keys do")
                .row(GuideCell.words("Normal"),
                        GuideCell.words("A letter is a command: it moves the caret, deletes, copies or starts"
                                + " insert mode."))
                .row(GuideCell.words("Insert"),
                        GuideCell.words("Letters, digits and symbols are typed into the file."))
                .row(GuideCell.words("Command"),
                        GuideCell.words("What you type after the colon runs when you press [[Enter]]."))
                .subheading("Keys in normal mode")
                .table("Keys in normal mode", "Key", "What it does")
                .row(GuideCell.code("i"), GuideCell.words("Starts insert mode before the caret."))
                .row(GuideCell.code("a"), GuideCell.words("Starts insert mode after the caret."))
                .row(GuideCell.code("A"), GuideCell.words("Starts insert mode at the end of the line."))
                .row(GuideCell.code("I"), GuideCell.words("Starts insert mode at the start of the line."))
                .row(GuideCell.code("o"), GuideCell.words("Opens a new line below and starts insert mode there."))
                .row(GuideCell.code("O"), GuideCell.words("Opens a new line above and starts insert mode there."))
                .row(GuideCell.code("h j k l"), GuideCell.words("Moves left, down, up and right."))
                .row(GuideCell.code("w"), GuideCell.words("Moves to the start of the next word, on the next line"
                        + " when this line has no more words."))
                .row(GuideCell.code("b"), GuideCell.words("Moves to the start of the previous word."))
                .row(GuideCell.code("e"), GuideCell.words("Moves to the end of the word the caret is on, or of the"
                        + " next one."))
                .row(GuideCell.code("0"), GuideCell.words("Moves to the start of the line."))
                .row(GuideCell.code("^"), GuideCell.words("Moves to the first character that is not a space."))
                .row(GuideCell.code("$"), GuideCell.words("Moves to the end of the line."))
                .row(GuideCell.code("G"), GuideCell.words("Moves to the last line of the file."))
                .row(GuideCell.code("gg"), GuideCell.words("Moves to the first line of the file."))
                .row(GuideCell.code("x"), GuideCell.words("Deletes the character under the caret, unless the caret"
                        + " is at the end of the line."))
                .row(GuideCell.code("D"), GuideCell.words("Deletes from the caret to the end of the line."))
                .row(GuideCell.code("dd"), GuideCell.words("Deletes the whole line and keeps it in the register."))
                .row(GuideCell.code("dw"), GuideCell.words("Deletes up to the next word. When the next word is on"
                        + " another line, it deletes to the end of this line."))
                .row(GuideCell.code("yy"), GuideCell.words("Copies the whole line into the register."))
                .row(GuideCell.code("p"), GuideCell.words("Puts the register back on the line below."))
                .row(GuideCell.code("P"), GuideCell.words("Puts the register back on the line above."))
                .row(GuideCell.code("u"), GuideCell.words("Undoes the last change."))
                .row(GuideCell.code("Ctrl+R"), GuideCell.words("Redoes the change that undo took back, in normal"
                        + " mode."))
                .row(GuideCell.code(":"), GuideCell.words("Starts a colon command. The bottom row shows the colon"
                        + " and what you type."))
                .row(GuideCell.code("Arrows, Home, End"), GuideCell.words("Move the caret."))
                .row(GuideCell.code("PageUp, PageDown"), GuideCell.words("Move the caret ten lines up or down."))
                .row(GuideCell.code("Enter"), GuideCell.words("Moves down one line."))
                .row(GuideCell.code("Backspace"), GuideCell.words("Moves left one column."))
                .row(GuideCell.code("Esc"), GuideCell.words("Stays in normal mode, and cancels a half-typed dd, dw,"
                        + " yy or gg."))
                .paragraph("The keys d, y and g wait for a second key. Only dd, dw, yy and gg do anything. Any other"
                        + " second key is ignored, and the wait ends.")
                .define("Register", "The place where dd and yy keep the line they copied, so that p and P can put it"
                        + " back.")
                .subheading("Keys in insert mode")
                .table("Keys in insert mode", "Key", "What it does")
                .row(GuideCell.words("Letters, digits and symbols"), GuideCell.words("Are typed into the file."))
                .row(GuideCell.code("Enter"), GuideCell.words("Starts a new line with the same indent as the line"
                        + " above. After a brace {, it indents four more spaces."))
                .row(GuideCell.code("Tab"), GuideCell.words("Types four spaces."))
                .row(GuideCell.code("Backspace"), GuideCell.words("Deletes the character before the caret."))
                .row(GuideCell.code("Delete"), GuideCell.words("Deletes the character under the caret."))
                .row(GuideCell.code("Esc"), GuideCell.words("Returns to normal mode. One undo takes back the whole"
                        + " insert."))
                .subheading("Colon commands")
                .paragraph("Type a colon command after the colon, then press [[Enter]]. Nothing happens until you"
                        + " press it.")
                .table("Colon commands", "Command", "What it does")
                .row(GuideCell.code(":w"), GuideCell.words("Writes the file to disk."))
                .row(GuideCell.code(":q"), GuideCell.words("Quits. It is refused while the text has changes that are"
                        + " not saved."))
                .row(GuideCell.code(":q!"), GuideCell.words("Quits without saving, even with changes."))
                .row(GuideCell.code(":wq"), GuideCell.words("Writes the file, then quits."))
                .row(GuideCell.code(":x"), GuideCell.words("Writes the file, then quits."))
                .row(GuideCell.code(":wq!"), GuideCell.words("Writes the file, then quits."))
                .row(GuideCell.code(":x!"), GuideCell.words("Writes the file, then quits."))
                .row(GuideCell.code(":<number>"), GuideCell.words("Goes to that line. 0 goes to line 1, and a number"
                        + " past the end goes to the last line."))
                .row(GuideCell.code(": Enter"), GuideCell.words("With nothing after the colon, does nothing."))
                .subheading("Messages on the bottom row")
                .table("Messages", "Message", "When you see it")
                .row(GuideCell.words("\"hello.txt\" [New]"),
                        GuideCell.words("You open a file that does not exist yet."))
                .row(GuideCell.words("\"hello.txt\" written"),
                        GuideCell.words("You write the file with :w, :wq or :x."))
                .row(GuideCell.words("1 line yanked"), GuideCell.words("You copy a line with yy."))
                .row(GuideCell.words("1 change; before"), GuideCell.words("You undo a change with u."))
                .row(GuideCell.words("1 change; after"), GuideCell.words("You redo a change with Ctrl+R."))
                .row(GuideCell.words("Already at oldest change"), GuideCell.words("You press u, and nothing is left"
                        + " to undo."))
                .row(GuideCell.words("Already at newest change"), GuideCell.words("You press Ctrl+R, and nothing is"
                        + " left to redo."))
                .row(GuideCell.words("E492: not an editor command: followed by what you typed"),
                        GuideCell.words("The colon command is not one of the commands above."))
                .row(GuideCell.words("E37: no write since last change (add ! to override)"),
                        GuideCell.words("You try to quit with changes that are not saved."))
                .row(GuideCell.words("Too long to save: followed by two numbers"),
                        GuideCell.words("The text is too long to save, and the file keeps its old content."))
                .command(CommandGuidePages.id(Platform.LINUX, "vim"))
                .ifSomethingGoesWrong(
                        "The bottom row says E37: no write since last change (add ! to override).",
                        "The text has changes that are not saved. Type :w to save, then :q. Or type :q! to leave"
                                + " without saving.",
                        "The bottom row says E492: not an editor command, followed by what you typed.",
                        "The colon command is not in the table above. Check its spelling.",
                        "The bottom row says Too long to save.",
                        "The text is more than 32,767 characters, so the file keeps its old content. Make the text"
                                + " shorter, or split it into more than one file.",
                        "You type 3 and then dd, and only one line goes.",
                        "Counts are not supported here. Type dd once for each line.",
                        "After d, y or g, nothing happens.",
                        "Only dd, dw, yy and gg are commands. Press [[Esc]] to cancel the wait, then type a command.",
                        "The bottom row says Already at oldest change.",
                        "There is nothing left to undo. Make the change again, or leave the file as it is.")
                .seeAlso("jsc:emacs", "jsc:vi", "jsc:ee")
                .register();
    }

    private static void emacs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("emacs", section).titled("Emacs")
                .paragraph("Emacs has no modes. Every key you press types a letter, unless you hold Control or Alt"
                        + " with it. A command is one of those held keys, sometimes followed by a second key. The"
                        + " keys with Control are written C-, and the keys with Alt are written M-.")
                .paragraph("While a command waits for its next key, the bottom row shows the keys you pressed so"
                        + " far, such as C-x-. Otherwise it shows the file's name. For a file without changes it"
                        + " reads -UUU:----F1 and then the name. For a file with changes that are not saved, the two"
                        + " dashes after -UUU: are two asterisks.")
                .define("Chord", "A key held with Control or Alt, sometimes followed by a second key, such as C-x"
                        + " C-s.")
                .define("Buffer", "The text the editor holds in memory until you save it.")
                .paragraph("Letters, digits and symbols type into the file. [[Enter]] starts a new line with the same"
                        + " indent as the line above, and [[Tab]] types four spaces. The arrow keys, Home and End"
                        + " move the caret, and PageUp and PageDown move it ten lines.")
                .subheading("Saving and leaving")
                .steps("Press [[Ctrl]]+[[X]], then [[Ctrl]]+[[S]], to save the text.",
                        "Press [[Ctrl]]+[[X]], then [[Ctrl]]+[[C]], to leave the editor.",
                        "If the bottom row asks a question, answer it with y or n.")
                .table("Chords and commands", "Keys", "What it does", "Message on the bottom row")
                .row(GuideCell.code("C-x C-s"), GuideCell.words("Saves the file."),
                        GuideCell.words("\"hello.txt\" written"))
                .row(GuideCell.code("C-x C-c"), GuideCell.words("Leaves the editor. With changes that are not saved,"
                        + " it asks first."),
                        GuideCell.words("Modified buffers exist; exit anyway? (y or n)"))
                .row(GuideCell.code("C-x u"), GuideCell.words("Undoes the last change."),
                        GuideCell.words("Undo, or No further undo information at the start of the text"))
                .row(GuideCell.code("C-g"), GuideCell.words("Cancels what you are doing."), GuideCell.words("Quit"))
                .row(GuideCell.code("C-b"), GuideCell.words("Moves one character left."), GuideCell.words("none"))
                .row(GuideCell.code("C-f"), GuideCell.words("Moves one character right."), GuideCell.words("none"))
                .row(GuideCell.code("C-p"), GuideCell.words("Moves one line up."), GuideCell.words("none"))
                .row(GuideCell.code("C-n"), GuideCell.words("Moves one line down."), GuideCell.words("none"))
                .row(GuideCell.code("C-a"), GuideCell.words("Moves to the start of the line."), GuideCell.words("none"))
                .row(GuideCell.code("C-e"), GuideCell.words("Moves to the end of the line."), GuideCell.words("none"))
                .row(GuideCell.code("C-d"), GuideCell.words("Deletes the character after the caret."),
                        GuideCell.words("none"))
                .row(GuideCell.code("C-k"), GuideCell.words("Kills the text from the caret to the end of the line;"
                        + " at the end of a line, kills the line break."), GuideCell.words("none"))
                .row(GuideCell.code("C-y"), GuideCell.words("Yanks back the text that C-k killed last."),
                        GuideCell.words("none"))
                .row(GuideCell.code("M-<"), GuideCell.words("Moves to the start of the file."), GuideCell.words("none"))
                .row(GuideCell.code("M->"), GuideCell.words("Moves to the end of the file."), GuideCell.words("none"))
                .row(GuideCell.code("M-x compile"), GuideCell.words("Compiles the file, as described below."),
                        GuideCell.words("Compilation finished"))
                .paragraph("Emacs does not know the other chords that you may know from Emacs, such as C-s for"
                        + " search. It says so: C-s is undefined, and the keys you typed are thrown away.")
                .subheading("Leaving with changes that are not saved")
                .paragraph("When the text has changes that are not saved, C-x C-c asks: Modified buffers exist; exit"
                        + " anyway? (y or n). Press y to leave without saving. Press n to stay in the file, then save"
                        + " with C-x C-s.")
                .paragraph("While the question is open, Ctrl+G or Esc cancels it, and the bottom row says Quit. Any"
                        + " other key asks again: Please answer y or n.")
                .define("Kill", "To cut text and keep it, so that yank can put it back.")
                .define("Yank", "Pasting back the text that was cut last.")
                .subheading("Compiling with M-x compile")
                .paragraph("M-x compile compiles the text as it is in the buffer, the way the compiler of its"
                        + " language reads a file. It does not save the file first. The result opens in a second"
                        + " area under the text, so the mistakes can be read while the text stays on the screen.")
                .paragraph("The ending of the file's name decides which language the file is in. Which languages"
                        + " can compile depends on the compilers installed on the machine.")
                .steps("Press [[Alt]]+[[X]].", "Type compile, then press [[Enter]].",
                        "Read the second area under the text.")
                .table("The second area", "Line", "What it means")
                .row(GuideCell.words("The language, then the file name"),
                        GuideCell.words("Which language compiled which file."))
                .row(GuideCell.words("<file> -> <N> lines of assembly"),
                        GuideCell.words("The compile worked, and N is the number of lines of assembly it made."))
                .row(GuideCell.words("Compilation finished"), GuideCell.words("The compile is over."))
                .row(GuideCell.words("Each complaint, one line each"),
                        GuideCell.words("One mistake the compiler found."))
                .row(GuideCell.words("Compilation exited abnormally with <N> error(s)"),
                        GuideCell.words("The compile failed, and N is the number of errors."))
                .row(GuideCell.words("no compiler knows <file>"),
                        GuideCell.words("No language on this machine claims the file's ending."))
                .command(CommandGuidePages.id(Platform.LINUX, "emacs"))
                .ifSomethingGoesWrong(
                        "The second area says no compiler knows, followed by the file name.",
                        "The file's ending is not one that a language on this machine uses. Save the file with the"
                                + " ending of its language, then compile again.",
                        "You press C-x C-c, and then a key that is not y or n.",
                        "The question repeats Please answer y or n. Press y or n.",
                        "The bottom row asks the question, and you want to keep working.",
                        "Press n to return to the text, then save with C-x C-s.",
                        "The bottom row says C-s is undefined.",
                        "This editor has no command on that chord. Use the chords in the table above.")
                .seeAlso("jsc:vim", "jsc:vi", "jsc:ee")
                .register();
    }

    private static void vi(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("vi", section).titled("vi")
                .paragraph("vi is the editor that comes with FreeBSD and UNIX System V. It is the same engine as"
                        + " [Vim](jsc:vim): the same keys, the same modes and the same colon commands.")
                .paragraph("The machine chooses which vi you get, and you cannot change it. FreeBSD gets nvi, its own"
                        + " version of vi. UNIX System V gets the original, with its own words.")
                .define("nvi", "FreeBSD's own version of vi.")
                .paragraph("The words differ from Vim's. vi keeps no name row of its own: its bottom row shows only"
                        + " the messages below, so there is no file name and no -- INSERT -- text. It shows no colour.")
                .table("The words of each voice", "When", "FreeBSD (nvi)", "UNIX System V")
                .row(GuideCell.words("You open a file that exists"),
                        GuideCell.code("hello.txt: unmodified: line 1"),
                        GuideCell.code("\"hello.txt\" 3 lines, 42 characters"))
                .row(GuideCell.words("You open a file that does not exist yet"),
                        GuideCell.code("hello.txt: new file: line 1"),
                        GuideCell.code("\"hello.txt\" [New file]"))
                .row(GuideCell.words("You write the file with :w"),
                        GuideCell.code("hello.txt: 3 lines, 42 characters"),
                        GuideCell.code("\"hello.txt\" 3 lines, 42 characters"))
                .paragraph("The numbers count the lines and the characters of the text. In this example, the file has"
                        + " 3 lines and 42 characters.")
                .paragraph("The keys are the same as in Vim, so the table of normal-mode keys in the Vim entry works"
                        + " here too. Undo and the yank of a line say the same words as in Vim, in both voices.")
                .command(CommandGuidePages.id(Platform.FREEBSD, "vi"))
                .ifSomethingGoesWrong(
                        "You type vi and the prompt says \"command not found: vi\".",
                        "vi comes only with FreeBSD and UNIX System V. On other systems, use [vim](jsc:vim).",
                        "The bottom row says E37: no write since last change (add ! to override).",
                        "Type :w to save, then :q. Or type :q! to leave without saving.")
                .seeAlso("jsc:vim", "jsc:the_systems")
                .register();
    }

    private static void ee(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("ee", section).titled("ee")
                .paragraph("ee is a small editor, and it comes only with FreeBSD. It has no modes. Every key you press"
                        + " types a letter, except the keys held with Control and the Escape key.")
                .paragraph("While the info window is on, five rows of shortcuts sit on the screen. Each row"
                        + " shows a key and the name of what it does. The ^ in front of a letter means Control: ^k"
                        + " is Control with K.")
                .define("Info window", "The five rows of shortcuts on the screen, which you can hide in"
                        + " the settings.")
                .table("The shortcut rows", "Key", "Name on the screen", "What it does")
                .row(GuideCell.code("^["), GuideCell.words("(escape) menu"), GuideCell.words("Opens the main menu."))
                .row(GuideCell.code("^y"), GuideCell.words("search prompt"), GuideCell.words("Asks for the text to"
                        + " search for."))
                .row(GuideCell.code("^k"), GuideCell.words("delete line"), GuideCell.words("Deletes the line the caret"
                        + " is on, and keeps it for undelete line."))
                .row(GuideCell.code("^p"), GuideCell.words("prev li"), GuideCell.words("Moves up one line."))
                .row(GuideCell.code("^g"), GuideCell.words("prev page"), GuideCell.words("Moves up one screen."))
                .row(GuideCell.code("^o"), GuideCell.words("ascii code"), GuideCell.words("Asks for a number from 0"
                        + " to 255, and types that character."))
                .row(GuideCell.code("^x"), GuideCell.words("search"), GuideCell.words("Searches again for the last text"
                        + " you searched for."))
                .row(GuideCell.code("^l"), GuideCell.words("undelete line"), GuideCell.words("Puts the last deleted"
                        + " line back under the caret's line."))
                .row(GuideCell.code("^n"), GuideCell.words("next li"), GuideCell.words("Moves down one line."))
                .row(GuideCell.code("^v"), GuideCell.words("next page"), GuideCell.words("Moves down one screen."))
                .row(GuideCell.code("^u"), GuideCell.words("end of file"), GuideCell.words("Moves to the last line of"
                        + " the file."))
                .row(GuideCell.code("^a"), GuideCell.words("begin of line"), GuideCell.words("Moves to the start of"
                        + " the line."))
                .row(GuideCell.code("^w"), GuideCell.words("delete word"), GuideCell.words("Deletes the word after the"
                        + " caret, and keeps it for restore word."))
                .row(GuideCell.code("^b"), GuideCell.words("back 1 char"), GuideCell.words("Moves one character left."))
                .row(GuideCell.code("^z"), GuideCell.words("next word"), GuideCell.words("Moves to the start of the"
                        + " next word."))
                .row(GuideCell.code("^t"), GuideCell.words("top of text"), GuideCell.words("Moves to the start of the"
                        + " text."))
                .row(GuideCell.code("^e"), GuideCell.words("end of line"), GuideCell.words("Moves to the end of the"
                        + " line."))
                .row(GuideCell.code("^r"), GuideCell.words("restore word"), GuideCell.words("Puts back the word that"
                        + " delete word removed last."))
                .row(GuideCell.code("^f"), GuideCell.words("forward char"),
                        GuideCell.words("Moves one character right."))
                .row(GuideCell.code("^c"), GuideCell.words("command"), GuideCell.words("Asks for a command: write, quit"
                        + " or exit."))
                .row(GuideCell.code("^d"), GuideCell.words("delete char"), GuideCell.words("Deletes the character under"
                        + " the caret, and keeps it for undelete char."))
                .row(GuideCell.code("^j"), GuideCell.words("undelete char"), GuideCell.words("Puts back the character"
                        + " that delete char removed last."))
                .paragraph("The fifth row ends with the tip ESC-Enter: exit. Names such as prev li and next li are"
                        + " short, because the screen is narrow.")
                .subheading("The main menu")
                .paragraph("Press [[Esc]] to open the main menu, titled ee main menu. Move with the Up and Down keys,"
                        + " choose with [[Enter]], or close the menu with [[Esc]].")
                .table("The main menu", "Item", "What it does")
                .row(GuideCell.words("leave editor"), GuideCell.words("Asks what to do with the changes: save changes,"
                        + " which is selected at first, or no save."))
                .row(GuideCell.words("help"), GuideCell.words("Shows the page ee command reference; press any key to"
                        + " close it."))
                .row(GuideCell.words("file operations"), GuideCell.words("read a file asks for a file and puts its"
                        + " text in at the caret; save file writes the open file."))
                .row(GuideCell.words("redraw screen"), GuideCell.words("Draws the screen again."))
                .row(GuideCell.words("settings"), GuideCell.words("info window: on and info window: off show or hide"
                        + " the five rows."))
                .row(GuideCell.words("search"), GuideCell.words("Asks for the text to search for."))
                .subheading("Leaving quickly")
                .steps("Press [[Esc]] to open the main menu.", "Press [[Enter]] to choose leave editor.",
                        "Press [[Enter]] again to choose save changes, which is selected at first.")
                .paragraph("Choose no save instead to leave without writing the file. ee does not ask again before it"
                        + " leaves.")
                .subheading("Opening, saving and the position row")
                .paragraph("When you open a file, the bottom row says \"hello.txt\" [New file] for a new file. For an"
                        + " existing file it says the name, the lines and the characters, as in \"hello.txt\" 3 lines,"
                        + " 42 characters. Saving says the same thing.")
                .paragraph("Under the shortcut rows, a row shows where the caret is: its line, its column and how many"
                        + " lines it is from the top of the screen.")
                .subheading("The command line")
                .paragraph("Press ^c. The bottom row shows the label \"Command\" and waits for a word. Type the word"
                        + " and press [[Enter]].")
                .table("Command words", "Word", "What it does")
                .row(GuideCell.code("write"), GuideCell.words("Saves the file."))
                .row(GuideCell.code("quit"), GuideCell.words("Leaves the editor at once, without saving."))
                .row(GuideCell.code("exit"), GuideCell.words("Saves the file, then leaves the editor."))
                .subheading("Searching and prompts")
                .paragraph("Press ^y, or choose search in the main menu, and type the text. Press [[Enter]]. The caret"
                        + " moves to the text. ^x searches again for the last text you typed.")
                .paragraph("Any prompt on the bottom row waits for you. Press [[Enter]] to answer it, [[Esc]] to"
                        + " cancel it, and Backspace to correct what you typed.")
                .command(CommandGuidePages.id(Platform.FREEBSD, "ee"))
                .ifSomethingGoesWrong(
                        "The bottom row says Unknown command, followed by what you typed.",
                        "The command line knows only write, quit and exit. Type one of those words.",
                        "The bottom row says Not found, followed by your text.",
                        "The text is not in the file, or it is spelled differently. Check it and search again.",
                        "You press ^x, and nothing happens.",
                        "No search text has been typed yet. Search with ^y first, then ^x searches again.",
                        "You choose read a file, and the bottom row says \"name\": No such file or directory.",
                        "The file does not exist, or its name is wrong. Type the name again, exactly as the file has"
                                + " it.",
                        "You leave with no save or with quit, and your changes are gone.",
                        "ee does not ask first when you leave with no save or with quit. Choose save changes in the"
                                + " main menu, or type write at the command prompt (^c), before you leave.",
                        "The shortcut rows have disappeared.",
                        "The info window is off. Open the main menu, choose settings, then info window: on.")
                .seeAlso("jsc:vi", "jsc:vim", "jsc:the_systems")
                .register();
    }
}
