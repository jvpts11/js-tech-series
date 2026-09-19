/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.automation.AutomationPayloads;
import dev.jstech.computers.operation.payload.cluster.ClusterManagerPayloads;
import dev.jstech.computers.operation.payload.crafting.CraftManagerPayloads;
import dev.jstech.computers.operation.payload.crafting.CraftPlannerPayloads;
import dev.jstech.computers.operation.payload.crafting.CraftingPayloads;
import dev.jstech.computers.operation.payload.desktop.DesktopPayloads;
import dev.jstech.computers.operation.payload.desktop.ThisPcPayloads;
import dev.jstech.computers.operation.payload.desktop.WelcomePayloads;
import dev.jstech.computers.operation.payload.desktop.WorkstationInfoPayloads;
import dev.jstech.computers.operation.payload.files.FileEditPayloads;
import dev.jstech.computers.operation.payload.files.FilePayloads;
import dev.jstech.computers.operation.payload.files.FileTransferPayloads;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.operation.payload.firmware.InstallerPayloads;
import dev.jstech.computers.operation.payload.gateway.GatewayManagerPayloads;
import dev.jstech.computers.operation.payload.interactor.ItemInfoPayloads;
import dev.jstech.computers.operation.payload.interactor.NetworkInteractorPayloads;
import dev.jstech.computers.operation.payload.iql.IqlPayloads;
import dev.jstech.computers.operation.payload.machine.MachinePayloads;
import dev.jstech.computers.operation.payload.network.NetworkPayloads;
import dev.jstech.computers.operation.payload.operations.OperationsPayloads;
import dev.jstech.computers.operation.payload.program.ConsolePayloads;
import dev.jstech.computers.operation.payload.program.DesktopShellPayloads;
import dev.jstech.computers.operation.payload.program.ProgramPayloads;
import dev.jstech.computers.operation.payload.terminal.TerminalLocalPayloads;
import dev.jstech.computers.operation.payload.terminal.TerminalPayloads;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Hands the payload registrar to each feature class, which registers the payloads it handles beside their
 * handlers, so the gate on a payload a client sends is read in the same place as the code it protects.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class ComputingPayloads {

    private ComputingPayloads() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        ProgramPayloads.register(registrar);
        ConsolePayloads.register(registrar);
        DesktopShellPayloads.register(registrar);
        FilePayloads.register(registrar);
        FileEditPayloads.register(registrar);
        FileTransferPayloads.register(registrar);
        DesktopPayloads.register(registrar);
        ThisPcPayloads.register(registrar);
        WelcomePayloads.register(registrar);
        WorkstationInfoPayloads.register(registrar);
        IqlPayloads.register(registrar);
        TerminalPayloads.register(registrar);
        TerminalLocalPayloads.register(registrar);
        NetworkInteractorPayloads.register(registrar);
        ItemInfoPayloads.register(registrar);
        OperationsPayloads.register(registrar);
        CraftingPayloads.register(registrar);
        CraftPlannerPayloads.register(registrar);
        CraftManagerPayloads.register(registrar);
        NetworkPayloads.register(registrar);
        MachinePayloads.register(registrar);
        FirmwarePayloads.register(registrar);
        InstallerPayloads.register(registrar);
        AutomationPayloads.register(registrar);
        ClusterManagerPayloads.register(registrar);
        GatewayManagerPayloads.register(registrar);
    }
}
