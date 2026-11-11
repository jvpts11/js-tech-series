---
section: arch_base
order: 309
icon: jsc:cd_rom
---
# Installing the base system (pacstrap)

The base system is the core of Arch Linux: the kernel ([](jsc:kernel_gentoo)), the core tools and the package manager, pacman. The command that installs it is pacstrap. It puts the packages on the disk, under /mnt, the folder where the new system is built.

Pacstrap is the first step that writes the new system, so the disk must be ready before it runs. The root partition has to be mounted on /mnt ([](jsc:mounting)).

On a real Arch install, pacstrap downloads the packages from the internet. Here the packages come from the Mirror, the package server of your network. A Mainframe must run it, and this computer must be on that network. Without a Mirror, pacstrap cannot start.

> [!DEFINE] pacman
> The package manager of Arch Linux: it finds, downloads and installs packages.

> [!DEFINE] Mirror
> The package server of a network. A Mainframe that runs it serves packages to every computer of the network.

1. Make sure the root partition is mounted on /mnt.
2. Type pacstrap -K /mnt base linux linux-firmware and press [[Enter]].
3. Wait until the prompt comes back. The step takes as long as your connection and your disk need.

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "pacstrap -K /mnt base linux linux-firmware", "typed": true}, {"text": "==> Creating install root at /mnt"}, {"text": "==> Installing packages to /mnt"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Then come the download bars, the checks of the packages, one line for each package it installs, and the last steps of the install. Pacstrap asks you nothing: it does not stop to ask you to confirm, and it installs straight away.

```table
{"caption": "What the base system brings", "rows": [[{"words": "Packages installed"}, {"words": "48"}], [{"words": "Kernel release"}, {"words": "6.11.5-arch1-1"}]]}
```

The 48 packages include base, linux, linux-firmware, pacman, systemd, bash, coreutils and mkinitcpio. The last steps of the install also build both boot images, so you do not need to build them again.

When pacstrap finishes, the new system has its folders for /etc, /boot, /root, /usr, /var, /proc, /sys, /dev and /run. Its kernel and its two boot images ([](jsc:arch_mkinitcpio)) are in /boot. The step is recorded as done only at that moment.

Running pacstrap again starts over. The steps you did before, such as the file system table and the boot steps, are forgotten, and you do them again. The clock setting is kept.

You can stop pacstrap with [[Ctrl]]+[[C]].

## If something goes wrong

> [!PROBLEM] The screen says "==> ERROR: /mnt is not a mountpoint!".
> Nothing is mounted on /mnt. Mount the root partition there, after its file system is made ([](jsc:mounting)): mount /dev/sda2 /mnt.

> [!PROBLEM] The screen says "error: failed retrieving file 'core.db' from mirror://mainframe : Could not resolve host: mainframe", and then "error: failed to synchronize all databases (unexpected error)".
> This computer cannot reach a Mirror. Put it on a network whose Mainframe runs the Mirror ([](jsc:mirror)).

> [!PROBLEM] You stopped pacstrap with [[Ctrl]]+[[C]] before it finished.
> The step is not recorded as done. Run pacstrap again from the start.

> [!SEE] jsc:arch_fstab
