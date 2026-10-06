/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.client.AbstractComputerScreen;
import dev.jstech.computers.client.FirmwareScreen;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.InstallMode;
import dev.jstech.core.api.client.IGuideBlockRenderer;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * The firmware's setup as a manual pictures it: the very screen a computer shows, in the look its board's age gives
 * it (the text BIOS, the blue BIOS or the UEFI), built for an example machine of that age and drawn small in the room
 * the page keeps. So the picture reads in the reader's language and never falls behind the screen it shows.
 *
 * <p>An entry names the look in its data: {@code {"look": "cli_bios"}}, {@code "blue_bios"} or {@code "uefi"}.
 */
public final class FirmwarePictures implements IGuideBlockRenderer {

    /** The kind of drawing a manual names to picture a firmware. */
    public static final ResourceLocation KIND = ResourceLocation.fromNamespaceAndPath("jsc", "firmware");

    /* The setups built so far, one a look, and the language they were built in. */
    private final Map<FirmwareKind, AbstractComputerScreen<?>> built = new EnumMap<>(FirmwareKind.class);
    private String builtIn = "";

    @Override
    public void draw(final GuiGraphics graphics, final Font font, final int x, final int y, final int width,
                     final int height, final CompoundTag data, final int mouseX, final int mouseY) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        final String language = minecraft.getLanguageManager().getSelected();
        if (!language.equals(this.builtIn)) {
            this.built.clear();
            this.builtIn = language;
        }
        final FirmwareKind look = lookOf(data.getString("look"));
        final AbstractComputerScreen<?> screen = this.built.computeIfAbsent(look, FirmwarePictures::build);
        final float scale = Math.min(width / (float) MonitorGlass.WIDTH, height / (float) MonitorGlass.HEIGHT);
        final int shownWidth = Math.round(MonitorGlass.WIDTH * scale);
        final int shownHeight = Math.round(MonitorGlass.HEIGHT * scale);
        final int left = x + (width - shownWidth) / 2;
        final int top = y + (height - shownHeight) / 2;
        Draw.pushScissor(graphics, left, top, left + shownWidth, top + shownHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        // The screen lays itself out round the glass, as on a game window; the glass falls on the picture.
        graphics.pose().translate(-MonitorGlass.BESIDE / 2.0F, -MonitorGlass.ABOVE_AND_BELOW / 2.0F, 0.0F);
        screen.paintFace(graphics, 0.0F);
        graphics.pose().popPose();
        Draw.popScissor(graphics);
    }

    private static FirmwareKind lookOf(final String look) {
        for (final FirmwareKind kind : FirmwareKind.values()) {
            if (kind.name().toLowerCase(Locale.ROOT).equals(look)) {
                return kind;
            }
        }
        return FirmwareKind.UEFI;
    }

    /* The setup of an example machine of the age that wears the look, as its monitor would show it. */
    private static AbstractComputerScreen<?> build(final FirmwareKind look) {
        final Minecraft minecraft = Minecraft.getInstance();
        final HardwareEra era = switch (look) {
            case CLI_BIOS -> HardwareEra.VINTAGE;
            case BLUE_BIOS -> HardwareEra.LEGACY;
            case UEFI -> HardwareEra.STANDARD;
        };
        final FirmwareStatePayload state = new FirmwareStatePayload(BlockPos.ZERO, era.id(), machine(look, era), 0,
                -1, entries(look), FirmwareStatePayload.RaidInfo.ABSENT);
        FirmwareScreen.faceWith(look, nameOf(computerOf(era)), state);
        final MonitorSessionMenu menu = new MonitorSessionMenu(0, minecraft.player.getInventory(), BlockPos.ZERO,
                BlockPos.ZERO, era, MonitorSessionMenu.Phase.FIRMWARE);
        return AbstractComputerScreen.face(() -> new FirmwareScreen(menu, minecraft.player.getInventory(),
                Component.empty()), MonitorGlass.BESIDE, MonitorGlass.ABOVE_AND_BELOW);
    }

    private static FirmwareStatePayload.Machine machine(final FirmwareKind look, final HardwareEra era) {
        return switch (look) {
            case CLI_BIOS -> new FirmwareStatePayload.Machine(text(computerOf(era)),
                    text(HardwareItems.CPU_INTEGRA_486DX2), 1, 66, "IA-16", 16,
                    text(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE), 8, 2, 4, text(HardwareItems.RAM_SIMM_4),
                    text(HardwareItems.GPU_VGA_256), 1, 2, era.text());
            case BLUE_BIOS -> new FirmwareStatePayload.Machine(text(computerOf(era)),
                    text(HardwareItems.CPU_INTEGRA_PENTIX_4_540), 1, 3200, "x86", 32,
                    text(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775), 1024, 2, 4, text(HardwareItems.RAM_DDR_512),
                    text(HardwareItems.GPU_VERTEX_6600_GT), 1, 4, era.text());
            case UEFI -> new FirmwareStatePayload.Machine(text(computerOf(era)),
                    text(HardwareItems.CPU_INTEGRA_CENTRO_C5_4690K), 4, 3500, "x86-64", 64,
                    text(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150), 16384, 2, 4,
                    text(ComputingModule.RAM_DDR3_8192), text(HardwareItems.GPU_VERTEX_GTX_970), 1, 6, era.text());
        };
    }

    /* What the example machine boots from: a system on its disk, and a second one or an installer beside it. */
    private static List<FirmwareStatePayload.Entry> entries(final FirmwareKind look) {
        final int guided = InstallMode.GUIDED.id();
        return switch (look) {
            case CLI_BIOS -> List.of(
                    disk(0, Text.literal("MC-DOS"), text(HardwareItems.DISK_TRENCH_20M), "20 MB"),
                    medium(Text.literal("MC-DOS"), text(ComputingModule.FLOPPY_DRIVE), guided));
            case BLUE_BIOS -> List.of(
                    disk(0, Text.literal("Frames XP"), text(HardwareItems.DISK_LINK_IDE_40G), "40 GB"),
                    disk(1, Text.literal("Debian"), text(HardwareItems.DISK_LINK_IDE_20G), "20 GB"));
            case UEFI -> List.of(
                    disk(0, Text.literal("Frames 10"), text(() -> ComputingModule.disk(StorageTier.SSD, DiskSize.TB_1)),
                            "1 TB"),
                    medium(Text.literal("Frames 10"), text(ComputingModule.DOCK_STATION), guided));
        };
    }

    private static FirmwareStatePayload.Entry disk(final int slot, final Text system, final Text device,
                                                   final String size) {
        return new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_DISK, slot, "", system, device, size,
                Text.EMPTY, true, -1);
    }

    private static FirmwareStatePayload.Entry medium(final Text system, final Text drive, final int mode) {
        return new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, 1L, "", system, drive, "",
                Text.EMPTY, true, mode);
    }

    private static Supplier<? extends ItemLike> computerOf(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> ComputingModule.VINTAGE_PERSONAL_COMPUTER;
            case LEGACY -> ComputingModule.LEGACY_PERSONAL_COMPUTER;
            default -> ComputingModule.PERSONAL_COMPUTER;
        };
    }

    /* A part named as its tooltip names it, in the reader's language. */
    private static Text text(final Supplier<? extends ItemLike> item) {
        return Text.literal(nameOf(item));
    }

    private static String nameOf(final Supplier<? extends ItemLike> item) {
        return new ItemStack(item.get()).getHoverName().getString();
    }
}
