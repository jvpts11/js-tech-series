---
section: the_editors
order: 277
---
# vi

vi is the editor that comes with FreeBSD and UNIX System V. It is the same engine as [Vim](jsc:vim): the same keys, the same modes and the same colon commands.

The machine chooses which vi you get, and you cannot change it. FreeBSD gets nvi, its own version of vi. UNIX System V gets the original, with its own words.

> [!DEFINE] nvi
> FreeBSD's own version of vi.

The words differ from Vim's. vi keeps no name row of its own: its bottom row shows only the messages below, so there is no file name and no -- INSERT -- text. It shows no colour.

Table: The words of each voice

| When | FreeBSD (nvi) | UNIX System V |
| --- | --- | --- |
| You open a file that exists | `hello.txt: unmodified: line 1` | `"hello.txt" 3 lines, 42 characters` |
| You open a file that does not exist yet | `hello.txt: new file: line 1` | `"hello.txt" [New file]` |
| You write the file with :w | `hello.txt: 3 lines, 42 characters` | `"hello.txt" 3 lines, 42 characters` |

The numbers count the lines and the characters of the text. In this example, the file has 3 lines and 42 characters.

The keys are the same as in Vim, so the table of normal-mode keys in the Vim entry works here too. Undo and the yank of a line say the same words as in Vim, in both voices.

```command
id: jsc:command/freebsd/vi
```

## If something goes wrong

> [!PROBLEM] You type vi and the prompt says "command not found: vi".
> vi comes only with FreeBSD and UNIX System V. On other systems, use [vim](jsc:vim).

> [!PROBLEM] The bottom row says E37: no write since last change (add ! to override).
> Type :w to save, then :q. Or type :q! to leave without saving.

> [!SEE] jsc:vim, jsc:the_systems
