/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * The conditions every robot senses: what it carries, whether its inventory is full, what block stands somewhere,
 * whether a block, items or a creature are near, the time of day, its charge, and any of these turned round or joined.
 * Blocks, items and creatures are named as the game's own advancements name them, by id or by tag.
 */
public final class CoreRobotConditions {

    /** The widest a robot looks around, in blocks. */
    public static final int MOST_RADIUS = 16;
    private static final int DAY = 24_000;

    public static final RobotConditionType<Holding> HOLDING = RobotConditions.declare(new RobotConditionType<>(
            id("holding"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemPredicate.CODEC.fieldOf("item").forGetter(Holding::item),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(Holding::count))
                    .apply(instance, Holding::new))));
    public static final RobotConditionType<InventoryFull> INVENTORY_FULL = RobotConditions.declare(
            new RobotConditionType<>(id("inventory_full"), MapCodec.unit(InventoryFull::new)));
    public static final RobotConditionType<BlockIs> BLOCK_IS = RobotConditions.declare(new RobotConditionType<>(
            id("block_is"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(BlockIs::pos),
                    BlockPredicate.CODEC.fieldOf("block").forGetter(BlockIs::block))
                    .apply(instance, BlockIs::new))));
    public static final RobotConditionType<BlockNear> BLOCK_NEAR = RobotConditions.declare(new RobotConditionType<>(
            id("block_near"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BlockPredicate.CODEC.fieldOf("block").forGetter(BlockNear::block),
                    Codec.intRange(1, MOST_RADIUS).fieldOf("radius").forGetter(BlockNear::radius))
                    .apply(instance, BlockNear::new))));
    public static final RobotConditionType<ItemsNear> ITEMS_NEAR = RobotConditions.declare(new RobotConditionType<>(
            id("items_near"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ItemsNear::item),
                    Codec.intRange(1, MOST_RADIUS).fieldOf("radius").forGetter(ItemsNear::radius))
                    .apply(instance, ItemsNear::new))));
    public static final RobotConditionType<EntityNear> ENTITY_NEAR = RobotConditions.declare(new RobotConditionType<>(
            id("entity_near"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EntityTypePredicate.CODEC.fieldOf("entity").forGetter(EntityNear::entity),
                    Codec.intRange(1, MOST_RADIUS).fieldOf("radius").forGetter(EntityNear::radius))
                    .apply(instance, EntityNear::new))));
    public static final RobotConditionType<TimeBetween> TIME_BETWEEN = RobotConditions.declare(
            new RobotConditionType<>(id("time_between"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.intRange(0, DAY - 1).fieldOf("from").forGetter(TimeBetween::from),
                    Codec.intRange(0, DAY - 1).fieldOf("to").forGetter(TimeBetween::to))
                    .apply(instance, TimeBetween::new))));
    public static final RobotConditionType<ChargeBelow> CHARGE_BELOW = RobotConditions.declare(
            new RobotConditionType<>(id("charge_below"), Codec.doubleRange(0.0, 1.0).fieldOf("share")
                    .xmap(ChargeBelow::new, ChargeBelow::share)));
    public static final RobotConditionType<Not> NOT = RobotConditions.declare(new RobotConditionType<>(id("not"),
            RobotConditions.CODEC.fieldOf("condition").xmap(Not::new, Not::condition)));
    public static final RobotConditionType<All> ALL = RobotConditions.declare(new RobotConditionType<>(id("all"),
            RobotConditions.CODEC.listOf().fieldOf("conditions").xmap(All::new, All::conditions)));
    public static final RobotConditionType<Any> ANY = RobotConditions.declare(new RobotConditionType<>(id("any"),
            RobotConditions.CODEC.listOf().fieldOf("conditions").xmap(Any::new, Any::conditions)));

    private CoreRobotConditions() {
    }

    /** Declares the Core's conditions, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The block nearest {@code robot}, within {@code radius}, that {@code block} names; none when there is none. */
    public static Optional<BlockPos> nearest(final RobotEntity robot, final BlockPredicate block, final int radius) {
        final ServerLevel level = (ServerLevel) robot.level();
        return BlockPos.findClosestMatch(robot.blockPosition(), radius, radius, pos -> block.matches(level, pos))
                .map(BlockPos::immutable);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /** It carries at least {@code count} of what {@code item} names. */
    public record Holding(ItemPredicate item, int count) implements IRobotCondition {

        @Override
        public RobotConditionType<Holding> type() {
            return HOLDING;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            int carried = 0;
            for (int slot = 0; slot < robot.inventory().getContainerSize(); slot++) {
                final ItemStack stack = robot.inventory().getItem(slot);
                if (!stack.isEmpty() && item.test(stack)) {
                    carried += stack.getCount();
                }
            }
            return carried >= count;
        }
    }

    /** Its inventory has no empty slot. */
    public record InventoryFull() implements IRobotCondition {

        @Override
        public RobotConditionType<InventoryFull> type() {
            return INVENTORY_FULL;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            for (int slot = 0; slot < robot.inventory().getContainerSize(); slot++) {
                if (robot.inventory().getItem(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    /** The block at {@code pos} is one {@code block} names. */
    public record BlockIs(BlockPos pos, BlockPredicate block) implements IRobotCondition {

        @Override
        public RobotConditionType<BlockIs> type() {
            return BLOCK_IS;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            return block.matches((ServerLevel) robot.level(), pos);
        }
    }

    /** A block {@code block} names stands within {@code radius} of it. */
    public record BlockNear(BlockPredicate block, int radius) implements IRobotCondition {

        @Override
        public RobotConditionType<BlockNear> type() {
            return BLOCK_NEAR;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            return nearest(robot, block, radius).isPresent();
        }
    }

    /** Items lie on the ground within {@code radius} of it, those {@code item} names when it names any. */
    public record ItemsNear(Optional<ItemPredicate> item, int radius) implements IRobotCondition {

        @Override
        public RobotConditionType<ItemsNear> type() {
            return ITEMS_NEAR;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            return !robot.level().getEntitiesOfClass(ItemEntity.class, new AABB(robot.blockPosition()).inflate(radius),
                    lying -> lying.isAlive() && item.map(wanted -> wanted.test(lying.getItem())).orElse(true))
                    .isEmpty();
        }
    }

    /** A creature or a thing of a kind {@code entity} names is within {@code radius} of it. */
    public record EntityNear(EntityTypePredicate entity, int radius) implements IRobotCondition {

        @Override
        public RobotConditionType<EntityNear> type() {
            return ENTITY_NEAR;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            return !robot.level().getEntities(robot, robot.getBoundingBox().inflate(radius),
                    near -> near.isAlive() && entity.matches(near.getType())).isEmpty();
        }
    }

    /**
     * The time of day is from {@code from} up to {@code to}, in the game's ticks of a day, 0 at sunrise and 6000 at
     * noon; a span past midnight, as from 18000 to 1000, wraps round.
     */
    public record TimeBetween(int from, int to) implements IRobotCondition {

        @Override
        public RobotConditionType<TimeBetween> type() {
            return TIME_BETWEEN;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            final long now = Math.floorMod(robot.level().getDayTime(), (long) DAY);
            return from <= to ? now >= from && now < to : now >= from || now < to;
        }
    }

    /** Its battery holds less than {@code share} of what it can; never for a robot that runs on nothing. */
    public record ChargeBelow(double share) implements IRobotCondition {

        @Override
        public RobotConditionType<ChargeBelow> type() {
            return CHARGE_BELOW;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            return robot.batteryCapacity() > 0 && robot.chargeShare() < share;
        }
    }

    /** {@code condition} does not hold. */
    public record Not(IRobotCondition condition) implements IRobotCondition {

        @Override
        public RobotConditionType<Not> type() {
            return NOT;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            return !condition.test(robot);
        }
    }

    /** Every one of {@code conditions} holds. */
    public record All(List<IRobotCondition> conditions) implements IRobotCondition {

        public All {
            conditions = List.copyOf(conditions);
        }

        @Override
        public RobotConditionType<All> type() {
            return ALL;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            for (final IRobotCondition each : conditions) {
                if (!each.test(robot)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** At least one of {@code conditions} holds. */
    public record Any(List<IRobotCondition> conditions) implements IRobotCondition {

        public Any {
            conditions = List.copyOf(conditions);
        }

        @Override
        public RobotConditionType<Any> type() {
            return ANY;
        }

        @Override
        public boolean test(final RobotEntity robot) {
            for (final IRobotCondition each : conditions) {
                if (each.test(robot)) {
                    return true;
                }
            }
            return false;
        }
    }
}
