/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.IBootMenuScreenOpener;
import dev.jstech.computers.block.IFirmwareScreenOpener;
import dev.jstech.computers.block.IInstallDoneScreenOpener;
import dev.jstech.computers.block.IInstallProgressScreenOpener;
import dev.jstech.computers.block.IInstallerScreenOpener;
import dev.jstech.computers.block.IKvmScreenOpener;
import dev.jstech.computers.block.IPostScreenOpener;
import dev.jstech.computers.block.ISystemBootScreenOpener;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.MonitorSessionMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only wiring for the Computing module: binds each computer menu to its screen, registers the data-cable renderer that draws mounted bus parts, and makes the standalone funnel part models available to the model manager.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class ComputingClientSetup {

    private ComputingClientSetup() {
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        // The desktop's per-machine caches belong to the world being left.
        DesktopScreen.forgetClientState();
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        // Wire the client-side firmware screen opener so blocks can open it without importing Minecraft.
        IFirmwareScreenOpener.Holder.set((pos, monitorPos, kind, name) -> FirmwareScreen.expect(kind, name));
        IPostScreenOpener.Holder.set((pos, monitorPos, kind, name, remaining, halted) ->
                BootSequenceScreen.expect(kind, name, remaining, halted));
        IInstallDoneScreenOpener.Holder.set(
                (pos, monitorPos, kind, osName, targetLabel, targetSlot, failure) -> {
                    if (failure.isEmpty()) {
                        OsInstallScreen.expectDone(kind, osName, targetLabel, targetSlot);
                    } else {
                        OsInstallScreen.expectFailed(kind, osName, targetLabel, failure);
                    }
                    OsInstallScreen.refreshOpen(pos);
                });
        /*
         * Written down first, always, and then given to the screen already showing it. Both, and in that order:
         * an installer on screen takes the new page in place, so a page that changes under the player does not
         * throw away what they were in the middle of typing, and the copy kept here is what the screen is built
         * from if it is ever built again. Only handing it to the open screen left that copy at whatever page had
         * arrived last with the screen shut, so a rebuild put the player back on it.
         */
        IInstallerScreenOpener.Holder.set(payload -> {
            InstallerScreen.expect(payload);
            if (Minecraft.getInstance().screen instanceof InstallerScreen open
                    && open.isFor(payload.hostPos())) {
                open.accept(payload);
            }
        });
        IBootMenuScreenOpener.Holder.set(
                (pos, monitorPos, menu, remaining) -> BootMenuScreen.expect(menu, remaining));
        /*
         * What is on the glass arrives before the session that shows it, so these hand the content over and
         * the menu opening is what puts the screen up.
         */
        ISystemBootScreenOpener.Holder.set(
                (pos, monitorPos, sequence, remaining, total, endsDark, splash, who) ->
                        SystemBootScreen.expect(sequence, remaining, total, endsDark, splash, who));
        IInstallProgressScreenOpener.Holder.set(
                (pos, monitorPos, kind, osName, targetLabel, ticksLeft, ticksTotal) -> {
                    OsInstallScreen.expectWorking(kind, osName, targetLabel, ticksLeft, ticksTotal);
                    OsInstallScreen.refreshOpen(pos);
                });
        IKvmScreenOpener.Holder.set(KvmChannelScreen::expect);

        /*
         * Every session on a monitor that is not a system shares one menu, so which screen it opens is read
         * off the phase it carries rather than off a menu type of its own.
         */
        event.register(ComputingModule.MONITOR_SESSION_MENU.get(),
                (final MonitorSessionMenu menu, final Inventory inv, final Component title) ->
                        switch (menu.phase()) {
                            case POST -> new BootSequenceScreen(menu, inv, title);
                            case INSTALL_PROGRESS -> new OsInstallScreen(menu, inv, title);
                            case FIRMWARE -> new FirmwareScreen(menu, inv, title);
                            case INSTALLER -> new InstallerScreen(menu, inv, title);
                            case BOOT_MENU -> new BootMenuScreen(menu, inv, title);
                            case KVM -> new KvmChannelScreen(menu, inv, title);
                            default -> new SystemBootScreen(menu, inv, title);
                        });
        event.register(ComputingModule.DESKTOP_MENU.get(), DesktopScreen::new);
        event.register(ComputingModule.MAINFRAME_MENU.get(), MainframeScreen::new);
        event.register(ComputingModule.PERSONAL_COMPUTER_MENU.get(), PersonalComputerScreen::new);
        event.register(ComputingModule.CRAFTING_COMPUTER_MENU.get(), CraftingComputerScreen::new);
        event.register(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER_MENU.get(), ClusterManagementComputerScreen::new);
        event.register(ComputingModule.PATTERN_ENCODER_MENU.get(), PatternEncoderScreen::new);
        event.register(ComputingModule.NETWORK_GATEWAY_MENU.get(), NetworkGatewayScreen::new);
        event.register(ComputingModule.COMMAND_PROMPT_MENU.get(),
                (final CommandPromptMenu menu,
                 final Inventory inv,
                 final Component title) -> new CommandPromptScreen<>(menu, inv, title));
        event.register(ComputingModule.DOS_TERMINAL_MENU.get(), DosTerminalScreen::new);
        event.register(ComputingModule.LINUX_TTY_MENU.get(), LinuxTtyScreen::new);
        event.register(ComputingModule.SERVER_RACK_MENU.get(), ServerRackScreen::new);
        event.register(ComputingModule.SERVER_ROUTER_MENU.get(), ServerRouterScreen::new);
        event.register(ComputingModule.SERVER_ASSEMBLY_MENU.get(), ServerAssemblyScreen::new);
        event.register(ComputingModule.COMPUTER_TERMINAL_MENU.get(), ComputerTerminalScreen::new);
        event.register(ComputingModule.EXPORT_BUS_MENU.get(), ExportBusScreen::new);
        event.register(ComputingModule.IMPORT_BUS_MENU.get(), ImportBusScreen::new);
        event.register(ComputingModule.CRAFTING_SWITCH_MENU.get(), CraftingSwitchScreen::new);
        event.register(ComputingModule.INPUT_BUS_MENU.get(), InputBusScreen::new);
        event.register(ComputingModule.RECEIVING_BUS_MENU.get(), ReceivingBusScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ComputingModule.DATA_CABLE_BE.get(), DataCableRenderer::new);
        event.registerBlockEntityRenderer(ComputingModule.TANK_BE.get(), TankRenderer::new);
        // Every rack cabinet (server, per era, and supercomputer) is one GeckoLib model on its controller.
        event.registerBlockEntityRenderer(ComputingModule.SERVER_RACK_BE.get(), RackRenderer::new);
        // The Mainframe is the same idea: one cabinet per era, drawn from the controller block.
        event.registerBlockEntityRenderer(ComputingModule.MAINFRAME_BE.get(), MainframeRenderer::new);
        // The Pattern Encoder: one burner body per era, with its bay, display and lamps.
        event.registerBlockEntityRenderer(ComputingModule.PATTERN_ENCODER_BE.get(), PatternEncoderRenderer::new);
    }

    @SubscribeEvent
    public static void registerExtraModels(final ModelEvent.RegisterAdditional event) {
        event.register(DataCableRenderer.IMPORT_MODEL);
        event.register(DataCableRenderer.EXPORT_MODEL);
    }
}
