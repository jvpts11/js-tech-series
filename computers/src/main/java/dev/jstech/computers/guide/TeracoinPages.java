/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages on Teracoin (computers/docs/design/teracoin.md): the coin and the Crypto Wallet; mining, with the miner,
 * the Mining Computer and the ASICs; the Mining Pool and the Mining Panel; and the 3D printers that turn TRC into
 * items.
 */
final class TeracoinPages {

    private TeracoinPages() {
    }

    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("cryptocurrency").titled("Teracoin")
                .icon(ComputingModule.ASIC_FORGE_S1).in(chapter).register();
        coin(guide, section);
        mining(guide, section);
        pools(guide, section);
        printers(guide, section);
    }

    private static void coin(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("teracoin", section).titled("Teracoin and the Crypto Wallet").icon(ComputingModule.MONITOR)
                .paragraph("Teracoin, TRC for short, is the cryptocurrency of the world: one chain of blocks for the"
                        + " whole server, owned by nobody. Miners find its blocks and are paid in TRC. You keep yours"
                        + " in a wallet, send them to other players, and turn them into items on a 3D printer.")
                .define("Teracoin", "The world's cryptocurrency, TRC. A TRC divides into eight decimal places.")
                .define("Wallet", "A file on a computer's disk that keeps the key of one address. Whoever has the"
                        + " file can spend what the address holds.")
                .define("Crypto Wallet", "Blockwright's program that makes wallets, shows their balance and their"
                        + " history, sends TRC and prints with them.")
                .paragraph("Install the Crypto Wallet on a computer of the Transition or later. The first time you open"
                        + " it, it offers to make a wallet: give it a name, and it saves the wallet's file in the"
                        + " Wallets folder of the computer's documents. With a USB stick in the computer, tick the box"
                        + " to save a copy on it as well.")
                .paragraph("The coins live on the chain, at the wallet's address, not in its file. Copying the file"
                        + " copies the access, not the coins: whoever has a copy spends the same balance. Lose every"
                        + " copy and the TRC are gone for good, so keep a copy somewhere safe.")
                .table("The wallet's tabs", "Tab", "What it does")
                .row(GuideCell.words("Overview"), GuideCell.words("The balance, what waits for the next block, and"
                        + " the latest moves."))
                .row(GuideCell.words("Send"), GuideCell.words("Sends TRC to an address. A send has no fee: it waits"
                        + " for the next block, or ten minutes when no block comes."))
                .row(GuideCell.words("Receive"), GuideCell.words("The wallet's address, to copy, also drawn as a"
                        + " square of blocks to show across a room."))
                .row(GuideCell.words("Transactions"), GuideCell.words("Every move of the wallet: received, sent,"
                        + " paid by a pool and printed."))
                .row(GuideCell.words("Print"), GuideCell.words("Prints items on the 3D printers linked to the"
                        + " computer ([](jsc:printers_3d))."))
                .row(GuideCell.words("Wallets"), GuideCell.words("The wallets on this computer, to open one or make"
                        + " another."))
                .subheading("Blocks, the difficulty and the reward")
                .paragraph("A block comes about every ten minutes. Each second, each miner's chance of finding it is"
                        + " its hashrate divided by the difficulty, which changes every ten blocks to keep that pace."
                        + " A block pays 50 TRC, halving every 2,100 blocks, and one block in twenty also brings a rare"
                        + " item, such as a diamond, into the network storage of the machine that found it. Your"
                        + " server's settings may give other numbers.")
                .ifSomethingGoesWrong(
                        "The wallet says its file no longer holds its key.",
                        "The file is gone or was changed. Open another wallet in the Wallets tab, or copy the file"
                                + " back from where you kept it.",
                        "A send says that is not a Teracoin address.",
                        "Copy the address again from the other wallet's Receive tab: it starts with trc1q.",
                        "A send waits a long time.",
                        "It waits for the next block. When none comes for ten minutes, it goes through on its own.")
                .seeAlso("jsc:mining", "jsc:mining_pools", "jsc:printers_3d")
                .register();
    }

    private static void mining(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mining", section).titled("Mining: the miner, the Mining Computer and the ASICs")
                .icon(ComputingModule.MINING_COMPUTER_STANDARD)
                .covers(ComputingModule.MINING_COMPUTER_TRANSITION).covers(ComputingModule.MINING_COMPUTER_STANDARD)
                .covers(ComputingModule.MINING_COMPUTER_ADVANCED).covers(HardwareItems.MOTHERBOARD_MNG_TRANSITION)
                .covers(HardwareItems.MOTHERBOARD_MNG_STANDARD).covers(HardwareItems.MOTHERBOARD_MNG_ADVANCED)
                .covers(HardwareItems.MINING_RISER_TRANSITION).covers(HardwareItems.MINING_RISER_STANDARD)
                .covers(HardwareItems.MINING_RISER_ADVANCED).covers(ComputingModule.ASIC_FORGE_S1)
                .covers(ComputingModule.ASIC_FORGE_A2)
                .paragraph("Mining is how TRC are made. A miner spends its hashrate on finding the next block, and the"
                        + " block's reward goes to the miner's payout address: the address of your wallet, from its"
                        + " Receive tab. Mining draws the watts of the parts that mine, which is its real cost.")
                .define("Hashrate", "How many hashes a second a miner tries. A processor mines at its capacity, a"
                        + " graphics card at ten times its power.")
                .define("Blockwright miner", "The program that mines on a computer, at its prompt or in the"
                        + " background.")
                .subheading("The miner at the prompt")
                .paragraph("Install the Blockwright miner, then give it your address: miner --address and the address."
                        + " Type miner to watch it mine, and Q to stop. miner start mines in the background, whenever"
                        + " the computer is on, and miner stop ends it; miner status says how it goes. miner --pool and"
                        + " a pool's name mines for that pool, and miner --alone for you alone.")
                .command(CommandGuidePages.id(Platform.MC_NET, "miner"))
                .subheading("The Mining Computer")
                .paragraph("The Mining Computer is a mining rig: an open frame that holds a mining board, a processor,"
                        + " memory, one power supply, one disk and graphics cards on risers, all in view. The"
                        + " Transition's stands in one block and takes 4 cards; the Standard's and the Advanced's are"
                        + " two blocks wide and take 6 and 8. It is a real computer, with no monitor needed: you reach"
                        + " it by ssh ([](jsc:remote_access)) or watch it in the Mining Panel.")
                .steps("Place the Mining Computer and open it.",
                        "Put in the mining board of its era, a processor, memory, a power supply, a disk, and a riser"
                                + " for each card it will carry.",
                        "Switch it off. With a graphics card in your hand, aim at a free riser and use it to seat the"
                                + " card. Sneak with an empty hand to take a card out.",
                        "Install a system and the miner, set the payout address, and turn on Mine at start so it mines"
                                + " whenever it is on.")
                .paragraph("A riser takes the cards of its era or older. On a riser a card mines at its full power:"
                        + " mining ignores the slot's cut.")
                .subheading("The ASICs")
                .paragraph("An ASIC is a block made only to mine, by Blockwright. It has no system: its window sets its"
                        + " payout address and its pool. Give it energy and put it on the network with a data cable;"
                        + " it mines while it has its watts.")
                .table("What mines how much", "Miner", "Hashrate", "Draws")
                .row(GuideCell.words("A Transition rig of four cards"), GuideCell.words("about 13,600 H/s"),
                        GuideCell.words("its parts' watts"))
                .row(GuideCell.words("A Standard rig of six cards"), GuideCell.words("about 157,000 H/s"),
                        GuideCell.words("its parts' watts"))
                .row(GuideCell.words("An Advanced rig of eight cards"), GuideCell.words("about 467,000 H/s"),
                        GuideCell.words("its parts' watts"))
                .row(GuideCell.words("Forge S1 (Standard)"), GuideCell.words("1,000,000 H/s"),
                        GuideCell.words("1,400 W"))
                .row(GuideCell.words("Forge A2 (Advanced)"), GuideCell.words("10,000,000 H/s"),
                        GuideCell.words("3,200 W"))
                .ifSomethingGoesWrong(
                        "A card will not seat on a riser.",
                        "Switch the rig off, aim at a free riser, and check the card is of the riser's era or older.",
                        "The miner says it has no address to pay.",
                        "Give it one: miner --address and your wallet's address, or the address in the ASIC's window.",
                        "The miner says nothing on the machine hashes.",
                        "Put in a processor, or seat graphics cards on the rig's risers.")
                .seeAlso("jsc:teracoin", "jsc:mining_pools")
                .register();
    }

    private static void pools(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mining_pools", section).titled("Mining Pools and the Mining Panel").icon(ComputingModule.SERVER)
                .paragraph("Mining alone, you find a block now and then. A Mining Pool gathers the miners of a network"
                        + " that choose it and mines with all of them together; when it finds a block, it pays each"
                        + " miner its share. That is a steady income instead of a block now and then.")
                .define("Mining Pool", "A service on a rack server that mines with its miners together and shares"
                        + " each block's reward by the work each gave.")
                .define("Mining Panel", "A desktop program that shows the pools of a network: their hashrate, their"
                        + " miners and the blocks they found.")
                .paragraph("Install the Mining Pool on a server in a rack of the network, from the Transition on"
                        + " ([](jsc:network_services)). A miner chooses it by its name, with miner --pool and the name,"
                        + " or with the pool button of an ASIC's window.")
                .paragraph("When the pool finds a block, it shares the reward by the work each miner gave since its"
                        + " last block, the miner's hashrate times the time it mined, less the pool's fee. The fee goes"
                        + " to the owner's address; the owner sets it from 0 to 5%, and it starts at 1%.")
                .table("The Mining Panel's tabs", "Tab", "What it shows")
                .row(GuideCell.words("Pool"), GuideCell.words("The pool's hashrate, what it pays an hour, a day and"
                        + " a week, the blocks it found and the difficulty."))
                .row(GuideCell.words("Miners"), GuideCell.words("Each miner, its kind, the address it pays, its"
                        + " hashrate and its share."))
                .row(GuideCell.words("Blocks"), GuideCell.words("The blocks the pool found, and who found each."))
                .row(GuideCell.words("Settings"), GuideCell.words("Only at the pool's own server: its name, its fee,"
                        + " its owner's address, who may join, and the miners asking to."))
                .ifSomethingGoesWrong(
                        "A miner says its pool is not on the network.",
                        "Check the pool's name in the Mining Panel, and give it again with miner --pool.",
                        "A miner says it waits for the owner.",
                        "The pool takes only the miners its owner accepts. The owner accepts it in the Settings tab,"
                                + " at the pool's own server.")
                .seeAlso("jsc:mining", "jsc:network_services")
                .register();
    }

    private static void printers(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("printers_3d", section).titled("3D printers: TRC into items").icon(ComputingModule.PROSA_I3_MK3)
                .covers(ComputingModule.PROSA_I3_MK3).covers(ComputingModule.RATTAN_LAB_X1)
                .paragraph("A 3D printer turns TRC into items. It prints the real item, a row at a time from the"
                        + " bottom, and puts it out by itself. The Prosa i3 MK3 of the Standard is an open frame that"
                        + " slides each item into its tray; the Rattan Lab X1 of the Advanced is closed, its bed a"
                        + " belt, and prints twice as fast.")
                .paragraph("Link the printer to your computer with a peripheral cable ([](jsc:peripheral_cables)). The"
                        + " server's settings list what can be printed and the price of each, and that list is all a"
                        + " printer prints.")
                .steps("Open the Crypto Wallet and its Print tab.",
                        "Choose the printer among those linked to the computer.",
                        "Click the item, and set how many copies.",
                        "Press Print.")
                .paragraph("Each item is paid as it starts, from the wallet that sent the job, at once. A wallet short"
                        + " of the price makes the printer wait until it has it; cancel, and nothing more is paid. The"
                        + " TRC paid leave the world for good. A printer draws no energy and uses no filament: its"
                        + " spools are only how it looks.")
                .paragraph("The printer's window shows the item at work and its layer, the queue with the wallet that"
                        + " pays each job, and the output's three slots. A full output makes it wait.")
                .subheading("Auto print")
                .paragraph("Auto print, a setting of each wallet, prints one item by itself as the balance grows. You"
                        + " give it the item, the printer and how much the wallet keeps: whenever the balance passes"
                        + " what it keeps by the price of one, one is printed. It runs while the computer holding the"
                        + " wallet's file is on, its window open or not.")
                .table("The starting list", "Item", "TRC")
                .row(GuideCell.words("Emerald"), GuideCell.words("5"))
                .row(GuideCell.words("Diamond"), GuideCell.words("10"))
                .row(GuideCell.words("Music disc"), GuideCell.words("15"))
                .row(GuideCell.words("Shulker shell"), GuideCell.words("20"))
                .row(GuideCell.words("Ancient debris"), GuideCell.words("30"))
                .row(GuideCell.words("Totem of Undying"), GuideCell.words("60"))
                .row(GuideCell.words("Netherite ingot"), GuideCell.words("80"))
                .row(GuideCell.words("Heart of the Sea"), GuideCell.words("100"))
                .row(GuideCell.words("Enchanted golden apple"), GuideCell.words("120"))
                .row(GuideCell.words("Trident"), GuideCell.words("150"))
                .row(GuideCell.words("Elytra"), GuideCell.words("400"))
                .row(GuideCell.words("Nether star"), GuideCell.words("800"))
                .ifSomethingGoesWrong(
                        "The Print tab says no 3D printer is linked to this computer.",
                        "Link one with a peripheral cable to this computer.",
                        "The printer waits for TRC.",
                        "The wallet holds less than the price. Send it TRC, or cancel the job.",
                        "The printer waits for the wallet.",
                        "The computer that sent the job is off, or the wallet's file is gone from it.",
                        "The printer says its output is full.",
                        "Take the items out of its output.")
                .seeAlso("jsc:teracoin", "jsc:peripheral_cables")
                .register();
    }
}
