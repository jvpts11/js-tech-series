---
section: network_operation
order: 105
icon: jsc:mainframe
---
# The Mirror

The Mirror is a shelf of packages for the network. A Mainframe that runs the Mirror holds up to 64 packages, and every computer on its network can install from that shelf.

The Mirror is a Mainframe service. It needs 64 megabytes (MB) of disk and 32 MB of random-access memory (RAM) on the Mainframe, and it serves packages only while the Mainframe is powered.

1. Turn the Mainframe on.
2. Type mirror install on the Mainframe's terminal.
3. Connect the other computers to the network of this Mainframe.
4. Install a package from the Mirror with the package manager of your system.

Table: The Mirror's commands

| Command | What it does |
| --- | --- |
| `mirror install` | Installs the Mirror on this Mainframe. |
| `mirror status` | Shows whether the Mirror is not installed, serving, or installed with the Mainframe off. |

You publish a package of your own with the sgpack command. Build it first with sgpack build, then publish it with sgpack publish. Publishing a name that is already on the shelf replaces that package, which is how you release a fix. Take a package back with sgpack unpublish.

> [!DEFINE] The Mirror
> The shelf of packages that a Mainframe serves to its network.

## If something goes wrong

> [!PROBLEM] could not resolve mirror://
> This computer is not on the network of a Mainframe that runs the Mirror. Connect it to that network, or install the Mirror on the Mainframe with mirror install.

> [!PROBLEM] the network has no running Mainframe to host the Mirror
> Turn the Mainframe on, and try again.

> [!PROBLEM] the Mirror is full (64 packages)
> Take one package off the shelf with sgpack unpublish.

> [!PROBLEM] this is not a package
> Build the package with sgpack build first.
