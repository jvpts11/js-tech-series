---
section: sigma_data
order: 424
---
# Listas e mapas

Um array guarda um número fixo de valores de um tipo, numerados a partir de 0. Você cria um com new e o seu tamanho: new int[4] cria um array de quatro ints, com os índices 0 a 3. Length diz quantas posições o array tem. Um índice fora do array para o programa com uma mensagem, como explica a [página de paradas do runtime](jsc:sigma_runtime_stops). Arrays existem no Sigma e no Sigma#.

Uma List é uma fileira crescente de valores de um tipo, escrita List<int> para uma lista de ints. Você cria uma com new List<int>(), e cada Add põe mais um item no fim. Somente no Sigma#.

Table: Membros de List

| Membro | Devolve | O que faz |
| --- | --- | --- |
| `Count` | Um int | Quantos itens a lista guarda. |
| `Add(item)` | Nada | Coloca o item no fim. |
| `Insert(index, item)` | Nada | Coloca o item na posição index, empurrando os itens seguintes. |
| `RemoveAt(index)` | Nada | Remove o item na posição index. |
| `Remove(item)` | Um bool | Remove um item que combine e diz se encontrou algum. |
| `Clear()` | Nada | Remove todos os itens. |
| `Contains(item)` | Um bool | Se o item está na lista. |
| `IndexOf(item)` | Um int | A posição do item, ou -1 quando ele não está lá. |
| `Get(index), Set(index, item)` | Um item, ou nada | Lê ou muda o item na posição index. |
| `Sort()` | Nada | Coloca os itens em ordem. |

Um Map guarda pares: uma chave e o valor que pertence a ela. Map<string, int> cria um mapa de chaves de texto para valores int. Put adiciona um par, e a chave encontra o valor de novo. Um mapa não tem o membro Clear. Somente no Sigma#.

> [!DEFINE] Chave
> A parte de um par que encontra o seu valor em um mapa, como o nome de uma fruta.

> [!DEFINE] Mapa
> Uma coleção de pares: cada chave leva a um valor. Escrito Map<K, V> no Sigma#.

Table: Membros de Map

| Membro | Devolve | O que faz |
| --- | --- | --- |
| `Count` | Um int | Quantos pares o mapa guarda. |
| `Put(key, value)` | Nada | Adiciona o par, ou muda o valor de uma chave que já está lá. |
| `Get(key)` | O valor | O valor da chave. Uma chave ausente devolve null e não para o programa. |
| `ContainsKey(key)` | Um bool | Se a chave está no mapa. |
| `TryGet(key, out value)` | Um bool | Se a chave está lá, e o seu valor sai por out. |
| `Remove(key)` | Um bool | Remove o par, e diz se a chave estava lá. |
| `Keys(), Values()` | Uma List | Cada chave, ou cada valor, em uma List. |

Para descobrir se uma chave está lá antes de lê-la, use ContainsKey, ou use TryGet, que diz e devolve o valor por out em uma só chamada. Um mapa é indexado com a chave entre colchetes, como em count["apple"].

```sgs Stock.sgs
using System.*;
using System.Collections.*;
using System.IO.*;

namespace Inventory;

class Stock
{
    static void Main()
    {
        Map<string, int> count = new Map<string, int>();
        count.Put("apple", 3);
        int got;
        if (count.TryGet("apple", out got))
        {
            Console.PrintLine("" + got);
        }
        List<string> names = count.Keys();
        foreach (string name in names)
        {
            Console.PrintLine(name);
        }
    }
}
```

A instrução foreach percorre uma List ou um array, um item depois do outro. Um mapa não pode ser percorrido com foreach: percorra Keys() ou Values() em vez disso, que são Lists.

O Sigma recusa os tipos List e Map e qualquer tipo escrito com colchetes angulares. No Sigma, use um array com uma contagem sua ao lado, ou use dois arrays para pares.

## Se algo der errado

> [!PROBLEM] O programa para e diz “não há posição 3 em uma list de 3”.
> O índice deve ir de 0 até um a menos que Count. Verifique antes de ler o item.

> [!PROBLEM] O programa para e diz “não há posição 4 em um array de 4”.
> Use os índices de 0 até o tamanho menos um.

> [!PROBLEM] O compilador diz que foreach percorre um array ou uma lista, não um Map.
> Percorra Keys() ou Values() em vez disso, como em foreach (string name in count.Keys()).

> [!PROBLEM] O compilador recusa List no Sigma e diz para usar um array, com uma contagem sua ao lado.
> Use um array, com uma contagem sua ao lado, como o Sigma permite.

> [!SEE] jsc:sigma_control_flow, jsc:sigma_no_exceptions
