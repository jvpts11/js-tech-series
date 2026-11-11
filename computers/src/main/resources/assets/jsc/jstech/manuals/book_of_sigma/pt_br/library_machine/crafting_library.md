---
section: library_machine
order: 451
---
# Interfaces e roteadores de fabricação a partir de um programa

Uma interface de fabricação guarda padrões e pede fabricações à rede. Um roteador de fabricação, no mesmo cabo de fabricação, decide quais entradas ele carrega, pelo seu filtro. As máquinas em si são explicadas em [](jsc:crafting_interfaces) e [](jsc:pattern_encoders).

Table: Membros de CraftInterface

| Membro | O que faz |
| --- | --- |
| `CraftInterface.Named(name)` | A interface com esse nome, ou null quando não existe nenhuma. |
| `CraftInterface.Name` | O nome da interface. |
| `CraftInterface.Exclusive(yes or no)` | A configuração de exclusividade da interface. |
| `CraftInterface.MaxJobs(number)` | O maior número de tarefas que a interface aceita. |
| `CraftInterface.Pause()` | Pausa a interface. |
| `CraftInterface.Resume()` | Retoma a interface. |
| `CraftInterface.Route(pattern, input, router)` | Envia uma entrada de um padrão por um roteador do próprio cabo da interface. Um roteador null encaminha essa entrada pelos filtros dos roteadores. |

Table: Membros de CraftRouter

| Membro | O que faz |
| --- | --- |
| `CraftRouter.Named(name)` | O roteador com esse nome, ou null quando não existe nenhum. |
| `CraftRouter.Name` | O nome do roteador. |
| `CraftRouter.Only(items)` | O roteador aceita apenas os itens listados. |
| `CraftRouter.AllBut(items)` | O roteador aceita todos os itens, exceto os listados. |
| `CraftRouter.Tag(tag)` | Adiciona uma tag ao roteador. |
| `CraftRouter.Any()` | Limpa o filtro, para que o roteador aceite qualquer coisa. |
| `CraftRouter.Fuzzy(yes or no)` | A configuração fuzzy do roteador. |

A interface e o roteador são encontrados na rede de fabricação da máquina. O nome pelo qual o seu programa os encontra é o nome que as janelas deles mostram, e uma janela pode renomeá-los.

Cada configuração passa pelo mesmo Motor das configurações de Barramento. Uma configuração recusada interrompe o programa com o texto do Motor. Cada configuração é registrada sob o nome da classe do programa, ou sob Sigma quando o programa não tem nome.

## Se algo der errado

> [!PROBLEM] O programa para e diz que não há nenhuma Interface de Fabricação com esse nome nesta rede.
> Confira o nome na janela da interface.

> [!PROBLEM] O programa para e diz que não há nenhum Roteador de Entrada de Fabricação com esse nome nesta rede.
> Confira o nome na janela do roteador.

> [!PROBLEM] O programa para e diz que a interface não tem nenhum padrão com esse nome.
> Confira o nome do padrão na janela da interface.

> [!PROBLEM] O programa para e diz que a entrada não é uma entrada do padrão.
> Use uma das entradas que o padrão tem.

> [!PROBLEM] O programa para e diz que não há nenhum roteador com esse nome no cabo da interface.
> Use um roteador no mesmo cabo da interface.

> [!SEE] jsc:crafting_interfaces, jsc:pattern_encoders, jsc:asking_for_a_craft
