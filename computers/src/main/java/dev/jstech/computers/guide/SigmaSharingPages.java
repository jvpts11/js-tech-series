/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the Sharing chapter of the Book of Sigma: packing a program into a package, publishing it on the
 * Mirror, installing the packages other players published, and keeping the revisions of a source file with Knot
 * and KnotHub.
 */
final class SigmaSharingPages {

    private SigmaSharingPages() {
    }

    /** Declares the sections and entries of the packages, Knot and KnotHub, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        sharingPackages(guide, chapter);
        sharingKnot(guide, chapter);
    }

    private static void sharingPackages(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sharing_packages").titled("Packages")
                .in(chapter).register();
        sigmaPackageManifest(guide, section);
        sigmaBuildPackage(guide, section);
        sigmaPublishMirror(guide, section);
        sigmaUnpublish(guide, section);
        sigmaInstallCommunity(guide, section);
        sigmaSharingErrors(guide, section);
    }

    private static void sharingKnot(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("sharing_knot").titled("Knot")
                .in(chapter).register();
        knot(guide, section);
        knothubService(guide, section);
    }

    private static void sigmaPackageManifest(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_package_manifest", section).titled("What a package says about itself")
                .paragraph("A package is one program packed up for somebody else. It holds the program's files and"
                        + " a short description of them, called the manifest. You can write a manifest by hand, and"
                        + " the game reads it the same way whoever wrote it.")
                .paragraph("The manifest is a plain text file named package.cpk. It has one thing on each line: a"
                        + " key, a colon, then the value, such as name: shop. The game reads the keys without regard"
                        + " to capitals. It skips a line with no colon, and any line that starts with a hash sign"
                        + " (#).")
                .paragraph("A key the game does not know is passed over, not refused. So a manifest written for a"
                        + " later version is still read here, without the newer ideas. When you publish a package,"
                        + " it goes to [the Mirror](jsc:mirror), the package service of the network, and the game"
                        + " checks the manifest first.")
                .table("Keys of a manifest", "Key", "What it holds")
                .row(GuideCell.code("name"), GuideCell.words("The package's name: lower case letters, digits and"
                        + " dashes, at most 32 characters."))
                .row(GuideCell.code("version"), GuideCell.words("Three numbers with dots between them, such as"
                        + " 1.0.0. Each number has 1 to 4 digits."))
                .row(GuideCell.code("house"), GuideCell.words("The software house the Mirror lists beside the"
                        + " package."))
                .row(GuideCell.code("entry"), GuideCell.words("The listing the package runs. It ends in .asm and"
                        + " must also be named on a file line."))
                .row(GuideCell.code("icon"), GuideCell.words("One of the eight icons below. Without this line the"
                        + " icon is gear."))
                .row(GuideCell.code("ram"), GuideCell.words("The memory the package asks for, in megabytes: a"
                        + " whole number of at least 1. The game records the value; it does not refuse a package"
                        + " on a small computer."))
                .row(GuideCell.code("about"), GuideCell.words("An optional line about the package. The Mirror shows"
                        + " it in place of the name and version."))
                .row(GuideCell.code("file"), GuideCell.words("One line for each file the package carries, with its"
                        + " path inside the package."))
                .paragraph("The icon is one of eight pictures the game already has: gear, chart, box, bell, wrench,"
                        + " flask, eye and clock. A package cannot bring its own artwork.")
                .subheading("Starting a manifest with sgpack init")
                .paragraph("In the folder you work in, type sgpack init and a name. The game makes the name lower"
                        + " case. If you type no name, the package is called program. The command writes package.cpk"
                        + " into that folder.")
                .steps("Type sgpack init shop and press [[Enter]].",
                        "Edit package.cpk and change the lines you need.")
                .table("What sgpack init writes", "Key", "Value written")
                .row(GuideCell.code("name"), GuideCell.words("The name you typed, in lower case, or program."))
                .row(GuideCell.code("version"), GuideCell.code("1.0.0"))
                .row(GuideCell.code("house"), GuideCell.words("The computer's name, or unsigned when it has none."))
                .row(GuideCell.code("entry"), GuideCell.words("The name with .asm added, such as shop.asm."))
                .row(GuideCell.code("icon"), GuideCell.code("gear"))
                .row(GuideCell.code("ram"), GuideCell.code("1"))
                .row(GuideCell.code("file"), GuideCell.words("The entry file, as the only file."))
                .paragraph("The command refuses to write over a manifest that is already there. It answers"
                        + " \"package.cpk is already here; edit it, or delete it to start over\".")
                .define("Package", "One program packed into a single text file, with its manifest and every file"
                        + " it carries.")
                .define("Manifest", "The package.cpk text file that describes a package: its name, version, entry"
                        + " and files.")
                .define("Listing", "The compiled .asm file that the compiler writes from a source file.")
                .ifSomethingGoesWrong(
                        "The build says package.cpk: name: must be lowercase letters, digits or dashes; got"
                                + " \"My Shop\".",
                        "Write the name in lower case, with only letters, digits and dashes, such as my-shop.",
                        "The build says package.cpk: version: must look like 1.0.0; got \"1.0\".",
                        "Write three numbers with dots, such as 1.0.0.",
                        "The build says package.cpk: entry: name the listing the package runs.",
                        "Write the file name of the listing on the entry line, such as shop.asm.",
                        "The build says package.cpk: entry: must be a compiled listing; got \"shop.sgs\".",
                        "Compile the source with the compiler ([](jsc:sigma_compile)), then name the .asm file"
                                + " it writes.",
                        "The build says package.cpk: files: must include the entry, \"shop.asm\".",
                        "Add a file line with the same name as the entry.",
                        "The build says package.cpk: icon: must be one of gear, chart, box, bell, wrench, flask,"
                                + " eye, clock; got \"star\".",
                        "Pick one of the eight icons from the list above.",
                        "The build says package.cpk: ram: a package needs at least a megabyte to run in.",
                        "Write 1 or a larger whole number on the ram line.",
                        "The build says package.cpk: files: a package with no files in it is not a package.",
                        "Add a file line for each file the package carries.")
                .seeAlso("jsc:sigma_build_package", "jsc:sigma_sharing_errors")
                .register();
    }

    private static void sigmaBuildPackage(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_build_package", section).titled("Building a package")
                .paragraph("Building turns the manifest and its files into one package file. Nothing is compiled in"
                        + " this step: the listing the manifest names must already exist, made by the compiler"
                        + " ([](jsc:sigma_compile)). Build only reads your files and joins them.")
                .paragraph("A built package is one text file, named after the package, such as mypkg-1.0.0.cpk. It"
                        + " starts with the line \".pkg 1\", then the manifest lines, then each file in a block that"
                        + " starts with three dashes and the file's name. Because it is plain text, you can read"
                        + " every line before you install it.")
                .paragraph("Build marks any line of a file that starts with three dashes or with a backslash, so the"
                        + " reader does not mistake it for the start of another file. The reader takes the mark off"
                        + " again, so your file comes back unchanged.")
                .subheading("Building the package")
                .steps("Make sure package.cpk and every file it names are in the folder.",
                        "Type sgpack build and press [[Enter]].",
                        "Read the line the build prints.")
                .paragraph("A successful build prints a line such as built mypkg-1.0.0.cpk (2 files, 512 bytes): the"
                        + " file name, how many files it holds and the size of the text in characters. The built"
                        + " file lands"
                        + " in the same folder, next to the manifest, so you can publish it at once.")
                .subheading("File names inside a package")
                .paragraph("Each file in a package keeps a name that stays inside the package's own folder: plain"
                        + " names separated by slashes, with no . or .. parts, no backslash and no colon. The game"
                        + " refuses any other name.")
                .ifSomethingGoesWrong(
                        "The build says no package.cpk here; run 'sgpack init' first.",
                        "Run sgpack init in this folder, or go to the folder that holds your manifest.",
                        "The build says sgpack: a file is named in the manifest but not here.",
                        "Copy that file into the folder, or remove its file line from package.cpk.",
                        "The build says package.cpk: files: a file name is not a file name a package may carry;"
                                + " it must stay inside the package's own folder.",
                        "Rename the file to a plain name, such as tools/run.asm, and change its file line to"
                                + " match.")
                .seeAlso("jsc:sigma_publish_mirror", "jsc:sigma_sharing_errors")
                .register();
    }

    private static void sigmaPublishMirror(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_publish_mirror", section).titled("Publishing a package to the Mirror")
                .paragraph("Publishing puts a built package on [the Mirror](jsc:mirror), the package service that lives"
                        + " on the network's Mainframe ([](jsc:mainframes)). Once it is there, any computer on the"
                        + " network can install it by name.")
                .paragraph("The Mirror serves packages only when it is installed on the Mainframe and the Mainframe is"
                        + " running. A Mainframe that is switched off keeps the Mirror installed, but serves nothing"
                        + " until it is on again.")
                .table("The sgpack verbs", "Verb", "What it does")
                .row(GuideCell.code("init [name]"), GuideCell.words("Writes a package.cpk to fill in."))
                .row(GuideCell.code("build"), GuideCell.words("Makes the package the manifest describes."))
                .row(GuideCell.code("publish [file]"), GuideCell.words("Puts it on the network's Mirror."))
                .row(GuideCell.code("unpublish <name>"), GuideCell.words("Takes it back off the Mirror."))
                .subheading("Publishing a built package")
                .steps("Build the package, so that its .cpk file is in the folder.",
                        "Type sgpack publish and press [[Enter]]. The command publishes the package the manifest"
                                + " describes.",
                        "To publish another file, type sgpack publish and its file name, such as"
                                + " mypkg-1.0.0.cpk.")
                .paragraph("A successful publish answers published mypkg 1.0.0 on the Mirror, with the name and the"
                        + " version. Publishing a name that is already there answers replaced mypkg 1.0.0 on the"
                        + " Mirror, and the new copy takes the old one's place in a single step.")
                .paragraph("The Mirror holds up to 64 packages per network. A new name is refused at that count."
                        + " Replacing a name you already published does not count against the limit.")
                .paragraph("The Mirror keeps the whole text of each package, and it does not record who published"
                        + " it. A successful publish also earns the achievement Ship It ([](jsc:achievements_sigma)),"
                        + " which asks you to pack a program with sgpack and publish it to the Mirror.")
                .paragraph("Publish checks the package before it sends it. It names only the first problem it finds,"
                        + " so fix that line and publish again.")
                .ifSomethingGoesWrong(
                        "Publish says could not resolve mirror:// - connect this computer to a network whose"
                                + " Mainframe runs the Mirror service.",
                        "Connect this computer to a network whose Mainframe runs the Mirror, and switch that"
                                + " Mainframe on ([](jsc:mirror)).",
                        "Publish says mypkg-1.0.0.cpk: this is not a package (build one with 'sgpack build').",
                        "Run sgpack build first, then publish the .cpk file it makes.",
                        "A new name is refused: the Mirror is full (64 packages).",
                        "Take a package you no longer need off the Mirror ([](jsc:sigma_unpublish)).",
                        "Publish says name the package to publish, or run this beside a package.cpk.",
                        "Name the file, such as sgpack publish mypkg-1.0.0.cpk, or run the command in the folder"
                                + " that holds the manifest.")
                .seeAlso("jsc:sigma_unpublish", "jsc:sigma_sharing_errors", "jsc:mainframe_services")
                .register();
    }

    private static void sigmaUnpublish(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_unpublish", section).titled("Taking a package off the Mirror")
                .paragraph("Unpublishing removes a package from the Mirror, so no computer can install it after that."
                        + " You name the package, not the file. The command is sgpack unpublish, followed by the"
                        + " package's name.")
                .steps("Type sgpack unpublish mypkg and press [[Enter]].",
                        "Read the answer: took mypkg off the Mirror.")
                .paragraph("Any player on the network can take any package off the Mirror. The Mirror keeps no record"
                        + " of who published a package, so nothing stops one player from removing another's package."
                        + " Check with the other players before you take away a package they use.")
                .ifSomethingGoesWrong(
                        "The command says usage: sgpack unpublish <name>.",
                        "Type the package's name after unpublish.",
                        "The command says the Mirror is not serving the name you typed.",
                        "The name is not on the Mirror. Check its spelling, then try again.",
                        "The command says could not resolve mirror://.",
                        "No Mirror serves this network. Connect to a network whose Mainframe runs the Mirror"
                                + " ([](jsc:mirror)).")
                .seeAlso("jsc:sigma_publish_mirror")
                .register();
    }

    private static void sigmaInstallCommunity(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_install_community", section).titled("Installing a package other players published")
                .paragraph("Players can publish packages for anyone on the network. You install one like any other"
                        + " program: with the install command of your system's package manager and the package's"
                        + " name, such as apt install mypkg on a Debian or Ubuntu system ([](jsc:software_commands)).")
                .paragraph("Installing a player's package needs the Sigma runtime on your computer"
                        + " ([](jsc:sigma_installing)). Without it, the install stops and says to install sigma"
                        + " first.")
                .paragraph("The game unpacks each package into a folder of its own: PROGRAMS, then the package's"
                        + " name. The folder holds only that package's files, so two packages cannot overwrite each"
                        + " other. The package then shows on the desktop with its icon, and opening it runs its entry"
                        + " listing.")
                .steps("Type the install command of your package manager, with the package's name.",
                        "Open the program from the desktop.")
                .paragraph("To remove a package, type the remove command of your package manager, with its name, such"
                        + " as apt remove mypkg. The game deletes exactly the files the install wrote, then the folder,"
                        + " and answers removed mypkg.")
                .ifSomethingGoesWrong(
                        "The install says this system installs programs from install media, not a package manager.",
                        "Install the program from the install media of the system instead ([](jsc:installing)).",
                        "The install says the Mirror's copy of this package is not readable.",
                        "Ask the player who published it to publish it again from a built file.",
                        "The install says mypkg is a Σ# program; install sigma first.",
                        "Install the Sigma runtime on this computer ([](jsc:sigma_installing)), then install again.",
                        "The install says no system disk to install onto.",
                        "Put a system disk in the computer, then install again.",
                        "The install says this system has no folders to install into.",
                        "Check the system disk, then install again.")
                .seeAlso("jsc:software_commands", "jsc:sigma_installing", "jsc:sigma_sharing_errors")
                .register();
    }

    private static void sigmaSharingErrors(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_sharing_errors", section).titled("Errors when packing and publishing")
                .paragraph("Each error starts with a prefix that tells you where it came from. An sgpack error can"
                        + " start with sgpack:. A line that starts with package.cpk: points at a line of your manifest."
                        + " A line that starts with a package file's name, such as mypkg-1.0.0.cpk:, comes from"
                        + " publish and points at that file.")
                .paragraph("The table gives each text as the game shows it, with its cause and where its fix is"
                        + " explained.")
                .table("Errors of the sgpack command", "The text you see", "What it means and the fix")
                .row(GuideCell.code("usage: sgpack init [name] | build | publish [file] | unpublish <name>"),
                        GuideCell.words("You typed no verb. Pick one of the four verbs."))
                .row(GuideCell.code("package.cpk is already here; edit it, or delete it to start over"),
                        GuideCell.words("init found a manifest. Edit it, or delete it to start over."))
                .row(GuideCell.code("no package.cpk here; run 'sgpack init' first"),
                        GuideCell.words("build found no manifest. Run sgpack init in this folder"
                                + " ([](jsc:sigma_package_manifest))."))
                .row(GuideCell.code("sgpack: <file> is named in the manifest but not here"),
                        GuideCell.words("A listed file is missing. Copy it into the folder, or remove its file"
                                + " line."))
                .row(GuideCell.code("package.cpk: <problem>"),
                        GuideCell.words("The manifest or a file fails a check. Fix the line the text names"
                                + " ([](jsc:sigma_package_manifest))."))
                .row(GuideCell.code("name the package to publish, or run this beside a package.cpk"),
                        GuideCell.words("publish has no file and no manifest. Name the file, or run the command in"
                                + " the manifest's folder."))
                .row(GuideCell.code("could not resolve mirror:// - connect this computer to a network whose"
                        + " Mainframe runs the Mirror service"),
                        GuideCell.words("No Mirror serves this network. Connect to a network whose Mainframe runs"
                                + " the Mirror ([](jsc:mirror))."))
                .row(GuideCell.code("mypkg-1.0.0.cpk: this is not a package (build one with 'sgpack build')"),
                        GuideCell.words("The file is not a built package. Run sgpack build, then publish the .cpk"
                                + " file."))
                .row(GuideCell.code("the Mirror is full (64 packages)"),
                        GuideCell.words("A new name is refused. Take a package off the Mirror"
                                + " ([](jsc:sigma_unpublish))."))
                .row(GuideCell.code("usage: sgpack unpublish <name>"),
                        GuideCell.words("unpublish has no name. Type the package's name after it."))
                .row(GuideCell.code("the Mirror is not serving <name>"),
                        GuideCell.words("The name is not on the Mirror. Check its spelling."))
                .row(GuideCell.code("could not resolve mirror://"),
                        GuideCell.words("unpublish finds no Mirror serving this network. See"
                                + " [the Mirror](jsc:mirror)."))
                .seeAlso("jsc:sigma_package_manifest", "jsc:sigma_build_package", "jsc:sigma_publish_mirror")
                .register();
    }

    private static void knot(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("knot", section).titled("Knot")
                .paragraph("Knot is a desktop program that keeps the source of a file you are still writing. Each"
                        + " time you push the file, Knot saves a copy as a revision in the network's repository. A"
                        + " revision holds one saved state of the file, with its author, its message and the time it"
                        + " was saved.")
                .paragraph("Knot then shows you who changed what, line by line. It works only where a KnotHub is"
                        + " running on the network ([](jsc:knothub_service)). Knot is not a package manager: a package"
                        + " is a finished program handed to others, and Knot keeps the source before that.")
                .subheading("The window")
                .table("Part of the window", "Part", "What it does")
                .row(GuideCell.words("Push, Pull and Refresh"),
                        GuideCell.words("Push saves the file as a new revision. Pull writes the picked revision back"
                                + " to disk. Refresh asks the KnotHub for the state again."))
                .row(GuideCell.words("File"),
                        GuideCell.words("The full path of the file a Push reads. A Pull does not use it."))
                .row(GuideCell.words("Message box"),
                        GuideCell.words("The note a push carries, up to 128 characters."))
                .row(GuideCell.words("History"),
                        GuideCell.words("The newest revisions, each with its number, author and message. It shows 4"
                                + " rows and scrolls."))
                .row(GuideCell.words("Comparison"),
                        GuideCell.words("The changes of the picked revision against the one before it: a plus sign"
                                + " for an added line, a minus sign for a removed line, and plain lines that did not"
                                + " change."))
                .row(GuideCell.words("Status bar"),
                        GuideCell.words("On the left, the last answer, or the KnotHub's host name. On the right, the"
                                + " number of revisions and the size of the repository."))
                .paragraph("Click a row of the history to pick that revision. The File field then shows the"
                        + " revision's file, and the comparison shows its changes. The newest revision is picked when"
                        + " the window opens.")
                .subheading("Pushing a file")
                .steps("Type the file's full path in the File field.",
                        "Type a message in the message box, if you want one.",
                        "Press Push.",
                        "Read the answer on the status bar, such as Pushed plant.sgs.")
                .paragraph("Knot reads the file from the system disk of the computer you are at, not from the text in"
                        + " the window. A push with an empty message is saved as \"no message\". A push of the same"
                        + " text as the newest revision of that file changes nothing, so Knot refuses it.")
                .subheading("Pulling a revision")
                .steps("Click the revision in the history.",
                        "Press Pull.",
                        "Read the answer, such as Wrote r3 to plant.sgs.")
                .paragraph("A Pull writes the revision back to the file it belongs to, on the system disk. It does not"
                        + " write to the path in the File field.")
                .table("Answers on the status bar", "Answer", "When you see it")
                .row(GuideCell.code("No Computer"), GuideCell.words("The window is not on a computer."))
                .row(GuideCell.code("No System Disk"), GuideCell.words("The computer has no system disk."))
                .row(GuideCell.code("No <file> on this machine"),
                        GuideCell.words("Push cannot find the file on the system disk."))
                .row(GuideCell.code("Nothing changed since the last revision"),
                        GuideCell.words("The file has the same text as its newest revision, or it is a new file and"
                                + " the repository already holds 64 files."))
                .row(GuideCell.code("Pushed <file>"), GuideCell.words("Push saved a revision."))
                .row(GuideCell.code("No such revision"),
                        GuideCell.words("The picked revision is gone from the history, or the KnotHub was reset."))
                .row(GuideCell.code("Not enough free space"),
                        GuideCell.words("Pull could not write: the disk is full."))
                .row(GuideCell.code("Could not write <file>"), GuideCell.words("Pull failed for another reason."))
                .row(GuideCell.code("Wrote r<number> to <file>"), GuideCell.words("Pull wrote the revision."))
                .subheading("Revisions and the comparison")
                .paragraph("A revision's number counts up across the whole repository, not file by file, and a number"
                        + " is never reused while the repository lives. The first revision of a file is compared with"
                        + " empty text, so every line of it shows as added.")
                .paragraph("The comparison runs line by line, in reading order. A line that both versions share is"
                        + " shown as it is. A line only in the older version is removed, and a line only in the newer"
                        + " version is added. It does not search for the smallest set of changes, so it can show more"
                        + " lines than that.")
                .table("Limits", "Limit", "Value")
                .row(GuideCell.words("Revisions kept per file"), GuideCell.words("64 revisions"))
                .row(GuideCell.words("Files in one repository"), GuideCell.words("64 files"))
                .row(GuideCell.words("Lines of comparison shown"), GuideCell.words("128 lines"))
                .row(GuideCell.words("Path in the File field"), GuideCell.words("160 characters"))
                .row(GuideCell.words("Message"), GuideCell.words("128 characters"))
                .warning("When a file already has 64 revisions, its next push drops the oldest one, and nothing"
                        + " tells you so.")
                .define("Knot", "The desktop program that saves the revisions of a file and shows what changed"
                        + " between them.")
                .define("Repository", "The store of every file and every revision that one KnotHub keeps.")
                .define("Revision", "One saved state of one file in the repository, with its author, its message and"
                        + " the time it was saved.")
                .define("Diff", "The list of lines that changed between a revision and the revision before it.")
                .ifSomethingGoesWrong(
                        "Push says No System Disk.",
                        "Put a system disk in the computer. Knot reads files from it.",
                        "Push says No <file> on this machine.",
                        "Check the path in the File field. Knot reads the file from the system disk, by its full path.",
                        "Push says Nothing changed since the last revision.",
                        "The file has the same text as its newest revision, so nothing new was saved. Change the file"
                                + " and push again. A new file is also refused once the repository holds 64 files, and"
                                + " the window gives the same text for that case.",
                        "Pull says Not enough free space.",
                        "The system disk is full. Free some space on it, then pull again.",
                        "Pull says No such revision.",
                        "The revision was dropped, or the KnotHub was reset. Pick another revision from the history.",
                        "The window says No KnotHub on this network.",
                        "No Server with KnotHub is running. See the KnotHub entry ([](jsc:knothub_service)).")
                .seeAlso("jsc:knothub_service", "jsc:sigma_projects")
                .register();
    }

    private static void knothubService(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("knothub_service", section).titled("KnotHub")
                .paragraph("KnotHub is the service that keeps the repository: every file you pushed with Knot and every"
                        + " revision of each one. It is installed on a Server, one of the computers in a Server Rack"
                        + " ([](jsc:server_racks)). It does not run on the Mainframe or on a desktop.")
                .paragraph("Each Server keeps its own repository, and the source is saved with that Server item. If you"
                        + " take the Server out of the rack, its history goes with it. Knot reads the repository of the"
                        + " first running KnotHub it finds on the network, in the order the network lists its Servers.")
                .paragraph("A KnotHub on a Server that is switched off does not answer. The window then shows no"
                        + " revisions, and its status bar reads no service.")
                .warning("Uninstalling KnotHub deletes all its source: every file and every revision. Its numbering"
                        + " starts again at revision 1.")
                .ifSomethingGoesWrong(
                        "The window says No KnotHub on this network, and it shows no revisions.",
                        "Put a Server with KnotHub installed in a Server Rack, and switch the Server on"
                                + " ([](jsc:server_racks)).",
                        "The status bar reads no service.",
                        "The same fix applies: a running KnotHub must answer on the network.",
                        "Push or Pull says No System Disk.",
                        "Put a system disk in the computer ([](jsc:knot)).",
                        "Pull says No such revision.",
                        "The revision was dropped, or the KnotHub was reset. Pick another revision from the history.")
                .seeAlso("jsc:knot", "jsc:server_racks")
                .register();
    }
}
