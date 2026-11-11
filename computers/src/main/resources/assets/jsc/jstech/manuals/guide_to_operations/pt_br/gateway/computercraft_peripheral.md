---
section: gateway
order: 168
---
# O periférico jsc_gateway

Um programa do ComputerCraft chega ao Gateway pelo seu periférico, que tem o tipo jsc_gateway. O programa envolve o periférico com peripheral.find("jsc_gateway") e chama os seus métodos. Cada chamada conta para o limite de chamadas ([](jsc:gateway_permissions)).

Os nomes nas chamadas são os nomes que o jogo usa. Um item é um id como iron_ingot, e o namespace minecraft: pode ser omitido para itens do jogo base. Um fluido é escrito como fluid/ seguido do seu id, e uma substância química como chemical/ seguido do seu id.

## Métodos de leitura

Table: Métodos de leitura, que precisam da opção de leitura ligada

| Método | O que faz |
| --- | --- |
| `capacity()` | Devolve um número: a capacidade da rede. |
| `used()` | Devolve um número: quanto da capacidade está em uso. |
| `types()` | Devolve uma tabela com cada nome e o seu total. |
| `total(name)` | Devolve um número: quanto daquele nome a rede guarda. |
| `find(name)` | Devolve uma lista de entradas, cada uma com um servidor e a quantidade que ele guarda. |
| `servers()` | Devolve uma lista de servidores, cada um com seu nome, uso e capacidade, e se está online. |
| `computers()` | Devolve uma lista dos computadores da rede, cada um com seu nome, rótulo, sistema operacional, tipo e estado online. |
| `operation(id)` | Devolve a tabela de uma operação, ou nil quando não há nenhuma. |
| `operations()` | Devolve uma lista das operações, as que estão rodando e as concluídas na última hora. |
| `watch(name)` | Retorna o total agora, e observa o nome, para que um evento jsc_stock seja disparado quando ele mudar. |
| `unwatch(name)` | Devolve true ou false: true quando o observador termina. |

## Métodos de operação

Table: Métodos de operação, que precisam da opção de operações ligada

| Método | O que faz |
| --- | --- |
| `pull(name, quantity, priority)` | Inicia um pull que traz itens para o buffer, e devolve o id da operação. |
| `push(name, quantity, priority)` | Inicia um push de itens do buffer para a rede, e devolve o id. |
| `craft(name, quantity, priority)` | Inicia uma fabricação do item, e devolve o id. |
| `cancel(id)` | Interrompe uma operação, e devolve true ou false. |
| `run(computer, program, args...)` | Inicia um programa em outro computador, e devolve o seu número de processo. |

O argumento de prioridade é opcional. A quantidade 0 significa todas ([](jsc:gateway_permissions)).

Uma operação tem um id longo. Você pode dar só as primeiras letras, desde que nenhuma outra operação comece com as mesmas letras.

## Métodos de estado e de log

Table: Métodos de estado e de log

| Método | O que faz |
| --- | --- |
| `online()` | Devolve true quando o Gateway está ligado a um host. |
| `name()` | Devolve o nome do Gateway. |
| `host()` | Devolve o nome do computador host, ou um texto vazio enquanto não está ligado. |
| `send(text)` | Envia uma linha ao host, e devolve true ou false. |
| `log(level, text)` | Grava uma linha no log do Gateway. O nível é info, warn ou error. |

Uma linha gravada no log é cortada em 96 caracteres, e o log guarda 40 entradas.

## Se algo der errado

> [!PROBLEM] Um fluido ou uma substância química puxados inicia uma operação, mas nada chega ao buffer.
> O buffer aceita só itens. Use pull com um item.

> [!PROBLEM] Um push cita um fluido ou uma substância química, e o texto diz "o buffer não contém fluid/<id>" ou "o buffer não contém chemical/<id>".
> O push tira itens apenas do buffer. Informe um nome de item no push.

> [!PROBLEM] O texto diz "não existe o método <name> em <peripheral>".
> O nome do método não está entre os da tabela desta página. Confira a grafia.

> [!PROBLEM] O texto diz "mais de uma operação começa com" as letras que você deu.
> Dê mais do id, até que só uma operação comece com ele.

> [!SEE] jsc:operations_catalogue
