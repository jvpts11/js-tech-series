---
section: clusters
order: 65
icon: jsc:server_router
covers_family: jsc:server_router
---
# Roteador de Servidores

O Roteador de Servidores é como você organiza um datacenter. Ele agrupa os Racks de Servidores em seções, uma para cada uma das suas faces além do uplink ([](jsc:datacenters)). Cada era, do Legacy em diante, tem o seu, e as suas faces aceitam os cabos de dados da sua era e de todas as anteriores ([](jsc:data_cables)).

1. Ligue a sua face de uplink à rede.
2. Ligue uma seção de racks a cada uma das suas outras faces.
3. Abra-o para ver cada seção e a sua contagem de racks.

Um Roteador de Servidores gerencia um número de Racks de Servidores no total, entre as seções, que a sua era define: 4 no Legacy, 8 no Transition, 16 no Standard e 32 no Advanced. A sua janela mostra a era no canto e a contagem de racks contra esse orçamento. Os racks além dele são contados como não gerenciados, e o roteador mostra a contagem de racks em vermelho.

O Gerenciador de Cluster executa cada seção como uma única máquina, na medida em que o seu cartão alcança ([](jsc:cluster_management_computers)).

## Se algo der errado

> [!PROBLEM] A contagem de racks aparece em vermelho.
> Há mais racks no roteador do que a sua era gerencia. Mova alguns para um segundo Roteador de Servidores, que tem orçamento próprio, ou use um roteador de uma era mais nova.

> [!SEE] jsc:datacenters, jsc:load_balancing
