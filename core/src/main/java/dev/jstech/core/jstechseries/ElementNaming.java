/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.jstechseries.command.SeriesCommands;
import dev.jstech.core.config.CoreConfigKeys;
import dev.jstech.core.material.IMaterialNamer;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialNames;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.NamingBalance;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.ServerState;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Naming what is discovered: on a server, the player who first discovers an element beyond the known table gives it
 * its name and its symbol, and the element and all its things take them, for everyone and in every language. The name
 * is kept with the world and sent to every player; it holds letters only, no more than the world allows, repeats no
 * other element's name, and ends in "-ium" where the world asks it to; the symbol has one or two letters and repeats
 * none. Once given, a name is not changed, but an operator can take one back, for an offensive name, and its
 * discoverer names it again. In a world where discoverers do not name elements, the series' own names are used.
 *
 * <p>What counts as discovering an element is the mod's that makes it: a mod calls {@link #discover} at its first
 * synthesis.
 */
@TextHolder
public final class ElementNaming {

    public static final TextKey DISCOVERED_YOU = TextKey.of("jscore.element.discovered",
            "You discovered element %s! Give it its name and symbol:");
    public static final TextKey DISCOVERED_SERIES = TextKey.of("jscore.element.discovered_series",
            "You discovered element %s: %s (%s)");
    public static final TextKey HOW = TextKey.of("jscore.element.how", "/jstech name_element %s <name> <symbol>");
    public static final TextKey NAMED = TextKey.of("jscore.element.named",
            "%s named element %s %s, with the symbol %s");
    public static final TextKey UNNAMED = TextKey.of("jscore.element.unnamed",
            "The name of element %s was taken back; its discoverer names it again");
    public static final TextKey NOT_BEYOND = TextKey.of("jscore.element.not_beyond",
            "There is no element %s beyond the known table");
    public static final TextKey NOT_DISCOVERED = TextKey.of("jscore.element.not_discovered",
            "Element %s has not been discovered yet");
    public static final TextKey NOT_DISCOVERER = TextKey.of("jscore.element.not_discoverer",
            "Only the player who discovered element %s names it");
    public static final TextKey ALREADY_NAMED = TextKey.of("jscore.element.already_named",
            "Element %s already has its name");
    public static final TextKey SERIES_NAMES = TextKey.of("jscore.element.series_names",
            "In this world elements keep the series' names");
    public static final TextKey LETTERS_ONLY = TextKey.of("jscore.element.letters_only",
            "A name holds letters only");
    public static final TextKey TOO_LONG = TextKey.of("jscore.element.too_long",
            "A name holds at most %s letters");
    public static final TextKey NOT_IUM = TextKey.of("jscore.element.not_ium", "In this world a name ends in -ium");
    public static final TextKey NAME_TAKEN = TextKey.of("jscore.element.name_taken",
            "%s is already an element's name");
    public static final TextKey BAD_SYMBOL = TextKey.of("jscore.element.bad_symbol",
            "A symbol is one or two letters");
    public static final TextKey SYMBOL_TAKEN = TextKey.of("jscore.element.symbol_taken",
            "%s is already an element's symbol");
    public static final TextKey NOT_NAMED = TextKey.of("jscore.element.not_named", "Element %s has no name to take back");

    /** Who discovered each element beyond the known table, and whether it keeps the series' name, by material id. */
    public static final ServerState<Map<ResourceLocation, Discovery>> DISCOVERED = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "element_discoveries"),
            Codec.unboundedMap(ResourceLocation.CODEC, Discovery.CODEC), Map.<ResourceLocation, Discovery>of())
            .synced(ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, Discovery.STREAM_CODEC))
            .server();

    /** What an element beyond the known table is called now: a given name, the series' name, or its systematic one. */
    public static final IMaterialNamer NAMER = new IMaterialNamer() {
        @Override
        public Optional<Text> name(final Material material) {
            final Optional<MaterialNames.Given> given = MaterialNames.given(material);
            if (given.isPresent()) {
                return Optional.of(Text.literal(given.get().name()));
            }
            if (keepsSeriesName(material)) {
                return Superheavies.number(material).flatMap(Superheavies::seriesName).map(TextKey::text);
            }
            return Optional.empty();
        }

        @Override
        public Optional<String> symbol(final Material material) {
            final Optional<MaterialNames.Given> given = MaterialNames.given(material);
            if (given.isPresent()) {
                return Optional.of(given.get().symbol());
            }
            if (keepsSeriesName(material)) {
                return Superheavies.number(material).flatMap(Superheavies::seriesSymbol);
            }
            return Optional.empty();
        }
    };

    private static final String NAME_COMMAND = "name_element";
    private static final String UNNAME_COMMAND = "unname_element";
    private static final String NUMBER = "number";
    private static final String NAME = "name";
    private static final String SYMBOL = "symbol";
    private static final String IUM = "ium";
    private static final int LONGEST_SYMBOL = 2;

    private ElementNaming() {
    }

    /** Keeps the discoveries with the world, declares the commands, and follows the world's setting, once. */
    public static void declare() {
        CoreStates.register(DISCOVERED);
        SeriesCommands.declare(NAME_COMMAND, SeriesCommands.EVERYONE, (node, context) -> node
                .then(Commands.argument(NUMBER, IntegerArgumentType.integer(Superheavies.FIRST, Superheavies.LAST))
                        .then(Commands.argument(NAME, StringArgumentType.word())
                                .then(Commands.argument(SYMBOL, StringArgumentType.word())
                                        .executes(ElementNaming::nameByCommand)))));
        SeriesCommands.declare(UNNAME_COMMAND, SeriesCommands.GAME_MASTERS, (node, context) -> node
                .then(Commands.argument(NUMBER, IntegerArgumentType.integer(Superheavies.FIRST, Superheavies.LAST))
                        .executes(ElementNaming::unnameByCommand)));
        // Turned off, the elements waiting for a name keep the series' instead; a name already given stays.
        CoreConfigKeys.FILE.whenChanged(CoreConfigKeys.ELEMENTS_DISCOVERERS_NAME, on -> {
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !on) {
                DISCOVERED.update(server, all -> {
                    final Map<ResourceLocation, Discovery> out = new HashMap<>();
                    all.forEach((id, found) -> out.put(id, new Discovery(found.by(), true)));
                    return Map.copyOf(out);
                });
            }
        });
    }

    /**
     * {@code player} discovered {@code element}: the first discovery is kept and its discoverer asked to name it, or,
     * where discoverers do not name elements, told its series' name. False when it was discovered before or is no
     * element beyond the known table.
     */
    public static boolean discover(final ServerPlayer player, final Material element) {
        final MinecraftServer server = player.getServer();
        final Optional<Integer> number = Superheavies.number(element);
        if (server == null || number.isEmpty() || DISCOVERED.get(server).containsKey(element.id())) {
            return false;
        }
        final boolean seriesName = !NamingBalance.discoverersName();
        DISCOVERED.update(server, all -> {
            final Map<ResourceLocation, Discovery> out = new HashMap<>(all);
            out.put(element.id(), new Discovery(player.getUUID(), seriesName));
            return Map.copyOf(out);
        });
        if (seriesName) {
            player.sendSystemMessage(GameText.component(DISCOVERED_SERIES.with(number.get(), element.name(),
                    symbolOf(element))));
        } else {
            final String suggestion = String.join(" ", "/" + SeriesCommands.ROOT, NAME_COMMAND,
                    String.valueOf(number.get()), "");
            player.sendSystemMessage(GameText.component(DISCOVERED_YOU.with(number.get())));
            player.sendSystemMessage(GameText.component(HOW.with(number.get())).withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestion))));
        }
        return true;
    }

    /** Whether {@code element} has been discovered in the world {@code server} runs. */
    public static boolean discovered(final MinecraftServer server, final Material element) {
        return DISCOVERED.get(server).containsKey(element.id());
    }

    /**
     * Names {@code element} {@code name}, with the symbol {@code symbol}, by {@code player}: the reason it may not, or
     * none when it is named, everyone told.
     */
    public static Optional<Text> name(final ServerPlayer player, final Material element, final String name,
                                      final String symbol) {
        final MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
        final Optional<Text> refusal = refusal(server, player.getUUID(), element, name, symbol);
        if (refusal.isPresent()) {
            return refusal;
        }
        final String written = symbol.substring(0, 1).toUpperCase(Locale.ROOT)
                + symbol.substring(1).toLowerCase(Locale.ROOT);
        final String number = Superheavies.number(element).map(String::valueOf).orElse(element.id().toString());
        MaterialNames.give(server, element, new MaterialNames.Given(name, written, player.getUUID()));
        server.getPlayerList().broadcastSystemMessage(GameText.component(NAMED.with(player.getGameProfile()
                .getName(), number, name, written)), false);
        return Optional.empty();
    }

    /** Takes back the name given to {@code element}, for an operator: false when it had none. */
    public static boolean unname(final MinecraftServer server, final Material element) {
        if (!MaterialNames.take(server, element)) {
            return false;
        }
        final String number = Superheavies.number(element).map(String::valueOf).orElse(element.id().toString());
        server.getPlayerList().broadcastSystemMessage(GameText.component(UNNAMED.with(number)), false);
        return true;
    }

    /**
     * Why {@code by} may not name {@code element} {@code name} with {@code symbol} in the world {@code server} runs,
     * or none when they may.
     */
    public static Optional<Text> refusal(final MinecraftServer server, final UUID by, final Material element,
                                         final String name, final String symbol) {
        final Optional<Integer> number = Superheavies.number(element);
        if (number.isEmpty()) {
            return Optional.of(NOT_BEYOND.with(element.id().toString()));
        }
        final Discovery found = DISCOVERED.get(server).get(element.id());
        if (found == null) {
            return Optional.of(NOT_DISCOVERED.with(number.get()));
        }
        if (MaterialNames.all(server).containsKey(element.id())) {
            return Optional.of(ALREADY_NAMED.with(number.get()));
        }
        if (found.seriesName() || !NamingBalance.discoverersName()) {
            return Optional.of(SERIES_NAMES.text());
        }
        if (!found.by().equals(by)) {
            return Optional.of(NOT_DISCOVERER.with(number.get()));
        }
        if (!letters(name)) {
            return Optional.of(LETTERS_ONLY.text());
        }
        if (name.length() > NamingBalance.longest()) {
            return Optional.of(TOO_LONG.with(NamingBalance.longest()));
        }
        if (NamingBalance.requireIum() && !name.toLowerCase(Locale.ROOT).endsWith(IUM)) {
            return Optional.of(NOT_IUM.text());
        }
        if (namesInUse(server, element).contains(name.toLowerCase(Locale.ROOT))) {
            return Optional.of(NAME_TAKEN.with(name));
        }
        if (symbol.isEmpty() || symbol.length() > LONGEST_SYMBOL || !letters(symbol)) {
            return Optional.of(BAD_SYMBOL.text());
        }
        if (symbolsInUse(server, element).contains(symbol.toLowerCase(Locale.ROOT))) {
            return Optional.of(SYMBOL_TAKEN.with(symbol));
        }
        return Optional.empty();
    }

    private static int nameByCommand(final CommandContext<CommandSourceStack> context) {
        final CommandSourceStack source = context.getSource();
        final ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        final Material element = Superheavies.of(IntegerArgumentType.getInteger(context, NUMBER));
        final Optional<Text> refusal = name(player, element, StringArgumentType.getString(context, NAME),
                StringArgumentType.getString(context, SYMBOL));
        if (refusal.isPresent()) {
            source.sendFailure(GameText.component(refusal.get()));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int unnameByCommand(final CommandContext<CommandSourceStack> context) {
        final int number = IntegerArgumentType.getInteger(context, NUMBER);
        if (!unname(context.getSource().getServer(), Superheavies.of(number))) {
            context.getSource().sendFailure(GameText.component(NOT_NAMED.with(number)));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    /* Whether the element keeps the series' name, on the side this is asked on. */
    private static boolean keepsSeriesName(final Material material) {
        final Map<ResourceLocation, Discovery> all;
        if (EffectiveSide.get().isClient()) {
            all = DISCOVERED.client();
        } else {
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return false;
            }
            all = DISCOVERED.get(server);
        }
        final Discovery found = all.get(material.id());
        return found != null && found.seriesName();
    }

    /* Every element's name in use, in lower case, but {@code naming}'s: the known table's and those beyond it. */
    private static Set<String> namesInUse(final MinecraftServer server, final Material naming) {
        final Set<String> out = new HashSet<>();
        for (final Element element : Element.values()) {
            out.add(element.nameKey().english().toLowerCase(Locale.ROOT));
        }
        for (final Material beyond : Superheavies.all().values()) {
            if (beyond != naming) {
                out.add(beyond.declaredName().english().toLowerCase(Locale.ROOT));
                out.add(beyond.name().english().toLowerCase(Locale.ROOT));
            }
            Superheavies.number(beyond).flatMap(Superheavies::seriesName)
                    .ifPresent(key -> out.add(key.english().toLowerCase(Locale.ROOT)));
        }
        MaterialNames.all(server).values().forEach(given -> out.add(given.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    /* Every element's symbol in use, in lower case, but {@code naming}'s. */
    private static Set<String> symbolsInUse(final MinecraftServer server, final Material naming) {
        final Set<String> out = new HashSet<>();
        for (final Element element : Element.values()) {
            out.add(element.symbol().toLowerCase(Locale.ROOT));
        }
        for (final Material beyond : Superheavies.all().values()) {
            Superheavies.number(beyond).flatMap(Superheavies::seriesSymbol)
                    .ifPresent(symbol -> out.add(symbol.toLowerCase(Locale.ROOT)));
        }
        MaterialNames.all(server).forEach((id, given) -> {
            if (!id.equals(naming.id())) {
                out.add(given.symbol().toLowerCase(Locale.ROOT));
            }
        });
        return out;
    }

    private static String symbolOf(final Material element) {
        return element.value(MaterialProperties.SYMBOL).orElse("");
    }

    private static boolean letters(final String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isLetter(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Who discovered an element, and whether it keeps the series' name, as it does when found in a world where
     * discoverers do not name elements.
     *
     * @param by         the discoverer
     * @param seriesName whether it keeps the series' name
     */
    public record Discovery(UUID by, boolean seriesName) {

        /** How it is kept with the world. */
        public static final Codec<Discovery> CODEC = RecordCodecBuilder.create(found -> found.group(
                UUIDUtil.CODEC.fieldOf("by").forGetter(Discovery::by),
                Codec.BOOL.fieldOf("series_name").forGetter(Discovery::seriesName)
        ).apply(found, Discovery::new));
        /** How it is sent. */
        public static final StreamCodec<ByteBuf, Discovery> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Discovery::by, ByteBufCodecs.BOOL, Discovery::seriesName, Discovery::new);

        public Discovery {
            Objects.requireNonNull(by, "by");
        }
    }
}
