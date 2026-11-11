---
section: examples_language
order: 511
---
# Programs

A program can start other programs, talk to them and wait for them. This example names itself, starts three programs, sends one of them a message, waits for two of them, stops the third, and runs a thread beside itself.

A process is a program that runs on the machine and is listed in its process list. Starting one costs more than most calls, because the machine makes a whole process ([](jsc:sigma_running)). If you start a process, you can wait for it, or leave it running.

## The program

```sgs Programs.sgs
using System.*;
using System.IO.*;
using System.Collections.*;
using System.Execution.*;
using System.Threading.*;
namespace Corpus;
class ProgramsCorpus {
    static void Main() {
        Program.SetName("corpus");
        string name = Program.Name;
        List<string> args = Program.Args;
        long dropped = Program.DroppedEvents;
        Process self = Program.Current;
        Process one = Program.Start("C:\\tool.asm");
        Process two = Program.Start("C:\\tool.asm", args);
        Process three = Program.Start("C:\\tool.asm", args, "lab");
        List<string> said = Program.Shell("dir");
        Program.OnMessage(m => Console.PrintLine(m.Text + m.From + m.Tick));
        Console.PrintLine(name + dropped + self.Id + said.Count);

        int id = one.Id;
        string oneName = one.Name;
        string host = one.Host;
        if (one.Running) {
            one.Wait();
        }
        bool done = two.Wait(20);
        int code = two.ExitCode;
        three.Kill();
        List<string> output = two.Output();
        bool sent = Process.Send(id, "hello");
        Console.PrintLine(oneName + host + done + code + output.Count + sent);

        Thread worker = Thread.Start(() => {
            Thread.Sleep(5);
            Thread.Yield();
        });
        Thread current = Thread.Current;
        int threadId = worker.Id;
        if (worker.Running) {
            worker.Join();
        }
        bool joined = worker.Join(10);
        worker.Stop();
        Console.PrintLine("" + current.Id + threadId + joined);
        Program.Exit(0);
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `Program.SetName("corpus")` | Names this program, so the process list shows the name. It costs nothing. |
| `Program.Name, Args, DroppedEvents, Current` | Read the program's name, its arguments, the number of events lost, and its own process. |
| `Program.Start(...)` | Starts a program at a path, with no arguments, with a list of arguments, or with a list and a third text. |
| `Program.Shell("dir")` | Runs one line at this machine's own prompt, to the end, and gives back what it printed. |
| `Program.OnMessage(...)` | Asks to be called with each message, and the lambda prints the message. |
| `one.Id, Name, Host` | The number, the name and the computer of another process. |
| `one.Running, ExitCode` | Tell whether the process still runs, and the number it gave back when it ended. |
| `one.Wait(), two.Wait(20)` | Wait until the process ends, or wait up to a time and answer whether it ended. |
| `three.Kill()` | Stops the process. |
| `two.Output()` | Gives back the lines the process printed. |
| `Process.Send(id, "hello")` | Sends the text hello to the process with that number. |
| `Thread.Start(() => { ... })` | Starts a thread: a body of code that runs beside the program, taking turns with it. |
| `Thread.Sleep(5), Thread.Yield()` | Pause the thread for a short time, or let the other code take a turn. |
| `Thread.Current, worker.Id, Running` | The thread that runs this code, the number of the worker, and whether it runs. |
| `worker.Join(), Join(10), Stop()` | Wait for the thread to end, wait for a time, or stop the thread. |
| `Program.Exit(0)` | Ends the program and hands 0 back to the machine as its exit code. |

> [!DEFINE] Exit code
> A number a program hands back when it ends. 0 usually means success.

## If something goes wrong

> [!PROBLEM] The program waits, and it does not go on, because the process has not ended.
> Use Wait with a time, which answers false when the time is up.

> [!PROBLEM] The Sigma compiler refuses Thread, because these machines run one thing at a time.
> Build the program in Sigma#, the full language.

> [!SEE] jsc:sigma_running, jsc:sigma_library
