---
section: dos_programs
order: 207
---
# O Midsoft Lithography e as suas teclas

O Midsoft Lithography mostra a Estação de Litografia ligada ao computador e grava as máscaras pelas quais ela expõe ([](jsc:mask_writing)). Digite litho no prompt para abri-lo. O programa ocupa a tela inteira e funciona no MC-DOS e no [MC-NET](jsc:mc_net). Num desktop ele abre como uma janela.

1. Ponha uma Máscara Virgem na estação.
2. Digite litho no prompt e aperte [[Enter]].
3. Aperte [[Tab]] até o tipo de chip que você quer ficar aceso, e escolha a sua máscara com as setas para cima e para baixo.
4. Aperte [[W]] para gravar a máscara na máscara virgem. A linha sobre as teclas mostra quanto da gravação já foi.
5. Aperte [[F3]] para devolver o terminal.

A esquerda da tela mostra a estação: o que ela está fazendo, o wafer e a camada que expõe, o que o computador reserva para ela, os seus líquidos e gases, e o que está no seu slot de máscara. Quando há várias estações ligadas ao computador, [[S]] mostra a próxima.

```command
id: jsc:command/mc_dos/litho
```

## Se algo der errado

> [!PROBLEM] A tela diz que nenhuma Estação de Litografia está ligada a este computador.
> Passe um cabo periférico da Porta Periférica de uma estação Legacy ou posterior até este computador.

> [!PROBLEM] W diz que a estação está expondo.
> A estação não grava máscara enquanto expõe um wafer. Espere o wafer sair e então troque a máscara.

> [!SEE] jsc:mask_writing, jsc:lithography_stations
