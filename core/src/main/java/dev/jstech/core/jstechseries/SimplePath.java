/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.ApiStatus;

/**
 * The simpler path a mod gives to what the shared catalogue holds, used without J's Industrial: a mod's recipe of a
 * component or a fuel that is not the industrial chain carries the condition {@code jscore:simple_path} with its mod,
 * and stays in the game unless J's Industrial is installed and the mod's own server rule, off by default, makes the
 * industrial chain the only path.
 *
 * <pre>{@code
 * SimplePath.rule(MODID, MySettings.FILE, MySettings.INDUSTRIAL_CHAIN_ONLY);
 * // in a recipe file: "neoforge:conditions": [{"type": "jscore:simple_path", "mod": "mymod"}]
 * }</pre>
 */
public final class SimplePath {

    private static final Map<String, BooleanSupplier> RULES = new ConcurrentHashMap<>();
    private static final String INDUSTRIAL = "jsindustrial";
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(
            NeoForgeRegistries.Keys.CONDITION_CODECS, JsCore.MODID);

    /** The condition, as recipe files name it: {@code jscore:simple_path}. */
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<Allowed>> ALLOWED =
            CONDITIONS.register("simple_path", () -> Allowed.CODEC);

    private SimplePath() {
    }

    /**
     * The server rule of the mod {@code modid}: whether, with J's Industrial installed, the industrial chain is the
     * only path to what the mod makes of the shared catalogue.
     */
    public static void rule(final String modid, final BooleanSupplier industrialChainOnly) {
        RULES.put(Objects.requireNonNull(modid, "modid"), Objects.requireNonNull(industrialChainOnly,
                "industrialChainOnly"));
    }

    /**
     * The server rule of the mod {@code modid}, as a switch in one of its world's settings files: read as the recipes
     * load, so it holds from a world's first load rather than from its first {@code /reload}.
     */
    public static void rule(final String modid, final ConfigFile file, final ConfigKey<Boolean> industrialChainOnly) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(industrialChainOnly, "industrialChainOnly");
        rule(modid, () -> ConfigFiles.forData(file).get(industrialChainOnly));
    }

    /** Whether the simpler path of the mod {@code modid} is open now. */
    public static boolean open(final String modid) {
        final BooleanSupplier rule = RULES.get(modid);
        return !(ModList.get().isLoaded(INDUSTRIAL) && rule != null && rule.getAsBoolean());
    }

    /** The condition a recipe of the mod {@code modid}'s simpler path carries, for its data generation. */
    public static ICondition condition(final String modid) {
        return new Allowed(modid);
    }

    /** Registers the condition, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void register(final IEventBus modBus) {
        CONDITIONS.register(modBus);
    }

    /**
     * The condition: the mod's simpler path is open.
     *
     * @param mod the mod whose path it is
     */
    public record Allowed(String mod) implements ICondition {

        /** How it is written in a recipe file. */
        public static final MapCodec<Allowed> CODEC = RecordCodecBuilder.mapCodec(allowed -> allowed.group(
                Codec.STRING.fieldOf("mod").forGetter(Allowed::mod)
        ).apply(allowed, Allowed::new));

        public Allowed {
            Objects.requireNonNull(mod, "mod");
        }

        @Override
        public boolean test(final IContext context) {
            return open(this.mod);
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return ALLOWED.get();
        }
    }
}
