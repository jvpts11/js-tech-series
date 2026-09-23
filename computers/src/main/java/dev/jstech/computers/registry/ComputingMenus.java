/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.menu.ClusterManagementComputerMenu;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.computers.menu.CraftingSwitchMenu;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.menu.DosTerminalMenu;
import dev.jstech.computers.menu.ExportBusMenu;
import dev.jstech.computers.menu.ImportBusMenu;
import dev.jstech.computers.menu.InputBusMenu;
import dev.jstech.computers.menu.LinuxTtyMenu;
import dev.jstech.computers.menu.MainframeMenu;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.menu.NetTerminalMenu;
import dev.jstech.computers.menu.NetworkGatewayMenu;
import dev.jstech.computers.menu.PatternEncoderMenu;
import dev.jstech.computers.menu.PersonalComputerMenu;
import dev.jstech.computers.menu.ReceivingBusMenu;
import dev.jstech.computers.menu.ServerAssemblyMenu;
import dev.jstech.computers.menu.ServerRackMenu;
import dev.jstech.computers.menu.ServerRouterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The menus the mod opens: the machines', the buses', and the sessions a monitor shows.
 */
public final class ComputingMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, JsComputers.MODID);

    // The buses on a cable

    public static final DeferredHolder<MenuType<?>, MenuType<ExportBusMenu>> EXPORT_BUS_MENU =
            menu("export_bus", ExportBusMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<ImportBusMenu>> IMPORT_BUS_MENU =
            menu("import_bus", ImportBusMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<InputBusMenu>> INPUT_BUS_MENU =
            menu("input_bus", InputBusMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<ReceivingBusMenu>> RECEIVING_BUS_MENU =
            menu("receiving_bus", ReceivingBusMenu::fromNetwork);

    // The machines

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingSwitchMenu>> CRAFTING_SWITCH_MENU =
            menu("crafting_switch", CraftingSwitchMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<ServerRackMenu>> SERVER_RACK_MENU =
            menu("server_rack", ServerRackMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<ServerAssemblyMenu>> SERVER_ASSEMBLY_MENU =
            menu("server_assembly", ServerAssemblyMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<PatternEncoderMenu>> PATTERN_ENCODER_MENU =
            menu("pattern_encoder", PatternEncoderMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<NetworkGatewayMenu>> NETWORK_GATEWAY_MENU =
            menu("network_gateway", NetworkGatewayMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<MainframeMenu>> MAINFRAME_MENU =
            menu("mainframe", MainframeMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<PersonalComputerMenu>> PERSONAL_COMPUTER_MENU =
            menu("personal_computer", PersonalComputerMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingComputerMenu>> CRAFTING_COMPUTER_MENU =
            menu("crafting_computer", CraftingComputerMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<ClusterManagementComputerMenu>>
            CLUSTER_MANAGEMENT_COMPUTER_MENU =
            menu("cluster_management_computer", ClusterManagementComputerMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<ServerRouterMenu>> SERVER_ROUTER_MENU =
            menu("server_router", ServerRouterMenu::fromNetwork);

    // What a monitor shows

    public static final DeferredHolder<MenuType<?>, MenuType<ComputerTerminalMenu>> COMPUTER_TERMINAL_MENU =
            menu("computer_terminal", ComputerTerminalMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<CommandPromptMenu>> COMMAND_PROMPT_MENU =
            menu("command_prompt", CommandPromptMenu::fromNetwork);

    /*
     * Every session on a monitor that is not a system: the self-test, the boot manager, the firmware setup,
     * a system coming up, an installer, and the rack's channel switch. One menu with a phase rather than one
     * per screen, because they carry exactly the same thing and differ only in what is drawn on the glass.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<MonitorSessionMenu>> MONITOR_SESSION_MENU =
            menu("monitor_session", MonitorSessionMenu::fromNetwork);

    /*
     * Each terminal platform opens its own screen. They carry the same data and differ in how the machine
     * greets and where it stands, so no system is ever met in another's voice.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<DosTerminalMenu>> DOS_TERMINAL_MENU =
            menu("dos_terminal", DosTerminalMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<LinuxTtyMenu>> LINUX_TTY_MENU =
            menu("linux_tty", LinuxTtyMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<NetTerminalMenu>> NET_TERMINAL_MENU =
            menu("net_terminal", NetTerminalMenu::fromNetwork);

    public static final DeferredHolder<MenuType<?>, MenuType<DesktopMenu>> DESKTOP_MENU =
            menu("desktop", DesktopMenu::fromNetwork);

    private ComputingMenus() {
    }

    public static void register(final IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    /** A menu opened with extra data from the server, read by the factory given. */
    private static <M extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<M>> menu(
            final String id, final IContainerFactory<M> factory) {
        return MENUS.register(id, () -> IMenuTypeExtension.create(factory));
    }
}
