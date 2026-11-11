---
section: the_achievements
order: 173
icon: jsc:cd_rom
---
# Sigma achievements

Sigma and Sigma# are the two programming languages of the computers, and the Sigma tab rewards you for writing them, compiling them and running them. The Book of Sigma explains both languages from zero ([](jsc:sigma_two_languages)).

Most Sigma achievements of a program credit the operator of the machine it runs on. The compile and run commands credit the player who typed them.

Table: The Sigma achievements

| Achievement | What it asks | How it is earned |
| --- | --- | --- |
| Sigma (root) | Install the Σ# Compiler. | The sgsc package is installed. |
| Erm, What the Sigma? (task) | Run a Sigma program. | A program starts at a terminal. |
| Expected ';' but Found '}' (secret) | Fail to compile a program. | A compile fails with errors, so the compiler produces nothing. |
| Back in My Day (goal) | Compile a Σ program on MC-DOS. | A Sigma program, not Sigma#, compiles on MC-DOS. |
| Stack Overflow (secret) | Have a program halt 1,024 calls deep. | A program's chain of calls goes more than 1,024 deep. |
| Undefined Behaviour (secret) | Have a program halt dividing by zero. | A program stops with a divide-by-zero or remainder-by-zero error. |
| Now You Have Two Problems (task) | Start a second thread in a program. | A program starts a thread. |
| Look Ma, No Console (task) | Open a window from a program. | A program opens a window. |
| Stonks (task) | Have a program woken by a watch on network stock. | A watch fires and wakes a program. |
| Works on My Machine (task) | Run the same program on two computers. | A program starts on a different machine from the one where you first ran it. |
| Ship It (goal) | Pack a program with sgpack and publish it to the Mirror. | A package is published to the Mirror. |
| Automate the Boring Stuff (goal) | Have a program set an Operation going on the network. | A program sets an Operation going. |
| Five Nines (challenge) | Keep a program running for seven days without stopping. | A program runs without stopping for 168,000 ticks. |
| Rubber Duck Debugging (task) | Stop a program at a breakpoint. | Virtual Studio's debugger stops your program at a breakpoint. |

Five Nines counts ticks, not seconds: 24,000 ticks make one in-game day, so seven days make 168,000 ticks. A tick is the game's basic unit of time, one twentieth of a second ([](jsc:units_and_ticks)).

Works on My Machine needs the game to remember where you ran a program: it keeps up to 64 program names for each player.

## If something goes wrong

> [!PROBLEM] The Expected ';' achievement appeared for an error you did not expect.
> Any failed compile earns it, whichever error it is. Read the error line to find the real problem ([](jsc:sigma_reading_an_error)).

> [!PROBLEM] A program stops with a message about calls.
> Calls went more than 1,024 deep: a method may be calling itself without end. Look for a method that calls itself ([](jsc:sigma_runtime_stops)).

> [!PROBLEM] A program stops with "divided by zero".
> Check the divisor before you divide. A program that halts this way earns Undefined Behaviour.

> [!PROBLEM] Five Nines does not appear.
> The program must run without a stop for 168,000 ticks. The game checks once every 1,200 ticks, so it can appear up to a minute later.

> [!SEE] jsc:sigma_compile, jsc:sigma_run, jsc:sigma_limits, jsc:book_of_sigma
