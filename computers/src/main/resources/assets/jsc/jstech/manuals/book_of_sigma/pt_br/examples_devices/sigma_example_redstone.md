---
section: examples_devices
order: 506
---
# Redstone

Redstone é o sinal de energia do mundo, com potência de 0 a 15. Uma Redstone Interface liga a máquina ao redstone do mundo, para que um programa leia um sinal ou emita um ([](jsc:redstone_interfaces)).

Este exemplo encontra uma interface chamada Gate, define-a para ler e para emitir potência 15, e imprime o seu nome e a sua potência. Você pode associar cada linha com a tabela abaixo. Depois encontra uma interface chamada Lever e imprime o seu nome.

## O programa

```sgs Redstone.sgs
using System.*;
using System.IO.*;
using System.Machine.*;
namespace Corpus;
class RedstoneCorpus {
    static void Main() {
        Redstone gate = redstone("Gate");
        if (gate != null) {
            gate.In().Out(15);
            Console.PrintLine(gate.Name + Convert.ToString(gate.Level()));
        }
        Redstone lever = Redstone.Named("Lever");
        if (lever != null) {
            Console.PrintLine(lever.Name);
        }
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `redstone("Gate")` | Encontra a Redstone Interface chamada Gate. Dá null quando nenhuma está ligada com esse nome. |
| `gate.In()` | Faz a interface ler o sinal. |
| `gate.Out(15)` | Faz a interface emitir potência 15, a mais forte. |
| `gate.Level()` | A potência que a interface lê ou emite. Ela é 0 quando nada a alimenta. |
| `gate.Name` | O nome da interface. |
| `Redstone.Named("Lever")` | Encontra uma segunda interface pelo nome, do mesmo jeito. |

> [!DEFINE] Redstone Interface
> Um bloco que liga a máquina ao redstone do mundo, para que um programa leia ou emita um sinal.

> [!DEFINE] Potência
> A potência de um sinal de redstone, de 0 a 15: potência 0 é desligado, e potência 15 é a mais forte.

## Se algo der errado

> [!PROBLEM] Nenhuma interface tem esse nome, então o valor é null.
> Dê à interface esse nome, e teste contra null antes de usá-la.

> [!PROBLEM] Out recebe um número fora de 0 a 15, e o programa para com "16 não é uma potência de redstone, que vai de 0 a 15".
> Use uma potência de 0 a 15.

> [!PROBLEM] A interface não está mais ligada à máquina, e o programa para com "nenhuma Interface de Redstone chamada 'Gate' está ligada a esta máquina".
> Ligue a interface à máquina de novo.

> [!SEE] jsc:redstone_interfaces, jsc:sigma_library
