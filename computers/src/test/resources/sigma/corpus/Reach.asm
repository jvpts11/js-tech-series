.asm 3
.arch jsc:x86
.start Corpus.ReachCorpus console

.class Corpus.ReachCorpus

.method static void Main() slots 6
    ldstr   "door"
    ldnull
    ldfn    Corpus.ReachCorpus.Heard(ChannelMessage) -> void
    call    Channel.Listen(string, Action<ChannelMessage>) -> void
    ldstr   "door"
    ldstr   "open"
    call    Channel.Send(string, object) -> void
    ldstr   "door"
    call    Channel.Close(string) -> void
    ldstr   "info"
    ldstr   "The corpus ran"
    call    EventLog.Write(string, string) -> void
    ldsfld  Navigation.Aboard
    brfalse L2
    ldsfld  Navigation.Piloted
    ldc.i4  0
    ceq
    br      L3
L2: ldc.i4  0
L3: brfalse L1
    ldstr   ""
    ldsfld  Navigation.X
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.Z
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.Altitude
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.Pressure
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.Speed
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.Climb
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.Pitch
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.Roll
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.Heading
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.AboveGround
    call    string.Concat(string, double) -> string
    ldsfld  Navigation.ClearAhead
    call    string.Concat(string, double) -> string
    call    Console.PrintLine(string) -> void
    newobj  List<double>()
    stloc   0
    ldloc   0
    ldc.i4  0
    conv.r8
    call    List.Add(double) -> void
    ldloc   0
    ldc.i4  90
    conv.r8
    call    List.Add(double) -> void
    ldloc   0
    ldc.i4  0
    conv.r8
    call    List.Add(double) -> void
    ldloc   0
    ldc.i4  4
    conv.r8
    ldc.i4  0
    call    Navigation.FollowRoute(List<double>, double, bool) -> bool
    pop
    ldc.i4  10
    conv.r8
    ldc.i4  90
    conv.r8
    ldc.i4  10
    conv.r8
    ldc.i4  4
    conv.r8
    call    Navigation.GoTo(double, double, double, double) -> bool
    pop
    ldc.i4  100
    conv.r8
    ldc.i4  2
    conv.r8
    call    Navigation.HoldAltitude(double, double) -> bool
    pop
    ldc.i4  20
    conv.r8
    ldc.i4  64
    conv.r8
    ldc.i4  20
    conv.r8
    ldc.i4  1
    conv.r8
    call    Navigation.Dock(double, double, double, double) -> bool
    pop
    call    Navigation.Release() -> bool
    pop
L1: ldstr   "Wallets/main.wallet"
    call    Wallet.Address(string) -> string
    stloc   1
    ldloc   1
    ldstr   ""
    ceq
    ldc.i4  0
    ceq
    brfalse L5
    ldloc   1
    call    Wallet.Balance(string) -> long
    ldc.i4  100000000
    conv.i8
    cgt
    br      L6
L5: ldc.i4  0
L6: brfalse L4
    ldstr   "Wallets/main.wallet"
    ldloc   1
    ldc.i4  100000000
    conv.i8
    ldstr   "Back to myself"
    call    Wallet.Send(string, string, long, string) -> bool
    pop
L4: ldstr   ""
    ldsfld  Chain.Height
    call    string.Concat(string, int) -> string
    ldsfld  Chain.Difficulty
    call    string.Concat(string, long) -> string
    ldsfld  Chain.Reward
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ldsfld  Miner.Installed
    brfalse L8
    ldsfld  Miner.Status
    ldsfld  Miner.Hashrate
    call    string.Concat(string, long) -> string
    ldsfld  Miner.Pool
    call    string.Concat(string, string) -> string
    ldsfld  Miner.Address
    call    string.Concat(string, string) -> string
    ldsfld  Miner.Background
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldloc   1
    call    Miner.SetAddress(string) -> void
    ldstr   ""
    call    Miner.UsePool(string) -> void
    call    Miner.MineAlone() -> void
    call    Miner.Start() -> bool
    brfalse L8
    call    Miner.Stop() -> void
L8: call    Pool.Names() -> List<string>
    stloc   2
    ldc.i4  0
    stloc   3
L9: ldloc   3
    ldloc   2
    ldfld   List.Count
    bge     L11
    ldloc   2
    ldloc   3
    call    List.Get(int) -> string
    stloc   4
    ldloc   4
    call    Pool.Of(string) -> Pool
    stloc   5
    ldloc   5
    ldfld   Pool.Name
    ldloc   5
    ldfld   Pool.Host
    call    string.Concat(string, string) -> string
    ldloc   5
    ldfld   Pool.FeePermille
    call    string.Concat(string, int) -> string
    ldloc   5
    ldfld   Pool.Hashrate
    call    string.Concat(string, long) -> string
    ldloc   5
    ldfld   Pool.Members
    call    string.Concat(string, int) -> string
    ldloc   5
    ldfld   Pool.Found
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
L10: ldloc   3
    ldc.i4  1
    add
    stloc   3
    br      L9
L11: ret

.method static void Heard(ChannelMessage) slots 1
    ldloc   0
    ldfld   ChannelMessage.Channel
    ldloc   0
    ldfld   ChannelMessage.From
    call    string.Concat(string, string) -> string
    ldloc   0
    ldfld   ChannelMessage.Value
    call    string.Concat(string, object) -> string
    ldloc   0
    ldfld   ChannelMessage.Tick
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ret
