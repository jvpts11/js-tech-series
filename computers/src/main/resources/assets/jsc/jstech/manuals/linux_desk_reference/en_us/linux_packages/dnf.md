---
section: linux_packages
order: 292
---
# dnf, verb by verb

dnf is the package manager of Fedora. It works the way apt does: you type dnf, a verb, then the name of what it works on. On Ubuntu and Debian the word is not a command, and the shell answers "command not found: dnf".

The verbs are the same as apt's, with one difference. On dnf, update is the upgrade: it brings installed packages up to date. Upgrade does the same job, so either word works.

Table: The dnf verbs

| Command | What it does |
| --- | --- |
| `dnf install <package>` | Installs the program from the Mirror. |
| `dnf remove <package>` | Removes it. purge and erase do the same. |
| `dnf search [term]` | Lists the matching packages. |
| `dnf list` | Lists the installed packages. |
| `dnf update` | Brings the installed packages up to date. |
| `dnf upgrade` | Brings the installed packages up to date, as dnf update does; full-upgrade and dist-upgrade are the same. |
| `dnf check-update` | Lists the installed packages the Mirror has a newer version of, each with the new version. It installs nothing. |

```command
id: jsc:command/linux/dnf
```

An install on Fedora prints its own lines, in this order.

```table
{"caption": "What dnf prints for an install", "heads": ["Line", "What it means"], "rows": [[{"literal": "Last metadata expiration check: <time> ago."}, {"words": "When the package lists were last read."}], [{"literal": "Dependencies resolved."}, {"words": "The packages the program needs are worked out."}], [{"literal": "Installing:  <package>  x86_64  <version>  mirror  <MB> MB"}, {"words": "The program, its version and its size."}], [{"literal": "Downloading Packages:"}, {"words": "The download from the Mirror starts, with a line for its speed and progress."}], [{"literal": "Running transaction"}, {"words": "The program is written to the computer."}], [{"literal": "Installed:"}, {"words": "Followed by the name of the installed program."}], [{"literal": "Complete!"}, {"words": "The install has finished."}]]}
```

```text
{"text": "A remove prints \"Removing:  <package>\" in place of the install lines. Every other refusal is the same as apt's, and begins with \"E: \"."}
```

## If something goes wrong

> [!PROBLEM] command not found: dnf
> This is Ubuntu or Debian, which use apt. Type apt in its place ([](jsc:apt)).

> [!PROBLEM] E: could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service
> The computer is not on a network whose Mainframe runs the Mirror. Connect it, as [](jsc:mirror_and_packages) explains.

> [!PROBLEM] E: unable to locate package <name>
> The name is wrong, or the program is not offered on Linux. Find the right name with dnf search.

> [!PROBLEM] <name> is already the newest version
> Nothing to do. dnf update brings every installed package up to date.

> [!SEE] jsc:apt, jsc:mirror_and_packages
