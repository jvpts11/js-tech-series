---
section: sharing_packages
order: 495
---
# Publishing a package to the Mirror

Publishing puts a built package on [the Mirror](jsc:mirror), the package service that lives on the network's Mainframe ([](jsc:mainframes)). Once it is there, any computer on the network can install it by name.

The Mirror serves packages only when it is installed on the Mainframe and the Mainframe is running. A Mainframe that is switched off keeps the Mirror installed, but serves nothing until it is on again.

Table: The sgpack verbs

| Verb | What it does |
| --- | --- |
| `init [name]` | Writes a package.cpk to fill in. |
| `build` | Makes the package the manifest describes. |
| `publish [file]` | Puts it on the network's Mirror. |
| `unpublish <name>` | Takes it back off the Mirror. |

## Publishing a built package

1. Build the package, so that its .cpk file is in the folder.
2. Type sgpack publish and press [[Enter]]. The command publishes the package the manifest describes.
3. To publish another file, type sgpack publish and its file name, such as mypkg-1.0.0.cpk.

A successful publish answers published mypkg 1.0.0 on the Mirror, with the name and the version. Publishing a name that is already there answers replaced mypkg 1.0.0 on the Mirror, and the new copy takes the old one's place in a single step.

The Mirror holds up to 64 packages per network. A new name is refused at that count. Replacing a name you already published does not count against the limit.

The Mirror keeps the whole text of each package, and it does not record who published it. A successful publish also earns the achievement Ship It ([](jsc:achievements_sigma)), which asks you to pack a program with sgpack and publish it to the Mirror.

Publish checks the package before it sends it. It names only the first problem it finds, so fix that line and publish again.

## If something goes wrong

> [!PROBLEM] Publish says could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service.
> Connect this computer to a network whose Mainframe runs the Mirror, and switch that Mainframe on ([](jsc:mirror)).

> [!PROBLEM] Publish says mypkg-1.0.0.cpk: this is not a package (build one with 'sgpack build').
> Run sgpack build first, then publish the .cpk file it makes.

> [!PROBLEM] A new name is refused: the Mirror is full (64 packages).
> Take a package you no longer need off the Mirror ([](jsc:sigma_unpublish)).

> [!PROBLEM] Publish says name the package to publish, or run this beside a package.cpk.
> Name the file, such as sgpack publish mypkg-1.0.0.cpk, or run the command in the folder that holds the manifest.

> [!SEE] jsc:sigma_unpublish, jsc:sigma_sharing_errors, jsc:mainframe_services
