---
section: examples_network
order: 504
---
# Fabricação

Fabricação é o trabalho da rede de fazer itens a partir de padrões. Este exemplo configura duas interfaces de fabricação e dois roteadores, depois pede à rede que funda ferro bruto.

A própria linguagem da rede, o IQL, também cuida da fabricação ([](jsc:iql)). As palavras dessa linguagem não são necessárias aqui: este programa usa somente as chamadas da biblioteca.

Padrões e trabalhos são explicados em ([](jsc:how_autocrafting_works)). Roteadores são explicados em ([](jsc:input_routers_and_receiving_buses)).

## O programa

```sgs Crafting.sgs
using System.*;
using System.IO.*;
using System.Network.*;
using System.Operations.*;
namespace Corpus;
class CraftingCorpus {
    static void Main() {
        CraftInterface kiln = craftInterface("Kiln");
        if (kiln != null) {
            kiln.Exclusive(true).MaxJobs(4).Pause();
            kiln.Resume();
            Console.PrintLine(kiln.Name);
        }
        CraftRouter north = craftRouter("North");
        if (north != null) {
            north.Only("gravel").Fuzzy(false);
            north.AllBut("dirt");
            north.Tag("c:sands");
            north.Any();
            Console.PrintLine(north.Name);
        }
        CraftInterface mixer = CraftInterface.Named("Mixer");
        CraftRouter west = CraftRouter.Named("West");
        if (mixer != null && west != null) {
            mixer.Route("Coarse dirt", "gravel", west);
            mixer.Route("Coarse dirt", "dirt", null);
        }
        AskResult smelted = Operations.Update("minecraft:raw_iron", "SMELT", 8);
        Console.PrintLine(smelted.Message);
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `craftInterface("Kiln")` | Encontra a Interface de Fabricação pelo nome que a janela dela lhe dá, ou devolve null quando nenhuma tem esse nome. |
| `Exclusive(true), MaxJobs(4), Pause()` | Mantém a interface para este programa, permite quatro trabalhos ao mesmo tempo e impede a interface de aceitar trabalhos. |
| `Resume()` | Permite que a interface aceite trabalhos de novo. |
| `craftRouter("North")` | Encontra um roteador pelo nome, do mesmo modo. |
| `Only("gravel"), Fuzzy(false)` | Configura o roteador para aceitar somente gravel, e desliga a correspondência aproximada. |
| `AllBut("dirt"), Tag("c:sands")` | Configura o roteador para recusar dirt, e para aceitar os itens da tag c:sands. |
| `Any()` | Faz o roteador aceitar qualquer coisa. |
| `CraftInterface.Named, CraftRouter.Named` | Encontra a interface Mixer e o roteador West pelos seus nomes. |
| `Route("Coarse dirt", "gravel", west)` | Envia a entrada de gravel do padrão Coarse dirt pelo roteador West. |
| `Route("Coarse dirt", "dirt", null)` | Roteia a entrada de dirt pelos próprios filtros dos roteadores, já que nenhum roteador foi dado. |
| `Operations.Update(...)` | Pede à rede para fundir oito ferros brutos, e devolve uma resposta com Ok e Message. |
| `Console.PrintLine(...)` | Imprime a mensagem daquela resposta. |

> [!DEFINE] Padrão
> Uma descrição guardada de um item que a rede consegue fabricar, e dos itens necessários para fabricá-lo.

> [!DEFINE] Job
> Uma tarefa de fabricação que a rede executa, como fabricar um item.

> [!DEFINE] Roteador
> Um filtro que envia uma entrada de um padrão para um cabo de fabricação.

## Se algo der errado

> [!PROBLEM] Um nome não retorna nada: nenhuma interface ou roteador tem esse nome, então o valor é null.
> Use o nome que a janela da interface mostra, e teste se é null antes de usá-lo.

> [!PROBLEM] Operations.Update executa em um computador sem rede, e o programa para com "este computador não está em uma rede".
> Execute o programa em um computador que esteja em uma rede.

> [!PROBLEM] Um Route não tem roteador, então a entrada segue os filtros dos roteadores em vez de um roteador só.
> Dê ao Route um roteador quando quiser escolher o caminho você mesmo.

> [!PROBLEM] Uma configuração é recusada, e o programa para com a mensagem da interface ou do roteador.
> Remova essa linha, ou use uma interface ou um roteador que aceite a configuração.

> [!SEE] jsc:iql, jsc:how_autocrafting_works, jsc:sigma_library
