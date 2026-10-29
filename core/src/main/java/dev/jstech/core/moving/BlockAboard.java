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
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 *   "mymod:crew_couch": {"seat": {"height": 0.1, "lying": true}},
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

    /**
     * What the block at {@code pos} does aboard: for a block merged into a formed structure, what its formed model
     * says that kind of block does, or else what the block it was does ({@link FormedShape#aboard(Block)}); for any
     * other, {@link #of}.
     */
    public static BlockAboard at(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FormedPartBlock)
                || !(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)) {
            return of(state);
        }
        final BlockState was = part.original();
        final FormedPlace place = part.place();
        if (place != null) {
            final Optional<BlockAboard> told = FormedShapes.aboard(level, place, was.getBlock());
            if (told.isPresent()) {
                return told.get();
            }
        }
        return of(was);
    }

    /**
     * The way the block at {@code pos} looks, in the yard: its facing; a block merged into a formed structure, the way
     * the block it was looked, turned as its structure stands; north for a block that looks no way.
     */
    public static Direction wayAt(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FormedPartBlock)
                || !(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)) {
            return BlockPhysics.wayOf(state);
        }
        final Direction was = BlockPhysics.wayOf(part.original());
        final FormedPlace place = part.place();
        if (place == null) {
            return was;
        }
        final int[] turned = place.turn().apply(was.getStepX(), was.getStepY(), was.getStepZ());
        return Direction.getNearest(turned[0], turned[1], turned[2]);
    }

    /** Whether it does anything of its own aboard. */
    public boolean any() {
        return seat.isPresent() || clamp.isPresent() || decoupler.isPresent() || tool.isPresent();
    }

    /**
     * A seat: where its sitter sits, above the block's bottom, whether it is a helm, whose sitter's controls move the
     * structure, and whether its sitter lies on it, as a capsule's crew lie on their couches.
     *
     * @param height how high above the block's bottom the sitter sits, in blocks; one lying, their back
     * @param helm   whether it is a helm
     * @param lying  whether its sitter lies on their back, face up, their feet the way the seat looks
     *               ({@link #wayAt}) and their eyes {@link #LYING_REACH} from the block's middle towards their head,
     *               looking round as they like; a seat that looks up or down is sat on upright
     */
    public record Seat(double height, boolean helm, boolean lying) {

        /**
         * How far from the middle of its block towards their head a sitter lying on a seat has their eyes: over the
         * block's edge, so looking straight up they see what is over the seat and a good way past their head.
         */
        public static final double LYING_REACH = 0.5;
        public static final Codec<Seat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, 2).optionalFieldOf("height", 0.5).forGetter(Seat::height),
                Codec.BOOL.optionalFieldOf("helm", false).forGetter(Seat::helm),
                Codec.BOOL.optionalFieldOf("lying", false).forGetter(Seat::lying)
        ).apply(instance, Seat::new));

        /** A seat sat on upright, a helm or not. */
        public Seat(final double height, final boolean helm) {
            this(height, helm, false);
        }
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
