/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.ServerRackScreen;
import dev.jstech.computers.client.os.CryptoWalletApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.MiningPanelApp;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.teracoin.MiningPools;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.ServerStacks;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Teracoin's windows on a Frames 10 desktop (computers/docs/design/teracoin.md), maximized as their mocks show them:
 * the Crypto Wallet, a wallet made in it and each of its tabs with a 3D printer beside the computer, the Mining Panel
 * looking at a Mining Pool on the network's rack server, with each of its tabs, and that server on the rack window's
 * card; the panel on the pool's own server, where its settings are; and the miner, alone at Frames 7's prompt and in
 * the pool at an Ubuntu terminal.
 */
public final class TeracoinWindowsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 1_200;
    /* Long enough for a tab to ask the machine what it shows and draw it. */
    private static final int TAB_WAIT = 20;
    /* Long enough for a window's opening to play out. */
    private static final int OPENING = 40;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PRINTER = COMPUTER.south();
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String WALLET = "jsc:crypto_wallet";
    private static final String PANEL = "jsc:mining_panel";
    private static final String PROMPT = "jsc:command_prompt";
    private static final String[] WALLET_TABS = {"overview", "send", "receive", "transactions", "print", "wallets"};
    /* The tabs of a pool seen from another computer: its settings are its own server's to show. */
    private static final String[] PANEL_TABS = {"pool", "miners", "blocks"};
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    private static final BlockPos ROUTER = new BlockPos(3, 2, 2);
    private static final int SERVER = 0;
    private static final int SETTINGS_TAB = 3;
    private static final BlockPos RACK_MONITOR = new BlockPos(6, 2, 6);
    private static final BlockPos AT_THE_RACK_MONITOR = new BlockPos(6, 2, 8);

    private TeracoinWindowsClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void cryptoWallet_makesAWalletAndShowsEachTab(final ClientTestContext ctx) {
        opened(ctx, WALLET)
                .then(SETTLE, () -> ((CryptoWalletApp) app(ctx, WALLET)).makeWallet("main"))
                .thenWaitUntil(() -> app(ctx, WALLET) instanceof CryptoWalletApp wallet && wallet.hasState()
                        && !wallet.address().isEmpty(), SCREEN_WAIT, "the wallet to be made and show its address");
        for (int tab = 0; tab < WALLET_TABS.length; tab++) {
            final int shown = tab;
            ctx.then(SETTLE, () -> {
                ((CryptoWalletApp) app(ctx, WALLET)).showTab(shown);
                // The advancements the wallet earns would toast over the window in its picture.
                ctx.mc().getToasts().clear();
            }).thenScreenshot(TAB_WAIT, "wallet-" + WALLET_TABS[tab]);
        }
    }

    @ClientTest(timeoutTicks = 4800)
    public static void miningPanel_showsEachTab(final ClientTestContext ctx) {
        pooled(ctx);
        opened(ctx, PANEL)
                .thenWaitUntilServer(level -> rack(ctx, level).networkUuid() != null && rack(ctx, level)
                        .networkUuid().equals(pc(ctx, level).networkUuid()), SCREEN_WAIT,
                        "the computer to join the rack's network", level -> "computer="
                                + pc(ctx, level).networkUuid() + " rack=" + rack(ctx, level).networkUuid())
                .thenWaitUntil(() -> app(ctx, PANEL) instanceof MiningPanelApp panel && panel.pool().any(),
                        BOOT_WAIT, "the Mining Panel to find the pool on the rack's server");
        for (int tab = 0; tab < PANEL_TABS.length; tab++) {
            final int shown = tab;
            ctx.then(SETTLE, () -> {
                ((MiningPanelApp) app(ctx, PANEL)).showTab(shown);
                ctx.mc().getToasts().clear();
            }).thenScreenshot(TAB_WAIT, "mining-panel-" + PANEL_TABS[tab]);
        }
        // The rack's own window, the pool's server on its card; an empty hand opens it rather than mounting a stack.
        ctx.then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenTeleport(SETTLE, RACK.north(2), Direction.SOUTH)
                .thenRightClick(SETTLE, RACK)
                .thenAwaitScreen(ServerRackScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(TAB_WAIT, "pool-rack-card");
    }

    /*
     * The pool's own server, the one place its settings are changed: an Ubuntu server with KDE Plasma in the rack of
     * an era network, running the pool and its panel, at the monitor beside the rack.
     */
    @ClientTest(timeoutTicks = 6000)
    public static void miningPanel_onThePoolsOwnServer_showsItsSettings(final ClientTestContext ctx) {
        EraJourney.lay(ctx, EraKit.STANDARD)
                .thenBuild(0, world -> {
                    world.placeMonitor(RACK_MONITOR, Direction.SOUTH, EraKit.STANDARD.monitor());
                    world.blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class).getServers()
                            .setStackInSlot(SERVER, ServerStacks.defaultServer());
                })
                .thenServer(SETTLE, level -> {
                    final ServerRackBlockEntity rack = eraRack(ctx, level);
                    rack.unitHost(SERVER).installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                            "ubuntu"));
                    rack.consoleOf(SERVER).install("jsc:kde_plasma");
                    rack.consoleOf(SERVER).install(Programs.MINING_POOL.toString());
                    rack.consoleOf(SERVER).install(PANEL);
                })
                .thenWaitUntilServer(level -> up(eraRack(ctx, level).unitHost(SERVER)), BOOT_WAIT,
                        "the rack's server to come up", level -> "running=" + eraRack(ctx, level)
                                .unitHost(SERVER).isRunning())
                .thenTeleport(SETTLE, AT_THE_RACK_MONITOR, Direction.NORTH)
                .thenRightClick(SETTLE, RACK_MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the server's desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(PANEL))
                .thenWaitUntil(() -> window(ctx, PANEL) != null, SCREEN_WAIT, "the Mining Panel to open in a window")
                .then(OPENING, () -> window(ctx, PANEL).setMaximized(true))
                .thenWaitUntil(() -> app(ctx, PANEL) instanceof MiningPanelApp panel && panel.pool().yours(),
                        BOOT_WAIT, "the panel on the pool's own server to be told the pool is its own")
                .then(SETTLE, () -> {
                    ((MiningPanelApp) app(ctx, PANEL)).showTab(SETTINGS_TAB);
                    ctx.mc().getToasts().clear();
                })
                .thenScreenshot(TAB_WAIT, "mining-panel-settings");
    }

    /* The miner at Frames 7's Command Prompt, mining alone to a new wallet's address on a computer off any network. */
    @ClientTest(timeoutTicks = 2400)
    public static void miner_atFramesSevensPrompt_minesAloneToAnAddress(final ClientTestContext ctx) {
        final AtomicReference<String> address = new AtomicReference<>("");
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_7"));
                    pc.console().install(Programs.MINER.toString());
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(0, level -> address.set(TeracoinState.of(level.getServer()).chain()
                        .createWallet(RandomGenerator.getDefault()).address()))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(PROMPT))
                .thenWaitUntil(() -> app(ctx, PROMPT) instanceof ShellApp, SCREEN_WAIT, "the Command Prompt to open")
                .then(OPENING, () -> ((ShellApp) app(ctx, PROMPT)).typeLines(List.of("miner --address "
                        + address.get())))
                .thenWaitUntil(() -> app(ctx, PROMPT) instanceof ShellApp shell
                                && shell.scrollbackText().contains("hashing"), BOOT_WAIT,
                        "the miner to say what it hashes")
                .then(SETTLE, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(TAB_WAIT, "miner-alone-frames-7");
    }

    /*
     * The miner's status on a computer mining in the network's pool, at an Ubuntu terminal under KDE Plasma: the pool,
     * what it pays, what each part hashes, the round and the share.
     */
    @ClientTest(timeoutTicks = 4800)
    public static void miner_inThePool_saysHowItMines(final ClientTestContext ctx) {
        final AtomicReference<String> address = new AtomicReference<>("");
        pooled(ctx);
        ctx.thenServer(0, level -> address.set(TeracoinState.of(level.getServer()).chain()
                .createWallet(RandomGenerator.getDefault()).address()));
        opened(ctx, "ubuntu", "jsc:kde_plasma", PROMPT, Programs.MINER.toString())
                .thenWaitUntil(() -> app(ctx, PROMPT) instanceof ShellApp, SCREEN_WAIT, "the terminal to open")
                .then(SETTLE, () -> ((ShellApp) app(ctx, PROMPT)).typeLines(List.of("miner --address "
                        + address.get() + " start")))
                .then(TAB_WAIT, () -> ((ShellApp) app(ctx, PROMPT)).typeLines(List.of("miner status")))
                .thenWaitUntil(() -> app(ctx, PROMPT) instanceof ShellApp shell
                                && shell.scrollbackText().contains("Total"), BOOT_WAIT,
                        "the miner to say how it mines in the pool")
                .then(SETTLE, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(TAB_WAIT, "miner-status-in-the-pool");
    }

    /*
     * The crafting fixture's network with the PC where its Crafting Computer stands, and a Mining Pool on its rack's
     * Debian server, up and found on the network.
     */
    private static void pooled(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.placeRunningMainframe(MAINFRAME);
                    world.setBlock(MAINFRAME.east(), ComputingModule.HBW_CABLE);
                    world.placeSeededRack(RACK);
                    world.setBlock(ROUTER, ComputingModule.PERSONAL_ROUTER.get());
                    world.setBlock(ROUTER.east(), ComputingModule.ETHERNET_CABLE);
                })
                .thenServer(SETTLE, level -> rack(ctx, level).unitHost(SERVER).installOs(
                        ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian")))
                .thenWaitUntilServer(level -> up(rack(ctx, level).unitHost(SERVER)), BOOT_WAIT,
                        "the rack's server to come up", level -> "running=" + rack(ctx, level).unitHost(SERVER)
                                .isRunning())
                .thenServer(SETTLE, level -> rack(ctx, level).consoleOf(SERVER)
                        .install(Programs.MINING_POOL.toString()))
                .thenWaitUntilServer(level -> !MiningPools.on(level, rack(ctx, level).networkUuid()).isEmpty(),
                        SCREEN_WAIT, "the rack's server to run the pool on the network", level -> {
                            final ServerRackBlockEntity at = rack(ctx, level);
                            final NetworkUuid network = at.networkUuid();
                            return "network=" + network + " servers=" + (network == null ? 0
                                    : NetworkSystem.get(level).serversOf(network).size()) + " installed="
                                    + at.hasService(SERVER, Programs.MINING_POOL) + " up="
                                    + at.serviceUp(SERVER, Programs.MINING_POOL) + " running="
                                    + at.unitRunning(SERVER);
                        });
    }

    /*
     * A running Frames 10 computer with {@code program} installed, opened on its desktop and maximized; a 3D printer
     * stands beside it, for the wallet's Print tab to list.
     */
    private static ClientTestContext opened(final ClientTestContext ctx, final String program) {
        return opened(ctx, "frames_10", "", program, program);
    }

    /*
     * The same on a computer of {@code system}, with {@code desktop} over it when one is named and {@code installed}
     * on it, {@code program} opened and maximized.
     */
    private static ClientTestContext opened(final ClientTestContext ctx, final String system, final String desktop,
                                            final String program, final String installed) {
        return ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    if (!desktop.isEmpty()) {
                        pc.console().install(desktop);
                    }
                    pc.console().install(installed);
                    world.placeMonitor(MONITOR, Direction.EAST);
                    world.setBlock(PRINTER, ComputingModule.PROSA_I3_MK3.get().defaultBlockState());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(program))
                .thenWaitUntil(() -> window(ctx, program) != null, SCREEN_WAIT, program + " to open in a window")
                // Maximized as its mocks show it; the title bar's own button is the disk tools' tests' to press.
                .then(OPENING, () -> window(ctx, program).setMaximized(true))
                .thenWaitUntil(() -> window(ctx, program).maximized(), SCREEN_WAIT, program + " to be maximized");
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return (ServerRackBlockEntity) level.getBlockEntity(ctx.abs(RACK));
    }

    private static ServerRackBlockEntity eraRack(final ClientTestContext ctx, final ServerLevel level) {
        return (ServerRackBlockEntity) level.getBlockEntity(ctx.abs(EraNetwork.RACK));
    }

    private static PersonalComputerBlockEntity pc(final ClientTestContext ctx, final ServerLevel level) {
        return (PersonalComputerBlockEntity) level.getBlockEntity(ctx.abs(COMPUTER));
    }

    private static boolean up(final IOsHost server) {
        return server.isRunning() && !server.needsPost() && !server.booting() && !server.atBootMenu()
                && server.crashPage() == null;
    }

    @Nullable
    private static IDesktopApp app(final ClientTestContext ctx, final String program) {
        final DesktopWindow window = window(ctx, program);
        return window == null ? null : window.app();
    }

    @Nullable
    private static DesktopWindow window(final ClientTestContext ctx, final String program) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        return desktop == null ? null : desktop.windowFor(program);
    }
}
