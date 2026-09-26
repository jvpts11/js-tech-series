/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.audio.SoundOutput;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The per-computer settings owned by the Settings app and the MC-DOS {@code config} command: the
 * knobs that live on the computer itself (as opposed to the computer name and wallpaper, which stay
 * on {@link ComputerConsoleState}, or the network share, which lives on the disk).
 *
 * <p>This class is pure and carries no Minecraft dependency, so its validation and the
 * {@code config}-command text can be unit-tested without the game. Persistence is done by
 * {@link ComputerConsoleState} reading and writing these plain fields; a live medium (a GPU, monitor,
 * or disk) never appears here.
 *
 * <p>All setters clamp to the documented range rather than rejecting, matching the config policy: an
 * out-of-range value is pinned to the nearest valid one, never accepted blindly.
 */
@TextHolder
public final class ComputerSettings {

    /** The most programs a panel keeps pinned; past that the panel has no room for its windows. */
    public static final int MAX_PINNED = 12;
    /** The most data the Network Interactor keeps starred on one machine. */
    public static final int MAX_FAVOURITES = 64;
    /** The most items whose recipe choice one machine remembers; the oldest choice makes room past that. */
    public static final int MAX_RECIPE_CHOICES = 64;
    /** The most extensions one machine remembers a program for; the oldest choice makes room past that. */
    public static final int MAX_DEFAULT_APPS = 64;
    /** The most folders one machine opens to the others on its network. */
    public static final int MAX_SHARES = 16;

    /** How many names the shell keeps on one machine, which is far more than anybody sets by hand. */
    public static final int MAX_VARIABLES = 64;
    /** What a fresh machine pins: its file explorer. */
    public static final String DEFAULT_PINNED = "files";

    /*
     * The words of the config listing around its keys and values, which are data a player types back. The listing
     * is the machine's own output, so it is written in English, the language a machine keeps what it prints in.
     */
    private static final TextKey DEFAULT_VALUE = TextKey.of("jsc.config.default_value", "%s (default)");
    private static final TextKey STARRED = TextKey.of("jsc.config.starred", "%s starred");
    private static final TextKey READ_WRITE = TextKey.of("jsc.config.read_write", "%s (read and write)");
    private static final TextKey READ_ONLY = TextKey.of("jsc.config.read_only", "%s (read only)");
    private static final TextKey REMOTE_ON =
            TextKey.of("jsc.config.remote_on", "%s (other computers may run programs here)");

    /**
     * One folder this machine opens to the others on its network.
     *
     * @param name     what the others reach it by ({@code \\host\name}): the folder's own name
     * @param path     the folder on this machine, as a DOS path ({@code C:\pub})
     * @param writable whether the others may write into it, or only read
     */
    public record Share(String name, String path, boolean writable) {

        public Share {
            name = name == null ? "" : name.trim();
            path = path == null ? "" : path.trim();
        }
    }

    /** The folders shared, in the order they were shared. */
    private final List<Share> shares = new ArrayList<>();

    /** Accent colour as an ARGB int; {@code 0} means "use the OS skin's default accent". */
    private int accent;
    /** Whether the taskbar clock shows a 12-hour time; default is 24-hour. */
    private boolean clock12h;
    /** GUI scale 1..4, or {@code 0} for automatic. */
    private int guiScale;
    /** Screen brightness 0..100. */
    private int brightness = 100;
    /** How loud the system plays its sound, 0..100; the machine's own noises (its drives, its fans) are not its. */
    private int volume = 100;
    /** Whether the system's sound is muted, which keeps the volume for when it is turned back on. */
    private boolean muted;
    /** Where the system sends its sound. */
    private SoundOutput soundOutput = SoundOutput.BOTH;
    /** The drive letter files save to by default. */
    private char defaultSaveDrive = 'C';
    /** Whether inserting removable media opens its folder automatically. */
    private boolean removableAutoOpen = true;
    /** The chosen theme preset id; {@code ""} means the system default. */
    private String themePreset = "";
    /** Whether the taskbar app strip is centered (Frames 11 look) rather than left-aligned; default centered. */
    private boolean taskbarCentered = true;
    /** Whether the desktop and its programs use the dark theme (Frames 11 only); default light. */
    private boolean darkMode;
    /**
     * The programs pinned to the panel, by program id path ({@code files}, {@code editor}), in the order
     * they were pinned. A fresh machine pins its file explorer, the way every desktop these imitate did.
     */
    private final List<String> pinned = new ArrayList<>(List.of(DEFAULT_PINNED));
    /** Default program id per lowercase file extension (e.g. {@code "txt" -> "jsc:editor"}). */
    private final Map<String, String> defaultApps = new LinkedHashMap<>();
    /**
     * The data the Network Interactor keeps starred, by data id ({@code item|minecraft:iron_ingot}), in the
     * order the player starred it. Kept by the machine, so every window on it shows the same stars.
     */
    private final List<String> favourites = new ArrayList<>();
    /**
     * Which recipe the player picked last for an item that more than one recipe makes, by data id: the index
     * into the recipes the network lists for it. The next craft of that item opens on the same recipe.
     */
    private final Map<String, Integer> recipeChoices = new LinkedHashMap<>();
    /**
     * The names the shell knows on this machine, by name in upper case: what a player set with {@code set} or
     * {@code export}. They belong to the machine rather than to a prompt, so a name set at a monitor is still
     * there in a window on the desktop, in a session opened from another machine, and after a restart.
     */
    private final Map<String, String> variables = new LinkedHashMap<>();

    public int accent() {
        return accent;
    }

    public void setAccent(final int argb) {
        this.accent = argb;
    }

    public boolean clock12h() {
        return clock12h;
    }

    public void setClock12h(final boolean value) {
        this.clock12h = value;
    }

    public int guiScale() {
        return guiScale;
    }

    /**
     * How big the desktop draws everything, as a percentage of its designed size: 100 is that size,
     * anything down to 50 fits more on the glass at the cost of smaller text, and 0 means the default,
     * which the desktop keeps at three quarters.
     */
    public void setGuiScale(final int scale) {
        this.guiScale = scale <= 0 ? 0 : clamp(scale, 50, 100);
    }

    public int brightness() {
        return brightness;
    }

    public void setBrightness(final int value) {
        this.brightness = clamp(value, 0, 100);
    }

    public int volume() {
        return volume;
    }

    public void setVolume(final int value) {
        this.volume = clamp(value, 0, 100);
    }

    public boolean muted() {
        return muted;
    }

    public void setMuted(final boolean value) {
        this.muted = value;
    }

    public SoundOutput soundOutput() {
        return soundOutput;
    }

    public void setSoundOutput(final SoundOutput output) {
        this.soundOutput = output == null ? SoundOutput.BOTH : output;
    }

    /** How loud the system's sound plays, 0 to 1: nothing while it is muted. */
    public float soundLevel() {
        return muted ? 0.0F : volume / 100.0F;
    }

    public char defaultSaveDrive() {
        return defaultSaveDrive;
    }

    public void setDefaultSaveDrive(final char drive) {
        if (Character.isLetter(drive)) {
            this.defaultSaveDrive = Character.toUpperCase(drive);
        }
    }

    public boolean removableAutoOpen() {
        return removableAutoOpen;
    }

    public void setRemovableAutoOpen(final boolean value) {
        this.removableAutoOpen = value;
    }

    /**
     * Whether the other computers on this machine's network may start programs and run commands here.
     * On by default: the network is the player's own, and a machine that says no says so on purpose.
     */
    private boolean remoteAllowed = true;

    public boolean remoteAllowed() {
        return remoteAllowed;
    }

    public void setRemoteAllowed(final boolean value) {
        this.remoteAllowed = value;
    }

    public String themePreset() {
        return themePreset;
    }

    public void setThemePreset(final String preset) {
        this.themePreset = preset == null ? "" : preset;
    }

    public boolean taskbarCentered() {
        return taskbarCentered;
    }

    public void setTaskbarCentered(final boolean value) {
        this.taskbarCentered = value;
    }

    public boolean darkMode() {
        return darkMode;
    }

    public void setDarkMode(final boolean value) {
        this.darkMode = value;
    }

    /** The programs pinned to the panel, by program id path, in the order they were pinned. */
    public List<String> pinned() {
        return Collections.unmodifiableList(pinned);
    }

    /** Replaces the pinned list (used on load): blanks and repeats are dropped, and the list is capped. */
    public void setPinned(final List<String> ids) {
        pinned.clear();
        if (ids != null) {
            for (final String id : ids) {
                pin(id);
            }
        }
    }

    /** Pins a program to the panel, at the end; a program already pinned stays where it is. */
    public boolean pin(final String id) {
        final String key = normalizeId(id);
        if (key.isEmpty() || pinned.contains(key) || pinned.size() >= MAX_PINNED) {
            return false;
        }
        pinned.add(key);
        return true;
    }

    /** Takes a program off the panel; false when it was not pinned. */
    public boolean unpin(final String id) {
        return pinned.remove(normalizeId(id));
    }

    public boolean isPinned(final String id) {
        return pinned.contains(normalizeId(id));
    }

    /** A program id as the pinned list keeps it: its path, lower-case, without a {@code jsc:} namespace. */
    static String normalizeId(final String id) {
        if (id == null) {
            return "";
        }
        final String trimmed = id.trim().toLowerCase(Locale.ROOT);
        final int colon = trimmed.indexOf(':');
        return colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
    }

    /** The starred data ids, in the order they were starred. */
    public List<String> favourites() {
        return Collections.unmodifiableList(favourites);
    }

    /** Replaces the starred list (used on load): blanks and repeats are dropped, and the list is capped. */
    public void setFavourites(final List<String> ids) {
        favourites.clear();
        if (ids != null) {
            for (final String id : ids) {
                favourite(id);
            }
        }
    }

    /** Stars a data id, at the end; one already starred stays where it is. False when there is no room. */
    public boolean favourite(final String id) {
        final String key = id == null ? "" : id.trim();
        if (key.isEmpty() || favourites.contains(key) || favourites.size() >= MAX_FAVOURITES) {
            return false;
        }
        favourites.add(key);
        return true;
    }

    /** Takes the star off a data id; false when it was not starred. */
    public boolean unfavourite(final String id) {
        return favourites.remove(id == null ? "" : id.trim());
    }

    public boolean isFavourite(final String id) {
        return favourites.contains(id == null ? "" : id.trim());
    }

    /** The folders this machine shares, in the order they were shared. */
    public List<Share> shares() {
        return Collections.unmodifiableList(shares);
    }

    /** Replaces the shares (used on load): blanks and repeats are dropped, and the list is capped. */
    public void setShares(final List<Share> given) {
        shares.clear();
        if (given != null) {
            for (final Share share : given) {
                share(share.path(), share.writable());
            }
        }
    }

    /**
     * Shares a folder under its own name; sharing it again changes whether it may be written to. The
     * name is the last part of the path, or the drive letter for a drive's root. False when there is
     * no room for one more.
     */
    public boolean share(final String path, final boolean writable) {
        final String where = path == null ? "" : path.trim();
        final String name = shareNameOf(where);
        if (name.isEmpty()) {
            return false;
        }
        final Share made = new Share(name, where, writable);
        for (int i = 0; i < shares.size(); i++) {
            if (shares.get(i).name().equalsIgnoreCase(name)) {
                shares.set(i, made);
                return true;
            }
        }
        if (shares.size() >= MAX_SHARES) {
            return false;
        }
        shares.add(made);
        return true;
    }

    /** Stops sharing a folder, by its share name or by its path; false when it was not shared. */
    public boolean unshare(final String nameOrPath) {
        final String wanted = nameOrPath == null ? "" : nameOrPath.trim();
        if (wanted.isEmpty()) {
            return false;
        }
        return shares.removeIf(share -> share.name().equalsIgnoreCase(wanted)
                || share.path().equalsIgnoreCase(wanted)
                || share.name().equalsIgnoreCase(shareNameOf(wanted)) && share.path().equalsIgnoreCase(wanted));
    }

    /** The share of that name, or null. */
    public Share shareNamed(final String name) {
        final String wanted = name == null ? "" : name.trim();
        for (final Share share : shares) {
            if (share.name().equalsIgnoreCase(wanted)) {
                return share;
            }
        }
        return null;
    }

    /** What a folder is shared as: its own name, or the drive letter when it is a drive's root. */
    public static String shareNameOf(final String path) {
        String trimmed = path == null ? "" : path.trim();
        while (trimmed.endsWith("\\") || trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return "";
        }
        final int cut = Math.max(trimmed.lastIndexOf('\\'), trimmed.lastIndexOf('/'));
        final String last = cut < 0 ? trimmed : trimmed.substring(cut + 1);
        if (last.length() == 2 && last.charAt(1) == ':' && Character.isLetter(last.charAt(0))) {
            return String.valueOf(Character.toLowerCase(last.charAt(0)));
        }
        return last;
    }

    /** The recipe the player picked last for a data id, or {@code -1} when none was picked. */
    public int recipeChoice(final String id) {
        return recipeChoices.getOrDefault(id == null ? "" : id.trim(), -1);
    }

    /** Remembers the recipe picked for a data id; a negative index forgets it. The oldest choice goes past the cap. */
    public void setRecipeChoice(final String id, final int index) {
        final String key = id == null ? "" : id.trim();
        if (key.isEmpty()) {
            return;
        }
        recipeChoices.remove(key);
        if (index < 0) {
            return;
        }
        while (recipeChoices.size() >= MAX_RECIPE_CHOICES) {
            recipeChoices.remove(recipeChoices.keySet().iterator().next());
        }
        recipeChoices.put(key, index);
    }

    /** An unmodifiable view of the remembered recipe choices, for serialisation. */
    public Map<String, Integer> recipeChoices() {
        return Collections.unmodifiableMap(recipeChoices);
    }

    /** The names the shell knows on this machine, by name in upper case. */
    public Map<String, String> variables() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * Gives {@code name} a value on this machine, or forgets it when the value is blank.
     *
     * <p>The name is kept in upper case because that is how every one of these shells writes an environment
     * name, and it means {@code set path=...} and {@code set PATH=...} are the same name rather than two.
     * The name set longest ago goes past the cap.
     */
    public void setVariable(final String name, final String value) {
        if (name == null || name.isBlank()) {
            return;
        }
        final String key = name.trim().toUpperCase(Locale.ROOT);
        variables.remove(key);
        if (value == null || value.isEmpty()) {
            return;
        }
        while (variables.size() >= MAX_VARIABLES) {
            variables.remove(variables.keySet().iterator().next());
        }
        variables.put(key, value);
    }

    /** Replaces the names the shell knows (used on load). */
    public void putVariables(final Map<String, String> map) {
        variables.clear();
        if (map != null) {
            map.forEach(this::setVariable);
        }
    }

    /** Replaces the remembered recipe choices (used on load). */
    public void putRecipeChoices(final Map<String, Integer> map) {
        recipeChoices.clear();
        if (map != null) {
            map.forEach(this::setRecipeChoice);
        }
    }

    /** The default program id for {@code ext} (lowercased), or {@code ""} when none is set. */
    public String defaultApp(final String ext) {
        return defaultApps.getOrDefault(ext == null ? "" : ext.toLowerCase(Locale.ROOT), "");
    }

    /**
     * Makes {@code programId} the program that opens files ending in {@code ext} on this machine; a blank id forgets
     * the choice. The choice made longest ago goes past the cap.
     */
    public void setDefaultApp(final String ext, final String programId) {
        if (ext == null || ext.isBlank()) {
            return;
        }
        final String key = ext.toLowerCase(Locale.ROOT);
        defaultApps.remove(key);
        if (programId == null || programId.isBlank()) {
            return;
        }
        while (defaultApps.size() >= MAX_DEFAULT_APPS) {
            defaultApps.remove(defaultApps.keySet().iterator().next());
        }
        defaultApps.put(key, programId.trim());
    }

    /** An unmodifiable view of the default-app map for serialisation and display. */
    public Map<String, String> defaultApps() {
        return Collections.unmodifiableMap(defaultApps);
    }

    /** Replaces the default-app map (used on load). */
    public void putDefaultApps(final Map<String, String> map) {
        defaultApps.clear();
        if (map != null) {
            map.forEach(this::setDefaultApp);
        }
    }

    /**
     * Applies one {@code key=value} setting, clamping as needed. Returns {@code true} when the key is
     * one this store takes and the value parsed; {@code false} for an unknown key or an unparseable
     * value (so the caller can route the key elsewhere or report an error). The keys are {@link SettingKey}'s;
     * the computer name, wallpaper, and network share are owned elsewhere and are not handled here.
     *
     * @param key   the setting key (case-insensitive)
     * @param value the raw value
     * @return true if the key was one this store takes and it was applied
     */
    public boolean applySetting(final String key, final String value) {
        return SettingKey.apply(this, key, value);
    }

    /** Human-readable {@code key   value} lines for the {@code config} command's listing. */
    public List<String> summaryLines() {
        final List<String> lines = new ArrayList<>();
        lines.add(pad(SettingKey.CLOCK) + (clock12h ? "12h" : "24h"));
        lines.add(pad(SettingKey.THEME) + (themePreset.isEmpty() ? ThemePreset.SYSTEM.id() : themePreset));
        lines.add(pad(SettingKey.TASKBAR) + (taskbarCentered ? "center" : "left"));
        lines.add(pad(SettingKey.DARKMODE) + (darkMode ? "on" : "off"));
        lines.add(pad(SettingKey.GUISCALE) + (guiScale == 0 ? DEFAULT_VALUE.with("75%").english() : guiScale + "%"));
        lines.add(pad(SettingKey.BRIGHTNESS) + brightness + "%");
        lines.add(pad(SettingKey.VOLUME) + volume + "%");
        lines.add(pad(SettingKey.MUTE) + (muted ? "on" : "off"));
        lines.add(pad(SettingKey.OUTPUT) + soundOutput.id());
        lines.add(pad(SettingKey.SAVEDRIVE) + defaultSaveDrive + ":");
        lines.add(pad(SettingKey.AUTOOPEN) + (removableAutoOpen ? "on" : "off"));
        lines.add(pad(SettingKey.ACCENT)
                + (accent == 0 ? "default" : String.format(Locale.ROOT, "#%06X", accent & 0xFFFFFF)));
        lines.add(pad("pinned") + (pinned.isEmpty() ? "none" : String.join(", ", pinned)));
        lines.add(pad("favourites") + (favourites.isEmpty() ? "none" : STARRED.with(favourites.size()).english()));
        final List<String> shared = new ArrayList<>();
        for (final Share share : shares) {
            shared.add((share.writable() ? READ_WRITE : READ_ONLY).with(share.name()).english());
        }
        lines.add(pad("shares") + (shared.isEmpty() ? "none" : String.join(", ", shared)));
        lines.add(pad(SettingKey.REMOTE) + (remoteAllowed ? REMOTE_ON.with("on").english() : "off"));
        return lines;
    }

    private static String pad(final SettingKey key) {
        return pad(key.key());
    }

    private static String pad(final String key) {
        return String.format(Locale.ROOT, "  %-12s", key);
    }

    private static int clamp(final int value, final int lo, final int hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
