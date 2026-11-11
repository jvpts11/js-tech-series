---
section: library_toolkit
order: 465
---
# Queue, Stack, Set, e Find e Sort com uma função

O Sigma# 3 acrescenta três coleções à List e ao Map, no System.Collections, e duas chamadas numa lista que recebem uma função que você escreve com =>.

Table: As coleções novas

| Membro | O que faz |
| --- | --- |
| `Queue<T>.Enqueue(value), Dequeue(), Peek()` | Uma fila devolve primeiro o que recebeu primeiro. Dequeue tira; Peek só olha. |
| `Stack<T>.Push(value), Pop(), Peek()` | Uma pilha devolve primeiro o que recebeu por último. |
| `Set<T>.Add(value), Remove(value), Items()` | Um conjunto guarda cada valor uma vez. Add dá não quando ele já tinha o valor; Items lista os valores na ordem em que chegaram. |
| `Count, Contains(value), Clear()` | Nas três, como numa lista. |

Table: Uma lista com uma função

| Membro | O que faz |
| --- | --- |
| `List<T>.Find(test)` | O primeiro elemento a que o teste diz sim, ou o valor vazio do tipo: 0, false ou null. |
| `List<T>.Sort(compare)` | Ordena a lista por uma comparação de dois elementos, que dá menos que 0 quando o primeiro vem antes. Os elementos que ela acha iguais mantêm a ordem. |

Tirar de uma fila ou pilha vazia para o programa. Find e Sort chamam a sua função uma vez para cada elemento que olham, e cada chamada custa o que as instruções dela custam.

```sgs Sorting.sgs
List<int> counts = new List<int>();
counts.Add(64); counts.Add(8); counts.Add(32);
counts.Sort((a, b) => a - b);
int big = counts.Find(n => n > 16);
```

> [!DEFINE] Fila
> Uma coleção que devolve primeiro o que entrou primeiro, como uma fila de pessoas.

> [!DEFINE] Pilha
> Uma coleção que devolve primeiro o que entrou por último, como uma pilha de pratos.

## Se algo der errado

> [!PROBLEM] O programa para e diz que a fila está vazia.
> Confira o Count antes do Dequeue ou do Peek.

> [!SEE] jsc:sigma_list_map
