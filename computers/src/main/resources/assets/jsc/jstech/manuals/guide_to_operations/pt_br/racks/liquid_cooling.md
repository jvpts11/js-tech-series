---
section: racks
order: 60
icon: jsc:cooling_tower_fan
covers: [jsc:cooling_tower_basin, jsc:cooling_tower_louvre, jsc:cooling_tower_fill, jsc:cooling_tower_fan, jsc:cooling_tower_terminal, jsc:chiller_evaporator, jsc:chiller_condenser, jsc:chiller_compressor, jsc:chiller_terminal]
---
# Resfriamento líquido

Um Supercomputer Rack que esquenta mais do que o ar consegue tirar é resfriado com água. Dois líquidos circulam pelos canos: **Água Fria**, indo para o que ela resfria, e **Água Quente**, voltando. Um milibalde por segundo leva um quilowatt.

> [!DEFINE] Circuito
> Um anel de cano por onde a mesma água dá a volta: sai fria, volta quente, é resfriada de novo.

Um rack tira 1.000 W pelo ar, mais as suas Cooling Units e Vent Panels. Com água fria chegando nele, ele tira até **8.000 W a mais**, e um rack cheio de Supercomputer Nodes roda na velocidade máxima.

1. Monte um **Chiller** e uma **Torre de Resfriamento** com o kit da cadeia e as peças de cada um, como a página de cada máquina num visualizador de receitas mostra. Alimente cada um com energia pela sua Power Entry.
2. Ligue o **circuito dos racks**: a saída COLD na ponta da caixa de água do Chiller até a entrada no topo de cada rack (o bloco de cima atrás à direita, olhando a frente dele), e a saída de cada rack (o bloco de cima na frente à esquerda) de volta à entrada HOT do Chiller.
3. Ligue o **circuito da torre**: a saída HOT na parte de trás da caixa de água do Chiller até a entrada HOT da torre, do lado esquerdo dela; a saída COLD da torre, do lado direito, de volta à entrada COLD na frente da caixa de água do Chiller.
4. Ligue **água** comum na entrada FILL da torre: ela evapora um milibalde para cada dez que resfria.
5. Para encher os circuitos no começo, ligue água comum em qualquer entrada COLD: ela conta como Água Fria.

Table: O que cada máquina faz na carga máxima

| Máquina | Resfria | Gasta |
| --- | --- | --- |
| Supercomputer Rack | até 8 mB/s, +8.000 W no orçamento | água fria |
| Chiller | até 120 mB/s, 120 kW | 30 kW, e 150 mB/s da água fria da torre |
| Torre de Resfriamento | até 200 mB/s, 200 kW | até 2 kW, e 20 mB/s de água comum |

A janela do rack mostra uma **gota** antes do estado enquanto a água o resfria: azul quando ele recebe tudo o que pede, âmbar quando falta água. Passe o mouse sobre a gota para ver os watts que a água leva e a vazão.

## Se algo der errado

> [!PROBLEM] A gota está âmbar.
> O rack recebe menos água fria do que precisa. Veja se o Chiller está ligado e com energia, se a torre tem água na entrada FILL e se a saída do rack consegue escoar: uma saída cheia também para a água.

> [!PROBLEM] O Chiller está parado com água quente dentro.
> Ele não resfria nada sem a água fria da torre para levar o calor embora. Confira o circuito da torre e a água da FILL.

> [!SEE] jsc:rack_cooling_and_heat, jsc:server_racks
