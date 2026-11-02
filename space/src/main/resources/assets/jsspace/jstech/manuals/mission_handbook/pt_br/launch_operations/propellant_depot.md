---
section: launch_operations
order: 9
icon: jsspace:transfer_pump
covers: [jsspace:sphere_shell, jsspace:tank_leg, jsspace:transfer_pump, jsspace:vent_stack]
---
# Depósito de Propelente

## O que é

Uma esfera branca soldada, sobre pernas, que guarda um propelente para a base. A esfera é construída como um cubo de Casco da Esfera, com aresta ímpar de três a nove blocos e o lado de fora inteiro. Uma coluna de Pernas do Tanque fica sob cada canto de baixo até o chão, as quatro da mesma altura; uma Chaminé de Respiro fica no meio do topo; e a Bomba de Transferência fica no chão, encostada numa perna. Quando o depósito fica inteiro, a bomba o desenha como uma esfera redonda.

## Para que serve

Guardar oxigênio líquido ou metano líquido, tanto quanto cabe numa bola da largura do cubo, para os canos levarem até a porta de serviço da [base de lançamento](jsspace:launch_pad). Um depósito de oxigênio líquido também enche de oxigênio, de volta ao estado de gás, uma Garrafa de Oxigênio usada na sua bomba.

## Como conseguir

No menu criativo, aba J's Space.

## Como usar

```multiblock
id: jsspace:propellant_depot
caption: O Depósito de Propelente, construído no tamanho aprovado
height: 120
```

1. Construa o cubo de Casco da Esfera, as quatro pernas, a chaminé e a bomba. Um Projetor de Estruturas mostra o depósito aprovado, de nove blocos de largura sobre pernas de dois, e um Construtor de Multiblocos usado no projetor o constrói; um depósito de outro tamanho se constrói à mão.
2. A bomba procura o seu depósito a cada dois segundos e o forma quando ele fica inteiro.
3. Encha a bomba por cano com um líquido. Um foguete de metalox precisa de um depósito de cada.
4. Leve um cano da bomba até a Porta de Serviço da Plataforma.

## O que pode dar errado

> [!PROBLEM] O depósito não se forma.
> A aresta é par, menor que três ou maior que nove; falta um bloco do lado de fora; as pernas não têm todas a mesma altura, ou estão sobre o ar; a chaminé não está no meio do topo; ou a bomba não está encostada numa perna.

> [!CAUTION]
> Um depósito que se desfaz deixa o seu líquido evaporar: a bomba fica vazia depois.

> [!PROBLEM] A bomba não aceita líquido.
> Ela aceita só oxigênio líquido ou metano líquido, um de cada vez, e só enquanto o depósito estiver formado.
