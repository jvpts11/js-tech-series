.asm 3
.arch jsc:x86
.start Corpus.BusCorpus console

.class Corpus.BusCorpus

.method static void Main() slots 3
    ldstr   "Ore in"
    call    Bus.Named(string) -> Bus
    stloc   0
    ldloc   0
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L1
    ldloc   0
    call    Bus.On() -> Bus
    call    Bus.Continuous() -> Bus
    ldstr   "iron_ore, coal"
    call    Bus.Only(string) -> Bus
    ldc.i4  16
    call    Bus.Keep(int) -> Bus
    ldc.i4  64
    call    Bus.Max(int) -> Bus
    ldc.i4  5
    call    Bus.Priority(int) -> Bus
    pop
    ldloc   0
    ldstr   "dirt"
    call    Bus.AllBut(string) -> Bus
    ldstr   "c:ores"
    call    Bus.Tag(string) -> Bus
    ldc.i4  1
    call    Bus.Fuzzy(bool) -> Bus
    ldstr   "iron_ore"
    ldc.i4  512
    call    Bus.WhenStock(string, int) -> Bus
    ldstr   "c:ores"
    ldc.i4  4096
    call    Bus.WhenStockTag(string, int) -> Bus
    pop
    ldloc   0
    ldc.i4  18
    ldc.i4  6
    call    Bus.Between(int, int) -> Bus
    ldstr   "Coal in"
    call    Bus.Named(string) -> Bus
    call    Bus.After(Bus) -> Bus
    call    Bus.OnDemand() -> Bus
    call    Bus.Off() -> Bus
    pop
    ldloc   0
    ldstr   "iron_ore"
    call    Bus.Item(string) -> BusItem
    ldc.i4  16
    call    BusItem.Keep(int) -> BusItem
    ldc.i4  64
    call    BusItem.Max(int) -> BusItem
    stloc   1
    ldloc   0
    ldfld   Bus.Name
    ldloc   1
    ldfld   BusItem.Bus
    call    string.Concat(string, string) -> string
    ldloc   1
    ldfld   BusItem.Item
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
L1: ldstr   "Chest wall"
    call    Bus.Named(string) -> Bus
    stloc   2
    ldloc   2
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L2
    ldloc   2
    call    Bus.ReadWrite() -> Bus
    call    Bus.ReadOnly() -> Bus
    call    Bus.WriteOnly() -> Bus
    pop
L2: ret
