/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.block.DataLinkNames;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.operation.payload.NetworkEventsPayload;
import dev.jstech.computers.operation.payload.wan.BorderLinkStatePayload;
import dev.jstech.computers.operation.payload.wan.WanPayloads;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.wan.GatewayLinks;
import dev.jstech.computers.wan.PortSettings;
import dev.jstech.computers.wan.WanMode;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;

/**
 * Nouvell BorderLink at the prompt of a WAN Gateway Computer (computers/docs/design/network.md): the gateway's ports
 * as BorderLink's Ports heading shows them, one port's line and switches, the switches set, and the gateway's own log.
 * It is how a gateway is run on a system with no desktop, and the words its MC-NET space suggests.
 */
@TextHolder
public final class BorderLinkCommand implements ICliCommand {

    /** The word it is typed as, which is also what its complaints open with. */
    private static final String NAME = "borderlink";
    /* BorderLink's program id, by its text: the shell decides where the verb exists with no game loaded. */
    private static final String PACKAGE = "jsc:borderlink";
    private static final int PORTS = 4;

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.borderlink.summary",
            "run this gateway's long-distance lines: its ports, what each far side may do here, and its log");
    private static final TextKey USAGE = TextKey.of("jsc.cli.borderlink.usage",
            "[ports] | port <1-4> | port <1-4> see|take|send|craft|relay|on on|off | log");
    private static final TextKey EXAMPLE_PORTS = TextKey.of("jsc.cli.borderlink.example_ports",
            "list the four ports and how their lines stand");
    private static final TextKey EXAMPLE_RELAY = TextKey.of("jsc.cli.borderlink.example_relay",
            "let port 2 carry other networks' Operations through");
    private static final TextKey EXAMPLE_TAKE = TextKey.of("jsc.cli.borderlink.example_take",
            "let the network at port 1's far end take items from this one");
    private static final TextKey NOT_A_GATEWAY = TextKey.of("jsc.cli.borderlink.not_a_gateway",
            "this machine is not a WAN Gateway Computer");
    private static final TextKey NO_SUCH_PORT = TextKey.of("jsc.cli.borderlink.no_such_port",
            "no port %s: the ports are 1 to 4");
    private static final TextKey NO_CARD = TextKey.of("jsc.cli.borderlink.no_card", "no card");
    private static final TextKey PORT_NO_CARD = TextKey.of("jsc.cli.borderlink.port_no_card",
            "port %s has no card: put a WAN Interface Card in it");
    private static final TextKey SET = TextKey.of("jsc.cli.borderlink.set", "port %s: %s %s");
    private static final TextKey COL_PORT = TextKey.of("jsc.cli.borderlink.col.port", "#");
    private static final TextKey COL_LINE = TextKey.of("jsc.cli.borderlink.col.line", "LINE");
    private static final TextKey COL_FAR = TextKey.of("jsc.cli.borderlink.col.far", "FAR END");
    private static final TextKey COL_MODE = TextKey.of("jsc.cli.borderlink.col.mode", "MODE");
    private static final TextKey COL_STATE = TextKey.of("jsc.cli.borderlink.col.state", "STATE");
    private static final TextKey UP = TextKey.of("jsc.cli.borderlink.up", "UP");
    private static final TextKey DOWN = TextKey.of("jsc.cli.borderlink.down", "DOWN");
    private static final TextKey LINES_UP = TextKey.of("jsc.cli.borderlink.lines_up", "%s of %s lines up");
    private static final TextKey PORT = TextKey.of("jsc.cli.borderlink.port", "port %s · %s");
    private static final TextKey NOTHING_THERE = TextKey.of("jsc.cli.borderlink.nothing_there",
            "nothing at the far end");
    private static final TextKey LINE = TextKey.of("jsc.cli.borderlink.line", "line: %s, %s it/t");
    private static final TextKey STATE = TextKey.of("jsc.cli.borderlink.state", "state: %s");
    private static final TextKey STATE_MODE = TextKey.of("jsc.cli.borderlink.state_mode", "state: %s, %s");
    private static final TextKey FAR_NETWORK = TextKey.of("jsc.cli.borderlink.far_network", "far network: %s");
    private static final TextKey MAY = TextKey.of("jsc.cli.borderlink.may",
            "the far side may: see %s, take %s, send %s, craft %s");
    private static final TextKey SWITCHES = TextKey.of("jsc.cli.borderlink.switches", "relay %s, port %s");
    private static final TextKey ON = TextKey.of("jsc.cli.borderlink.on", "on");
    private static final TextKey OFF = TextKey.of("jsc.cli.borderlink.off", "off");
    private static final TextKey NOTHING_LOGGED = TextKey.of("jsc.cli.borderlink.nothing_logged",
            "nothing logged yet: a line coming up or going down is");

    /* Where each column of the ports listing begins. */
    private static final int LINE_AT = 4;
    private static final int FAR_AT = 18;
    private static final int MODE_AT = 36;
    private static final int STATE_AT = 48;

    /** On a WAN Gateway Computer that has BorderLink, on every system it runs on. */
    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().onHost(HostScope.WAN_GATEWAY_COMPUTER).fromPackage(PACKAGE);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.NETWORK;
    }

    @Override
    public Text summary() {
        return SUMMARY.text();
    }

    @Override
    public Text usage() {
        return USAGE.text();
    }

    @Override
    public List<Example> examples() {
        return List.of(new Example("borderlink", EXAMPLE_PORTS.text()),
                new Example("borderlink port 2 relay on", EXAMPLE_RELAY.text()),
                new Example("borderlink port 1 take on", EXAMPLE_TAKE.text()));
    }

    @Override
    public void run(final CliContext ctx) {
        if (!(ctx.computer().hostBlock() instanceof WanGatewayBlockEntity gateway)
                || !(gateway.getLevel() instanceof ServerLevel level)) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, NOT_A_GATEWAY));
            return;
        }
        final String verb = ctx.arg(0).toLowerCase(Locale.ROOT);
        switch (verb) {
            case "", "ports" -> ports(ctx, WanPayloads.state(gateway, level));
            case "log" -> log(ctx, WanPayloads.state(gateway, level));
            case "port" -> port(ctx, gateway, level);
            default -> ctx.out().error(CliTexts.USAGE.with(NAME, usage()));
        }
    }

    private static void ports(final CliContext ctx, final BorderLinkStatePayload state) {
        ctx.out().line(CliLine.of(CliSpan.of(COL_PORT, CliStyle.HEADER), CliSpan.pad(LINE_AT),
                CliSpan.of(COL_LINE, CliStyle.HEADER), CliSpan.pad(FAR_AT), CliSpan.of(COL_FAR, CliStyle.HEADER),
                CliSpan.pad(MODE_AT), CliSpan.of(COL_MODE, CliStyle.HEADER), CliSpan.pad(STATE_AT),
                CliSpan.of(COL_STATE, CliStyle.HEADER)));
        for (int i = 0; i < state.ports().size(); i++) {
            final BorderLinkStatePayload.Port port = state.ports().get(i);
            if (!port.hasCard()) {
                ctx.out().line(CliLine.of(CliSpan.plain(String.valueOf(i + 1)), CliSpan.pad(LINE_AT),
                        CliSpan.of(NO_CARD, CliStyle.DIM)));
                continue;
            }
            ctx.out().line(CliLine.of(CliSpan.plain(String.valueOf(i + 1)), CliSpan.pad(LINE_AT),
                    CliSpan.plain(lineName(port)), CliSpan.pad(FAR_AT), CliSpan.plain(port.far()),
                    CliSpan.pad(MODE_AT), CliSpan.plain(modeName(port)), CliSpan.pad(STATE_AT),
                    CliSpan.of(port.up() ? UP : DOWN, port.up() ? CliStyle.OK : CliStyle.ERROR)));
        }
        ctx.out().dim(LINES_UP.with(state.linesUp(), state.cards()));
    }

    /* One port as its panel shows it, or, with a switch and a word, that switch set. */
    private static void port(final CliContext ctx, final WanGatewayBlockEntity gateway, final ServerLevel level) {
        final int number = number(ctx.arg(1));
        if (number < 1) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, NO_SUCH_PORT.with(ctx.arg(1))));
            return;
        }
        final BorderLinkStatePayload.Port port = WanPayloads.state(gateway, level).ports().get(number - 1);
        if (!port.hasCard()) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, PORT_NO_CARD.with(number)));
            return;
        }
        final String which = ctx.arg(2).toLowerCase(Locale.ROOT);
        if (which.isEmpty()) {
            describe(ctx, number, port);
            return;
        }
        final String word = ctx.arg(3).toLowerCase(Locale.ROOT);
        if (!word.equals("on") && !word.equals("off")) {
            ctx.out().error(CliTexts.USAGE.with(NAME, USAGE));
            return;
        }
        final PortSettings now = gateway.portSettings(number);
        final boolean on = word.equals("on");
        final PortSettings set = switch (which) {
            case "see" -> now.with(WanRight.SEE, on);
            case "take" -> now.with(WanRight.TAKE, on);
            case "send" -> now.with(WanRight.SEND, on);
            case "craft" -> now.with(WanRight.CRAFT, on);
            case "relay" -> now.withRelay(on);
            case "on" -> now.withOn(on);
            default -> null;
        };
        if (set == null) {
            ctx.out().error(CliTexts.USAGE.with(NAME, USAGE));
            return;
        }
        gateway.setPortSettings(number, set);
        // The switch and the word are written the way they are typed, so they are the command's own words.
        ctx.out().ok(SET.with(number, which, word));
    }

    private static void describe(final CliContext ctx, final int number, final BorderLinkStatePayload.Port port) {
        ctx.out().header(PORT.with(number, port.far().isEmpty() ? NOTHING_THERE.text() : Text.literal(port.far())));
        indented(ctx, LINE.with(lineName(port), port.speed()));
        indented(ctx, port.wanMode() == WanMode.NONE ? STATE.with(port.up() ? UP : DOWN)
                : STATE_MODE.with(port.up() ? UP : DOWN, modeName(port)));
        if (port.up() && port.wanMode() == WanMode.FEDERATION) {
            indented(ctx, FAR_NETWORK.with(port.network()));
        }
        final PortSettings settings = port.settings();
        indented(ctx, MAY.with(onOff(settings.see()), onOff(settings.take()), onOff(settings.send()),
                onOff(settings.craft())));
        indented(ctx, SWITCHES.with(onOff(settings.relay()), onOff(settings.on())));
    }

    private static void log(final CliContext ctx, final BorderLinkStatePayload state) {
        if (state.log().isEmpty()) {
            ctx.out().dim(NOTHING_LOGGED);
            return;
        }
        for (final NetworkEventsPayload.Row row : state.log()) {
            final EventLevel what = EventLevel.byId(row.level());
            ctx.out().line(CliLine.build().plain(Stamps.of(row.dayTime()) + "  ").add(what.label(),
                    what == EventLevel.INFO ? CliStyle.INFO : what == EventLevel.WARN ? CliStyle.WARN
                            : CliStyle.ERROR).plain("  ").plain(row.message()).done());
        }
    }

    private static void indented(final CliContext ctx, final Text words) {
        ctx.out().line(CliLine.build().plain("  ").plain(words).done());
    }

    private static Text onOff(final boolean on) {
        return (on ? ON : OFF).text();
    }

    /* The line in a port by the name the mod gives every data link; its own name when it names none. */
    private static Text lineName(final BorderLinkStatePayload.Port port) {
        final DataLink link = port.link();
        return link == null ? Text.literal(port.line()) : DataLinkNames.of(link).text();
    }

    private static Text modeName(final BorderLinkStatePayload.Port port) {
        return port.wanMode() == WanMode.NONE ? Text.EMPTY : GatewayLinks.modeName(port.wanMode()).text();
    }

    /* The port a word names, 1 to 4; 0 when it names none. */
    private static int number(final String word) {
        try {
            final int number = Integer.parseInt(word);
            return number >= 1 && number <= PORTS ? number : 0;
        } catch (final NumberFormatException notANumber) {
            return 0;
        }
    }
}
