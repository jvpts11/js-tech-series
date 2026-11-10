.asm 4
.arch jsc:x86
.start Corpus.PointersCorpus console

.struct Corpus.Cell
.source Pointers.sgs
.field int Value
.field long Weight
.field byte Mark

.class Corpus.PointersCorpus
.source Pointers.sgs

.method static int Sum(int*, int) slots 4
.line 6
    ldc.i4  0
    stloc   2
.line 7
    ldc.i4  0
    stloc   3
L1: ldloc   3
    ldloc   1
    clt
    brfalse L3
.line 8
    ldloc   2
    ldloc   0
    ldloc   3
    conv.i8
    ldc.i8  4
    mul
    add
    ldind.i4
    add
    stloc   2
.line 7
L2: ldloc   3
    dup
    ldc.i4  1
    add
    stloc   3
    pop
    br      L1
.line 10
L3: ldloc   2
    ret

.method static void Main() slots 17
.try L6 L9 catch AccessViolationException L9 L8
    ldc.i8  4
    localloc
    stloc   9
.line 14
    ldc.i4  4
    ldc.i4  4
    mul
    conv.i8
    call    Memory.Alloc(long) -> void*
    stloc   0
.line 15
    ldc.i4  0
    stloc   1
L1: ldloc   1
    ldc.i4  4
    clt
    brfalse L3
.line 16
    ldloc   0
    ldloc   1
    conv.i8
    ldc.i8  4
    mul
    add
    ldloc   1
    ldc.i4  1
    add
    stind.i4
.line 15
L2: ldloc   1
    dup
    ldc.i4  1
    add
    stloc   1
    pop
    br      L1
.line 18
L3: ldstr   ""
    ldloc   0
    ldc.i4  4
    call    Corpus.PointersCorpus.Sum(int*, int) -> int
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
.line 19
    ldloc   0
    ldc.i4  8
    ldc.i4  4
    mul
    conv.i8
    call    Memory.Resize(void*, long) -> void*
    stloc   0
.line 20
    ldc.i4  2
    conv.i8
    ldc.i4  24
    conv.i8
    call    Memory.AllocZeroed(long, long) -> void*
    stloc   2
.line 21
    ldloc   2
    ldc.i4  3
    stind.i4
.line 22
    ldloc   2
    ldc.i8  24
    add
    ldloc   2
    stloc   3
    newobj  Corpus.Cell()
    dup
    ldloc   3
    ldind.i4
    stfld   Corpus.Cell.Value
    dup
    ldloc   3
    ldc.i8  8
    add
    ldind.i8
    stfld   Corpus.Cell.Weight
    dup
    ldloc   3
    ldc.i8  16
    add
    ldind.u1
    stfld   Corpus.Cell.Mark
    copy
    stloc   4
    stloc   5
    ldloc   5
    ldloc   4
    ldfld   Corpus.Cell.Value
    stind.i4
    ldloc   5
    ldc.i8  8
    add
    ldloc   4
    ldfld   Corpus.Cell.Weight
    stind.i8
    ldloc   5
    ldc.i8  16
    add
    ldloc   4
    ldfld   Corpus.Cell.Mark
    stind.i1
.line 23
    ldloc   2
    ldc.i8  24
    add
    ldc.i8  16
    add
    ldc.i4  255
    stind.i1
.line 24
    ldc.i4  16
    conv.i8
    localloc
    stloc   6
.line 25
    ldloc   6
    ldc.i4  7
    ldc.i4  16
    conv.i8
    call    Memory.Fill(void*, int, long) -> void
.line 26
    ldc.i4  16
    conv.i8
    call    Memory.Alloc(long) -> void*
    stloc   7
.line 27
    ldloc   7
    ldc.i4  0
    ldc.i4  16
    conv.i8
    call    Memory.Fill(void*, int, long) -> void
.line 28
    ldloc   7
    ldloc   6
    ldc.i4  16
    conv.i8
    call    Memory.Copy(void*, void*, long) -> void
.line 29
    ldloc   7
    ldloc   6
    ldc.i4  16
    conv.i8
    call    Memory.Copy(void*, void*, long) -> void
.line 30
    ldloc   7
    ldloc   6
    ldc.i4  16
    conv.i8
    call    Memory.Compare(void*, void*, long) -> int
    ldstr   " "
    call    string.Concat(int, string) -> string
    ldloc   7
    ldloc   6
    ldc.i4  16
    conv.i8
    call    Memory.Compare(void*, void*, long) -> int
    call    string.Concat(string, int) -> string
    call    Console.PrintLine(string) -> void
.line 31
    ldc.i4  5
    stloc   8
    ldloc   9
    ldloc   8
    stind.i4
.line 32
    ldloc   9
    stloc   10
.line 33
    ldloc   10
    stloc   11
    ldloc   11
    ldloc   11
    ldind.i4
    ldc.i4  1
    add
    stind.i4
.line 34
    ldloc   9
    ldind.i4
    ldstr   " "
    call    string.Concat(int, string) -> string
    ldloc   10
    conv.i8
    ldc.i8  0
    ceq
    brfalse L4
    ldstr   "none"
    br      L5
L4: ldstr   "some"
L5: call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
.line 35
    ldc.i4  2
    conv.i8
    ldc.i4  8
    conv.i8
    call    Memory.AllocZeroed(long, long) -> void*
    stloc   12
.line 36
    ldloc   12
    ldc.i4  4
    ldc.i4  8
    mul
    conv.i8
    call    Memory.Resize(void*, long) -> void*
    stloc   12
.line 37
    ldloc   12
    call    Memory.Free(void*) -> void
.line 38
    ldloc   7
    call    Memory.Free(void*) -> void
.line 39
    ldloc   2
    call    Memory.Free(void*) -> void
.line 40
    ldloc   0
    call    Memory.Free(void*) -> void
.line 42
L6: ldloc   0
    ldind.i4
    stloc   13
    leave   L8
L9: stloc   14
    ldloc   14
    stloc   15
.line 44
    ldstr   "freed: "
    ldloc   15
    ldfld   Exception.Message
    call    string.Concat(string, string) -> string
    call    Console.PrintLine(string) -> void
    leave   L8
.line 46
L8: newobj  AccessViolationException()
    stloc   16
.line 47
    ldstr   "made"
    newobj  AccessViolationException(string)
    stloc   16
.line 48
    ldstr   "made again"
    ldloc   16
    newobj  AccessViolationException(string, Exception)
    stloc   16
.line 49
    ldloc   16
    ldfld   Exception.Message
    call    Console.PrintLine(string) -> void
    ret

.class Exception
.field string Message
.field string File
.field int Line
.field string[] Trace
.field Exception Inner

.method void .ctor() slots 0
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    stfld   Message
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    stfld   Message
    ldthis
    ldloc   1
    stfld   Inner
    ret

.class NullReferenceException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class InvalidOperationException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class ObjectDisposedException : InvalidOperationException

.method void .ctor() slots 0
    ldthis
    call    InvalidOperationException..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    InvalidOperationException..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    InvalidOperationException..ctor(string, Exception) -> void
    ret

.class DivideByZeroException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class IndexOutOfRangeException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class ArgumentException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class ArgumentOutOfRangeException : ArgumentException

.method void .ctor() slots 0
    ldthis
    call    ArgumentException..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    ArgumentException..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    ArgumentException..ctor(string, Exception) -> void
    ret

.class FormatException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class IOException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class FileNotFoundException : IOException

.method void .ctor() slots 0
    ldthis
    call    IOException..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    IOException..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    IOException..ctor(string, Exception) -> void
    ret

.class NetworkException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret

.class AccessViolationException : Exception

.method void .ctor() slots 0
    ldthis
    call    Exception..ctor() -> void
    ret

.method void .ctor(string) slots 1
    ldthis
    ldloc   0
    call    Exception..ctor(string) -> void
    ret

.method void .ctor(string, Exception) slots 2
    ldthis
    ldloc   0
    ldloc   1
    call    Exception..ctor(string, Exception) -> void
    ret
