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
import dev.jstech.core.id.StableCodecs;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable descriptor for a kernel that an OS can run on top of.
 *
 * <p>A kernel defines the scheduling model and filesystem model available to the OS. Community
 * addons can register custom kernels via {@link JSComputersAPI#registerKernel(KernelDef)} to
 * ship alternative kernel implementations (e.g. Unix-like) without modifying the mod core. The
 * {@link #CODEC} keeps this JSON-serialisable so the built-in kernels can later move to a datapack.
 *
 * @param id          unique registry key for this kernel (e.g. {@code jsc:dos})
 * @param scheduler   the task-scheduling model this kernel provides
 * @param filesystem  the filesystem model this kernel provides
 * @param shellFamily the command-line syntax family every OS on this kernel speaks (DOS or POSIX)
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
