---
section: desktop_settings
order: 215
icon: jsc:monitor
---
# Visual effects and the basic look

Effects are the animations and visual extras of a Frames desktop: the glass of Frames 7, the fades, the shadows and the motion when a window opens. You can switch them on or off. Each edition has its own Effects page, reached from another page of Settings.

Table: Where the Effects page is

| Edition | Reached from | Its title |
| --- | --- | --- |
| Frames 95 | Display | Effects |
| Frames XP | System | Performance Options |
| Frames 7 | System | Performance Options |
| Frames 10 | Personalize | Personalize > Effects |
| Frames 11 | Personalize | Personalize > Visual effects |

## What each Effects page offers

Table: The switches of each Effects page

| Edition | The switches |
| --- | --- |
| Frames 95 | One box: "Animate windows when minimizing and restoring". The footer has OK, Cancel and Apply. |
| Frames XP | Four presets, from "Let Frames choose what's best for my computer" to "Custom:". Four boxes: animating windows, fading or sliding menus, fading or sliding ToolTips, and drop shadows for icon labels; the footer has OK, Cancel and Apply. |
| Frames 7 | The same presets, plus "Enable transparent glass", and boxes for the animations, the menus, the ToolTips and the icon shadows. Under Ease of Access, a switch: "Turn off all unnecessary animations (when possible)". |
| Frames 10 | Two switches: "Transparency effects" and "Play animations in Frames". There are no buttons: a change takes effect at once. |
| Frames 11 | One switch: "Animation effects". There are no buttons: a change takes effect at once. |

On Frames 10, with Play animations in Frames off, windows open, close and minimise at once, and the live tiles stop turning.

## The basic look of Frames 7

Frames 7 draws its glass only when the graphics can run it. Your computer checks its graphics first. If they are too weak, Frames 7 falls back to the Basic look: the same frames and buttons, but opaque, with no glass. The check applies only to a computer built of parts.

The check uses the graphics score of the computer. The score comes from the best graphics card, or from the integrated graphics when there is no card. Video memory (VRAM), the memory of the graphics card, sets the highest score a card can reach:

Table: The highest score by video memory

| Video memory of the card | Highest score |
| --- | --- |
| Under 128 MB | 2.9 |
| 128 MB to 255 MB | 4.9 |
| 256 MB to 511 MB | 5.9 |
| 512 MB to 1,023 MB | 6.9 |
| 1,024 MB or more | 7.9 |

The Basic look applies when the score is under 3.0. A card with under 128 MB of video memory therefore always gets the Basic look on Frames 7, and so does a computer with integrated graphics and no card.

Frames 10 and Frames 11 check the graphics too. They keep their look, and only their effects go off.

When the Basic look is on, every effect is off. The Effects page then shows this note: "The graphics are too weak for these effects, so all are off. A stronger card brings back what is chosen here." Your choices stay saved.

When Frames 7 falls back to the Basic look, a balloon titled "Frames 7 Basic" appears, once.

## The experience rating

The System page of Frames 7 shows an experience rating. It has five scores: the processor, the memory, the graphics, the gaming and the disk. The lowest of the five is the base score.

## If something goes wrong

> [!PROBLEM] The glass is opaque, and the effects are gone on Frames 7.
> The graphics are too weak for the glass, so Frames 7 uses the Basic look. Install a stronger graphics card, with more video memory, and check the System page.

> [!PROBLEM] Animations stop on Frames 10.
> Play animations in Frames is off. Switch it on, on the Effects page.

> [!PROBLEM] Animations stop on Frames 7.
> The Ease of Access switch Turn off all unnecessary animations is on. Switch it off.

> [!DEFINE] Basic look
> The opaque look that Frames 7 falls back to, when its graphics cannot run the glass.

> [!SEE] jsc:personalize, jsc:graphics_cards, jsc:frames_settings
