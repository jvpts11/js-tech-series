---
section: fab
order: 43
icon: jsc:mould_blank
covers: [jsc:mould_blank]
---
# Cortar um molde

Cada soquete, slot e conector de uma placa é feito do seu próprio corpo de plástico, cada ventilador é montado na sua própria estrutura com rotor, e cada gabinete, computador pessoal, monitor, mainframe e rack de servidores leva a sua própria moldura, como cada cooler líquido leva a tampa da sua bomba; a Injetora molda cada uma no seu próprio molde de aço. Ela também reveste cada cabo com a sua própria capa, extrudada pela matriz do próprio cabo, uma ferramenta de aço cortada do mesmo jeito. Um molde nunca se gasta: um trabalho o empresta à injetora e o pega de volta, e um molde serve a todo corpo da sua peça. Todo molde é cortado num Bloco de Molde Virgem, quatro placas de aço feitas numa só na Bancada de Eletrônica.

Você corta o molde pelo computador ligado à injetora, com o Midsoft Lithography, o programa que grava as máscaras de uma Estação de Litografia.

1. Instale o Midsoft Lithography no computador ligado à Porta Periférica atrás da torre da injetora, pelo seu disco de instalação.
2. Ponha um Bloco de Molde Virgem no slot de molde da injetora.
3. Abra o programa: digite litho no prompt do MC-DOS ou do MC-NET, ou abra Litografia no desktop.
4. Escolha a família e depois o molde: com as setas e [[Tab]] na tela de texto, com as abas e a lista na janela.
5. Aperte [[W]] na tela de texto, ou Cortar Molde na janela.

A injetora corta o molde no tempo de um passo, 10 segundos, e o computador reserva 8 MB da sua memória enquanto corta. Qualquer computador em funcionamento corta qualquer molde. O molde fica então onde estava o bloco virgem, e a injetora molda nele o seu corpo.

Uma injetora não corta molde enquanto molda uma peça: corte o molde quando a peça sair.

```table
{"caption": "As listas do programa", "heads": ["Lista", "Os seus moldes"], "rows": [[{"words": "Soquetes"}, {"words": "o corpo de cada soquete de processador"}], [{"words": "Slots"}, {"words": "o corpo de cada slot de memória e de expansão"}], [{"words": "Conectores"}, {"words": "o corpo de cada conector de armazenamento e de energia"}], [{"words": "Ventiladores"}, {"words": "a estrutura de ventilador de cada era"}], [{"words": "Molduras"}, {"words": "a moldura de cada gabinete, computador pessoal, monitor, mainframe e rack de servidores e da unidade de disquete, as tampas das bombas dos coolers líquidos e a capa do braço robótico"}], [{"words": "Cabos"}, {"words": "a matriz de extrusão da capa de cada cabo"}]]}
```

```command
id: jsc:command/mc_net/litho
```

## Se algo der errado

> [!PROBLEM] O programa diz que nenhuma Estação de Litografia está ligada a este computador.
> Ligue por cabo periférico a Porta Periférica atrás da torre da injetora a este computador: o programa mostra uma injetora ligada como mostra uma estação.

> [!PROBLEM] Ele diz para pôr um Bloco de Molde Virgem na injetora.
> A injetora não tem bloco virgem para cortar. Ponha um Bloco de Molde Virgem no seu slot de molde e corte o molde de novo.

> [!PROBLEM] Ele diz que a injetora está moldando.
> A injetora está moldando uma peça no molde que tem. Espere a peça sair, tire o molde, ponha um bloco virgem e corte o molde novo.

> [!SEE] jsc:board_machines, jsc:mask_writing
