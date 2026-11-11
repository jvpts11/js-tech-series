---
section: help_at_the_prompt
order: 283
icon: jsc:guide_to_operations
---
# info and its keys

info is the Linux tool for the manuals. Where man shows one page, info shows the manuals as linked screens. Each chapter, section and entry of a manual is a node, and the commands of this computer sit beside them.

info takes the whole terminal, as the help screens do. Press q to leave it and get your prompt back.

## Using info

1. Type info and press [[Enter]] to open the directory, the list of every manual and command.
2. Type info, a space and a name, such as info graphics-cards, to open that entry directly.
3. Press [[Tab]] to pick a link, then [[Enter]] to follow it.
4. Press q to leave info.

The directory is the first screen. It lists every manual, with one menu line for the commands of this computer. Press d at any time to return to it.

Table: Keys in info

| Key | What it does |
| --- | --- |
| `q` | Quits info. |
| `n, p` | Go to the next node, or to the previous one. |
| `u` | Goes to the node above this one. |
| `t` | Goes to the top node of the manual. |
| `d` | Goes to the directory of every manual. |
| `l` | Goes back to the node you read last. |
| `h` | Shows the keys. |
| `m` | Asks for a menu item by its name. Press Enter after it. |
| `s or /` | Asks for a search text, which Enter runs; Escape or Ctrl+G cancels. |
| `Space, Page Down` | Move one screen down. |
| `Backspace, Page Up` | Move one screen up. |
| `Up, Down` | Move one line. |
| `Tab, Shift+Tab` | Pick the next or the previous link. |
| `Enter` | Follows the picked link. |

```command
id: jsc:command/linux/info
```

> [!DEFINE] node
> One piece of an info manual, a chapter, a section or an entry, read as one screen.

## If something goes wrong

> [!PROBLEM] The reply is "command not found: info" on the other systems, and on FreeBSD "info: not found. Try: apropos <word>, or man intro".
> info is on Linux only. Use man <command> on UNIX System V and FreeBSD, or help <command> on MC-DOS, MC-NET and Frames.

> [!PROBLEM] The reply is "No menu item" and a name, after you press m.
> The name must match a menu item of this node. Press d or t to go elsewhere, or pick a link with Tab.

> [!PROBLEM] The reply is "No 'Next' pointer for this node.", or the same for Prev or Up.
> This node has no neighbour in that direction. Use n, p, u or l to go somewhere else.

> [!PROBLEM] The reply is "No previous nodes."
> You have not read another node yet, so there is nothing to go back to. Read a node with n first.

> [!PROBLEM] The reply is "Search failed:" followed by the text you searched for.
> The text is not in this manual. Try a shorter word.

> [!SEE] jsc:man_pages, jsc:help_command
