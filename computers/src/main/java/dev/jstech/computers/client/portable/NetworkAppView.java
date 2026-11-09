/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.client.portable.IAppKit.Entry;
import dev.jstech.computers.client.portable.IAppKit.Tone;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.CraftPlanPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.operation.payload.portable.PortableNetworkPayload;
import dev.jstech.computers.operation.payload.portable.PortableViewPayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.Portables;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The network app (computers/docs/design/wireless.md), a simpler Network Interactor in its words: the Stock, searched,
 * each item opening to be requested into the device's own storage or crafted; the Device, what that storage holds,
 * each line taken into the inventory or returned, and the Deposit, the inventory line by line; and the Craft, what
 * the network makes, how many, its plan and the jobs running.
 */
final class NetworkAppView implements IMobileAppView {

    private static final int ASK_EVERY = 40;
    private static final long STEP = 16L;

    @Override
    public int askEvery() {
        return ASK_EVERY;
    }

    @Override
    @Nullable
    public PortableActionPayload ask(final UUID device, final MobileAppState state) {
        return PortableActionPayload.of(device, Action.NETWORK);
    }

    @Override
    public void paint(final AppPage page) {
        final MobileAppState state = page.state();
        final PortableNetworkPayload view = PortableApps.network(page.device());
        final String network = view == null || view.network().isEmpty() ? null : view.network();
        final boolean inner = state.item() != null || state.depositing();
        float y = page.kit().bar(page.c(), MobileApp.NETWORK, page.top(), title(state), network, inner);
        if (!inner) {
            y = page.kit().tabs(page.c(), y, List.of(say(AppTexts.STOCK), say(AppTexts.DEVICE),
                    say(AppTexts.CRAFT)), state.networkTab(), List.of(tab(state, MobileAppState.STOCK),
                    tab(state, MobileAppState.DEVICE), tab(state, MobileAppState.CRAFT)));
        }
        softKeys(page, view);
        if (view == null || view.network().isEmpty()) {
            page.kit().note(page.c(), y + 20, say(view == null ? MobileTexts.NO_NETWORK : AppTexts.OUT_OF_REACH),
                    Tone.DIM);
            return;
        }
        if (!view.online()) {
            page.kit().note(page.c(), y + 20, say(AppTexts.OFFLINE), Tone.WARN);
            return;
        }
        if (state.depositing()) {
            deposit(page, y);
        } else if (state.networkTab() == MobileAppState.DEVICE) {
            device(page, view, y);
        } else if (state.networkTab() == MobileAppState.CRAFT) {
            if (state.item() == null) {
                crafts(page, view, y);
            } else {
                craft(page, view, state.item(), y);
            }
        } else if (state.item() == null) {
            stock(page, view, y);
        } else {
            item(page, view, state.item(), y);
        }
    }

    /* The Stock: the search, the network's items by how many it holds, and what the device holds of its room. */
    private static void stock(final AppPage page, final PortableNetworkPayload view, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final MobileAppState state = page.state();
        float y = top;
        // Frames Pocket's lists, picked and opened by the soft key, had no search over them.
        if (!kit.picksBeforeOpening()) {
            y = kit.field(c, 4, top, c.width() - 8, state.search(),
                    say(AppTexts.SEARCH_TYPES.with(MobileParts.number(view.types()))),
                    state.typing() == MobileAppState.Field.SEARCH,
                    MobileTap.run(() -> state.typeInto(MobileAppState.Field.SEARCH))) + 3;
        }
        y = kit.columns(c, y, say(AppTexts.NAME_COLUMN), say(AppTexts.IN_NETWORK_COLUMN));
        final List<NetworkItemEntry> items = found(view.stock(), state.search());
        final float foot = page.bottom() - 13;
        final int rows = Math.max(1, (int) ((foot - y) / kit.rowPitch()));
        final int from = Math.min(page.frame().scroll(), Math.max(0, items.size() - rows));
        if (items.isEmpty()) {
            kit.note(c, y + 8, say(AppTexts.EMPTY_STOCK), Tone.DIM);
        }
        // On a look where a tap picks a row, the first is picked until another is.
        final StorageKey picked = !kit.picksBeforeOpening() ? null : state.picked() != null ? state.picked()
                : items.isEmpty() ? null : items.getFirst().key();
        for (int n = from; n < items.size() && n < from + rows; n++) {
            final NetworkItemEntry entry = items.get(n);
            final boolean low = belowLevel(page, entry.key(), entry.total());
            y = kit.row(c, y, entry.key().stack(1), name(entry.key()), MobileParts.number(entry.total()),
                    low ? Tone.BAD : Tone.TEXT, MobileTap.run(() -> {
                        if (kit.picksBeforeOpening()) {
                            state.pick(entry.key());
                        } else {
                            state.choose(entry.key());
                        }
                    }), entry.key().equals(picked));
        }
        kit.note(c, foot + 3, say(AppTexts.THIS_DEVICE_HOLDS.with(view.capacity() - view.free(), view.capacity())),
                Tone.DIM);
    }

    /* One item: how many the network holds, how many to request into the device, and crafting more. */
    private static void item(final AppPage page, final PortableNetworkPayload view, final StorageKey key,
                             final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final MobileAppState state = page.state();
        final float w = c.width();
        final long inNetwork = count(view.stock(), key);
        kit.card(c, 5, top, w - 10, 62);
        c.item(key.stack(1), w / 2 - 12, top + 4, 24);
        kit.note(c, top + 31, name(key), Tone.TEXT);
        kit.note(c, top + 41, say(AppTexts.IN_THE_NETWORK.with(MobileParts.number(inNetwork))), Tone.DIM);
        final Long level = levelOf(page, key);
        if (level != null && inNetwork < level) {
            kit.note(c, top + 51, say(AppTexts.BELOW.with(MobileParts.number(level))), Tone.WARN);
        }
        float y = top + 68;
        kit.card(c, 5, y, w - 10, 50);
        kit.section(c, y + 2, say(AppTexts.REQUEST));
        final long amount = Math.min(state.amount(), Math.max(1L, view.free()));
        stepper(page, y + 14, amount);
        kit.note(c, y + 32, say(AppTexts.INTO_THIS_DEVICE), Tone.DIM);
        kit.note(c, y + 41, say(AppTexts.FREE_OF.with(view.free(), view.capacity())), Tone.DIM);
        y += 56;
        if (view.free() > 0 && inNetwork > 0) {
            y = kit.button(c, 8, y, w - 16, say(AppTexts.REQUEST_N.with(amount)), true, MobileTap.run(() -> {
                page.send(PortableActionPayload.item(page.device(), Action.REQUEST, key, amount));
                state.networkTab(MobileAppState.DEVICE);
            })) + 4;
        }
        if (craftable(view, key)) {
            y = kit.button(c, 8, y, w - 16, say(AppTexts.CRAFT_MORE), false, MobileTap.run(() -> {
                state.networkTab(MobileAppState.CRAFT);
                choose(page, key);
            })) + 4;
        }
        kit.note(c, y + 4, say(AppTexts.TAKE_IT_IN_DEVICE), Tone.DIM);
    }

    /* The Device: its room, each item it holds taken into the inventory or returned, and the way to the Deposit. */
    private static void device(final AppPage page, final PortableNetworkPayload view, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final float w = c.width();
        kit.card(c, 5, top, w - 10, 22);
        kit.section(c, top + 2, say(AppTexts.THIS_DEVICE));
        final long used = view.capacity() - view.free();
        c.text(say(AppTexts.USED_OF.with(used, view.capacity())), w - 10, top + 3, kit.ink(Tone.DIM),
                MobileCanvas.SMALL, MobileCanvas.Align.RIGHT, false);
        kit.meter(c, 11, top + 14, w - 22, view.capacity() <= 0 ? 0 : (float) used / view.capacity(), Tone.TEXT);
        float y = top + 27;
        final float foot = page.bottom() - 32;
        if (view.stored().isEmpty()) {
            kit.note(c, y + 8, say(AppTexts.NOTHING_ON_DEVICE), Tone.DIM);
        }
        final float block = kit.rowPitch() + 18;
        final int rows = Math.max(1, (int) ((foot - y) / block));
        final int from = Math.min(page.frame().scroll(), Math.max(0, view.stored().size() - rows));
        for (int n = from; n < view.stored().size() && n < from + rows; n++) {
            final NetworkItemEntry entry = view.stored().get(n);
            kit.card(c, 5, y - 2, w - 10, block - 2);
            kit.row(c, y, entry.key().stack(1), name(entry.key()), MobileParts.number(entry.total()), Tone.TEXT,
                    null, false);
            final float half = (w - 24) / 2;
            kit.button(c, 10, y + kit.rowPitch(), half, say(AppTexts.TAKE), true, MobileTap.run(() ->
                    page.send(PortableActionPayload.item(page.device(), Action.TAKE, entry.key(), entry.total()))));
            kit.button(c, 14 + half, y + kit.rowPitch(), half, say(AppTexts.RETURN), false, MobileTap.run(() ->
                    page.send(PortableActionPayload.item(page.device(), Action.RETURN, entry.key(),
                            entry.total()))));
            y += block;
        }
        kit.button(c, 8, foot + 2, w - 16, say(AppTexts.DEPOSIT_DOTS), false,
                MobileTap.run(() -> page.state().deposit(true)));
        kit.note(c, foot + 21, say(AppTexts.TAKE_HINT), Tone.DIM);
    }

    /* The Deposit: the inventory line by line, each given to the network, or all of it at once. */
    private static void deposit(final AppPage page, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final float w = c.width();
        float y = kit.section(c, top, say(AppTexts.YOUR_INVENTORY));
        final List<Integer> slots = depositable();
        final float foot = page.bottom() - 30;
        final int rows = Math.max(1, (int) ((foot - y) / kit.rowPitch()));
        final int from = Math.min(page.frame().scroll(), Math.max(0, slots.size() - rows));
        final Inventory inventory = inventory();
        final String give = say(AppTexts.GIVE);
        final float giveW = c.width(give, MobileCanvas.SMALL) + 10;
        for (int n = from; n < slots.size() && n < from + rows && inventory != null; n++) {
            final int slot = slots.get(n);
            final ItemStack stack = inventory.getItem(slot);
            // The name, then the count, then the Give at the row's end.
            final String count = MobileParts.number(stack.getCount());
            final float countRight = w - giveW - 8;
            kit.row(c, y, stack, c.fit(stack.getHoverName().getString(), countRight - 24
                    - c.width(count, MobileCanvas.SMALL), MobileCanvas.SMALL), "", Tone.DIM, null, false);
            c.text(count, countRight, y + 3, kit.ink(Tone.DIM), MobileCanvas.SMALL, MobileCanvas.Align.RIGHT, false);
            kit.button(c, w - giveW - 4, y, giveW, give, false, MobileTap.run(() ->
                    page.send(PortableActionPayload.numbered(page.device(), Action.DEPOSIT, "", slot))));
            y += kit.rowPitch();
        }
        if (!slots.isEmpty()) {
            kit.button(c, 6, foot + 2, w - 12, say(AppTexts.DEPOSIT_ALL), true, MobileTap.run(() ->
                    page.send(PortableActionPayload.of(page.device(), Action.DEPOSIT_ALL))));
        }
        kit.note(c, foot + 19, say(AppTexts.INTO_THE_NETWORK), Tone.DIM);
    }

    /* The Craft: what the network makes, each opening to be made, and the jobs running on it. */
    private static void crafts(final AppPage page, final PortableNetworkPayload view, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        float y = top;
        if (view.crafts().isEmpty()) {
            kit.note(c, y + 8, say(AppTexts.NO_CRAFTS), Tone.DIM);
            y += 20;
        }
        final float jobsAt = page.bottom() - 16 - Math.min(2, view.jobs().size()) * 20.0F;
        final int rows = Math.max(1, (int) ((jobsAt - y) / kit.rowPitch()));
        final List<CraftCatalogPayload.Entry> crafts = view.crafts();
        final int from = Math.min(page.frame().scroll(), Math.max(0, crafts.size() - rows));
        for (int n = from; n < crafts.size() && n < from + rows; n++) {
            final CraftCatalogPayload.Entry entry = crafts.get(n);
            final StorageKey key = StorageKey.of(entry.result());
            y = kit.row(c, y, entry.result(), entry.label().isEmpty() ? name(key) : entry.label(), "",
                    Tone.TEXT, MobileTap.run(() -> choose(page, key)), false);
            c.disc(c.width() - 8, y - kit.rowPitch() + 6, 2.5F, dot(kit, entry.availability()));
        }
        jobs(page, view, jobsAt);
    }

    /* One craft: what it is made by, its ingredients by the plan, how many, crafting or planning, and the jobs. */
    private static void craft(final AppPage page, final PortableNetworkPayload view, final StorageKey key,
                              final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final MobileAppState state = page.state();
        final float w = c.width();
        final CraftCatalogPayload.Entry entry = entryOf(view, key);
        kit.card(c, 5, top, w - 10, 58);
        c.item(key.stack(1), 11, top + 4, 20);
        c.text(c.fit(entry == null || entry.label().isEmpty() ? name(key) : entry.label(), w - 50,
                MobileCanvas.NORMAL), 36, top + 5, kit.ink(Tone.TEXT), MobileCanvas.NORMAL);
        c.text(c.fit(say(entry != null && entry.multiStage() ? AppTexts.A_PIPELINE : AppTexts.A_PATTERN), w - 46,
                MobileCanvas.SMALL), 36, top + 15, kit.ink(Tone.DIM), MobileCanvas.SMALL);
        final CraftPlanPayload plan = planFor(key, state.amount());
        float x = 11;
        if (plan != null) {
            for (final CraftPlanPayload.Row row : plan.rows()) {
                if (x > w - 40) {
                    break;
                }
                c.item(row.item(), x, top + 27, 14);
                final String times = say(AppTexts.TIMES.with(MobileParts.number(row.need())));
                c.text(times, x + 16, top + 31, kit.ink(row.satisfied() ? Tone.TEXT : Tone.BAD), MobileCanvas.SMALL);
                x += 20 + c.width(times, MobileCanvas.SMALL);
            }
            final String verdict = plan.feasible() ? say(AppTexts.ALL_THERE)
                    : say(AppTexts.SHORT_OF.with(MobileParts.number(plan.maxFeasible())));
            kit.note(c, top + 46, verdict, plan.feasible() ? Tone.GOOD : Tone.WARN);
        }
        float y = top + 63;
        kit.card(c, 5, y, w - 10, 48);
        kit.section(c, y + 2, say(AppTexts.HOW_MANY));
        stepper(page, y + 12, state.amount());
        final float half = (w - 26) / 2;
        kit.button(c, 11, y + 31, half, say(AppTexts.CRAFT_N.with(state.amount())), true, MobileTap.run(() -> {
            page.send(PortableActionPayload.item(page.device(), Action.CRAFT, key, state.amount()));
            state.choose(null);
        }));
        kit.button(c, 15 + half, y + 31, half, say(AppTexts.PLAN_IT), false, MobileTap.run(() -> {
            PortableApps.forgetPlan();
            page.send(PortableActionPayload.item(page.device(), Action.PLAN, key, state.amount()));
        }));
        y += 54;
        jobs(page, view, y);
    }

    /* The jobs running on the network: what each makes, how far it has got, and its status. */
    private static void jobs(final AppPage page, final PortableNetworkPayload view, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final float w = c.width();
        float y = kit.section(c, top, say(AppTexts.RUNNING_ON.with(view.network())));
        if (view.jobs().isEmpty()) {
            kit.note(c, y + 1, say(AppTexts.NOTHING_RUNNING), Tone.DIM);
            return;
        }
        for (final OperationRecord job : view.jobs()) {
            if (y + 20 > page.bottom()) {
                break;
            }
            kit.card(c, 5, y - 1, w - 10, 19);
            c.item(job.key().stack(1), 9, y + 1, 12);
            c.text(c.fit(say(AppTexts.NAMED_TIMES.with(name(job.key()), MobileParts.number(job.requested()))),
                    w - 60, MobileCanvas.SMALL), 25, y + 1, kit.ink(Tone.TEXT), MobileCanvas.SMALL);
            final float share = job.requested() <= 0 ? 0 : Math.min(1.0F, (float) job.moved() / job.requested());
            c.text(MobileTexts.say(MobileTexts.PERCENT.with(Math.round(share * 100))), w - 9, y + 1,
                    kit.ink(Tone.DIM), MobileCanvas.SMALL, MobileCanvas.Align.RIGHT, false);
            kit.meter(c, 25, y + 10, w - 46, share, Tone.GOOD);
            c.text(ComputerTerminalScreen.statusLabel(job.status()), 25, y + 13.5F, kit.ink(Tone.DIM),
                    MobileCanvas.SMALL);
            y += 21;
        }
    }

    /* The minus, the count and the plus, centred across the page. */
    private static void stepper(final AppPage page, final float y, final long amount) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final MobileAppState state = page.state();
        final float w = c.width();
        kit.button(c, 12, y, 16, "-", false, MobileTap.run(() -> state.addAmount(-STEP)));
        kit.button(c, w - 28, y, 16, "+", false, MobileTap.run(() -> state.addAmount(STEP)));
        c.text(MobileParts.number(amount), w / 2, y, kit.ink(Tone.TEXT), MobileCanvas.BIG, MobileCanvas.Align.CENTRE,
                false);
    }

    /* Frames Pocket's soft keys: the left one requests the chosen item; the Menu holds the other pages. */
    private static void softKeys(final AppPage page, @Nullable final PortableNetworkPayload view) {
        final MobileAppState state = page.state();
        final List<Entry> menu = List.of(
                new Entry(say(AppTexts.REQUEST_DOTS), tab(state, MobileAppState.STOCK)),
                new Entry(say(AppTexts.ON_DEVICE_DOTS), tab(state, MobileAppState.DEVICE)),
                new Entry(say(AppTexts.DEPOSIT_DOTS), MobileTap.run(() -> {
                    state.networkTab(MobileAppState.DEVICE);
                    state.deposit(true);
                })),
                new Entry(say(AppTexts.CRAFT_DOTS), tab(state, MobileAppState.CRAFT)));
        final int at = state.depositing() ? 2 : state.networkTab() == MobileAppState.STOCK ? 0
                : state.networkTab() == MobileAppState.DEVICE ? 1 : 3;
        final List<NetworkItemEntry> items = view == null ? List.of() : found(view.stock(), state.search());
        final Entry left = state.networkTab() == MobileAppState.STOCK && state.item() == null && !items.isEmpty()
                ? new Entry(say(AppTexts.REQUEST), MobileTap.run(() -> state.choose(state.picked() != null
                        ? state.picked() : items.getFirst().key()))) : null;
        page.keys(left, menu, at);
    }

    private static MobileTap tab(final MobileAppState state, final int tab) {
        return MobileTap.run(() -> state.networkTab(tab));
    }

    /* Chooses the craft {@code key} and asks how the network would make the amount chosen. */
    private static void choose(final AppPage page, final StorageKey key) {
        page.state().choose(key);
        PortableApps.forgetPlan();
        page.send(PortableActionPayload.item(page.device(), Action.PLAN, key, page.state().amount()));
    }

    private static String title(final MobileAppState state) {
        return state.depositing() ? say(AppTexts.DEPOSIT) : MobileParts.name(MobileApp.NETWORK);
    }

    /* The items of the stock whose names have {@code search} in them, the most held first. */
    private static List<NetworkItemEntry> found(final List<NetworkItemEntry> stock, final String search) {
        final String wanted = search.strip().toLowerCase(Locale.ROOT);
        final List<NetworkItemEntry> out = new ArrayList<>();
        for (final NetworkItemEntry entry : stock) {
            if (entry.key().isItem() && (wanted.isEmpty() || name(entry.key()).toLowerCase(Locale.ROOT)
                    .contains(wanted))) {
                out.add(entry);
            }
        }
        out.sort(Comparator.comparingLong(NetworkItemEntry::total).reversed());
        return out;
    }

    /* The slots of the inventory with something in them to deposit: all but the portables. */
    private static List<Integer> depositable() {
        final Inventory inventory = inventory();
        final List<Integer> slots = new ArrayList<>();
        if (inventory == null) {
            return slots;
        }
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && Portables.id(stack) == null) {
                slots.add(slot);
            }
        }
        return slots;
    }

    @Nullable
    private static Inventory inventory() {
        return Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory();
    }

    /* Whether the device watches {@code key} and the network has fewer than its level. */
    private static boolean belowLevel(final AppPage page, final StorageKey key, final long count) {
        final Long level = levelOf(page, key);
        return level != null && count < level;
    }

    @Nullable
    private static Long levelOf(final AppPage page, final StorageKey key) {
        final PortableViewPayload system = PortableViews.of(page.device());
        if (system != null) {
            for (final PortableViewPayload.Watch watch : system.watched()) {
                if (watch.key().equals(key)) {
                    return watch.level();
                }
            }
        }
        return null;
    }

    private static long count(final List<NetworkItemEntry> entries, final StorageKey key) {
        for (final NetworkItemEntry entry : entries) {
            if (entry.key().equals(key)) {
                return entry.total();
            }
        }
        return 0L;
    }

    private static boolean craftable(final PortableNetworkPayload view, final StorageKey key) {
        return entryOf(view, key) != null;
    }

    @Nullable
    private static CraftCatalogPayload.Entry entryOf(final PortableNetworkPayload view, final StorageKey key) {
        for (final CraftCatalogPayload.Entry entry : view.crafts()) {
            if (StorageKey.of(entry.result()).equals(key)) {
                return entry;
            }
        }
        return null;
    }

    /* The plan last told, when it is for {@code amount} of {@code key}. */
    @Nullable
    private static CraftPlanPayload planFor(final StorageKey key, final long amount) {
        final CraftPlanPayload plan = PortableApps.plan();
        return plan != null && plan.quantity() == amount && StorageKey.of(plan.result()).equals(key) ? plan : null;
    }

    /* The stock dot of a craft: green when it can be made now, amber for some of it, red for none. */
    private static int dot(final IAppKit kit, final byte availability) {
        return kit.ink(availability == CraftCatalogPayload.DOT_GREEN ? Tone.GOOD
                : availability == CraftCatalogPayload.DOT_AMBER ? Tone.WARN : Tone.BAD);
    }

    private static String name(final StorageKey key) {
        return key.displayName().getString();
    }

    private static String say(final TextKey key) {
        return MobileTexts.say(key);
    }

    private static String say(final Text text) {
        return MobileTexts.say(text);
    }
}
