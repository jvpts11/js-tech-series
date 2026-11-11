---
section: sigma_start
order: 411
---
# Projects and versions

A project keeps the files of one program together, with the settings that build it. When you make a project, its settings live in the project file, a text file in the project folder. The file of Sigma# is named NAME.sgsproj, and the file of Sigma is named NAME.sgproj.

Each line of the file holds one setting, as a key, a colon and a value. A line written in another way is skipped.

Table: What a project file holds

| Key | What it holds |
| --- | --- |
| `name` | The project's name. |
| `kind` | One of console, script, library or empty. |
| `language` | jsc:sigma for Sigma, or jsc:sigma_sharp for Sigma#. |
| `source` | One line for each source file, relative to the project folder. |
| `reference` | One line for each library project it uses. |
| `entry` | The listing it builds, such as build/NAME.asm. Empty for a library. |
| `platform` | The instruction set it builds for, such as jsc:x86. |
| `langversion` | The language version it is held to. Written only when set. Without it, the project follows the installed compiler. |

The kind tells the compiler what to build. A console program runs and returns. A script stays up. A library holds shared classes and has no start of its own. An empty project has nothing in it yet.

A solution keeps its projects together, one folder for each project. Its file lists each project by its path, and it names the startup project, the one that Start runs. When the startup project is empty, Start uses the first project that can run.

A project builds for one instruction set, which you choose in the popup named Platform target.

> [!DEFINE] langversion
> The language version a project is held to. Version 1 is the first language, and version 2 is the current one.

> [!DEFINE] held
> The version a build really uses: the lower of the project's langversion and the version of the installed compiler.

Sigma and Sigma# share their version numbers. Sigma 2 is the subset of Sigma# 2, so a project has a single number for both.

## Setting the language version

1. Open the Properties popup of the project in Virtual Studio.
2. Find the row Language version.
3. Press the Default button to follow the installed compiler, or press a numbered button to hold the project to that version.

Under the buttons, Virtual Studio explains the choice. "Default follows the installed compiler (sgsc 2.0)." is the first hint. "A lower version refuses what came after it:" is the second. After you choose a number, the popup says "Written to the project as langversion" with that number.

Choosing Default removes the langversion line from the project file. Your build then uses the installed compiler's version. With a number, your build uses the lower of that number and the installed compiler's version.

On the command line the project file is not read. The option --lang sets the version for one compile ([](jsc:sigma_compile)). The sigma run command on a source file uses the newest version, not the project's.

Version 1 is the first release. Version 2 added the types for the network, the machines and the windows, such as Bus, CraftInterface, Table and Chart. It added members to older types, such as Console.Read and File.Move. It also lets Main return an int and take the arguments, and the smaller Sigma gained Random.

## If something goes wrong

> [!PROBLEM] "'puts' needs Σ# 2; this project is Σ# 1" appears.
> Set the project's Language version to Default, or to version 2.

> [!PROBLEM] "this compiler knows Σ# 1 at most; Σ# 2 needs a newer one" appears.
> Update the compiler package, or use a lower version after --lang.

> [!PROBLEM] "'3' is not a version of Σ#; there are 1 to 2" appears.
> Use 1 or 2 after --lang.

> [!SEE] jsc:sigma_sharing, jsc:sigma_compile
