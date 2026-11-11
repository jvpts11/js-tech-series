---
section: starting_a_computer
order: 8
icon: jsc:floppy_disk
---
# Systems that players write

A player can write an operating system of their own in Sigma#, and a desktop of their own to go with it. The language and every call are in [The Book of Sigma](jsc:system_os_library). This page is about you installing and sharing one.

1. Write the system in a folder, with the desktop and its services beside it.
2. Type sigma build --os and the folder's name, with a blank medium in a drive: the installer is written onto it.
3. Type sigma build --desktop and the desktop's name to make its package.
4. Type sgpack --publish and the folder's name to put both on the network's Mirror.

A player's system installs as any other: from its medium through the firmware, or through the network ([](jsc:network_boot)). The boot manager lists it by its name. Its desktop comes with it the first time the system starts it, and the package managers of the mod's systems install the desktop from the Mirror by its name.

A system shows its own page while it comes up, and its own page when it goes down: the message at the top, the error with its lines, the system, its author, the process and the memory, and at the foot when it restarts.

## If something goes wrong

> [!PROBLEM] The machine goes down on the system's own page.
> Read the error and its line on the page, fix the system, build it again and install it again. Restart the machine from its power button when the page says so.

> [!PROBLEM] The build says no blank medium can be written.
> Put a blank floppy, CD or USB stick of the system's era in a drive. The system is kept anyway, and sgpack --publish can still put it on the Mirror.

> [!SEE] jsc:network_boot, jsc:system_os_library, jsc:the_systems
