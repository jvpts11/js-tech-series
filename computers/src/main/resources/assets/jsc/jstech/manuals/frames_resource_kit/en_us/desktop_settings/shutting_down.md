---
section: desktop_settings
order: 217
icon: jsc:monitor
---
# Signing out, shutting down and restarting

When you have finished, you can log off, shut the machine down, or restart it. Each edition has its own control for this, and most of them open the same Power dialog.

Table: The power control of each edition

| Edition | Where it is |
| --- | --- |
| Frames 95 | The row Shut Down, under the programs in Start. |
| Frames XP | Log Off and Turn Off Computer, in the footer of Start. |
| Frames 7 | The row Shut down, at the foot of Start. |
| Frames 10 | The power button, at the foot of the rail in Start. |
| Frames 11 | The power button, in the footer of Start. |

## The Power dialog

The Power dialog is a short list with the title "Power". It has three rows, in this order, and each row shows a short hint under its name.

Table: The rows of the Power dialog

| Row | What it does |
| --- | --- |
| Shut down | The machine powers off. |
| Restart | The machine powers off and starts again. If there is no goodbye message to show, the power-on self-test (POST) screen opens at once. |
| Log off | The screen closes, and the machine keeps running. |

A click outside the list closes the dialog without doing anything. The Escape key closes it too.

## Shutting down from the desktop

1. Open the Start menu.
2. Click the power control of your edition, as the table above shows.
3. Click Shut down, Restart or Log off in the Power dialog.

## Logging off on Frames XP

On Frames XP, the footer's Log Off closes the desktop at once, without the Power dialog. The machine keeps running.

## What the computer does

The desktop closes first. Then the machine acts on your choice: a shutdown turns it off, a restart starts it again, and a log off leaves it running. To open the firmware setup, use the button Restart to firmware on the System page of [Settings](jsc:frames_settings).

## If something goes wrong

> [!PROBLEM] You chose Shut down by accident.
> The dialog asks nothing after a row is chosen, so the choice goes at once. Click outside the list, or press [[Esc]], before you choose.

> [!PROBLEM] The computer restarts and shows the power-on self-test (POST) again.
> That is a restart, and it is expected. The check runs again at every start.

> [!PROBLEM] The computer still runs after Log off.
> That is expected: Log off closes only the screen. To shut the machine down, open the desktop again and use the Power dialog.

> [!DEFINE] Power dialog
> The short list of Shut down, Restart and Log off, with the title Power.

> [!SEE] jsc:firmware, jsc:frames_settings
