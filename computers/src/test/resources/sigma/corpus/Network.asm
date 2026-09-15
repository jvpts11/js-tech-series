.asm 2
.start Corpus.NetworkCorpus console

.class Corpus.NetworkCorpus

.method static void Main() slots 38
    ldsfld  Network.Online
    stloc   0
    ldsfld  Network.Current
    stloc   1
    ldsfld  Network.Capacity
    ldsfld  Network.Used
    add
    ldstr   "minecraft:iron_ingot"
    call    Network.Total(string) -> long
    add
    stloc   2
    call    Network.Types() -> List<string>
    stloc   3
    ldstr   ""
    ldloc   0
    call    string.Concat(string, bool) -> string
    ldloc   1
    call    string.Concat(string, string) -> string
    ldloc   2
    call    string.Concat(string, long) -> string
    ldloc   3
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "minecraft:iron_ingot"
    call    Network.Find(string) -> List<HoldingInfo>
    stloc   4
    ldc.i4  0
    stloc   5
L1: ldloc   5
    ldloc   4
    ldfld   List.Count
    bge     L3
    ldloc   4
    ldloc   5
    call    List.Get(int) -> HoldingInfo
    stloc   6
    ldloc   6
    ldfld   HoldingInfo.Server
    ldloc   6
    ldfld   HoldingInfo.Quantity
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
L2: ldloc   5
    ldc.i4  1
    add
    stloc   5
    br      L1
L3: call    Network.Servers() -> List<ServerInfo>
    stloc   7
    ldc.i4  0
    stloc   8
L4: ldloc   8
    ldloc   7
    ldfld   List.Count
    bge     L6
    ldloc   7
    ldloc   8
    call    List.Get(int) -> ServerInfo
    stloc   9
    ldloc   9
    ldfld   ServerInfo.Name
    ldloc   9
    ldfld   ServerInfo.Stored
    call    string.Concat(string, long) -> string
    ldloc   9
    ldfld   ServerInfo.Capacity
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
L5: ldloc   8
    ldc.i4  1
    add
    stloc   8
    br      L4
L6: ldstr   "minecraft:iron_ingot"
    ldnull
    ldfn    Corpus.NetworkCorpus.Told(StockEvent) -> void
    call    Network.Watch(string, Action<StockEvent>) -> Subscription
    stloc   10
    ldstr   "minecraft:iron_ingot"
    ldc.i4  10
    conv.i8
    ldnull
    ldfn    Corpus.NetworkCorpus.Told(StockEvent) -> void
    call    Network.WatchBelow(string, long, Action<StockEvent>) -> Subscription
    stloc   11
    ldstr   "minecraft:iron_ingot"
    ldc.i4  100
    conv.i8
    ldnull
    ldfn    Corpus.NetworkCorpus.Told(StockEvent) -> void
    call    Network.WatchAbove(string, long, Action<StockEvent>) -> Subscription
    stloc   12
    ldstr   ""
    ldloc   10
    ldfld   Subscription.Id
    call    string.Concat(string, int) -> string
    ldloc   10
    ldfld   Subscription.Item
    call    string.Concat(string, string) -> string
    ldloc   11
    ldfld   Subscription.Id
    call    string.Concat(string, int) -> string
    ldloc   12
    ldfld   Subscription.Id
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "desk"
    call    Network.Computer(string) -> RemoteComputer
    stloc   13
    call    Network.Computers() -> List<RemoteComputer>
    stloc   14
    ldloc   13
    ldfld   RemoteComputer.Host
    ldloc   13
    ldfld   RemoteComputer.Name
    call    string.Concat(string, string) -> string
    ldloc   13
    ldfld   RemoteComputer.Type
    call    string.Concat(string, string) -> string
    ldloc   13
    ldfld   RemoteComputer.Os
    call    string.Concat(string, string) -> string
    ldloc   13
    ldfld   RemoteComputer.Online
    call    string.Concat(string, bool) -> string
    ldloc   14
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    newobj  List<string>()
    stloc   15
    ldloc   13
    ldstr   "C:\\tool.asm"
    call    RemoteComputer.Start(string) -> Process
    stloc   16
    ldloc   13
    ldstr   "C:\\tool.asm"
    ldloc   15
    call    RemoteComputer.Start(string, List<string>) -> Process
    stloc   17
    ldloc   13
    ldstr   "C:\\tool.asm"
    ldloc   15
    ldstr   "lab"
    call    RemoteComputer.Start(string, List<string>, string) -> Process
    stloc   18
    ldloc   13
    ldstr   "dir"
    call    RemoteComputer.Shell(string) -> List<string>
    stloc   19
    ldloc   13
    ldloc   16
    ldfld   Process.Id
    ldstr   "hi"
    call    RemoteComputer.Send(int, string) -> bool
    stloc   20
    ldloc   13
    call    RemoteComputer.Processes() -> List<Process>
    stloc   21
    ldstr   ""
    ldloc   17
    ldfld   Process.Id
    call    string.Concat(string, int) -> string
    ldloc   18
    ldfld   Process.Id
    call    string.Concat(string, int) -> string
    ldloc   19
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    ldloc   20
    call    string.Concat(string, bool) -> string
    ldloc   21
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "QUERY items"
    call    Iql.Run(string) -> IqlResult
    stloc   22
    ldstr   "QUERY items"
    call    Iql.Query(string) -> List<Map<string, object>>
    stloc   23
    ldstr   "restock"
    call    Iql.Exec(string) -> IqlResult
    stloc   24
    ldstr   "restock"
    ldloc   15
    call    Iql.Exec(string, List<string>) -> IqlResult
    stloc   25
    ldstr   "C:\\job.iql"
    call    Iql.RunFile(string) -> IqlResult
    stloc   26
    ldstr   ""
    ldloc   22
    ldfld   IqlResult.Ok
    call    string.Concat(string, bool) -> string
    ldloc   22
    ldfld   IqlResult.Message
    call    string.Concat(string, string) -> string
    ldloc   22
    ldfld   IqlResult.Rows
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    ldloc   23
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldstr   ""
    ldloc   24
    ldfld   IqlResult.Ok
    call    string.Concat(string, bool) -> string
    ldloc   25
    ldfld   IqlResult.Ok
    call    string.Concat(string, bool) -> string
    ldloc   26
    ldfld   IqlResult.Ok
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldsfld  Mainframe.Online
    stloc   27
    ldsfld  Mainframe.PeakToday
    stloc   28
    ldstr   "PULL"
    call    Mainframe.Stats(string) -> WorkStat
    stloc   29
    call    Mainframe.Work() -> List<WorkStat>
    stloc   30
    ldstr   ""
    ldloc   27
    call    string.Concat(string, bool) -> string
    ldloc   28
    call    string.Concat(string, int) -> string
    ldloc   29
    ldfld   WorkStat.Type
    call    string.Concat(string, string) -> string
    ldloc   29
    ldfld   WorkStat.Count
    call    string.Concat(string, int) -> string
    ldloc   29
    ldfld   WorkStat.AverageWait
    call    string.Concat(string, int) -> string
    ldloc   29
    ldfld   WorkStat.AverageRun
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldstr   ""
    ldloc   29
    ldfld   WorkStat.ShortfallPercent
    call    string.Concat(string, int) -> string
    ldloc   29
    ldfld   WorkStat.Moved
    call    string.Concat(string, long) -> string
    ldloc   30
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "minecraft:oak_log"
    ldc.i4  64
    conv.i8
    call    Operations.Pull(string, long) -> AskResult
    stloc   31
    ldstr   "minecraft:oak_log"
    ldc.i4  64
    conv.i8
    call    Operations.Push(string, long) -> AskResult
    stloc   32
    ldstr   "minecraft:stick"
    ldc.i4  4
    conv.i8
    call    Operations.Craft(string, long) -> AskResult
    stloc   33
    ldstr   "op-1"
    call    Operations.Cancel(string) -> AskResult
    stloc   34
    ldstr   "op-1"
    ldstr   "HIGH"
    call    Operations.Reprioritise(string, string) -> AskResult
    stloc   35
    ldstr   "op-1"
    call    Operations.Get(string) -> OperationInfo
    stloc   36
    call    Operations.List() -> List<OperationInfo>
    stloc   37
    ldstr   ""
    ldloc   31
    ldfld   AskResult.Ok
    call    string.Concat(string, bool) -> string
    ldloc   31
    ldfld   AskResult.Message
    call    string.Concat(string, string) -> string
    ldloc   32
    ldfld   AskResult.Ok
    call    string.Concat(string, bool) -> string
    ldloc   33
    ldfld   AskResult.Ok
    call    string.Concat(string, bool) -> string
    ldloc   34
    ldfld   AskResult.Ok
    call    string.Concat(string, bool) -> string
    ldloc   35
    ldfld   AskResult.Ok
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldloc   36
    ldfld   OperationInfo.Id
    ldloc   36
    ldfld   OperationInfo.Type
    call    string.Concat(string, string) -> string
    ldloc   36
    ldfld   OperationInfo.Item
    call    string.Concat(string, string) -> string
    ldloc   36
    ldfld   OperationInfo.Moved
    call    string.Concat(string, long) -> string
    ldloc   36
    ldfld   OperationInfo.Requested
    call    string.Concat(string, long) -> string
    ldloc   36
    ldfld   OperationInfo.Status
    call    string.Concat(string, string) -> string
    ldloc   36
    ldfld   OperationInfo.Priority
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    ldstr   ""
    ldloc   37
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ret

.method static void Told(StockEvent) slots 1
    ldloc   0
    ldfld   StockEvent.Item
    ldloc   0
    ldfld   StockEvent.Total
    call    string.Concat(string, long) -> string
    ldloc   0
    ldfld   StockEvent.Previous
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ret
