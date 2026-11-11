---
section: library_readings
order: 459
---
# Settings: a program's own settings

A program can keep its own settings, each a name and a line of text, and read them back the next time it runs. Its system keeps them in a file the way the real one did, so you can read that file with the system's own tools. You reach them through Settings, a built-in type of System.Machine. Settings came with Sigma# 3.

Table: Settings members

| Member | What it does |
| --- | --- |
| `Settings.Get(name, fallback)` | The setting with that name, or the fallback when the program has none by that name. |
| `Settings.Set(name, value)` | Keeps the setting, in place of what it held. It gives no when the disk refuses the file. |
| `Settings.Remove(name)` | Takes the setting away. It gives no when the program had none by that name. |
| `Settings.Names()` | The names of every setting the program keeps. |
| `Settings.Path` | Where the settings are kept, written as the system writes paths. |

Each program keeps its settings under the name of the class it was started from, so two programs never share one.

Table: Where each system keeps them

| System | Where |
| --- | --- |
| Frames | In its registry, the file C:\\Frames\\System32\\config\\SOFTWARE, under HKEY_LOCAL_MACHINE\\SOFTWARE and the program's name. |
| MC-DOS | In an INI file of its own at the top of C:, its name cut to eight letters. |
| Linux, the BSDs and UNIX | In ~/.config, a folder of the program's own holding a file called settings. A program that stays up as a service keeps them in /etc instead, in a file of its name ending in .conf. |
| Quince OS X and quinceOS | In ~/Library/Preferences, a property list of the program's name. |
| The classic Quince systems | In the System Folder's Preferences. |

A setting's name is letters, digits, dots, dashes and underscores, up to 64 of them. Its value is one line, up to 1024 characters. The file weighs on the disk like any other.

```sgs Settings.sgs
int runs = Convert.ToInt(Settings.Get("runs", "0")) + 1;
Settings.Set("runs", "" + runs);
Console.PrintLine("Run " + runs + ", kept in " + Settings.Path);
```

> [!DEFINE] Registry
> The one file Frames keeps every program's settings in, each under a key of its own.

## If something goes wrong

> [!PROBLEM] The program stops and says the name is not a setting's name.
> Use letters, digits, dots, dashes and underscores only, up to 64 of them.

> [!PROBLEM] The program stops and says a setting holds one line.
> Take the line breaks out of the value, and keep it to 1024 characters.

> [!PROBLEM] Settings.Set gives no.
> The system disk is full or cannot be written. Free some room on it.

> [!SEE] jsc:file_tree, jsc:computer_info
