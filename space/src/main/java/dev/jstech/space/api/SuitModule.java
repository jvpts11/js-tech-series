/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.Objects;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * A module a suit's piece takes: what it does, which piece it fits, and how much. Every danger has its module, which
 * lets a player stay there as long as they like.
 *
 * <pre>{@code
 * {"name": "item.jsspace.radiation_vest", "kind": "radiation", "piece": "chest", "amount": 0.5}
 * }</pre>
 *
 * @param name   the key of its name in the language files
 * @param kind   what it does
 * @param piece  the piece of a suit it fits
 * @param amount how much: the share of the tanks it adds, of the radiation it stops, the light it gives, of the glare
 *               it cuts
 */
public record SuitModule(String name, Kind kind, EquipmentSlot piece, double amount) {

    /** As a data pack's file writes one. */
    public static final Codec<SuitModule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SuitModule::name),
            Kind.CODEC.fieldOf("kind").forGetter(SuitModule::kind),
            EquipmentSlot.CODEC.fieldOf("piece").forGetter(SuitModule::piece),
            Codec.doubleRange(0, 15).fieldOf("amount").forGetter(SuitModule::amount)
    ).apply(instance, SuitModule::new));

    public SuitModule {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(piece, "piece");
    }

    /** What a module does. */
    public enum Kind implements IStableName {
        /** More air in the tanks: its amount a share of what they hold. */
        AIR("air"),
        /** Stops cosmic radiation and the Sun's storms: its amount the share it stops. */
        RADIATION("radiation"),
        /** Lights the way: its amount the light it gives, 0 to 15. */
        LIGHT("light"),
        /**
         * Cuts the Sun's glare where no air softens it, a visor lowered over the helmet's: its amount the share it
         * cuts, 1 for all of it.
         */
        GLARE("glare");

        public static final Codec<Kind> CODEC = StableCodecs.byName(Kind.class);

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
