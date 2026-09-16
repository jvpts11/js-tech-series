.asm 3
.arch jsc:x86
.start Corpus.ProgramsCorpus console

.class Corpus.ProgramsCorpus

.method static void Main() slots 19
    ldstr   "corpus"
    call    Program.SetName(string) -> void
    ldsfld  Program.Name
    stloc   0
    ldsfld  Program.Args
    stloc   1
    ldsfld  Program.DroppedEvents
    stloc   2
    ldsfld  Program.Current
    stloc   3
    ldstr   "C:\\tool.asm"
    call    Program.Start(string) -> Process
    stloc   4
    ldstr   "C:\\tool.asm"
    ldloc   1
    call    Program.Start(string, List<string>) -> Process
    stloc   5
    ldstr   "C:\\tool.asm"
    ldloc   1
    ldstr   "lab"
    call    Program.Start(string, List<string>, string) -> Process
    stloc   6
    ldstr   "dir"
    call    Program.Shell(string) -> List<string>
    stloc   7
    ldthis
    ldfn    Corpus.ProgramsCorpus.0lambda1(ProcessMessage) -> void
    call    Program.OnMessage(Action<ProcessMessage>) -> void
    ldloc   0
    ldloc   2
    call    string.Concat(string, long) -> string
    ldloc   3
    ldfld   Process.Id
    call    string.Concat(string, int) -> string
    ldloc   7
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldloc   4
    ldfld   Process.Id
    stloc   8
    ldloc   4
    ldfld   Process.Name
    stloc   9
    ldloc   4
    ldfld   Process.Host
    stloc   10
    ldloc   4
    ldfld   Process.Running
    brfalse L1
    ldloc   4
    call    Process.Wait() -> void
L1: ldloc   5
    ldc.i4  20
    conv.i8
    call    Process.Wait(long) -> bool
    stloc   11
    ldloc   5
    ldfld   Process.ExitCode
    stloc   12
    ldloc   6
    call    Process.Kill() -> void
    ldloc   5
    call    Process.Output() -> List<string>
    stloc   13
    ldloc   8
    ldstr   "hello"
    call    Process.Send(int, string) -> bool
    stloc   14
    ldloc   9
    ldloc   10
    call    string.Concat(string, string) -> string
    ldloc   11
    call    string.Concat(string, bool) -> string
    ldloc   12
    call    string.Concat(string, int) -> string
    ldloc   13
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    ldloc   14
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldthis
    ldfn    Corpus.ProgramsCorpus.0lambda2() -> void
    call    Thread.Start(Action) -> Thread
    stloc   15
    ldsfld  Thread.Current
    stloc   16
    ldloc   15
    ldfld   Thread.Id
    stloc   17
    ldloc   15
    ldfld   Thread.Running
    brfalse L2
    ldloc   15
    call    Thread.Join() -> void
L2: ldloc   15
    ldc.i4  10
    conv.i8
    call    Thread.Join(long) -> bool
    stloc   18
    ldloc   15
    call    Thread.Stop() -> void
    ldstr   ""
    ldloc   16
    ldfld   Thread.Id
    call    string.Concat(string, int) -> string
    ldloc   17
    call    string.Concat(string, int) -> string
    ldloc   18
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldc.i4  0
    call    Program.Exit(int) -> void
    ret

.method void 0lambda1(ProcessMessage) slots 1
    ldloc   0
    ldfld   ProcessMessage.Text
    ldloc   0
    ldfld   ProcessMessage.From
    call    string.Concat(string, int) -> string
    ldloc   0
    ldfld   ProcessMessage.Tick
    call    string.Concat(string, long) -> string
    call    Console.PrintLine(string) -> void
    ret

.method void 0lambda2() slots 0
    ldc.i4  5
    conv.i8
    call    Thread.Sleep(long) -> void
    call    Thread.Yield() -> void
    ret
