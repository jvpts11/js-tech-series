---
section: hardware
order: 27
icon: jsc:sound_card_tone_blaster
covers_family: jsc:sound_cards
---
# Sound cards

Sound comes from the board or from a sound card. A Vintage or Legacy board has no sound of its own, so it needs a sound card for anything more than a beep.

> [!DEFINE] PC speaker
> The small beeper of the computer. On a Vintage or Legacy board with no sound card, it is the only sound.

The Artisan Tone Blaster family gives you the sound cards. Each one fits one era and one bus.

Table: The sound cards

| Sound card | Era | Bus |
| --- | --- | --- |
| Artisan Tone Blaster | Vintage | ISA |
| Artisan Tone Blaster 128 | Vintage | PCI |
| Artisan Tone Blaster Live | Legacy | AGP 8x |
| Artisan Tone Blaster Audigy | Legacy | PCI-e 1.0 |
| Artisan Tone Blaster Hi-Fi | Transition | PCI-e 1.0 |

A computer takes one sound card, of its board's era, and the card's bus must belong to the board's bus family ([the bus family](jsc:build_refusals)).

From the Transition on, a board has sound of its own, so the card is no longer needed. Its tooltip says "Sound on the board: plays everything". The earlier boards say "PC speaker: beeps only".

The sound you hear comes out through the monitors linked to the computer, and through speakers joined to it by peripheral cable ([the speakers](jsc:speakers)). With no monitor and no speaker linked, the sound goes to the PC speaker. Otherwise the sound card plays if there is one, then the board's own sound if it has any, and otherwise the PC speaker.

## If something goes wrong

> [!PROBLEM] The build says a sound card of one era does not sit on a board of another.
> Use a sound card of the board's era.

> [!PROBLEM] The build says "one sound card per machine".
> Take one of the two sound cards out.

> [!PROBLEM] A Vintage or Legacy computer only beeps.
> It has no sound card, and no monitor or speaker is linked to carry its sound. Put in a sound card of the board's era, or link a monitor or speakers.

> [!SEE] jsc:speakers, jsc:monitors, jsc:motherboards
