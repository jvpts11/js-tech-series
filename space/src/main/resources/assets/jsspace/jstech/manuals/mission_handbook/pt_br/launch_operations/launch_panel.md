---
section: launch_operations
order: 10
icon: jsspace:launch_panel
covers: [jsspace:launch_panel]
---
# Painel de Lançamento

## O que é

O console LCC-1 junto à base, onde um lançamento é contado e recebe o seu GO. Ele encontra a plataforma na sua base a até 24 blocos ao redor, 8 abaixo e 16 acima, e lê o foguete dela. O seu quadro mostra o GO ou NO-GO de cada sistema: veículo, propelente, braços da torre, dilúvio, tempo, tripulação, alcance e garras.

## Para que serve

Lançar. A contagem corre a partir de T-60: os braços da torre recuam a partir de T-42, girando nas dobradiças até ficarem livres do foguete em T-10, a tampa do botão LANÇAR sobe em T-10, os motores acendem em T-6 e as garras soltam o foguete em T-0. Um cancelamento traz os braços de volta ao foguete. A contagem corre também no [computador de voo](jsspace:flight_computer) do foguete, e quem aperta LANÇAR fica dono do foguete em voo, para a sua equipe usar.

A contagem é da [plataforma móvel](jsspace:launcher), uma só para a sua base, não do painel. O painel e a página de lançamento do computador de voo do foguete mostram-na e mexem nela: o que se faz num aparece no outro, e qualquer um a põe em espera. A base não precisa de painel; a tripulação pode fazer a contagem sozinha, de dentro da cápsula.

## Como conseguir

No menu criativo, aba J's Space.

## Como usar

Use o painel para abrir o console; mexa nos controles com o mouse.

- [ ] VEÍCULO ... GO
- [ ] PROPELENTE ... GO, LOX e CH4 a 99%
- [ ] TEMPO ... GO
- [ ] DILÚVIO ... AUTO
- [ ] GARRAS ... AUTO
- [ ] Chave MESTRE ... ARMAR
- [ ] Tampa da IGNIÇÃO ... levantar
- [ ] IGNIÇÃO ... ARMAR
- [ ] CONTAGEM CORRENDO ... apertar
- [ ] LANÇAR ... apertar de T-10 até antes de T-6

```card
title: Deixar um mundo 1/4
[ ] Translado: plataforma na base
[ ] Abastecimento: LOX e CH4 a 99%
[ ] Painel: tudo GO
```

```card
title: Deixar um mundo 2/4
[ ] Armar: MESTRE, tampa, IGNIÇÃO
[ ] T-60: CONTAGEM CORRENDO
[ ] T-42: BRAÇOS RECUAM
```

```card
title: Deixar um mundo 3/4
[ ] T-10: a tampa sobe, apertar LANÇAR
[ ] T-6: motores acesos, dilúvio
[ ] T-0: SOLTURA, começa a subida
```

```card
title: Deixar um mundo 4/4
[ ] Separação: estágios gastos soltos
[ ] Espaço: o foguete entra no espaço
```

## O que pode dar errado

> [!PROBLEM] CONTAGEM CORRENDO não faz nada.
> MESTRE ou IGNIÇÃO não estão armados, ou o veículo, o propelente, o tempo, as garras ou o dilúvio estão NO-GO. O dilúvio fica NO-GO enquanto a porta de serviço da base tiver menos do que um dilúvio inteiro em AUTO: encha-a, ou desligue o DILÚVIO e confie na vala das chamas. Uma base sem porta de serviço mostra o dilúvio como NENHUM, que não suspende nada. TRIPULAÇÃO e ALCANCE nunca suspendem a contagem: ver a tripulação sentada e a base livre é trabalho do próprio jogador.

> [!PROBLEM] A contagem fica em ESPERA em T-6.
> LANÇAR não foi apertado até a ignição. Aperte-o: a contagem segue enquanto tudo estiver GO.

> [!PROBLEM] A contagem fica em ESPERA sozinha.
> Um sistema ficou NO-GO, uma tempestade de raios chegou sobre a base, ou a chave MESTRE foi para SEGURO. Aperte CONTAGEM CORRENDO quando tudo estiver GO de novo. VOLTAR T-10 leva uma espera abaixo de T-10 de volta a T-10; CANCELAR recomeça de T-60 e põe a chave MESTRE em SEGURO.

> [!PROBLEM] O lançamento é cancelado em T-6, com os motores apagados.
> O foguete pesa mais neste mundo do que o primeiro estágio empurra, e o servidor mantém desligados os motores de um foguete assim, para nada ser gasto. Use um motor maior, ou deixe o foguete mais leve. Onde o servidor deixa os motores acenderem, um foguete pesado demais fica na base queimando o propelente.

> [!PROBLEM] ABORTAR acende em T-0.
> O foguete não pôde ser solto, e o lançamento foi cancelado.
