---
section: library_machine
order: 450
---
# Barramentos a partir de um programa

Um barramento é um bloco que move itens entre a rede e uma máquina. Os seus ajustes dizem quais itens ele trata, quantos mantém e move, e quando trabalha. O próprio barramento é explicado em [](jsc:buses).

Se você quer definir um barramento, Bus.Named o encontra pelo nome na rede. Ele devolve null quando nenhum barramento com esse nome está na rede. Bus chegou na versão 2.

Table: Membros do barramento

| Membro | O que faz |
| --- | --- |
| `Bus.Named(name)` | O barramento com esse nome, ou null quando não há nenhum. |
| `Bus.Name` | O nome do barramento. |
| `Bus.On()` | Liga o barramento. |
| `Bus.Off()` | Desliga o barramento. |
| `Bus.Continuous()` | Define o modo em que o barramento funciona. |
| `Bus.OnDemand()` | Define o outro modo, sob demanda. |
| `Bus.ReadWrite()` | Define o barramento para mover itens nos dois sentidos. |
| `Bus.ReadOnly()` | Define o barramento para tirar itens somente. |
| `Bus.WriteOnly()` | Define o barramento para pôr itens somente. |
| `Bus.Only(items)` | O barramento move apenas os itens listados. |
| `Bus.AllBut(items)` | O barramento move todos os itens, exceto os listados. |
| `Bus.Tag(tag)` | Adiciona uma tag ao barramento. |
| `Bus.Keep(number)` | Quantos de um item o barramento mantém no lugar. |
| `Bus.Max(number)` | O máximo de um item que o barramento move de uma vez. |
| `Bus.Priority(number)` | Qual barramento vai primeiro. |
| `Bus.Fuzzy(yes or no)` | O ajuste FUZZY do barramento. |
| `Bus.WhenStock(item, number)` | Uma condição sobre o estoque de um item, explicada em [](jsc:bus_conditions). |
| `Bus.WhenStockTag(tag, number)` | A mesma condição, sobre o estoque de uma tag. |
| `Bus.Between(from, to)` | As horas do dia em que o barramento trabalha, de 0 a 23. |
| `Bus.After(bus)` | Nomeia outro barramento depois do qual este barramento vem, como uma condição. |
| `Bus.Item(name)` | Os ajustes de um item deste barramento, como um BusItem. |

Cada ajuste devolve o barramento, então os ajustes podem ser encadeados. Por exemplo, bus("Ore in").Keep(16).Max(64) mantém 16 do item e move no máximo 64 de uma vez.

Only e AllBut recebem os itens separados por vírgulas. Itens em branco são descartados. Between recebe horas de 0 a 23.

Um ajuste passa pela própria linguagem da rede. Quando o barramento não consegue aceitá-lo, o programa para com a resposta do motor. Cada ajuste é marcado com o programa que o fez: a classe de onde o programa parte, ou Sigma quando ele não tem uma.

## Um BusItem

Um BusItem é um item de um barramento. Os seus campos são BusItem.Bus e BusItem.Item, os nomes do barramento e do item. Os seus ajustes são BusItem.Keep e BusItem.Max, e funcionam como os mesmos ajustes do barramento.

## Se algo der errado

> [!PROBLEM] O programa para e diz que não há barramento com esse nome nesta rede.
> Confira o nome na janela do barramento. Bus.Named devolve null quando o barramento não está na rede.

> [!PROBLEM] O programa para e diz que o barramento não pode ser definido assim.
> A era do barramento ou o seu tipo não tem esse ajuste. Escolha outro ajuste, ou use outro barramento.

> [!PROBLEM] O programa para e diz que o item não está no filtro do barramento.
> Adicione o item ao filtro do barramento, ou defina o barramento para aceitar todo item.

> [!PROBLEM] O programa para e diz que não há item ou fluido com esse nome.
> Confira o nome do item. Os nomes são os que a rede usa.

> [!PROBLEM] O programa para e diz que a hora não é uma hora do dia.
> Use horas de 0 a 23.

> [!SEE] jsc:buses, jsc:bus_conditions, jsc:bus_filters_and_tags
