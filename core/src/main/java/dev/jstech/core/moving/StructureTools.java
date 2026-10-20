/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The tools of a moving structure ({@link BlockAboard.Tool}), working the world in front of them as the structure
 * passes, as a combine harvester drives and harvests at once: a drill mines the block before it, as long as that
 * block takes to mine with what the drill mines as; a harvester takes ripe crops and plants them again, and the upper
 * part of a stacked plant; a plough tills the ground; a placer sets the blocks the structure carries where there is
 * room. What a tool takes goes into the structure's storage, its chests and the like, or onto the ground when it has
 * none; what a placer sets comes from it. A tool never works land that its claims keep from it, nor a block of
 * another moving structure.
 */
final class StructureTools {

    /* How far each drill has mined the block before it, by the drill's place, and what that block was. */
    private final Map<BlockPos, Progress> mining = new HashMap<>();
    /* When each tool may work next, in game ticks, by its place. */
    private final Map<BlockPos, Long> ready = new HashMap<>();
    /* The storage aboard, found again when the structure's blocks change. */
    private final List<BlockPos> storage = new ArrayList<>();
    private long storageAt = -STORAGE_AGAIN - 1L;

    /** Blocks a harvester takes whole, as melons and pumpkins. */
    static final TagKey<Block> HARVESTED_WHOLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "harvested_whole"));
    /** Plants a harvester takes all but the lowest of, as sugar cane and bamboo. */
    static final TagKey<Block> HARVESTED_ABOVE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "harvested_above"));
    /* A mining progress of this much is a block mined. */
    private static final float MINED = 1.0F;
    /* The game's own: a block's hardness is mined at a thirtieth of the tool's speed a tick, or a hundredth by a tool
     * it does not drop to. */
    private static final float RIGHT_TOOL = 30.0F;
    private static final float WRONG_TOOL = 100.0F;
    /* Ticks in a second, and how often the storage aboard is looked for again at most. */
    private static final double TICKS = 20;
    private static final int STORAGE_AGAIN = 40;
    private static final double HALF = 0.5;

    /* One tool's tick: it works the blocks in front of it, as far as it reaches. */
    void work(final ServerLevel level, final MovingStructure structure, final BlockPos pos, final BlockState state,
              final BlockAboard.Tool tool) {
        final long now = level.getGameTime();
        if (tool.kind() != BlockAboard.ToolKind.DRILL && ready.getOrDefault(pos, Long.MIN_VALUE) > now) {
            return;
        }
        final Direction way = BlockPhysics.wayOf(state);
        final StructureFrame frame = structure.frame();
        for (int step = 1; step <= tool.reach(); step++) {
            final BlockPos target = ahead(frame, pos, way, step);
            if (!worksAt(level, structure, target)) {
                continue;
            }
            final boolean done = switch (tool.kind()) {
                case DRILL -> drill(level, structure, pos, target, tool);
                case HARVESTER -> harvest(level, structure, target, tool);
                case PLOUGH -> plough(level, target);
                case PLACER -> place(level, structure, target, frame, way);
            };
            if (done && tool.kind() != BlockAboard.ToolKind.DRILL) {
                ready.put(pos.immutable(), now + Math.max(1, Math.round(TICKS / tool.speed())));
                return;
            }
        }
    }

    /* The world's block {@code step} blocks in front of the tool at the yard's {@code pos}, looking {@code way}. */
    private static BlockPos ahead(final StructureFrame frame, final BlockPos pos, final Direction way, final int step) {
        final double x = pos.getX() + HALF + way.getStepX() * step;
        final double y = pos.getY() + HALF + way.getStepY() * step;
        final double z = pos.getZ() + HALF + way.getStepZ() * step;
        return BlockPos.containing(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }

    /* Whether a tool may work the world's block {@code target}: loaded, of the world, and not kept from it. */
    private static boolean worksAt(final ServerLevel level, final MovingStructure structure, final BlockPos target) {
        if (YardLayout.isYardBlock(target.getX()) || level.isOutsideBuildHeight(target) || !Loaded.at(level, target)) {
            return false;
        }
        return Protection.may(Actor.world(structure.entity().blockPosition()), ProtectedAction.BREAK, level, target);
    }

    /* A drill mines the block before it a little each tick, as fast as its speed and what it mines as allow. */
    private boolean drill(final ServerLevel level, final MovingStructure structure, final BlockPos pos,
                          final BlockPos target, final BlockAboard.Tool tool) {
        final BlockState block = level.getBlockState(target);
        final float hardness = block.getDestroySpeed(level, target);
        if (block.isAir() || hardness < 0 || !block.getFluidState().isEmpty() && block.getCollisionShape(level,
                target).isEmpty()) {
            mining.remove(pos);
            return false;
        }
        final ItemStack with = tool.lootTool();
        final boolean drops = !block.requiresCorrectToolForDrops() || with.isCorrectToolForDrops(block);
        final float itemSpeed = with.isEmpty() ? 1.0F : with.getDestroySpeed(block);
        final float rate = hardness == 0 ? MINED
                : (float) (tool.speed() * itemSpeed / hardness / (drops ? RIGHT_TOOL : WRONG_TOOL));
        Progress progress = mining.get(pos);
        if (progress == null || !progress.target.equals(target) || progress.state != block) {
            if (progress != null) {
                level.destroyBlockProgress(progress.id, progress.target, -1);
            }
            progress = new Progress(target.immutable(), block, idOf(structure, pos));
            mining.put(pos.immutable(), progress);
        }
        progress.done += rate;
        if (progress.done < MINED) {
            level.destroyBlockProgress(progress.id, target, (int) (progress.done * 10));
            return false;
        }
        level.destroyBlockProgress(progress.id, target, -1);
        mining.remove(pos);
        final BlockEntity entity = level.getBlockEntity(target);
        final List<ItemStack> taken = drops ? Block.getDrops(block, level, target, entity, null, with) : List.of();
        level.destroyBlock(target, false);
        stow(level, structure, taken, target);
        return true;
    }

    /* A harvester takes a ripe crop and plants it again, a crop taken whole, or the top of a stacked plant. */
    private boolean harvest(final ServerLevel level, final MovingStructure structure, final BlockPos target,
                            final BlockAboard.Tool tool) {
        final BlockState block = level.getBlockState(target);
        final BlockState replanted;
        if (block.getBlock() instanceof CropBlock crop) {
            replanted = crop.isMaxAge(block) ? crop.getStateForAge(0) : null;
        } else if (block.getBlock() instanceof NetherWartBlock) {
            replanted = block.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE
                    ? block.setValue(NetherWartBlock.AGE, 0) : null;
        } else if (block.getBlock() instanceof CocoaBlock) {
            replanted = block.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE ? block.setValue(CocoaBlock.AGE, 0)
                    : null;
        } else if (block.getBlock() instanceof SweetBerryBushBlock) {
            replanted = block.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE
                    ? block.setValue(SweetBerryBushBlock.AGE, 1) : null;
        } else if (block.is(HARVESTED_WHOLE)
                || block.is(HARVESTED_ABOVE) && level.getBlockState(target.below()).is(block.getBlock())) {
            replanted = Blocks.AIR.defaultBlockState();
        } else {
            return false;
        }
        if (replanted == null) {
            return false;
        }
        final List<ItemStack> taken = new ArrayList<>(Block.getDrops(block, level, target,
                level.getBlockEntity(target), null, tool.lootTool()));
        if (!replanted.isAir()) {
            // The seed planted again is one of what it gave, as a farmer's own.
            final ItemStack seed = block.getBlock().getCloneItemStack(level, target, block);
            for (final ItemStack each : taken) {
                if (!seed.isEmpty() && ItemStack.isSameItem(each, seed)) {
                    each.shrink(1);
                    break;
                }
            }
            level.setBlock(target, replanted, Block.UPDATE_ALL);
            level.levelEvent(2001, target, Block.getId(block));
        } else {
            level.destroyBlock(target, false);
        }
        stow(level, structure, taken, target);
        return true;
    }

    /* A plough tills the ground before it, as a hoe does, where there is air above to till. */
    private static boolean plough(final ServerLevel level, final BlockPos target) {
        final BlockState ground = level.getBlockState(target);
        if (!level.getBlockState(target.above()).isAir()) {
            return false;
        }
        // Tilled as a hoe tills: the game asks the hoe in hand whether it may.
        final UseOnContext tilling = new UseOnContext(level, null, InteractionHand.MAIN_HAND,
                new ItemStack(Items.IRON_HOE), new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target,
                false));
        final BlockState tilled = ground.getToolModifiedState(tilling, ItemAbilities.HOE_TILL, false);
        if (tilled == null || tilled == ground) {
            return false;
        }
        level.setBlock(target, tilled, Block.UPDATE_ALL_IMMEDIATE);
        level.playSound(null, target, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    /* A placer sets a block it carries where there is room before it, facing the way it looks. */
    private boolean place(final ServerLevel level, final MovingStructure structure, final BlockPos target,
                          final StructureFrame frame, final Direction way) {
        if (!level.getBlockState(target).canBeReplaced()) {
            return false;
        }
        for (final IItemHandler handler : storage(level, structure)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                final ItemStack held = handler.getStackInSlot(slot);
                if (!(held.getItem() instanceof BlockItem item)) {
                    continue;
                }
                final ItemStack one = handler.extractItem(slot, 1, true);
                if (one.isEmpty()) {
                    continue;
                }
                final Direction looks = worldWay(frame, way);
                final DirectionalPlaceContext context = new DirectionalPlaceContext(level, target, looks, one,
                        looks.getOpposite());
                if (item.place(context).consumesAction()) {
                    handler.extractItem(slot, 1, false);
                    return true;
                }
            }
        }
        return false;
    }

    /* What a tool took goes into the storage aboard, the rest onto the ground where it was taken. */
    private void stow(final ServerLevel level, final MovingStructure structure, final List<ItemStack> taken,
                      final BlockPos at) {
        for (final ItemStack stack : taken) {
            ItemStack left = stack;
            for (final IItemHandler handler : storage(level, structure)) {
                left = ItemHandlerHelper.insertItemStacked(handler, left, false);
                if (left.isEmpty()) {
                    break;
                }
            }
            if (!left.isEmpty()) {
                final ItemEntity dropped = new ItemEntity(level, at.getX() + HALF, at.getY() + HALF, at.getZ() + HALF,
                        left);
                dropped.setDefaultPickUpDelay();
                level.addFreshEntity(dropped);
            }
        }
    }

    /* The storage aboard: every block of the structure holding items, found again now and then. */
    private List<IItemHandler> storage(final ServerLevel level, final MovingStructure structure) {
        if (level.getGameTime() - storageAt > STORAGE_AGAIN) {
            storageAt = level.getGameTime();
            storage.clear();
            for (int chunkX = structure.bounds().minX() >> 4; chunkX <= structure.bounds().maxX() >> 4; chunkX++) {
                for (int chunkZ = structure.bounds().minZ() >> 4; chunkZ <= structure.bounds().maxZ() >> 4;
                     chunkZ++) {
                    final LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }
                    for (final BlockPos held : chunk.getBlockEntities().keySet()) {
                        if (structure.bounds().contains(held.getX(), held.getY(), held.getZ())) {
                            storage.add(held.immutable());
                        }
                    }
                }
            }
        }
        final List<IItemHandler> handlers = new ArrayList<>();
        for (final BlockPos held : storage) {
            final IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, held, null);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /* The world's way nearest the yard's way {@code way}, as {@code frame} is turned. */
    private static Direction worldWay(final StructureFrame frame, final Direction way) {
        return Direction.getNearest(frame.turnX(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnY(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnZ(way.getStepX(), way.getStepY(), way.getStepZ()));
    }

    /* A number for a drill's cracks in the block it mines, its own and no player's. */
    private static int idOf(final MovingStructure structure, final BlockPos pos) {
        return -(structure.id().value().hashCode() * 31 + pos.hashCode() & Integer.MAX_VALUE) - 1;
    }

    /* A drill's mining of a block: the block, what it was, its cracks' number, and how far mined. */
    private static final class Progress {

        private final BlockPos target;
        private final BlockState state;
        private final int id;
        private float done;

        private Progress(final BlockPos target, final BlockState state, final int id) {
            this.target = target;
            this.state = state;
            this.id = id;
        }
    }
}
