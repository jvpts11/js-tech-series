/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.advancement.ProgramTravels;
import dev.jstech.computers.storage.StorageVolumes;
import dev.jstech.core.JsCore;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.persistence.CoreChunkData;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Every kind of thing the series saves, saved today carrying the version of its layout, and a save of each from before
 * there were versions read back as it was: every block entity of the series' mods, the networks a chunk is on, the
 * operator of a machine, the programs a player first ran, and the store of the drives' contents.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SavedTypeLayoutGameTests {

    private static final String ARENA = "empty";
    private static final Set<String> SERIES = Set.of(JsCore.MODID, JsComputers.MODID, JsIndustrial.MODID);

    private SavedTypeLayoutGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void blockEntities_readTheirSavesFromBeforeVersions(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        final Set<BlockEntityType<?>> seen = new HashSet<>();
        final List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (final Block block : BuiltInRegistries.BLOCK) {
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!SERIES.contains(id.getNamespace()) || !(block instanceof EntityBlock maker)) {
                continue;
            }
            final BlockState state = block.defaultBlockState();
            final BlockEntity made = maker.newBlockEntity(pos, state);
            if (!(made instanceof SyncedBlockEntity entity) || !seen.add(made.getType())) {
                continue;
            }
            try {
                final CompoundTag saved = entity.saveWithoutMetadata(registries);
                if (SaveLayout.versionOf(saved) != entity.fields().layout().version()) {
                    wrong.add(id + " saved without its version: " + saved.get(SaveLayout.VERSION_KEY));
                    continue;
                }
                final CompoundTag before = saved.copy();
                before.remove(SaveLayout.VERSION_KEY);
                final BlockEntity fresh = Objects.requireNonNull(maker.newBlockEntity(pos, state), "a fresh one");
                fresh.loadWithComponents(before, registries);
                final CompoundTag again = fresh.saveWithoutMetadata(registries);
                if (!again.equals(saved)) {
                    wrong.add(id + " read its save from before versions as " + again + ", not " + saved);
                }
                checked++;
            } catch (final RuntimeException failed) {
                wrong.add(id + " failed: " + failed);
            }
        }
        helper.assertTrue(wrong.isEmpty(), "block entities that did not: " + wrong);
        helper.assertTrue(checked > 10, "only " + checked + " block entities were found to check");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chunkNetworks_readAChunkSavedBeforeTheirLayout(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final ChunkPos where = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        final NetworkUuid network = new NetworkUuid(UUID.randomUUID());
        final CoreChunkData networks = CoreChunkData.of(new LinkedHashSet<>(List.of(network)));
        final LevelChunk saving = new LevelChunk(helper.getLevel(), where);
        saving.setData(CoreAttachments.CHUNK_NETWORKS.get(), networks);

        final CompoundTag attachments = Objects.requireNonNull(saving.writeAttachmentsToNBT(registries),
                "the chunk's attachments");
        final String key = attachmentKey(CoreAttachments.CHUNK_NETWORKS.get());
        helper.assertTrue(SaveLayout.versionOf(attachments.getCompound(key)) == 1, "the version beside the list");
        unwrap(attachments, key);
        final LevelChunk reading = new LevelChunk(helper.getLevel(), where);
        reading.readAttachmentsFromNBT(registries, attachments);

        same(helper, networks.networks(), reading.getData(CoreAttachments.CHUNK_NETWORKS.get()).networks(),
                "the networks of a chunk saved before the layout");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void machineOperator_readsAMachineSavedBeforeItsLayout(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        final EntityBlock maker = firstMachine();
        final BlockState state = ((Block) maker).defaultBlockState();
        final UUID operator = UUID.randomUUID();
        final BlockEntity machine = Objects.requireNonNull(maker.newBlockEntity(pos, state), "a machine");
        machine.setData(MachineOperators.OPERATOR, operator);

        final CompoundTag saved = machine.saveWithoutMetadata(registries);
        final CompoundTag attachments = saved.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY);
        unwrap(attachments, attachmentKey(MachineOperators.OPERATOR.get()));
        final BlockEntity fresh = Objects.requireNonNull(maker.newBlockEntity(pos, state), "a fresh machine");
        fresh.loadWithComponents(saved, registries);

        same(helper, operator, fresh.getData(MachineOperators.OPERATOR), "the operator of a machine saved before");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void programFirstRun_readsAPlayerSavedBeforeItsLayout(final GameTestHelper helper) {
        final Map<String, Long> firstRuns = new HashMap<>(Map.of("hello", 42L));
        final ArmorStand saving = new ArmorStand(EntityType.ARMOR_STAND, helper.getLevel());
        saving.setData(ProgramTravels.FIRST_RUN, firstRuns);

        final CompoundTag saved = saving.saveWithoutId(new CompoundTag());
        unwrap(saved.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY), attachmentKey(ProgramTravels.FIRST_RUN.get()));
        final ArmorStand reading = new ArmorStand(EntityType.ARMOR_STAND, helper.getLevel());
        reading.load(saved);

        same(helper, firstRuns, reading.getData(ProgramTravels.FIRST_RUN), "the first runs of a player saved before");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void storageVolumes_readAFileSavedBeforeTheirLayout(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final CompoundTag volume = new CompoundTag();
        volume.putUUID("Id", UUID.randomUUID());
        final ListTag volumes = new ListTag();
        volumes.add(volume);
        final CompoundTag before = new CompoundTag();
        before.put("Volumes", volumes);

        final StorageVolumes read = StorageVolumes.factory().deserializer().apply(before, registries);
        final CompoundTag saved = read.save(new CompoundTag(), registries);

        same(helper, 1, read.count(), "the volume of a file saved before the layout");
        same(helper, StorageVolumes.LAYOUT.version(), SaveLayout.versionOf(saved), "the version it is saved in now");
        helper.succeed();
    }

    /* The first machine of J's Computers whose block entity is one of the series'. */
    private static EntityBlock firstMachine() {
        for (final Block block : BuiltInRegistries.BLOCK) {
            if (JsComputers.MODID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())
                    && block instanceof EntityBlock maker
                    && maker.newBlockEntity(BlockPos.ZERO, block.defaultBlockState()) instanceof SyncedBlockEntity) {
                return maker;
            }
        }
        throw new IllegalStateException("J's Computers has no machine");
    }

    private static String attachmentKey(final AttachmentType<?> type) {
        return Objects.requireNonNull(NeoForgeRegistries.ATTACHMENT_TYPES.getKey(type), "a registered attachment")
                .toString();
    }

    /* Puts the bare value back where its layout's wrapper was, as a save from before the layout held it. */
    private static void unwrap(final CompoundTag attachments, final String key) {
        final CompoundTag wrapped = attachments.getCompound(key);
        attachments.put(key, Objects.requireNonNull(wrapped.get(SaveLayout.VALUE_KEY), "the wrapped value of " + key));
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
