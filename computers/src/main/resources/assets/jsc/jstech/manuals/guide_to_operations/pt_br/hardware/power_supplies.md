---
section: hardware
order: 24
icon: jsc:psu_200
covers_family: jsc:power_supplies
---
# Fontes de alimentação

A maioria das peças consome energia, e a fonte de alimentação a fornece. Os processadores, a memória, as placas de vídeo, os discos e as placas de expansão consomem watts cada um. A fonte precisa fornecer pelo menos o total deles.

Há doze fontes, de 200 W a 3.000 W, cada uma com a sua própria eficiência.

> [!DEFINE] Eficiência
> A parcela da energia que a fonte puxa da linha e que chega às peças: 300 W de peças numa fonte de 80% puxam 375 W da linha. A verificação da montagem não usa esse valor.

Table: As fontes de alimentação

| Fonte de alimentação | Era | Potência | Eficiência |
| --- | --- | --- | --- |
| MF PowerBasic 200 | Vintage | 200 W | 80% |
| MF PowerBasic 300 | Vintage | 300 W | 80% |
| MF PowerBasic 350 | Legacy | 350 W | 80% |
| MF PowerBasic 500B | Legacy | 500 W | 80% |
| MF PowerBasic 450B | Transition | 450 W | 85% |
| MF PowerGold 650G | Standard | 650 W | 90% |
| MF PowerGold 850G | Standard | 850 W | 90% |
| MF PowerGold 1000G | Advanced | 1.000 W | 90% |
| MF PowerPlat 1200P | Advanced | 1.200 W | 92% |
| MF PowerPlat 1600P | Advanced | 1.600 W | 92% |
| MF ServerPSU 2000P | Advanced | 2.000 W | 94% |
| MF ServerPSU 3000P (Redundante) | Advanced | 3.000 W | 94% |

A montagem soma o consumo dos processadores, da memória, dos discos e das placas de expansão. Se a soma for maior do que a fonte fornece, a montagem falha com PSU insufficient ([Quando a montagem é recusada](jsc:build_refusals)).

A tela de montagem de todo computador tem um indicador DRAW: o consumo contra os watts da fonte, âmbar perto do limite e vermelho além dele, para você ver a soma enquanto escolhe. A dica dele dá os watts que a fonte puxa da linha.

Um computador cuja fonte fornece menos do que as peças consomem não liga. Ele permanece desligado, e um servidor diz o motivo.

Um computador ligado puxa sua energia da rede elétrica através da fonte: os watts das peças sobre a eficiência da fonte, como J's Energy na classe de baixa tensão ou como FE, de qualquer cabo que traga energia. Sem energia ele não liga. Quando a energia para de chegar, ele se apaga na hora, e o log de eventos da rede registra isso. Um servidor pode desligar o consumo.

Uma fonte se desgasta enquanto está ligada, e por fim falha. Quanto mais perto do seu limite trabalha, mais depressa se desgasta: até metade do limite ela vive oito vezes mais do que em carga total, até quatro quintos três vezes. Em carga total uma PowerBasic dura 60 dias de jogo, uma PowerGold 100, uma PowerPlat 150 e uma ServerPSU 250, e um rack quente demais corta isso pela metade. O desgaste aparece numa barra embaixo da fonte, como a durabilidade de um item. A partir de 80% de desgaste a fonte fica degradada, e você é avisado a cada boot; a 100% ela falha, e o computador se apaga até você pôr uma nova. A 3000P tem duas unidades: uma pode falhar, e a outra aguenta a carga.

Um computador que perde a energia em vez de ser desligado desliga de repente. Uma linha que ainda traz alguma coisa, mas menos do que o computador consome, é energia instável: o sistema cai primeiro na sua página vermelha, e uma fonte que falha mostra a dela. O que as Operations do Mainframe carregavam se perde, e o log diz o quê. Na próxima vez que o sistema ligar, ele verifica os discos que estavam em uso: dois segundos com um journal, até um minuto sem, e nada no MC-DOS. Desligue os seus computadores pelo botão e você não perde nada.

## Se algo der errado

> [!PROBLEM] O computador não inicia, ou a compilação diz "PSU insuficiente".
> As peças consomem mais do que a fonte fornece. Use uma fonte com mais watts.

> [!PROBLEM] O computador não inicia, e o indicador DRAW diz SEM ENERGIA.
> Nenhuma energia chega a ele. Leve um cabo até ele a partir de um gerador ou de uma bateria.

> [!PROBLEM] O computador se apagou, e o botão de energia diz "PSU failure".
> A fonte se desgastou. Ponha uma fonte nova, e use-a mais abaixo do seu limite.

> [!PROBLEM] O computador cai numa página vermelha sobre energia instável.
> A linha dá a ele menos do que ele consome. Acrescente um gerador ou uma bateria, ou tire máquinas da linha.

> [!PROBLEM] O computador liga com uma longa verificação do disco.
> Ele apagou de repente da última vez. Deixe terminar: a verificação não apaga nada.

> [!SEE] jsc:build_refusals, jsc:disks, jsc:processors
