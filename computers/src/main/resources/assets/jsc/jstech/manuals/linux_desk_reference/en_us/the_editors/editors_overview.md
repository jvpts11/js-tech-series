---
section: the_editors
order: 274
---
# Editors at the prompt

A file holds text: a note, a setting or a program. To change the text you need an editor. At the prompt, the editor takes over the whole screen while you work, and gives the screen back when you leave. The prompt is where you type commands ([](jsc:shell_prompt)).

Four editors take the screen: [vim](jsc:vim), [emacs](jsc:emacs), [vi](jsc:vi) and [ee](jsc:ee). Each one has its own keys, and its entry lists them.

The arrow keys move the caret.

> [!DEFINE] Caret
> The cursor in the text: the place where the next letter goes.

You start an editor by typing its name, a space, and the name of the file. The file does not have to exist. The editor opens an empty file under that name, and the file is made when you first save it.

1. Type the name of the editor, a space and the name of the file, then press [[Enter]].
2. Write or change the text, with the keys of that editor.
3. Save the file, then leave the editor with that editor's keys.

Table: Editors and the systems they run on

| Editor | Systems it runs on | Comes with the system |
| --- | --- | --- |
| `vim` | MC-DOS, Frames, Linux, FreeBSD and UNIX System V | No, you install it |
| `emacs` | MC-DOS, Frames, Linux, FreeBSD and UNIX System V | No, you install it |
| `vi` | FreeBSD and UNIX System V | Yes |
| `ee` | FreeBSD | Yes |

To install vim or emacs, see [](jsc:programs). Until an editor is installed, the prompt does not know its name, so install it before you type the name.

```table
{"caption": "Free disk space needed to install an editor", "heads": ["Editor", "Free disk space needed"], "rows": [[{"literal": "vim"}, {"amount": 8, "unit": "MB"}], [{"literal": "emacs"}, {"amount": 24, "unit": "MB"}]]}
```

## If something goes wrong

> [!PROBLEM] The prompt says "command not found: vim" and "type 'help' to list commands".
> The editor is not installed on this computer. Install it ([](jsc:programs)), then type its name again.

> [!PROBLEM] On FreeBSD the same problem looks like "vim: not found. Try: apropos <word>, or man intro".
> The cause and the cure are the same.

> [!PROBLEM] You type ee on Linux or MC-DOS, and the prompt says the command is not found.
> ee comes only with FreeBSD ([](jsc:the_systems)). On other systems, use another editor.

> [!PROBLEM] You type the name of an editor and nothing else, and the prompt says "usage: vim <file>".
> Type the name of a file after the editor's name, with a space between them.

> [!SEE] jsc:programs, jsc:the_systems
