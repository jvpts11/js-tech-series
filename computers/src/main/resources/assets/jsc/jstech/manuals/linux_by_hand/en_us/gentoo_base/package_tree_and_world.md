---
section: gentoo_base
order: 322
---
# The package tree, the profile and @world

Portage installs programs from recipes. The package tree is the collection of those recipes, one for each program.

> [!DEFINE] ebuild
> The build recipe of one program: where to fetch its source, and how to build it.

The tree comes from a dated snapshot of the repository. Portage checks the snapshot against its checksum and its GNU Privacy Guard (GPG) signature. You get the tree with emerge-webrsync, which downloads the snapshot, or with emerge --sync, which copies it over rsync. Either one works with no tree yet.

1. Inside the new system, type emerge-webrsync and press [[Enter]].
2. Type eselect profile list and press [[Enter]] to see the profiles.
3. Type eselect profile set, then the number you want, and press [[Enter]].
4. Type emerge --ask --verbose --update --deep --newuse @world and press [[Enter]].

> [!DEFINE] profile
> A named set of default build choices. It says how programs are built, and it installs nothing.

The profile sets the defaults that every build follows. Choose one from this table.

Table: The profiles

| Number | Profile |
| --- | --- |
| 1 | `default/linux/vel64/23.0` |
| 2 | `default/linux/vel64/23.0/systemd` |
| 3 | `default/linux/vel64/23.0/desktop` |
| 4 | `default/linux/vel64/23.0/desktop/gnome` |
| 5 | `default/linux/vel64/23.0/desktop/plasma` |
| 6 | `default/linux/vel64/23.0/no-multilib` |
| 7 | `default/linux/vel64/23.0/hardened` |

Choose a profile with eselect profile set, even when profile 1 suits you.

> [!DEFINE] @world
> The set of packages the system wants. Updating it brings each one up to date.

The update command compiles every package of the world that is out of date. Each package has USE flags, which are switches that turn its optional features on or off.

> [!DEFINE] USE flag
> A switch that turns an optional feature of a program on or off when it is built.

Table: The packages of the world

| Package and version | Archive and size | USE flags |
| --- | --- | --- |
| `sys-libs/ncurses 6.5_p20250125` | ncurses-6.5.tar.gz, 3.6 MB; replaces 6.4_p20240414 | On: cxx, tinfo. Off: debug, doc. |
| `app-shells/bash 5.2_p37` | bash-5.2.tar.gz, 10.7 MB; replaces 5.2_p26 | On: net, nls, readline. Off: afs, plugins. |
| `app-editors/vim 9.1.0794` | vim-9.1.0794.tar.gz, 16.8 MB; replaces nothing | On: acl, crypt, nls. Off: X, lua, perl. |

With --ask, Portage stops and asks: Would you like to merge these packages? [Yes/No]. An answer that starts with n quits, and the screen says Quitting. Any other answer carries on, Enter included.

Each package then goes through its phases. It downloads its archive from the Mirror, unpacks the source, prepares it, configures it, compiles it, installs it and merges it into the system.

## If something goes wrong

> [!PROBLEM] emerge says "The portage tree is empty".
> Run emerge-webrsync, or emerge --sync, before you merge anything.

> [!PROBLEM] emerge says "Could not resolve mirror://mainframe".
> No Mainframe running the [Mirror](jsc:mirror) is on this network. Connect to one, then try again.

> [!PROBLEM] emerge says "command not found" when run outside the new system, since the package manager is the new system's.
> Step into the new system first: chroot /mnt/gentoo /bin/bash.

> [!PROBLEM] emerge says "No packages given."
> Type the name of at least one package after emerge.

> [!PROBLEM] emerge says there are no ebuilds to satisfy the name you typed.
> Check the spelling. The name must be a program the Mirror has ([](jsc:building_from_source)).

> [!PROBLEM] eselect says that the target does not appear to be valid.
> Type a number from the list that eselect profile list shows.

> [!SEE] jsc:make_conf, jsc:kernel_gentoo, jsc:building_from_source
