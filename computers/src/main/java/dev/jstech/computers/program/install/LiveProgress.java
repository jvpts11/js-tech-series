/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.install.voice.PortageVoices;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * How far an installation by hand has got: which of its steps have been done.
 *
 * <p>Each of these is set by a tool when it has finished and not when it was started, so what is recorded
 * here is always what is really on the disk. A fetch stopped half way has fetched nothing, and nothing here
 * says otherwise.
 */
final class LiveProgress {

    /** The packages asked for inside the new system, which belong to the system that comes out of this. */
    private final Set<String> asked = new LinkedHashSet<>();

    /** The archive of a base system is on the disk, ready to unpack. */
    boolean fetched;
    /** The base system is laid out over the disk. */
    boolean base;
    /** The table of what to mount at boot has been written by the tool that writes it. */
    boolean fstab;
    /** The package tree has been fetched. */
    boolean synced;
    /** The number of the profile that was chosen, or zero while it is still the one the base system came with. */
    int profile;
    /** The system has been brought up to date with its tree. */
    boolean worldUpdated;
    /** The kernel sources are merged. */
    boolean sources;
    /** The link the kernel tools follow points at those sources. */
    boolean kernelChosen;
    /** A kernel has been compiled by hand, its modules and the kernel itself not yet installed. */
    boolean kernelCompiled;
    /** A kernel is installed where the bootloader looks for one. */
    boolean kernelBuilt;
    /** The image the kernel is handed at boot has been built. */
    boolean initramfs;
    /** The bootloader's package is installed, which is what puts its installer on the machine at all. */
    boolean grubPackage;
    boolean bootloader;
    /** The bootloader has been given its list of what to start. */
    boolean grubConfig;
    /** The hardware clock has been set from the system's. */
    boolean clockSet;
    /** The name the player gave the machine with the tool for it, empty while they have not. */
    String chosenName = "";

    /** The profile the chooser stars: the one that was chosen, or the first, which is what a base system comes on. */
    int profileInForce() {
        return this.profile == 0 ? 1 : this.profile;
    }

    List<String> askedFor() {
        return List.copyOf(this.asked);
    }

    void ask(final String named) {
        this.asked.add(named);
    }

    /** The base system was made again from nothing, so everything built on the old one went with it. */
    void startOver() {
        this.fetched = false;
        this.base = false;
        this.fstab = false;
        this.synced = false;
        this.sources = false;
        this.kernelCompiled = false;
        this.kernelBuilt = false;
        this.initramfs = false;
        this.grubPackage = false;
        this.bootloader = false;
        this.grubConfig = false;
    }

    void save(final LiveSaved out) {
        out.put("fetched", this.fetched);
        out.put("base", this.base);
        out.put("fstab", this.fstab);
        out.put("synced", this.synced);
        out.put("profile", Integer.toString(this.profile));
        out.put("world_updated", this.worldUpdated);
        out.put("sources", this.sources);
        out.put("kernel_chosen", this.kernelChosen);
        out.put("kernel_compiled", this.kernelCompiled);
        out.put("kernel_built", this.kernelBuilt);
        out.put("initramfs", this.initramfs);
        out.put("grub_package", this.grubPackage);
        out.put("bootloader", this.bootloader);
        out.put("grub_config", this.grubConfig);
        out.put("clock_set", this.clockSet);
        out.put("chosen_name", this.chosenName);
        out.put("asked", String.join(" ", this.asked));
    }

    void load(final LiveSaved saved) {
        this.fetched = saved.flag("fetched");
        this.base = saved.flag("base");
        this.fstab = saved.flag("fstab");
        this.synced = saved.flag("synced");
        this.profile = PortageVoices.profileOf(saved.value("profile", "0"));
        this.worldUpdated = saved.flag("world_updated");
        this.sources = saved.flag("sources");
        this.kernelChosen = saved.flag("kernel_chosen");
        this.kernelCompiled = saved.flag("kernel_compiled");
        this.kernelBuilt = saved.flag("kernel_built");
        this.initramfs = saved.flag("initramfs");
        this.grubPackage = saved.flag("grub_package");
        this.bootloader = saved.flag("bootloader");
        this.grubConfig = saved.flag("grub_config");
        this.clockSet = saved.flag("clock_set");
        this.chosenName = saved.value("chosen_name", "");
        for (final String pkg : saved.value("asked", "").split(" ")) {
            if (!pkg.isEmpty()) {
                this.asked.add(pkg);
            }
        }
    }
}
