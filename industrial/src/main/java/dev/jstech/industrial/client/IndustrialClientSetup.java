/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client;

import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.menu.ProcessingMachineMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.function.ToIntFunction;

/**
 * Client-only wiring for the Industrial module: binds each machine menu type to its screen so right-clicking a machine
 * opens the correct GUI. The processing machines share one screen, each in its own progress colour.
 */
@EventBusSubscriber(modid = JsIndustrial.MODID, value = Dist.CLIENT)
public final class IndustrialClientSetup {

    private IndustrialClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(IndustrialModule.MACERATOR_MENU.get(),
                processing(MachineScreenSupport.Colours::maceratorProgress));
        event.register(IndustrialModule.COAL_GENERATOR_MENU.get(), CoalGeneratorScreen::new);
        event.register(IndustrialModule.ELECTRIC_FURNACE_MENU.get(),
                processing(MachineScreenSupport.Colours::furnaceProgress));
        event.register(IndustrialModule.COMPRESSOR_MENU.get(),
                processing(MachineScreenSupport.Colours::compressorProgress));
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(IndustrialModule.TANK_BE.get(), TankRenderer::new);
    }

    private static MenuScreens.ScreenConstructor<ProcessingMachineMenu, ProcessingMachineScreen> processing(
            final ToIntFunction<MachineScreenSupport.Colours> progressColour) {
        return (menu, inventory, title) -> new ProcessingMachineScreen(menu, inventory, title, progressColour);
    }
}
