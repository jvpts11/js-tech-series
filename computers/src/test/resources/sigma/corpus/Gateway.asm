.asm 3
.arch jsc:x86
.start Corpus.GatewayCorpus console

.class Corpus.GatewayCorpus

.method static void Main() slots 16
    ldsfld  Gateway.Online
    stloc   0
    ldsfld  Gateway.Current
    stloc   1
    call    Gateway.Names() -> List<string>
    stloc   2
    ldstr   "east"
    call    Gateway.Select(string) -> bool
    stloc   3
    ldstr   ""
    ldloc   0
    call    string.Concat(string, bool) -> string
    ldloc   1
    call    string.Concat(string, string) -> string
    ldloc   2
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    ldloc   3
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    call    Gateway.Computers() -> List<CcComputer>
    stloc   4
    ldc.i4  0
    stloc   5
L1: ldloc   5
    ldloc   4
    ldfld   List.Count
    bge     L3
    ldloc   4
    ldloc   5
    call    List.Get(int) -> CcComputer
    stloc   6
    ldstr   ""
    ldloc   6
    ldfld   CcComputer.Id
    call    string.Concat(string, long) -> string
    ldloc   6
    ldfld   CcComputer.Name
    call    string.Concat(string, string) -> string
    ldloc   6
    ldfld   CcComputer.Label
    call    string.Concat(string, string) -> string
    ldloc   6
    ldfld   CcComputer.Online
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
L2: ldloc   5
    ldc.i4  1
    add
    stloc   5
    br      L1
L3: call    Gateway.Peripherals() -> List<CcPeripheral>
    stloc   7
    ldc.i4  0
    stloc   8
L4: ldloc   8
    ldloc   7
    ldfld   List.Count
    bge     L6
    ldloc   7
    ldloc   8
    call    List.Get(int) -> CcPeripheral
    stloc   9
    ldloc   9
    ldfld   CcPeripheral.Name
    ldloc   9
    ldfld   CcPeripheral.Type
    call    string.Concat(string, string) -> string
    ldloc   9
    ldfld   CcPeripheral.Methods
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
L5: ldloc   8
    ldc.i4  1
    add
    stloc   8
    br      L4
L6: ldstr   "left"
    ldstr   "getName"
    call    Gateway.Call(string, string) -> object
    stloc   10
    ldstr   "left"
    ldstr   "setOutput"
    ldc.i4  1
    call    Gateway.Call(string, string, object) -> object
    stloc   11
    ldstr   "left"
    ldstr   "setOutput"
    ldstr   "top"
    ldc.i4  1
    call    Gateway.Call(string, string, object, object) -> object
    stloc   12
    ldstr   "left"
    ldstr   "setOutput"
    ldstr   "top"
    ldc.i4  1
    ldc.i4  1
    call    Gateway.Call(string, string, object, object, object) -> object
    stloc   13
    ldc.i4  1
    conv.i8
    call    Gateway.TurnOn(long) -> bool
    brfalse L9
    ldc.i4  1
    conv.i8
    call    Gateway.Shutdown(long) -> bool
    br      L10
L9: ldc.i4  0
L10: brfalse L7
    ldc.i4  1
    conv.i8
    call    Gateway.Reboot(long) -> bool
    br      L8
L7: ldc.i4  0
L8: stloc   14
    ldc.i4  1
    conv.i8
    ldstr   "hello"
    call    Gateway.Send(long, string) -> bool
    stloc   15
    ldnull
    ldfn    Corpus.GatewayCorpus.Heard(GatewayMessage) -> void
    call    Gateway.OnMessage(Action<GatewayMessage>) -> void
    ldstr   ""
    ldloc   10
    call    string.Concat(string, object) -> string
    ldloc   11
    call    string.Concat(string, object) -> string
    ldloc   12
    call    string.Concat(string, object) -> string
    ldloc   13
    call    string.Concat(string, object) -> string
    ldloc   14
    call    string.Concat(string, bool) -> string
    ldloc   15
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ret

.method static void Heard(GatewayMessage) slots 1
    ldstr   ""
    ldloc   0
    ldfld   GatewayMessage.From
    call    string.Concat(string, long) -> string
    ldloc   0
    ldfld   GatewayMessage.Text
    call    string.Concat(string, string) -> string
    ldloc   0
    ldfld   GatewayMessage.Tick
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ret
