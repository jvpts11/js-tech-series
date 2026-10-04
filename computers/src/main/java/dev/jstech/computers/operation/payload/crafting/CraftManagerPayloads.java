/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.client.os.CraftingManagerApp;
import dev.jstech.computers.crafting.CraftingDispatch;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.DownloadToMediaPayload;
import dev.jstech.computers.operation.payload.LoadFromMediaPayload;
import dev.jstech.computers.operation.payload.MoveCraftPayload;
import dev.jstech.computers.operation.payload.RemoveRomCraftPayload;
import dev.jstech.computers.operation.payload.RequestCraftManagerPayload;
import dev.jstech.computers.operation.payload.SetCraftInterfacePayload;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.VolumeLabel;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.operation.payload.WireStrings.wire;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.craftFileNameFor;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.deleteCraftFromDisk;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.reconcileCraftsFolder;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.sanitizeFileBase;
import static dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk.writeCraftToDisk;
import static dev.jstech.computers.operation.payload.files.FileAccess.mediaStackFor;

/**
 * The Crafting Manager's payloads: recipes loaded from media into a place of the Crafting Computer (a card's ROM or
 * an interface it drives), moved between places, written back to media or removed, and an interface set from the
 * Interfaces tab.
 */
public final class CraftManagerPayloads {

    private CraftManagerPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestCraftManagerPayload.TYPE, RequestCraftManagerPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestCraftManagerPayload::hostPos),
                CraftManagerPayloads::handleRequestCraftManager);
        registrar.playToClient(CraftManagerStatePayload.TYPE, CraftManagerStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(CraftManagerPayloads::handleCraftManagerState));
        ComputerAccess.accept(registrar, LoadFromMediaPayload.TYPE, LoadFromMediaPayload.STREAM_CODEC,
                ComputerAccess.machine(LoadFromMediaPayload::hostPos), CraftManagerPayloads::handleLoadFromMedia);
        ComputerAccess.accept(registrar, DownloadToMediaPayload.TYPE, DownloadToMediaPayload.STREAM_CODEC,
                ComputerAccess.machine(DownloadToMediaPayload::hostPos), CraftManagerPayloads::handleDownloadToMedia);
        ComputerAccess.accept(registrar, RemoveRomCraftPayload.TYPE, RemoveRomCraftPayload.STREAM_CODEC,
                ComputerAccess.machine(RemoveRomCraftPayload::hostPos), CraftManagerPayloads::handleRemoveRomCraft);
        ComputerAccess.accept(registrar, MoveCraftPayload.TYPE, MoveCraftPayload.STREAM_CODEC,
                ComputerAccess.machine(MoveCraftPayload::hostPos), CraftManagerPayloads::handleMoveCraft);
        ComputerAccess.accept(registrar, SetCraftInterfacePayload.TYPE, SetCraftInterfacePayload.STREAM_CODEC,
                ComputerAccess.machine(SetCraftInterfacePayload::hostPos),
                CraftManagerPayloads::handleSetCraftInterface);
    }

    /** The names of the {@code .craft} files on a medium, in listing order, capped to what a wire field carries. */
    public static List<String> craftFileListFromMedia(final ItemStack media) {
        final List<DiskFilesystem.FileEntry> entries = DiskFilesystem.list(media, "", FilesystemKind.HIERARCHICAL);
        final List<String> names = new ArrayList<>();
        for (final DiskFilesystem.FileEntry e : entries) {
            /*
             * A name the wire cannot carry would disconnect the player on every listing; the filesystem's own name
             * limit is the cap, so this only guards against a path the filesystem should never hold.
             */
            if (e.type() == FileType.CRAFT && names.size() < CraftManagerStatePayload.MAX_MEDIA_FILES
                    && e.path().length() <= FsPaths.MAX_NAME_LENGTH) {
                names.add(e.path());
            }
        }
        return names;
    }

    /**
     * Builds a full {@link CraftManagerStatePayload} for {@code cc}: picks the linked drive whose writable removable
     * medium holds {@code .craft} files (or, when none does, the first one holding writable media, so a download
     * still has a target), lists that medium's files, then what the computer keeps by place and its interfaces.
     */
    public static CraftManagerStatePayload buildCraftManagerState(final CraftingComputerBlockEntity cc,
                                                                   final ServerLevel level) {
        return buildCraftManagerState(cc, level, Text.EMPTY, false);
    }

    /** What a place is called in a window: a card by its name and slot, an interface by its name. */
    public static Text placeTitle(final CraftPlaces.Place place) {
        if (place.isCard()) {
            return CraftTexts.CARD_PLACE.with(GameText.of(place.card().getHoverName()),
                    place.slot() - CraftingComputerBlockEntity.PCIE_SLOTS_START + 1);
        }
        return interfaceTitle(place.part(), place.site());
    }

    /** What an interface is called in a window: its name, or its kind and where it is when it has none. */
    public static Text interfaceTitle(final CraftingInterfacePart part, @Nullable final CraftingFloor.Site site) {
        if (!part.name().isEmpty()) {
            return Text.literal(part.name());
        }
        final BlockPos at = site == null ? BlockPos.ZERO : site.cable();
        return CraftTexts.INTERFACE_AT.with(GameText.of(part.partItem().getHoverName()), at.getX(), at.getY(),
                at.getZ());
    }

    private static void handleRequestCraftManager(final RequestCraftManagerPayload payload, final ServerPlayer player,
                                                  final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        // Self-heal the crafts/ mirror against the cards' ROM before presenting the state.
        reconcileCraftsFolder(cc, level);
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level));
    }

    /**
     * Routes the Crafting Manager state to whichever view asked for it: the desktop program, or the Patterns heading
     * of a network machine's space, which does the same work without a window.
     */
    private static void handleCraftManagerState(final CraftManagerStatePayload payload, final Player player) {
        if (!ComputerTerminalScreen.acceptCraftManager(payload)) {
            CraftingManagerApp.accept(payload);
        }
    }

    /**
     * Loads {@code .craft} files from a removable medium: a bench recipe into a card's ROM, a processing or pipeline
     * recipe into an interface, the one the player chose or else the first with room. With {@code allMissing}, every
     * file the computer does not keep yet is loaded.
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
        final List<String> toLoad = new ArrayList<>();
        if (payload.allMissing()) {
            for (final DiskFilesystem.FileEntry e : DiskFilesystem.list(media, "", FilesystemKind.HIERARCHICAL)) {
                if (e.type() == FileType.CRAFT) {
                    toLoad.add(e.path());
                }
            }
        } else {
            toLoad.addAll(payload.fileNames());
        }
        int loaded = 0;
        int parsed = 0;
        boolean full = false;
        for (final String fileName : toLoad) {
            final Optional<String> content = DiskFilesystem.read(media, fileName);
            if (content.isEmpty()) {
                continue;
            }
            final NetworkRecipe recipe = parse(content.get(), level);
            if (recipe == null) {
                continue;
            }
            parsed++;
            if (keeps(cc, level, recipe)) {
                continue;
            }
            final CraftPlaces places = CraftPlaces.of(cc, level);
            final CraftPlaces.Place chosen = places.at(payload.place());
            final CraftPlaces.Place into = chosen != null && chosen.isCard() == recipe.bench().isPresent()
                    ? chosen : recipe.bench().isPresent() ? places.cardWithRoom() : places.interfaceWithRoom();
            if (into == null || !into.put(recipe)) {
                full = true;
                continue;
            }
            loaded++;
            if (recipe.bench().isPresent()) {
                // A bench recipe is mirrored under crafts/ on the system disk, so it shows in the Files app.
                writeCraftToDisk(cc, craftFileNameFor(recipe.bench().get()) + ".craft", content.get());
            }
        }
        cc.setChanged();
        cc.forgetFloor();
        final Text status;
        if (full) {
            status = CraftTexts.LOADED_ROM_FULL.with(loaded, parsed);
        } else if (loaded > 0) {
            status = (loaded == 1 ? CraftTexts.LOADED_ONE : CraftTexts.LOADED_MANY).with(loaded);
        } else {
            status = (parsed > 0 ? CraftTexts.ALREADY_LOADED : CraftTexts.NOTHING_TO_LOAD).text();
        }
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level, status, full));
    }

    /** Removes the chosen recipes, highest first so an earlier removal does not move a later one. */
    private static void handleRemoveRomCraft(final RemoveRomCraftPayload payload, final ServerPlayer player,
                                             final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        final List<Integer> refs = new ArrayList<>(payload.romIndices());
        refs.sort(Comparator.reverseOrder());
        final CraftPlaces places = CraftPlaces.of(cc, level);
        for (final int ref : refs) {
            final CraftPlaces.Place place = places.at(CraftPlaces.placeOf(ref));
            final NetworkRecipe taken = place == null ? null : place.take(CraftPlaces.entryOf(ref));
            if (taken != null && taken.bench().isPresent()) {
                deleteCraftFromDisk(cc, craftFileNameFor(taken.bench().get()) + ".craft");
            }
        }
        cc.setChanged();
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level));
    }

    /** Moves a recipe to another place of the same kind: a pattern to another interface, a bench recipe to a card. */
    private static void handleMoveCraft(final MoveCraftPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        final CraftPlaces places = CraftPlaces.of(cc, level);
        final CraftPlaces.Place from = places.at(CraftPlaces.placeOf(payload.ref()));
        final CraftPlaces.Place to = places.at(payload.place());
        final int entry = CraftPlaces.entryOf(payload.ref());
        Text status = Text.EMPTY;
        if (from != null && to != null && from != to && entry < from.recipes().size()) {
            final NetworkRecipe recipe = from.recipes().get(entry);
            final boolean fits = to.isCard() == recipe.bench().isPresent() && to.used() < to.capacity()
                    && to.recipes().stream().noneMatch(held -> held.sameRecipe(recipe));
            if (fits && from.take(entry) != null && to.put(recipe)) {
                status = CraftTexts.MOVED.with(recipe.displayText(), placeTitle(to));
            } else {
                status = CraftTexts.NOT_MOVED.with(placeTitle(to));
            }
        }
        cc.setChanged();
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level, status, false));
    }

    /** Sets one thing of an interface from the Interfaces tab, as a hand in its own window would. */
    private static void handleSetCraftInterface(final SetCraftInterfacePayload payload, final ServerPlayer player,
                                                final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof CraftingComputerBlockEntity cc)) {
            return;
        }
        final CraftPlaces.Place place = CraftPlaces.of(cc, level).at(payload.place());
        if (place != null && place.part() != null) {
            switch (payload.setting()) {
                case SetCraftInterfacePayload.PAUSED -> place.part().setPaused(payload.value() != 0, "");
                case SetCraftInterfacePayload.EXCLUSIVE -> place.part().setExclusive(payload.value() != 0, "");
                case SetCraftInterfacePayload.MAX_JOBS -> place.part().setMaxJobs(payload.value(), "");
                default -> {
                    return;
                }
            }
        }
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level));
    }

    /** Writes the chosen recipes as {@code .craft} files onto a removable medium. */
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
        final CraftPlaces places = CraftPlaces.of(cc, level);
        for (final int ref : payload.romIndices()) {
            final CraftPlaces.Place place = places.at(CraftPlaces.placeOf(ref));
            final int entry = CraftPlaces.entryOf(ref);
            if (place == null || entry >= place.recipes().size()) {
                continue;
            }
            final NetworkRecipe recipe = place.recipes().get(entry);
            final Optional<String> content = serialize(recipe, level);
            if (content.isEmpty()) {
                continue;
            }
            final String base = recipe.bench().isPresent() ? craftFileNameFor(recipe.bench().get())
                    : sanitizeFileBase(recipe.displayName());
            /*
             * A recipe already on the disc under this name is never overwritten: a different one gets the next free
             * suffix, the same one is simply there already. The encoder writes by the same rule.
             */
            final String fileName = DiskFilesystem.uniquePath(media, base, ".craft", content.get());
            DiskFilesystem.write(media, fileName, FileType.CRAFT, content.get(), mediaFreeWeightFor(media),
                    FilesystemKind.HIERARCHICAL, level.getGameTime());
        }
        // Propagate the updated filesystem component to the reader slot.
        final String rest = key.substring("media:".length());
        final int slash = rest.indexOf('/');
        final String rawPos = slash < 0 ? rest : rest.substring(0, slash);
        try {
            final long encoded = Long.parseLong(rawPos);
            if (Loaded.blockEntity(level, BlockPos.of(encoded)) instanceof MediaReaderBlockEntity reader) {
                reader.setChanged();
            }
        } catch (final NumberFormatException ignored) {
            // A key that names no reader writes to nothing a reader shows.
        }
        PacketDistributor.sendToPlayer(player, buildCraftManagerState(cc, level));
    }

    /** The same, carrying a status line, and whether that line warns: something the player has to act on. */
    private static CraftManagerStatePayload buildCraftManagerState(final CraftingComputerBlockEntity cc,
                                                                    final ServerLevel level, final Text status,
                                                                    final boolean warns) {
        String mediaVolumeKey = "";
        Text mediaLabel = Text.EMPTY;
        List<String> mediaFiles = List.of();
        /*
         * A computer commonly has more than one drive linked (a floppy drive, a DVD drive, a dock), and the one with
         * a blank medium in it may well come first: the disc the player just wrote is the one they mean.
         */
        for (final long endpoint : cc.enabledEndpoints()) {
            if (Loaded.blockEntity(level, BlockPos.of(endpoint)) instanceof MediaReaderBlockEntity reader) {
                final ItemStack m = reader.mediaSlot().getStackInSlot(0);
                if (m.isEmpty() || !(m.getItem() instanceof FormattedMediaItem fmt) || !fmt.writable()) {
                    continue;
                }
                final List<String> files = craftFileListFromMedia(m);
                if (mediaVolumeKey.isEmpty() || !files.isEmpty()) {
                    mediaVolumeKey = "media:" + endpoint;
                    final String label = VolumeLabel.of(m, "");
                    mediaLabel = label.isEmpty() ? CraftTexts.REMOVABLE_DRIVE.text() : Text.literal(wire(label, 64));
                    mediaFiles = files;
                }
                if (!files.isEmpty()) {
                    break;
                }
            }
        }
        final Set<String> onMedia = new HashSet<>(mediaFiles);
        final CraftPlaces places = CraftPlaces.of(cc, level);
        final List<CraftManagerStatePayload.WirePlace> wirePlaces = new ArrayList<>();
        final List<CraftManagerStatePayload.WireInterface> interfaces = new ArrayList<>();
        final MainframeBlockEntity mainframe = cc.networkUuid() == null ? null
                : NetworkLookup.resolveMainframe(level, cc.networkUuid());
        final CraftingFloor floor = cc.floor();
        for (int p = 0; p < places.all().size(); p++) {
            final CraftPlaces.Place place = places.all().get(p);
            final List<CraftManagerStatePayload.WireRomEntry> entries = new ArrayList<>();
            final List<NetworkRecipe> recipes = place.recipes();
            for (int e = 0; e < recipes.size(); e++) {
                final NetworkRecipe recipe = recipes.get(e);
                final String fileName = (recipe.bench().isPresent() ? craftFileNameFor(recipe.bench().get())
                        : sanitizeFileBase(recipe.displayName())) + ".craft";
                entries.add(new CraftManagerStatePayload.WireRomEntry(CraftPlaces.ref(p, e), recipe.displayText(),
                        onMedia.contains(fileName), recipe.bench().isPresent() ? CraftManagerStatePayload.BENCH
                                : recipe.multi().isPresent() ? CraftManagerStatePayload.PIPELINE
                                : CraftManagerStatePayload.PROCESSING));
            }
            final int drives = place.isCard() && place.card().getItem() instanceof CraftingCardItem card
                    ? card.spec().interfaces() : 0;
            wirePlaces.add(new CraftManagerStatePayload.WirePlace(place.isCard(), placeTitle(place), place.used(),
                    place.capacity(), drives, entries));
            if (!place.isCard() && floor != null) {
                interfaces.add(interfaceRow(level, p, place, floor, mainframe));
            }
        }
        final int waiting = floor == null ? 0 : floor.interfaces().size() - floor.driven().size();
        return new CraftManagerStatePayload(mediaVolumeKey, mediaLabel, mediaFiles, wirePlaces,
                cc.craftingCardFactor() > 0.0, status, warns, interfaces, cc.interfaceBudget(), Math.max(0, waiting));
    }

    /* One interface as the Interfaces tab lists it: its machine, how full it is, its mode and what it is doing. */
    private static CraftManagerStatePayload.WireInterface interfaceRow(
            final ServerLevel level, final int index, final CraftPlaces.Place place, final CraftingFloor floor,
            @Nullable final MainframeBlockEntity mainframe) {
        final CraftingInterfacePart part = place.part();
        final CraftingFloor.Reach reach = floor.reach(place.site());
        final Text machine = reach.machine() == null ? Text.EMPTY
                : GameText.of(level.getBlockState(reach.machine()).getBlock().getName());
        final List<NetworkProcessingOperation> running = mainframe == null ? List.of()
                : mainframe.craftJobsOn(part.id());
        final byte state;
        Text detail = Text.EMPTY;
        if (part.paused()) {
            state = CraftManagerStatePayload.PAUSED;
        } else if (reach.machine() == null) {
            state = CraftManagerStatePayload.NO_MACHINE;
        } else if (!running.isEmpty()) {
            state = CraftManagerStatePayload.RUNNING;
            detail = running.size() == 1 ? CraftTexts.RUNNING_ONE.with(jobText(running.get(0)))
                    : CraftTexts.RUNNING_MANY.with(running.size());
        } else if (!part.owed().isEmpty()) {
            state = CraftManagerStatePayload.DRAINING;
            detail = CraftTexts.DRAINING.text();
        } else {
            state = CraftManagerStatePayload.IDLE;
        }
        return new CraftManagerStatePayload.WireInterface(index, interfaceTitle(part, place.site()), machine,
                part.patterns().size(), part.capacity(), CraftingDispatch.exclusive(part, reach), state, detail,
                part.maxJobs());
    }

    /** A job as a window names it: what it makes and how many, "Bronze Ingot x16". */
    public static Text jobText(final NetworkProcessingOperation job) {
        return CraftTexts.JOB.with(job.pattern().displayText(), job.requested());
    }

    /* Whether the computer keeps {@code recipe} already: a card's ROM, or an interface it drives. */
    private static boolean keeps(final CraftingComputerBlockEntity cc, final ServerLevel level,
                                 final NetworkRecipe recipe) {
        for (final CraftPlaces.Place place : CraftPlaces.of(cc, level).all()) {
            if (place.recipes().stream().anyMatch(held -> held.sameRecipe(recipe))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static NetworkRecipe parse(final String content, final ServerLevel level) {
        return switch (CraftFile.typeOf(content)) {
            case "proc" -> CraftFile.parseProcessing(content, level.registryAccess())
                    .map(NetworkRecipe::ofProcessing).orElse(null);
            case "multi" -> CraftFile.parseMultiStage(content, level.registryAccess())
                    .map(NetworkRecipe::ofMultiStage).orElse(null);
            default -> CraftFile.parse(content, level.registryAccess()).map(NetworkRecipe::ofBench).orElse(null);
        };
    }

    private static Optional<String> serialize(final NetworkRecipe recipe, final ServerLevel level) {
        if (recipe.proc().isPresent()) {
            return CraftFile.serializeProcessing(recipe.proc().get(), level.registryAccess());
        }
        if (recipe.multi().isPresent()) {
            return CraftFile.serializeMultiStage(recipe.multi().get(), level.registryAccess());
        }
        return recipe.bench().isPresent() ? CraftFile.serialize(recipe.bench().get(), level.registryAccess())
                : Optional.empty();
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
