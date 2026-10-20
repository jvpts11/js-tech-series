/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.moving.physics.BlockPhysics;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * What a block does aboard a moving structure, as data, so any mod and data pack takes part: a seat, or a helm whose
 * sitter steers the structure; a docking clamp that holds another structure's clamp met face to face as one with it;
 * a decoupler that parts the structure there at a signal, as a rocket's stages part; and a tool that works the world
 * as the structure passes: a drill, a harvester, a plough or a placer.
 *
 * <pre>{@code
 * "values": {
 *   "mymod:pilot_seat": {"seat": {"height": 0.5, "helm": true}},
 *   "mymod:docking_port": {"clamp": {"reach": 1.5, "signal": "back"}},
 *   "mymod:stage_separator": {"decoupler": {"push": 4000}},
 *   "mymod:drill_head": {"tool": {"kind": "drill", "speed": 2, "loot": "minecraft:iron_pickaxe"}}
 * }
 * }</pre>
 *
 * @param seat      the seat it is
 * @param clamp     the docking clamp it is
 * @param decoupler the decoupler it is
 * @param tool      the tool it is
 */
public record BlockAboard(Optional<Seat> seat, Optional<Clamp> clamp, Optional<Decoupler> decoupler,
                          Optional<Tool> tool) {

    /** A block that does nothing of its own aboard. */
    public static final BlockAboard NONE = new BlockAboard(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    public static final Codec<BlockAboard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Seat.CODEC.optionalFieldOf("seat").forGetter(BlockAboard::seat),
            Clamp.CODEC.optionalFieldOf("clamp").forGetter(BlockAboard::clamp),
            Decoupler.CODEC.optionalFieldOf("decoupler").forGetter(BlockAboard::decoupler),
            Tool.CODEC.optionalFieldOf("tool").forGetter(BlockAboard::tool)
    ).apply(instance, BlockAboard::new));
    /** What each block does aboard, by the block. */
    public static final DataMapType<Block, BlockAboard> ABOARD = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "block_aboard"), Registries.BLOCK, CODEC)
            .synced(CODEC, false).build();

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(ABOARD));
    }

    /** What the block {@code state} does aboard: the map's, or {@link #NONE}. */
    public static BlockAboard of(final BlockState state) {
        final BlockAboard mapped = state.getBlockHolder().getData(ABOARD);
        return mapped == null ? NONE : mapped;
    }

    /** Whether it does anything of its own aboard. */
    public boolean any() {
        return seat.isPresent() || clamp.isPresent() || decoupler.isPresent() || tool.isPresent();
    }

    /**
     * A seat: where its sitter sits, above the block's bottom, and whether it is a helm, whose sitter's controls move
     * the structure.
     *
     * @param height how high above the block's bottom the sitter sits, in blocks
     * @param helm   whether it is a helm
     */
    public record Seat(double height, boolean helm) {

        public static final Codec<Seat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 2).optionalFieldOf("height", 0.5).forGetter(Seat::height),
                Codec.BOOL.optionalFieldOf("helm", false).forGetter(Seat::helm)
        ).apply(instance, Seat::new));
    }

    /**
     * A docking clamp: on its signal, it holds the clamp of another structure met face to face within its reach, the
     * two as one; without it, it lets go.
     *
     * @param reach  how far apart two clamps' faces may be and still meet, in blocks
     * @param signal where the signal that closes it reaches it from
     */
    public record Clamp(double reach, BlockPhysics.SignalSide signal) {

        public static final Codec<Clamp> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.1, 8).optionalFieldOf("reach", 1.5).forGetter(Clamp::reach),
                BlockPhysics.SignalSide.CODEC.optionalFieldOf("signal", BlockPhysics.SignalSide.ANY)
                        .forGetter(Clamp::signal)
        ).apply(instance, Clamp::new));
    }

    /**
     * A decoupler: on its signal it is gone, and the structure parts there, its two sides pushed apart.
     *
     * @param push   how hard it pushes the two sides apart, in newton seconds
     * @param signal where the signal that fires it reaches it from
     */
    public record Decoupler(double push, BlockPhysics.SignalSide signal) {

        public static final Codec<Decoupler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 1.0e9).optionalFieldOf("push", 0.0).forGetter(Decoupler::push),
                BlockPhysics.SignalSide.CODEC.optionalFieldOf("signal", BlockPhysics.SignalSide.ANY)
                        .forGetter(Decoupler::signal)
        ).apply(instance, Decoupler::new));
    }

    /**
     * A tool that works the world in front of it as the structure passes.
     *
     * @param kind  what it does
     * @param speed how fast it works: a drill's mining speed, as a pickaxe's; every other's, times a second at most
     * @param loot  the item whose drops it takes, as if mined with it; a drill of none takes a block's drops by hand
     * @param reach how many blocks ahead of it it works
     */
    public record Tool(ToolKind kind, double speed, Optional<ResourceLocation> loot, int reach) {

        public static final Codec<Tool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StableCodecs.byName(ToolKind.class).fieldOf("kind").forGetter(Tool::kind),
                Codec.doubleRange(0.01, 1_000).optionalFieldOf("speed", 1.0).forGetter(Tool::speed),
                ResourceLocation.CODEC.optionalFieldOf("loot").forGetter(Tool::loot),
                Codec.intRange(1, 4).optionalFieldOf("reach", 1).forGetter(Tool::reach)
        ).apply(instance, Tool::new));

        /** The item it takes drops as if mined with: its loot's, or nothing, by hand. */
        public ItemStack lootTool() {
            final Item item = loot.map(BuiltInRegistries.ITEM::get).orElse(null);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
    }

    /** What a tool aboard does. */
    public enum ToolKind implements IStableName {
        /** Mines the block in front of it, as fast as its speed. */
        DRILL("drill"),
        /** Harvests the ripe crop in front of it, and plants it again. */
        HARVESTER("harvester"),
        /** Tills the ground in front of and under it. */
        PLOUGH("plough"),
        /** Places the blocks it carries where there is room in front of it. */
        PLACER("placer");

        private final String serializedName;

        ToolKind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
