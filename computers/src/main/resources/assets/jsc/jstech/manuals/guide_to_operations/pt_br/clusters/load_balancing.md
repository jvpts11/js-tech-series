---
section: clusters
order: 66
---
# Balanceamento de carga

O balanceamento de carga decide para onde vai um item novo quando vários servidores poderiam recebê-lo. Cada seção de um Roteador de Servidores tem seu próprio modo de balanceamento, e o botão do roteador passa uma seção para o modo seguinte.

> [!DEFINE] Balanceamento de carga
> Distribuir os itens novos entre os servidores, para que nenhum servidor encha primeiro.

Table: Modos de balanceamento

| Modo | Rótulo na tela | O que um item novo faz |
| --- | --- | --- |
| Rodízio | ROUND-ROBIN | O próximo servidor na vez. O ponto de partida gira a cada face. |
| Menos carregado | LEAST-LOADED | Ordena os servidores por espaço livre, o que tiver mais espaço livre primeiro, e enche nessa ordem. |
| Manual | MANUAL | Enche os servidores na ordem da lista. |

Toda seção começa em ROUND-ROBIN, e você vê o modo de cada seção no roteador. O roteador guarda sua posição por face, então em ROUND-ROBIN cada nova gravação vai para o próximo servidor na vez.

Um servidor também pode executar o serviço Balanceador de Carga: um programa que espalha as gravações pelas unidades da baia, em vez de enchê-las uma após a outra ([](jsc:programs)).

## Se algo der errado

> [!PROBLEM] Todos os itens vão para um só servidor.
> Se a seção estiver em MANUAL, ela enche o primeiro servidor da lista. Pressione o botão até que mostre ROUND-ROBIN ou LEAST-LOADED.

> [!PROBLEM] As unidades da baia enchem uma após a outra.
> Execute o serviço Balanceador de Carga nesse servidor.

> [!SEE] jsc:server_router, jsc:programs
