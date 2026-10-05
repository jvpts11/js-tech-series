/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.core.id.StableCodecs;
import net.minecraft.resources.ResourceLocation;

/**
 * A kernel: the core of an operating system, which every system built on it shares.
 *
 * <p>A kernel says three things about its systems: how they share the processor among their programs (not at all,
 * by each program giving way, or by the kernel taking turns for them), how they keep files (none, one folder, or
 * folders inside folders), and how their command line behaves. An operating system names the kernel it runs on
 * ({@link OsDef}), so several systems can share one, as every Linux distribution here shares {@code jsc:linux}.
 * Addons register their own via {@link ComputersRegisterEvent#kernel}. The {@link #CODEC} keeps this
 * JSON-serialisable so the built-in kernels can later move to a datapack.
 *
 * <pre>{@code
 * new KernelDef(ResourceLocation.fromNamespaceAndPath("myaddon", "micro"),
 *         SchedulerKind.PREEMPTIVE, FilesystemKind.HIERARCHICAL, ShellFamily.POSIX)
 * }</pre>
 *
 * @param id          unique registry key for this kernel (e.g. {@code jsc:dos})
 * @param scheduler   the task-scheduling model this kernel provides
 * @param filesystem  the filesystem model this kernel provides
 * @param shellFamily the command-line syntax family every OS on this kernel speaks (DOS, POSIX or NET)
 */
public record KernelDef(
        ResourceLocation id,
        SchedulerKind scheduler,
        FilesystemKind filesystem,
        ShellFamily shellFamily
) {

    public static final Codec<KernelDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(KernelDef::id),
            StableCodecs.byName(SchedulerKind.class).fieldOf("scheduler").forGetter(KernelDef::scheduler),
            StableCodecs.byName(FilesystemKind.class).fieldOf("filesystem").forGetter(KernelDef::filesystem),
            StableCodecs.byName(ShellFamily.class).optionalFieldOf("shell_family", ShellFamily.DOS)
                    .forGetter(KernelDef::shellFamily)
    ).apply(inst, KernelDef::new));

    public KernelDef {
        if (shellFamily == null) {
            shellFamily = ShellFamily.DOS;
        }
    }
}
