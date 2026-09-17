/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.os.CraftingManagerApp;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.DownloadToMediaPayload;
import dev.jstech.computers.operation.payload.LoadFromMediaPayload;
import dev.jstech.computers.operation.payload.RemoveRomCraftPayload;
import dev.jstech.computers.operation.payload.RequestCraftManagerPayload;
import dev.jstech.computers.operation.payload.SetMachineConfigPayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.VolumeLabel;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.StorageKey;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.jstech.computers.operation.payload.WireStrings.wire;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.craftFileNameFor;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.deleteCraftFromDisk;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.reconcileCraftsFolder;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.sanitizeFileBase;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.writeCraftToDisk;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaStackFor;

/**
 * The Crafting Manager's payloads: patterns loaded from media, written back to media or removed, and the settings
 * of a machine.
 */
public final class CraftManagerPayloads {

    private CraftManagerPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestCraftManagerPayload.TYPE, RequestCraftManagerPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestCraftManagerPayload::hostPos), CraftManagerPayloads::handleRequestCraftManager);
        ComputerAccess.accept(registrar, SetMachineConfigPayload.TYPE, SetMachineConfigPayload.STREAM_CODEC,
                ComputerAccess.machine(SetMachineConfigPayload::hostPos), CraftManagerPayloads::handleSetMachineConfig);
        registrar.playToClient(CraftManagerStatePayload.TYPE, CraftManagerStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(CraftManagerPayloads::handleCraftManagerState));
        ComputerAccess.accept(registrar, LoadFromMediaPayload.TYPE, LoadFromMediaPayload.STREAM_CODEC,
                ComputerAccess.machine(LoadFromMediaPayload::hostPos), CraftManagerPayloads::handleLoadFromMedia);
        ComputerAccess.accept(registrar, DownloadToMediaPayload.TYPE, DownloadToMediaPayload.STREAM_CODEC,
                ComputerAccess.machine(DownloadToMediaPayload::hostPos), CraftManagerPayloads::handleDownloadToMedia);
        ComputerAccess.accept(registrar, RemoveRomCraftPayload.TYPE, RemoveRomCraftPayload.STREAM_CODEC,
                ComputerAccess.machine(RemoveRomCraftPayload::hostPos), CraftManagerPayloads::handleRemoveRomCraft);
    }

    /** The names of the {@code .craft} files on a medium, in listing order, capped to what a wire field carries. */
    public static List<String> craftFileListFromMedia(final ItemStack media) {
        final List<DiskFilesystem.FileEntry> entries =
                DiskFilesystem.list(media, "", FilesystemKind.HIERARCHICAL);
        final List<String> names = new ArrayList<>();
        for (final DiskFilesystem.FileEntry e : entries) {
            /*
             * A name the wire cannot carry would disconnect the player on every listing; the filesystem's own
             * name limit is the cap, so this only guards against a path the filesystem should never hold.
             */
            if (e.type() == FileType.CRAFT && names.size() < CraftManagerStatePayload.MAX_MEDIA_FILES
                    && e.path().length() <= FsPaths.MAX_NAME_LENGTH) {
                names.add(e.path());
            }
        }
        return names;
    }

    /** The Machines tab sets a machine's concurrency config on a Crafting Computer, then gets a fresh state. */
    private static void handleSetMachineConfig(final SetMachineConfigPayload payload, final ServerPlayer player,
                                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        cc.setMachineConfig(payload.machineKey(), new CraftingComputerBlockEntity.MachineConfig(
                payload.maxJobs(), payload.locked(), payload.feedMax()));
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level));
    }

    /**
     * Returns the Crafting Manager state for a Crafting Computer: the first linked drive's medium
     * and its {@code .craft} files plus the computer's Recipe ROM with cross-reference flags.
     */
    private static void handleRequestCraftManager(final RequestCraftManagerPayload payload, final ServerPlayer player,
                                                  final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        // Self-heal the crafts/ mirror against the ROM before presenting the state.
        reconcileCraftsFolder(cc, level);
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level));
    }

    /** Routes the Crafting Manager state payload to the open {@link CraftingManagerApp}. */
    private static void handleCraftManagerState(final CraftManagerStatePayload payload, final Player player) {
        CraftingManagerApp.accept(payload);
    }

    /**
     * Loads {@code .craft} files from a removable medium into the Crafting Computer's Recipe ROM.
     * When {@code allMissing} is set, every file not already covered by a ROM pattern is loaded.
     */
    private static void handleLoadFromMedia(final LoadFromMediaPayload payload, final ServerPlayer player,
                                            final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        final String key = payload.mediaVolumeKey();
        if (!key.startsWith("media:")) {
            return;
        }
        final ItemStack media = mediaStackFor(level, cc, key);
        if (media.isEmpty()) {
            return;
        }
        final List<String> toLoad;
        if (payload.allMissing()) {
            // Collect every .craft file on the medium that is not already in the ROM.
            final Set<String> romNames = new HashSet<>();
            for (final CraftingPattern p : cc.romPatterns()) {
                romNames.add(craftFileNameFor(p) + ".craft");
            }
            final List<DiskFilesystem.FileEntry> entries =
                    DiskFilesystem.list(media, "", FilesystemKind.HIERARCHICAL);
            toLoad = new ArrayList<>();
            for (final DiskFilesystem.FileEntry e : entries) {
                if (e.type() == FileType.CRAFT && !romNames.contains(e.path())) {
                    toLoad.add(e.path());
                }
            }
        } else {
            toLoad = payload.fileNames();
        }
        int loaded = 0;
        int parsed = 0;
        boolean romFull = false;
        for (final String fileName : toLoad) {
            final Optional<String> content = DiskFilesystem.read(media, fileName);
            if (content.isEmpty()) {
                continue;
            }
            final String kind = CraftFile.typeOf(content.get());
            if (cc.romUsed() >= CraftingComputerBlockEntity.RECIPE_ROM_LIMIT) {
                romFull = true; // bench, processing and multi-stage all share the one ROM budget
                continue;
            }
            boolean added = false;
            String diskName = fileName;
            if ("proc".equals(kind)) {
                final var p = CraftFile.parseProcessing(content.get(), level.registryAccess());
                if (p.isEmpty()) {
                    continue;
                }
                parsed++;
                added = cc.loadMachineRecipe(
                        NetworkRecipe.ofProcessing(p.get()));
            } else if ("multi".equals(kind)) {
                final var p = CraftFile.parseMultiStage(content.get(), level.registryAccess());
                if (p.isEmpty()) {
                    continue;
                }
                parsed++;
                added = cc.loadMachineRecipe(
                        NetworkRecipe.ofMultiStage(p.get()));
            } else {
                final var p = CraftFile.parse(content.get(), level.registryAccess());
                if (p.isEmpty()) {
                    continue;
                }
                parsed++;
                added = cc.loadPattern(p.get());
                diskName = craftFileNameFor(p.get()) + ".craft";
            }
            /*
             * The recipe registers in the ROM (what the network can craft) and the .craft is mirrored under
             * crafts/ on the system disk so it shows up in the Files app.
             */
            if (added) {
                loaded++;
            }
            writeCraftToDisk(cc, diskName, content.get());
        }
        cc.setChanged();
        final String status;
        if (romFull) {
            status = "Loaded " + loaded + " of " + parsed + " - ROM full ("
                    + cc.romUsed() + "/" + CraftingComputerBlockEntity.RECIPE_ROM_LIMIT + ")";
        } else if (loaded > 0) {
            status = "Loaded " + loaded + " craft" + (loaded == 1 ? "" : "s");
        } else {
            status = parsed > 0 ? "Already loaded" : "Nothing to load";
        }
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level, status));
    }

    /**
     * Removes the selected Recipe ROM patterns from the Crafting Computer, deleting each mirrored
     * {@code .craft} file under {@code crafts/} on the system disk as well. Indices are applied
     * highest-first so an earlier removal does not shift a later index.
     */
    private static void handleRemoveRomCraft(final RemoveRomCraftPayload payload, final ServerPlayer player,
                                             final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        final List<Integer> indices = new ArrayList<>(payload.romIndices());
        indices.sort(Comparator.reverseOrder());
        for (final int idx : indices) {
            if (idx >= CraftManagerStatePayload.MACHINE_ROM_BASE) {
                // A machine recipe (processing/multi-stage): the offset index addresses that list.
                cc.removeMachineRecipe(idx - CraftManagerStatePayload.MACHINE_ROM_BASE);
                continue;
            }
            final List<CraftingPattern> rom = cc.romPatterns();
            if (idx < 0 || idx >= rom.size()) {
                continue;
            }
            final String diskName = craftFileNameFor(rom.get(idx)) + ".craft";
            cc.removePattern(idx);
            deleteCraftFromDisk(cc, diskName);
        }
        cc.setChanged();
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level));
    }

    /**
     * Serializes selected Recipe ROM patterns as {@code .craft} files onto a removable medium.
     */
    private static void handleDownloadToMedia(final DownloadToMediaPayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        final String key = payload.mediaVolumeKey();
        if (!key.startsWith("media:")) {
            return;
        }
        final ItemStack media = mediaStackFor(level, cc, key);
        if (media.isEmpty()) {
            return;
        }
        final List<CraftingPattern> rom = cc.romPatterns();
        for (final int idx : payload.romIndices()) {
            final Optional<String> content;
            final String base;
            if (idx >= CraftManagerStatePayload.MACHINE_ROM_BASE) {
                // A machine recipe: serialize it back to its typed .craft form.
                final int mi = idx - CraftManagerStatePayload.MACHINE_ROM_BASE;
                final var recipes = cc.machineRecipes();
                if (mi < 0 || mi >= recipes.size()) {
                    continue;
                }
                final var r = recipes.get(mi);
                if (r.proc().isPresent()) {
                    content = CraftFile.serializeProcessing(r.proc().get(), level.registryAccess());
                } else if (r.multi().isPresent()) {
                    content = CraftFile.serializeMultiStage(r.multi().get(), level.registryAccess());
                } else {
                    continue;
                }
                base = sanitizeFileBase(r.displayName());
            } else {
                if (idx < 0 || idx >= rom.size()) {
                    continue;
                }
                final CraftingPattern pattern = rom.get(idx);
                content = CraftFile.serialize(pattern, level.registryAccess());
                base = craftFileNameFor(pattern);
            }
            if (content.isEmpty()) {
                continue;
            }
            /*
             * A recipe already on the disc under this name is never overwritten: a different one gets the
             * next free suffix, the same one is simply there already. The encoder writes by the same rule.
             */
            final String fileName = DiskFilesystem.uniquePath(media, base, ".craft", content.get());
            final long freeWeight = mediaFreeWeightFor(media);
            DiskFilesystem.write(media, fileName, FileType.CRAFT, content.get(),
                    freeWeight, FilesystemKind.HIERARCHICAL, level.getGameTime());
        }
        // Propagate the updated filesystem component to the reader slot.
        final String rest = key.substring("media:".length());
        final int slash = rest.indexOf('/');
        final String rawPos = slash < 0 ? rest : rest.substring(0, slash);
        try {
            final long encoded = Long.parseLong(rawPos);
            if (level.getBlockEntity(BlockPos.of(encoded))
                    instanceof MediaReaderBlockEntity reader) {
                reader.setChanged();
            }
        } catch (final NumberFormatException ignored) {
        }
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level));
    }

    /**
     * Builds a full {@link CraftManagerStatePayload} for {@code cc}: picks the linked drive whose writable
     * removable medium holds {@code .craft} files (or, when none does, the first one holding writable
     * media, so a download still has a target), lists that medium's files, and annotates each ROM pattern
     * with whether a matching file already exists on it.
     */
    public static CraftManagerStatePayload buildCraftManagerState(final CraftingComputerBlockEntity cc,
                                                                   final ServerLevel level) {
        return buildCraftManagerState(cc, level, "");
    }

    private static CraftManagerStatePayload buildCraftManagerState(final CraftingComputerBlockEntity cc,
                                                                    final ServerLevel level, final String status) {
        String mediaVolumeKey = "";
        String mediaLabel = "";
        List<String> mediaFiles = List.of();

        /*
         * A computer commonly has more than one drive linked (a floppy drive, a DVD drive, a dock), and the
         * one with a blank medium in it may well come first: the disc the player just wrote is the one they
         * mean, wherever it sits.
         */
        for (final long endpoint : cc.linkedEndpoints()) {
            if (level.getBlockEntity(BlockPos.of(endpoint))
                    instanceof MediaReaderBlockEntity reader) {
                final ItemStack m = reader.mediaSlot().getStackInSlot(0);
                if (m.isEmpty()
                        || !(m.getItem() instanceof FormattedMediaItem fmt)
                        || !fmt.writable()) {
                    continue;
                }
                final List<String> files = craftFileListFromMedia(m);
                if (mediaVolumeKey.isEmpty() || !files.isEmpty()) {
                    mediaVolumeKey = "media:" + endpoint;
                    mediaLabel = wire(VolumeLabel.of(m, "Removable Drive"), 64);
                    mediaFiles = files;
                }
                if (!files.isEmpty()) {
                    break;
                }
            }
        }

        final Set<String> mediaFileSet = new HashSet<>(mediaFiles);
        final List<CraftManagerStatePayload.WireRomEntry> romEntries = new ArrayList<>();
        final List<CraftingPattern> rom = cc.romPatterns();
        for (int i = 0; i < rom.size() && i < CraftManagerStatePayload.MAX_ROM_ENTRIES; i++) {
            final CraftingPattern p = rom.get(i);
            /*
             * Every string below is cut to its wire field: a result renamed to a long name or a modded machine
             * with a long id must never make the state impossible to send.
             */
            final String name = wire(p.displayName(), 64);
            final String fileName = craftFileNameFor(p) + ".craft";
            romEntries.add(new CraftManagerStatePayload.WireRomEntry(i, name, mediaFileSet.contains(fileName)));
        }
        /*
         * Machine recipes (processing / multi-stage) share the ROM and must be listed too, since an invisible entry
         * reads as "not loaded" and then the duplicate check looks wrong. Their indices are offset so the
         * remove action can tell them apart from the bench patterns above.
         */
        final var machineRecipes = cc.machineRecipes();
        for (int i = 0; i < machineRecipes.size()
                && romEntries.size() < CraftManagerStatePayload.MAX_ROM_ENTRIES; i++) {
            final var r = machineRecipes.get(i);
            final String name = wire(r.displayName() + (r.multi().isPresent() ? " [multi]" : " [machine]"), 64);
            romEntries.add(new CraftManagerStatePayload.WireRomEntry(
                    CraftManagerStatePayload.MACHINE_ROM_BASE + i, name, false));
        }
        /*
         * The routed machines (the Machines tab): each distinct machine type the wired switches declare, with
         * its concurrency config. Keyed by the machine's block registry id, which is what a pattern targets.
         * One wire per PHYSICAL machine (deduped by position). Paused/Feed are read per machine; Max Jobs is the
         * machine type's shared ceiling. The label distinguishes machines of one type by their face and position.
         */
        final List<CraftManagerStatePayload.WireMachine> machines = new ArrayList<>();
        final Set<BlockPos> seen = new HashSet<>();
        for (final var dm : cc.availableMachines()) {
            final BlockPos pos = dm.machinePos();
            final String typeKey = dm.machineType();
            if (typeKey == null || typeKey.isBlank() || pos == null || !seen.add(pos)
                    || machines.size() >= CraftManagerStatePayload.MAX_MACHINES) {
                continue;
            }
            final String machineKey = CraftingComputerBlockEntity.machineStateKey(pos);
            final CraftingComputerBlockEntity.MachineConfig perMachine = cc.machineConfig(machineKey);
            final int typeMaxJobs = cc.machineConfig(typeKey).maxJobs();
            final String face = dm.face() != null
                    ? dm.face().getName().substring(0, 1).toUpperCase(Locale.ROOT) + " " : "";
            final String label = !dm.name().isBlank() ? dm.name()
                    : face + "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
            machines.add(new CraftManagerStatePayload.WireMachine(
                    wire(machineKey, 64), wire(typeKey, 48), wire(label, 80),
                    perMachine.locked(), perMachine.feedMax(), typeMaxJobs));
        }
        return new CraftManagerStatePayload(mediaVolumeKey, mediaLabel, mediaFiles, romEntries,
                cc.craftingCardFactor() > 0.0, wire(status, 96), machines);
    }

    /** Computes the remaining free weight on a removable medium (filesystem component only). */
    private static long mediaFreeWeightFor(final ItemStack media) {
        if (!(media.getItem() instanceof FormattedMediaItem fmt)) {
            return 0L;
        }
        final long capWeight = (long) fmt.format().capacityItems() * StorageKey.MB_EQ_PER_ITEM;
        final long fsUsed = DiskFilesystem.filesWeight(media);
        return Math.max(0L, capWeight - fsUsed);
    }
}
