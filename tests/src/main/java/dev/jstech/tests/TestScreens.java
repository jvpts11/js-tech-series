/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValue;
import dev.jstech.core.menu.SyncedValue;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A menu whose screen is declared in a file, {@code assets/jstests/jstech/screens/test_machine.json}: two slots with a
 * bar between them, values of the menu shown by name (one past what the game's own menu data carries, one a word), a
 * button and a check box, and the player's inventory. What proves screens as data, values bound by name and styles.
 */
@TextHolder
public final class TestScreens {

    /** The screen's file. */
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_machine");
    /** A number past what a short carries, which only a synced value brings whole. */
    public static final long BIG = 40_000L;
    /** A word the menu shows. */
    public static final String WORD = "kiln";
    /** How far the bar is filled, of 100. */
    public static final int PROGRESS = 50;
    /** How many times the screen's button was pressed. */
    public static final AtomicInteger PRESSED = new AtomicInteger();

    public static final TextKey TITLE = TextKey.of("jstests.screen.test_machine", "Test Machine");
    public static final TextKey PRESS = TextKey.of("jstests.screen.press", "Press");
    public static final TextKey READY = TextKey.of("jstests.screen.ready", "Ready");
    /** What the narrator calls the box, which the file names for it. */
    public static final TextKey READY_SPOKEN = TextKey.of("jstests.screen.ready_spoken", "Ready to run");
    /** The words of the screens of controls and of views. */
    public static final TextKey CONTROLS = TextKey.of("jstests.screen.controls", "Controls");
    public static final TextKey VIEWS = TextKey.of("jstests.screen.views", "Views");
    public static final TextKey SPEED = TextKey.of("jstests.screen.speed", "Speed: %s%%");
    public static final TextKey SIGNAL = TextKey.of("jstests.screen.signal", "Signal: %s of 15");
    public static final TextKey EJECT = TextKey.of("jstests.screen.eject", "Auto-eject");
    public static final TextKey MODE_ALWAYS = TextKey.of("jstests.screen.mode_always", "Ignore redstone");
    public static final TextKey MODE_SIGNAL = TextKey.of("jstests.screen.mode_signal", "With a signal");
    public static final TextKey SORT = TextKey.of("jstests.screen.sort", "Sort: %s");
    public static final TextKey SORT_AMOUNT = TextKey.of("jstests.screen.sort_amount", "By amount");
    public static final TextKey SORT_NAME = TextKey.of("jstests.screen.sort_name", "By name");
    public static final TextKey POWER = TextKey.of("jstests.screen.power", "Power");
    public static final TextKey MADE = TextKey.of("jstests.screen.made", "Per min");
    public static final TextKey SHARE = TextKey.of("jstests.screen.share", "Energy");
    /** The screens of controls and of views, with no menu. */
    public static final ResourceLocation CONTROLS_FILE = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_controls");
    public static final ResourceLocation VIEWS_FILE = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_views");

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,
            JsTests.MODID);
    /** The menu's type. */
    public static final DeferredHolder<MenuType<?>, MenuType<TestDataMenu>> MENU = MENUS.register("data_screen",
            () -> new MenuType<>(TestDataMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private TestScreens() {
    }

    /** Registers the menu, from the test mod's constructor. */
    public static void register(final IEventBus modBus) {
        MENUS.register(modBus);
    }

    /** What opens the menu for a player. */
    public static MenuProvider provider() {
        return new SimpleMenuProvider((id, inventory, player) -> new TestDataMenu(id, inventory),
                GameText.component(TITLE));
    }

    /** The menu: two slots of its own, the player's inventory, and its values by name. */
    public static final class TestDataMenu extends CoreMenu {

        private final ItemStackHandler items = new ItemStackHandler(2);
        private final SyncedValue<Long> big;
        private final SyncedValue<String> word;
        private final MenuValue progress;

        private static final GuiLayout.SlotPosition ANYWHERE = new GuiLayout.SlotPosition(0, 0);

        public TestDataMenu(final int id, final Inventory inventory) {
            super(MENU.get(), id, inventory, player -> true);
            slots(slot(items, 0, ANYWHERE), outputSlot(items, 1, ANYWHERE));
            playerInventory(inventory, ANYWHERE);
            this.big = synced("big", ByteBufCodecs.VAR_LONG, 0L, () -> BIG);
            this.word = synced("word", ByteBufCodecs.STRING_UTF8, "", () -> WORD);
            this.progress = value("progress", () -> PROGRESS);
            flag("ready", () -> true);
        }

        /** The big number, as this copy of the menu has it. */
        public long big() {
            return big.get();
        }

        /** The word, as this copy of the menu has it. */
        public String word() {
            return word.get();
        }

        /** The bar's fill, as this copy of the menu has it. */
        public int progress() {
            return progress.get();
        }
    }
}
