---
section: iql_settings
order: 376
---
# SET INTERFACE, SET ROUTER, RENAME, PAUSE e RESUME

Uma Interface de Fabricação guarda os padrões e pede à rede as fabricações. Um Roteador de Entrada de Fabricação envia as entradas de um padrão para um roteador que você escolhe. O roteador precisa estar no mesmo cabo ([](jsc:crafting_interfaces)).

Estas instruções alteram uma interface ou um roteador, ou os nomes deles. Quando um nome tem espaços, escreva-o entre aspas.

Table: As instruções para interfaces e roteadores

| Instrução | O que faz |
| --- | --- |
| `SET INTERFACE 'x' EXCLUSIVE ON` | A interface fabrica uma receita por vez. |
| `SET INTERFACE 'x' EXCLUSIVE OFF` | A interface pode executar várias receitas. |
| `SET INTERFACE 'x' MAX JOBS 4` | No máximo 4 fabricações por vez. 0 significa quantas vierem. |
| `SET INTERFACE 'x' MAX JOBS AUTO` | O mesmo que 0. |
| `SET INTERFACE 'x' ROUTE 'pattern' INPUT item TO ROUTER 'router'` | Envia essa entrada do padrão para o roteador que você indicar. |
| `SET INTERFACE 'x' ROUTE 'pattern' INPUT item AUTO` | Envia essa entrada sem indicar um roteador. |
| `SET ROUTER 'x' setting` | Aplica qualquer ajuste de SET BUS ([](jsc:set_bus)) a um Roteador de Entrada de Fabricação. |
| `PAUSE INTERFACE 'x'` | Pausa a interface. |
| `RESUME INTERFACE 'x'` | Retoma a interface pausada. |
| `RENAME INTERFACE 'x' TO 'new name'` | Dá um novo nome à interface. RENAME ROUTER faz o mesmo para um roteador. |

Table: As respostas

| Resposta | O que significa |
| --- | --- |
| `<name> set` | A alteração foi salva. |
| `<old> is now called <new>` | A renomeação funcionou. |
| `no Crafting Interface named <name> on this network` | Nenhuma interface com esse nome está na rede. |
| `no Crafting Input Router named <name> on this network` | Nenhum roteador com esse nome está na rede. |
| `the interface <name> holds no pattern called <pattern>` | A interface não tem nenhum padrão com esse nome. |
| `<input> is not an input of <pattern>` | Indique uma das entradas do padrão. |
| `no router named <router> on the cable of the interface <name>` | O roteador não está no cabo dessa interface. |

## Se algo der errado

> [!PROBLEM] SET INTERFACE precisa do nome da interface, entre aspas quando ela tem espaços
> Digite o nome da interface depois de INTERFACE.

> [!PROBLEM] SET ROUTER precisa do nome do roteador, entre aspas quando ele tem espaços
> Digite o nome do roteador depois de ROUTER.

> [!PROBLEM] SET INTERFACE precisa de um ajuste: EXCLUSIVE, MAX JOBS ou ROUTE
> Adicione um desses três ajustes depois do nome.

> [!PROBLEM] uma interface não tem o ajuste <word>
> Use EXCLUSIVE, MAX JOBS ou ROUTE.

> [!PROBLEM] RENAME precisa de TO e do novo nome
> Escreva TO e o novo nome depois do nome antigo.

> [!PROBLEM] esperava o nome do padrão
> Digite o nome do padrão depois de ROUTE.

> [!PROBLEM] esperava uma das entradas do padrão
> Digite o item da entrada depois de INPUT, ou AUTO.

> [!PROBLEM] esperava o nome do roteador
> Digite o nome do roteador depois de TO ROUTER, ou escreva AUTO.

> [!SEE] jsc:crafting_interfaces, jsc:pattern_encoders, jsc:set_bus
