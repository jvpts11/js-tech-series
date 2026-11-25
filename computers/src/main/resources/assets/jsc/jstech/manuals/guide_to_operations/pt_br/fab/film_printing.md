---
section: fab
order: 43
icon: jsc:blank_film
covers: [jsc:blank_film, jsc:phototool_vintage, jsc:phototool_legacy, jsc:phototool_transition, jsc:phototool_standard, jsc:phototool_advanced]
---
# Imprimir um filme

Duas coisas da cadeia são desenhos em filme: os Layouts do Vintage, o desenho de cada chip de que a sua Estação de Litografia grava a máscara do chip, e a fotoferramenta de cada era, o desenho das camadas da sua placa gravada. A impressora de um computador imprime qualquer um deles num Filme Virgem, duas folhas dele feitas de uma folha de PVC na Bancada de Eletrônica do Vintage. Um Layout se gasta ao gravar a sua máscara; uma fotoferramenta nunca se gasta: um trabalho a empresta ao Gravador de Placas e a pega de volta, e uma serve a toda placa da sua era.

Você imprime o filme pelo computador a que a impressora está ligada, com o Midsoft Lithography, o programa que grava as máscaras de uma Estação de Litografia.

1. Instale o Midsoft Lithography no computador a que a impressora está ligada, pelo seu disco de instalação.
2. Ponha Filme Virgem na bandeja da impressora.
3. Abra o programa: digite litho no prompt do MC-DOS ou do MC-NET, ou abra Litografia no desktop.
4. Mostre a impressora: com [[S]] na tela de texto, pelo menu Estação na janela.
5. Escolha o tipo e depois o filme: com as setas e [[Tab]] na tela de texto, com as abas e a lista na janela.
6. Aperte [[W]] na tela de texto, ou Imprimir Filme na janela.

A impressora imprime o filme no tempo de uma página e o põe na sua saída, gastando um Filme Virgem da bandeja. Qualquer computador em funcionamento imprime qualquer filme em qualquer impressora.

```table
{"caption": "As listas do programa", "heads": ["Lista", "Os seus filmes"], "rows": [[{"words": "Layouts"}, {"words": "o Layout da memória e o do controlador e o de cada die de processador e de vídeo do Vintage"}], [{"words": "Fotoferramentas"}, {"words": "a fotoferramenta de cada era, as camadas da sua placa gravada"}]]}
```

```command
id: jsc:command/mc_net/litho
```

## Se algo der errado

> [!PROBLEM] O programa diz que nenhuma Estação de Litografia está ligada a este computador.
> Ligue uma impressora a este computador pelo seu cabo: o programa mostra uma impressora como mostra uma estação.

> [!PROBLEM] Ele diz para pôr Filme Virgem na bandeja da impressora.
> A bandeja tem papel, um mapa ou nada. Ponha Filme Virgem nela e imprima o filme de novo.

> [!PROBLEM] Ele diz que a fila da impressora está cheia.
> A impressora tem tantos documentos esperando quanto cabem nela. Imprima o filme quando um deles sair.

> [!SEE] jsc:mask_writing, jsc:board_machines
