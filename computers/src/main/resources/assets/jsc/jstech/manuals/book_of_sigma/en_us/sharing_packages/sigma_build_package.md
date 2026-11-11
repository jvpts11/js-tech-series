---
section: sharing_packages
order: 494
---
# Building a package

Building turns the manifest and its files into one package file. Nothing is compiled in this step: the listing the manifest names must already exist, made by the compiler ([](jsc:sigma_compile)). Build only reads your files and joins them.

A built package is one text file, named after the package, such as mypkg-1.0.0.cpk. It starts with the line ".pkg 1", then the manifest lines, then each file in a block that starts with three dashes and the file's name. Because it is plain text, you can read every line before you install it.

Build marks any line of a file that starts with three dashes or with a backslash, so the reader does not mistake it for the start of another file. The reader takes the mark off again, so your file comes back unchanged.

## Building the package

1. Make sure package.cpk and every file it names are in the folder.
2. Type sgpack build and press [[Enter]].
3. Read the line the build prints.

A successful build prints a line such as built mypkg-1.0.0.cpk (2 files, 512 bytes): the file name, how many files it holds and the size of the text in characters. The built file lands in the same folder, next to the manifest, so you can publish it at once.

## File names inside a package

Each file in a package keeps a name that stays inside the package's own folder: plain names separated by slashes, with no . or .. parts, no backslash and no colon. The game refuses any other name.

## If something goes wrong

> [!PROBLEM] The build says no package.cpk here; run 'sgpack init' first.
> Run sgpack init in this folder, or go to the folder that holds your manifest.

> [!PROBLEM] The build says sgpack: a file is named in the manifest but not here.
> Copy that file into the folder, or remove its file line from package.cpk.

> [!PROBLEM] The build says package.cpk: files: and a name, then "is not a file name a package may carry; it must stay inside the package's own folder".
> Rename the file to a plain name, such as tools/run.asm, and change its file line to match.

> [!SEE] jsc:sigma_publish_mirror, jsc:sigma_sharing_errors
