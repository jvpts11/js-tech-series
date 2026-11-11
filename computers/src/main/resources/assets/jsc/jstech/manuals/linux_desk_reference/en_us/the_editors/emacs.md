---
section: the_editors
order: 276
---
# Emacs

Emacs has no modes. Every key you press types a letter, unless you hold Control or Alt with it. A command is one of those held keys, sometimes followed by a second key. The keys with Control are written C-, and the keys with Alt are written M-.

While a command waits for its next key, the bottom row shows the keys you pressed so far, such as C-x-. Otherwise it shows the file's name. For a file without changes it reads -UUU:----F1 and then the name. For a file with changes that are not saved, the two dashes after -UUU: are two asterisks.

> [!DEFINE] Chord
> A key held with Control or Alt, sometimes followed by a second key, such as C-x C-s.

> [!DEFINE] Buffer
> The text the editor holds in memory until you save it.

Letters, digits and symbols type into the file. [[Enter]] starts a new line with the same indent as the line above, and [[Tab]] types four spaces. The arrow keys, Home and End move the caret, and PageUp and PageDown move it ten lines.

## Saving and leaving

1. Press [[Ctrl]]+[[X]], then [[Ctrl]]+[[S]], to save the text.
2. Press [[Ctrl]]+[[X]], then [[Ctrl]]+[[C]], to leave the editor.
3. If the bottom row asks a question, answer it with y or n.

Table: Chords and commands

| Keys | What it does | Message on the bottom row |
| --- | --- | --- |
| `C-x C-s` | Saves the file. | "hello.txt" written |
| `C-x C-c` | Leaves the editor. With changes that are not saved, it asks first. | Modified buffers exist; exit anyway? (y or n) |
| `C-x u` | Undoes the last change. | Undo, or No further undo information at the start of the text |
| `C-g` | Cancels what you are doing. | Quit |
| `C-b` | Moves one character left. | none |
| `C-f` | Moves one character right. | none |
| `C-p` | Moves one line up. | none |
| `C-n` | Moves one line down. | none |
| `C-a` | Moves to the start of the line. | none |
| `C-e` | Moves to the end of the line. | none |
| `C-d` | Deletes the character after the caret. | none |
| `C-k` | Kills the text from the caret to the end of the line; at the end of a line, kills the line break. | none |
| `C-y` | Yanks back the text that C-k killed last. | none |
| `M-<` | Moves to the start of the file. | none |
| `M->` | Moves to the end of the file. | none |
| `M-x compile` | Compiles the file, as described below. | Compilation finished |

Emacs does not know the other chords that you may know from Emacs, such as C-s for search. It says so: C-s is undefined, and the keys you typed are thrown away.

## Leaving with changes that are not saved

When the text has changes that are not saved, C-x C-c asks: Modified buffers exist; exit anyway? (y or n). Press y to leave without saving. Press n to stay in the file, then save with C-x C-s.

While the question is open, Ctrl+G or Esc cancels it, and the bottom row says Quit. Any other key asks again: Please answer y or n.

> [!DEFINE] Kill
> To cut text and keep it, so that yank can put it back.

> [!DEFINE] Yank
> Pasting back the text that was cut last.

## Compiling with M-x compile

M-x compile compiles the text as it is in the buffer, the way the compiler of its language reads a file. It does not save the file first. The result opens in a second area under the text, so the mistakes can be read while the text stays on the screen.

The ending of the file's name decides which language the file is in. Which languages can compile depends on the compilers installed on the machine.

1. Press [[Alt]]+[[X]].
2. Type compile, then press [[Enter]].
3. Read the second area under the text.

Table: The second area

| Line | What it means |
| --- | --- |
| The language, then the file name | Which language compiled which file. |
| <file> -> <N> lines of assembly | The compile worked, and N is the number of lines of assembly it made. |
| Compilation finished | The compile is over. |
| Each complaint, one line each | One mistake the compiler found. |
| Compilation exited abnormally with <N> error(s) | The compile failed, and N is the number of errors. |
| no compiler knows <file> | No language on this machine claims the file's ending. |

```command
id: jsc:command/linux/emacs
```

## If something goes wrong

> [!PROBLEM] The second area says no compiler knows, followed by the file name.
> The file's ending is not one that a language on this machine uses. Save the file with the ending of its language, then compile again.

> [!PROBLEM] You press C-x C-c, and then a key that is not y or n.
> The question repeats Please answer y or n. Press y or n.

> [!PROBLEM] The bottom row asks the question, and you want to keep working.
> Press n to return to the text, then save with C-x C-s.

> [!PROBLEM] The bottom row says C-s is undefined.
> This editor has no command on that chord. Use the chords in the table above.

> [!SEE] jsc:vim, jsc:vi, jsc:ee
