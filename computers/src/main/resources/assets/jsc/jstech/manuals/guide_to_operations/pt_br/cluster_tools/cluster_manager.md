---
section: cluster_tools
order: 155
icon: jsc:cluster_management_computer
---
# O Gerenciador de Cluster

O Gerenciador de Cluster é a janela que permite rodar muitas máquinas como se fossem uma só. É um programa de área de trabalho para um Computador de Gerenciamento de Cluster ([](jsc:cluster_management_computers)). Ele precisa de uma Placa de Interface de Cluster nesse computador ([](jsc:expansion_cards)).

Cada máquina em um cluster é um nó. Quais clusters você alcança depende do alcance do cartão.

> [!DEFINE] Cluster
> Um grupo de máquinas que o Gerenciador de Cluster administra como uma unidade só: um supercomputador, ou uma seção de um datacenter.

> [!DEFINE] Nó
> Uma máquina de um cluster, que o Gerenciador de Cluster pode ligar e na qual pode instalar um sistema ou um programa.

> [!DEFINE] Alcance
> O que uma Placa de Interface de Cluster pode controlar: os servidores de datacenter, os supercomputadores e os clusters de inteligência artificial (IA), dependendo da placa.

A janela roda em Frames, Linux, FreeBSD e UNIX System V. Sem placa, ela diz "É necessária uma Placa de Interface de Cluster."

Table: As partes da janela

| Parte | O que faz |
| --- | --- |
| Abas superiores | Supercomputadores, Datacenters e IA. Cada aba lista os clusters do seu tipo. |
| Abas de supercomputador | NODES, MAPA DO CLUSTER e QUEUE. |
| Abas de datacenter | SERVERS, INVENTORY e DIAGNOSTICS. |
| Botões do rodapé | SISTEMA EM TODOS, PROGRAMA EM TODOS, LIGAR TUDO ou DESLIGAR TUDO, e CANCELAR JOB, explicados abaixo. |

Table: Os botões do rodapé

| Botão | O que faz |
| --- | --- |
| SISTEMA EM TODOS | Instala um sistema em cada nó do cluster, a partir de um disco em uma unidade vinculada ([](jsc:cluster_installs)). |
| PROGRAMA EM TODOS | Instala um programa em cada nó, a partir de um disco de programa em uma unidade vinculada. |
| LIGAR TUDO ou DESLIGAR TUDO | Liga ou desliga todos os nós do cluster. Não instala nada. |
| CANCELAR JOB | Interrompe a instalação em andamento depois dos nós que estão sendo gravados. |

Cada linha de nó tem seus próprios botões. SYSTEM e PROGRAM instalam só naquele nó. LIGAR e DESLIGAR só ligam ou desligam o nó.

## Seções de datacenter

Uma seção de datacenter é o conjunto de servidores atrás de uma face de um Roteador de Servidores ([](jsc:server_router)). O botão BALANCE alterna o modo de balanceamento de carga do roteador, de ROUND-ROBIN para LEAST-LOADED, depois MANUAL, e volta ao início. Os modos são explicados em ([](jsc:load_balancing)).

Abra INVENTORY para ver o que a seção guarda. A dica do inventário diz que você clica em um item para tirá-lo, e solta uma pilha na janela para depositá-la.

## Nomes

Pressione RENAME e digite um nome. Um nome vazio volta ao padrão. Os supercomputadores são nomeados SC-1, SC-2 e assim por diante, na ordem em que a rede os registrou. Uma seção recebe o nome do seu roteador e a face em que ela fica, como Router · NORTH.

## Estados dos nós

Cada linha de nó mostra o seu estado em maiúsculas. SEM SISTEMA significa que o nó não tem sistema instalado. COMPARTIMENTO DESLIGADO significa que o nó está desligado. INSTALLING significa que um sistema ou um programa está sendo gravado nele agora.

## Diagnóstico

Abra DIAGNOSTICS para ver como uma seção está indo. No alto ficam três números do último minuto: a latência, os ticks de um pedido entrando no roteador até a resposta do seu servidor, ao lado da base da seção; a vazão, os itens por segundo que passam pela seção, ao lado do máximo que ela consegue mover; e quantos alertas estão ativos.

> [!DEFINE] Latência base
> Os ticks que os servidores de uma seção levam para responder quando nada mais está no caminho, pelos discos que eles têm.

Abaixo deles você vê a última hora, minuto a minuto: a latência em âmbar e a vazão em azul, cada uma na sua própria escala. Abaixo do gráfico ficam os alertas ativos agora, e há quanto tempo.

Table: Os alertas de uma seção

| Alerta | Nível | Quando ele aparece |
| --- | --- | --- |
| Discos quase cheios | WARN, depois ERROR | Os discos de um servidor estão 90% cheios, depois 98%. |
| Rack acima do seu orçamento térmico | WARN | Um rack faz mais calor do que dissipa, e diz o quanto suas máquinas estão sendo limitadas. |
| Roteador no limite | WARN | O roteador gerencia tantos racks quanto a sua era permite. |
| Latência acima do dobro da base | WARN | A latência do último minuto é mais que o dobro da base da seção. |

Todo alerta também vai para o log de eventos da rede quando aparece. Uma latência acima do dobro da base só fica ativa enquanto dura; depois o gráfico a guarda, e o pé da aba diz há quanto tempo foi e quanto durou.

## Se algo der errado

> [!PROBLEM] A janela diz "É necessária uma Placa de Interface de Cluster."
> Instale uma Placa de Interface de Cluster no computador ([](jsc:expansion_cards)).

> [!PROBLEM] A lista não mostra nenhum supercomputador e nenhuma seção.
> A placa pode ser antiga demais para alcançá-los. A tabela em ([](jsc:cluster_installs)) mostra o que cada placa alcança.

> [!PROBLEM] A janela diz "essa linha não é um nó que esta placa alcança".
> A placa não alcança essa máquina. Use uma placa mais nova para ela.

> [!PROBLEM] A janela diz "selecione um cluster primeiro".
> Selecione um cluster na janela e pressione o botão de novo.

> [!SEE] jsc:cluster_command, jsc:cluster_installs, jsc:supercomputers, jsc:datacenters, jsc:server_racks
