.asm 3
.arch jsc:x86
.start Corpus.ReadingsCorpus console

.class Corpus.ReadingsCorpus

.method static void Main() slots 19
    ldstr   ""
    ldsfld  Power.DrawWatts
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldsfld  Power.Supply
    stloc   0
    ldloc   0
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L1
    ldloc   0
    ldfld   SupplyInfo.Name
    ldloc   0
    ldfld   SupplyInfo.RatingWatts
    call    string.Concat(string, int) -> string
    ldloc   0
    ldfld   SupplyInfo.Units
    call    string.Concat(string, int) -> string
    ldloc   0
    ldfld   SupplyInfo.UnitsFailed
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldloc   0
    ldfld   SupplyInfo.Degraded
    brfalse L2
    ldstr   "degraded"
    br      L5
L2: ldloc   0
    ldfld   SupplyInfo.Failed
    brfalse L4
    ldstr   "failed"
    br      L5
L4: ldstr   "good"
L5: call    Console.PrintLine(string) -> void
L1: ldsfld  Power.Ups
    stloc   1
    ldloc   1
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L6
    ldloc   1
    ldfld   UpsInfo.Name
    ldloc   1
    ldfld   UpsInfo.ChargePercent
    call    string.Concat(string, int) -> string
    ldloc   1
    ldfld   UpsInfo.MinutesLeft
    call    string.Concat(string, long) -> string
    ldloc   1
    ldfld   UpsInfo.OnBattery
    brtrue  L7
    ldloc   1
    ldfld   UpsInfo.Bypass
    br      L8
L7: ldc.i4  1
L8: call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
L6: ldsfld  Thermal.Known
    brfalse L10
    ldsfld  Thermal.Throttled
    ldc.i4  0
    ceq
    br      L11
L10: ldc.i4  0
L11: brfalse L9
    ldstr   ""
    ldsfld  Thermal.Celsius
    call    string.Concat(string, int) -> string
    ldsfld  Thermal.AmbientCelsius
    call    string.Concat(string, int) -> string
    ldsfld  Thermal.HeatWatts
    call    string.Concat(string, int) -> string
    ldsfld  Thermal.BudgetWatts
    call    string.Concat(string, int) -> string
    ldsfld  Thermal.ClockPercent
    call    string.Concat(string, int) -> string
    ldsfld  Thermal.LastTripCelsius
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
L9: ldsfld  Wireless.Joined
    brfalse L12
    ldsfld  Wireless.AccessPoint
    ldsfld  Wireless.Standard
    call    string.Concat(string, string) -> string
    ldsfld  Wireless.Speed
    call    string.Concat(string, int) -> string
    ldsfld  Wireless.Signal
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
L12: ldstr   "C"
    call    DiskHealth.Of(string) -> DiskHealth
    stloc   2
    ldloc   2
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L13
    ldloc   2
    ldfld   DiskHealth.Drive
    call    Console.PrintLine(string) -> void
L13: call    DiskHealth.All() -> List<DiskHealth>
    stloc   3
    ldc.i4  0
    stloc   4
L14: ldloc   4
    ldloc   3
    ldfld   List.Count
    bge     L16
    ldloc   3
    ldloc   4
    call    List.Get(int) -> DiskHealth
    stloc   5
    ldloc   5
    ldfld   DiskHealth.Drive
    ldloc   5
    ldfld   DiskHealth.DamagePercent
    call    string.Concat(string, int) -> string
    ldloc   5
    ldfld   DiskHealth.BadSectorsPercent
    call    string.Concat(string, int) -> string
    ldloc   5
    ldfld   DiskHealth.CorruptedItems
    call    string.Concat(string, long) -> string
    ldloc   5
    ldfld   DiskHealth.LastDefragTick
    call    string.Concat(string, long) -> string
    ldloc   5
    ldfld   DiskHealth.Dead
    brfalse L17
    ldstr   "dead"
    br      L18
L17: ldstr   ""
L18: call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
L15: ldloc   4
    ldc.i4  1
    add
    stloc   4
    br      L14
L16: ldstr   "runs"
    ldstr   "runs"
    ldstr   "0"
    call    Settings.Get(string, string) -> string
    call    Settings.Set(string, string) -> bool
    pop
    ldstr   "old"
    call    Settings.Remove(string) -> bool
    pop
    call    Settings.Names() -> List<string>
    stloc   6
    ldc.i4  0
    stloc   7
L19: ldloc   7
    ldloc   6
    ldfld   List.Count
    bge     L21
    ldloc   6
    ldloc   7
    call    List.Get(int) -> string
    stloc   8
    ldloc   8
    ldsfld  Settings.Path
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
L20: ldloc   7
    ldc.i4  1
    add
    stloc   7
    br      L19
L21: ldstr   "Boiler"
    call    Sensors.Read(string) -> List<SensorReading>
    stloc   9
    ldc.i4  0
    stloc   10
L22: ldloc   10
    ldloc   9
    ldfld   List.Count
    bge     L24
    ldloc   9
    ldloc   10
    call    List.Get(int) -> SensorReading
    stloc   11
    ldloc   11
    ldfld   SensorReading.Name
    ldloc   11
    ldfld   SensorReading.Value
    call    string.Concat(string, double) -> string
    ldloc   11
    ldfld   SensorReading.Least
    call    string.Concat(string, double) -> string
    ldloc   11
    ldfld   SensorReading.Most
    call    string.Concat(string, double) -> string
    ldloc   11
    ldfld   SensorReading.Unit
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
L23: ldloc   10
    ldc.i4  1
    add
    stloc   10
    br      L22
L24: call    Sensors.Names() -> List<string>
    stloc   12
    ldc.i4  0
    stloc   13
L25: ldloc   13
    ldloc   12
    ldfld   List.Count
    bge     L27
    ldloc   12
    ldloc   13
    call    List.Get(int) -> string
    stloc   14
    ldloc   14
    call    Console.PrintLine(string) -> void
L26: ldloc   13
    ldc.i4  1
    add
    stloc   13
    br      L25
L27: ldstr   "Gate"
    call    Signals.Radio(string) -> RadioInfo
    stloc   15
    ldloc   15
    ldnull
    ceq
    ldc.i4  0
    ceq
    brfalse L29
    ldstr   "Gate"
    ldc.i4  9
    call    Signals.Send(string, int) -> bool
    br      L30
L29: ldc.i4  0
L30: brfalse L28
    ldloc   15
    ldfld   RadioInfo.Role
    ldloc   15
    ldfld   RadioInfo.Band
    call    string.Concat(string, string) -> string
    ldloc   15
    ldfld   RadioInfo.Frequency
    call    string.Concat(string, int) -> string
    ldloc   15
    ldfld   RadioInfo.Value
    call    string.Concat(string, int) -> string
    ldloc   15
    ldfld   RadioInfo.Clash
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
L28: call    Signals.Heard() -> List<HeardSignal>
    stloc   16
    ldc.i4  0
    stloc   17
L31: ldloc   17
    ldloc   16
    ldfld   List.Count
    bge     L33
    ldloc   16
    ldloc   17
    call    List.Get(int) -> HeardSignal
    stloc   18
    ldloc   18
    ldfld   HeardSignal.Band
    ldloc   18
    ldfld   HeardSignal.Frequency
    call    string.Concat(string, int) -> string
    ldloc   18
    ldfld   HeardSignal.Bars
    call    string.Concat(string, int) -> string
    ldloc   18
    ldfld   HeardSignal.Value
    call    string.Concat(string, int) -> string
    ldloc   18
    ldfld   HeardSignal.Clash
    call    string.Concat(string, bool) -> string
    ldloc   18
    ldfld   HeardSignal.Relayed
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
L32: ldloc   17
    ldc.i4  1
    add
    stloc   17
    br      L31
L33: ret
