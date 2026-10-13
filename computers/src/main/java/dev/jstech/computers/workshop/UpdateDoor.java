/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What every way into an UPDATE checks before it sends one, so the network's language, a program and the Network
 * Interactor refuse the same things in the same words: only a Personal Computer's own cards change items for the
 * network, the card has to be in it, the item has to take what is asked, and whoever asked has to be there to pay
 * for whatever is not free.
 */
@TextHolder
public final class UpdateDoor {

    public static final TextKey PC_ONLY = TextKey.of("jsc.update.pc_only",
            "only a Personal Computer's own cards change items for the network");
    public static final TextKey NO_CARD = TextKey.of("jsc.update.no_card", "this computer has no %s");
    public static final TextKey NOBODY = TextKey.of("jsc.update.nobody",
            "somebody has to ask for it: the card is paid for with their experience");
    public static final TextKey NO_SMELT = TextKey.of("jsc.update.no_smelt", "the %s does not smelt");
    public static final TextKey NO_ENCHANT = TextKey.of("jsc.update.no_enchant", "the %s takes no enchantment");
    public static final TextKey NOT_WORN = TextKey.of("jsc.update.not_worn", "the %s is not worn");
    public static final TextKey NO_MATERIAL = TextKey.of("jsc.update.no_material",
            "the network holds nothing that mends the %s");
    public static final TextKey OFFERS = TextKey.of("jsc.update.offers", "The offers for the %s:");
    /** One offer: which, the enchantment it shows as its clue, the level it needs and what it costs in levels. */
    public static final TextKey OFFER = TextKey.of("jsc.update.offer", "%s. %s . . . ? (level %s) for %s");
    public static final TextKey NO_OFFER = TextKey.of("jsc.update.no_offer", "%s. no offer");
    public static final TextKey LEVEL = TextKey.of("jsc.update.level", "%s level");
    public static final TextKey LEVELS = TextKey.of("jsc.update.levels", "%s levels");
    /** The most an anvil's repair uses of a material: each piece mends a quarter. */
    private static final int MOST_MATERIAL = 4;

    private UpdateDoor() {
    }

    /**
     * Why {@code host} cannot send {@code action} for {@code key} now, {@code payer} asking, or null when it can.
     * The network's own state (an engine, the item's stock) is the caller's to check.
     */
    @Nullable
    public static Text refusal(final Level level, @Nullable final Object host, final StorageKey key,
                               final UpdateAction action, @Nullable final ServerPlayer payer) {
        if (!(host instanceof PersonalComputerBlockEntity computer)) {
            return PC_ONLY.text();
        }
        if (!action.card().in(computer.workshopCards())) {
            return NO_CARD.with(GameText.of(cardItem(action.card()).getHoverName()));
        }
        if (action.paid() && payer == null) {
            return NOBODY.text();
        }
        return refusal(level, key, action);
    }

    /** Why the item {@code key} cannot be given {@code action} at all, or null when it can. */
    @Nullable
    public static Text refusal(final Level level, final StorageKey key, final UpdateAction action) {
        final ItemStack item = key.stack(1);
        final String name = key.displayName().getString();
        return switch (action) {
            case SMELT -> Workshop.smelt(level, item) == null ? NO_SMELT.with(name) : null;
            case ENCHANT -> item.isEnchantable() ? null : NO_ENCHANT.with(name);
            case REPAIR -> item.isDamageableItem() && item.isDamaged() ? null : NOT_WORN.with(name);
            case COMBINE, NAME -> null;
        };
    }

    /** Whether the item {@code key} can be given {@code action}, which a tab of the Update window greys out without. */
    public static boolean accepts(final Level level, final StorageKey key, final UpdateAction action) {
        return key.isItem() && refusal(level, key, action) == null;
    }

    /**
     * What mends the item {@code item} among what the network holds ({@code stock}): the material it holds most of, or
     * null when it holds none.
     */
    @Nullable
    public static StorageKey material(final Map<StorageKey, Long> stock, final ItemStack item) {
        StorageKey best = null;
        long most = 0L;
        for (final Map.Entry<StorageKey, Long> entry : stock.entrySet()) {
            final StorageKey key = entry.getKey();
            if (key.isItem() && entry.getValue() > most && key.item() != item.getItem()
                    && item.getItem().isValidRepairItem(item, key.stack(1))) {
                best = key;
                most = entry.getValue();
            }
        }
        return best;
    }

    /**
     * The three offers the Enchanting Card would make {@code payer} for {@code key}, as lines: the clue the table
     * shows, the level it needs and the levels the card asks.
     */
    public static Text offersText(final ServerPlayer payer, final StorageKey key) {
        final List<Text> lines = new ArrayList<>();
        lines.add(OFFERS.with(key.displayName().getString()));
        final List<Workshop.Offer> offers = Workshop.offersFor(payer, key.stack(1));
        for (int i = 0; i < offers.size(); i++) {
            final Workshop.Offer offer = offers.get(i);
            lines.add(offer.required() <= 0 ? NO_OFFER.with(i + 1)
                    : OFFER.with(i + 1, GameText.of(offer.clue()), offer.required(), levels(offer.levels())));
        }
        return TextLists.join("\n", lines);
    }

    /**
     * How much of a material a repair of {@code item} takes: enough to mend it fully, at a quarter of its durability a
     * piece and never more than an anvil uses at once.
     */
    public static int materialNeeded(final ItemStack item) {
        if (!item.isDamageableItem() || item.getMaxDamage() <= 0) {
            return 1;
        }
        final int quarter = Math.max(1, item.getMaxDamage() / MOST_MATERIAL);
        return Math.max(1, Math.min(MOST_MATERIAL, (item.getDamageValue() + quarter - 1) / quarter));
    }

    /** The item of {@code card}, for its name and its picture. */
    public static ItemStack cardItem(final WorkshopCard card) {
        return new ItemStack(switch (card) {
            case CRAFTING_TABLE -> ComputingModule.CRAFTING_TABLE_CARD.get();
            case FURNACE -> ComputingModule.FURNACE_CARD.get();
            case ENCHANTING -> ComputingModule.ENCHANTING_CARD.get();
            case ANVIL -> ComputingModule.ANVIL_CARD.get();
        });
    }

    /** {@code count} levels, in the words for one or for more. */
    public static Text levels(final int count) {
        return (count == 1 ? LEVEL : LEVELS).with(count);
    }
}
