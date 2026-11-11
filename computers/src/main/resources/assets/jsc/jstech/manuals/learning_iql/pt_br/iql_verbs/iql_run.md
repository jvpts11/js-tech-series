---
section: iql_verbs
order: 373
---
# RUN: máquinas de outros mods

Máquinas de outros mods podem entrar na rede, e algumas delas fazem trabalho que a rede pede. RUN pede a uma delas um tipo de trabalho que o mod dela acrescenta. Escreva RUN e depois o id do tipo: o nome do mod, dois-pontos e o nome do tipo.

> [!DEFINE] tipo de Operation
> Um tipo de trabalho que a rede pode pedir. Outros mods acrescentam tipos próprios, e só as máquinas deles os fazem.

```iql beam.iql
RUN mymod:accel_run ON 'Cyclotron-3fa2c1' WITH beam = 'proton', energy = 20 PRIORITY HIGH
```

Table: As partes de RUN

| Parte | O que faz |
| --- | --- |
| `ON 'name'` | Nomeia a máquina que faz o trabalho. Sem ON, a rede dá o trabalho à máquina que já tem menos dele. |
| `WITH field = value` | Dá os campos que o tipo lê. Uma vírgula vai entre dois campos. Palavras vão entre aspas; números e true ou false, não. |
| `PRIORITY HIGH` | Define o lugar do trabalho na fila, como em qualquer outra instrução. |

## Nomeando a máquina

Uma máquina atende pelo nome que você deu a ela na própria tela. Uma máquina sem nome próprio atende pelo nome do bloco, um hífen e seis letras e algarismos, como Cyclotron-3fa2c1. Maiúsculas não importam.

Você também pode nomear uma máquina pelo lugar onde ela está, como mostra a listagem. A coluna at da tabela da máquina mostra essa posição ([](jsc:query_and_select)).

```iql by_place.iql
RUN mymod:accel_run ON '12, 64, -3'
```

## O que acontece depois

O trabalho espera numa fila do Mainframe, como os outros. Quando chega a vez dele, o Mainframe o entrega à máquina e libera a fila na hora. A máquina então trabalha pelo tempo que o trabalho levar, e o Mainframe segue com outros trabalhos.

Até a máquina terminar, o trabalho fica na aba Processes do Network Manager. Você pode pará-lo ali, e a máquina então para. Quando termina, ele vai para o log sob RUN, com o tipo, a máquina e o que você pediu.

Um job também pode pedir trabalho a uma máquina, por exemplo quando um evento do mundo chega:

```iql shield.iql
CREATE JOB shield AS RUN mymod:shield_up WHEN EVENT mymod:solar_storm COMING
```

Table: As respostas de RUN

| Resposta | O que significa |
| --- | --- |
| `<kind> queued for <machine>` | O trabalho espera a sua vez e depois vai para essa máquina. |
| `no machine on the network carries out <kind>` | Nenhuma máquina da rede faz esse tipo de trabalho. |
| `no machine named <name> is on the network` | Nenhuma máquina da rede tem esse nome ou está nesse lugar. |

## Se algo der errado

> [!PROBLEM] RUN precisa do id de um tipo de Operation, como mod:tipo
> Digite o id do tipo depois de RUN, com o nome do mod e dois-pontos antes do nome do tipo.

> [!PROBLEM] nenhum mod registrou um tipo de Operation <id>
> Confira o id. O mod que acrescenta o tipo precisa estar instalado.

> [!PROBLEM] <máquina> não executa <tipo>
> Nomeie uma máquina que faça esse tipo de trabalho, ou deixe o ON de fora.

> [!PROBLEM] <tipo> não tem o campo <campo>; os campos dele são <campos>
> Use um dos campos que a resposta lista.

> [!PROBLEM] os campos não servem para <tipo>: <motivo>
> Dê a cada campo um valor do tipo certo, como um número onde o tipo lê um número.

> [!PROBLEM] <tipo> é pedido pela tela da própria máquina, nunca na linguagem da rede
> Peça esse trabalho na tela da própria máquina.

> [!SEE] jsc:iql_jobs, jsc:query_and_select, jsc:clauses_priority_if_with
