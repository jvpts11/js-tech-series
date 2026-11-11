---
section: examples_network
order: 503
---
# Barramentos

Um barramento é uma parte da rede que move itens, e cada barramento tem um nome. Este exemplo encontra dois barramentos pelos seus nomes e os configura, uma chamada por vez.

Cada configuração devolve o mesmo barramento, então as chamadas se encadeiam: uma configuração é seguida pela próxima na mesma linha. Os próprios barramentos, com seus filtros e tags, são explicados em ([](jsc:buses)).

## O programa

```sgs Buses.sgs
using System.*;
using System.IO.*;
using System.Network.*;
namespace Corpus;
class BusCorpus {
    static void Main() {
        Bus ore = bus("Ore in");
        if (ore != null) {
            ore.On().Continuous().Only("iron_ore, coal").Keep(16).Max(64).Priority(5);
            ore.AllBut("dirt").Tag("c:ores").Fuzzy(true).WhenStock("iron_ore", 512).WhenStockTag("c:ores", 4096);
            ore.Between(18, 6).After(Bus.Named("Coal in")).OnDemand().Off();
            BusItem iron = ore.Item("iron_ore").Keep(16).Max(64);
            Console.PrintLine(ore.Name + iron.Bus + iron.Item);
        }
        Bus wall = Bus.Named("Chest wall");
        if (wall != null) {
            wall.ReadWrite().ReadOnly().WriteOnly();
        }
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `bus("Ore in")` | Encontra o barramento chamado Ore in, ou devolve null quando nenhum barramento tem esse nome. |
| `if (ore != null)` | Verifica se o barramento foi encontrado antes de usar qualquer configuração. |
| `On(), Continuous(), Only(...)` | Liga o barramento, mantém-o em movimento e aceita somente os itens listados. |
| `Keep(16), Max(64), Priority(5)` | Define quantos manter, o máximo a mover e o número de prioridade. |
| `AllBut(...), Tag(...), Fuzzy(true)` | Recusa os itens listados, adiciona uma tag e liga a correspondência aproximada de itens. |
| `WhenStock("iron_ore", 512)` | Age somente quando o estoque de minério de ferro chega a 512. |
| `WhenStockTag("c:ores", 4096)` | Age somente quando o estoque da tag chega a 4.096. |
| `Between(18, 6)` | Define duas horas do dia, cada uma de 0 a 23. |
| `After(Bus.Named("Coal in"))` | Executa este barramento depois do barramento chamado Coal in. |
| `OnDemand(), Off()` | Faz o barramento se mover somente quando a rede pedir, e o desliga. |
| `ore.Item("iron_ore")` | Retorna as configurações de um item do barramento, para que Manter e Máximo possam ser definidos só para esse item. |
| `Console.PrintLine(...)` | Imprime o nome do barramento, depois o barramento e o item a que essas configurações pertencem. |
| `wall.ReadWrite(), ReadOnly(), WriteOnly()` | Mostra as três configurações de acesso, uma após a outra, no segundo barramento. |

## Se algo der errado

> [!PROBLEM] O nome não retorna nada: nenhum barramento tem esse nome, então o barramento é null.
> Confira a grafia do nome, e teste se é null antes de usar o barramento.

> [!PROBLEM] Between recebe uma hora fora do intervalo de 0 a 23, e o programa para com uma mensagem como "25 não é uma hora do dia".
> Informe horas de 0 a 23 somente.

> [!PROBLEM] Um barramento não aceita algumas configurações, e o programa para naquela linha com a mensagem do barramento.
> Remova essa linha, ou use um barramento que aceite a configuração.

> [!SEE] jsc:buses, jsc:sigma_library
