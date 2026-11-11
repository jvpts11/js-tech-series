---
section: gentoo_base
order: 320
---
# make.conf and MAKEOPTS

Portage reads a settings file before it builds any program: /etc/portage/make.conf, in the new system. The base system comes with one. It holds the compiler flags, the language of the build messages, and the setting that decides how many compiles run at once.

Your central processing unit (CPU) has several cores. A core is one of its processing units, and each one can run its own work at the same time.

> [!DEFINE] core
> One of the processing units inside a processor, each able to run its own work at the same time.

Table: Lines of make.conf in the base system

| Line | What it does |
| --- | --- |
| `COMMON_FLAGS="-O2 -pipe"` | The compiler flags for every program. -O2 asks for faster programs, and -pipe makes the compiler itself faster. |
| `LC_MESSAGES=POSIX.utf8` | Writes the build messages in English. |

The base file has no MAKEOPTS line. Without one, every build runs a single compile at a time, which is slow on a machine with several cores. Add the line yourself.

> [!DEFINE] MAKEOPTS
> The setting in make.conf that says how many compiles run at once.

## Adding the line

1. Type nano -w /mnt/gentoo/etc/portage/make.conf and press [[Enter]].
2. Add a line MAKEOPTS="-j4" of your own, and leave the other lines as they are.
3. Save the file and close nano.

You can also add the line in one command, from inside the new system: type echo 'MAKEOPTS="-j4"' >> /etc/portage/make.conf and press [[Enter]].

The number follows -j. The count comes from the MAKEOPTS line that is not commented out, and a line that begins with # sets nothing. More compiles at once than cores buys nothing, so the build uses the smaller of the two numbers.

Table: How many compiles run at once

| The line in make.conf | Cores | Compiles at once |
| --- | --- | --- |
| none | any | 1 |
| `MAKEOPTS="-j4"` | 8 | 4 |
| `MAKEOPTS="-j16"` | 4 | 4 |
| `MAKEOPTS="-j"` | any | 1 |
| `MAKEOPTS="-j0"` | any | 1 |

A -j with no number counts as no setting, and a count below 1 becomes 1 compile. If you write several MAKEOPTS lines, the last one counts.

## If something goes wrong

> [!PROBLEM] Builds are slow, even on a strong machine.
> The base file has no MAKEOPTS line, so every build runs one compile at a time. Add one, as shown above.

> [!PROBLEM] Your MAKEOPTS line does nothing.
> The line starts with #, which makes it a comment. Remove the # at the start of the line.

> [!PROBLEM] nano says "<name> is a directory".
> The name you gave is a folder. Type the full file name: /mnt/gentoo/etc/portage/make.conf.

> [!SEE] jsc:package_tree_and_world, jsc:building_from_source
