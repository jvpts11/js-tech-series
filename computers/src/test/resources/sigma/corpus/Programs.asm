.asm 2
.start Corpus.ProgramsCorpus console

.class Corpus.ProgramsCorpus

.method static void Main() slots 20
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
    ldstr   "class A {}"
    ldloc   1
    ldstr   "a.sgs"
    call    Program.RunSource(string, List<string>, string) -> Process
    stloc   7
    ldstr   "dir"
    call    Program.Shell(string) -> List<string>
    stloc   8
    ldthis
    ldfn    Corpus.ProgramsCorpus.0lambda1(ProcessMessage) -> void
    call    Program.OnMessage(Action<ProcessMessage>) -> void
    ldloc   0
    ldloc   2
    call    string.Concat(string, long) -> string
    ldloc   3
    ldfld   Process.Id
    call    string.Concat(string, int) -> string
    ldloc   8
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
    ldloc   4
    ldfld   Process.Id
    stloc   9
    ldloc   4
    ldfld   Process.Name
    stloc   10
    ldloc   4
    ldfld   Process.Host
    stloc   11
    ldloc   4
    ldfld   Process.Running
    brfalse L1
    ldloc   4
    call    Process.Wait() -> void
L1: ldloc   5
    ldc.i4  20
    conv.i8
    call    Process.Wait(long) -> bool
    stloc   12
    ldloc   5
    ldfld   Process.ExitCode
    stloc   13
    ldloc   6
    call    Process.Kill() -> void
    ldloc   7
    call    Process.Output() -> List<string>
    stloc   14
    ldloc   9
    ldstr   "hello"
    call    Process.Send(int, string) -> bool
    stloc   15
    ldloc   10
    ldloc   11
    call    string.Concat(string, string) -> string
    ldloc   12
    call    string.Concat(string, bool) -> string
    ldloc   13
    call    string.Concat(string, int) -> string
    ldloc   14
    ldfld   List.Count
    call    string.Concat(string, int) -> string
    ldloc   15
    call    string.Concat(string, bool) -> string
    call    Console.PrintLine(string) -> void
    ldthis
    ldfn    Corpus.ProgramsCorpus.0lambda2() -> void
    call    Thread.Start(Action) -> Thread
    stloc   16
    ldsfld  Thread.Current
    stloc   17
    ldloc   16
    ldfld   Thread.Id
    stloc   18
    ldloc   16
    ldfld   Thread.Running
    brfalse L2
    ldloc   16
    call    Thread.Join() -> void
L2: ldloc   16
    ldc.i4  10
    conv.i8
    call    Thread.Join(long) -> bool
    stloc   19
    ldloc   16
    call    Thread.Stop() -> void
    ldstr   ""
    ldloc   17
    ldfld   Thread.Id
    call    string.Concat(string, int) -> string
    ldloc   18
    call    string.Concat(string, int) -> string
    ldloc   19
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
