---
section: fab
order: 42
icon: jsc:mask_blank
covers: [jsc:mask_blank]
---
# Gravar uma máscara

Uma Estação de Litografia expõe cada camada de um wafer por uma máscara, e uma máscara nunca se gasta: você grava uma máscara e ela serve a todo wafer do seu chip. Toda máscara é gravada numa Máscara Virgem, uma placa de máscara da Polidora Óptica com uma pepita de cromo depositada nela na Câmara de Deposição.

A estação Vintage grava as suas máscaras por fotorredução: ponha nela uma máscara virgem e o Layout do chip, e ela dá a máscara, gastando o Layout. Um Layout é o desenho do chip, impresso num Filme Virgem pela impressora de um computador.

Da Legacy em diante, você grava a máscara pelo computador ligado à estação, com o Midsoft Lithography.

> [!DEFINE] Midsoft Lithography
> O programa de um computador ligado a uma Estação de Litografia da Legacy em diante: mostra a estação e grava as suas máscaras. No MC-DOS e no MC-NET ocupa a tela inteira; num desktop é uma janela.

1. Instale o Midsoft Lithography no computador ligado à estação, pelo seu disco de instalação.
2. Ponha uma Máscara Virgem na estação.
3. Abra o programa: digite litho no prompt do MC-DOS ou do MC-NET, ou abra Litografia no desktop.
4. Escolha o tipo de chip e depois a máscara: com as setas e [[Tab]] na tela de texto, com as abas e a lista na janela.
5. Aperte [[W]] na tela de texto, ou Gravar Máscara na janela.

A estação grava a máscara no tempo de uma camada, 20 segundos, e na metade dele num computador duas eras mais novo do que o que a estação pede. O computador reserva a memória da estação enquanto grava, como enquanto expõe. A máscara fica então onde estava a máscara virgem, e a estação expõe por ela.

Uma estação não grava máscara enquanto expõe um wafer: troque a máscara quando o wafer sair. Ela grava as máscaras das eras que atende para as quais o seu computador faz chips, já que os chips de cada era são feitos pelos computadores da era anterior. A estação ArF grava as máscaras da Transition num computador Legacy, e as da Standard também num computador Transition ou posterior.

```table
{"caption": "As listas do programa", "heads": ["Lista", "As suas máscaras"], "rows": [[{"words": "Processadores"}, {"words": "uma máscara de processador para cada microarquitetura da era"}], [{"words": "Gráficos"}, {"words": "uma máscara de gráficos para cada microarquitetura da era"}], [{"words": "Memória"}, {"words": "o chip de memória e o chip controlador da era"}], [{"words": "Portáteis"}, {"words": "as famílias de portáteis da era"}], [{"words": "Hashing"}, {"words": "o chip de hashing da Blockwright, na Standard e na Advanced"}], [{"words": "Luz"}, {"words": "a máscara dos diodos laser, da Legacy à Standard, e a dos LEDs, na Standard"}]]}
```

```command
id: jsc:command/mc_net/litho
```

## Se algo der errado

> [!PROBLEM] O programa diz que nenhuma Estação de Litografia está ligada a este computador.
> Passe um cabo periférico da Porta Periférica de uma estação Legacy ou posterior até este computador. Uma estação Vintage não precisa de computador: grava as suas máscaras a partir de um Layout.

> [!PROBLEM] Ele diz para pôr uma Máscara Virgem na estação.
> A estação não tem máscara virgem onde gravar. Ponha uma Máscara Virgem nela e grave a máscara de novo.

> [!PROBLEM] Ele diz que a estação está expondo.
> A estação está expondo um wafer pela máscara que tem. Espere o wafer sair, tire a máscara, ponha uma máscara virgem e grave a máscara nova.

> [!PROBLEM] A máscara que eu quero não está na lista.
> A estação grava só as máscaras das suas eras para as quais este computador faz chips. Na estação ArF, um computador Transition ou posterior lista também as máscaras da Standard.

> [!SEE] jsc:lithography_stations, jsc:chip_factory_machines
