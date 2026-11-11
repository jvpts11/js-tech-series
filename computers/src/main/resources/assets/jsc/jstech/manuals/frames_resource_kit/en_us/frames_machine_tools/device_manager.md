---
section: frames_machine_tools
order: 222
---
# The Device Manager

The Device Manager lists the hardware of the machine and the port each piece is plugged into. It lets you disable a device, which stops the computer from using it, and enable it again. It is on Frames only.

Its look follows the edition. On Frames 95 it sits in System Properties, under the name System. On Frames XP it is a console window, with the views in its View menu. On Frames 11 the views are a switch in its menu row.

Table: Views

| View | What the list groups by |
| --- | --- |
| Devices by type | What each device is. |
| Devices by connection | How each device is connected. |
| Devices by port | The port each device is plugged into. |

Table: Menus of the window

| Menu | Entries |
| --- | --- |
| File | Print... (does nothing), Exit |
| Action | Enable or Disable, Update Driver... (does nothing), Uninstall (does nothing), Properties |
| View | The three views, and Show hidden devices (does nothing) |
| Help | About Device Manager |

## Disabling a device

1. Select the device in the list.
2. Open the Action menu and choose Disable. A device that is already disabled offers Enable.
3. Look at the list: a disabled device is drawn crossed out.

A device you disable stays plugged in, and you can enable it again at any time. Only a device on one of this computer's own linked ports can be disabled.

## What a disabled device stops doing

Table: When a device is disabled

| Device | What stops |
| --- | --- |
| Monitor | It is not driven, and its video memory is not counted. |
| Speaker | It is silent. |
| Any Redstone Interface | It does nothing while its link is disabled. |
| Network Gateway | It is not active while its owner is disabled. |
| Dock Station | It does not start a desktop. |

> [!DEFINE] Port
> A socket on the machine, such as a card slot or a link port, that a device is plugged into.

> [!DEFINE] Hub
> A device that adds ports of its own. A device plugged into a hub is listed under it.

A hub is a device too, and disabling a hub disables every device under it. If you unlink a device, its disabled setting is removed, so a device you link again comes back enabled.

## If something goes wrong

> [!PROBLEM] The device you want to disable is missing from the list.
> Only a device on one of this computer's linked ports can be disabled. Link it first ([](jsc:peripheral_cables)).

> [!PROBLEM] Every device under a hub is disabled.
> Enable the hub again. A device disabled by itself stays disabled, so enable it too.

> [!PROBLEM] Update Driver, Uninstall, Show hidden devices and Print do nothing when you choose them.
> Use Enable and Disable, which are the actions that work.

> [!SEE] jsc:disable_device, jsc:peripheral_cables
