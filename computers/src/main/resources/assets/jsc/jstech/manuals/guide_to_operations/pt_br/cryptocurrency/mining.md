---
section: cryptocurrency
order: 115
icon: jsc:mining_computer_standard
covers: [jsc:mining_computer_transition, jsc:mining_computer_standard, jsc:mining_computer_advanced, jsc:motherboard_mng_transition, jsc:motherboard_mng_standard, jsc:motherboard_mng_advanced, jsc:mining_riser_transition, jsc:mining_riser_standard, jsc:mining_riser_advanced, jsc:asic_forge_s1, jsc:asic_forge_a2]
---
# Mineração: o minerador, o Computador de Mineração e os ASICs

Minerar é como os TRC são feitos. Um minerador gasta o seu hashrate procurando o próximo bloco, e a recompensa do bloco vai para o endereço de pagamento do minerador: o endereço da sua carteira, da aba Receber. Minerar consome os watts das peças que mineram, e esse é o seu custo de verdade.

> [!DEFINE] Hashrate
> Quantos hashes por segundo um minerador tenta. Um processador minera na sua capacidade, uma placa de vídeo a dez vezes a sua potência.

> [!DEFINE] Minerador Blockwright
> O programa que minera num computador, no prompt ou em segundo plano.

## O minerador no prompt

Instale o Minerador Blockwright e dê a ele o seu endereço: miner --address e o endereço. Digite miner para vê-lo minerar, e Q para parar. miner start minera em segundo plano, sempre que o computador está ligado, e miner stop encerra; miner status diz como vai. miner --pool e o nome de um pool minera para esse pool, e miner --alone só para você.

```command
id: jsc:command/mc_net/miner
```

## O Computador de Mineração

O Computador de Mineração é um rig de mineração: uma estrutura aberta que segura uma placa de mineração, um processador, memória, uma fonte, um disco e placas de vídeo em risers, tudo à vista. O da Transition ocupa um bloco e leva 4 placas; o da Standard e o da Advanced têm dois blocos de largura e levam 6 e 8. É um computador de verdade, sem precisar de monitor: você o alcança por ssh ([](jsc:remote_access)) ou o acompanha no Painel de Mineração.

1. Coloque o Computador de Mineração e abra-o.
2. Ponha a placa de mineração da sua era, um processador, memória, uma fonte, um disco e um riser para cada placa que ele vai levar.
3. Desligue-o. Com uma placa de vídeo na mão, mire num riser livre e use-o para encaixar a placa. Agache com a mão vazia para tirar uma placa.
4. Instale um sistema e o minerador, defina o endereço de pagamento e ligue Minerar ao ligar para ele minerar sempre que estiver ligado.

Um riser leva as placas da sua era ou mais antigas. Num riser uma placa minera com toda a sua potência: a mineração ignora o corte do slot.

## Os ASICs

Um ASIC é um bloco feito só para minerar, pela Blockwright. Ele não tem sistema: a sua janela define o endereço de pagamento e o pool. Dê energia a ele e ponha-o na rede com um cabo de dados; ele minera enquanto tiver os seus watts.

Table: O que minera quanto

| Minerador | Hashrate | Consome |
| --- | --- | --- |
| Um rig Transition de quatro placas | cerca de 13.600 H/s | os watts das suas peças |
| Um rig Standard de seis placas | cerca de 157.000 H/s | os watts das suas peças |
| Um rig Advanced de oito placas | cerca de 467.000 H/s | os watts das suas peças |
| Forge S1 (Standard) | 1.000.000 H/s | 1.400 W |
| Forge A2 (Advanced) | 10.000.000 H/s | 3.200 W |

## Se algo der errado

> [!PROBLEM] Uma placa não encaixa num riser.
> Desligue o rig, mire num riser livre e confira se a placa é da era do riser ou mais antiga.

> [!PROBLEM] O minerador diz que não tem endereço para pagar.
> Dê um a ele: miner --address e o endereço da sua carteira, ou o endereço na janela do ASIC.

> [!PROBLEM] O minerador diz que nada na máquina faz hashes.
> Ponha um processador, ou encaixe placas de vídeo nos risers do rig.

> [!SEE] jsc:teracoin, jsc:mining_pools
