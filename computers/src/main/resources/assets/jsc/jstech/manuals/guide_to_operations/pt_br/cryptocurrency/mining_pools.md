---
section: cryptocurrency
order: 116
icon: jsc:server
---
# Pools de Mineração e o Painel de Mineração

Minerando sozinho, você acha um bloco de vez em quando. Um Pool de Mineração junta os mineradores de uma rede que o escolhem e minera com todos eles juntos; quando acha um bloco, paga a cada minerador a sua parte. É uma renda constante em vez de um bloco de vez em quando.

> [!DEFINE] Pool de Mineração
> Um serviço num servidor de rack que minera com os seus mineradores juntos e reparte a recompensa de cada bloco pelo trabalho que cada um deu.

> [!DEFINE] Painel de Mineração
> Um programa de desktop que mostra os pools de uma rede: o hashrate, os mineradores e os blocos que acharam.

Instale o Pool de Mineração num servidor num rack da rede, a partir da Transition ([](jsc:network_services)). Um minerador o escolhe pelo nome, com miner --pool e o nome, ou com o botão de pool da janela de um ASIC.

Quando o pool acha um bloco, reparte a recompensa pelo trabalho que cada minerador deu desde o último bloco, o hashrate do minerador vezes o tempo que ele minerou, menos a taxa do pool. A taxa vai para o endereço do dono; o dono a define de 0 a 5%, e ela começa em 1%.

Table: As abas do Painel de Mineração

| Aba | O que mostra |
| --- | --- |
| Pool | O hashrate do pool, quanto ele paga por hora, por dia e por semana, os blocos que achou e a dificuldade. |
| Mineradores | Cada minerador, o seu tipo, o endereço que paga, o seu hashrate e a sua parte. |
| Blocos | Os blocos que o pool achou, e quem achou cada um. |
| Configurações | Só no próprio servidor do pool: o nome, a taxa, o endereço do dono, quem pode entrar e os mineradores que pedem para entrar. |

## Se algo der errado

> [!PROBLEM] Um minerador diz que o seu pool não está na rede.
> Confira o nome do pool no Painel de Mineração e dê-o de novo com miner --pool.

> [!PROBLEM] Um minerador diz que espera o dono.
> O pool só aceita os mineradores que o dono aprova. O dono o aprova na aba Configurações, no próprio servidor do pool.

> [!SEE] jsc:mining, jsc:network_services
