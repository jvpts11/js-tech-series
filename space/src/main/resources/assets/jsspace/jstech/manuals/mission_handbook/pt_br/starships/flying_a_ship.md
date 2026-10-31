---
section: starships
order: 6
icon: jsspace:pilot_seat
---
# Pilotar uma nave

## O que é

Como uma nave pequena voa depois de sair da doca. Ela vai para onde o nariz aponta, sem derrapar, e o que funciona em terra funciona a bordo.

## Para que serve

Decolar na vertical, cruzar a terra, subir ao espaço sem tela de carregamento, acoplar e pousar numa plataforma ou numa pista.

## Como conseguir

Dê partida numa nave construída inteira num [estaleiro](jsspace:shipyard): ela é sua para pilotar.

## Como usar

1. Use o Assento do Piloto para sentar no comando. O HUD acende no vidro da cabine, e na tela inteira na vista de trás ([[F5]]).
2. A tecla de andar para a frente ([[W]]) abre o acelerador, que fica onde você deixar; a de trás ([[S]]) fecha. [[A]] e [[D]] viram a nave. Pular ([[Espaço]]) levanta o nariz, correr ([[Ctrl Esquerdo]]) abaixa.
3. Abaixo de 10 metros por segundo, os bocais dos motores de ar viram para baixo e a nave paira, de nariz nivelado: pular e correr a levam direto para cima e para baixo.
4. Mais rápido, as asas a sustentam. [[B]] dá impulso num trecho vazio, bem longe do chão ou no espaço.
5. Continue subindo: setecentos blocos acima do limite de construção do mundo, a nave entra no espaço do seu sistema.
6. [[J]] troca o piloto automático: POUSO, ÓRBITA, ACOPLA e desligado. Segurar qualquer tecla do comando devolve o comando a você.
7. Levante do assento com a tecla de agachar ([[Shift Esquerdo]]): o acelerador fecha, e a nave desce devagar e fica estacionada.

Table: O HUD

| Onde | O que mostra |
| --- | --- |
| Topo | O rumo |
| Meio | A escada de arfagem, e o marcador de trajetória no lugar da mira |
| Esquerda | A velocidade, `VEL M/S`, e a barra do impulso, âmbar enquanto impulsiona |
| Direita | A altitude, `ALT`, e a subida, `VV` |
| Acima | A energia do reator, `POT`, e o deutério, `D2` |
| Abaixo | O ângulo dos bocais, `BOCAIS`, e `TREM RECOLHIDO` ou `TREM BAIXADO`, âmbar quando baixado |
| Base | O piloto automático, `PA`: `POUSO`, `ÓRBITA`, `ACOPLA`, o ativo aceso |

O POUSO assenta a nave na plataforma mais próxima que você pode usar, a até 512 blocos, ou, com rodas, numa pista livre; o losango dele marca o ponto. A ÓRBITA abre o acelerador e sobe a 55 graus, dando impulso onde pode, e solta no espaço. O ACOPLA, no espaço, leva o colar de acoplagem até o da nave mais próxima a até 200 blocos, com a cruz dele no outro colar. Quando eles se tocam alinhados, as duas viram uma só; [[J]] solta a nave acoplada.

O deutério do reator dura quatro horas no ar. Sem ele, o impulso não enche mais; uma doca reabastece.

```card
title: Pouso automático 1/2
[ ] Destino: plataforma livre, ou pista com rodas
[ ] Alcance: até 512 blocos
[ ] Piloto automático: J até acender POUSO
```

```card
title: Pouso automático 2/2
[ ] Mãos livres: nenhuma tecla do comando
[ ] Toque: desce, para e solta
```

```card
title: Pouso manual 1/2
[ ] Velocidade: abaixo de 10 m/s
[ ] Pairar: bocais baixos, TREM BAIXADO
[ ] Descida: tecla de correr, reto
```

```card
title: Pouso manual 2/2
[ ] Com rodas: acelerador fechado, rolar até parar
[ ] Estacionada: levantar do comando
```

## O que pode dar errado

> [!PROBLEM] [[J]] pula o POUSO.
> Não há plataforma livre que você possa usar a até 512 blocos, nem pista livre.

> [!PROBLEM] [[B]] não faz nada.
> A nave está perto demais do chão, ou a barra do impulso está vazia.

> [!CAUTION]
> Os motores de ar respiram o ar. Onde ele é rarefeito demais, eles não sustentam a nave, e ela afunda.
