/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.machine.MachinePrograms;
import dev.jstech.computers.gateway.GatewayLog;
import dev.jstech.computers.gateway.GatewayPermissions;
import dev.jstech.computers.gateway.GatewayService;
import dev.jstech.computers.gateway.GatewayStats;
import dev.jstech.computers.integration.computercraft.GatewayPeripheral;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The ComputerCraft side of a Gateway, driven the way a Lua program drives it: a computer attaches to the
 * peripheral and reads the network, pulls into the buffer and pushes back, is told when its operations
 * settle and when a watched total moves, is refused what the permissions deny, pays the host for every
 * call, and starts a program on another of our computers.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GatewayBridgeGameTests {

    private GatewayBridgeGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos GATEWAY = new BlockPos(6, 2, 2);
    private static final int CC_ID = 3;
    private static final String COBBLESTONE = "minecraft:cobblestone";

    /** A ComputerCraft computer as the peripheral sees it, keeping every event queued on it. */
    private static final class FakeComputer implements IComputerAccess {

        private final int id;
        private final List<List<Object>> events = new ArrayList<>();

        private FakeComputer(final int id) {
            this.id = id;
        }

        @Override
        public String mount(final String desiredLocation, final Mount mount, final String driveName) {
            return null;
        }

        @Override
        public String mountWritable(final String desiredLocation, final WritableMount mount, final String driveName) {
            return null;
        }

        @Override
        public void unmount(final String location) {
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public void queueEvent(final String event, final Object... arguments) {
            final List<Object> row = new ArrayList<>();
            row.add(event);
            if (arguments != null) {
                row.addAll(Arrays.asList(arguments));
            }
            events.add(row);
        }

        @Override
        public String getAttachmentName() {
            return "right";
        }

        @Override
        public Map<String, IPeripheral> getAvailablePeripherals() {
            return Map.of();
        }

        @Override
        public IPeripheral getAvailablePeripheral(final String name) {
            return null;
        }

        @Override
        public WorkMonitor getMainThreadMonitor() {
            return new WorkMonitor() {
                @Override
                public boolean canWork() {
                    return true;
                }

                @Override
                public boolean shouldWork() {
                    return true;
                }

                @Override
                public void trackWork(final long time, final TimeUnit unit) {
                }
            };
        }

        /** Whether an event by that name arrived with {@code first} as its first argument. */
        boolean heard(final String event, final Object first) {
            return events.stream().anyMatch(row -> row.get(0).equals(event) && row.size() > 1 && first.equals(row.get(1)));
        }

        /** The arguments of the first event by that name with {@code first} first, or an empty list. */
        List<Object> argumentsOf(final String event, final Object first) {
            return events.stream().filter(row -> row.get(0).equals(event) && row.size() > 1 && first.equals(row.get(1)))
                    .map(row -> row.subList(1, row.size())).findFirst().orElse(List.of());
        }

        int count(final String event) {
            return (int) events.stream().filter(row -> row.get(0).equals(event)).count();
        }
    }

    /** The Mainframe with stock, the host computer "desk" with the Gateway on its back, and "lab" beside it. */
    private record Fleet(MainframeBlockEntity mainframe, PersonalComputerBlockEntity host, PersonalComputerBlockEntity lab) {

        NetworkStorage storage(final GameTestHelper helper) {
            return NetworkStorage.of(helper.getLevel(), mainframe.networkUuid());
        }
    }

    private static Fleet wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity host = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        host.console().setComputerName("desk");
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 3));
        lab.console().setComputerName("lab");
        helper.setBlock(GATEWAY, ComputingModule.NETWORK_GATEWAY.get().defaultBlockState()
                .setValue(NetworkGatewayBlock.FACING, Direction.EAST));
        return new Fleet(mainframe, host, lab);
    }

    private static NetworkGatewayBlockEntity gateway(final GameTestHelper helper) {
        if (helper.getBlockEntity(GATEWAY) instanceof NetworkGatewayBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("no Gateway at " + GATEWAY);
    }

    /** The peripheral on the Gateway's front face, with {@code computer} attached to it. */
    private static GatewayPeripheral attach(final GameTestHelper helper, final FakeComputer computer) {
        final IPeripheral found = helper.getLevel().getCapability(PeripheralCapability.get(),
                helper.absolutePos(GATEWAY), Direction.EAST);
        if (!(found instanceof GatewayPeripheral peripheral)) {
            throw new IllegalStateException("the Gateway's front is not a jsc_gateway peripheral: " + found);
        }
        peripheral.attach(computer);
        return peripheral;
    }

    private static String refusal(final Runnable call) {
        try {
            call.run();
            return "";
        } catch (final RuntimeException wrapped) {
            return wrapped.getMessage() == null ? "" : wrapped.getMessage();
        }
    }

    /** Runs a peripheral call, turning its Lua error into a runtime one the assertion can read. */
    private interface LuaCall {
        void run() throws LuaException;
    }

    private static Runnable lua(final LuaCall call) {
        return () -> {
            try {
                call.run();
            } catch (final LuaException error) {
                throw new IllegalStateException(error.getMessage(), error);
            }
        };
    }

    private static long bufferCount(final NetworkGatewayBlockEntity gateway, final StorageKey key) {
        long count = 0L;
        for (int i = 0; i < gateway.buffer().getSlots(); i++) {
            final ItemStack held = gateway.buffer().getStackInSlot(i);
            if (!held.isEmpty() && StorageKey.of(held).equals(key)) {
                count += held.getCount();
            }
        }
        return count;
    }

    @GameTest(template = ARENA)
    public static void bridge_readsTheNetworkForAComputerCraftComputer(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final FakeComputer cc = new FakeComputer(CC_ID);
        final GatewayPeripheral[] peripheral = new GatewayPeripheral[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    fleet.storage(helper).insert(StorageKey.of(Items.COBBLESTONE), 200);
                    peripheral[0] = attach(helper, cc);
                    lua(() -> {
                        final Map<String, Long> types = peripheral[0].types(cc);
                        helper.assertTrue(Long.valueOf(200L).equals(types.get(COBBLESTONE)),
                                "types names what the network holds by registry id; got " + types);
                        helper.assertTrue(peripheral[0].total(cc, "cobblestone") == 200L, "total takes the vanilla namespace as read");
                        final List<Map<String, Object>> where = peripheral[0].find(cc, COBBLESTONE);
                        helper.assertTrue(!where.isEmpty() && where.get(0).get("quantity").equals(200L),
                                "find names the server that holds it; got " + where);
                        helper.assertTrue(!peripheral[0].servers(cc).isEmpty(), "servers lists the rack's server");
                        helper.assertTrue(peripheral[0].capacity(cc) > 0L && peripheral[0].used(cc) > 0L,
                                "capacity and used read the network's use");
                    }).run();
                })
                .thenExecuteAfter(2, () -> lua(() -> {
                    final List<Map<String, Object>> computers = peripheral[0].computers(cc);
                    helper.assertTrue(computers.stream().anyMatch(row -> "lab".equals(row.get("name"))),
                            "computers lists the other machines of the network; got " + computers);
                    final String unknown = refusal(lua(() -> peripheral[0].total(cc, "minecraft:no_such_thing")));
                    helper.assertTrue(unknown.contains("unknown item"), "an unknown name is refused; got " + unknown);
                    helper.assertTrue(fleet.host().cannon().owed() > 0, "the host is charged for the calls");
                    helper.assertTrue(gateway(helper).stats().lastMinute(GatewayStats.Kind.CALL, helper.getLevel().getGameTime()) >= 7,
                            "the calls are counted");
                    helper.assertTrue(gateway(helper).attachedComputers().stream().anyMatch(c -> c.id() == CC_ID),
                            "the computer shows as attached");
                }).run())
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void bridge_pullsIntoTheBufferAndPushesBack(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final FakeComputer cc = new FakeComputer(CC_ID);
        final GatewayPeripheral[] peripheral = new GatewayPeripheral[1];
        final String[] ids = new String[2];
        final StorageKey cobble = StorageKey.of(Items.COBBLESTONE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> fleet.storage(helper).insert(cobble, 200))
                // A pull is planned from the Mainframe's index, which learns of the stock a moment after it lands.
                .thenWaitUntil(() -> helper.assertTrue(fleet.mainframe().networkIndex().available(cobble) >= 200L,
                        "waiting for the index to see the stock"))
                .thenExecute(() -> {
                    peripheral[0] = attach(helper, cc);
                    lua(() -> ids[0] = peripheral[0].pull(cc, COBBLESTONE, 32, Optional.empty())).run();
                    helper.assertTrue(ids[0] != null && ids[0].length() > 8, "pull hands back the operation's id; got " + ids[0]);
                    lua(() -> {
                        final Map<String, Object> op = peripheral[0].operation(cc, ids[0]);
                        helper.assertTrue(op != null && "pull".equals(op.get("type")) && op.get("requested").equals(32L),
                                "the operation can be looked up while it runs; got " + op);
                    }).run();
                })
                .thenWaitUntil(() -> helper.assertTrue(bufferCount(gateway(helper), cobble) == 32L
                        && cc.heard(GatewayService.EVENT_OPERATION, ids[0]), "waiting for the pull to land in the buffer"))
                .thenExecute(() -> {
                    helper.assertTrue("done".equals(cc.argumentsOf(GatewayService.EVENT_OPERATION, ids[0]).get(1)),
                            "the computer hears how the pull went; got " + cc.argumentsOf(GatewayService.EVENT_OPERATION, ids[0]));
                    helper.assertTrue(fleet.storage(helper).count(cobble) == 168L, "the network gave the 32; holds "
                            + fleet.storage(helper).count(cobble));
                    lua(() -> {
                        final Map<String, Object> settled = peripheral[0].operation(cc, ids[0]);
                        helper.assertTrue(settled != null && "done".equals(settled.get("status")) && settled.get("moved").equals(32L),
                                "a settled operation still answers; got " + settled);
                        ids[1] = peripheral[0].push(cc, COBBLESTONE, 32, Optional.of("high"));
                    }).run();
                    helper.assertTrue(bufferCount(gateway(helper), cobble) == 0L, "push takes the stack out of the buffer at once");
                })
                .thenWaitUntil(() -> helper.assertTrue(fleet.storage(helper).count(cobble) == 200L
                        && cc.heard(GatewayService.EVENT_OPERATION, ids[1]), "waiting for the push to settle"))
                .thenExecute(() -> {
                    final List<String> whats = gateway(helper).log().entries().stream().map(GatewayLog.Entry::what).toList();
                    helper.assertTrue(whats.contains("pull 32 " + COBBLESTONE) && whats.contains("push 32 " + COBBLESTONE),
                            "the Gateway's log has both requests signed by the computer; got " + whats);
                    helper.assertTrue(gateway(helper).log().entries().stream().anyMatch(e -> e.who().equals("CC #" + CC_ID)),
                            "signed CC #3");
                    final String empty = refusal(lua(() -> peripheral[0].push(cc, COBBLESTONE, 1, Optional.empty())));
                    helper.assertTrue(empty.contains("holds no"), "pushing from an empty buffer is refused; got " + empty);
                    final String uncraftable = refusal(lua(() -> peripheral[0].craft(cc, "minecraft:diamond", 1, Optional.empty())));
                    helper.assertTrue(uncraftable.contains("no pattern crafts"), "a craft nothing makes is refused; got " + uncraftable);
                })
                .thenSucceed();
    }

    /*
     * The other door: not a ComputerCraft program reading our network, but one of OUR programs, moved
     * onto one of their computers, asking the same machine the same things by the same names.
     */
    @GameTest(template = ARENA)
    public static void bridge_answersWhatOneOfOurProgramsAsksItsMachineFromOverThere(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final FakeComputer cc = new FakeComputer(CC_ID);
        final GatewayPeripheral[] peripheral = new GatewayPeripheral[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    fleet.storage(helper).insert(StorageKey.of(Items.COBBLESTONE), 200);
                    peripheral[0] = attach(helper, cc);
                })
                .thenExecuteAfter(2, () -> lua(() -> {
                    final Object total = peripheral[0].ask(cc, new ObjectArguments("Network", "Total", COBBLESTONE));
                    helper.assertTrue(Double.valueOf(200.0).equals(total),
                            "Network.Total answers the program what it would answer at home; got " + total);
                    final Object online = peripheral[0].ask(cc, new ObjectArguments("Network", "Online"));
                    helper.assertTrue(Boolean.TRUE.equals(online), "a property is a call that takes nothing");
                    final Object servers = peripheral[0].ask(cc, new ObjectArguments("Network", "Servers"));
                    helper.assertTrue(servers instanceof Map<?, ?> table && "List".equals(table.get("type")),
                            "a list comes back in the shape the program reads one; got " + servers);
                    helper.assertTrue(fleet.host().cannon().owed() > 0, "the host pays for the program's questions");
                }).run())
                .thenExecuteAfter(2, () -> {
                    final String nothing = refusal(lua(() ->
                            peripheral[0].ask(cc, new ObjectArguments("Reactor", "Heat"))));
                    helper.assertTrue(nothing.contains("not something this computer answers"),
                            "a thing no machine of ours has is said so; got " + nothing);
                    gateway(helper).setPermissions(GatewayPermissions.DEFAULT.withRead(false), "desk", "set read off");
                    final String refused = refusal(lua(() ->
                            peripheral[0].ask(cc, new ObjectArguments("Network", "Total", COBBLESTONE))));
                    helper.assertTrue(refused.contains("reading the network is off"),
                            "the Gateway's own rules hold for our programs too; got " + refused);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void bridge_refusesWhatThePermissionsDeny(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final FakeComputer cc = new FakeComputer(CC_ID);
        final GatewayPeripheral[] peripheral = new GatewayPeripheral[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    fleet.storage(helper).insert(StorageKey.of(Items.COBBLESTONE), 200);
                    peripheral[0] = attach(helper, cc);
                    final NetworkGatewayBlockEntity g = gateway(helper);
                    g.setPermissions(GatewayPermissions.DEFAULT.withRead(false), "desk", "set read off");
                    final String read = refusal(lua(() -> peripheral[0].types(cc)));
                    helper.assertTrue(read.contains("reading the network is off"), "reads are refused with reads off; got " + read);
                    g.setPermissions(GatewayPermissions.DEFAULT.withOperations(false), "desk", "set operations off");
                    final String ops = refusal(lua(() -> peripheral[0].pull(cc, COBBLESTONE, 1, Optional.empty())));
                    helper.assertTrue(ops.contains("operations are off"), "operations are refused with operations off; got " + ops);
                    helper.assertTrue(refusal(lua(() -> peripheral[0].total(cc, COBBLESTONE))).isEmpty(),
                            "reads still answer with operations off");
                    helper.assertTrue(g.log().entries().stream().filter(e -> e.tone() == GatewayLog.Tone.DENIED).count() >= 2,
                            "each refusal is in the log");
                })
                .thenExecuteAfter(2, () -> {
                    final NetworkGatewayBlockEntity g = gateway(helper);
                    g.setPermissions(GatewayPermissions.DEFAULT.withCallCap(4), "desk", "set cap 4");
                    int answered = 0;
                    for (int i = 0; i < 4; i++) {
                        if (refusal(lua(() -> peripheral[0].capacity(cc))).isEmpty()) {
                            answered++;
                        }
                    }
                    helper.assertTrue(answered == 4, "the first four calls of a tick are answered; got " + answered);
                    final String busy = refusal(lua(() -> peripheral[0].capacity(cc)));
                    helper.assertTrue(busy.contains("busy"), "the fifth is refused as busy; got " + busy);
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(refusal(lua(() -> peripheral[0].capacity(cc))).isEmpty(),
                        "the next tick answers again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void bridge_watchesATotalAndTellsTheComputerWhenItMoves(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final FakeComputer cc = new FakeComputer(CC_ID);
        final GatewayPeripheral[] peripheral = new GatewayPeripheral[1];
        final StorageKey cobble = StorageKey.of(Items.COBBLESTONE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    fleet.storage(helper).insert(cobble, 200);
                    peripheral[0] = attach(helper, cc);
                    lua(() -> helper.assertTrue(peripheral[0].watch(cc, "cobblestone") == 200L, "watch answers the total now")).run();
                    helper.assertTrue(gateway(helper).watchesOf(CC_ID).containsKey(COBBLESTONE), "the watch is kept by its full name");
                    fleet.storage(helper).insert(cobble, 50);
                })
                .thenWaitUntil(() -> helper.assertTrue(cc.heard(GatewayService.EVENT_STOCK, COBBLESTONE),
                        "waiting for the stock event"))
                .thenExecute(() -> {
                    final List<Object> said = cc.argumentsOf(GatewayService.EVENT_STOCK, COBBLESTONE);
                    helper.assertTrue(said.size() == 3 && said.get(1).equals(250L) && said.get(2).equals(200L),
                            "the event carries the total and what it was; got " + said);
                    lua(() -> helper.assertTrue(peripheral[0].unwatch(cc, COBBLESTONE), "unwatch says it was watched")).run();
                    fleet.storage(helper).insert(cobble, 50);
                })
                .thenExecuteAfter(45, () -> helper.assertTrue(cc.count(GatewayService.EVENT_STOCK) == 1,
                        "nothing more is heard after unwatch; got " + cc.count(GatewayService.EVENT_STOCK)))
                .thenSucceed();
    }

    private static final String TOOL = """
            using System.*;
            using System.IO.*;
            using System.Execution.*;
            namespace Programs;
            class Tool {
                static void Main() {
                    Console.PrintLine("tool " + Program.Args.Get(0));
                }
            }
            """;

    private static String listing(final String source) {
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(new SourceFile("Program.can", source)));
        if (!built.ok()) {
            throw new IllegalStateException(String.join("\n", built.lines()));
        }
        return built.assembly();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void bridge_startsAProgramOnAnotherComputer(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final FakeComputer cc = new FakeComputer(CC_ID);
        final int[] started = new int[1];
        final ILanguageProcess[] tool = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    final ServerCliComputer lab = new ServerCliComputer(fleet.lab(), helper.getLevel());
                    helper.assertTrue(lab.writeFile("C:\\tool.asm", listing(TOOL)).ok(), "the tool is on lab");
                    final GatewayPeripheral peripheral = attach(helper, cc);
                    lua(() -> started[0] = peripheral.run(cc, new ObjectArguments("lab", "C:\\tool.asm", "x"))).run();
                    helper.assertTrue(started[0] > 0, "run hands back the process id; got " + started[0]);
                    // The process itself is kept: a returned program with nobody waiting on it leaves the machine's list.
                    final MachinePrograms.Live live = fleet.lab().cannon().byId(started[0]);
                    helper.assertTrue(live != null, "lab lists the program it was asked to run");
                    tool[0] = live.process();
                    helper.assertTrue(lab.setConfig("remote", "off").ok(), "lab says no from now on");
                    final String refused = refusal(lua(() -> peripheral.run(cc, new ObjectArguments("lab", "C:\\tool.asm"))));
                    helper.assertTrue(refused.contains("does not take programs"), "a computer that says no refuses; got " + refused);
                    final String nowhere = refusal(lua(() -> peripheral.run(cc, new ObjectArguments("nowhere", "C:\\tool.asm"))));
                    helper.assertTrue(nowhere.contains("no such computer"), "an unknown computer refuses; got " + nowhere);
                })
                .thenExecuteAfter(20, () -> helper.assertTrue(tool[0].console().contains("tool x"),
                        "the tool ran on lab with its argument; got " + tool[0].console() + " (" + tool[0].message() + ")"))
                .thenSucceed();
    }
}
