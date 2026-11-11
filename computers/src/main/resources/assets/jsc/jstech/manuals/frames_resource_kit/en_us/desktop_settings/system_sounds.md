---
section: desktop_settings
order: 216
icon: jsc:monitor
---
# System sounds

Frames plays short sounds for the events of the machine: startup, shutdown, an error, a notice, a beep, and a device that you plug in or take out. Each edition has its own chimes, so a Frames 95 startup sounds different from a Frames 11 startup. Each sound has a subtitle: the caption that names it on the screen, such as "Frames 95 chimes".

Table: The startup chime and the error sound

| Edition | Startup and shutdown | Error |
| --- | --- | --- |
| Frames 95 | Frames 95 chimes | Frames 95 error |
| Frames XP | Frames XP chimes | Frames XP error |
| Frames 7 | Frames 7 chimes | Frames XP error, borrowed |
| Frames 10 | Frames 10 chimes | Frames 11 error, borrowed |
| Frames 11 | Frames 11 chimes | Frames 11 error |

Frames 7 has no error sound of its own, so it uses the one of Frames XP. Frames 10 uses the one of Frames 11. A shutdown plays the startup chime of the same edition again.

## Sounds that the editions share

Table: Sounds shared by the editions

| Sound | When you hear it |
| --- | --- |
| Notice | "Frames chimes a notice", when a balloon appears, unless Quiet hours is on. |
| Beep | "Frames dings", when you click beside a dialog. |
| Device connects | When you plug a device in. Frames 95 has no such sound. |
| Device disconnects | When you take a device out. Frames 95 has no such sound. |

## Where the sound goes

The machine plays the sound through its monitor, its speakers, or both. The Output setting on the Sound page of [Settings](jsc:frames_settings) chooses: Monitor, Speakers or Both.

The same Sound page holds the Volume, which changes in steps of 10 percent from 0 to 100 percent, and the Mute switch, which shows On or Off. On Frames 10, Quiet hours also stops the notice sound, as the [notices](jsc:notices) entry explains.

## If something goes wrong

> [!PROBLEM] You hear no sound at all.
> Open the Sound page of Settings. Mute must be Off, and Output must reach a speaker or the monitor.

> [!PROBLEM] The Sound page says "No speakers linked".
> Link speakers to the computer's sound output.

> [!PROBLEM] The Sound page says "No sound hardware".
> The computer has no sound card or board sound. See [Sound cards](jsc:sound_cards).

> [!PROBLEM] The Test button is grey.
> The computer cannot play a sound at the moment, so Test is off.

> [!DEFINE] chime
> A short tune that the machine plays at startup and at shutdown.

> [!DEFINE] subtitle
> The caption that shows the name of a sound, such as the name of its chime.

> [!SEE] jsc:frames_settings, jsc:speakers, jsc:sound_cards
