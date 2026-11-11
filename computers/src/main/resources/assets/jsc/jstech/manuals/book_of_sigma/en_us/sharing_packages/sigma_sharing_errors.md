---
section: sharing_packages
order: 498
---
# Errors when packing and publishing

Each error starts with a prefix that tells you where it came from. An sgpack error can start with sgpack:. A line that starts with package.cpk: points at a line of your manifest. A line that starts with a package file's name, such as mypkg-1.0.0.cpk:, comes from publish and points at that file.

The table gives each text as the game shows it, with what it means and how to fix it.

Table: Errors of the sgpack command

| The text you see | What it means and the fix |
| --- | --- |
| `usage: sgpack init [name] \| build \| publish [file] \| unpublish <name>` | You typed no verb. Pick one of the four verbs. |
| `package.cpk is already here; edit it, or delete it to start over` | init found a manifest. Edit it, or delete it to start over. |
| `no package.cpk here; run 'sgpack init' first` | build found no manifest. Run sgpack init in this folder ([](jsc:sigma_package_manifest)). |
| `sgpack: <file> is named in the manifest but not here` | A listed file is missing. Copy it into the folder, or remove its file line. |
| `package.cpk: <problem>` | The manifest or a file fails a check. Fix the line the text names ([](jsc:sigma_package_manifest)). |
| `name the package to publish, or run this beside a package.cpk` | publish has no file and no manifest. Name the file, or run the command in the manifest's folder. |
| `could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service` | No Mirror serves this network. Connect to a network whose Mainframe runs the Mirror ([](jsc:mirror)). |
| `mypkg-1.0.0.cpk: this is not a package (build one with 'sgpack build')` | The file is not a built package. Run sgpack build, then publish the .cpk file. |
| `the Mirror is full (64 packages)` | The Mirror has no room for a new name. Take a package off the Mirror ([](jsc:sigma_unpublish)). |
| `usage: sgpack unpublish <name>` | unpublish has no name. Type the package's name after it. |
| `the Mirror is not serving <name>` | The name is not on the Mirror. Check its spelling. |
| `could not resolve mirror://` | unpublish finds no Mirror serving this network. See [the Mirror](jsc:mirror). |

> [!SEE] jsc:sigma_package_manifest, jsc:sigma_build_package, jsc:sigma_publish_mirror
