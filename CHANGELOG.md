# Changelog

All notable changes to the J's Tech Series are recorded here, newest first. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); version numbers and phase letters follow
[docs/RELEASING.md](docs/RELEASING.md).

## [Unreleased]

### Added
- A 32-bit x86 machine now runs what was built for the 16-bit one, as the real 386 ran what an 8086 ran, and a
  64-bit machine runs both. Nothing runs what was built for a machine after it. This is what lets one program
  serve every age: `scc` builds for the oldest machine there is unless told otherwise, so a program written in
  Sigma runs on a Vintage computer and on every computer that came later. `sgsc` still builds for the 32-bit
  machines, since the full language's library is not something a Vintage computer has.
- `scc`, the Sigma Compiler Collection, compiles a `.sg` program into the assembly a machine runs. It is a package
  of its own, 4 MB of disk and 2 MB of memory, and the earliest machines can hold it: it is the whole toolchain
  there, since what it writes is the assembly the machine already runs and there is no runtime to install beside
  it. A Legacy machine or later can use it too, and a program built with it runs everywhere. The same program
  compiled by `scc` and by `sgsc` gives the same listing, which is what makes Sigma a subset in fact.
- Sigma has a library of its own, `Standard`, and it is the only one it can reach. Eight types with a handful of
  members each: `Console`, `File`, `Program`, `Math`, `Convert`, `Time`, `Computer` and `Script`. They are the same
  types the full language has, under a second namespace rather than copies of them, so a call written in Sigma
  compiles to exactly the line of assembly Sigma Sharp would write for it. What Sigma does not get is the other
  thirty-eight types and most of the members of these eight. `Script` lives there too, since it is how a program
  that stays up is written and Sigma cannot reach `System` at all.
- The compiler now knows two languages: Sigma Sharp, which is everything it has always taken, and Sigma, a smaller
  one for the machines that could never be programmed at all. Sigma is a true subset, so anything written in it is
  also Sigma Sharp and compiles on a newer machine untouched. It keeps classes with inheritance, `virtual` and
  `override`, structs, enums, arrays, the loops, `out`, `is` and `as`; it has no interfaces, records, delegates,
  events, properties, lambdas, `foreach`, `List`, `Map`, generics, `var`, `lock`, threads, windows, `abstract`, or
  strings with holes in them. Every refusal says what to write instead, because there is a way to write all of it:
  an interface is a class whose methods are virtual, a record is a struct, a `foreach` is a `for` over the array's
  `Length`, a `Map` is two arrays. Nothing already written changes: the full language is what every compiler and
  editor still uses.
- A program that stays up can now be written by standing on the class `Script` instead of implementing `IScript`.
  `Script` already has `OnInit`, `OnTick` and `OnDestroy`, doing nothing, so a script written this way fills in only
  the ones it uses and says `override` on each, while one written on the interface goes on writing all three with no
  word at all. Both are the same program to the machine, and `IScript` is not going anywhere.
- A class can now say which of its methods another class may replace, with `virtual`, `override` and `abstract`.
  A method marked `virtual` may be replaced by one below it written with `override`, and that one may be replaced
  in its turn without anyone writing `virtual` again. A method marked `abstract` has no body at all, only an
  `abstract` class may hold one, such a class cannot be made with `new`, and the first class below it that can be
  made has to give every one of them a body. Giving an interface the method it asked for needs no word, since that
  replaces nothing. `static` methods, constructors and everything in a `struct` take none of the three.
- F12 during the power-on self-test opens a one-time boot menu: every disk with a system and every drive holding
  something bootable, picked with the arrow keys and booted with Enter. What is picked there is for that boot only,
  and the boot order saved in the firmware setup is left exactly as it was.
- `sgsc --arch <architecture>` builds a program for a named architecture, by its id (`jsc:x86_64`) or by the name it
  is written under (`x86-64`). Left alone, the compiler builds for the oldest architecture that runs the program, so
  it runs on every machine it could have. An architecture nothing answers to is refused before anything is written.
- Virtual Studio's Project Properties has a Platform target: one button per architecture, and a line under them
  saying which machines the program will run on, amber once the choice leaves machines behind. A project keeps it on
  a `platform:` line, and a project file without one builds as it always did. A library project has no platform of
  its own, since it is compiled into the program that references it.
- `Computer.Cpu.Architecture` gives a program the architecture the machine it runs on is built on, written the way a
  listing names its own, so a program can compare the two.
- Every mod's jar now carries its licence texts under `META-INF`: the GNU LGPL 3.0 the series is licensed
  under, and the GNU GPL 3.0 it builds on.
- `Program.DroppedEvents` counts the clicks and watch alerts a program missed because too many of its calls were
  already waiting.
- Open with: a double-click on a file of a kind the computer does not know asks which program opens it, among the
  programs on the computer that open files. Only this time opens it once; Always makes that program open every file
  with the same extension on that computer. Choose another program..., at the end of Open with on the desktop and
  in Files (where Open with is now a submenu), brings the same choice up for any file. An addon registers a program
  that opens any file with `JSComputersAPI.registerFileOpener`.

- A machine in a rack now comes up the way any other machine does: its own power-on self-test, its own stop at a
  boot manager, and its own system taking the time it takes, all on the machine's clocks. Its bay switch is its
  power button, so flipping it off stops whatever was under way and flipping it on starts a self-test. The pages
  only reach a monitor while the KVM switch is on that bay, and the machine goes on regardless, so whoever
  switches over is put wherever it has got to.
- A machine in a rack now installs a system the way any other machine does. The bay keeps the copy and the
  installer it is in, written onto the Server item with the rest of its session, so the work goes on with nobody
  watching, takes the time it costs and is still going when the world comes back. It used to be written to the
  bay's disk there and then, in no time at all, with no installer and nothing to watch. Where a rack holds more
  than one machine, the installer's pages only reach the monitor while the KVM switch is on that machine's
  channel, so a page meant for one bay never lands in front of somebody looking at another.
- What was chosen while installing a system by hand now survives into it. The machine answers to the name the
  installer was given, and its prompt and the network use it. The packages asked for inside the new system are
  really installed on it. The filesystem table goes onto the disk it describes, so the installed system can read
  back the line its own bootloader was pointed at. A restart is refused while something is still arriving,
  rather than leaving the system half of what was asked for.
- Fetching a system by hand takes time, and what needs it waits. `pacstrap` and the stage 3 bring a base system
  over the Mirror at the network's speed, printing what they are fetching a package at a time, and nothing
  enters a system that is still arriving. Extra packages afterwards, with `pacman -S` or `emerge`, take their
  own time the same way.
- A hand-installed distribution now goes through the steps it was missing. `grub-install` installs the
  bootloader for the firmware the machine really has: on an older one it goes on the disk and says so, on a
  modern one it goes in the boot partition and refuses with nowhere to put it. `grub-mkconfig` writes the list
  of what to start, generated from the system that is really installed, and `mkinitcpio` builds what the kernel
  is handed at boot. `hostname` names the machine and writes it into the new system's own file. A reboot before
  any of those says which one is missing, since a bootloader with nothing to start starts nothing.
- A disk can be partitioned by hand before a distribution is installed on it. `fdisk /dev/sdX` opens the
  partition editor with its own one-letter commands: `g` for a new table, `n` for a partition (`n 512M`, or
  nothing for the rest of the disk), `t 1 uefi` to mark the one the firmware boots from, and `p`, `d`, `m`, `w`
  and `q`. Nothing reaches the disk until `w`, and `q` throws away everything typed since it opened. `lsblk`
  lists the disks at the size they really are with their partitions underneath, `mkfs.ext4` refuses a disk that
  someone has partitioned instead of wiping the table, `mkfs.fat` makes the boot partition, and that one mounts
  at `/mnt/boot`, under the root and after it.
- The live medium of a hand-installed distribution carries files, and `ls`, `cd`, `cat` and `less` to read them.
  The guide the real medium ships with is in `/root/install.txt`, written from the steps the shell really
  accepts. What a step wrote is what reading it back shows: the filesystem table is not there until `genfstab`
  writes it, and then it names the disk the install actually used. Inside the chroot a path is the new system's
  own, so `/etc/fstab` in there is the file written to `/mnt/etc/fstab` outside, and the prompt says where you
  are standing.
- The Frames editions put up a welcome the first time they come up, in the shape each of them used: a tip that
  changes with a column of buttons beside it, a pane of places to go with the machine written out next to it, and
  four cards. It asks nothing, because the installer already asked. Everything on it is read off the computer it
  is running on, down to the tips: the one about installing software names the Mirror only where a Mirror is
  answering, and the one about the network only appears on a machine that is on one. Every button opens a program
  that is really there, under the name that edition gives it. Unticking "Show this at startup" is remembered with
  the system.

### Changed
- A disk carries as many systems as it has room for, rather than one. Installing a second used to write over
  the first, with nothing anywhere saying what had been lost; it is installed beside it now and becomes what
  the disk boots. Every system on a disk takes its own room, so a disk carrying two is charged for two, and
  each of them remembers having been met on its own, so a system installed beside one you have already seen
  still greets you the first time it comes up.
- The Frames editions have a boot manager of their own, the Midsoft Boot Manager, whose file on the disk is
  `kickmgr`. Only the Linux family had one, so a Frames edition installed beside another could not be chosen
  between. Each family speaks in its own words: GRUB names the device an installation sits on, the Midsoft one
  names the edition and its disk. A boot manager lists every system on every disk rather than one for each
  disk, and the Frames one stays out of the way with a single installation, as it did, appearing once there is
  a second system to choose between. What is picked there is for that boot only; what a disk boots by default
  is written on the disk.
- The Frames editions now come up behind the picture each of them really came up behind, in place of the one
  screen every system shared: the sky with the logo over it and the bar along its foot, the black one with
  the three blocks running through their trough and the small print at its feet, and the maker's mark over a
  turning ring of dots. Each of them goes down behind the same picture, saying what it is doing. Every other
  system goes on reading out its own start, which is what those really did, and a system an addon brings gets
  that too without having to say anything.
- How long a system takes to install is now read off the machine's own parts rather than off its generation.
  Three things decide it and a player chose all three: the medium it is read from, the disk it is written to,
  and the processor that unpacks it in between. It used to be the medium and the generation alone, so every
  machine of an age took exactly as long as every other one and nothing anybody put in a computer ever showed.
  The disk is the one still being chosen while the installer is open, so the time is worked out again the
  moment a different disk is picked: putting a solid-state disk in a Legacy machine really does cut the wait,
  and the page says so before the copy starts. The floor and the ceiling are where they were, three seconds
  and ninety.
- A program is now built for the oldest architecture that has what it turned out to need, rather than for a fixed
  one. The compiler reads the listing back for the instructions it actually used and stamps the oldest machine of
  the line that has all of them, so a program runs everywhere it could have run instead of only on the newest
  chip. `--arch` still forces one, and forcing is left alone even where an older one would have done. Today every
  program is x86, because the 32-bit and the 64-bit x86 have exactly the same instruction set; whatever a later
  update adds to the newer one is what will start moving programs up.
- `sgsc` now refuses to build for x86-16, naming `scc` instead. Those machines run the smaller language only, and
  the way that stays true is at the compiler: a listing they can load can only have come from a source they could
  have held.
- Writing a method with the name and parameters of one a base class already has is now an error rather than a
  silent replacement. Either the one above is `virtual` and the new one wanted `override`, or it is not and the two
  are a collision; the message says which, and says what to add. There is no way to hide a base method. Which
  method runs has always been decided by what the object really is, and still is: what changed is that replacing
  one has to be written down, so nothing already compiled behaves differently.
- Compiling the Gentoo kernel now depends on the machine and on the player. The processor's cores and clock
  decide how much work it gets through, and how much of it happens at once comes from `MAKEOPTS` in
  `/etc/portage/make.conf`, capped at the cores the machine really has: left alone it builds one thing at a
  time, `-j4` on a four-core machine cuts the wait to a quarter, and asking for sixty-four of them on that same
  machine is still four. `echo` writes the line that sets it.
- How long an Operation waited and how long it ran are counted without a ceiling, so a craft left running for
  days still reads correctly instead of turning over.
- A mod built on this one now adds what it brings at one named moment, by listening for `CoreRegisterEvent` or
  `ComputersRegisterEvent`, and the two mods of the series add their own the same way rather than by a path of
  their own. After the loading is done every registry is closed, so what a world can install and knows how to do
  does not change under somebody playing it. Two things of the same id are refused rather than one quietly
  replacing the other. What a mod may use is gathered in `dev.jstech.core.api` and `dev.jstech.computers.api`, and
  `docs/API.md` says what is promised, what is not, and how long anything lives once it is marked as going.
  `JSComputersAPI` is now `JsComputersApi` in that package.
- The Start button carries its edition's own mark, four panes in that edition's colours, instead of the same
  four-coloured flag on all of them. It is the mark the setup, the installer and the screen the system comes up
  behind already wear, so a desktop now looks like the thing that installed it.
- The newest edition greets the machine by name the first time it comes up, in place of the maker's name and
  inside the same wait, so a first start says something different rather than taking longer.
- A system remembers having been met, and the mark rides on the disk it is installed on rather than on the
  machine. A computer with two systems therefore meets each of them once, a disk carried to another computer
  arrives already met, and erasing a disk and installing again is a first meeting all over. The advancement for
  booting a system for the first time used to fire on every boot; it now fires once, the first time that system
  comes up in front of somebody.
- Installing a system now goes through that system's own installer instead of one screen for all of them. It asks
  only what the game has: which disk, with what each disk already holds and the room the system needs; what the
  computer is called, which becomes the name the prompt and the network use; and, where a Mirror answers, a
  desktop to install along with it. A disk that already carries a system is erased first, after a confirmation
  naming what is lost, and the erasing happens when the copying starts, so leaving before that still leaves
  everything as it was. The installer belongs to the machine: closing the monitor does not cancel it, it is saved
  with the world, and opening the monitor again returns to the page it had reached.
- A machine running a Linux system now stops at its boot manager on the way up, listing every disk that carries a
  system, so the second one is something a player finds rather than something they have to know about. It boots the
  usual one by itself after five seconds; any key stops the count and the machine waits there. On the modern
  machines the firmware setup is one of the entries. Whatever is chosen there is for that start only. A new
  settings file beside the world save, `jscomputers-server.toml`, holds the switch that hides the menu.
- Switching a machine off now shows the system closing itself, on the systems that had such a screen. The earliest
  ones have none, as they had none in life: the monitor goes dark where it stands. A machine that goes down because
  its parts no longer make a computer says nothing either, since that is a plug coming out and not a shutdown.
- A Linux machine now reads out its kernel and its services as it starts: the architecture its processor really
  understands (i686 on the 32-bit machines, x86_64 on the others), the processor and the memory it found, a line
  per drive, and then the services this machine has, with the network only when a cable reaches one and a display
  manager only when a desktop is installed. Gentoo says it in OpenRC's words rather than systemd's.
- A starting system now says what it is finding, and every line of it is read off the machine: MC-DOS counts the
  memory above the line and gives a letter to each drive that is really in, naming it, and mentions a network only
  when a cable reaches one; MC-NET asks the network for the link, the Mainframe, the index and the storage, and
  says that nothing answered rather than opening on an empty list.
- A system now takes time to come up. Between the self-test ending and the desktop or the prompt opening, the
  monitor shows the system starting, for as long as that system's size, the disk it sits on and the machine's
  generation say it should: seconds from a solid-state drive, the better part of half a minute for a large system
  on a mechanical one. Like the self-test, it belongs to the machine: closing the monitor does not stop it, and
  opening one again joins it where it has got to.
- A Command Prompt window now opens with the name of the system it belongs to and that system's own copyright, the
  way the terminal on a machine with no desktop already did, instead of greeting the player with the mod's name.
- A self-test now ends by naming what it is about to boot ("Booting from Disk 0: Frames XP"), and a machine with
  nothing to boot ends on its own era's way of saying so and waits for a key, instead of dropping the player into
  the firmware setup without a word. A Legacy machine posts on black with its maker's badge, the way the boards of
  that time did; the setup it opens with DEL is still the blue one.
- The power-on self-test and the firmware's hardware page now show the machine that is actually there: the name its
  owner gave it, the processor by model with its cores and its architecture, the memory in megabytes with the slots
  it fills, the board, the video card, and the monitors really linked rather than a "connected" that was always
  true. No screen inside the fiction names the mod any more: the maker is JSC Technologies and the firmware version
  comes from one place.
- Putting a system on a disk is now the machine's work rather than the screen's. It takes as long as the system is
  big and the medium is slow, on the same rule a program's setup follows, where it used to be three and a half
  seconds for every system on every machine. Closing the monitor no longer throws the install away: it carries on,
  it is saved with the machine, and opening the monitor again joins it where it has got to. Taking the medium out
  part-way through stops it with nothing written, and a machine that cannot take the system says so before the copy
  starts instead of after it.
- The power-on self-test now belongs to the machine instead of to the screen watching it. It takes as long as the
  machine gives it reason to, growing with the memory and the devices seated and shrinking with the era, where it
  used to be three and a half seconds on every computer. Closing the monitor part-way through no longer stops the
  machine coming up or starts the test over: reopening shows what is left of it, and a machine nobody is watching
  boots all the same.
- A compiled listing now names the processor architecture it was built for, on a `.arch` line under the version, and
  the format's version is 3. A listing compiled before this still loads and runs as it did, and is read as having
  been built for the 32-bit machines.
- A machine now refuses a program built for an architecture its processor does not run, naming both (`A4015`, "built
  for x86-64; this machine is x86"). The 64-bit machines run everything written for the 32-bit ones, as they do in
  life; the earliest machines run only their own.
- A processor's tooltip now names its architecture, "x86-64, 64-bit", where it used to give the word size alone, and
  This PC, System Monitor and Settings say what the machine in front of you is built on.
- A computer now settles whether its installed parts can run it when those parts change, instead of working it out
  again for every machine on every tick.
- A computer now sends one player only so much in a tick, and whatever is left over goes first on the next one, so
  that a machine whose windows all changed at once, or one whose desktop has just been opened onto a full canvas,
  no longer puts the lot on the wire in a single tick. A window that has closed is always told of at once.
- A canvas now sends only what has been drawn on it since the last time it went over, instead of its whole picture
  again whenever anything in its window changes. Clearing one sends it whole again.
- A running computer now asks after the cable it is already attached to, one block, instead of looking round all six
  of its sides every tick and building a list of them to do it.
- A computer whose desktop somebody is watching now writes a window out for the wire only when something in it has
  moved. It used to write every open window out in full each tick and compare all of it with what it had last sent,
  so a desktop sitting still cost the server the same as one being used.
- The programming language is now called Σ# (Sigma Sharp). Its sources end in `.sgs` and its projects in
  `.sgsproj`; `sgsc` compiles, `sigma run` runs, and `sgpack` packs a program for the Mirror. The programs to
  install are the Σ# Compiler and the Sigma Runtime, from the Sigma Foundation. The compiler's error codes start
  with S (`S2001`) and a listing's with A (`A4012`), their numbers unchanged. Files saved as `.can` and projects
  as `.canproj` are no longer taken for programs; renaming them brings them back.
- A listing that calls, makes or reads something no computer has is refused before it starts, where it used to start
  and stop at that line: the terminal says what nothing answers and on which line (`A4013`), or that the listing
  writes a value that can only be read (`A4014`). A listing the machine cannot read at all now says why, rather than
  only that the file is not a Σ# program, and a program saved running from a refused listing does not come back.
- The strip under an editor's completion list now prices every call of the library and the machine, the free ones
  included, and the Gateway's and the windows' calls as well. A call written several ways, such as `Gateway.Call`,
  shows the price of the way the list is on, and a value a program may write says what writing it costs.
- A program pays for the size of the files it reads and writes. `File.Read` and `File.TryRead` cost 50 plus one for
  every 4 KB they read, and `File.Write` and `File.Append` 100 plus two for every 4 KB they write, where each used to
  cost the same for any size. `File.Append` adds to the end of a file without reading it first and is priced on what it
  adds; it used to read the whole file back and write all of it again.
- Every call a program makes to the machine costs one instruction more than it did. The price a call is given is now
  what it costs on top of the instruction that makes it, for the machine's calls as it already was for a program's
  own, so a price in the strip under an editor's completion list means the same thing for every call.
- A compiled `.asm` listing belongs to the computers rather than to Σ#: the machines run listings themselves, and Σ#
  only compiles. For addon authors, a language may now only compile (no binary extensions, and `start` and `restore`
  left alone): the machine runs the listing it compiles to, and compiles a source file on the way in when one is
  run. No language may claim `.asm`; `LanguageRegistry.reserve` keeps an extension back from every language, and
  `sourceOf` finds the language a file is written in. A listing opened in a terminal editor is coloured the way the
  desktop editors colour it.
- For addon authors, a language that runs its own files is handed a view of the machine: `start(binary, view,
  arguments)` and `restore(binary, saved, view)` take an `IMachineView` in place of the block entity and the memory
  size. The view tells the machine's time and how much memory the program may hold, and takes the lines the program
  prints, which the machine keeps; a language's processes no longer report `console()`, `written()` or `heapBytes()`.
- For addon authors, `ICliComputer`, the computer a shell command is handed, is made of one smaller interface for each
  thing a command reaches: `ICliMachine`, `ICliFiles`, `ICliNetwork`, `ICliOperations`, `ICliPackages`,
  `ICliInstallation`, `ICliConfig`, `ICliRemote` and `ICliProcesses`. Every member answers as a computer without that
  part does unless the computer answers it, so a computer made to test a command writes only what the command uses.
- A computer saves its running programs in a new form: a listing several programs run is saved once, and a program in
  an addon's language is saved with the language's id and the version of what it wrote. A program whose language is
  no longer installed is left out alone, and a save the computer cannot read brings no program back, which its
  terminal says the next time it is used. Programs running in a world saved before this version do not come back.
  For addon authors, `restore` is given the `stateVersion()` the program was saved with.
- Programs run faster. What a program's calls, branches and `new` reach is worked out once, when the program
  loads, instead of on every line it runs, and so is which of the language's own functions (text, `List`,
  `Map`, `Math`, `Convert`) a call means. Depending on what a program does, each instruction takes between 13
  and 34 percent less time.
- Compiled listings use format version 2. A listing compiled by 0.3.0a is refused with A4012; compiling its
  source again brings it up to date.
- `Random` draws from a generator of its own, so a given `Random.Seed` draws different numbers than it did in
  0.3.0a.
- A program keeps at most 256 calls waiting for their turn, holding at most 64 KB between them. A click or a watch
  alert that finds no room is dropped and counted in `Program.DroppedEvents`; a message that finds no room is
  refused, and `Process.Send` returns false. Closing a window and stopping a script always get in, ahead of
  everything already waiting.
- A program's windows count against its memory as they grow: rows added to a list, strokes drawn on a canvas and
  widgets placed by hand. Clearing a canvas or a list gives that memory back, and a text box keeps only its latest
  text however much a player types.
- Saves, network packets and menu data carry each setting by an id of its own instead of by its place in a
  list, so adding a setting never changes what an old one means. Machine states that 0.3.0a worlds stored the
  old way are not read back.
- Programs that were running when a world was saved by an earlier version do not carry on when it loads: they are
  left out, with a line in the server log, and can be started again.
- The programs a computer stops in one tick share 4,096 instructions of farewell (`OnDestroy`) between them, where
  each used to get 4,096 of its own. A computer switched off or broken shares that budget evenly among its programs,
  and a program stopped after the tick's budget is spent is stopped without its farewell.
- A computer checks the server's clock once every 512 instructions its programs run, or at once after a call into the
  machine, instead of after every 64. A computer that runs out of time in a tick can go that much further before it
  stops, and running its programs costs the server less.
- A file can have any extension. One of a kind the computer does not know, such as `thing.fk` written by a
  program, is saved, copied, moved and renamed like any other. The prompt, programs and the editor used to refuse
  it, and copying or renaming a file to such a name turned it into a text file.
- A language an addon registers is refused, with a line in the log, when it claims a file extension another
  language already has. Registering one again under the same id replaces the one before, and the log says so.
  Languages can be added or removed only while the game loads.
- The handlers behind the computers' screens and programs are split into packages by feature, with nothing changing
  in how they behave.
- The virtual machine that runs programs lives in packages of its own, with nothing of Minecraft in them, and reads
  what is wrong with a listing without the compiler.
- A running program is made of parts that each do one job: who it is, its console, its random numbers, the lines
  typed at it, the calls waiting their turn, its windows, its watches, who it tells when something is said to it,
  which of its threads runs next, the locks they hold, comparing its values, checking their types, reading and
  writing its fields, making its calls and its objects, carrying out its instructions, saving it and reading it
  back, and turning what is said to it into calls waiting their turn. Nothing a program does changes.
- Each of a program's threads holds what it is waiting for as one value. Every change to a window or a widget goes
  through one place, a window marks each change to what it shows and a canvas each time it is cleared, and a
  program's memory takes in whatever the computer hands the program and checks what the program reaches into.
- A computer keeps the programs it runs in a table of its own, which knows no language and finds a program by its
  number without building anything. It starts every program in one place, keeps the one in front of its terminal
  apart from the rest, and decides in one place, for its terminal too, whether a program is still going; screens
  list a computer's programs without reaching into them.
- Running programs costs the server less. A computer looks up what its programs watch only when one of them watches
  something, as one count for each item; it knows who is looking at it from the screens they open and close, instead
  of going through every player on every tick; it looks for its Gateways only after one of them is spoken to, and
  hands its programs what a Gateway heard without copying their list; and it claims its share of each tick without
  building anything. A program waiting on another one is told when that one ends, or asks at most once a tick when
  the other is on another computer.
- The rate at which a processor's clock buys instructions is named beside the time the server lends a computer's
  programs.
- Each call the system answers says who answers it (the language, the program's own process or the computer) and
  what it costs, and the compiler takes the types of the language's library from those declarations, where they are
  written once.
- A program's calls are matched to the system's declarations when it loads, and answered through them: its console,
  its random numbers, its threads, what it says about itself, its waits on other programs, the calls on its windows
  and widgets and what they hold, the values it reads of itself, of the world's clock and of the language's own text
  and collections, the widgets, lists and maps it makes, the watches it sets on the network, its calls on the
  computer's drives, what it reads of the computer itself, its network and its Mainframe, the Operations it asks the
  network for, the IQL statements it runs, the programs it starts and asks after, on its own computer or another of
  its network, and its calls through the computer's Gateways, which services the computer keeps answer. A computer
  keeps the shell its programs reach its drives and its network through, instead of making one for each call.
- A computer installs programs in one place, whether the prompt or a program asks: which programs it has, installing
  one from the disc in a linked drive, and the manual installation a live medium walks through, which ends by writing
  the new system to the chosen disk and rebooting into it.
- A computer installs packages in one place, whether the prompt or a program asks: what its package manager is, what
  the network's Mirror can serve it, installing, removing, updating, and publishing a package of your own for the
  rest of the network to install. The Mirror itself is kept there too, so the list of the network's services has each
  row answered by whoever keeps that service.
- A computer speaks the network's own language in one place, whether the prompt, a program or the management studio
  asks: installing the engine on the Mainframe and starting or stopping it, and carrying out every statement that
  changes something, buses included. The engine that runs a statement is now handed only what it uses, which is one
  way to read and one way to act, instead of a whole computer.
- A computer asks its network for work in one place, whether the prompt or a program asks: pulling an item in, pushing
  one out, asking for a craft, holding an item where it is and letting it go again, listing what is in flight and
  stopping or hurrying one of them, and the Mainframe's own jobs on its index. What the Mainframe measured of that
  work is read beside it, from the Mainframe itself.
- A computer reads its data network in one place, whether the prompt or a program asks: whether it is on a network and
  which, what it holds and where, its servers, and the rows a query brings back about items, servers, operations,
  computers and recipes. The verb and the state of an Operation are now named by the record that carries them, so the
  prompt, the terminal and a program's queries all read them the same way.
- A computer reads and writes its drives in one place, whether the prompt or a program asks. The drives it can see are
  gathered from the machine itself, a path that leads to another machine of the network is followed by one resolver,
  and opening, listing, writing, moving and formatting all go through one service over both.
- Every source file's header names the mod it belongs to.

### Fixed
- A screen the monitor put up that was not a system (the self-test, the boot manager, a system coming up, the
  firmware setup, an installer, a rack's channel switch) turned up in front of a player who had walked away,
  interrupting whatever they were doing. The server was never told when one of those closed, so it went on
  counting that player as watching the machine and put the next page in front of them. Those screens are menus
  now, like every other computer screen, so closing one tells the machine; they take the monitor of the machine's
  own generation instead of always the newest; and a recipe viewer sits beside them as it does beside the rest.
- A drive's name ran over the figures beside it on an installer's disk page. The name was cut to a fixed
  width that took no account of how wide those figures had turned out, so a long name and the size it was
  next to were drawn on top of each other. The room is measured now, on both the table and the wizard.
- A name being typed in an installer had a caret that never blinked and a field a long name ran out of. The
  caret blinks, and a name longer than the box scrolls under it the way a text field does.
- The buttons of an installer said nothing about the cursor being over them. The one under it is lit now, in
  every one of the five shapes an installer can wear.
- Frames 11 Setup opened on its table of disks, so the first thing it showed was a question about erasing
  something, with nothing having said what was about to happen. It opens on a word first, as the others do.
- A machine whose self-test found nothing to boot only stood at its failure for whoever was already watching.
  Opening the monitor afterwards dropped the player into the firmware setup with no word about why the machine
  had not started, and the failure closed itself after three seconds even for somebody looking straight at it.
  Standing there is now something the machine is doing, so every monitor opened on it finds it there, and it
  waits for a key however long that takes.
- The firmware setup read the machine once, when it opened. Taking an installation disc out of a drive and
  putting another in left the boot list naming the disc that had been removed, so booting it installed the
  system that was no longer in the machine. The setup asks the machine what it holds while it is open.
- The boot manager could not be left. Every key it did not use it swallowed, Escape included, so the only way
  off the list was to boot something: looking at what the other disk held was a decision with no way back.
  Escape leaves the monitor now, and the machine goes on standing at its boot manager, where opening the
  monitor again finds it.
- The shell of a live installation medium turned away `echo` and `pacman`, although the sequence behind it
  answered both. That made two of the steps impossible to take: the line that sets the build options in
  `/etc/portage/make.conf`, which is how a Gentoo kernel is told to compile more than one thing at a time,
  and installing a package into the new system afterwards. The shell now takes its verbs from the sequence
  itself, so there is no second list to forget a verb in.
- A computer's name could be fifteen letters, which is what the machines of the first age allowed and no fun
  to be held to. It is 256 now, and the three packets that carry a name onto a screen were all cut to 48,
  two of them by refusing a longer one rather than by trimming it.
- Installing a system from the firmware showed the same grey box whatever system it was, so the installer each
  one was drawn for was never reached: Frames XP Setup, the Debian and Fedora wizards, the pages that ask where
  the system goes and what the computer is called. The system was written all the same, which is why it was easy
  to miss. The firmware now sends the machine off and shows whatever the machine answers with, which is that
  system's own installer, a plain copy, or nothing at all when the machine wrote it there and then. Arch and
  Gentoo are unaffected: installing those means booting the medium and doing it by hand, which never went that
  way.
- A machine refused to start any program that reads or writes a field of one of its own classes, saying nothing
  answered it. That was every program with a field, and every program with a lambda, since what a lambda keeps hold
  of is a field too. Programs written before this run now with no change to them.
- A hold a player put on the storage with `LOCK` was let go of by closing and reopening the world, although it
  promised to last until somebody unlocked it. It is written down with the Mainframe now and taken again when the
  world comes back.
- Two readings of the storage under way at once were believed in the order they finished, so a reading started
  first could arrive last and put the network back to an older picture of itself: items taken in between came
  back, and items put in went missing until the next reading. Only the newest reading is believed now.
- A failed Operation said only the word `failed`, whatever had gone wrong. Opening it now says why in a line
  anybody can act on: what the network had run out of, what another Operation was holding, which stage of a
  pattern gave up, or that nothing on the network knows how to make the thing at all.
- Switching a Mainframe off left the Operations it was running reading as still running, for good: a terminal
  went on showing a craft under way by a machine that had no power, and nothing ever moved it off that. They
  now read as discarded, which is what happened to them.
- A craft, a pipeline or a machine run whose saved pattern could not be read came back as nothing at all, with
  no word anywhere about what had been lost. The same for a slot of a saved pattern and a row of the
  Operations log. What could not be read is now named in the log, so a world that comes back short says why.
- An amount too large to weigh came back as less than nothing, and everything after it believed that: a
  request for it passed every check that it fitted, putting it away added room to a disk rather than using it,
  and free space read as more than the disk holds. Asking for more than there could ever be now gets
  everything there is, the same answer a merely large number gets.
- An element written to and read in the same breath now names its place once. `n[Next()] += 5` and `n[Next()]++`
  worked out the index twice, so anything the index did on its way to a number happened twice, and if it did not
  give the same number both times the value was read from one element and written to another.
- A server in a rack no longer sits owing a power-on self-test that nothing ever runs. The flag was there and
  nothing carried it along, so the only thing that could end one was a screen open on that bay reporting it done:
  a rack nobody was looking at never came up, and opening the monitor was what made a machine start.
- A Linux machine on a cable now says its network is up as it comes up. The line was asked of the shell running on
  the machine rather than of the machine, so one that had no shell to ask came up silent about a network it was
  plainly on. A machine is on a network the moment a cable reaches one, which is before anything on it is running.
- An MC-NET machine that has nobody to ask about the network no longer comes up reporting that its link is down.
  Every line of that system's start is a claim about the network, and a machine that cannot put the question has no
  grounds for any of them, least of all for the one saying the cable is dead; it says nothing instead.
- A machine built west or north of the world's origin no longer ignores the drive an install was told to read from,
  and no longer carries on copying a system after the medium has been taken out of it. A drive is named by the
  position it stands at, and a position out there is a negative number, which both of those read as "no drive
  named".
- A shell window on a desktop now sends what you type to the machine you are connected to. Only the terminal that
  takes the whole screen of a monitor did: in a window, `ssh` opened the session and said it had, and then every
  command ran on the computer standing in front of you, with a prompt that named no machine either.
- A second person opening a computer's desktop now sees the windows its programs already have open. The machine kept
  one record of what it had sent, not one per player, so whoever opened after somebody else was shown an empty desktop
  until a window happened to change, and on a machine where nothing was moving it stayed empty.
- A prompt connected to another computer now says so: it shows the name of the machine the line is going to, ahead of
  that machine's own prompt. On MC-DOS and Frames the prompt is only the drive and folder, so a session looked exactly
  like sitting at your own keyboard and there was no way to tell the command had left the computer in front of you.
  The commands did run on the far machine all along; only the prompt kept quiet about it.
- `Program.Shell` runs a line at the computer's own prompt and hands back what it printed. It used to stop the
  program at the call.
- A program can no longer be compiled with `Program.RunSource`, which no computer answered, so the program stopped at
  the call. A listing that still uses it is refused when it loads, and the terminal says so (`A4013`).
- A program can no longer be compiled with the Gateway calls that reached into a ComputerCraft computer
  (`HasAgent`, `Run`, `Shell`, `Read`, `Write`, `List`, `Serve`, `Program` and `Programs`). They went away with
  the agent that answered them, but the compiler still accepted them, so the program stopped at the call.
- Cutting the last data cable between a Mainframe and the rest of its network now takes the network away
  from everything past the cut. The computers there no longer keep working through a Mainframe they are not
  connected to, and the network does not come back when the world is loaded again.
- Constructors can chain to another with `: base(...)` or `: this(...)`. The first chained call used to stop
  the program.
- `Map.TryGet` and `Map.Remove` treat a key that holds `null` as present, as `ContainsKey` does; they used to
  answer false.
- `Map.TryGet` on a missing key fills its value with what a variable of the map's value type starts with
  (nothing for text and objects, zero or false for numbers and bools) instead of 0 in every map.
- `Program.Current.Name` follows `Program.SetName`. It used to keep the name the program had when
  `Program.Current` was first read.
- A program that stays up for a very long time no longer sees its count of instructions run, or of lines
  written, wrap into negative numbers.
- A program's random numbers carry on where they were when the world is saved and loaded, instead of starting
  the sequence over.
- Lines typed at a program's terminal before the program asked for them are still there after a save.
- A line read with `Console.ReadLine` is still there after the world is saved and loaded, and it counts against the
  program's memory like any other text. It used to come back empty.
- An event with several handlers joined to it with `+=` runs all of them, in the order they were joined, when a
  player clicks or closes a window, a message arrives or a watched stock changes. Only the first handler used to
  run.
- A program that calls itself without end halts with a message once its calls go more than 1,024 deep. It used to
  keep adding calls, held in the server's memory, for as long as it ran.
- A thread that ends while it holds a lock, stopped with `Stop` or taken down with its program, lets the lock go,
  and whoever waited for it carries on. The program used to halt with "the runtime could not carry this out".
- A script whose `OnTick` runs longer than a tick, or that is still busy with a handler when the next tick comes,
  skips that tick. It used to run one extra `OnTick` as soon as it was free.
- A program whose last window a player closed ends even when the world is saved before the program has heard about
  it. It used to keep running with no window after the load.
- What a program drew on a canvas and the widgets it placed in a window by hand come back after the world is saved
  and loaded, and so do what a player typed into a text box and any text the program read from one. They used to
  come back empty.
- A row or a column can no longer be put inside itself, and a window holds at most 256 widgets however deep they sit
  in its rows and columns. A row inside itself used to fill its window with copies of the same widgets.
- A program's widgets take from a player only what they could really receive: a row the list has, a point inside
  the canvas, a line of up to 256 characters, and nothing at all while hidden. A forged request used to set a
  selection past the last row or a click far outside the canvas.
- The Cluster Manager shows a section balancing round-robin as round-robin. It used to say MANUAL.
- A listing edited by hand in which a type stands on itself no longer sends the server into an endless loop
  when the program loads.
- A saved program whose state is damaged, or was taken from a listing that has changed since, is left out when the
  world loads, with a line in the server log. It used to come back with parts of it wrong, or keep the computer
  holding it from loading at all.
- A program holding something its save cannot write is left out of the save, with a line in the server log, instead
  of being saved with that part silently empty.
- A machine that has started more than two billion programs numbers the next one from 1 again, passing over numbers
  still in use. The next program used to get a number below zero and be reported as not started while it ran.
- A program run at the prompt from a path written with backslashes, such as `sigma run C:\progs\game.asm`, is
  listed by its file. It used to be listed under the whole path.
- A computer whose terminal had a program in front of it that could not be loaded back, because its language is no
  longer installed or its saved state was refused, gives its prompt back after the load. Every line typed there used
  to go nowhere until Ctrl+C.
- A computer that a Gateway kept busy still owes that work after the world is saved and loaded, and goes on paying
  it back out of its programs' share of each tick. The debt used to vanish with the save.
- A machine fed a gas or a fluid through the network, such as a Purification Chamber burning oxygen, no longer
  stalls on the last lot of a request when it uses a little more than its pattern says, and no longer settles that
  request as partial: when it runs dry while the request is short, the network gives it another lot's worth.
- `Program.Send` answers false for a program that has already returned. It used to answer true while the
  program was still listed for its terminal or its parent to read, though nothing ever read the line.

## [0.3.0a] - 2026-09-13 - The Programming Update

Codename: Lithium.

The computers became programmable. Cannon, the series' own language, is written, compiled and run on
the computers themselves, with five editors to write it in, and ComputerCraft's computers reach the
data network through the Network Gateway. The computing mod is now called J's Computers.

### Added
- Computers can now be programmed with the series' own programming language, Cannon. You write it at any computer, compile
  it there with `cannonc` down to assembly, and run what comes out. The compiler produces a listing you can open and read a
  line at a time, so any output can be verified.
- For now, a program is one of two things, and says which by how it is written. One with a `Main` runs at the
  terminal that started it, holds the prompt, prints as it goes and is gone when it returns. One that
  implements `IScript` stays up: set up once, called every tick, told when it is stopped, and still running
  after the world has been away and come back.
- A program spends only what the machine's processors are worth in a tick, so an old computer really does
  print line by line where a fast one finishes at once, and one that loops forever costs its machine the
  same tick as one that does nothing. There is no ceiling on that worth: a faster machine gets through
  more, however fast it is. What protects the server is the clock instead: each machine may run its
  programs for so much real time a tick, and all machines together for so much, both set in
  `jstech-balance.toml`. A machine the server had no time for goes first on the next tick, so a busy
  server slows every computer evenly and never stops one.
- A program can do more than one thing at once. `Thread.Start` runs a body beside the rest of the
  program, the two taking turns a few instructions at a time over the same memory; a thread can sleep
  for so many ticks, yield its turn, wait for another to end (or give up after a while) and be stopped.
  Because nothing runs at the same instant, a single expression is never torn; a run of them can be,
  and `lock (thing) { ... }` keeps it together, held on every way out of the block. Threads, their waits
  and their locks come back from a save where they were.
- Programs can start programs. `Program.Start` runs a compiled program from the same disks, with
  arguments the other reads through `Program.Args`, and hands back a handle to wait on, read the output
  of, ask the exit code of, or stop. A program ends itself with `Program.Exit(code)`; what another program
  started keeps its output and code for that program to read until it goes. Programs on one machine
  can send each other lines, heard through `Program.OnMessage`. At the prompt, `cannon run` passes on
  whatever follows the file name.
- Computers on one data network can share folders with each other. `config share C:\pub` opens a folder
  for reading, `config share C:\pub write` for writing too, and `config unshare pub` closes it. Every
  other machine on the network reaches it as `\\host\pub` at the prompt (`/net/host/pub` on the Linux
  systems), from a program through `File`, and in the file explorer under Network, where hosts, their
  shares and the files inside can be browsed, opened and copied.
- A program can reach the other computers on its network. `Network.Computers()` lists them and
  `Network.Computer("desk")` picks one; on it a program can start a program from that machine's disks
  (the same handle as a local one comes back, to wait on, read and stop), run a line at its prompt, send
  a line to one of its programs and list what it runs. Everything runs on the other machine, out of its
  own budget. A machine that would rather not take any of that says `config remote off`.
- A program can speak the network's own language. `Iql.Run` sends a statement to the Mainframe as the
  prompt would and hands back whether it went, what it said and the rows it read; `Iql.Query` gives
  the rows alone and stops the program on a refusal; `Iql.Exec` runs a saved procedure and `Iql.RunFile`
  a file of statements, one a line.
- What a program can reach: the machine's own drives, with the same paths the prompt uses; what the machine
  is made of and what it is running; what the network holds, could hold, and which servers hold it; what the
  Mainframe has been doing this past hour; and the network itself, to pull, push, craft and cancel. Every row
  it leaves in the network's log names the program that asked.
- A program can ask to be told instead of asking. It says once that it wants to know when something runs
  low, and is called when it does: on the crossing, not for as long as it stays crossed.
- Programs can be handed to other people. `canpack` wraps one up with everything it needs into a single
  readable file, publishes it to the network's Mirror, and anyone on that network installs it like any other
  package, marked as a player's own. An installed one gets an icon on the desktop and a terminal to run in.
- A program's memory is counted like everything else the machine holds, so the Task Manager lists it beside
  the windows and services, and a machine without the memory for one says so instead of trying.
- The file explorer knows a source file, a compiled program and a package on sight, and running one is a
  double-click.
- The Network Gateway, a peripheral that puts the data network within reach of ComputerCraft's computers
  when CC: Tweaked is installed. It is placed facing away from the computer it serves: the peripheral cable
  plugs into its back, a ComputerCraft computer, wired modem or networking cable meets its front, and
  there it is a `jsc_gateway` peripheral under its own name. Between the two sides sits a buffer of nine
  slots, reachable from the other faces by a chest, a hopper or a turtle, and shown on the block's own
  screen with a status line and two lights. Several Gateways may serve one computer.
- The Gateway Manager, a program for the computer that has Gateways on its ports: the rail lists them
  by name, Rename and Identify (the block's lights blink) act on the selected one, and four tabs show it.
  Status puts this side and the ComputerCraft side as two cards, says how CC names the Gateway, shows
  the buffer with a button that empties it into the network as operations, and lists the last requests.
  Permissions holds the switches for reading the network and running operations, a priority ceiling for
  requests from CC, and how many calls a tick the Gateway answers. Computers lists the ComputerCraft
  computers attached and can send them a test event; Log keeps the last forty things the Gateway did.
  The `gateway` command does the same at any prompt.
- Through a Gateway, a ComputerCraft program reads the network (what it holds and where, how much room is
  left, which computers are on it), pulls items into the Gateway's buffer and pushes them back, asks for
  a craft, follows and cancels operations, watches a total and is told when it moves, writes a line in
  the Gateway's log, and starts a program on one of our computers. Every call is paid for out of the host
  computer's tick, counted against the Gateway's call cap, kept under its permissions and written in its
  log; an operation that settles and a watched total that moves come back as events.
- `cannon run hello.can` runs a Cannon source file as it is, compiling it on the way in.
- A Cannon program reaches the ComputerCraft side through its machine's Gateways. `Gateway.Names` lists them
  and `Gateway.Select` picks the one a program means, which stays with the program; `Gateway.Computers` and
  `Gateway.Peripherals` say what is on that Gateway's wire and what each thing answers to; `Gateway.Call`
  calls one of those methods and brings back what it said (a table comes back as a list or a map, a whole
  number whole); `Gateway.TurnOn`, `Shutdown` and `Reboot` work the computers over there; `Gateway.Send`
  says something to one of them, and `Gateway.OnMessage` hears what they say back through the Gateway's own
  `send`. A machine with no Gateway, or a Gateway with no ComputerCraft behind it, says so.
- A Cannon program can open windows of its own on the desktop of the machine it runs on. `System.UI` gives it
  a `Window` and the widgets to put in it (`Label`, `Button`, `TextBox`, `CheckBox`, `ProgressBar`, `ListBox`
  and a `Canvas` it draws on itself), laid out in rows and columns where a weight takes a share of the room
  left over, or placed exactly where the program says. It hears what the player does through `OnClick`,
  `OnChange`, `OnSubmit`, `OnToggle`, `OnSelect` and the window's `OnClose`. The machine's own system draws
  it, so the same program is a Frames 95 window on 95 and an XP one on XP, and it has a place on the taskbar
  under the program's name. A window is saved with the program and comes back with it; shutting the last one
  ends the program unless it opens another. A machine that boots to a prompt has nowhere to put a window and
  says so.
- A number, a bool or a character can now go where an `object` goes, and a cast takes the number back out
  as whichever kind is asked for: `int n = (int) held;`.
- Mods may add a programming language of their own, and remove this one. A language that registers itself
  gets the prompt, the terminal, the task manager, saving and the tick budget without writing any of them.
- Five editors to write a program in, each a different bargain between what it shows you and what it costs
  the machine to keep open. Virtual Studio is the whole workshop in one window, and the only one that tells you
  what a line will cost the program before you write it. Virtual Studio Code offers the same suggestions in a
  fifth of the memory, with the machine's own console welded into the bottom of the window. Exposure suggests
  nothing and instead compiles every program on the disk, so changing something shared shows which of the
  others stopped building. Vim and Emacs open no window at all: they take over the terminal they were started
  from, which is what lets a rack server with no screen be programmed, or one reached from another machine.
  Emacs splits that terminal, so a program and what the compiler said about it are readable at once.
- Whatever language a file is written in colours it, marks in the margin where the compiler stopped, and
  suggests what can follow a name, in every one of the editors. A language a mod adds gets all of it.
- Virtual Studio works in solutions. It opens on a Start Window: what you opened lately, or a project or
  folder or file to open, or a new project to create. Creating one picks a template first, filtered by
  language, platform and kind, then names the project and its solution, and writes a solution file, a
  project file and a first source that already builds. The Solution Explorer shows every project with its
  dependencies, properties, sources and what it built; Build compiles a project with the libraries it
  references and writes the listing where the project file says; Start builds the startup project and
  runs it at the terminal; Package hands the project to `canpack`.
- Virtual Studio Code works the way its namesake does: a Welcome page, a folder opened as a tree beside
  the editor, a command palette on Ctrl+Shift+P, F5 to build and run the open file at the terminal welded
  into the window, and an Extensions page listing every language the machine knows.
- Installing takes time. Setup runs as a job on the machine: a window with a progress bar on a desktop,
  lines at the prompt, cancellable from either, and how long it takes depends on where the program comes
  from (a floppy is slow, a DVD faster, the network faster still) and how big the program is. A machine
  that goes away mid-setup picks it up where it was.
- `setup.exe` on an installation disc, double-clicked in the explorer, starts that setup.
- An installed program is on the disk. Its folder under Program Files (or Program Files (x86), for one
  older than the system) holds the program, its settings file, its libraries, a font when it brought one
  and what came on its disc, and the system's own folder is called Frames, with the same kind of contents.
  The explorer, the prompt and the studios read them like any other file.
- An installation disc reads as a disc: its sources folder holds the setup, the packed program and its
  checksums, its support folder the read-me and the checksum list.
- Every terminal on a machine is its own session, with its own place on the disk: two Command Prompts
  can be in two folders, and what one prints is only in that one. Closing a Fedora terminal and opening
  it again shows what it printed before.
- Cannon strings can carry values: `$"Total {a + b}"` fills the braces with what the expression comes to.
  Two braces in a row are one brace of text.
- The code editors in both studios grew up: text can be selected with the mouse or with Shift and the
  arrows, cut, copied and pasted; Ctrl+Z and Ctrl+Y undo and redo; the font zooms with Ctrl and the
  wheel; a new line keeps the indentation of the one before; a bracket or quote closes itself; Tab and
  Shift+Tab indent the selected lines; and the marks in the margin say what the compiler said when the
  cursor rests on them.
- Files close. Ctrl+W, the cross on the tab or File > Close closes the open file, asking first when it
  has changes. Ctrl+W in Virtual Studio Code does the same.
- The explorers of both studios show the same icons the file explorer shows, and the file explorer's
  columns can be dragged wider or narrower.
- Implement Interface: with the cursor on a class that says it implements one, Edit > Implement Interface
  (or Ctrl+.) writes the methods the interface asks for that the class does not have yet.
- Virtual Studio's dock splits into Error List, Output and Terminal; the Error List is a table of code,
  description, file and line, and the panels and the Solution Explorer resize by dragging their edges.
  Starting a console program runs it in the dock's terminal.
- The Create a new project page shows the templates used lately beside the list, filters by language,
  platform and project type, and the Configure page asks where the project goes, with a "..." to browse.
- Cannon has namespaces and usings, and every type lives in one. `namespace Tools;` at the top of a file
  puts its types there, or a `namespace Tools { }` block does, and blocks nest. Types in the same
  namespace see each other plainly; anything else is brought in with a using: `using Tools.*;` for the
  whole namespace, `using Tools.Counter;` for one type, and `Tools.Counter` names one from anywhere. A
  file with no namespace, or a name used without its using, is told so by the compiler.
- The language's own library is the `System` namespace, with a namespace inside it per subject: the
  console and files under `System.IO`, lists and maps under `System.Collections`, `Math` under
  `System.Utils`, the machine under `System.Machine`, the network under `System.Network`, what the
  Mainframe has been doing under `System.Operations`. A program says which of it it uses.
- Structs and records. A struct is a value: assigning one copies it, two are equal when their fields
  are. A record is written as a line, `record Point(int X, int Y);`, and gets its fields, its
  constructor, its `ToString` and its equality from that line.
- Classes nest: a class declared inside another is named through it, `Farm.Counter`, and is a type in
  every way the outer one is.
- Programs take input. `Console.ReadLine()` waits for a line typed at the terminal the program runs in,
  and `Console.HasLine()` says whether one is waiting; a program waiting reads as "input" in the Task
  Manager. Typing at a terminal with a program in front goes to the program. `ReadInt`, `ReadLong`,
  `ReadDouble` and `ReadBool` wait the same way and hand the line back as a value, and stop the program
  with the text they could not read when it was not one.
- A program can say what it is called. `Program.SetName("Sorter")`, under `System.Execution`, is the
  name the Task Manager, `cannon ps` and the machine's own process list show for it, and `Program.Name`
  reads it back. A program that gives itself no name is listed as `cannonrt`, the runtime running it,
  the way an interpreted program shows up under its interpreter on any machine. The name survives the
  world being away and back.
- `Convert` turns text into values the other way round: `ToInt`, `ToLong`, `ToFloat`, `ToDouble`,
  `ToBool` and `ToString`, which stop on text that is not one, and `TryInt`, `TryLong`, `TryDouble` and
  `TryBool`, which say whether it was and hand the value out sideways, for a program that would rather
  ask again.
- The file explorer's address bar is a text field once it is clicked into, with the whole path
  selected: Ctrl+C copies it, typing replaces it, a click in the text puts the caret there, and Shift
  with the arrows (or a drag) selects part of it to copy. Every text field on every desktop selects
  the same way, and Ctrl+A selects all of one.
- The file explorer's right button, on an empty part of a folder, offers "Open in" the desktop's
  terminal, which comes up with its prompt in that folder.
- The Solution Explorer works with the right button: a source offers Open, Exclude From Project and
  Delete (which asks first, then takes the file off the disk, off its tab and out of the project); a
  project offers Build, Set as Startup Project, Add New Item, Add Existing Item, Add Project Reference
  and Properties; the solution offers Build, Clean and Add New Project.
- A machine remembers what its windows had open, not only that they were open: after the game itself
  was closed, Virtual Studio comes back on its solution with the same files on its tabs, Virtual
  Studio Code and Exposure on their folder and files, and the explorer on its folder.
- The editors suggest the program's own names, not only the language's: after a dot, what the variable,
  the field, `this`, `base` or the type before it has, read one name at a time through a chain; on a
  bare name, the variables in reach, the members of the class around the caret and every type the
  program declares, in the other files of the folder and in the libraries the project references too.
  The suggestions come while the line is still broken, which is when they are asked for.
- Every terminal moves the caret within the line: Left and Right, Home and End, Ctrl with Left or Right
  by a word, and Delete.
- Vim knows more of itself: w, b, e, ^, G and gg to move, dw and D to cut, yy, p and P to copy a line
  and put it back, u and Ctrl+R to undo and redo, and `:12` to go to a line. Emacs answers C-f, C-b,
  C-n, C-p, C-a, C-e, C-d, C-k, C-y, C-x u, M-< and M->, and asks before leaving with changes unwritten.
  Both slide a long line sideways to keep the caret in view.
- The code editors have a scroll bar down the side and one along the bottom, slide sideways with Shift
  and the wheel, and take a whole step of indentation back on Backspace. The right button on the code
  opens Cut, Copy, Paste, Toggle Line Comment and the refactorings, which is where Implement Interface
  lives now (Ctrl+. still works). Virtual Studio's zoom is a control in the status bar and under View;
  Virtual Studio Code keeps it under View > Appearance.
- The explorer's New entry opens a menu of what can be made, a folder first. A click past the last crumb
  of the address bar turns it into a path to type, copy or paste; every text field answers Ctrl+C, X
  and V.
- The desktop's right button offers what a desktop offers: Open, Open with, Rename, Delete and
  Properties on an icon; New, Refresh, Display settings, Personalize and Properties on the wallpaper.
- The system has a file window of its own, the one every program opens to choose a file, a folder or
  where to save: the places on the left as the explorer lists them, the way back, forward and up, the
  address as crumbs that can be typed over, the folder's contents with the explorer's icons and columns
  and only the files of the kind asked for, then the name, the kind and Open, Save or Select Folder. A
  double click enters a folder or takes a file, Save asks before replacing, and it wears each desktop's
  clothes. Virtual Studio, Virtual Studio Code and Exposure open and save through it; any program can.
  It is a window of its own: it comes up over the program that asked, listed with that program on the
  panel rather than as a program of its own, and holds the program until it is answered or put away.
- The panel lists programs, not windows. A program opened twice has one entry, and the entry says how
  the program stands: pinned with nothing open, open, in front, or put away, each in the panel's own
  language (Frames 11 marks the icon underneath and splits the mark for several windows; Frames XP
  pushes the button in and groups several under a count; KDE and Cinnamon underline, fill and stack).
  Resting the cursor on an entry of Frames 11, KDE or Cinnamon shows a card per window with its live
  picture; a click on a card brings that window forward, its cross closes it alone. Frames XP and 95
  list the titles instead, the way those desktops did.
- Programs pin to the panel. The right button on an entry, on a desktop icon or in Start offers Pin to
  taskbar, and a pinned program keeps its place with nothing open (on the quick launch beside Start on
  Frames XP; in place on Frames 11, KDE and Cinnamon). The pins are the machine's, kept with its other
  settings, and a fresh machine pins its file explorer. `settings pin files` and `unpin` at the prompt
  do the same.
- Exposure is an editor: File makes, opens, saves and closes files and picks the folder, Source comments,
  goes to a line and refactors, Project rebuilds the folder and runs the open program at the terminal.
- Settings > Display has a Scale: the desktop draws everything at 100, 90, 80, 75, 66 or 50 percent of
  its designed size, so a machine can fit more on its glass. Every machine starts at 75, which is the
  size that reads best on the monitor. The `settings guiscale` line at the prompt sets it too.

- Every system, desktop, service and program holds a share of the computer's RAM, in megabytes, and a program
  opens only while it still fits: a bundled program weighs a share of the system it ships with, an installed one
  what its generation weighs, so a modern tool needs the gigabytes a modern machine has. The System Monitor lists
  who holds what.
- A balloon over the notification area carries the notices a computer raises by itself, the first being the
  one about memory.
- The Task Manager, on every desktop, in the shape that desktop really had: the Close Program box on Frames
  95, the four-tab manager with its menu and status bars on Frames XP, the page rail on Frames 11, and the
  system monitor each Linux desktop's own package brings. It reads this machine (its processes and what
  each holds, its memory, processor, disks and network link) and ends the program you pick. It has no icon
  of its own: right-click the panel and it is one entry on the menu, the way these desktops offered it.
- Every panel's notification area shows whether the computer is on a data network, alongside a speaker and
  the memory bar; resting the cursor on it reads out the link and the figures.
- The Network Interactor's grid is driven from the keyboard: a dotted cell marks where it is, the arrows
  move it, Enter opens the request dialog, C the craft dialog, F stars the item and the slash puts the
  keyboard in the search. A line under the status says so.
- A click on the grid selects an item and a double click opens it. Several are selected at once by
  dragging a band across them, with Shift and the arrows, with Control and a click, or with Control+A;
  a click on empty grid drops the selection. Enter or Request then asks for all of them in one dialog, a
  quantity each, and F stars them together. The details panel lists what is selected and what it weighs.
- Two drop-downs beside the search narrow the grid to the mod that made the item and to what the item is
  (ingots, ores, raw materials, blocks, machines, tools, food and so on); both stack with the search.
- Items can be starred. A star sits on the cell, a Favourites tab at the strip's right end shows the starred
  items alone (a craftable one even when none is in stock), and the machine keeps the stars, so every window
  on it shows the same.
- Two grips reshape the window's insides: the one between the grid and the details panel gives the grid
  more columns (the inventory keeps its nine and sits centred under the wider grid), and the one above the
  inventory folds its top rows away so the grid gets their room. Where they were put, the search, the
  filters, the sort and the tab all come back with the window.
- The details panel says what the network holds of the item and where, which recipes make it and through
  which machines, what other patterns use it in, and offers Request, Craft and the star. The item's id,
  weight, tags and components follow below.
- An item the network makes more than one way (a processing recipe and a multi-stage one, say) shows the
  recipes side by side in the craft dialog, each with its machines, stages, time and inputs against the
  stock. Picking one plans with it and a strip says what it does differently from the other: the ingredients
  swapped, the stages added, the seconds gained or lost for the amount asked, and what it is short of. The
  pick is remembered per item, so the next request opens on it. Left and Right pick from the keyboard.
- The craft dialog's plan reads a short ingredient in red and says under it what the network would craft to
  cover it, or that nothing on the network makes it.
- Every zone of the Network Interactor has a boundary of its own now, drawn by the desktop's skin: the
  toolbar on a band, the items in a captioned, sunken field of drawn cells (empty ones too, so an empty
  network looks like one), the inventory in a second field, the details in a framed panel with a header
  strip, and a status bar with segments and a storage gauge. With nothing chosen, the details panel and the
  Status tab show the network's card: the Mainframe, the storage used of what the servers hold, the counts,
  and what is running.

### Changed
- The computing mod is J's Computers. It was J's Computronics, and Computronics is the name of a mod
  that already exists. The mod id stays `jsc`, so nothing a world holds changes name.
- The Task Manager ends a program you wrote, as it ends a window. What the machine itself is made of still
  cannot be ended, because that is the machine and not something you started.
- The network knows how much it could hold, not only how much it does: a server offers its drives, a
  personal computer the share its owner published.
- Work asked for through IQL says so in the network's log. It was reading as the shell's.
- The desktop's open-program counter became a memory meter, "used/total MB", in every desktop's panel.
- Frames XP wears its own shell again: the Start pill with its flag, task buttons carrying each program's
  icon and showing the window in front as pushed in, a Start menu that opens on the player's own face and
  name, and a wallpaper with clouds over its hill.
- Desktop icons sit on a grid wide enough for their names, wrapped over two lines and written in the smaller
  text, so a long one no longer runs across the icon beside it and the desktop keeps its room.
- Task buttons share the strip out between them instead of each keeping a fixed width, so the open programs
  stay visible however many there are.
- An Operation's provenance rows name the computer that asked and the program it asked through, as in
  "lab-pc (Interactor)"; a bus reads as its kind and its name.
- Installing scales with the machine: the same program takes twice as long on each older generation of
  hardware, so a Vintage machine reading a floppy really waits and a modern one reading the network
  hardly does.
- `apt`, `dnf`, `pacman` and `pckmgr` speak as they do: a package is fetched, unpacked and set up with
  its own version, and the progress bar grows in place on one line instead of printing a new line each
  time. Removing goes the same way.
- Setup on a Linux machine happens at the prompt only; the Setup window is a Frames thing.
- A file whose extension belongs to no language is opened as text, not compiled: an assembly listing is
  read, not built.
- `cannonc` says how to run what it wrote. Compiling and running are two commands, and the prompt now
  names the second one after the first has finished.
- Virtual Studio Code's side panel is the folder and nothing else: the folder's name, then its tree,
  with no caption over it and no list of open editors above it, since the tabs already say what is
  open. A file or a folder in the tree opens on a double click; one click only picks it.

### Removed
- The one-line console at the bottom of the Network Interactor. The window does by itself what it was there
  for, and the Command Prompt is the terminal, with a history.

### Fixed
- The installation disc's setup no longer sits in the local disk's root, and the installed program's
  files are where the shortcut says they are.
- Two open terminals no longer mirror each other's output and folder.
- Virtual Studio's menus open and close on the click; the File menu no longer stays open with the others
  dead beside it.
- The name of a new file starts with the cursor before the extension, and the arrow keys move it.
- The "Open with Virtual Studio" entry of the explorer's menu is one entry, not a row of them.
- Errors are listed in a table with room for the description instead of running into each other.
- Backspace on the indented line under an opening brace no longer pulls the closing brace up onto the
  line above: a space before the caret and the end of a line after it were being taken for a pair.
- The editors read the whole folder when they mark errors, so a class that extends one written in the
  file beside it is no longer told that class does not exist.
- Two methods with the same name and the same parameters are refused as one declared twice.
- Typing a B with Ctrl held at a computer no longer switches the game's narrator on and off: the game
  saw no text box and took the key for itself.
- A running program with a long name no longer drops the player's connection when the machine's
  settings are read: every name in that message is cut to what the message can carry.
- A program with a struct or a record in it runs. The compiler wrote them into the listing under
  their own words and the machine did not know those words, so it said the listing was not something
  Cannon could run.
- A record joined to a string reads as its fields, `Item { Name = iron, Qty = 3 }`, the way its own
  `ToString` writes it, rather than as the name of its type. Any class with a `ToString` of its own
  reads that way in a sentence.
- A click on the empty end of an editor's tab strip no longer closes the last tab. It counted as that
  tab's close mark, so a few stray clicks there emptied the editor.
- Vim and Emacs open the file that was named. A file named from inside its folder (`vim Program.can`
  after `cd progs`) was asked for by its bare name, so the editor opened an empty file of that name
  instead of the one on the disk.
- Emacs takes a command after `M-x`. The letters typed after it were run into it (`M-xcompile`), so
  `M-x compile` and `C-x u` could never be read.
- A terminal program stopped on a read no longer stays on the machine's list for ever when it loses
  the terminal. Only the program in front of the terminal gets what is typed, so one that was left
  behind by another program taking the terminal could never be answered; it sat at no cost and some
  memory, listed as running. It is stopped the moment the terminal moves on, and a machine finding one
  in that state clears it.
- A Cluster Management Computer can be renamed from its own screen. The name typed there was refused.

### Security
- A computer acts only on what a player at its screen sends. A modified client could put any position
  in a request and reach a machine it had never opened. Every request from a client is now checked on
  the server against the screen that player really has open: a desktop, command prompt or terminal on
  that machine within reach, a firmware, self-test, installer or KVM screen the server opened on a
  monitor still showing the machine, or the assembly screen of the block being changed. A request that
  fails the check is dropped and written to the server log.

## [0.2.0a] - 2026-09-06

The mod became a series. The code every module shares now lives in a library mod of its own, J's Core,
and the industrial machines in their own mod, J's Industrial; J's Computers keeps the computers, the
network and the programs. The three ship together at one version. Worlds from 0.1.0a do not carry over:
the industrial blocks and the material items changed their ids.

### Added
- J's Core (`jscore`), the library every mod of the series requires: the hardware eras and the industrial
  tiers, the material catalogue, the data network, the Operations framework, energy, peripheral links,
  multiblocks, configuration, the event bus, persistence, the unit formatter and the screen toolkit.
- J's Industrial (`jsindustrial`), the machines and their recipes, with a creative tab of their own; the
  Pattern Studio pairs its recipes with the Macerator, the Compressor and the Electric Furnace.
- Priorities for network Operations: a level from LOW to HIGH, chosen in the Network Interactor's request
  and craft dialogs or with an IQL `PRIORITY` clause, and changed on a live Operation from the Task Manager.
  A queued Operation gains a level while it waits, so none starves.
- Cancelling an Operation in flight, from the Task Manager or with `cancel <id>` at the prompt; `ops` now
  lists each Operation's id.
- The time every Operation waited and ran on its log entry, the last hour's statistics per type in a Stats
  tab of the Network Manager, and a `stats` prompt command.
- One server config for the engine's balance, `jstech-balance.toml`: the disk latencies, the waiting
  timeout, the priority aging period, the Subframe share and the orphan expiry, clamped on load.
- A registry of Operation types and lifecycle events on the core's event bus, for mods that follow or drive
  the network's work.
- A README for the series and one per mod, and a page on the interface components the programs are built
  from.

### Changed
- Every desktop program and dialog draws from one set of interface components; the desktop's error dialog
  is one of them.
- Subframes lend their share of capacity and their GPUs' queues to the Mainframe orchestrating them.
- A REINDEX reads the disks on its tick and rebuilds the catalog off it; a craft request is listed as
  pending while its plan is made off the tick.
- Saved Operations left unresumed for longer than the orphan expiry are discarded on reload instead of
  resumed.
- The server config `jsc-server.toml` is replaced by `jstech-balance.toml`.

### Fixed
- A pull into a computer that was broken mid-transfer no longer keeps taking items out of the network.

## [0.1.0a] - 2026-09-05

The first numbered version. Everything before it was the unversioned groundwork of the `0.0.x` line.
The mod is in alpha: computing is the only module, and it is still growing.

### Added
- Hardware eras (Vintage, Legacy, Standard) for computers, monitors and drives, with 3D cabinets for
  the mainframes, the server racks and the USB flash drive.
- Removable media (floppies, CDs, DVDs and USB sticks), the drives that read them, and install media
  for systems and programs; This PC installs a program straight from an inserted disc.
- The Frames desktops (95, XP and 11), Linux systems with a boot manager, dual boot, package managers
  and three desktop environments, and the programs that ship with them: Command Prompt, Files, Editor,
  This PC, Settings, System Monitor, Calculator, Network Manager and the Network Interactor; the
  Crafting Manager, the Craft Planner, Storage Insights, the Automation Manager and Minesweeper install
  from their own media.
- Servers that mount in racks, one rack per era, the Supercomputer rack on its HPC fabric, and the
  Cluster Management Computer with its Cluster Manager program.
- Drive contents kept as storage volumes in their own save data, with disks sized by era.
- Machine autocrafting: the Crafting Computer, the crafting cable and switch, input, output and
  receiving buses, machine patterns, multi-stage recipes, parallel stages and crafting-card threads.
- Chemicals and fluids as network data, carried by the buses; Mekanism machines and the Fusion Reactor
  can be fed and driven through the network, including D-T fuel made from network gases.
- Crafting from the command line and from IQL through the same planner the graphical terminal uses.
- The Pattern Studio, where recipes are authored on the computer, and the Pattern Encoder, one per
  era, which burns them onto media.
- JEI: the ingredient list sits beside every monitor screen, and a recipe transfers into the Pattern
  Studio's bench or machine draft.
- Every system and program is credited to its software house, and tooltips colour their era.
- A contribution policy for automated tooling and a public documentation folder.

### Changed
- Requires NeoForge 21.1.248 or newer.

### Fixed
- The idle tick cost of big bases and the autosave freeze their drives caused.
- Desktop windows stay current after a restore.
- The Crafting Manager sees a disc inserted after its window opened.
- Breaking a drive that still holds a disc removes the drive.
- A rack server's desktop no longer crashes the monitor before the rack's era has reached the client.

[Unreleased]: https://github.com/jvpts11/js-tech-series/compare/v0.3.0a...HEAD
[0.3.0a]: https://github.com/jvpts11/js-tech-series/releases/tag/v0.3.0a
[0.2.0a]: https://github.com/jvpts11/js-tech-series/releases/tag/v0.2.0a
[0.1.0a]: https://github.com/jvpts11/js-tech-series/releases/tag/v0.1.0a
