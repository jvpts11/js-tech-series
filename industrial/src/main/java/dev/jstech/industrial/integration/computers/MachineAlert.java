/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextTags;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;

/**
 * One alert a controller raised: about which machine, of what, when, what the machine read then, whether an operator
 * has acknowledged it, and whether what raised it still holds.
 *
 * @param id           its number among its controller's alerts
 * @param pos          where the machine stands
 * @param dayTime      the world's clock when it was raised
 * @param machine      the machine's name, as the machines table gives it
 * @param kind         what it is about
 * @param reading      what the machine read as it was raised: "1 840 °C, limit 1 750 °C"
 * @param acknowledged whether an operator has seen it; seen, it stays, dimmed
 * @param active       whether what raised it still holds
 */
public record MachineAlert(long id, long pos, long dayTime, String machine, AlertKind kind, Text reading,
                           boolean acknowledged, boolean active) {

    private static final Codec<Text> TEXT = CompoundTag.CODEC.xmap(TextTags::read, TextTags::write);
    public static final Codec<MachineAlert> CODEC = RecordCodecBuilder.create(alert -> alert.group(
            Codec.LONG.fieldOf("id").forGetter(MachineAlert::id),
            Codec.LONG.fieldOf("pos").forGetter(MachineAlert::pos),
            Codec.LONG.fieldOf("day_time").forGetter(MachineAlert::dayTime),
            Codec.STRING.fieldOf("machine").forGetter(MachineAlert::machine),
            StableCodecs.byName(AlertKind.class).fieldOf("kind").forGetter(MachineAlert::kind),
            TEXT.fieldOf("reading").forGetter(MachineAlert::reading),
            Codec.BOOL.fieldOf("acknowledged").forGetter(MachineAlert::acknowledged),
            Codec.BOOL.fieldOf("active").forGetter(MachineAlert::active)
    ).apply(alert, MachineAlert::new));

    public MachineAlert {
        Objects.requireNonNull(machine, "machine");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(reading, "reading");
    }

    /** The same alert, acknowledged. */
    public MachineAlert seen() {
        return new MachineAlert(id, pos, dayTime, machine, kind, reading, true, active);
    }

    /** The same alert, what raised it over. */
    public MachineAlert over() {
        return new MachineAlert(id, pos, dayTime, machine, kind, reading, acknowledged, false);
    }
}
