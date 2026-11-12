/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.sigma.DesktopFacts;
import dev.jstech.computers.sigma.SystemFacts;
import dev.jstech.core.text.LongText;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * How a player's systems and desktops are written down: in the world's save, and in what a client is told of them.
 *
 * <p>Apart from the records, which the compiler makes and which carry no Minecraft dependency.
 */
public final class PlayerCodecs {

    public static final Codec<SystemFacts.Kernel> KERNEL = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("base").forGetter(SystemFacts.Kernel::base),
            Codec.STRING.fieldOf("scheduler").forGetter(SystemFacts.Kernel::scheduler),
            Codec.INT.fieldOf("slice").forGetter(SystemFacts.Kernel::slice),
            Codec.STRING.fieldOf("file_system").forGetter(SystemFacts.Kernel::fileSystem),
            Codec.STRING.listOf().fieldOf("drivers").forGetter(SystemFacts.Kernel::drivers),
            Codec.STRING.fieldOf("memory_rule").forGetter(SystemFacts.Kernel::memoryRule),
            Codec.STRING.fieldOf("shell").forGetter(SystemFacts.Kernel::shell)
    ).apply(inst, SystemFacts.Kernel::new));

    public static final Codec<SystemFacts> SYSTEM_FACTS = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(SystemFacts::name),
            Codec.STRING.fieldOf("version").forGetter(SystemFacts::version),
            Codec.STRING.fieldOf("author").forGetter(SystemFacts::author),
            Codec.STRING.fieldOf("era").forGetter(SystemFacts::era),
            Codec.STRING.fieldOf("isa").forGetter(SystemFacts::isa),
            Codec.INT.fieldOf("memory_mb").forGetter(SystemFacts::memoryMb),
            KERNEL.fieldOf("kernel").forGetter(SystemFacts::kernel),
            Codec.STRING.listOf().optionalFieldOf("services", List.of()).forGetter(SystemFacts::services)
    ).apply(inst, SystemFacts::new));

    public static final Codec<DesktopFacts> DESKTOP_FACTS = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(DesktopFacts::name),
            Codec.STRING.fieldOf("version").forGetter(DesktopFacts::version),
            Codec.STRING.fieldOf("author").forGetter(DesktopFacts::author),
            Codec.STRING.fieldOf("menu").forGetter(DesktopFacts::menu),
            Codec.STRING.fieldOf("panel_at").forGetter(DesktopFacts::panelAt),
            Codec.STRING.fieldOf("frames").forGetter(DesktopFacts::frames),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("colours").forGetter(DesktopFacts::colours),
            Codec.STRING.fieldOf("icons").forGetter(DesktopFacts::icons),
            Codec.STRING.fieldOf("wallpaper").forGetter(DesktopFacts::wallpaper),
            Codec.STRING.listOf().fieldOf("programs").forGetter(DesktopFacts::programs)
    ).apply(inst, DesktopFacts::new));

    /*
     * A system's or a desktop's files, by path inside its folder. A file's text may be longer than a save's string
     * holds, as one of box drawing or of any script but the Latin is at fewer letters than the files allow, and one
     * such file would fail the save of every system and desktop the world keeps.
     */
    private static final Codec<Map<String, String>> FILES = Codec.unboundedMap(Codec.STRING, LongText.CODEC);

    public static final Codec<PlayerSystem> SYSTEM = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(PlayerSystem::id),
            SYSTEM_FACTS.fieldOf("facts").forGetter(PlayerSystem::facts),
            Codec.STRING.fieldOf("folder").forGetter(PlayerSystem::folder),
            FILES.fieldOf("files").forGetter(PlayerSystem::files)
    ).apply(inst, PlayerSystem::new));

    public static final Codec<PlayerDesktop> DESKTOP = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(PlayerDesktop::id),
            DESKTOP_FACTS.fieldOf("facts").forGetter(PlayerDesktop::facts),
            Codec.STRING.fieldOf("folder").forGetter(PlayerDesktop::folder),
            FILES.fieldOf("files").forGetter(PlayerDesktop::files)
    ).apply(inst, PlayerDesktop::new));

    private PlayerCodecs() {
    }
}
