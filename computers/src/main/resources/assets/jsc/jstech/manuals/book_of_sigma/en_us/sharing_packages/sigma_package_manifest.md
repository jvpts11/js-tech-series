---
section: sharing_packages
order: 493
---
# What a package says about itself

A package is one program packed up for somebody else. It holds the program's files and a short description of them, called the manifest. You can write a manifest by hand, and the game reads it the same way whoever wrote it.

The manifest is a plain text file named package.cpk. It has one thing on each line: a key, a colon, then the value, such as name: shop. The game reads the keys without regard to capitals. It skips a line with no colon, and any line that starts with a hash sign (#).

A key the game does not know is passed over, not refused. So a manifest written for a later version is still read here, without the newer ideas. When you publish a package, it goes to [the Mirror](jsc:mirror), the package service of the network, and the game checks the manifest first. The ram line is only recorded: the game does not refuse a package on a small computer.

Table: Keys of a manifest

| Key | What it holds |
| --- | --- |
| `name` | The package's name: lower case letters, digits and dashes, at most 32 characters. |
| `version` | Three numbers with dots between them, such as 1.0.0. Each number has 1 to 4 digits. |
| `house` | The software house the Mirror lists beside the package. |
| `entry` | The listing the package runs. It ends in .asm and must also be named on a file line. |
| `icon` | One of the eight icons below. Without this line the icon is gear. |
| `ram` | The memory the package asks for, in megabytes: a whole number of at least 1. |
| `about` | An optional line about the package. The Mirror shows it in place of the name and version. |
| `file` | One line for each file the package carries, with its path inside the package. |

The icon is one of eight pictures the game already has: gear, chart, box, bell, wrench, flask, eye and clock. A package cannot bring its own artwork.

## Starting a manifest with sgpack init

In the folder you work in, type sgpack init and a name. The game makes the name lower case. If you type no name, the package is called program. The command writes package.cpk into that folder.

1. Type sgpack init shop and press [[Enter]].
2. Edit package.cpk and change the lines you need.

Table: What sgpack init writes

| Key | Value written |
| --- | --- |
| `name` | The name you typed, in lower case, or program. |
| `version` | `1.0.0` |
| `house` | The computer's name, or unsigned when it has none. |
| `entry` | The name with .asm added, such as shop.asm. |
| `icon` | `gear` |
| `ram` | `1` |
| `file` | The entry file, as the only file. |

The command refuses to write over a manifest that is already there. It answers "package.cpk is already here; edit it, or delete it to start over".

> [!DEFINE] Package
> One program packed into a single text file, with its manifest and every file it carries.

> [!DEFINE] Manifest
> The package.cpk text file that describes a package: its name, version, entry and files.

> [!DEFINE] Listing
> The compiled .asm file that the compiler writes from a source file.

## If something goes wrong

> [!PROBLEM] The build says package.cpk: name: must be lowercase letters, digits or dashes; got "My Shop".
> Write the name in lower case, with only letters, digits and dashes, such as my-shop.

> [!PROBLEM] The build says package.cpk: version: must look like 1.0.0; got "1.0".
> Write three numbers with dots, such as 1.0.0.

> [!PROBLEM] The build says package.cpk: entry: name the listing the package runs.
> Write the file name of the listing on the entry line, such as shop.asm.

> [!PROBLEM] The build says package.cpk: entry: must be a compiled listing; got "shop.sgs".
> Compile the source with the compiler ([](jsc:sigma_compile)), then name the .asm file it writes.

> [!PROBLEM] The build says package.cpk: files: must include the entry, "shop.asm".
> Add a file line with the same name as the entry.

> [!PROBLEM] The build says package.cpk: icon: must be one of gear, chart, box, bell, wrench, flask, eye, clock; got "star".
> Pick one of the eight icons: gear, chart, box, bell, wrench, flask, eye or clock.

> [!PROBLEM] The build says package.cpk: ram: a package needs at least a megabyte to run in.
> Write 1 or a larger whole number on the ram line.

> [!PROBLEM] The build says package.cpk: files: a package with no files in it is not a package.
> Add a file line for each file the package carries.

> [!SEE] jsc:sigma_build_package, jsc:sigma_sharing_errors
