/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import com.mojang.serialization.Codec;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * A piece of real data a material carries: its symbol, its atomic number, its formula, its colour, its molar mass. A
 * property is never a game rule. It is given in code or by a data pack, and its line in a material's tooltip shows
 * always, only with Shift held, or never, as the property says unless a material says otherwise.
 *
 * @param id     the property's id
 * @param codec  how its value is written in a data pack and sent
 * @param shown  when its line shows in a tooltip, unless a material says otherwise
 * @param name   what its line calls it: "Symbol"
 * @param format how its value reads on that line
 * @param <T>    what its value is
 */
public record MaterialProperty<T>(ResourceLocation id, Codec<T> codec, Shown shown, TextKey name,
                                  Function<T, Text> format) {

    /** When a property's line shows in a tooltip. */
    public enum Shown {
        ALWAYS("always"),
        /** Only while Shift is held. */
        SHIFT("shift"),
        NEVER("never");

        private final String serializedName;

        /** How it is written in a data pack. */
        public static final Codec<Shown> CODEC = Codec.STRING.xmap(Shown::byName, Shown::serializedName);

        Shown(final String serializedName) {
            this.serializedName = serializedName;
        }

        /** The name it is written as in a data pack. */
        public String serializedName() {
            return this.serializedName;
        }

        private static Shown byName(final String name) {
            for (final Shown shown : values()) {
                if (shown.serializedName.equals(name)) {
                    return shown;
                }
            }
            return NEVER;
        }
    }

    public MaterialProperty {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(shown, "shown");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(format, "format");
    }

    /** How its value reads, for a value of any kind: what the tooltip shows. */
    @SuppressWarnings("unchecked")
    public Text read(final Object value) {
        return this.format.apply((T) value);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
