---
section: examples_devices
order: 505
---
# Máquina

Este exemplo lê a máquina em que roda: seu nome, seu processador, seu sistema, sua memória, seus discos e seus processos. Ele também lê e escreve arquivos, com os caminhos que o shell usa.

Os registros que a máquina fornece são fotografias, tiradas quando o programa pede. Um programa que precisa do estado de novo pede outra vez, e guarda o que lhe foi informado enquanto isso. Se você mudar a máquina, peça de novo para ver o novo estado.

## O programa

```sgs Machine.sgs
using System.*;
using System.IO.*;
using System.Collections.*;
using System.Machine.*;
namespace Corpus;
class MachineCorpus {
    static void Main() {
        bool there = File.Exists("a.txt");
        string text = File.Read("a.txt");
        if (File.TryRead("a.txt", out string held)) {
            Console.PrintLine(held);
        }
        bool wrote = File.Write("a.txt", "x") && File.Append("a.txt", "y");
        bool tidied = File.Delete("a.txt") && File.MkDir("d");
        foreach (string entry in File.List("C:\\")) {
            Console.PrintLine(entry);
        }
        Console.PrintLine("" + there + text + wrote + tidied);

        string name = Computer.Name;
        CpuInfo cpu = Computer.Cpu;
        OsInfo os = Computer.Os;
        int ram = Computer.RamMb + Computer.FreeRamMb;
        bool online = Computer.Online;
        Console.PrintLine(name + cpu.Mhz + cpu.Cores + cpu.Era + cpu.Architecture
            + os.Id + os.Name + ram + online);
        foreach (DiskInfo disk in Computer.Disks()) {
            Console.PrintLine(disk.Mount + disk.UsedMb + disk.CapacityMb);
        }
        List<string> programs = Computer.Programs();
        foreach (ProcessInfo process in Computer.Processes()) {
            Console.PrintLine("" + process.Id + process.Name + process.State + process.HeldBytes + programs.Count);
        }
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `File.Exists("a.txt")` | Informa se o arquivo está ali. |
| `File.Read("a.txt")` | Lê o arquivo inteiro como texto. O programa para quando o arquivo não existe. |
| `File.TryRead("a.txt", out held)` | Lê o arquivo e responde true quando consegue. O texto vai para held. |
| `File.Write, File.Append` | Escreve texto no arquivo, ou acrescenta texto ao seu fim. Cada um responde true quando acontece. |
| `File.Delete, File.MkDir` | Apaga um arquivo, ou cria uma pasta. Cada um responde true quando acontece. |
| `File.List(...)` | Lista as entradas de uma pasta, uma de cada vez. |
| `Computer.Name, Cpu, Os` | O nome da máquina, o seu processador (velocidade, núcleos, era e arquitetura) e o seu sistema. |
| `Computer.RamMb, FreeRamMb` | A memória da máquina em megabytes, e a parte dela que está livre. |
| `Computer.Disks()` | Reúne cada disco, com o seu ponto de montagem, os megabytes usados e a sua capacidade. |
| `Computer.Programs()` | Lista os programas da máquina. |
| `Computer.Processes()` | Reúne cada processo, com seu número, nome, estado e os bytes que ocupa. |

> [!DEFINE] Caminho
> O caminho de um arquivo ou de uma pasta, escrito com as pastas que o contêm.

> [!DEFINE] Bloco
> A unidade em que um arquivo é lido ou escrito. O custo de ler ou escrever cresce a cada bloco.

O record do processador informa a sua era, que é a era de hardware da máquina ([](jsc:eras)).

## Se algo der errado

> [!PROBLEM] File.Read é chamado para um arquivo que não existe, e o programa para.
> Use File.TryRead no lugar, que responde false quando o arquivo não está ali.

> [!PROBLEM] Uma escrita não acontece, porque o disco está cheio, e a escrita responde false.
> Confira a resposta de cada escrita e libere um pouco de espaço no disco.

> [!PROBLEM] Ler ou escrever um arquivo grande custa mais, bloco por bloco.
> Leia o arquivo uma vez e guarde o seu texto em uma variável.

> [!SEE] jsc:sigma_library, jsc:eras
