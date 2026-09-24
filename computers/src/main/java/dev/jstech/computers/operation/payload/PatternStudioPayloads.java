/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import static dev.jstech.computers.operation.payload.PatternStudioTexts.BAY_EMPTY;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.BENCH_NOT_RECIPE;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.BENCH_STAGE_ADDED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.CANNOT_READ;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.CARD_REQUIRED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.COULD_NOT_ADD;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.DRAFT_INCOMPLETE;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.DRIVE_MEDIUM;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.EJECTED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.ENCODER_BAY_EMPTY;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.ENCODER_QUEUE_FULL;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.ENCODER_WRITING;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.IN_FOLDER;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.LOADED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.MACHINE_INCOMPLETE;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.MACHINE_STAGE_ADDED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.NOT_LOADED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.NOT_MACHINE_RECIPE;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.NOT_PIPELINE;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.NOT_RECIPE_FILE;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.NO_ENCODER;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.NO_PATTERN_ENCODER;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.NO_SYSTEM_DISK;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.ONLY_CRAFTING_COMPUTER;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.OPENED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.PIPELINE_EMPTY;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.PIPELINE_IN_PIPELINE;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.QUEUE_CLEARED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.SAVED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.SAVE_FAILED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.SENT;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.STAGE_ADDED;
import static dev.jstech.computers.operation.payload.PatternStudioTexts.SYSTEM_DISK;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingSwitchBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.client.os.PatternStudioApp;
import dev.jstech.computers.crafting.AnyTagResolver;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.MachineCategory;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.crafting.RecipeBook;
import dev.jstech.computers.crafting.RecipeMachines;
import dev.jstech.computers.operation.payload.crafting.CraftFilesOnDisk;
import dev.jstech.computers.operation.payload.crafting.CraftManagerPayloads;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.operation.payload.terminal.TerminalHosts;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.VolumeLabel;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The Pattern Studio's server side: answers a window's request with the workbench state, applies its edits,
 * serves the Recipe Book, and carries a finished draft to where it goes: the linked encoder, the system disk's
 * crafts folder, or this Crafting Computer's Recipe ROM. Every handler resolves the host through the monitor
 * the player is at, so a far-away computer cannot be edited by sending its position.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class PatternStudioPayloads {

    /** The system disk drive key in the Studio's file list. */
    public static final String DISK_KEY = "disk";
    private static final String CRAFTS_DIR = "crafts";

    private PatternStudioPayloads() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        ComputerAccess.accept(registrar, RequestPatternStudioPayload.TYPE, RequestPatternStudioPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestPatternStudioPayload::host), PatternStudioPayloads::handleRequest);
        ComputerAccess.accept(registrar, PatternStudioEditPayload.TYPE, PatternStudioEditPayload.STREAM_CODEC,
                ComputerAccess.machine(PatternStudioEditPayload::host), PatternStudioPayloads::handleEdit);
        registrar.playToClient(PatternStudioStatePayload.TYPE, PatternStudioStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> {
                    /*
                     * The Patterns heading of a network machine's space does the Studio's work without the
                     * Studio's window, so it asks the same question and takes the same answer.
                     */
                    if (!ComputerTerminalScreen.acceptPatternStudio(payload)) {
                        PatternStudioApp.accept(payload);
                    }
                }));
    }

    // resolution

    /** The host at {@code hostPos} as an OS host with a workbench, if the player is at one of its monitors. */
    @Nullable
    public static IOsHost studioHost(final ServerPlayer player, final ServerLevel level, final BlockPos hostPos,
                                    final BlockPos monitorPos) {
        final var terminal = TerminalHosts.niHost(player, level, hostPos, monitorPos);
        return terminal instanceof IOsHost host && host.studio() != null ? host : null;
    }

    private static void handleRequest(final RequestPatternStudioPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        final IOsHost host = studioHost(player, level, payload.host(), payload.monitorPos());
        if (host == null) {
            return;
        }
        host.studio().refreshPreview(level);
        PacketDistributor.sendToPlayer(player, buildState(level, host, Text.EMPTY, -1));
    }

    // edits

    private static void handleEdit(final PatternStudioEditPayload payload, final ServerPlayer player,
                                   final ServerLevel level) {
        final IOsHost host = studioHost(player, level, payload.host(), payload.monitorPos());
        if (host == null) {
            return;
        }
        final PatternWorkbench studio = host.studio();
        Text status = Text.EMPTY;
        int tab = -1;
        /*
         * A ghost cell takes what the player carries when the click came with nothing named: a recipe
         * viewer's drop names the item itself, a click on a cell names the cursor.
         */
        final ItemStack carried = player.containerMenu.getCarried();
        final ItemStack item = !payload.item().isEmpty() ? payload.item() : carried;
        switch (payload.action()) {
            case PatternStudioEditPayload.BENCH_SET_CELL -> {
                studio.setGhost(payload.index(), item);
                studio.forget(PatternWorkbench.Kind.BENCH);
            }
            case PatternStudioEditPayload.BENCH_CLEAR_CELL -> studio.setGhost(payload.index(), ItemStack.EMPTY);
            case PatternStudioEditPayload.BENCH_SET_TAG -> studio.setAnyTag(payload.index(), payload.text());
            case PatternStudioEditPayload.BENCH_SET_NAME -> studio.setBenchName(payload.text(), payload.text2());
            case PatternStudioEditPayload.BENCH_CLEAR -> studio.clearBench();
            case PatternStudioEditPayload.PROC_SET_INPUT -> {
                studio.setProcCell(false, payload.index(), PatternWorkbench.DataCell.fromStack(item));
                studio.forget(PatternWorkbench.Kind.MACHINE);
            }
            case PatternStudioEditPayload.PROC_SET_OUTPUT -> {
                studio.setProcCell(true, payload.index(), PatternWorkbench.DataCell.fromStack(item));
                studio.forget(PatternWorkbench.Kind.MACHINE);
            }
            case PatternStudioEditPayload.PROC_CLEAR_INPUT -> studio.setProcCell(false, payload.index(), null);
            case PatternStudioEditPayload.PROC_CLEAR_OUTPUT -> studio.setProcCell(true, payload.index(), null);
            case PatternStudioEditPayload.PROC_SET_INPUT_AMOUNT ->
                    studio.setProcAmount(false, payload.index(), payload.value());
            case PatternStudioEditPayload.PROC_SET_OUTPUT_AMOUNT ->
                    studio.setProcAmount(true, payload.index(), payload.value());
            case PatternStudioEditPayload.PROC_SET_CHANCE -> studio.setOutputChance(payload.index(), (int) payload.value());
            case PatternStudioEditPayload.PROC_SET_MACHINE -> studio.setMachineType(payload.text());
            case PatternStudioEditPayload.PROC_SET_TIMEOUT -> studio.setProcTimeout((int) payload.value());
            case PatternStudioEditPayload.PROC_SET_NAME -> studio.setProcName(payload.text(), payload.text2());
            case PatternStudioEditPayload.PROC_CLEAR -> studio.clearMachine();
            case PatternStudioEditPayload.PIPE_ADD_BENCH -> {
                studio.refreshPreview(level);
                status = (studio.addBenchStage() ? BENCH_STAGE_ADDED : BENCH_NOT_RECIPE).text();
                tab = 2;
            }
            case PatternStudioEditPayload.PIPE_ADD_PROC -> {
                status = (studio.addProcessingStage() ? MACHINE_STAGE_ADDED : MACHINE_INCOMPLETE).text();
                tab = 2;
            }
            case PatternStudioEditPayload.PIPE_ADD_FILE -> {
                status = addStageFromFile(level, host, payload.text(), payload.text2());
                tab = 2;
            }
            case PatternStudioEditPayload.PIPE_REMOVE -> studio.removeStage(payload.index());
            case PatternStudioEditPayload.PIPE_SET_NAME -> studio.setPipelineName(payload.text(), payload.text2());
            case PatternStudioEditPayload.PIPE_CLEAR -> studio.clearPipeline();
            case PatternStudioEditPayload.OPEN_FILE -> {
                final int[] opened = {-1};
                status = openFile(level, host, payload.text(), payload.text2(), opened);
                tab = opened[0];
            }
            case PatternStudioEditPayload.SAVE_TO_DISK -> status = saveToDisk(level, host, PatternWorkbench.Kind.byId(payload.index()));
            case PatternStudioEditPayload.LOAD_INTO_ROM -> status = loadIntoRom(level, host, PatternWorkbench.Kind.byId(payload.index()));
            case PatternStudioEditPayload.BURN -> status = burn(level, host, PatternWorkbench.Kind.byId(payload.index()));
            case PatternStudioEditPayload.ENCODER_CANCEL -> {
                final PatternEncoderBlockEntity enc = encoderOf(level, host);
                if (enc != null) {
                    enc.cancelAll();
                    status = QUEUE_CLEARED.text();
                }
            }
            case PatternStudioEditPayload.ENCODER_EJECT -> {
                final PatternEncoderBlockEntity enc = encoderOf(level, host);
                if (enc == null) {
                    status = NO_ENCODER.text();
                } else if (enc.locked()) {
                    status = ENCODER_WRITING.text();
                } else {
                    final ItemStack out = enc.ejectMedia();
                    if (!out.isEmpty() && !player.addItem(out)) {
                        player.drop(out, false);
                    }
                    status = (out.isEmpty() ? BAY_EMPTY : EJECTED).text();
                }
            }
            default -> {
                return;
            }
        }
        studio.refreshPreview(level);
        host.setChanged();
        PacketDistributor.sendToPlayer(player, buildState(level, host, status, tab));
    }

    /** Fills the bench from a recipe viewer's transfer: the nine cells, tags cleared. */
    public static void applyBenchGrid(final IOsHost host, final ServerLevel level, final List<ItemStack> grid,
                                      final String recipeId) {
        final PatternWorkbench studio = host.studio();
        if (studio == null) {
            return;
        }
        // The viewer sends the cells it showed; the recipe itself says which of them accept a tag.
        List<String> tags = null;
        final ResourceLocation id = recipeId == null || recipeId.isEmpty() ? null : ResourceLocation.tryParse(recipeId);
        if (id != null) {
            final var holder = level.getRecipeManager().byKey(id);
            if (holder.isPresent() && holder.get().value() instanceof CraftingRecipe recipe) {
                tags = RecipeBook.benchTags(recipe);
            }
        }
        studio.setGrid(grid, tags);
        studio.forget(PatternWorkbench.Kind.BENCH);
        studio.refreshPreview(level);
        host.setChanged();
    }

    /**
     * Fills the machine draft from a recipe viewer's transfer, paired with the machine the data maps the
     * recipe type to (and the network declares), when there is one.
     */
    public static void applyProcessingCells(final IOsHost host, final ServerLevel level,
                                            final List<PatternWorkbench.DataCell> inputs,
                                            final List<PatternWorkbench.DataCell> outputs, final String recipeType) {
        final PatternWorkbench studio = host.studio();
        if (studio == null) {
            return;
        }
        final String machine = defaultMachine(level, host, recipeType);
        studio.applyProcessingCells(inputs, outputs, machine.isEmpty() ? null : machine);
        studio.forget(PatternWorkbench.Kind.MACHINE);
        host.setChanged();
    }

    // files

    /** The medium behind a drive key: a linked reader's disc, or the system disk. Empty when there is none. */
    private static ItemStack volumeFor(final ServerLevel level, final IOsHost host, final String key) {
        if (DISK_KEY.equals(key)) {
            return host.systemDisk();
        }
        return key.startsWith("media:") ? FileAccess.mediaStackFor(level, host, key) : ItemStack.EMPTY;
    }

    /** The path a craft file name has on {@code key}: under the crafts folder on a hierarchical system disk. */
    private static String pathOn(final IOsHost host, final String key, final String fileName) {
        if (DISK_KEY.equals(key) && FileAccess.filesystemKindOf(host) == FilesystemKind.HIERARCHICAL) {
            return CRAFTS_DIR + "/" + fileName;
        }
        return fileName;
    }

    private static Optional<String> readCraft(final ServerLevel level, final IOsHost host, final String key,
                                              final String fileName) {
        final ItemStack volume = volumeFor(level, host, key);
        if (volume.isEmpty() || fileName.isBlank() || fileName.contains("..")) {
            return Optional.empty();
        }
        return DiskFilesystem.read(volume, pathOn(host, key, fileName));
    }

    private static Text openFile(final ServerLevel level, final IOsHost host, final String key,
                                 final String fileName, final int[] tab) {
        final Optional<String> content = readCraft(level, host, key, fileName);
        if (content.isEmpty()) {
            return CANNOT_READ.with(fileName);
        }
        final PatternWorkbench studio = host.studio();
        final var registries = level.registryAccess();
        switch (CraftFile.typeOf(content.get())) {
            case "proc" -> {
                final Optional<ProcessingPattern> p = CraftFile.parseProcessing(content.get(), registries);
                if (p.isEmpty()) {
                    return NOT_MACHINE_RECIPE.with(fileName);
                }
                studio.loadMachine(p.get(), key, fileName);
                tab[0] = 1;
            }
            case "multi" -> {
                final Optional<MultiStagePattern> p = CraftFile.parseMultiStage(content.get(), registries);
                if (p.isEmpty()) {
                    return NOT_PIPELINE.with(fileName);
                }
                studio.loadPipeline(p.get(), key, fileName);
                tab[0] = 2;
            }
            default -> {
                final Optional<CraftingPattern> p = CraftFile.parse(content.get(), registries);
                if (p.isEmpty()) {
                    return NOT_RECIPE_FILE.with(fileName);
                }
                studio.loadBench(p.get(), key, fileName);
                tab[0] = 0;
            }
        }
        return OPENED.with(fileName);
    }

    private static Text addStageFromFile(final ServerLevel level, final IOsHost host, final String key,
                                         final String fileName) {
        final Optional<String> content = readCraft(level, host, key, fileName);
        if (content.isEmpty()) {
            return CANNOT_READ.with(fileName);
        }
        final var registries = level.registryAccess();
        final PatternWorkbench studio = host.studio();
        return switch (CraftFile.typeOf(content.get())) {
            case "proc" -> CraftFile.parseProcessing(content.get(), registries)
                    .map(p -> studio.addStage(MultiStagePattern.Stage.proc(p)) ? STAGE_ADDED.with(p.displayName())
                            : COULD_NOT_ADD.with(fileName))
                    .orElse(NOT_MACHINE_RECIPE.with(fileName));
            case "multi" -> PIPELINE_IN_PIPELINE.text();
            default -> CraftFile.parse(content.get(), registries)
                    .map(p -> studio.addStage(MultiStagePattern.Stage.bench(p)) ? STAGE_ADDED.with(p.displayName())
                            : COULD_NOT_ADD.with(fileName))
                    .orElse(NOT_RECIPE_FILE.with(fileName));
        };
    }

    /** The file base name a draft is written under: its name, sanitized, else its result's registry path. */
    private static String fileBaseFor(final PatternWorkbench studio, final PatternWorkbench.Kind kind) {
        final String name = studio.draftName(kind);
        if (!name.isBlank()) {
            return CraftFilesOnDisk.sanitizeFileBase(name);
        }
        return switch (kind) {
            case BENCH -> "pattern";
            case MACHINE -> "machine";
            case PIPELINE -> "pipeline";
        };
    }

    private static Text saveToDisk(final ServerLevel level, final IOsHost host, final PatternWorkbench.Kind kind) {
        final PatternWorkbench studio = host.studio();
        if (kind == PatternWorkbench.Kind.BENCH) {
            studio.refreshPreview(level);
        }
        final Optional<String> content = studio.serialize(kind, level.registryAccess());
        if (content.isEmpty()) {
            return DRAFT_INCOMPLETE.text();
        }
        final ItemStack disk = host.systemDisk();
        final FilesystemKind fs = FileAccess.filesystemKindOf(host);
        if (disk.isEmpty() || fs == FilesystemKind.NONE) {
            return NO_SYSTEM_DISK.text();
        }
        final String base;
        if (fs == FilesystemKind.HIERARCHICAL) {
            DiskFilesystem.mkdir(disk, CRAFTS_DIR, fs);
            base = CRAFTS_DIR + "/" + fileBaseFor(studio, kind);
        } else {
            base = fileBaseFor(studio, kind);
        }
        final String path = DiskFilesystem.uniquePath(disk, base, ".craft", content.get());
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, path, FileType.CRAFT, content.get(),
                host.systemDiskFreeWeight(), fs, level.getGameTime());
        if (result != DiskFilesystem.WriteResult.OK) {
            // The filesystem's own word for what went wrong, as it names it.
            return SAVE_FAILED.with(result.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        }
        host.setChanged();
        final String fileName = path.substring(path.lastIndexOf('/') + 1);
        studio.remember(kind, DISK_KEY, fileName);
        return SAVED.with(fileName);
    }

    private static Text loadIntoRom(final ServerLevel level, final IOsHost host, final PatternWorkbench.Kind kind) {
        if (!(host instanceof CraftingComputerBlockEntity cc)) {
            return ONLY_CRAFTING_COMPUTER.text();
        }
        if (cc.craftingCardFactor() <= 0.0) {
            return CARD_REQUIRED.text();
        }
        final PatternWorkbench studio = host.studio();
        final boolean loaded;
        final String name;
        switch (kind) {
            case BENCH -> {
                studio.refreshPreview(level);
                final CraftingPattern p = studio.benchPattern();
                if (p == null) {
                    return BENCH_NOT_RECIPE.text();
                }
                loaded = cc.loadPattern(p);
                name = p.displayName();
            }
            case MACHINE -> {
                if (!studio.machineComplete()) {
                    return MACHINE_INCOMPLETE.text();
                }
                final ProcessingPattern p = studio.processingPattern();
                loaded = cc.loadMachineRecipe(NetworkRecipe.ofProcessing(p));
                name = p.displayName();
            }
            default -> {
                if (!studio.pipelineComplete()) {
                    return PIPELINE_EMPTY.text();
                }
                final MultiStagePattern p = studio.multiStagePattern();
                loaded = cc.loadMachineRecipe(NetworkRecipe.ofMultiStage(p));
                name = p.displayName();
            }
        }
        if (!loaded) {
            return NOT_LOADED.text();
        }
        CraftFilesOnDisk.reconcileCraftsFolder(cc, level);
        return LOADED.with(name);
    }

    private static Text burn(final ServerLevel level, final IOsHost host, final PatternWorkbench.Kind kind) {
        final PatternEncoderBlockEntity encoder = encoderOf(level, host);
        if (encoder == null) {
            return NO_PATTERN_ENCODER.text();
        }
        final PatternWorkbench studio = host.studio();
        if (kind == PatternWorkbench.Kind.BENCH) {
            studio.refreshPreview(level);
        }
        final Optional<String> content = studio.serialize(kind, level.registryAccess());
        if (content.isEmpty()) {
            return DRAFT_INCOMPLETE.text();
        }
        if (!encoder.hasMedia()) {
            return ENCODER_BAY_EMPTY.text();
        }
        final String base = fileBaseFor(studio, kind);
        if (!encoder.queueBurn(base, content.get())) {
            return ENCODER_QUEUE_FULL.text();
        }
        return SENT.with(base);
    }

    /**
     * The machine a transferred recipe of type {@code typeId} is paired with: the first mapped machine the
     * network actually declares, else the first mapped machine, else the recipe type's generic category; empty
     * when the type is unknown, leaving the choice to the picker.
     */
    static String defaultMachine(final ServerLevel level, final IOsHost host, final String typeId) {
        if (typeId == null || typeId.isEmpty()) {
            return "";
        }
        final List<String> mapped = RecipeMachines.machinesFor(typeId);
        final Map<String, String> declared = declaredMachines(level, host);
        for (final String m : mapped) {
            if (declared.containsKey(m)) {
                return m;
            }
        }
        if (!mapped.isEmpty()) {
            return mapped.get(0);
        }
        return MachineCategory.genericIdOf(typeId);
    }

    // state

    @Nullable
    static PatternEncoderBlockEntity encoderOf(final ServerLevel level, final IOsHost host) {
        for (final long endpoint : host.linkedEndpoints()) {
            if (level.getBlockEntity(BlockPos.of(endpoint)) instanceof PatternEncoderBlockEntity encoder) {
                return encoder;
            }
        }
        return null;
    }

    /** The machine types the network's switches declare, keyed by type with a readable label. */
    private static Map<String, String> declaredMachines(final ServerLevel level, final IOsHost host) {
        final Map<String, String> out = new LinkedHashMap<>();
        final List<CraftingComputerBlockEntity> computers = new ArrayList<>();
        if (host instanceof CraftingComputerBlockEntity cc) {
            computers.add(cc);
        }
        final MainframeBlockEntity mf = host.networkUuid() == null ? null
                : NetworkLookup.resolveMainframe(level, host.networkUuid());
        if (mf != null) {
            for (final BlockPos pos : mf.craftingComputerPositions()) {
                final BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CraftingComputerBlockEntity cc && !computers.contains(cc)) {
                    computers.add(cc);
                }
            }
        }
        for (final CraftingComputerBlockEntity cc : computers) {
            for (final CraftingSwitchBlockEntity.DeclaredMachine dm : cc.availableMachines()) {
                final String type = dm.machineType();
                if (type == null || type.isBlank() || out.containsKey(type)) {
                    continue;
                }
                out.put(type, !dm.name().isBlank() ? dm.name() : type);
            }
        }
        return out;
    }

    public static PatternStudioStatePayload buildState(final ServerLevel level, final IOsHost host,
                                                       final Text status, final int tabHint) {
        final PatternWorkbench studio = host.studio();
        final MainframeBlockEntity mf = host.networkUuid() == null ? null
                : NetworkLookup.resolveMainframe(level, host.networkUuid());
        final Map<StorageKey, Long> stock = mf == null ? Map.of() : mf.networkIndex().snapshot();

        // Bench: each cell with what an "any" cell would use right now, and how much of it the network holds.
        final List<PatternStudioStatePayload.BenchCell> bench = new ArrayList<>();
        final List<ItemStack> grid = studio.grid();
        final List<String> tags = studio.anyTags();
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            final ItemStack cell = grid.get(i);
            final String tag = tags.get(i);
            ItemStack resolved = ItemStack.EMPTY;
            long count = 0L;
            if (!cell.isEmpty()) {
                if (!tag.isEmpty()) {
                    final ItemStack pick = AnyTagResolver.mostStocked(tag, stock);
                    resolved = pick == null || pick.isEmpty() ? ItemStack.EMPTY : pick.copyWithCount(1);
                }
                final ItemStack counted = resolved.isEmpty() ? cell : resolved;
                count = stock.getOrDefault(StorageKey.of(counted), 0L);
            }
            bench.add(new PatternStudioStatePayload.BenchCell(cell, wire(tag, PatternStudioStatePayload.MAX_TAG),
                    resolved, count));
        }

        final List<PatternStudioStatePayload.ProcCell> inputs = new ArrayList<>();
        final List<PatternStudioStatePayload.ProcCell> outputs = new ArrayList<>();
        for (int i = 0; i < PatternWorkbench.PROC_GRID; i++) {
            final PatternWorkbench.DataCell in = studio.procInput(i);
            if (in != null) {
                inputs.add(new PatternStudioStatePayload.ProcCell(i, in, ProcessingPattern.FULL_CHANCE,
                        stock.getOrDefault(in.key(), 0L)));
            }
            final PatternWorkbench.DataCell out = studio.procOutput(i);
            if (out != null) {
                outputs.add(new PatternStudioStatePayload.ProcCell(i, out, studio.outputChance(i),
                        stock.getOrDefault(out.key(), 0L)));
            }
        }

        final List<PatternStudioStatePayload.Stage> stages = new ArrayList<>();
        for (final MultiStagePattern.Stage stage : studio.stages()) {
            if (stages.size() >= PatternStudioStatePayload.MAX_STAGES) {
                break;
            }
            if (stage.bench().isPresent()) {
                final CraftingPattern p = stage.bench().get();
                stages.add(new PatternStudioStatePayload.Stage(p.displayText(), true, p.result()));
            } else if (stage.proc().isPresent()) {
                final ProcessingPattern p = stage.proc().get();
                final ProcessingPattern.ProcessingOutput primary = p.primaryOutput();
                stages.add(new PatternStudioStatePayload.Stage(p.displayText(), false, primary != null && primary.key().isItem() ? primary.key().stack(1) : ItemStack.EMPTY));
            }
        }

        // Drives: every linked reader holding a medium, then the system disk's crafts folder.
        final List<PatternStudioStatePayload.Drive> drives = new ArrayList<>();
        for (final long endpoint : host.linkedEndpoints()) {
            if (drives.size() >= PatternStudioStatePayload.MAX_DRIVES - 1) {
                break;
            }
            if (level.getBlockEntity(BlockPos.of(endpoint)) instanceof MediaReaderBlockEntity reader) {
                final ItemStack m = reader.mediaSlot().getStackInSlot(0);
                if (m.isEmpty() || !(m.getItem() instanceof FormattedMediaItem fmt)) {
                    continue;
                }
                // "CD Drive: Blank CD-RW": the reader, then the medium's own label or name.
                final Text drive = GameText.of(level.getBlockState(BlockPos.of(endpoint)).getBlock().getName());
                drives.add(new PatternStudioStatePayload.Drive("media:" + endpoint,
                        DRIVE_MEDIUM.with(drive, VolumeLabel.text(m)),
                        fmt.writable(), CraftManagerPayloads.craftFileListFromMedia(m)));
            }
        }
        final ItemStack disk = host.systemDisk();
        final FilesystemKind fs = FileAccess.filesystemKindOf(host);
        if (!disk.isEmpty() && fs != FilesystemKind.NONE) {
            final List<String> files = new ArrayList<>();
            final String dir = fs == FilesystemKind.HIERARCHICAL ? CRAFTS_DIR : "";
            for (final DiskFilesystem.FileEntry e : DiskFilesystem.list(disk, dir, fs)) {
                final String name = e.path().substring(e.path().lastIndexOf('/') + 1);
                if (e.type() == FileType.CRAFT && files.size() < PatternStudioStatePayload.MAX_FILES
                        && name.length() <= PatternStudioStatePayload.MAX_NAME) {
                    files.add(name);
                }
            }
            final String label = VolumeLabel.of(disk, "");
            drives.add(new PatternStudioStatePayload.Drive(DISK_KEY,
                    IN_FOLDER.with(label.isEmpty() ? SYSTEM_DISK.text() : Text.literal(label), CRAFTS_DIR),
                    true, files));
        }

        final PatternEncoderBlockEntity enc = encoderOf(level, host);
        final PatternStudioStatePayload.Encoder encoder = enc == null ? PatternStudioStatePayload.Encoder.none()
                : new PatternStudioStatePayload.Encoder(true, enc.era().text(),
                        enc.hasMedia() ? VolumeLabel.text(enc.mediaStack()) : Text.EMPTY,
                        enc.statusLine(), enc.progressPercent(), enc.queued(), enc.busy(),
                        enc.phase() == PatternEncoderBlockEntity.Phase.ERROR);

        final List<PatternStudioStatePayload.Machine> machines = new ArrayList<>();
        for (final Map.Entry<String, String> e : declaredMachines(level, host).entrySet()) {
            if (machines.size() >= PatternStudioStatePayload.MAX_MACHINES) {
                break;
            }
            machines.add(new PatternStudioStatePayload.Machine(wire(e.getKey(), PatternStudioStatePayload.MAX_KEY),
                    wire(e.getValue(), PatternStudioStatePayload.MAX_LABEL)));
        }

        boolean romBench = false;
        boolean romProc = false;
        boolean romPipe = false;
        boolean hasCard = false;
        final boolean craftingComputer = host instanceof CraftingComputerBlockEntity;
        if (host instanceof CraftingComputerBlockEntity cc) {
            hasCard = cc.craftingCardFactor() > 0.0;
            final CraftingPattern benchPattern = studio.benchPattern();
            romBench = benchPattern != null && cc.romContains(benchPattern);
            if (studio.machineComplete()) {
                final NetworkRecipe r = NetworkRecipe.ofProcessing(studio.processingPattern());
                for (final NetworkRecipe existing : cc.machineRecipes()) {
                    if (existing.sameRecipe(r)) {
                        romProc = true;
                        break;
                    }
                }
            }
            if (studio.pipelineComplete()) {
                final NetworkRecipe r = NetworkRecipe.ofMultiStage(studio.multiStagePattern());
                for (final NetworkRecipe existing : cc.machineRecipes()) {
                    if (existing.sameRecipe(r)) {
                        romPipe = true;
                        break;
                    }
                }
            }
        }

        return new PatternStudioStatePayload(bench, studio.preview(),
                wire(studio.benchName(), PatternStudioStatePayload.MAX_NAME),
                wire(studio.benchNote(), PatternStudioStatePayload.MAX_NOTE),
                wire(studio.openedFile(PatternWorkbench.Kind.BENCH), PatternStudioStatePayload.MAX_NAME),
                inputs, outputs, wire(studio.machineType(), PatternStudioStatePayload.MAX_KEY), studio.procTimeout(),
                wire(studio.procName(), PatternStudioStatePayload.MAX_NAME),
                wire(studio.procNote(), PatternStudioStatePayload.MAX_NOTE),
                wire(studio.openedFile(PatternWorkbench.Kind.MACHINE), PatternStudioStatePayload.MAX_NAME),
                stages, wire(studio.pipelineName(), PatternStudioStatePayload.MAX_NAME),
                wire(studio.pipelineNote(), PatternStudioStatePayload.MAX_NOTE),
                wire(studio.openedFile(PatternWorkbench.Kind.PIPELINE), PatternStudioStatePayload.MAX_NAME),
                drives, encoder, machines, craftingComputer, hasCard, romBench, romProc, romPipe, status, tabHint);
    }

    private static String wire(final String s, final int max) {
        final String v = s == null ? "" : s;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
