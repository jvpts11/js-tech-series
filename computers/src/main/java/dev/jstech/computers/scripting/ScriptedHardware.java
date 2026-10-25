/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.scripting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.hardware.HardwareCodecs;
import dev.jstech.computers.hardware.PlainCardSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The parts and the kinds a modpack's startup scripts declare as objects. The scripts run before the game sets up, so
 * what they declare is kept as they write it and declared through the register event, the kinds before the parts that
 * use them, each read with its sheet's codec: a field written wrong stops the game saying what is wrong and of what.
 */
public final class ScriptedHardware {

    private static final List<Kept> KEPT = new ArrayList<>();
    /* Whether the register event has been: what a script declares after it would never be read. */
    private static boolean declared;

    private ScriptedHardware() {
    }

    /** Declares everything the scripts kept, through {@code event}; the mod's listener on the event. */
    public static synchronized void declare(final ComputersRegisterEvent event) {
        declared = true;
        for (final Kept kept : KEPT) {
            if (kept.kind().isKind()) {
                kept.kind().declare(event, kept);
            }
        }
        for (final Kept kept : KEPT) {
            if (!kept.kind().isKind()) {
                kept.kind().declare(event, kept);
            }
        }
    }

    /**
     * Reads {@code sheet}, an object as a script writes it, with {@code codec}.
     *
     * @param what what the sheet is of, for the message: "the processor mypack:cpu"
     * @throws IllegalArgumentException with what is wrong, when the sheet does not read
     */
    public static <T> T read(final Codec<T> codec, final Object sheet, final String what) {
        final DataResult<T> read;
        try {
            read = codec.parse(JavaOps.INSTANCE, plain(sheet));
        } catch (final IllegalArgumentException | NullPointerException wrong) {
            throw new IllegalArgumentException(what + " does not read: " + wrong.getMessage(), wrong);
        }
        return read.getOrThrow(message -> new IllegalArgumentException(what + " does not read: " + message));
    }

    /* Keeps what a script declares, while the game has not yet set up. */
    static synchronized void keep(final Kind kind, final @Nullable String item, final Object sheet,
                                  final @Nullable String era) {
        if (declared) {
            throw new IllegalStateException("hardware is declared from a startup script, which runs while the game "
                    + "loads; this script runs after it has set up");
        }
        KEPT.add(new Kept(kind, item, sheet, era));
    }

    /*
     * What a script engine hands over, made of the plain things a codec reads: its objects as maps, its arrays as
     * lists and its strings as strings, whatever classes the engine keeps them in.
     */
    private static Object plain(final Object value) {
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, entry) -> out.put(String.valueOf(key), plain(entry)));
            return out;
        }
        if (value instanceof List<?> list) {
            final List<Object> out = new ArrayList<>(list.size());
            list.forEach(entry -> out.add(plain(entry)));
            return out;
        }
        return value instanceof CharSequence text ? text.toString() : value;
    }

    private static Item item(final Kept kept, final String what) {
        final String id = kept.item() == null ? "" : kept.item();
        final ResourceLocation key = id.indexOf(':') < 0 ? ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id)
                : ResourceLocation.parse(id);
        final Item item = BuiltInRegistries.ITEM.get(key);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("the " + what + " " + key + " is declared, but no item is registered "
                    + "under that id");
        }
        return item;
    }

    /** What a script declares: a kind of hardware, or a part an item of the modpack is. */
    enum Kind {
        MEMORY_GENERATION(true) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.memoryGeneration(read(HardwareCodecs.NEW_MEMORY_GENERATION, kept.sheet(), "a memory generation"));
            }
        },
        SLOT_GENERATION(true) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.slotGeneration(read(HardwareCodecs.NEW_SLOT_GENERATION, kept.sheet(), "a slot generation"));
            }
        },
        FORM_FACTOR(true) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.formFactor(read(HardwareCodecs.NEW_FORM_FACTOR, kept.sheet(), "a form factor"));
            }
        },
        CARD_KIND(true) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.cardKind(read(HardwareCodecs.NEW_CARD_KIND, kept.sheet(), "a kind of card"));
            }
        },
        PROCESSOR(false) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.processor(item(kept, "processor"), read(HardwareCodecs.PROCESSOR, kept.sheet(),
                        "the processor " + kept.item()));
            }
        },
        GRAPHICS_CARD(false) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.graphicsCard(item(kept, "graphics card"), read(HardwareCodecs.GRAPHICS_CARD, kept.sheet(),
                        "the graphics card " + kept.item()));
            }
        },
        MEMORY(false) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.memory(item(kept, "memory module"), read(HardwareCodecs.MEMORY, kept.sheet(),
                        "the memory module " + kept.item()));
            }
        },
        DISK(false) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.disk(item(kept, "disk"), read(HardwareCodecs.DISK, kept.sheet(), "the disk " + kept.item()));
            }
        },
        MOTHERBOARD(false) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.motherboard(item(kept, "motherboard"), read(HardwareCodecs.MOTHERBOARD, kept.sheet(),
                        "the motherboard " + kept.item()));
            }
        },
        POWER_SUPPLY(false) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                event.powerSupply(item(kept, "power supply"), read(HardwareCodecs.POWER_SUPPLY, kept.sheet(),
                        "the power supply " + kept.item()), read(HardwareCodecs.ERA, kept.era() == null ? ""
                        : kept.era(), "the era of the power supply " + kept.item()));
            }
        },
        CARD(false) {
            @Override
            void declare(final ComputersRegisterEvent event, final Kept kept) {
                final PlainCardSpec card = read(HardwareCodecs.CARD, kept.sheet(), "the card " + kept.item());
                event.expansionCard(item(kept, "card"), card, card.era());
            }
        };

        private final boolean kind;

        Kind(final boolean kind) {
            this.kind = kind;
        }

        /* Whether it declares a kind of hardware rather than a part. */
        boolean isKind() {
            return kind;
        }

        abstract void declare(ComputersRegisterEvent event, Kept kept);
    }

    /* What a script declared, as it wrote it. */
    record Kept(Kind kind, @Nullable String item, Object sheet, @Nullable String era) {
    }
}
