---
section: gentoo_base
order: 319
---
# The stage 3 archive

A stage 3 is a ready-made archive of a base Gentoo system: the basic files, the tools and the libraries, before the kernel and before any program you chose. You download it and unpack it into the new system's disk, so you do not build the base from source yourself.

> [!DEFINE] stage 3
> A tar archive of a base Gentoo system, ready to unpack in place of building it.

> [!DEFINE] make.conf
> The file of build settings that Portage reads before it compiles a program.

Its name tells you what it holds. In stage3-vel64-openrc.tar.xz, vel64 is the processor architecture, and openrc is the init system, the program that starts the services at boot.

The disk must be mounted first ([](jsc:mounts_and_chroot)). The download saves the file in the folder you stand in, and the live medium starts you in /root, so you move to /mnt/gentoo before you download.

## Downloading and unpacking

1. Type cd /mnt/gentoo and press [[Enter]].
2. Type wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz and press [[Enter]].
3. Type tar xpvf stage3-\*.tar.xz --xattrs-include='\*.\*' and press [[Enter]].
4. Wait for the prompt to come back. Unpacking takes longer on a slow disk.

```session
{"lines": [{"prompt": "livecd /mnt/gentoo # ", "text": "wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz", "typed": true}, {"text": "--<date>--  mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz"}, {"text": "Resolving mainframe... done."}, {"text": "Connecting to mainframe... connected."}, {"text": "Mirror request sent, awaiting response... 200 OK"}], "look": "jsc:guide/terminal/bash", "title": "Gentoo"}
```

The first line of the answer carries the date. A progress bar then fills while the archive downloads, and the screen says when the file is saved.

The letters after tar matter. x unpacks the archive, p keeps the file permissions, and f names the archive that follows. The v letter prints each path as it is unpacked. Without v, the screen stays quiet until the unpacking ends. --xattrs-include keeps the extra file information that some programs need.

When the unpacking ends, the new system has its folders, among them /etc, /boot, /usr and /var. It also has two starting files: /etc/portage/make.conf, and an /etc/fstab that holds only a header. Unpacking the archive again undoes every later step, so the checklist asks for them again.

## If something goes wrong

> [!PROBLEM] The download says "Name or service not known".
> No Mainframe running the Mirror is on this network, or the address does not begin with mirror://mainframe.

> [!PROBLEM] The download says "ERROR 404: Not Found."
> The address must end in /gentoo/stage3-vel64-openrc.tar.xz, spelled exactly like that.

> [!PROBLEM] tar says "Cowardly refusing to create an empty archive".
> The command names no stage 3 file. Type the whole command, with stage3 in the file name.

> [!PROBLEM] tar says "Cannot open: No such file or directory".
> The archive is in /root, the folder you started in. Type cd /mnt/gentoo, then download the stage 3 again there.

> [!PROBLEM] tar says "refusing to unpack a whole system over the live medium".
> The disk is not mounted at /mnt/gentoo, or you are not in that folder. Mount it ([](jsc:mounts_and_chroot)), then type cd /mnt/gentoo.

> [!SEE] jsc:mounts_and_chroot, jsc:make_conf, jsc:installing
