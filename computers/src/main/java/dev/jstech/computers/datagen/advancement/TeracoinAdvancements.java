/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import dev.jstech.computers.advancement.JscEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * The Teracoin tab (computers/docs/design/advancements.md): wallets, mining, the pools and the printers, rooted in the
 * first wallet, on a gold ground.
 */
public final class TeracoinAdvancements extends JscAdvancementTab {

    public TeracoinAdvancements() {
        super("teracoin", ResourceLocation.withDefaultNamespace("textures/block/gold_block.png"));
        this.root(Items.GOLD_NUGGET, "Not Your Keys, Not Your Coins", "Make a wallet in the Crypto Wallet",
                on(JscEvents.WALLET_MADE));
        this.goal("proof_of_work", "root", item("jsc:mining_computer_transition"), "Proof of Work", "Find a block",
                on(JscEvents.BLOCK_FOUND));
        this.task("pool_party", "root", Items.WATER_BUCKET, "Pool Party", "Join a mining pool",
                on(JscEvents.POOL_JOINED));
        this.goal("to_the_moon", "proof_of_work", Items.FIREWORK_ROCKET, "To the Moon",
                "Fill every riser of a Mining Computer", on(JscEvents.RISERS_FILLED));
        this.goal("application_specific", "proof_of_work", item("jsc:asic_forge_s1"), "Application-Specific",
                "Mine with an ASIC", on(JscEvents.ASIC_MINING));
        this.task("pizza_day", "root", Items.PUMPKIN_PIE, "Pizza Day", "Send TRC to another player",
                on(JscEvents.TRC_SENT));
        this.challenge("hodl", "pizza_day", Items.DIAMOND, "HODL",
                "Hold 1,000 TRC in one wallet for seven days without sending any", on(JscEvents.HODL));
        this.task("you_wouldnt_download_a_car", "root", item("jsc:prosa_i3_mk3"), "You Wouldn't Download a Car",
                "Print an item on a 3D printer", on(JscEvents.PRINTED_3D));
        this.secret("the_halvening", "proof_of_work", Items.CLOCK, "The Halvening",
                "Be there when the reward halves", on(JscEvents.HALVING));
    }
}
