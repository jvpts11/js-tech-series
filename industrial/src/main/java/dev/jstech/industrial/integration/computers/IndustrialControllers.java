/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.item.CabinetBlockItem;
import dev.jstech.computers.os.HostScope;
import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.IndustrialModule;
import java.util.List;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;

/**
 * The Industrial Controller Computers, one in each case J's Computers ships: the Vintage, the Legacy and the
 * Transition in their one case each, the Standard and the Advanced in the Neutral, High Performance and Aesthetic
 * cases. A new machine, the Industrial Controller, whose case takes the parts a Personal Computer of its era takes.
 * Like all of J's Computers' hardware today they have no recipe.
 */
@TextHolder
public final class IndustrialControllers {

    /** The machine, as a sentence names it. */
    public static final TextKey AN_INDUSTRIAL_CONTROLLER = TextKey.of("jsindustrial.machine.industrial_controller",
            "an Industrial Controller Computer");
    /** What the package manager says of a program made for it asked onto another machine. */
    public static final TextKey INDUSTRIAL_CONTROLLER_ONLY = TextKey.of(
            "jsindustrial.machine.industrial_controller_only", "%s only installs on an Industrial Controller Computer");
    /** The machine every Industrial Controller Computer is, and that only the Fieldbus Cards are made for. */
    public static final HostScope MACHINE = new HostScope("jsindustrial:industrial_controller",
            AN_INDUSTRIAL_CONTROLLER, INDUSTRIAL_CONTROLLER_ONLY);
    /* How a case is fitted into an item slot, as J's Computers fits its own small computers. */
    private static final CabinetBlockItem.Fit FIT = new CabinetBlockItem.Fit(16.0F, 0.0F, -0.5F, 0.0F);
    /* The lamps on the front, dark on the item, and the parts of the bays, which the case as it comes has none of. */
    private static final List<String> AT_REST = List.of(ComputingLooks.COMPUTER_POWER_LAMP,
            ComputingLooks.COMPUTER_DISK_LAMP, ComputingLooks.COMPUTER_BAY_PARTS);

    public static final BlockEntry<IndustrialControllerBlock> INDUSTRIAL_CONTROLLER_COMPUTER = controller(
            "industrial_controller_computer", HardwareEra.STANDARD, CaseStyle.NEUTRAL)
            .named("Industrial Controller Computer").register();
    public static final BlockEntry<IndustrialControllerBlock> HIGH_PERFORMANCE_INDUSTRIAL_CONTROLLER_COMPUTER =
            controller("high_performance_industrial_controller_computer", HardwareEra.STANDARD,
                    CaseStyle.HIGH_PERFORMANCE).named("High Performance Industrial Controller Computer").register();
    public static final BlockEntry<IndustrialControllerBlock> AESTHETIC_INDUSTRIAL_CONTROLLER_COMPUTER = controller(
            "aesthetic_industrial_controller_computer", HardwareEra.STANDARD, CaseStyle.AESTHETIC)
            .named("Aesthetic Industrial Controller Computer").register();
    public static final BlockEntry<IndustrialControllerBlock> VINTAGE_INDUSTRIAL_CONTROLLER_COMPUTER = controller(
            "vintage_industrial_controller_computer", HardwareEra.VINTAGE, CaseStyle.SOLE)
            .named("Vintage Industrial Controller Computer").register();
    public static final BlockEntry<IndustrialControllerBlock> LEGACY_INDUSTRIAL_CONTROLLER_COMPUTER = controller(
            "legacy_industrial_controller_computer", HardwareEra.LEGACY, CaseStyle.SOLE)
            .named("Legacy Industrial Controller Computer").register();
    public static final BlockEntry<IndustrialControllerBlock> TRANSITION_INDUSTRIAL_CONTROLLER_COMPUTER = controller(
            "transition_industrial_controller_computer", HardwareEra.TRANSITION, CaseStyle.SOLE)
            .named("Transition Industrial Controller Computer").register();
    public static final BlockEntry<IndustrialControllerBlock> ADVANCED_INDUSTRIAL_CONTROLLER_COMPUTER = controller(
            "advanced_industrial_controller_computer", HardwareEra.ADVANCED, CaseStyle.NEUTRAL)
            .named("Advanced Industrial Controller Computer").register();
    public static final BlockEntry<IndustrialControllerBlock>
            ADVANCED_HIGH_PERFORMANCE_INDUSTRIAL_CONTROLLER_COMPUTER = controller(
                    "advanced_high_performance_industrial_controller_computer", HardwareEra.ADVANCED,
                    CaseStyle.HIGH_PERFORMANCE).named("Advanced High Performance Industrial Controller Computer")
            .register();
    public static final BlockEntry<IndustrialControllerBlock> ADVANCED_AESTHETIC_INDUSTRIAL_CONTROLLER_COMPUTER =
            controller("advanced_aesthetic_industrial_controller_computer", HardwareEra.ADVANCED,
                    CaseStyle.AESTHETIC).named("Advanced Aesthetic Industrial Controller Computer").register();
    /** Every Industrial Controller Computer, the oldest first. */
    public static final List<BlockEntry<IndustrialControllerBlock>> ALL = List.of(
            VINTAGE_INDUSTRIAL_CONTROLLER_COMPUTER, LEGACY_INDUSTRIAL_CONTROLLER_COMPUTER,
            TRANSITION_INDUSTRIAL_CONTROLLER_COMPUTER, INDUSTRIAL_CONTROLLER_COMPUTER,
            HIGH_PERFORMANCE_INDUSTRIAL_CONTROLLER_COMPUTER, AESTHETIC_INDUSTRIAL_CONTROLLER_COMPUTER,
            ADVANCED_INDUSTRIAL_CONTROLLER_COMPUTER, ADVANCED_HIGH_PERFORMANCE_INDUSTRIAL_CONTROLLER_COMPUTER,
            ADVANCED_AESTHETIC_INDUSTRIAL_CONTROLLER_COMPUTER);

    private IndustrialControllers() {
    }

    /**
     * What an Industrial Controller Computer of {@code era} is: the boards, the parts and the case of a Personal
     * Computer of that era, as the Industrial Controller.
     */
    public static ComputerDef computer(final Era era) {
        final ComputerDef personal = PersonalComputerBlockEntity.computer(era);
        return new ComputerDef(era, personal.formFactors(), MACHINE, personal.parts());
    }

    /**
     * Declares the computers, their cards and their cable, and listens for J's Computers' register event to make
     * them what they are; through the guard, once J's Computers is found installed.
     */
    static void declare(final IEventBus modEventBus) {
        FieldbusCards.declare();
        IndustrialControlCable.declare();
        modEventBus.addListener(ComputersRegisterEvent.class, IndustrialControllers::register);
    }

    /* The machine, the cards and each computer, on J's Computers' register event. */
    private static void register(final ComputersRegisterEvent event) {
        event.machine(MACHINE);
        FieldbusCards.register(event);
        for (final BlockEntry<IndustrialControllerBlock> controller : ALL) {
            event.computer(controller.get(), computer(controller.get().era()));
        }
    }

    /*
     * A controller, whose case its block entity draws as a Personal Computer's is: the block shows nothing but the
     * particles a break scatters, those of its case, and without noOcclusion the full cube would block its own light
     * and cull the faces of its neighbours. Its item shows the same case, switched off.
     */
    private static BlockBuilder<IndustrialControllerBlock> controller(final String id, final Era era,
                                                                       final CaseStyle style) {
        return IndustrialModule.CONTENT.block(id, properties -> new IndustrialControllerBlock(properties, era, style))
                .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.0F).noOcclusion())
                .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(id + "_body",
                        "jsc:block/computer/" + style.caseName(era) + "_particle")))
                .geo(ComputingLooks.COMPUTER)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.COMPUTER,
                        block.caseModel(), FIT, AT_REST))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(IndustrialModule.CONTROLLERS);
    }
}
