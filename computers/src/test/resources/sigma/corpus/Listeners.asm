.asm 3
.arch jsc:x86
.start Corpus.ListenersCorpus console

.class Corpus.ListenersCorpus

.method static void Main() slots 1
    ldnull
    ldfn    Corpus.ListenersCorpus.Settled(OperationInfo) -> void
    call    Operations.OnSettled(Action<OperationInfo>) -> void
    ldstr   "minecraft:iron_block"
    ldc.i4  2
    conv.i8
    call    Operations.Craft(string, long) -> AskResult
    stloc   0
    ldloc   0
    ldfld   AskResult.Ok
    brfalse L2
    ldloc   0
    ldfld   AskResult.Id
    ldstr   ""
    ceq
    ldc.i4  0
    ceq
    br      L3
L2: ldc.i4  0
L3: brfalse L1
    ldloc   0
    ldfld   AskResult.Id
    ldnull
    ldfn    Corpus.ListenersCorpus.Settled(OperationInfo) -> void
    call    Operations.OnSettled(string, Action<OperationInfo>) -> void
L1: ldnull
    ldfn    Corpus.ListenersCorpus.Logged(NetworkEvent) -> void
    call    Network.OnEvent(Action<NetworkEvent>) -> void
    ret

.method static void Settled(OperationInfo) slots 1
    ldloc   0
    ldfld   OperationInfo.Id
    ldloc   0
    ldfld   OperationInfo.Status
    call    string.Concat(string, string) -> string
    ldloc   0
    ldfld   OperationInfo.Moved
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ret

.method static void Logged(NetworkEvent) slots 1
    ldloc   0
    ldfld   NetworkEvent.Level
    ldloc   0
    ldfld   NetworkEvent.Machine
    call    string.Concat(string, string) -> string
    ldloc   0
    ldfld   NetworkEvent.Text
    call    string.Concat(string, string) -> string
    ldloc   0
    ldfld   NetworkEvent.Tick
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ret
