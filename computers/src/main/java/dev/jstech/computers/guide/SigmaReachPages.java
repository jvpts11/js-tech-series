/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Pages of the library chapter of the Book of Sigma on what a program reaches beyond its machine, which came with the
 * third version: other programs through the network's event log and its channels, the moving structure its computer
 * stands on, and Teracoin.
 */
final class SigmaReachPages {

    private SigmaReachPages() {
    }

    /** Declares the section and its pages, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("library_reach").titled("Messages, flying and Teracoin")
                .in(chapter).register();
        messaging(guide, section);
        navigation(guide, section);
        teracoin(guide, section);
    }

    private static void messaging(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("messaging_library", section).titled("EventLog and Channel: speaking to other programs")
                .paragraph("Your program can write to the network's event log, and it can send values to programs on"
                        + " other computers. You do both through EventLog and Channel, built-in types of"
                        + " System.Network. Both came with Sigma# 3, and both need the machine to be on a network: on"
                        + " a machine that is on none, a call stops the program with a NetworkException, which you can"
                        + " catch.")
                .table("EventLog and Channel members", "Member", "What it does")
                .row(GuideCell.code("EventLog.Write(level, text)"), GuideCell.words("Writes an entry to the network's"
                        + " event log, from this machine, at the level info, warn or error. The entry is one line of up"
                        + " to 256 characters. Every program listening through Network.OnEvent hears it."))
                .row(GuideCell.code("Channel.Listen(name, handler)"), GuideCell.words("Calls the handler with a"
                        + " ChannelMessage each time a value is sent on the channel of that name. A later call for the"
                        + " same channel takes the place of the one before."))
                .row(GuideCell.code("Channel.Close(name)"), GuideCell.words("Stops hearing the channel."))
                .row(GuideCell.code("Channel.Send(name, value)"), GuideCell.words("Sends a copy of the value on the"
                        + " channel to the programs of the other machines that hear it: on this network, and on the"
                        + " networks federated with it that let it send there."))
                .table("Records of Channel", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("ChannelMessage"), GuideCell.code("ChannelMessage.Channel, ChannelMessage.From, "
                                + "ChannelMessage.Value, ChannelMessage.Tick"),
                        GuideCell.words("The channel it came on, the machine that sent it, the copy of the value, and"
                                + " the tick it arrived on."))
                .paragraph("A channel's name is up to 64 characters. A value travels as a copy, as a"
                        + " GenericComponent's Data does: text, numbers, yes and no, and lists, maps and records of"
                        + " them, up to 32 KB in all. When you change your copy, nothing changes at the sender.")
                .paragraph("A value takes a tick to cross its network, one more when the computer that sends it is on"
                        + " Wi-Fi, and one for each border between federated networks it crosses. Sending costs 200"
                        + " credits, as asking for an Operation does; an entry in the log costs 100 credits, as a write"
                        + " to a file does. Listening costs nothing, the handler runs on your program's credits, and"
                        + " what your program hears is kept when the world is saved.")
                .listing("Door.sgs",
                        "Channel.Listen(\"door\", message =>",
                        "        Console.PrintLine(message.From + \" says \" + message.Value));",
                        "Channel.Send(\"door\", \"open\");",
                        "EventLog.Write(\"warn\", \"The north door is open\");")
                .ifSomethingGoesWrong(
                        "The program stops and says this computer is not on a network.",
                        "EventLog and Channel need a network. Check Network.Online first.",
                        "A program never hears what you send.",
                        "It hears only a channel it listens to, by the same name, and only from another machine. Across"
                                + " a federation, check that it lets the sender's network send to the listener's.")
                .seeAlso("jsc:network_watches", "jsc:wan_gateways")
                .register();
    }

    private static void navigation(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("navigation_library", section).titled("Navigation: flying the structure your computer stands on")
                .paragraph("A computer built onto a moving structure of J's Core, a ship, an airship or a cart, can fly"
                        + " it. Your program tells the structure's autopilot where to go, and reads what a flight"
                        + " sensor would read where the computer stands. You do both through Navigation, a built-in"
                        + " type of System.Navigation. Navigation came with Sigma# 3.")
                .table("Navigation readings", "Member", "What it gives")
                .row(GuideCell.code("Navigation.Aboard"), GuideCell.words("Yes when the computer stands on a moving"
                        + " structure."))
                .row(GuideCell.code("Navigation.Piloted"), GuideCell.words("Yes while an autopilot flies it."))
                .row(GuideCell.code("Navigation.X, Navigation.Z"), GuideCell.words("Where the computer is in the world,"
                        + " east and south, in metres."))
                .row(GuideCell.code("Navigation.Altitude"), GuideCell.words("How high it is, in metres."))
                .row(GuideCell.code("Navigation.Pressure"), GuideCell.words("The air pressure there, in kilopascals."))
                .row(GuideCell.code("Navigation.Speed"), GuideCell.words("How fast it goes, in metres a second."))
                .row(GuideCell.code("Navigation.Climb"), GuideCell.words("How fast it goes up, in metres a second;"
                        + " below nothing when it goes down."))
                .row(GuideCell.code("Navigation.Pitch, Navigation.Roll"), GuideCell.words("How far its nose is up and"
                        + " how far it leans to the side, in degrees."))
                .row(GuideCell.code("Navigation.Heading"), GuideCell.words("Where it looks, in degrees from north round"
                        + " by east."))
                .row(GuideCell.code("Navigation.AboveGround, Navigation.ClearAhead"), GuideCell.words("How far down"
                        + " the ground is, and how far ahead the first solid block is, in metres, up to 64 metres."))
                .table("Navigation commands", "Member", "What it does")
                .row(GuideCell.code("Navigation.GoTo(x, y, z, speed)"), GuideCell.words("Flies to that place of the"
                        + " world and holds there."))
                .row(GuideCell.code("Navigation.HoldAltitude(y, speed)"), GuideCell.words("Holds that height, and"
                        + " leaves the other controls to the helm."))
                .row(GuideCell.code("Navigation.FollowRoute(points, speed, loop)"), GuideCell.words("Flies to each"
                        + " point in turn: round again with loop yes, else holding at the last."))
                .row(GuideCell.code("Navigation.Dock(x, y, z, speed)"), GuideCell.words("Docks to the clamp of another"
                        + " structure at that place of the world."))
                .row(GuideCell.code("Navigation.Release()"), GuideCell.words("Takes the autopilot off."))
                .paragraph("Each command gives yes when the computer stands on a structure to fly, and no on the"
                        + " ground, where the readings are those of the place the computer stands, still. A speed is"
                        + " the fastest the autopilot goes, from 0.01 to 100000 metres a second; any other stops your"
                        + " program with an ArgumentOutOfRangeException. Each reading costs a glance, and each command"
                        + " costs what a write does.")
                .paragraph("A route is a List<double> of points, each written as its x, y and z, with up to 256"
                        + " points. Dock gives no when no other structure has a clamp at that place; the autopilot"
                        + " comes in slowly from off the clamp's face and lets go once the clamps hold. A pilot at the"
                        + " helm still steers whatever the autopilot does.")
                .listing("Patrol.sgs",
                        "if (Navigation.Aboard && !Navigation.Piloted) {",
                        "    Navigation.HoldAltitude(120, 4);",
                        "}")
                .define("Autopilot", "The part of a moving structure that works its engines towards a place, a height"
                        + " or a clamp, so that you need not hold the helm.")
                .ifSomethingGoesWrong(
                        "Every command gives no.",
                        "The computer is not on a moving structure. Build it onto one, and check Navigation.Aboard.",
                        "The structure does not move.",
                        "It needs engines that push it the way it is told to go.")
                .seeAlso("jsc:sensors_library", "jsc:signals_library")
                .register();
    }

    private static void teracoin(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("teracoin_library", section).titled("Teracoin: wallets, the chain, the miner and the pools")
                .paragraph("Your program can reach Teracoin: the balance of any address, the wallets in your"
                        + " computer's files, the chain, your computer's own miner and the Mining Pools of its network."
                        + " You do it through Wallet, Chain, Miner and Pool, built-in types of System.Teracoin. They"
                        + " came with Sigma# 3.")
                .paragraph("Amounts are whole hundred-millionths of a TRC, as the chain keeps them: a TRC is 100000000"
                        + " of them, and your program never rounds a coin away.")
                .table("Wallet and Chain members", "Member", "What it does")
                .row(GuideCell.code("Wallet.Balance(address)"), GuideCell.words("What the address holds."))
                .row(GuideCell.code("Wallet.Address(path)"), GuideCell.words("The address of the wallet in that file,"
                        + " or empty text when the file holds none."))
                .row(GuideCell.code("Wallet.Send(path, to, amount, label)"), GuideCell.words("Sends the amount from"
                        + " the wallet in that file to the address, with the label, and gives yes; no when the wallet"
                        + " does not hold that much."))
                .row(GuideCell.code("Chain.Height"), GuideCell.words("How many blocks the chain has."))
                .row(GuideCell.code("Chain.Difficulty"), GuideCell.words("How hard the next block is to find."))
                .row(GuideCell.code("Chain.Reward"), GuideCell.words("What finding the next block pays."))
                .table("Miner members", "Member", "What it does")
                .row(GuideCell.code("Miner.Installed"), GuideCell.words("Yes when the miner is installed."))
                .row(GuideCell.code("Miner.Status"), GuideCell.words("What it is doing: off, alone, pool, no_address,"
                        + " no_pool, waiting_for_owner or nothing_hashes."))
                .row(GuideCell.code("Miner.Hashrate"), GuideCell.words("How many hashes a second it makes."))
                .row(GuideCell.code("Miner.Pool, Miner.Address"), GuideCell.words("The pool it mines for, empty when it"
                        + " mines alone, and the address it pays."))
                .row(GuideCell.code("Miner.Background"), GuideCell.words("Yes while it runs in the background."))
                .row(GuideCell.code("Miner.SetAddress(address), Miner.UsePool(name), Miner.MineAlone()"),
                        GuideCell.words("Sets the address it pays, the pool it mines for (empty for the network's"
                                + " first), or mining alone."))
                .row(GuideCell.code("Miner.Start(), Miner.Stop()"), GuideCell.words("Runs it in the background, and"
                        + " stops it. Start gives no when it has no address to pay."))
                .table("Pool members", "Member", "What it does")
                .row(GuideCell.code("Pool.Names()"), GuideCell.words("The names of the Mining Pools on the network."))
                .row(GuideCell.code("Pool.Of(name)"), GuideCell.words("The pool of that name, the network's first for"
                        + " empty text, or null when there is none."))
                .row(GuideCell.code("Pool.Name, Pool.Host, Pool.FeePermille, Pool.Hashrate, Pool.Members, Pool.Found"),
                        GuideCell.words("Its name, the server it runs on, its fee in thousandths, how many hashes a"
                                + " second its miners make, how many miners it has, and how many blocks it found."))
                .paragraph("A wallet is its file: whoever reads the file can spend from it, as in the Wallet program."
                        + " A file that holds no wallet, an address written wrong, an amount of nothing or a label of"
                        + " more than one line stops your program with an ArgumentException. What you send leaves the"
                        + " wallet at once and reaches the other with the next block.")
                .paragraph("Miner reads and sets your computer's miner as the miner command does, and what you set is"
                        + " kept with the computer.")
                .listing("Rent.sgs",
                        "string wallet = \"Wallets/main.wallet\";",
                        "if (Wallet.Balance(Wallet.Address(wallet)) > 500000000) {",
                        "    Wallet.Send(wallet, \"trc1qexampleexampleexampleexample\", 100000000, \"Rent\");",
                        "}")
                .define("Hundred-millionth", "The smallest amount of Teracoin: a TRC split into 100000000 parts, the"
                        + " way the chain counts every amount you hold.")
                .ifSomethingGoesWrong(
                        "Miner.Start gives no.",
                        "Your miner has no address to pay. Call Miner.SetAddress first.",
                        "Wallet.Send gives no.",
                        "The wallet does not hold that much. Read Wallet.Balance first.")
                .seeAlso("jsc:teracoin", "jsc:mining", "jsc:mining_pools")
                .register();
    }
}
