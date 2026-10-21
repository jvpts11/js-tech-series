/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.jstech.core.client.notification.NotificationsClient;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideTips;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.notification.NotificationAction;
import dev.jstech.core.text.GameText;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * The tip at the right time, on a player's game: once a second the inventory is looked over, and the first item in it
 * a manual explains that the player was never told of brings a quiet notification of information, whose click opens
 * the item's page. One tip a look, so a full inventory tells of its items one by one; once an item, each game keeping
 * the items it told of in {@code jstech/manual_tips.json} in its folder. Not in creative, where items are not picked
 * up but taken, nor for a spectator; and not at all when the player turned the tips off ({@link GuideTips}).
 */
public final class ManualTips {

    private static final Logger LOGGER = CoreLogs.GUIDE.logger();
    /** How often the inventory is looked over, in ticks. */
    private static final int EVERY_TICKS = 20;
    private static final String FOLDER = "jstech";
    private static final String FILE = "manual_tips.json";
    private static final Set<String> TOLD = new LinkedHashSet<>();

    private static boolean read;
    private static int ticks;

    private ManualTips() {
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ManualTips::onClientTick);
    }

    public static void onClientTick(final ClientTickEvent.Post event) {
        if (++ticks < EVERY_TICKS) {
            return;
        }
        ticks = 0;
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !GuideTips.onFirstPickup() || player.isCreative() || player.isSpectator()) {
            return;
        }
        final Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            final Optional<String> entry = GuideClient.entryOf(stack);
            final Optional<GuideManual> manual = entry.flatMap(found -> GuideLibrary.loaded().manualFor(found));
            if (manual.isPresent() && tell(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                NotificationsClient.show(GuideTips.TIP.of(GuideTips.TITLE.with(GameText.of(stack.getHoverName())),
                        GuideTips.PICKED_UP.text()).withAction(new NotificationAction.OpenManual(manual.get().id(),
                        entry.get())));
                return;
            }
        }
    }

    /** Whether the player was told of the item of that id. */
    public static synchronized boolean told(final String item) {
        readOnce();
        return TOLD.contains(item);
    }

    /** Notes that the player was told of the item of that id, and writes the disk; whether they had not been. */
    public static synchronized boolean tell(final String item) {
        readOnce();
        if (!TOLD.add(item)) {
            return false;
        }
        write();
        return true;
    }

    /** Forgets that the player was told of the item of that id, as a player who never picked it up (tests do). */
    public static synchronized void forget(final String item) {
        readOnce();
        if (TOLD.remove(item)) {
            write();
        }
    }

    private static void readOnce() {
        if (read) {
            return;
        }
        read = true;
        final Path file = path();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            final JsonElement all = JsonParser.parseString(Files.readString(file));
            if (all.isJsonArray()) {
                all.getAsJsonArray().forEach(item -> TOLD.add(item.getAsString()));
            }
        } catch (final IOException | RuntimeException e) {
            // A file spoiled by hand only brings the tips again.
            LOGGER.warn("The items the manuals' tips told of could not be read from {}", file, e);
        }
    }

    private static void write() {
        final JsonArray all = new JsonArray();
        TOLD.forEach(all::add);
        final Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, all.toString());
        } catch (final IOException e) {
            LOGGER.warn("The items the manuals' tips told of could not be written to {}", file, e);
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER).resolve(FILE);
    }
}
