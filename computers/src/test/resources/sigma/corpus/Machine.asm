.asm 2
.start Corpus.MachineCorpus console

.class Corpus.MachineCorpus

.method static void Main() slots 20
    ldstr   "a.txt"
    call    File.Exists(string) -> bool
    stloc   0
    ldstr   "a.txt"
    call    File.Read(string) -> string
    stloc   1
    ldstr   "a.txt"
    call    File.TryRead(string, out string) -> bool
    stloc   2
    brfalse L1
    ldloc   2
    call    Console.PrintLine(string) -> void
L1: ldstr   "a.txt"
    ldstr   "x"
    call    File.Write(string, string) -> bool
    brfalse L2
    ldstr   "a.txt"
    ldstr   "y"
    call    File.Append(string, string) -> bool
    br      L3
L2: ldc.i4  0
L3: stloc   3
    ldstr   "a.txt"
    call    File.Delete(string) -> bool
    brfalse L4
    ldstr   "d"
    call    File.MkDir(string) -> bool
    br      L5
L4: ldc.i4  0
L5: stloc   4
    ldstr   "C:\\"
    call    File.List(string) -> List<string>
    stloc   5
    ldc.i4  0
    stloc   6
L6: ldloc   6
    ldloc   5
    ldfld   List.Count
    bge     L8
    ldloc   5
    ldloc   6
    call    List.Get(int) -> string
    stloc   7
    ldloc   7
    call    Console.PrintLine(string) -> void
L7: ldloc   6
    ldc.i4  1
    add
    stloc   6
    br      L6
L8: ldstr   ""
    ldloc   0
    call    string.Concat(string, bool) -> string
    ldloc   1
    call    string.Concat(string, string) -> string
    ldloc   3
    call    string.Concat(string, bool) -> string
    ldloc   4
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldsfld  Computer.Name
    stloc   8
    ldsfld  Computer.Cpu
    stloc   9
    ldsfld  Computer.Os
    stloc   10
    ldsfld  Computer.RamMb
    ldsfld  Computer.FreeRamMb
    add
    stloc   11
    ldsfld  Computer.Online
    stloc   12
    ldloc   8
    ldloc   9
    ldfld   CpuInfo.Mhz
    call    string.Concat(string, int) -> string
    ldloc   9
    ldfld   CpuInfo.Cores
    call    string.Concat(string, int) -> string
    ldloc   9
    ldfld   CpuInfo.Era
    call    string.Concat(string, string) -> string
    ldloc   10
    ldfld   OsInfo.Id
    call    string.Concat(string, string) -> string
    ldloc   10
    ldfld   OsInfo.Name
    call    string.Concat(string, string) -> string
    ldloc   11
    call    string.Concat(string, int) -> string
    ldloc   12
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    call    Computer.Disks() -> List<DiskInfo>
    stloc   13
    ldc.i4  0
    stloc   14
L9: ldloc   14
    ldloc   13
    ldfld   List.Count
    bge     L11
    ldloc   13
    ldloc   14
    call    List.Get(int) -> DiskInfo
    stloc   15
    ldloc   15
    ldfld   DiskInfo.Mount
    ldloc   15
    ldfld   DiskInfo.UsedMb
    call    string.Concat(string, long) -> string
    ldloc   15
    ldfld   DiskInfo.CapacityMb
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
L10: ldloc   14
    ldc.i4  1
    add
    stloc   14
    br      L9
L11: call    Computer.Programs() -> List<string>
    stloc   16
    call    Computer.Processes() -> List<ProcessInfo>
    stloc   17
    ldc.i4  0
    stloc   18
L12: ldloc   18
    ldloc   17
    ldfld   List.Count
    bge     L14
    ldloc   17
    ldloc   18
    call    List.Get(int) -> ProcessInfo
    stloc   19
    ldstr   ""
    ldloc   19
    ldfld   ProcessInfo.Id
    call    string.Concat(string, int) -> string
    ldloc   19
    ldfld   ProcessInfo.Name
    call    string.Concat(string, string) -> string
    ldloc   19
    ldfld   ProcessInfo.State
    call    string.Concat(string, string) -> string
    ldloc   19
    ldfld   ProcessInfo.HeldBytes
    call    string.Concat(string, long) -> string
    ldloc   16
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
L13: ldloc   18
    ldc.i4  1
    add
    stloc   18
    br      L12
L14: ret
