/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.CraftPlannerApp;
import dev.jstech.computers.client.os.CraftingManagerApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.NetworkInteractorApp;
import dev.jstech.computers.client.os.PatternStudioApp;
import dev.jstech.computers.client.os.WorkshopApp;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.integration.jei.payload.SetPatternPayload;
import dev.jstech.computers.integration.jei.payload.SetProcessingPatternPayload;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.ClientTestFailure;
import dev.jstech.tests.testkit.computers.CraftFiles;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's fabrication in a player's hands, on the era's network with a Crafting Computer of the era beside it.
 * From the Legacy: at the Crafting Computer's desktop the player writes three patterns in the Pattern Studio (planks
 * from a log and sticks from planks at the bench, and an ingot smelted from raw iron), each burned by the era's Pattern
 * Encoder onto the era's rewritable disc; moves the disc from the encoder to the drive beside the computer; loads the
 * three in the Crafting Manager; and asks the Network Interactor for sticks, which the network makes in two steps, and
 * for ingots, which the furnace fed by a Crafting Interface on the crafting cable smelts while the world is saved and
 * loaded again. The Craft Planner plans planks and crafts them from its button. The Workshop's four personal cards,
 * two to a computer, do their work in its tabs. On the Vintage, whose machines have no studio, the patterns are put in
 * the computer and the player asks for the crafts at MC-DOS's prompt.
 */
public final class EraFabricationClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int WAIT = 200;
    private static final int CRAFT_WAIT = 1_200;
    private static final int TIMEOUT = 16_000;
    /* The Crafting Computer where the second computer stands, its monitor east of it and the desk beyond. */
    private static final BlockPos CRAFTER = EraNetwork.SECOND_COMPUTER;
    private static final BlockPos CRAFTER_MONITOR = EraNetwork.SECOND_MONITOR;
    private static final BlockPos AT_THE_CRAFTER = EraNetwork.AT_THE_SECOND_DESK;
    /** The encoder on top of the computer, and where the player reaches it. */
    private static final BlockPos ENCODER = CRAFTER.above();
    private static final BlockPos BY_THE_ENCODER = new BlockPos(11, 2, 6);
    /** The drive north of the computer, its front to the north, and where the player reaches it. */
    private static final BlockPos DRIVE = new BlockPos(9, 2, 5);
    private static final BlockPos BY_THE_DRIVE = new BlockPos(9, 2, 3);
    /* The machine: the crafting cable south of the computer, the interface's own cable climbing to the furnace. */
    private static final BlockPos CRAFTING_CABLE = new BlockPos(9, 2, 7);
    private static final BlockPos INTERFACE_CABLE = new BlockPos(9, 2, 8);
    private static final BlockPos CABLE_BELOW_FURNACE = new BlockPos(9, 2, 9);
    private static final BlockPos OWN_CABLE_1 = new BlockPos(10, 2, 8);
    private static final BlockPos OWN_CABLE_2 = new BlockPos(10, 3, 8);
    private static final BlockPos OWN_CABLE_3 = new BlockPos(10, 4, 8);
    private static final BlockPos OWN_CABLE_4 = new BlockPos(10, 4, 9);
    private static final BlockPos CABLE_ABOVE_FURNACE = new BlockPos(9, 4, 9);
    private static final BlockPos FURNACE = new BlockPos(9, 3, 9);
    private static final BlockPos BY_THE_MACHINE = new BlockPos(11, 2, 8);
    private static final String STUDIO = "Pattern Studio";
    private static final String MANAGER = "Crafting Manager";
    private static final String NETWORK = "Network";
    private static final String PLANNER = "Craft Planner";
    private static final String WORKSHOP = "Workshop";
    private static final int LOGS = 16;
    private static final int RAW_IRON = 4;

    private EraFabricationClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_craftsAreAskedForAtThePrompt(final ClientTestContext ctx) {
        final EraKit kit = EraKit.VINTAGE;
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            final CraftingComputerBlockEntity crafter = EraNetwork.craftingComputer(world, kit);
            crafter.loadPattern(CraftFiles.oakPlanks());
        });
        ctx.thenServer(SETTLE, level -> NetworkStorage.of(level, mainframe(ctx, level).networkUuid())
                        .insert(StorageKey.of(Items.OAK_LOG), LOGS))
                .thenTeleport(SETTLE, AT_THE_CRAFTER, Direction.WEST)
                .thenRightClick(SETTLE, CRAFTER_MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, EraJourney.BOOT_WAIT)
                .then(SETTLE, () -> {
                    ctx.type("iql CRAFT 8 oak_planks");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntilServer(level -> stored(ctx, level, Items.OAK_PLANKS) >= 8, CRAFT_WAIT,
                        "the craft asked for at the prompt to make planks",
                        level -> "planks=" + stored(ctx, level, Items.OAK_PLANKS) + " logs="
                                + stored(ctx, level, Items.OAK_LOG) + "; the crafter can craft "
                                + crafter(ctx, level).canCraft() + " at " + crafter(ctx, level).craftingThroughput()
                                + " (capacity " + crafter(ctx, level).capacity() + ", busy "
                                + crafter(ctx, level).craftBusy() + "); the prompt said " + said(crafter(ctx, level)))
                .thenScreenshot(SETTLE, "vintage-craft-at-the-prompt")
                .then(SETTLE, () -> {
                    ctx.type("iql CRAFT 8 oak_planks");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the prompt to be left")
                .thenSaveAndReload(SETTLE)
                .thenWaitUntilServer(level -> stored(ctx, level, Items.OAK_PLANKS) >= 16, CRAFT_WAIT,
                        "the craft asked for before the save to finish after the reload",
                        level -> "planks=" + stored(ctx, level, Items.OAK_PLANKS));
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_patternsAreMadeLoadedAndCrafted(final ClientTestContext ctx) {
        patterns(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_patternsAreMadeLoadedAndCrafted(final ClientTestContext ctx) {
        patterns(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_patternsAreMadeLoadedAndCrafted(final ClientTestContext ctx) {
        patterns(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_patternsAreMadeLoadedAndCrafted(final ClientTestContext ctx) {
        patterns(ctx, EraKit.ADVANCED);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_workshopCardsDoTheirWork(final ClientTestContext ctx) {
        workshop(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_workshopCardsDoTheirWork(final ClientTestContext ctx) {
        workshop(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_workshopCardsDoTheirWork(final ClientTestContext ctx) {
        workshop(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_workshopCardsDoTheirWork(final ClientTestContext ctx) {
        workshop(ctx, EraKit.ADVANCED);
    }

    private static void patterns(final ClientTestContext ctx, final EraKit kit) {
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            final CraftingComputerBlockEntity crafter = EraNetwork.craftingComputer(world, kit);
            crafter.console().install(Programs.PATTERN_STUDIO.toString());
            crafter.console().install(Programs.CRAFTING_MANAGER.toString());
            crafter.console().install(Programs.CRAFT_PLANNER.toString());
            // Its front to the east, where the player stands to reach it, as a player placing it from there leaves it.
            world.setBlock(ENCODER, facing(encoderOf(kit), Direction.EAST));
            world.setBlock(DRIVE, facing(driveOf(kit), Direction.NORTH));
            for (final BlockPos cable : List.of(CRAFTING_CABLE, INTERFACE_CABLE, CABLE_BELOW_FURNACE, OWN_CABLE_1,
                    OWN_CABLE_2, OWN_CABLE_3, OWN_CABLE_4, CABLE_ABOVE_FURNACE)) {
                world.setBlock(cable, ComputingModule.CRAFTING_CABLE);
            }
            world.setBlock(FURNACE, Blocks.FURNACE);
        });
        ctx.thenServer(SETTLE, level -> {
            NetworkStorage.of(level, mainframe(ctx, level).networkUuid()).insert(StorageKey.of(Items.OAK_LOG), LOGS);
            NetworkStorage.of(level, mainframe(ctx, level).networkUuid())
                    .insert(StorageKey.of(Items.RAW_IRON), RAW_IRON);
            ((FurnaceBlockEntity) level.getBlockEntity(ctx.abs(FURNACE))).setItem(1, new ItemStack(Items.COAL, 8));
        });
        machine(ctx, kit);
        disc(ctx, kit);
        studio(ctx, kit);
        load(ctx, kit);
        interactor(ctx, kit);
        planner(ctx, kit);
    }

    /* The Crafting Interface on the crafting cable, the router over the furnace and the receiving bus under it. */
    private static void machine(final ClientTestContext ctx, final EraKit kit) {
        ctx.thenGive(0, new ItemStack(interfaceOf(kit)), new ItemStack(ComputingModule.CRAFTING_ROUTER_ITEM.get()),
                        new ItemStack(ComputingModule.RECEIVING_BUS_ITEM.get()))
                .thenTeleport(SETTLE, BY_THE_MACHINE, Direction.WEST)
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .thenSneakClick(1, INTERFACE_CABLE, Direction.EAST)
                .then(2, () -> ctx.selectHotbar(1))
                .thenSneakClick(1, CABLE_ABOVE_FURNACE, Direction.DOWN)
                .then(2, () -> ctx.selectHotbar(2))
                .thenSneakClick(1, CABLE_BELOW_FURNACE, Direction.UP)
                .thenScreenshot(SETTLE, shot(kit, "machine"));
    }

    /* The era's rewritable disc onto the encoder above the computer, by hand. */
    private static void disc(final ClientTestContext ctx, final EraKit kit) {
        ctx.thenGive(0, new ItemStack(rewritableOf(kit)))
                .thenTeleport(SETTLE, BY_THE_ENCODER, Direction.WEST)
                .then(SETTLE, () -> ctx.selectHotbar(0));
        if (kit != EraKit.VINTAGE) {
            DiscSteps.openTray(ctx, ENCODER);
        }
        ctx.thenRightClick(1, ENCODER)
                .thenWaitUntilServer(level -> encoder(ctx, level).hasMedia(), WAIT,
                        "the disc to go into the encoder", level -> "encoder=" + encoder(ctx, level).mediaStack());
        if (kit != EraKit.VINTAGE) {
            DiscSteps.closeTray(ctx, ENCODER);
        }
    }

    /* Three patterns written in the Pattern Studio, each burned onto the disc in the encoder. */
    private static void studio(final ClientTestContext ctx, final EraKit kit) {
        atTheDesktop(ctx);
        EraJourney.launch(ctx, kit, STUDIO)
                .thenWaitUntil(() -> app(ctx, STUDIO, PatternStudioApp.class) != null
                                && app(ctx, STUDIO, PatternStudioApp.class).isLoaded()
                                && app(ctx, STUDIO, PatternStudioApp.class).state().encoder().linked(), WAIT,
                        "the Pattern Studio to open and see the encoder")
                .then(0, () -> bench(ctx, "minecraft:oak_planks", new ItemStack(Items.OAK_LOG)))
                .thenWaitUntilServer(level -> crafter(ctx, level).studio().preview().is(Items.OAK_PLANKS), WAIT,
                        "the planks to land on the bench", level -> "preview=" + crafter(ctx, level).studio().preview())
                .then(SETTLE, () -> ctx.clickDesktop(studioPoint(ctx, app(ctx, STUDIO, PatternStudioApp.class)
                        .barButtonCenter(0))))
                .thenWaitUntilServer(level -> craftFiles(ctx, level) == 1, WAIT * 2, "the planks' pattern on the disc",
                        level -> "files=" + craftFiles(ctx, level) + " " + encoder(ctx, level).statusLine())
                .then(SETTLE, () -> bench(ctx, "minecraft:stick", new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY,
                        ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS)))
                .thenWaitUntilServer(level -> crafter(ctx, level).studio().preview().is(Items.STICK), WAIT,
                        "the sticks to land on the bench", level -> "preview=" + crafter(ctx, level).studio().preview())
                .then(SETTLE, () -> ctx.clickDesktop(studioPoint(ctx, app(ctx, STUDIO, PatternStudioApp.class)
                        .barButtonCenter(0))))
                .thenWaitUntilServer(level -> craftFiles(ctx, level) == 2, WAIT * 2, "the sticks' pattern on the disc",
                        level -> "files=" + craftFiles(ctx, level) + " " + encoder(ctx, level).statusLine())
                .then(SETTLE, () -> {
                    final PatternStudioApp app = app(ctx, STUDIO, PatternStudioApp.class);
                    PacketDistributor.sendToServer(new SetProcessingPatternPayload(app.host(), app.monitorPos(),
                            List.of(PatternWorkbench.DataCell.fromStack(new ItemStack(Items.RAW_IRON))),
                            List.of(PatternWorkbench.DataCell.fromStack(new ItemStack(Items.IRON_INGOT)))));
                })
                .thenWaitUntilServer(level -> crafter(ctx, level).studio().procInput(0) != null, WAIT,
                        "the smelt to land in the machine draft", level -> "")
                .then(SETTLE, () -> ctx.clickDesktop(studioPoint(ctx, app(ctx, STUDIO, PatternStudioApp.class)
                        .barButtonCenter(0))))
                .thenWaitUntilServer(level -> craftFiles(ctx, level) == 3, WAIT * 2, "the smelt's pattern on the disc",
                        level -> "files=" + craftFiles(ctx, level) + " " + encoder(ctx, level).statusLine())
                .thenScreenshot(SETTLE, shot(kit, "pattern-studio"))
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the desktop to be left");
    }

    /* The disc from the encoder to the drive beside the computer, and the three patterns loaded in the manager. */
    private static void load(final ClientTestContext ctx, final EraKit kit) {
        ctx.thenGive(0)
                .thenTeleport(SETTLE, BY_THE_ENCODER, Direction.WEST)
                .then(SETTLE, () -> ctx.selectHotbar(0));
        if (kit != EraKit.VINTAGE) {
            DiscSteps.openTray(ctx, ENCODER);
        }
        ctx.thenSneakClick(1, ENCODER, Direction.EAST)
                .thenWaitUntilServer(level -> !encoder(ctx, level).hasMedia(), WAIT,
                        "a crouched click to lift the disc out of the encoder", level -> "");
        if (kit != EraKit.VINTAGE) {
            DiscSteps.closeTray(ctx, ENCODER);
        }
        ctx.thenTeleport(SETTLE, BY_THE_DRIVE, Direction.SOUTH)
                .then(SETTLE, () -> ctx.selectHotbar(0));
        DiscSteps.openTray(ctx, DRIVE)
                .thenRightClick(SETTLE, DRIVE)
                .thenWaitUntilServer(level -> !reader(ctx, level).mediaSlot().getStackInSlot(0).isEmpty(), WAIT,
                        "the disc to go into the drive", level -> "");
        DiscSteps.closeTray(ctx, DRIVE);
        atTheDesktop(ctx);
        EraJourney.launch(ctx, kit, MANAGER)
                .thenWaitUntil(() -> app(ctx, MANAGER, CraftingManagerApp.class) != null
                                && app(ctx, MANAGER, CraftingManagerApp.class).mediaFiles().size() == 3, WAIT,
                        "the Crafting Manager to list the three patterns on the disc")
                .then(SETTLE, () -> {
                    final CraftingManagerApp app = app(ctx, MANAGER, CraftingManagerApp.class);
                    for (int i = 0; i < 3; i++) {
                        ctx.clickDesktop(app.mediaRowCenter(i));
                    }
                })
                .then(2, () -> ctx.clickDesktop(app(ctx, MANAGER, CraftingManagerApp.class).actionButtonCenter(0)))
                .thenWaitUntil(() -> app(ctx, MANAGER, CraftingManagerApp.class).romNames().size() == 3, WAIT,
                        "Load to put the three patterns where the network crafts from")
                .thenScreenshot(SETTLE, shot(kit, "crafting-manager"))
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the desktop to be left");
    }

    /*
     * The Network Interactor asks for sticks, which the network makes from logs in two steps, and for ingots, which
     * the furnace smelts while the world is saved and loaded again.
     */
    private static void interactor(final ClientTestContext ctx, final EraKit kit) {
        atTheDesktop(ctx);
        EraJourney.launch(ctx, kit, NETWORK)
                .thenWaitUntil(() -> app(ctx, NETWORK, NetworkInteractorApp.class) != null, WAIT,
                        "the Network Interactor to open")
                .then(SETTLE, () -> ctx.clickDesktop(appPoint(ctx, NETWORK,
                        app(ctx, NETWORK, NetworkInteractorApp.class).craftingTabCenter())))
                .thenWaitUntil(() -> app(ctx, NETWORK, NetworkInteractorApp.class).craftableNames().contains("Stick"),
                        WAIT, "the Crafting tab to offer sticks")
                .then(0, () -> requestCraft(ctx, "Stick", 0))
                .thenWaitUntilServer(level -> stored(ctx, level, Items.STICK) >= 4, CRAFT_WAIT,
                        "the sticks to be made, planks first", level -> "sticks=" + stored(ctx, level, Items.STICK)
                                + " planks=" + stored(ctx, level, Items.OAK_PLANKS))
                .thenWaitUntil(() -> app(ctx, NETWORK, NetworkInteractorApp.class).craftableNames()
                        .contains("Iron Ingot"), WAIT, "the Crafting tab to offer ingots")
                .then(0, () -> requestCraft(ctx, "Iron Ingot", 1))
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(FURNACE)) instanceof FurnaceBlockEntity f
                                && f.getItem(0).is(Items.RAW_IRON), WAIT,
                        "the interface to feed raw iron into the furnace", level -> "")
                .thenScreenshot(SETTLE, shot(kit, "smelting"))
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the desktop to be left")
                .thenSaveAndReload(SETTLE)
                .thenWaitUntilServer(level -> stored(ctx, level, Items.IRON_INGOT) >= 2, CRAFT_WAIT,
                        "the ingots smelted across the save to reach the network",
                        level -> "ingots=" + stored(ctx, level, Items.IRON_INGOT));
    }

    /* The Craft Planner plans planks from the logs left, and its Craft button makes them. */
    private static void planner(final ClientTestContext ctx, final EraKit kit) {
        final long[] before = new long[1];
        ctx.thenServer(SETTLE, level -> before[0] = stored(ctx, level, Items.OAK_PLANKS));
        atTheDesktop(ctx);
        EraJourney.launch(ctx, kit, PLANNER)
                .thenWaitUntil(() -> planner(ctx) != null && planner(ctx).catalogRowPoint("Oak Planks") != null,
                        WAIT, "the Craft Planner to list planks in its catalogue")
                .then(SETTLE, () -> ctx.clickDesktop(planner(ctx).catalogRowPoint("Oak Planks")))
                .thenWaitUntil(() -> planner(ctx).planFeasible() && planner(ctx).planStages() > 0, WAIT,
                        "the planner to plan planks the network can make now")
                .thenScreenshot(SETTLE, shot(kit, "craft-planner"))
                .then(SETTLE, () -> ctx.clickDesktop(planner(ctx).craftPoint()))
                .thenWaitUntilServer(level -> stored(ctx, level, Items.OAK_PLANKS) > before[0], CRAFT_WAIT,
                        "the planner's Craft button to make planks", level -> "planks="
                                + stored(ctx, level, Items.OAK_PLANKS) + " before=" + before[0]);
    }

    /*
     * The four personal cards, two to a computer: on the first the crafting table and the furnace, which smelts what
     * was left in it; on the second the enchanting table, which offers three enchantments, and the anvil, which
     * repairs a worn pickaxe. Each card's tab is opened with a click.
     */
    private static void workshop(final ClientTestContext ctx, final EraKit kit) {
        EraJourney.lay(ctx, kit).thenBuild(0, world -> EraNetwork.secondComputer(world, kit));
        ctx.thenServer(SETTLE, level -> {
            final PersonalComputerBlockEntity first = computerAt(ctx, level, EraNetwork.COMPUTER);
            cards(first, ComputingModule.CRAFTING_TABLE_CARD.get(), ComputingModule.FURNACE_CARD.get());
            first.console().install(Programs.WORKSHOP.toString());
            first.workshop().put(Workshop.FURNACE_IN, new ItemStack(Items.RAW_IRON, 4));
            final PersonalComputerBlockEntity second = computerAt(ctx, level, EraNetwork.SECOND_COMPUTER);
            cards(second, ComputingModule.ENCHANTING_CARD.get(), ComputingModule.ANVIL_CARD.get());
            second.console().install(Programs.WORKSHOP.toString());
            second.workshop().put(Workshop.ENCHANT_ITEM, new ItemStack(Items.DIAMOND_SWORD));
            final ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
            worn.setDamageValue(worn.getMaxDamage() - 40);
            second.workshop().put(Workshop.ANVIL_LEFT, worn);
            second.workshop().put(Workshop.ANVIL_RIGHT, new ItemStack(Items.IRON_INGOT, 2));
            ctx.serverPlayer().setExperienceLevels(34);
        });
        ctx.thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(DesktopScreen.class, EraJourney.BOOT_WAIT);
        EraJourney.launch(ctx, kit, WORKSHOP)
                .thenWaitUntil(() -> workshopApp(ctx) != null && workshopApp(ctx).state() != null, WAIT,
                        "the Workshop to open with the computer's cards")
                .then(SETTLE, () -> ctx.clickDesktop(appPoint(ctx, WORKSHOP,
                        workshopApp(ctx).tabCenter(WorkshopCard.FURNACE.index()))))
                .thenWaitUntil(() -> workshopApp(ctx).activeTab() == WorkshopCard.FURNACE.index(), WAIT,
                        "the Furnace tab to open with a click")
                .thenWaitUntilServer(level -> computerAt(ctx, level, EraNetwork.COMPUTER).workshop()
                                .slot(Workshop.FURNACE_OUT).is(Items.IRON_INGOT), CRAFT_WAIT,
                        "the furnace card to smelt the raw iron left in it", level -> "out="
                                + computerAt(ctx, level, EraNetwork.COMPUTER).workshop().slot(Workshop.FURNACE_OUT))
                .thenScreenshot(SETTLE, shot(kit, "workshop-furnace"))
                .then(SETTLE, () -> ctx.clickDesktop(appPoint(ctx, WORKSHOP,
                        workshopApp(ctx).tabCenter(WorkshopCard.CRAFTING_TABLE.index()))))
                .thenWaitUntil(() -> workshopApp(ctx).activeTab() == WorkshopCard.CRAFTING_TABLE.index(), WAIT,
                        "the Crafting Table tab to open with a click")
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the desktop to be left")
                .thenTeleport(SETTLE, EraNetwork.AT_THE_SECOND_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.SECOND_MONITOR)
                .thenAwaitScreen(DesktopScreen.class, EraJourney.BOOT_WAIT);
        EraJourney.launch(ctx, kit, WORKSHOP)
                .thenWaitUntil(() -> workshopApp(ctx) != null && workshopApp(ctx).state() != null
                                && workshopApp(ctx).state().offers().size() == 3, WAIT,
                        "the Workshop to offer three enchantments for the sword")
                .thenScreenshot(SETTLE, shot(kit, "workshop-enchanting"))
                .then(SETTLE, () -> ctx.clickDesktop(appPoint(ctx, WORKSHOP,
                        workshopApp(ctx).tabCenter(WorkshopCard.ANVIL.index()))))
                .thenWaitUntil(() -> workshopApp(ctx).activeTab() == WorkshopCard.ANVIL.index()
                                && !workshopApp(ctx).state().anvilResult().isEmpty(), WAIT,
                        "the Anvil tab to show the pickaxe repaired")
                .thenScreenshot(SETTLE, shot(kit, "workshop-anvil"));
    }

    /* Two personal cards in the first free card slots of the computer. */
    private static void cards(final PersonalComputerBlockEntity computer, final Item... cards) {
        int card = 0;
        for (int slot = PersonalComputerBlockEntity.GPU_SLOTS_START; slot < PersonalComputerBlockEntity.GPU_SLOTS_START
                + PersonalComputerBlockEntity.GPU_SLOTS && card < cards.length; slot++) {
            if (computer.getHardware().getStackInSlot(slot).isEmpty()) {
                computer.getHardware().setStackInSlot(slot, new ItemStack(cards[card++]));
            }
        }
        if (card < cards.length) {
            throw new ClientTestFailure("the computer has no room for its cards");
        }
    }

    /* The player sits at the Crafting Computer's desktop. */
    private static void atTheDesktop(final ClientTestContext ctx) {
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the screen to be left")
                .thenTeleport(SETTLE, AT_THE_CRAFTER, Direction.WEST)
                .then(SETTLE, () -> ctx.selectHotbar(8))
                .thenRightClick(SETTLE, CRAFTER_MONITOR)
                .thenAwaitScreen(DesktopScreen.class, EraJourney.BOOT_WAIT);
    }

    /* A bench recipe sent to the Studio's bench: the payload the recipe viewer's transfer button sends. */
    private static void bench(final ClientTestContext ctx, final String recipe, final ItemStack... cells) {
        final PatternStudioApp app = app(ctx, STUDIO, PatternStudioApp.class);
        final List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(i < cells.length ? cells[i] : ItemStack.EMPTY);
        }
        PacketDistributor.sendToServer(new SetPatternPayload(app.host(), app.monitorPos(), grid, recipe));
    }

    /* The craft popup for {@code name} opened on the Crafting tab, raised {@code plusOnes} times and sent. */
    private static void requestCraft(final ClientTestContext ctx, final String name, final int plusOnes) {
        final NetworkInteractorApp app = app(ctx, NETWORK, NetworkInteractorApp.class);
        final int index = app.craftableNames().indexOf(name);
        ctx.assertTrue(index >= 0, "the Crafting tab must list " + name + "; got " + app.craftableNames());
        ctx.clickDesktop(appPoint(ctx, NETWORK, app.craftableCellCenter(index)));
        ctx.clickDesktop(appPoint(ctx, NETWORK, app.craftableCellCenter(index)));
        ctx.assertTrue(app.isCraftPopupOpen(), "double-clicking " + name + " opens the request popup");
        for (int i = 0; i < plusOnes; i++) {
            ctx.clickDesktop(appPoint(ctx, NETWORK, app.craftPopupStepCenter(2)));
        }
        ctx.clickDesktop(appPoint(ctx, NETWORK, app.craftPopupSubmitCenter()));
    }

    private static int[] studioPoint(final ClientTestContext ctx, final int[] local) {
        return appPoint(ctx, STUDIO, local);
    }

    /* An app's content-local point on the desktop. */
    private static int[] appPoint(final ClientTestContext ctx, final String label, final int[] local) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window == null) {
            throw new ClientTestFailure("the " + label + " window is gone");
        }
        return DesktopSteps.contentPoint(window, local);
    }

    private static <T> T app(final ClientTestContext ctx, final String label, final Class<T> type) {
        return DesktopSteps.app(ctx, label, type);
    }

    private static CraftPlannerApp planner(final ClientTestContext ctx) {
        return app(ctx, PLANNER, CraftPlannerApp.class);
    }

    private static WorkshopApp workshopApp(final ClientTestContext ctx) {
        return app(ctx, WORKSHOP, WorkshopApp.class);
    }

    /* The last lines a machine's prompt answered with, for a failure to say what it said. */
    private static String said(final CraftingComputerBlockEntity computer) {
        final List<WireLine> glass = computer.console().glass();
        final StringBuilder out = new StringBuilder();
        for (final WireLine line : glass.subList(Math.max(0, glass.size() - 4), glass.size())) {
            line.spans().forEach(span -> out.append(span.text().english()));
            out.append(" | ");
        }
        return out.toString();
    }

    private static int craftFiles(final ClientTestContext ctx, final ServerLevel level) {
        int files = 0;
        for (final DiskFilesystem.FileEntry entry : DiskFilesystem.list(encoder(ctx, level).media().getStackInSlot(0),
                "", FilesystemKind.HIERARCHICAL)) {
            if (entry.type() == FileType.CRAFT) {
                files++;
            }
        }
        return files;
    }

    private static Block encoderOf(final EraKit kit) {
        return switch (kit) {
            case VINTAGE -> ComputingModule.VINTAGE_PATTERN_ENCODER.get();
            case LEGACY -> ComputingModule.LEGACY_PATTERN_ENCODER.get();
            case TRANSITION -> ComputingModule.TRANSITION_PATTERN_ENCODER.get();
            case STANDARD -> ComputingModule.PATTERN_ENCODER.get();
            case ADVANCED -> ComputingModule.ADVANCED_PATTERN_ENCODER.get();
        };
    }

    /* The disc the era's encoder writes, and the drive that reads it. */
    private static Item rewritableOf(final EraKit kit) {
        return switch (kit) {
            case VINTAGE -> ComputingModule.FLOPPY_DISK.get();
            case LEGACY -> ComputingModule.CD_RW.get();
            case TRANSITION, STANDARD -> ComputingModule.DVD_RW.get();
            case ADVANCED -> ComputingModule.BD_RE.get();
        };
    }

    private static Block driveOf(final EraKit kit) {
        return switch (kit) {
            case VINTAGE -> ComputingModule.FLOPPY_DRIVE.get();
            case LEGACY -> ComputingModule.CD_DRIVE.get();
            case TRANSITION, STANDARD -> ComputingModule.DVD_DRIVE.get();
            case ADVANCED -> ComputingModule.BLU_RAY_DRIVE.get();
        };
    }

    private static Item interfaceOf(final EraKit kit) {
        return switch (kit) {
            case VINTAGE -> ComputingModule.VINTAGE_CRAFTING_INTERFACE_ITEM.get();
            case LEGACY -> ComputingModule.LEGACY_CRAFTING_INTERFACE_ITEM.get();
            case TRANSITION -> ComputingModule.TRANSITION_CRAFTING_INTERFACE_ITEM.get();
            case STANDARD -> ComputingModule.CRAFTING_INTERFACE_ITEM.get();
            case ADVANCED -> ComputingModule.ADVANCED_CRAFTING_INTERFACE_ITEM.get();
        };
    }

    private static BlockState facing(final Block block, final Direction front) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, front);
    }

    private static long stored(final ClientTestContext ctx, final ServerLevel level, final Item item) {
        return NetworkStorage.of(level, mainframe(ctx, level).networkUuid()).count(StorageKey.of(item));
    }

    private static PatternEncoderBlockEntity encoder(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(ENCODER, PatternEncoderBlockEntity.class);
    }

    private static MediaReaderBlockEntity reader(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(DRIVE, MediaReaderBlockEntity.class);
    }

    private static CraftingComputerBlockEntity crafter(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(CRAFTER, CraftingComputerBlockEntity.class);
    }

    private static PersonalComputerBlockEntity computerAt(final ClientTestContext ctx, final ServerLevel level,
                                                          final BlockPos at) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(at, PersonalComputerBlockEntity.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }
}
