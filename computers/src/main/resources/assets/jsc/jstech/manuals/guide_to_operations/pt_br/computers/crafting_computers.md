---
section: computers
order: 51
icon: jsc:crafting_computer
covers_family: jsc:crafting_computers
---
# Computadores de Fabricação

O Computador de Fabricação fabrica para a rede. Ele roda as receitas de bancada sozinho, e comanda as máquinas que fazem o resto por meio de [Interfaces de Fabricação](jsc:crafting_interfaces).

Ele só fabrica com uma Placa de Fabricação dentro: sem uma, ele não fabrica nada. A placa define quantas Interfaces de Fabricação ele comanda, e guarda os [padrões](jsc:patterns) de bancada na própria memória, então eles se mudam com a placa ([](jsc:crafting_cards)).

Monte-o como um Computador Pessoal ([](jsc:personal_computers)). Ele aceita os mesmos tipos de peça, e tem estes:

```table
{"caption": "O que um Computador de Fabricação tem", "rows": [[{"words": "Placa-mãe"}, {"literal": "1"}], [{"words": "Processadores"}, {"literal": "1"}], [{"words": "Memória"}, {"literal": "4"}], [{"words": "Placas PCI Express"}, {"literal": "4"}], [{"words": "Discos"}, {"literal": "2"}], [{"words": "Fonte de alimentação"}, {"literal": "1"}], [{"words": "Baias de unidades"}, {"literal": "2"}]]}
```

A era da Placa de Fabricação define quantas Interfaces de Fabricação ele comanda, e quantos padrões a memória dele guarda:

Table: Placas de Fabricação por era

| Era | Interfaces de Fabricação | Padrões na placa |
| --- | --- | --- |
| Vintage | 2 | 2 |
| Legacy | 4 | 4 |
| Transition | 5 | 5 |
| Standard | 6 | 6 |
| Advanced | 8 | 8 |

Abra o Gerenciador de Fabricação nele para carregar os padrões e acompanhar as tarefas ([](jsc:crafting_manager)).

## Se algo der errado

> [!PROBLEM] Ele não fabrica nada.
> Ele não tem uma Placa de Fabricação, ou ela está desligada. Coloque uma placa nele e ligue-o.

> [!PROBLEM] Um padrão de bancada sumiu.
> Ela fica na placa. Foi junto com a placa para outro computador, então coloque a placa de volta.

> [!SEE] jsc:autocrafting
