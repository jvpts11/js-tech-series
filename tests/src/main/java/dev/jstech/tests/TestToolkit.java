/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.capability.CapabilityKind;
import dev.jstech.core.capability.CapabilityKinds;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.IPartHost;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.text.TextKey;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * What the Core's kit for those who make mods is proved with: a capability of the test mod's own, offered by the test
 * kiln on its top and by a probe part on whatever face of a cable it is mounted, a part whose blade turns, and a call
 * made from a chest's screen.
 */
public final class TestToolkit {

    /** What the kiln's probe reads on its top. */
    public static final int KILN_READING = 7;
    /** What a probe part reads on its face. */
    public static final int PART_READING = 11;

    /** A capability of the test mod's own, which hands over a reading. */
    public static final CapabilityKind<IProbe> PROBE = CapabilityKinds.declare(rl("probe"), IProbe.class);
    /** A call a chest's screen makes, setting {@link #CALLED}. */
    public static final MenuCall<ChestMenu, Integer> SET = MenuCalls.declare(rl("set"), ChestMenu.class,
            ByteBufCodecs.VAR_INT, (menu, player, value) -> TestToolkit.CALLED.set(value));
    /** What the last call set. */
    public static final AtomicInteger CALLED = new AtomicInteger();

    private static final TextKey PROBE_NAME = TextKey.of("jstests.part.probe", "Probe part");
    private static final DeferredRegister<PartType<?>> PARTS = DeferredRegister.create(CoreParts.KEY, JsTests.MODID);
    /** A part that reads {@link #PART_READING} on its face; drawn as stone, which the game always has. */
    public static final DeferredHolder<PartType<?>, PartType<ProbePart>> PROBE_PART = PARTS.register("probe_part",
            () -> new PartType<>(ProbePart::new, PROBE_NAME, ResourceLocation.withDefaultNamespace("block/stone")));
    private static final TextKey FAN_NAME = TextKey.of("jstests.part.fan", "Fan part");
    /**
     * A part of a kind that moves, always at work: its model is the barrier's, which draws nothing, and its blade is
     * drawn each frame, turning.
     */
    public static final DeferredHolder<PartType<?>, PartType<FanPart>> FAN_PART = PARTS.register("fan_part",
            () -> new PartType<>(FanPart::new, FAN_NAME, ResourceLocation.withDefaultNamespace("block/barrier"))
                    .moving());

    private TestToolkit() {
    }

    /** Registers the part and the kiln's capability, from the test mod's constructor, on both sides. */
    public static void register(final IEventBus modBus) {
        PARTS.register(modBus);
        modBus.addListener(RegisterCapabilitiesEvent.class, TestToolkit::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        PROBE.onBlockEntity(event, TestMachines.MACHINE.get(),
                (machine, face) -> face == Direction.UP ? () -> KILN_READING : null);
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }

    /** What the probe capability hands over. */
    @FunctionalInterface
    public interface IProbe {

        int reading();
    }

    /** A part that offers the probe on its face and nothing else. */
    public static final class ProbePart implements IFacePart {

        @Override
        public PartType<?> type() {
            return PROBE_PART.get();
        }

        @Override
        public void attach(final IPartHost host, final Direction face) {
        }

        @Override
        public ItemStack partItem() {
            return ItemStack.EMPTY;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> @Nullable T capability(final BlockCapability<T, @Nullable Direction> capability) {
            return capability == PROBE.block() ? (T) (IProbe) () -> PART_READING : null;
        }
    }

    /** A part whose blade turns, always at work. */
    public static final class FanPart implements IFacePart {

        @Override
        public PartType<?> type() {
            return FAN_PART.get();
        }

        @Override
        public void attach(final IPartHost host, final Direction face) {
        }

        @Override
        public boolean busy() {
            return true;
        }

        @Override
        public ItemStack partItem() {
            return ItemStack.EMPTY;
        }
    }
}
