---
section: library_basics
order: 442
---
# Computer: o que a máquina é

Um programa pode perguntar à máquina em que roda o que ela é: seu nome, seu processador, seu sistema, sua memória e seus discos. As respostas são fotos tiradas no momento em que você pergunta, não visões que se atualizam sozinhas.

Você lê a máquina pelo Computer, um tipo embutido cujos membros você lê diretamente. Escreva Computer.Name, e não o nome de uma variável que guarda um computador.

Table: Membros de Computer

| Membro | O que entrega |
| --- | --- |
| `Computer.Name` | O nome da máquina, como texto. |
| `Computer.Cpu` | O processador (CPU), como um record CpuInfo. Somente Sigma#. |
| `Computer.Os` | O sistema, como um record OsInfo. Somente Sigma#. |
| `Computer.RamMb` | A memória em megabytes (MB), como um número inteiro. |
| `Computer.FreeRamMb` | A memória livre agora, em megabytes (MB). Somente Sigma#. |
| `Computer.Online` | Sim ou não. |
| `Computer.Disks()` | Uma lista de records DiskInfo. Somente Sigma#. |
| `Computer.Programs()` | Os nomes dos programas. Somente Sigma#. |
| `Computer.Processes()` | Uma lista de records ProcessInfo. Somente Sigma#. |

Name, RamMb e Online funcionam também no Sigma. O resto é somente Sigma#.

## Os records

Um record é um valor com campos fixos, e você lê cada campo pelo nome. Estes são os records que os membros de Computer devolvem.

Table: Records de Computer

| Record | Campos | O que cada campo guarda |
| --- | --- | --- |
| `CpuInfo` | `CpuInfo.Mhz, CpuInfo.Cores, CpuInfo.Era, CpuInfo.Architecture` | Mhz: o clock mais rápido de todos os núcleos, em megahertz (MHz). Cores: todos os núcleos juntos. Era: a era do primeiro processador. Architecture: o conjunto de instruções que ele executa. |
| `DiskInfo` | `DiskInfo.Mount, DiskInfo.UsedMb, DiskInfo.CapacityMb` | Mount: a letra da unidade. UsedMb e CapacityMb: megabytes usados e no total. |
| `OsInfo` | `OsInfo.Id, OsInfo.Name` | O id do sistema e o seu nome. |
| `ProcessInfo` | `ProcessInfo.Id, ProcessInfo.Name, ProcessInfo.State, ProcessInfo.HeldBytes` | O número do processo, o seu nome, o seu estado como texto e os bytes de memória que ele ocupa. |

Os nomes das eras são vintage, legacy, transition, standard, advanced, exa e singularity, escritos em minúsculas. Uma máquina tem sete eras para escolher, e uma era não é a mesma coisa que um tier de hardware.

> [!DEFINE] Arquitetura
> O conjunto de instruções que um processador executa. Um programa feito para um roda nesse processador e em todos os mais novos.

> [!DEFINE] Ponto de montagem
> A letra da unidade pela qual um disco é acessado.

> [!DEFINE] Propriedade
> Um valor que pertence a um objeto ou a um record, e que você lê pelo nome, como Computer.Name.

## Se algo der errado

> [!PROBLEM] Um valor parece desatualizado.
> Pergunte de novo. Cada leitura tira uma nova foto da máquina.

> [!PROBLEM] Um programa Sigma não enxerga Computer.Cpu.
> Use o Sigma#, que oferece isso, ou use os membros que o Sigma oferece aqui.

> [!SEE] jsc:the_systems, jsc:processors, jsc:eras
