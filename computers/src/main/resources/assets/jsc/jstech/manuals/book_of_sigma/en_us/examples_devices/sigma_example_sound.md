---
section: examples_devices
order: 507
---
# Sound

Sound makes a beep or a tune through the machine, and plays a recording from its disks. This example uses the four sound calls, and one speaker found by its name.

A beep comes out of the speaker inside the case, and each new beep cuts off the one before it. A tune plays through the sound card, which plays every note of a chord.

A recording from the machine's disks comes out of the monitors and speakers, as its system says.

## The program

```sgs Sound.sgs
using System.*;
using System.IO.*;
using System.Sound.*;
namespace Corpus;
class SoundCorpus {
    static void Main() {
        Sound.Beep(880, 200);
        bool tune = Sound.Tones("C4:250 E4 G4 C4+E4+G4:500");
        bool song = Sound.Play("Users/Public/Music/intro.ogg");
        Sound.Stop();
        Speaker desk = Speaker.Named("Desk left");
        if (desk != null) {
            bool alone = desk.Play("Users/Public/Music/alarm.wav");
            Console.PrintLine(desk.Name + alone);
        }
        Console.PrintLine("" + tune + song);
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `Sound.Beep(880, 200)` | Plays a beep at 880 hertz for 200 milliseconds. |
| `Sound.Tones(...)` | Plays a tune: C4 for 250 milliseconds, then E4 and G4, then the chord C4, E4 and G4 for 500 milliseconds. It answers true when the tune plays. |
| `Sound.Play(...)` | Plays a recording from the machine's disks. It answers false when the file is not playable. |
| `Sound.Stop()` | Stops the sound of the program. |
| `Speaker.Named("Desk left")` | Finds a speaker linked to the machine by its name. It gives null when none has that name. |
| `desk.Play(...)` | Plays a recording through that speaker only, and answers true when it plays. |
| `desk.Name` | The name of the speaker. |

> [!DEFINE] Beep
> A plain tone at one pitch, for a set time.

> [!DEFINE] Pitch
> How high a sound is, measured in hertz. 440 hertz is the note A4.

> [!DEFINE] Note
> A named pitch, such as C4, that a tune can play.

> [!DEFINE] Chord
> Several notes played together, written with plus signs, such as C4+E4+G4.

> [!DEFINE] Speaker
> A block that makes sound for the machine.

> [!DEFINE] Recording
> A music file on the machine's disk, such as an .ogg or a .wav file.

## If something goes wrong

> [!PROBLEM] Tones answers false, and no tune plays.
> Check that the machine has a sound device and that its volume is above 0.

> [!PROBLEM] A tune holds a word that is not a note, and the program stops with a message that ends "is not a note".
> Write notes such as C4, a pitch in hertz, or R for a rest.

> [!PROBLEM] A chord on the speaker inside the case sounds as its first note only.
> Play the tune through the sound card, which plays the whole chord.

> [!PROBLEM] Speaker.Named gives null, because no speaker has that name.
> Test for null before you use the speaker, and check its name.

> [!PROBLEM] Play answers false for a file that is missing or cannot be played.
> Check the path, written with slashes, and the kind of the file.

> [!PROBLEM] A beep asks for a pitch below 20 or above 20,000 hertz.
> A beep is set to the nearest limit, so give a pitch from 20 to 20,000 hertz.

> [!SEE] jsc:soundfoundry, jsc:sigma_library
