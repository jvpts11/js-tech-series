# Third-party notices

The J's Tech Series is licensed under the LGPL-3.0 (see `COPYING.LESSER` and `COPYING`). It carries the
third-party work listed here, each under its own licence, whose notice is reproduced below as that licence asks.

## SnakeYAML

J's Core reads and writes settings files in YAML with SnakeYAML, which it carries unchanged inside its own jar
(`META-INF/jarjar/`), since NeoForge brings no YAML library of its own. SnakeYAML is licensed under the Apache
License, Version 2.0, whose full text is in `licenses/Apache-2.0.txt` here and in `META-INF/licenses/` of the
Core's jar.

https://bitbucket.org/snakeyaml/snakeyaml

```
Copyright (c) 2008, SnakeYAML

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License
is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied. See the License for the specific language governing permissions and limitations under
the License.
```

## neofetch

J's Computers' `screenfetch` command draws the logos of the systems it runs on as neofetch draws them: the art and
its colours for Ubuntu, Debian, Fedora, Arch Linux, Gentoo and FreeBSD, and the two marks of Frames, taken from
neofetch's script (release 7.1.0, and the script after it for one of the marks). They live in
`computers/src/main/java/dev/jstech/computers/program/cli/ScreenfetchLogos.java`.

https://github.com/dylanaraps/neofetch

```
The MIT License (MIT)

Copyright (c) 2015-2021 Dylan Araps

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Sounds of J's Computers

The machine and system sounds in `computers/src/main/resources/assets/jsc/sounds/` are recordings made by other
people. Each was cut to length, made mono where the game places it in the world, levelled and saved as Ogg Vorbis;
the running loops were seamed, and the floppy disk's eject joins two recordings. [ASSET_REGISTRY.md](ASSET_REGISTRY.md)
lists which recording each file comes from.

From Freesound, under the Creative Commons CC0 1.0 Universal Public Domain Dedication
(https://creativecommons.org/publicdomain/zero/1.0/), which asks for no notice; the authors are named with thanks:
Pixel_Stick (the power button), 607freesound (the old computer starting up and the floppy drive reading), conath
(the hard drive), griffinjennings (the disc tray), micropolis (the floppy disk going in and being drawn out),
asiekierka (the floppy disk ejected), soundandmelodies (the server room), Johnmode (Frames 95's chime), Lumineve
(Frames XP's chime), marlonnnnnn (Frames 11's chime and the Frames notice), Kastenfrosch (Frames 11's error), CZghost
(the failed self-test), Klerrp (the hard drive seeking), Sanderboah (the monitor switching off), leocb (a server on
its rails), SamsterBirdies (the disc drive turning), kyles (the Vintage Mainframe's tapes), dland (a device plugged in
and pulled out on Frames), Breviceps (the minefield's click), Tony B kksm (the mine exploding) and Fupicat (the
minefield cleared). From Kenney's Interface Sounds (https://kenney.nl/assets/interface-sounds), under CC0 as well: the
Frames bell.

### The desktops' own sound themes

The Unix desktops sound like themselves: their sounds are taken from the themes those desktops ship, under the licences
their authors gave them. Each file was cut of its silence, levelled and saved as Ogg Vorbis, and nothing else was
changed; a file shared alike stays under its licence, changes included.

- GNOME today (`os/gnome/`), from the freedesktop.org sound theme
  (https://gitlab.freedesktop.org/xdg/xdg-sound-theme): the error, the notice and the device sounds from Ivica Ico
  Bukvic's Borealis theme, under the Creative Commons Attribution-ShareAlike 3.0 licence
  (https://creativecommons.org/licenses/by-sa/3.0/); the bell by Dr. Richard Boulanger et al., under the Creative
  Commons Attribution 3.0 Unported licence (https://creativecommons.org/licenses/by/3.0/).
- GNOME as it looked on Legacy machines (`os/gnome_legacy/`), from gnome-audio 2.22.2
  (https://download.gnome.org/sources/gnome-audio/2.22/): the start-up and shut-down sounds from Andreas Karlsson's
  Silvertheme, under the Creative Commons Attribution-ShareAlike 2.0 licence
  (https://creativecommons.org/licenses/by-sa/2.0/); the error and the notice from Ivica Ico Bukvic's Borealis theme,
  under the Creative Commons Attribution 3.0 licence (https://creativecommons.org/licenses/by/3.0/).
- KDE as it looked on Legacy machines (`os/kde_plasma_legacy/`), from KDE's Oxygen sound theme
  (https://invent.kde.org/plasma/oxygen-sounds): Copyright (C) 2008 Nuno Filipe Povoa, under the GNU Lesser General
  Public License, version 3 or later, whose text ships with this mod (`COPYING.LESSER` and `COPYING`).
- KDE Plasma today (`os/kde_plasma/`), from KDE's Ocean sound theme (https://invent.kde.org/plasma/ocean-sound-theme):
  Copyright (C) 2023 Guilherme Marçal Silva, under the Creative Commons Attribution-ShareAlike 4.0 licence
  (https://creativecommons.org/licenses/by-sa/4.0/).

From Pixabay, under the Pixabay Content License (https://pixabay.com/service/license-summary/), which asks for no
notice either: EdR (the monitor), EagleStealthTeam (the server fan), BigKahuna360 (the USB drive pulled out), a
Pixabay sound of a USB drive plugged in, SoundShelfStudio (Frames 95's error) and Universfield (Frames XP's error).

The self-test beep comes from an old YouTube video (https://www.youtube.com/watch?v=eTvft2-afpo) that states no
licence. It will be replaced if its owner asks.
