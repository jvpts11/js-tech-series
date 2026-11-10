/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a system a player wrote has said while it runs on a machine (computers/docs/design/player-systems.md):
 * the picture of its coming up, the look of its panic page, the services it registered and started, the desktop it
 * started, the program the input goes to, and its text screen.
 *
 * <p>Kept with the machine, so a world closed and opened again finds the system as it was. A fresh start of the machine
 * starts it fresh.
 */
public final class SystemRun {

    private final SystemScreen screen = new SystemScreen();
    private final Map<String, String> registered = new LinkedHashMap<>();
    private final Map<String, Integer> started = new LinkedHashMap<>();
    /* What has been typed for a program the system handed the input to, until Enter sends it. */
    private final StringBuilder typed = new StringBuilder();
    private String logo = "";
    private String line = "";
    private int progress;
    private boolean done;
    private String page = OWN;
    private String background = "";
    private String text = "";
    private int rebootAfter = NO_REBOOT;
    private String halted = "";
    private List<String> trace = List.of();
    @Nullable
    private ResourceLocation desktop;
    private int focus = SYSTEM_PID;
    private boolean pictureChanged;
    private boolean screenChanged;

    /** The machine's process 1, which the system is. */
    public static final int SYSTEM_PID = 1;
    /** A panic page that stays until the machine is restarted. */
    public static final int NO_REBOOT = -1;
    /** The pages a system's panic may come down on: the mod's blue, red or kernel-panic text, or its own. */
    public static final List<String> PAGES = List.of("blue", "red", "console", "own");
    /** The page a system's panic comes down on unless it chooses another: its own. */
    public static final String OWN = "own";
    /** The longest line a program the system handed the input to is typed at once. */
    public static final int MOST_TYPED = 256;

    /** Forgets everything the last run said, as a machine starting the system again does. */
    public void reset() {
        this.screen.colours("lightgray", "black");
        this.screen.clear();
        this.registered.clear();
        this.started.clear();
        this.logo = "";
        this.line = "";
        this.progress = 0;
        this.done = false;
        this.page = OWN;
        this.background = "";
        this.text = "";
        this.rebootAfter = NO_REBOOT;
        this.halted = "";
        this.trace = List.of();
        this.desktop = null;
        this.focus = SYSTEM_PID;
        this.pictureChanged = true;
        this.screenChanged = true;
    }

    public SystemScreen screen() {
        return this.screen;
    }

    /** Marks the text screen as changed, for whoever watches to be sent it. */
    public void screenChanged() {
        this.screenChanged = true;
    }

    /** Whether the text screen changed since it was last sent; asking forgets it. */
    public boolean takeScreenChange() {
        final boolean was = this.screenChanged;
        this.screenChanged = false;
        return was;
    }

    /** Whether the picture of the system coming up changed since it was last sent; asking forgets it. */
    public boolean takePictureChange() {
        final boolean was = this.pictureChanged;
        this.pictureChanged = false;
        return was;
    }

    public String logo() {
        return this.logo;
    }

    public void logo(final String path) {
        this.logo = path == null ? "" : path;
        this.pictureChanged = true;
    }

    public String line() {
        return this.line;
    }

    public void line(final String said) {
        this.line = said == null ? "" : said;
        this.pictureChanged = true;
    }

    public int progress() {
        return this.progress;
    }

    public void progress(final int percent) {
        this.progress = Math.max(0, Math.min(100, percent));
        this.pictureChanged = true;
    }

    /** Whether the system said it is up. */
    public boolean done() {
        return this.done;
    }

    public void finish() {
        this.done = true;
        this.progress = 100;
        this.pictureChanged = true;
    }

    public String page() {
        return this.page;
    }

    public void page(final String kind) {
        this.page = kind;
    }

    /** The colour behind the system's own pages, "#RRGGBB" or a name, or empty for the mod's own. */
    public String background() {
        return this.background;
    }

    /** The colour of the text on the system's own pages, or empty for the mod's own. */
    public String text() {
        return this.text;
    }

    public void colours(final String ground, final String writing) {
        this.background = ground;
        this.text = writing;
        this.page = OWN;
        this.pictureChanged = true;
    }

    public int rebootAfter() {
        return this.rebootAfter;
    }

    public void rebootAfter(final int seconds) {
        this.rebootAfter = Math.max(NO_REBOOT, seconds);
    }

    /** The message the system went down with, or empty while it runs. */
    public String halted() {
        return this.halted;
    }

    /** The error under the message: the fault and the calls it came through, with their files and lines. */
    public List<String> trace() {
        return this.trace;
    }

    public void halt(final String message, final List<String> lines) {
        this.halted = message == null || message.isEmpty() ? " " : message;
        this.trace = List.copyOf(lines);
    }

    /** The services the system registered, by name, each with the program it runs. */
    public Map<String, String> registered() {
        return this.registered;
    }

    /** The services up, by name, each with its process. */
    public Map<String, Integer> started() {
        return this.started;
    }

    @Nullable
    public ResourceLocation desktop() {
        return this.desktop;
    }

    public void desktop(@Nullable final ResourceLocation id) {
        this.desktop = id;
    }

    /** The process the input from the monitor goes to: the system's own, or a program it handed it to. */
    public int focus() {
        return this.focus;
    }

    public void focus(final int pid) {
        this.focus = pid;
        this.typed.setLength(0);
    }

    /** Keeps what was typed for the program the input goes to, held to a line's worth. */
    public void type(final String characters) {
        if (this.typed.length() + characters.length() <= MOST_TYPED) {
            this.typed.append(characters);
        }
    }

    /** The line typed so far, which Enter hands over; the next one starts empty. */
    public String takeLine() {
        final String line = this.typed.toString();
        this.typed.setLength(0);
        return line;
    }

    public void save(final CompoundTag tag) {
        tag.putString("Logo", this.logo);
        tag.putString("Line", this.line);
        tag.putInt("Progress", this.progress);
        tag.putBoolean("Done", this.done);
        tag.putString("Page", this.page);
        tag.putString("Background", this.background);
        tag.putString("Text", this.text);
        tag.putInt("RebootAfter", this.rebootAfter);
        tag.putString("Halted", this.halted);
        tag.put("Trace", strings(this.trace));
        if (this.desktop != null) {
            tag.putString("Desktop", this.desktop.toString());
        }
        tag.putInt("Focus", this.focus);
        final CompoundTag services = new CompoundTag();
        this.registered.forEach(services::putString);
        tag.put("Registered", services);
        final CompoundTag up = new CompoundTag();
        this.started.forEach(up::putInt);
        tag.put("Started", up);
        tag.put("Rows", strings(this.screen.rows()));
        tag.putIntArray("Fore", this.screen.foregrounds());
        tag.putIntArray("Back", this.screen.backgrounds());
        tag.putIntArray("Cursor", this.screen.cursorAt());
    }

    public void load(final CompoundTag tag) {
        this.reset();
        this.logo = tag.getString("Logo");
        this.line = tag.getString("Line");
        this.progress = tag.getInt("Progress");
        this.done = tag.getBoolean("Done");
        this.page = tag.contains("Page") ? tag.getString("Page") : OWN;
        this.background = tag.getString("Background");
        this.text = tag.getString("Text");
        this.rebootAfter = tag.contains("RebootAfter") ? tag.getInt("RebootAfter") : NO_REBOOT;
        this.halted = tag.getString("Halted");
        this.trace = read(tag.getList("Trace", Tag.TAG_STRING));
        this.desktop = tag.contains("Desktop") ? ResourceLocation.tryParse(tag.getString("Desktop")) : null;
        this.focus = tag.contains("Focus") ? tag.getInt("Focus") : SYSTEM_PID;
        final CompoundTag services = tag.getCompound("Registered");
        for (final String name : services.getAllKeys()) {
            this.registered.put(name, services.getString(name));
        }
        final CompoundTag up = tag.getCompound("Started");
        for (final String name : up.getAllKeys()) {
            this.started.put(name, up.getInt(name));
        }
        this.screen.restore(read(tag.getList("Rows", Tag.TAG_STRING)), tag.getIntArray("Fore"),
                tag.getIntArray("Back"), tag.getIntArray("Cursor"));
    }

    private static ListTag strings(final List<String> lines) {
        final ListTag list = new ListTag();
        for (final String one : lines) {
            list.add(StringTag.valueOf(one));
        }
        return list;
    }

    private static List<String> read(final ListTag list) {
        final List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }
}
