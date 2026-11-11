---
section: hardware
order: 26
icon: jsc:tower_cooler_standard
covers_family: jsc:cooling
---
# Refrigeração e temperatura

Cada peça que você põe num computador transforma em calor os watts que consome, e o computador precisa tirar esse calor. O que ele consegue tirar é o seu orçamento de calor: o que o gabinete tira por conta própria, mais o que os coolers sobre os processadores e as ventoinhas do gabinete acrescentam.

> [!DEFINE] Orçamento de calor
> Os watts de calor que um computador consegue tirar de si mesmo.

Table: O que cada gabinete tira

| Gabinete | Tira | Encaixes de ventoinha |
| --- | --- | --- |
| Minitorre AT Vintage | 60 W | 1 na frente |
| Torre ATX Legacy | 100 W | 1 atrás, 1 na frente |
| Torre média Transition | 150 W | 1 atrás, 2 na frente |
| Neutral, Standard e Advanced | 200 W | 1 atrás, 2 na frente |
| High Performance, Standard e Advanced | 300 W | 1 atrás, 2 no topo, 3 na frente |
| Aesthetic, Standard e Advanced | 150 W | 1 atrás, 1 no topo, 2 na frente |

Table: O que cada peça de refrigeração tira

| Peça | Vintage | Legacy | Transition | Standard | Advanced |
| --- | --- | --- | --- | --- | --- |
| Cooler de fábrica | 20 W | 60 W | 100 W | 150 W | 200 W |
| Cooler torre | nenhum | nenhum | nenhum | 250 W | 300 W |
| MF AquaLoop 240 ou 360 | nenhum | nenhum | nenhum | 300 W | 400 W |
| Ventoinha de gabinete | 20 W | 30 W | 40 W | 50 W | 60 W |

Você os encaixa na face REFRIGERAÇÃO da janela de montagem: abra-a pela aba embaixo das lâmpadas. Cada processador tem um encaixe de cooler sobre o soquete, e os encaixes de ventoinha do gabinete ficam onde o gabinete os tem, atrás, no topo e na frente. A lâmpada da aba acende enquanto falta refrigeração ao computador para o que ele leva. Um cooler ou uma ventoinha da era do gabinete ou de uma mais antiga entra; um de uma era posterior, não.

Um water cooler, o MF AquaLoop, ocupa o encaixe de cooler do seu processador e dois encaixes de ventoinha do gabinete para o radiador: os dois do topo quando o gabinete os tem, senão os dois da frente. O de 360 mm também ocupa dois. O radiador traz as suas próprias ventoinhas.

A temperatura é a do ambiente mais 60 °C vezes o calor dividido pelo orçamento de calor: com o ambiente a 20 °C e o calor igual ao orçamento, o computador fica a 80 °C. O ambiente vem do bioma, cerca de 20 °C nas planícies, 38 °C no deserto e 8 °C na neve, e o Nether soma 10 °C.

Table: O que a temperatura faz

| Temperatura | O que acontece |
| --- | --- |
| Até 80 °C | Velocidade total |
| De 80 °C a 90 °C | A velocidade cai aos poucos, até 85% a 90 °C |
| 90 °C | Throttling: ele desacelera só o bastante para ficar em 90 °C, até 25% da sua velocidade |
| 100 °C | Desligamento térmico: quando nem 25% segura o calor, ele desliga na hora |

Desacelerar corta a capacidade do computador e os créditos dos seus programas, e gasta a fonte duas vezes mais rápido ([](jsc:power_supplies)). Um desligamento térmico é um desligamento súbito: os discos são verificados na próxima partida, e o computador não liga de novo até esfriar abaixo de 60 °C. O autoteste do firmware então avisa do desligamento, e o boot segue.

A temperatura leva cerca de meio minuto para chegar quase toda aonde uma mudança a leva. Você a lê na janela de montagem, na página de hardware do firmware, em Refrigeração no Gerenciador de Dispositivos e no bloco TEMP da aba Local do terminal.

O gabinete de um Mainframe tira 1.500 W por conta própria, e um cooler sobre cada um dos seus soquetes acrescenta a isso. Um servidor num rack não tem cooler próprio: o rack o refrigera ([](jsc:rack_cooling_and_heat)).

## Se algo der errado

> [!PROBLEM] O computador está mais lento do que deveria, e marca 90 °C.
> Ele produz mais calor do que tira. Encaixe um cooler melhor ou mais ventoinhas de gabinete, ou tire uma peça.

> [!PROBLEM] O computador apagou e não liga.
> Ele teve um desligamento térmico a 100 °C. Espere até esfriar abaixo de 60 °C, e refrigere-o melhor antes de ligá-lo de novo.

> [!PROBLEM] Um cooler ou uma ventoinha não entra.
> É de uma era posterior à do gabinete, o encaixe está embaixo do radiador de um water cooler, ou um water cooler não acha dois encaixes livres para o radiador.

> [!SEE] jsc:power_supplies, jsc:rack_cooling_and_heat
