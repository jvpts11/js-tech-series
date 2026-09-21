/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.TtyScript;
import java.util.ArrayList;
import java.util.List;

/**
 * What the archiver says while it lays a whole system out over a disk: every path in it, one after another.
 *
 * <p>Asked to work quietly it says nothing at all, which is what the real one does and why the handbook asks
 * it not to: a screenful of paths going past is the only sign that a step with no bar and a good while of
 * work in it is doing anything. The paths are those of a real base system, in the order an archive of one
 * holds them, the root first, then each directory and what is in it.
 */
public final class ArchiveVoice {

    private static final String[] ETC = {"DIR_COLORS", "bash/", "bash/bashrc", "bash/bash_logout", "conf.d/",
        "conf.d/consolefont", "conf.d/hostname", "conf.d/hwclock", "conf.d/keymaps", "conf.d/modules", "conf.d/net",
        "csh.cshrc", "env.d/", "env.d/00basic", "env.d/50baselayout", "env.d/scc/", "environment", "fstab",
        "gai.conf", "group", "gshadow", "host.conf", "hosts", "init.d/", "init.d/bootmisc", "init.d/consolefont",
        "init.d/devfs", "init.d/dmesg", "init.d/fsck", "init.d/hostname", "init.d/hwclock", "init.d/keymaps",
        "init.d/killprocs", "init.d/local", "init.d/localmount", "init.d/modules", "init.d/mount-ro",
        "init.d/net.lo", "init.d/netmount", "init.d/procfs", "init.d/root", "init.d/savecache", "init.d/swap",
        "init.d/sysctl", "init.d/sysfs", "init.d/termencoding", "init.d/udev", "init.d/urandom", "inittab",
        "inputrc", "issue", "ld.so.conf", "ld.so.conf.d/", "locale.gen", "login.defs", "modprobe.d/",
        "modprobe.d/aliases.conf", "nsswitch.conf", "openrc/", "os-release", "pam.d/", "pam.d/login",
        "pam.d/passwd", "pam.d/su", "pam.d/system-auth", "passwd", "portage/", "portage/make.conf",
        "portage/make.profile", "portage/package.use/", "portage/repos.conf/", "profile", "profile.d/",
        "protocols", "rc.conf", "resolv.conf", "securetty", "services", "shadow", "shells", "skel/",
        "skel/.bash_logout", "skel/.bash_profile", "skel/.bashrc", "sysctl.conf", "sysctl.d/", "timezone",
        "udev/", "udev/udev.conf", "xattr.conf"};

    private static final String[] BINARIES = ("awk basename bash bunzip2 bzip2 cat chgrp chmod chown chroot cmp cp "
            + "cut date dd df diff dirname dmesg du echo ed egrep emerge env eselect expr false fgrep find free "
            + "gawk getent grep groups gunzip gzip head hostname id install kill killall less ln locale login ls "
            + "lsblk make md5sum mkdir mknod mktemp more mount mv nano nice nproc od passwd paste patch perl pgrep "
            + "ping pkill portageq printf ps pwd python3.12 readlink realpath rm rmdir rsync scc sed seq sh sha256sum "
            + "sleep sort split stat strings su sync tail tar tee test top touch tr true tty umount uname uniq "
            + "uptime vi wc wget which whoami xargs xz yes zcat").split(" ");

    private static final String[] LIBRARIES = ("ld-linux-x86-64.so.2 libacl.so.1 libattr.so.1 libblkid.so.1 "
            + "libbz2.so.1 libc.so.6 libcap.so.2 libcrypt.so.2 libcrypto.so.3 libdl.so.2 libffi.so.8 libgmp.so.10 "
            + "libkmod.so.2 liblzma.so.5 libm.so.6 libmagic.so.1 libmount.so.1 libncursesw.so.6 libpam.so.0 "
            + "libpcre2-8.so.0 libpthread.so.0 libreadline.so.8 libresolv.so.2 librt.so.1 libssl.so.3 "
            + "libtinfo.so.6 libudev.so.1 libuuid.so.1 libz.so.1 libzstd.so.1").split(" ");

    private static final String[] PYTHON = ("abc argparse ast base64 bisect calendar codecs configparser contextlib "
            + "copy csv datetime decimal difflib enum fnmatch functools getopt gettext glob gzip hashlib heapq "
            + "inspect io ipaddress keyword linecache locale lzma mimetypes numbers operator optparse os pathlib "
            + "pickle platform posixpath pprint queue random reprlib selectors shlex shutil signal socket ssl stat "
            + "string struct subprocess sysconfig tarfile tempfile textwrap threading tokenize traceback types "
            + "typing uuid warnings weakref zipfile").split(" ");

    private static final String[] LOCALES = ("bg ca cs da de el en_GB eo es et fi fr ga gl he hr hu id it ja ko lt "
            + "nb nl pl pt pt_BR ro ru sk sl sr sv tr uk vi zh_CN zh_TW").split(" ");

    private static final String[] CATALOGUES = {"bash.mo", "coreutils.mo", "findutils.mo", "gawk.mo", "grep.mo",
        "sed.mo", "tar.mo", "util-linux.mo"};

    /** Every path, made once: it is the same archive every time, and nobody needs it made twice. */
    private static final List<String> PATHS = paths();

    private ArchiveVoice() {
    }

    /**
     * Unpacking the archive of a base system.
     *
     * @param naming   whether it was asked to name what it lays out
     * @param ticks    how long this disk takes over it
     * @param unpacked what having it unpacked means to the machine, done at the end and not before
     */
    public static TtyScript unpack(final boolean naming, final int ticks, final Runnable unpacked) {
        final TtyScript.Builder script = TtyScript.script();
        if (naming) {
            script.flood(ticks, PATHS.size(), index -> CliLine.plain(PATHS.get(index)));
        } else {
            script.pause(ticks);
        }
        return script.effect(unpacked).done();
    }

    /** How many paths the archive names, which is how many lines a full unpack comes to. */
    public static int pathCount() {
        return PATHS.size();
    }

    private static List<String> paths() {
        final List<String> out = new ArrayList<>(4096);
        out.addAll(List.of("./", "./bin", "./boot/", "./dev/", "./dev/console", "./dev/null", "./etc/"));
        for (final String each : ETC) {
            out.add("./etc/" + each);
        }
        out.addAll(List.of("./home/", "./lib", "./lib64/", "./media/", "./mnt/", "./opt/", "./proc/", "./root/",
                "./run/", "./sbin", "./sys/", "./tmp/", "./usr/", "./usr/bin/"));
        for (final String each : BINARIES) {
            out.add("./usr/bin/" + each);
        }
        /* The compiler a base system carries is this world's: Sigma's, with a front end for each of its two. */
        out.addAll(List.of("./usr/lib/", "./usr/lib/scc/", "./usr/lib/scc/x86_64-pc-linux-gnu/",
                "./usr/lib/scc/x86_64-pc-linux-gnu/14/"));
        for (final String each : new String[]{"sg1", "sgs1", "collect2", "crtbegin.asm", "crtend.asm", "libscc.a",
            "libscc_s.so", "libsigma.so.6.0.33", "lto1", "lto-wrapper"}) {
            out.add("./usr/lib/scc/x86_64-pc-linux-gnu/14/" + each);
        }
        out.add("./usr/lib/python3.12/");
        for (final String each : PYTHON) {
            out.add("./usr/lib/python3.12/" + each + ".py");
        }
        out.add("./usr/lib/python3.12/__pycache__/");
        for (final String each : PYTHON) {
            out.add("./usr/lib/python3.12/__pycache__/" + each + ".cpython-312.pyc");
        }
        out.add("./usr/lib64/");
        for (final String each : LIBRARIES) {
            out.add("./usr/lib64/" + each);
        }
        out.addAll(List.of("./usr/share/", "./usr/share/locale/"));
        for (final String locale : LOCALES) {
            out.add("./usr/share/locale/" + locale + "/");
            out.add("./usr/share/locale/" + locale + "/LC_MESSAGES/");
            for (final String catalogue : CATALOGUES) {
                out.add("./usr/share/locale/" + locale + "/LC_MESSAGES/" + catalogue);
            }
        }
        out.addAll(List.of("./usr/share/man/", "./usr/share/man/man1/"));
        for (final String each : BINARIES) {
            out.add("./usr/share/man/man1/" + each + ".1.bz2");
        }
        out.addAll(List.of("./usr/src/", "./var/", "./var/cache/", "./var/db/", "./var/db/pkg/", "./var/empty/",
                "./var/lib/", "./var/lib/portage/", "./var/lib/portage/world", "./var/log/", "./var/spool/",
                "./var/tmp/"));
        return List.copyOf(out);
    }
}
