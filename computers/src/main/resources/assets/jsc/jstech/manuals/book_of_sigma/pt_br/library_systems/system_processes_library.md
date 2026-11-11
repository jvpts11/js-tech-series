---
section: library_systems
order: 477
---
# Processos, memória, discos, serviços e drivers

Com estes você lê a máquina em que o seu sistema roda, e roda os programas dele. Qualquer programa pode ler a máquina com eles, e só o sistema de um jogador tem serviços próprios.

Table: Membros de Processes

| Membro | O que faz |
| --- | --- |
| `Processes.Start(path), Processes.Start(path, args)` | Inicia um programa do disco e dá o processo dele. |
| `Processes.Stop(pid, signal)` | Para o processo com esse número. |
| `Processes.List()` | Cada processo que a máquina roda. |
| `Processes.SetPriority(pid, level)` | high, normal, low ou idle. |
| `Processes.Yield()` | Dá o resto da vez ao próximo programa. |
| `Processes.Current` | O processo que pergunta. |

Table: Membros de Disk e Memory

| Membro | O que dá |
| --- | --- |
| `Disk.FreeMb, Disk.SizeMb` | O espaço que sobra no disco do sistema e o tamanho dele, em megabytes (MB). |
| `Disk.Mounted()` | Os outros discos e mídias que a máquina lê. |
| `Memory.TotalMb, Memory.FreeMb` | A memória da máquina e o que dela está livre, em megabytes (MB). |
| `Memory.HeldBy(pid)` | O que um processo ocupa, em megabytes (MB). |
| `Memory.Ledger()` | Uma linha para cada coisa que ocupa memória. |
| `Memory.Refuse, Memory.GoDown` | O que OnOutOfMemory dá para recusar, e para cair. |
| `Memory.Alloc, AllocZeroed, Resize and Free` | A memória crua do próprio programa, que os ponteiros alcançam, a partir da versão 3, como mostra a página sobre ponteiros. |
| `Memory.Copy, Fill and Compare` | Bytes de memória crua movidos, preenchidos ou comparados, uma instrução a cada 64 deles. |

Table: Membros de Services e Drivers

| Membro | O que faz |
| --- | --- |
| `Services.Register(name, path)` | Nomeia um programa do disco como serviço, que inicia quando o sistema inicia. |
| `Services.Start(name), Services.Stop(name)` | Inicia ou para um serviço. Cada um dá true quando conseguiu. |
| `Services.Status(name)` | running, stopped ou unknown. |
| `Drivers.Devices()` | Os dispositivos que o sistema controla. |
| `Drivers.Has(class)` | Se a máquina tem um dispositivo dessa classe que o sistema controla. |
| `Device.Name, Device.Class` | O nome de um dispositivo, e a classe dele. |

O Sigma alcança pelo Standard os membros que recebem e dão valores simples, e deixa ao Sigma# o que dá uma lista ou um processo.

## Se algo der errado

> [!PROBLEM] Services.Start dá false.
> Não há programa com esse nome no disco. Compile o serviço na pasta do sistema.

> [!SEE] jsc:system_os_library
