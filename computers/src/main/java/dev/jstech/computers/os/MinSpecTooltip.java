/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the minimum-requirement lines for an OS or a program from its registered {@link OsDef} /
 * {@link ProgramSpec}. Used by install-media tooltips and the This PC app so the player sees what hardware and OS
 * something needs before installing it. Only common types are referenced, so this is safe to call from item
 * tooltips (which run client-side) without a client/server boundary.
 *
 * <p>Each list comes two ways from the same sentences: as text, for a screen that resolves it or a file that is
 * written in English, and as a tooltip's components, with the era in the colour every hardware tooltip gives it.
 */
@TextHolder
public final class MinSpecTooltip {

    private static final TextKey DISK_FOOTPRINT = TextKey.of("jsc.os.min_spec.disk_footprint", "Disk footprint: %s MB");
    private static final TextKey NEEDS_ERA = TextKey.of("jsc.os.min_spec.needs_era", "Needs %s hardware or later");
    private static final TextKey REQUIRES = TextKey.of("jsc.os.min_spec.requires", "Requires %s");
    private static final TextKey OR_NEWER = TextKey.of("jsc.os.min_spec.or_newer", "%s or newer");
    private static final TextKey CPU = TextKey.of("jsc.os.min_spec.cpu", "CPU %s MHz+");
    private static final TextKey VRAM = TextKey.of("jsc.os.min_spec.vram", "VRAM %s MB+");
    private static final TextKey DISK_FREE = TextKey.of("jsc.os.min_spec.disk_free", "Disk %s MB free");

    private MinSpecTooltip() {
    }

    /** The minimum-spec lines for an OS installer: the hardware era it needs and the disk it occupies. */
    public static List<Text> osMinSpecText(final ResourceLocation osId) {
        final List<Text> lines = new ArrayList<>(2);
        final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
        if (os == null) {
            return lines;
        }
        lines.add(NEEDS_ERA.with(os.minEra().text()));
        lines.add(DISK_FOOTPRINT.with(os.footprintMb()));
        return lines;
    }

    /** The same lines as a tooltip draws them. */
    public static List<Component> osMinSpec(final ResourceLocation osId) {
        final List<Component> lines = new ArrayList<>(2);
        final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
        if (os == null) {
            return lines;
        }
        lines.add(needsEra(os.minEra()));
        lines.add(line(DISK_FOOTPRINT.with(os.footprintMb())));
        return lines;
    }

    /** The minimum-spec lines for a program: its OS floor (platform + version) and its hardware minimums. */
    public static List<Text> programMinSpecText(final ResourceLocation progId) {
        final List<Text> lines = new ArrayList<>(5);
        final ProgramSpec prog = progId == null ? null : OsRegistry.getProgram(progId);
        if (prog == null) {
            return lines;
        }
        lines.add(REQUIRES.with(minOsLabel(prog)));
        if (prog.minEra() != HardwareEra.VINTAGE) {
            lines.add(NEEDS_ERA.with(prog.minEra().text()));
        }
        lines.addAll(hardware(prog));
        return lines;
    }

    /** The same lines as a tooltip draws them. */
    public static List<Component> programMinSpec(final ResourceLocation progId) {
        final List<Component> lines = new ArrayList<>(5);
        final ProgramSpec prog = progId == null ? null : OsRegistry.getProgram(progId);
        if (prog == null) {
            return lines;
        }
        /*
         * The OS requirement is the headline the player cares about, so it is highlighted (aqua) inside a
         * muted "Requires"; the raw hardware minimums follow in grey.
         */
        lines.add(Component.translatableWithFallback(REQUIRES.key(), REQUIRES.english(),
                GameText.component(minOsLabel(prog)).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        /*
         * The era floor sits with the hardware minimums because that is what it is: a machine of an
         * older generation cannot run it at any clock speed.
         */
        if (prog.minEra() != HardwareEra.VINTAGE) {
            // Worded exactly like the OS line above: the same requirement must not read as two rules.
            lines.add(needsEra(prog.minEra()));
        }
        for (final Text minimum : hardware(prog)) {
            lines.add(line(minimum));
        }
        return lines;
    }

    /** A readable, comma-joined list of platform labels in enum order: names, the same in every language. */
    public static String platformsLabel(final Set<Platform> platforms) {
        final List<String> names = new ArrayList<>(platforms.size());
        for (final Platform p : Platform.values()) {
            if (platforms.contains(p)) {
                names.add(p.label());
            }
        }
        return String.join(", ", names);
    }

    /** A readable name for a hardware era. */
    public static Text eraLabel(final HardwareEra era) {
        return era.text();
    }

    /** "Needs X hardware or later", with the era in the colour every hardware tooltip gives it. */
    private static Component needsEra(final HardwareEra era) {
        return Component.translatableWithFallback(NEEDS_ERA.key(), NEEDS_ERA.english(),
                HardwareTooltip.eraName(era, era.text())).withStyle(ChatFormatting.DARK_GRAY);
    }

    /** The hardware minimums a program names, each only when it names one. */
    private static List<Text> hardware(final ProgramSpec prog) {
        final List<Text> lines = new ArrayList<>(3);
        if (prog.minCpuMhz() > 0) {
            lines.add(CPU.with(prog.minCpuMhz()));
        }
        if (prog.minVramMb() > 0) {
            lines.add(VRAM.with(prog.minVramMb()));
        }
        if (prog.minDiskMb() > 0) {
            lines.add(DISK_FREE.with(prog.minDiskMb()));
        }
        return lines;
    }

    /**
     * The system floor a program needs: the one it names in its family's order, otherwise its platforms.
     *
     * <p>The name comes from the systems themselves rather than being written here, so a family the mod does
     * not ship reads the same way as the one it does.
     */
    private static Text minOsLabel(final ProgramSpec prog) {
        if (prog.minOsRank() <= 0) {
            return Text.literal(platformsLabel(prog.platforms()));
        }
        return OR_NEWER.with(OsRegistry.systemOfRank(prog.minOsRank()));
    }

    private static MutableComponent line(final Text text) {
        return GameText.component(text).withStyle(ChatFormatting.DARK_GRAY);
    }
}
