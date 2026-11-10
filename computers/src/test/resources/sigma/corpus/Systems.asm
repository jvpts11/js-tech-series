.asm 3
.arch jsc:x86
.start Corpus.SystemsCorpus console

.class Corpus.SystemsCorpus

.method static void Main() slots 20
    ldsfld  Era.Legacy
    stloc   0
    ldloc   0
    ldfld   Era.Name
    ldsfld  Era.Vintage
    ldfld   Era.Name
    call    string.Concat(string, string) -> string
    ldsfld  Era.Transition
    ldfld   Era.Name
    call    string.Concat(string, string) -> string
    ldsfld  Era.Standard
    ldfld   Era.Name
    call    string.Concat(string, string) -> string
    ldsfld  Era.Advanced
    ldfld   Era.Name
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    ldsfld  Isa.IA32
    stloc   1
    ldloc   1
    ldfld   Isa.Name
    ldsfld  Isa.IA16
    ldfld   Isa.Name
    call    string.Concat(string, string) -> string
    ldsfld  Isa.X86_64
    ldfld   Isa.Name
    call    string.Concat(string, string) -> string
    ldsfld  Isa.M68K
    ldfld   Isa.Name
    call    string.Concat(string, string) -> string
    ldsfld  Isa.PowerPC
    ldfld   Isa.Name
    call    string.Concat(string, string) -> string
    ldsfld  Isa.ARM64
    ldfld   Isa.Name
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "linux"
    call    Kernel.Of(string) -> Kernel
    stloc   2
    call    Kernel.Own() -> Kernel
    ldstr   "cooperative"
    call    Kernel.Scheduler(string) -> Kernel
    ldstr   "preemptive"
    ldc.i4  64
    call    Kernel.Scheduler(string, int) -> Kernel
    ldstr   "posix"
    call    Kernel.FileSystem(string) -> Kernel
    ldstr   "text, graphics, sound"
    call    Kernel.Drivers(string) -> Kernel
    ldstr   "end"
    call    Kernel.MemoryRule(string) -> Kernel
    ldstr   "own"
    call    Kernel.Shell(string) -> Kernel
    stloc   3
    ldloc   2
    ldfld   Kernel.Name
    ldloc   3
    ldfld   Kernel.Name
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    call    Screen.Clear() -> void
    ldc.i4  0
    ldc.i4  0
    call    Screen.Cursor(int, int) -> void
    ldstr   "white"
    ldstr   "#241733"
    call    Screen.Colours(string, string) -> void
    ldstr   "Lantern "
    call    Screen.Write(string) -> void
    ldstr   "is up: "
    ldsfld  Screen.Columns
    call    string.Concat(string, int) -> string
    ldstr   " by "
    call    string.Concat(string, string) -> string
    ldsfld  Screen.Rows
    call    string.Concat(string, int) -> string
    call    Screen.WriteLine(string) -> void
    ldstr   "lantern/notes.asm"
    call    Processes.Start(string) -> Process
    stloc   4
    ldstr   "lantern/files.asm"
    newobj  List<string>()
    call    Processes.Start(string, List<string>) -> Process
    stloc   5
    ldsfld  Processes.Current
    call    Input.Focus(Process) -> void
    ldc.i4  2
    ldstr   "high"
    call    Processes.SetPriority(int, string) -> void
    ldc.i4  3
    ldstr   "term"
    call    Processes.Stop(int, string) -> void
    call    Processes.Yield() -> void
    call    Processes.List() -> List<ProcessInfo>
    stloc   6
    ldc.i4  0
    stloc   7
L1: ldloc   7
    ldloc   6
    ldfld   List.Count
    bge     L3
    ldloc   6
    ldloc   7
    call    List.Get(int) -> ProcessInfo
    stloc   8
    ldloc   8
    ldfld   ProcessInfo.Name
    call    Console.PrintLine(string) -> void
L2: ldloc   7
    ldc.i4  1
    add
    stloc   7
    br      L1
L3: ldstr   ""
    ldsfld  Disk.FreeMb
    call    string.Concat(string, long) -> string
    ldstr   " of "
    call    string.Concat(string, string) -> string
    ldsfld  Disk.SizeMb
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    call    Disk.Mounted() -> List<string>
    stloc   9
    ldc.i4  0
    stloc   10
L4: ldloc   10
    ldloc   9
    ldfld   List.Count
    bge     L6
    ldloc   9
    ldloc   10
    call    List.Get(int) -> string
    stloc   11
    ldloc   11
    call    Console.PrintLine(string) -> void
L5: ldloc   10
    ldc.i4  1
    add
    stloc   10
    br      L4
L6: ldstr   ""
    ldsfld  Memory.TotalMb
    call    string.Concat(string, int) -> string
    ldsfld  Memory.FreeMb
    call    string.Concat(string, int) -> string
    ldc.i4  1
    call    Memory.HeldBy(int) -> int
    call    string.Concat(string, int) -> string
    ldsfld  Memory.Refuse
    call    string.Concat(string, int) -> string
    ldsfld  Memory.GoDown
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    call    Memory.Ledger() -> List<string>
    stloc   12
    ldc.i4  0
    stloc   13
L7: ldloc   13
    ldloc   12
    ldfld   List.Count
    bge     L9
    ldloc   12
    ldloc   13
    call    List.Get(int) -> string
    stloc   14
    ldloc   14
    call    Console.PrintLine(string) -> void
L8: ldloc   13
    ldc.i4  1
    add
    stloc   13
    br      L7
L9: ldstr   "watchman"
    ldstr   "lantern/watchman.asm"
    call    Services.Register(string, string) -> void
    ldstr   "watchman"
    call    Services.Start(string) -> bool
    stloc   15
    ldstr   "watchman"
    call    Services.Stop(string) -> bool
    stloc   16
    ldstr   "watchman"
    call    Services.Status(string) -> string
    ldloc   15
    call    string.Concat(string, bool) -> string
    ldloc   16
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    call    Drivers.Devices() -> List<Device>
    stloc   17
    ldc.i4  0
    stloc   18
L10: ldloc   18
    ldloc   17
    ldfld   List.Count
    bge     L12
    ldloc   17
    ldloc   18
    call    List.Get(int) -> Device
    stloc   19
    ldloc   19
    ldfld   Device.Name
    ldloc   19
    ldfld   Device.Class
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
L11: ldloc   18
    ldc.i4  1
    add
    stloc   18
    br      L10
L12: ldstr   ""
    ldstr   "sound"
    call    Drivers.Has(string) -> bool
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldstr   "/lantern/logo.pix"
    call    Boot.Logo(string) -> void
    ldstr   "Lighting the lantern..."
    call    Boot.Line(string) -> void
    ldc.i4  60
    call    Boot.Progress(int) -> void
    call    Boot.Done() -> void
    ldstr   "#241733"
    ldstr   "#FFD27A"
    call    Panic.Colours(string, string) -> void
    ldc.i4  20
    call    Panic.RebootAfter(int) -> void
    ldstr   "own"
    call    Panic.Page(string) -> void
    ldstr   "lantern-desktop"
    call    Desktop.Start(string) -> void
    ldsfld  Desktop.Current
    call    Console.PrintLine(string) -> void
    call    Desktop.Stop() -> void
    ldstr   "The lantern went out."
    call    Panic.Halt(string) -> void
    ret

.method static void Heard(InputEvent) slots 1
    ldloc   0
    ldfld   InputEvent.Kind
    ldloc   0
    ldfld   InputEvent.Key
    call    string.Concat(string, int) -> string
    ldloc   0
    ldfld   InputEvent.Char
    call    string.Concat(string, string) -> string
    ldloc   0
    ldfld   InputEvent.X
    call    string.Concat(string, int) -> string
    ldloc   0
    ldfld   InputEvent.Y
    call    string.Concat(string, int) -> string
    ldloc   0
    ldfld   InputEvent.Button
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ret
