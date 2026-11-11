---
section: library_systems
order: 475
---
# System.OS: um sistema operacional próprio

Você pode escrever um sistema operacional próprio em Sigma#. Ele é uma classe que implementa ISystem, no namespace System.OS. A máquina o roda como o processo 1 dela, e desenha as páginas, os arquivos e as janelas dele por ele, então até uma máquina fraca dá conta.

> [!DEFINE] Sistema
> A classe que implementa ISystem: ela declara o que o sistema é, e os eventos dela comandam a máquina.

> [!DEFINE] Kernel
> As regras pelas quais um sistema roda: como ele divide o processador, o sistema de arquivos, os drivers, o que faz quando a memória acaba, e o shell.

Table: O que um sistema declara

| Propriedade | O que diz |
| --- | --- |
| `Name, Version, Author` | O nome do sistema, a versão e quem o escreveu, como texto. |
| `Era` | A era de hardware mais antiga em que o sistema se instala. |
| `Isa` | O conjunto de instruções para o qual o sistema é compilado. |
| `MemoryMb` | A memória que o sistema guarda para si, em megabytes (MB). |
| `Kernel` | Um dos kernels do mod, ou um próprio do sistema. |

Você escreve cada uma delas com => e um valor que o compilador conhece: um escrito por extenso, ou um que a biblioteca nomeia. O compilador as lê para conhecer o sistema antes que ele rode.

Table: Eventos de um sistema

| Evento | Quando roda |
| --- | --- |
| `OnBoot()` | Uma vez, quando a máquina inicia. |
| `OnTick()` | A cada tick enquanto o sistema está no ar. |
| `OnInput(InputEvent e)` | Quando uma tecla é pressionada ou o mouse é clicado na tela própria do sistema. |
| `OnShutdown()` | Uma vez, quando a máquina desliga. |
| `int OnSchedule(List<int> ready)` | Opcional. Antes de cada fatia, com os números dos processos prontos para rodar, num kernel próprio. Ele dá o número do processo que roda em seguida. |
| `int OnOutOfMemory(string asker, int mb)` | Opcional. Quando um programa pede mais memória do que a máquina tem, num kernel próprio. Ele dá Memory.Refuse, Memory.GoDown ou o número de um processo a encerrar. |

Table: Membros de Era e Isa

| Membro | O que é |
| --- | --- |
| `Era.Vintage, Era.Legacy, Era.Transition, Era.Standard, Era.Advanced` | As eras de hardware. |
| `Isa.IA16, Isa.IA32, Isa.X86_64, Isa.M68K, Isa.PowerPC, Isa.ARM64` | Os conjuntos de instruções. |
| `Era.Name, Isa.Name` | A era ou o conjunto de instruções, como texto. |

Table: Membros de Kernel

| Membro | O que faz |
| --- | --- |
| `Kernel.Of(name)` | Um dos kernels do mod, com todas as regras dele: dos, win9x, nt, linux, freebsd, unix ou net_min. |
| `Kernel.Own()` | Um kernel próprio do sistema, cujas regras você escolhe com os membros abaixo. |
| `Kernel.Scheduler(kind), Kernel.Scheduler(kind, slice)` | Como o processador é dividido: none, cooperative ou preemptive, com uma fatia de 16 a 256 instruções. Uma fatia tem 64 instruções quando você não dá nenhuma. |
| `Kernel.FileSystem(kind)` | O sistema de arquivos: dos, posix ou flat. |
| `Kernel.Drivers(classes)` | Os dispositivos que o sistema controla, nomeados num só texto com vírgulas: text, graphics, sound, network, disks, optical, usb e printer. |
| `Kernel.MemoryRule(rule)` | O que acontece quando a memória acaba: refuse, end ou down. |
| `Kernel.Shell(family)` | O shell: dos, posix, mc-net, ou own para uma tela que o próprio sistema escreve. |
| `Kernel.Name` | O nome do kernel, como texto. |

## As regras de um kernel próprio

Sem escalonador, só o sistema e o programa da frente rodam, e os outros esperam. Um programa cooperativo roda até chamar Processes.Yield, esperar ou terminar. Um kernel preemptivo divide o processador em fatias: um programa de prioridade alta ganha duas fatias por rodada, um de prioridade baixa fica de fora de uma rodada a cada duas, e um ocioso só roda quando todos os outros programas esperam.

Um dispositivo sem driver não existe para o sistema: sem driver de som ele não toca nada, e sem driver de rede ele não está em rede nenhuma.

```sgs LanternOs.sgs
using System.OS.*;
namespace Lantern;

class LanternOs : ISystem {
    public string Name => "Lantern OS";
    public string Version => "1.2";
    public string Author => "Alex";
    public Era Era => Era.Legacy;
    public Isa Isa => Isa.IA32;
    public int MemoryMb => 48;
    public Kernel Kernel => Kernel.Of("linux");

    public void OnBoot() {
        Boot.Line("Lighting the lantern...");
        Boot.Done();
        Desktop.Start("lantern-desktop");
    }
    public void OnTick() { }
    public void OnInput(InputEvent e) { }
    public void OnShutdown() { }
}
```

sigma build --os grava o sistema numa mídia virgem da era dele, numa das unidades da máquina, e sgpack --publish o coloca no Mirror da rede, de onde qualquer máquina da rede pode inicializar o instalador dele pelo firmware.

## Se algo der errado

> [!PROBLEM] A compilação para com S3062.
> O sistema deixa de fora uma propriedade que tem de declarar. Acrescente-a, escrita com =>.

> [!PROBLEM] O sistema cai na própria página logo que inicia.
> A página nomeia o erro e a linha dele. Corrija essa linha, compile de novo e instale de novo.

> [!SEE] jsc:system_screen_library, jsc:system_processes_library, jsc:system_desktop_library
