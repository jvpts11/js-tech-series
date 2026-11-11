/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.os.DiskSystems;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.UnixTree;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a backup keeps of a network and how it is put back: the network's items, then for each computer the files in its
 * home folder, its settings, its scripts and its system image, the network's crafting patterns and the Mainframe's
 * saved views, procedures and jobs (computers/docs/design/programs.md). Items come back through the network's book of
 * losses, which the Backup Service reads; everything else is written back as it was kept, over what is there now.
 */
public final class BackupContents {

    private static final String ITEMS = "items";
    private static final String MACHINES = "machines";
    private static final String PATTERNS = "patterns";
    private static final String CATALOG = "catalog";
    private static final String KEY = "key";
    private static final String COUNT = "count";
    private static final String NODE = "node";
    private static final String NAME = "name";
    private static final String FILES = "files";
    private static final String SETTINGS = "settings";
    private static final String SCRIPTS = "scripts";
    private static final String IMAGE = "image";
    private static final String SYSTEMS = "systems";
    private static final String CONSOLE = "console";
    private static final String POS = "pos";
    private static final String ROM = "rom";
    private static final String RECIPES = "recipes";
    private static final String TYPE = "type";
    private static final String BODY = "body";
    private static final String TRIGGER = "trigger";
    private static final String SPEC = "spec";
    private static final String SYSTEM = "system";
    private static final String WEIGHT = "weight";
    /** The scripts a backup of scripts keeps: the network's language, Sigma, Sigma# and Sigma#'s projects. */
    private static final Set<FileType> SCRIPT_TYPES = Set.of(FileType.IQL, FileType.SG, FileType.SGS,
            FileType.SGSPROJ);

    private BackupContents() {
    }

    /** What a backup keeps, what it weighs on a holder of its era, and how many of the network's items it keeps. */
    public record Captured(CompoundTag content, long weight, long items) {
    }

    /**
     * One computer's system image in a backup.
     *
     * @param node    the computer's node
     * @param machine what it was called
     * @param system  the system it ran, by its name
     * @param weight  what the image weighs, in thousandths of an item of the holder's era
     */
    public record Image(UUID node, String machine, String system, long weight) {
    }

    /** Takes a backup of {@code parts} of {@code network} now, weighed as a holder of {@code era} weighs it. */
    public static Captured capture(final ServerLevel level, final NetworkUuid network, final Set<BackupPart> parts,
                                   final Era era) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        final CompoundTag content = new CompoundTag();
        long weight = 0L;
        long items = 0L;
        if (parts.contains(BackupPart.ITEMS)) {
            final ListTag list = new ListTag();
            for (final Map.Entry<StorageKey, Long> held : NetworkStorage.of(level, network).query().entrySet()) {
                final Optional<Tag> key = StorageKey.CODEC.encodeStart(ops, held.getKey()).result();
                if (key.isEmpty() || held.getValue() <= 0L) {
                    continue;
                }
                final CompoundTag one = new CompoundTag();
                one.put(KEY, key.get());
                one.putLong(COUNT, held.getValue());
                list.add(one);
                items += held.getValue();
                weight += held.getKey().weight(held.getValue());
            }
            content.put(ITEMS, list);
        }
        final ListTag machines = new ListTag();
        for (final NetworkMachines.Machine machine : NetworkMachines.of(level, network)) {
            final CompoundTag one = new CompoundTag();
            weight += captureMachine(machine, parts, era, one);
            if (one.size() > 2) {
                machines.add(one);
            }
        }
        content.put(MACHINES, machines);
        if (parts.contains(BackupPart.PATTERNS)) {
            final ListTag patterns = capturePatterns(level, network, ops);
            content.put(PATTERNS, patterns);
            weight += FsPaths.sizeMbEq(patterns.sizeInBytes(), era);
        }
        if (parts.contains(BackupPart.SCRIPTS)) {
            final ListTag catalog = captureCatalog(level, network);
            content.put(CATALOG, catalog);
            weight += FsPaths.sizeMbEq(catalog.sizeInBytes(), era);
        }
        return new Captured(content, weight, items);
    }

    /** The network's items a backup keeps, read back. */
    public static Map<StorageKey, Long> items(final ServerLevel level, final CompoundTag content) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        final Map<StorageKey, Long> out = new LinkedHashMap<>();
        for (final Tag oneTag : content.getList(ITEMS, Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) oneTag;
            StorageKey.CODEC.parse(ops, one.get(KEY)).result()
                    .ifPresent(key -> out.merge(key, one.getLong(COUNT), Long::sum));
        }
        return out;
    }

    /**
     * Puts back what a backup of {@code parts} kept, its items aside, on the computers of {@code network} that are
     * still there: their files, settings and scripts, the systems of the computers {@code images} names, then the
     * network's patterns and the Mainframe's saved definitions. Answers how many computers it reached.
     */
    public static int restore(final ServerLevel level, final NetworkUuid network, final CompoundTag content,
                              final Set<BackupPart> parts, final Set<UUID> images) {
        int reached = 0;
        for (final Tag oneTag : content.getList(MACHINES, Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) oneTag;
            if (!one.hasUUID(NODE)) {
                continue;
            }
            final UUID node = one.getUUID(NODE);
            final NetworkMachines.Machine machine = NetworkMachines.find(level, network, new NodeUuid(node));
            if (machine != null && restoreMachine(machine.host(), one, parts, images.contains(node))) {
                reached++;
            }
        }
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        if (parts.contains(BackupPart.PATTERNS)) {
            restorePatterns(level, content.getList(PATTERNS, Tag.TAG_COMPOUND), ops);
        }
        if (parts.contains(BackupPart.SCRIPTS)) {
            restoreCatalog(level, network, content.getList(CATALOG, Tag.TAG_COMPOUND));
        }
        return reached;
    }

    /** The system images a backup keeps, one for each computer it imaged, in the order it took them. */
    public static List<Image> images(final CompoundTag content) {
        final List<Image> out = new ArrayList<>();
        for (final Tag oneTag : content.getList(MACHINES, Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) oneTag;
            if (one.hasUUID(NODE) && one.contains(IMAGE)) {
                final CompoundTag image = one.getCompound(IMAGE);
                out.add(new Image(one.getUUID(NODE), one.getString(NAME), image.getString(SYSTEM),
                        image.getLong(WEIGHT)));
            }
        }
        return out;
    }

    /** The computers whose share a backup keeps, by name: whose files, settings or system it holds. */
    public static List<String> machines(final CompoundTag content) {
        final List<String> out = new ArrayList<>();
        for (final Tag oneTag : content.getList(MACHINES, Tag.TAG_COMPOUND)) {
            out.add(((CompoundTag) oneTag).getString(NAME));
        }
        return out;
    }

    /** The folder a system keeps its person's files in, without a leading slash; empty for one that keeps none. */
    public static String homeOf(@Nullable final OsDef os) {
        if (os == null || os.platform() == Platform.MC_NET) {
            return "";
        }
        if (os.platform() == Platform.MC_DOS || os.platform() == Platform.FRAMES) {
            return "Users/Public";
        }
        return UnixTree.of(os.platform(), os.familyRank()).homePath();
    }

    /* One computer's share of a backup; answers what it weighs. */
    private static long captureMachine(final NetworkMachines.Machine machine, final Set<BackupPart> parts,
                                       final Era era, final CompoundTag one) {
        final IOsHost host = machine.host();
        one.putUUID(NODE, machine.node().value());
        one.putString(NAME, machine.name());
        // The console travels on the disk: it is written there first, so the disk holds what the machine knows now.
        host.markChanged();
        final ItemStack disk = host.systemDisk();
        final OsDef os = host.installedOs();
        final FilesystemContents fs = disk == null || disk.isEmpty() ? FilesystemContents.EMPTY
                : disk.getOrDefault(ComputingComponents.FILESYSTEM.get(), FilesystemContents.EMPTY);
        long weight = 0L;
        if (parts.contains(BackupPart.FILES)) {
            final String home = homeOf(os);
            if (!home.isEmpty()) {
                final FilesystemContents files = under(fs, home);
                one.put(FILES, encode(files));
                weight += files.usedWeight(era);
            }
        }
        final ComputerConsoleState console = host.console();
        if (parts.contains(BackupPart.SETTINGS) && console != null) {
            final CompoundTag settings = console.settingsTag();
            one.put(SETTINGS, settings);
            weight += FsPaths.sizeMbEq(settings.sizeInBytes(), era);
        }
        if (parts.contains(BackupPart.SCRIPTS)) {
            final FilesystemContents scripts = scriptsIn(fs);
            one.put(SCRIPTS, encode(scripts));
            weight += scripts.usedWeight(era);
        }
        if (parts.contains(BackupPart.IMAGES) && os != null && disk != null && !disk.isEmpty()) {
            final CompoundTag image = new CompoundTag();
            final DiskSystems systems = disk.get(ComputingComponents.DISK_SYSTEMS.get());
            if (systems != null) {
                DiskSystems.CODEC.encodeStart(NbtOps.INSTANCE, systems).result().ifPresent(tag -> image.put(SYSTEMS,
                        tag));
            }
            final FilesystemContents files = systemFiles(fs, os);
            image.put(FILES, encode(files));
            final CompoundTag consoleTag = disk.get(ComputingComponents.DISK_CONSOLE.get());
            if (consoleTag != null) {
                image.put(CONSOLE, consoleTag.copy());
            }
            final long imageWeight = files.usedWeight(era) + FsPaths.sizeMbEq(image.sizeInBytes(), era);
            image.putString(SYSTEM, os.displayName());
            image.putLong(WEIGHT, imageWeight);
            one.put(IMAGE, image);
            weight += imageWeight;
        }
        return weight;
    }

    /* Puts one computer's share back, its image only when it was chosen; answers whether there was anything to. */
    private static boolean restoreMachine(final IOsHost host, final CompoundTag one, final Set<BackupPart> parts,
                                          final boolean imaged) {
        boolean wrote = false;
        if (imaged && parts.contains(BackupPart.IMAGES) && one.contains(IMAGE)) {
            final ItemStack disk = diskFor(host);
            if (disk != null) {
                final CompoundTag image = one.getCompound(IMAGE);
                if (image.contains(SYSTEMS)) {
                    DiskSystems.CODEC.parse(NbtOps.INSTANCE, image.get(SYSTEMS)).result()
                            .ifPresent(systems -> disk.set(ComputingComponents.DISK_SYSTEMS.get(), systems));
                }
                writeFiles(disk, decode(image.get(FILES)));
                if (image.contains(CONSOLE) && host.console() != null) {
                    host.console().load(image.getCompound(CONSOLE));
                }
                wrote = true;
            }
        }
        final ItemStack disk = diskFor(host);
        if (disk != null) {
            if (parts.contains(BackupPart.FILES) && one.contains(FILES)) {
                writeFiles(disk, decode(one.get(FILES)));
                wrote = true;
            }
            if (parts.contains(BackupPart.SCRIPTS) && one.contains(SCRIPTS)) {
                writeFiles(disk, decode(one.get(SCRIPTS)));
                wrote = true;
            }
        }
        if (parts.contains(BackupPart.SETTINGS) && one.contains(SETTINGS) && host.console() != null) {
            host.console().loadSettings(one.getCompound(SETTINGS));
            wrote = true;
        }
        if (wrote) {
            host.markChanged();
        }
        return wrote;
    }

    /* The disk a machine's files go back onto: its system's, or its first when a dead one took the system with it. */
    @Nullable
    private static ItemStack diskFor(final IOsHost host) {
        final ItemStack system = host.systemDisk();
        if (system != null && !system.isEmpty()) {
            return system;
        }
        final List<ItemStack> disks = host.diskStacks();
        return disks.isEmpty() ? null : disks.getFirst();
    }

    private static void writeFiles(final ItemStack disk, final FilesystemContents kept) {
        FilesystemContents now = disk.getOrDefault(ComputingComponents.FILESYSTEM.get(), FilesystemContents.EMPTY);
        for (final String dir : kept.directories()) {
            now = now.withDir(dir);
        }
        for (final StoredFile file : kept.files().values()) {
            now = now.with(file);
        }
        disk.set(ComputingComponents.FILESYSTEM.get(), now);
    }

    /* The files and folders under {@code folder}, the folder itself among them. */
    private static FilesystemContents under(final FilesystemContents fs, final String folder) {
        final String prefix = folder + "/";
        final Map<String, StoredFile> files = new LinkedHashMap<>();
        fs.files().forEach((path, file) -> {
            if (path.startsWith(prefix)) {
                files.put(path, file);
            }
        });
        final Set<String> dirs = new LinkedHashSet<>();
        for (final String dir : fs.directories()) {
            if (dir.equals(folder) || dir.startsWith(prefix)) {
                dirs.add(dir);
            }
        }
        return new FilesystemContents(files, dirs);
    }

    /* Every script on a disk, wherever it is. */
    private static FilesystemContents scriptsIn(final FilesystemContents fs) {
        final Map<String, StoredFile> files = new LinkedHashMap<>();
        fs.files().forEach((path, file) -> {
            if (SCRIPT_TYPES.contains(file.type())) {
                files.put(path, file);
            }
        });
        return new FilesystemContents(files, Set.of());
    }

    /* A system's own files: its folder and its loader, which is what a corrupted system lost. */
    private static FilesystemContents systemFiles(final FilesystemContents fs, final OsDef os) {
        final String folder = SystemIntegrity.folderOf(os);
        final String loader = SystemIntegrity.loaderOf(os);
        final FilesystemContents inFolder = folder == null || folder.isEmpty() ? FilesystemContents.EMPTY
                : under(fs, folder);
        final Map<String, StoredFile> files = new LinkedHashMap<>(inFolder.files());
        if (loader != null && fs.files().containsKey(loader)) {
            files.put(loader, fs.files().get(loader));
        }
        return new FilesystemContents(files, inFolder.directories());
    }

    private static Tag encode(final FilesystemContents files) {
        return FilesystemContents.CODEC.encodeStart(NbtOps.INSTANCE, files).result().orElse(new CompoundTag());
    }

    private static FilesystemContents decode(@Nullable final Tag tag) {
        return tag == null ? FilesystemContents.EMPTY
                : FilesystemContents.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(FilesystemContents.EMPTY);
    }

    /* The bench recipes and machine recipes of every Crafting Computer of the network, by where each stands. */
    private static ListTag capturePatterns(final ServerLevel level, final NetworkUuid network,
                                          final RegistryOps<Tag> ops) {
        final ListTag out = new ListTag();
        for (final NetworkSystem.CraftingComputerNode node : NetworkSystem.get(level).craftingComputersOf(network)) {
            if (!(Loaded.blockEntity(level, BlockPos.of(node.pos())) instanceof CraftingComputerBlockEntity computer)) {
                continue;
            }
            final CompoundTag one = new CompoundTag();
            one.putLong(POS, node.pos());
            final ListTag rom = new ListTag();
            for (final CraftingPattern pattern : computer.romPatterns()) {
                CraftingPattern.CODEC.encodeStart(ops, pattern).result().ifPresent(rom::add);
            }
            final ListTag recipes = new ListTag();
            for (final NetworkRecipe recipe : computer.machineRecipes()) {
                NetworkRecipe.CODEC.encodeStart(ops, recipe).result().ifPresent(recipes::add);
            }
            one.put(ROM, rom);
            one.put(RECIPES, recipes);
            out.add(one);
        }
        return out;
    }

    /* Each Crafting Computer that is still where it stood takes back the recipes it no longer keeps. */
    private static void restorePatterns(final ServerLevel level, final ListTag patterns, final RegistryOps<Tag> ops) {
        for (final Tag oneTag : patterns) {
            final CompoundTag one = (CompoundTag) oneTag;
            if (!(Loaded.blockEntity(level, BlockPos.of(one.getLong(POS))) instanceof CraftingComputerBlockEntity
                    computer)) {
                continue;
            }
            for (final Tag pattern : one.getList(ROM, Tag.TAG_COMPOUND)) {
                CraftingPattern.CODEC.parse(ops, pattern).result().ifPresent(computer::loadPattern);
            }
            final List<NetworkRecipe> held = computer.machineRecipes();
            for (final Tag recipe : one.getList(RECIPES, Tag.TAG_COMPOUND)) {
                NetworkRecipe.CODEC.parse(ops, recipe).result()
                        .filter(kept -> held.stream().noneMatch(now -> now.sameRecipe(kept)))
                        .ifPresent(computer::loadMachineRecipe);
            }
        }
    }

    /* The Mainframe's saved views, procedures and jobs. */
    private static ListTag captureCatalog(final ServerLevel level, final NetworkUuid network) {
        final ListTag out = new ListTag();
        final MainframeBlockEntity mainframe = mainframeOf(level, network);
        if (mainframe == null) {
            return out;
        }
        // By their ids, as the Mainframe saves its catalog.
        for (final IqlSavedObject object : mainframe.iqlCatalog().all()) {
            final CompoundTag one = new CompoundTag();
            one.putByte(TYPE, (byte) object.type().id());
            one.putString(NAME, object.name());
            one.putString(BODY, object.body());
            one.putByte(TRIGGER, (byte) object.triggerKind().id());
            one.putString(SPEC, object.triggerSpec());
            out.add(one);
        }
        return out;
    }

    private static void restoreCatalog(final ServerLevel level, final NetworkUuid network, final ListTag catalog) {
        final MainframeBlockEntity mainframe = mainframeOf(level, network);
        if (mainframe == null) {
            return;
        }
        for (final Tag oneTag : catalog) {
            final CompoundTag one = (CompoundTag) oneTag;
            final IqlDefinition.ObjectType type = IqlDefinition.ObjectType.byId(one.getByte(TYPE));
            if (type != IqlDefinition.ObjectType.NONE) {
                mainframe.iqlCatalog().put(new IqlSavedObject(type, one.getString(NAME), one.getString(BODY),
                        IqlDefinition.TriggerKind.byId(one.getByte(TRIGGER)), one.getString(SPEC)));
            }
        }
        mainframe.markIqlCatalogChanged();
    }

    @Nullable
    private static MainframeBlockEntity mainframeOf(final ServerLevel level, final NetworkUuid network) {
        return NetworkSystem.get(level).mainframePositionOf(network)
                .map(pos -> Loaded.blockEntity(level, BlockPos.of(pos)) instanceof MainframeBlockEntity mainframe
                        ? mainframe : null)
                .orElse(null);
    }
}
