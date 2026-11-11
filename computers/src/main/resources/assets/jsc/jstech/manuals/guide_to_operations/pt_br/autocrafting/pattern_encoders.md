---
section: autocrafting
order: 144
icon: jsc:pattern_encoder
covers_family: jsc:pattern_encoders
---
# O Codificador de Padrões

Um padrão em um computador não serve para outro, então um padrão precisa viajar. O Codificador de Padrões é o aparelho que grava padrões em mídias, como disquetes, discos e pen drives, e você leva a mídia de um computador para outro. O codificador se conecta a um computador com um Cabo Periférico ([](jsc:peripheral_cables)).

1. Escreva o padrão no Pattern Studio ([](jsc:pattern_studio)).
2. Coloque uma mídia virgem em um Codificador de Padrões conectado ao computador e grave o padrão nela.
3. No Computador de Fabricação, abra o Gerenciador de Fabricação e carregue o padrão da mídia ([](jsc:crafting_manager)).

Um padrão de bancada vai para um cartão, e um padrão de máquina vai para a interface da sua máquina. O Codificador de Padrões comum é o Standard, e cada era tem o seu próprio codificador.

Table: Mídias que cada codificador grava

| Era do codificador | Mídia que ele grava |
| --- | --- |
| Vintage | Disquete |
| Legacy | CD-RW |
| Transition | DVD-RW e CD-RW |
| Standard | DVD-RW, CD-RW e Pen Drive |
| Advanced | BD-RE e Pen Drive |

Table: As mídias dos padrões

| Mídia | Era | O codificador consegue gravá-la? |
| --- | --- | --- |
| Disquete | Vintage | sim |
| CD-RW | Legacy | sim |
| CD-ROM | Legacy | não |
| DVD-RW | Transition | sim |
| DVD-ROM | Transition | não |
| Pen Drive | Standard | sim |
| BD-RE | Advanced | sim |
| BD-ROM | Advanced | não |

Uma mídia somente leitura, marcada como não, é recusada, porque um padrão precisa de uma mídia que o codificador consiga gravar.

A partir da era Legacy, o codificador grava discos, e os discos ficam em uma bandeja. Pressione o botão de ejetar para trazê-la para fora, coloque o disco nela e pressione o botão de novo, ou empurre a bandeja para dentro com a mão vazia. Nada é gravado enquanto a bandeja está para fora, e a bandeja fica fechada enquanto um padrão é gravado. Um disquete ou um pen drive não usa a bandeja: ele entra na sua ranhura, aconteça o que acontecer com a bandeja.

```scene
{"balloons": [{"number": 1, "part": {"x": 1, "y": 0, "z": 1}, "text": "O Codificador de Padrões"}, {"detail": "Pressione o botão de ejeção para colocá-la para fora ou para recolhê-la.", "number": 2, "part": {"box": [2, 11, 12, 12, 13, 16], "x": 1, "y": 0, "z": 1}, "text": "A bandeja de disco"}, {"number": 3, "part": {"box": [12, 9, 15, 14, 11, 16], "x": 1, "y": 0, "z": 1}, "text": "O botão de ejeção"}], "blocks": [{"data": "{Media:{Size:1,Items:[{Slot:0,id:\"jsc:dvd_rw\",count:1}]},TrayOpen:1b}", "state": "jsc:pattern_encoder[facing=south]", "x": 1, "y": 0, "z": 1}], "caption": "Um Codificador de Padrões com a bandeja para fora e um DVD-RW deitado sobre ela. Arraste para ver os outros lados.", "height": 110, "view": {"pitch": 20.0, "yaw": -25.0, "zoom": 1.2}}
```

Uma gravação passa por três fases: busca, gravação e verificação. A tela mostra "Buscando", depois "Gravando" e o nome do arquivo, depois "Verificando" e o nome, e por último "Concluído:" e o nome.

Table: Quão rápido um padrão é gravado

| Mídia | Velocidade de gravação | Busca | Verificação |
| --- | --- | --- | --- |
| Disquete | 250 bytes por tick | 20 ticks | 10 ticks |
| CD | 1.024 bytes por tick | 30 ticks | 10 ticks |
| DVD | 4.096 bytes por tick | 15 ticks | 5 ticks |
| Blu-ray | 16.384 bytes por tick | 12 ticks | 4 ticks |
| Pen Drive | sem limite | 4 ticks | 2 ticks |

Busca é o tempo para achar o lugar na mídia, e verificação é a conferência feita relendo a mídia. Um tick é a vigésima parte de um segundo, então 20 ticks fazem um segundo.

A fila comporta até 8 gravações. Uma gravação é recusada quando a fila está cheia. O arquivo recebe o nome do padrão, com .craft no final, e o seu nome é tornado único na mídia. Quando a gravação termina, o codificador relê a mídia e compara. Se não bater, aparece "Falha na verificação" e o nome do arquivo.

A baia fica travada enquanto uma gravação está em andamento, então você não consegue tirar o disco até a gravação terminar. O painel mostra "Não conectado" quando nenhum computador está ligado, e "Conectado a" com o nome do computador quando há um.

Table: O painel do codificador

| Linha no painel | O que significa |
| --- | --- |
| Pronto | O codificador espera por uma tarefa. |
| Inserir mídia | Não há nenhuma mídia para gravar na baia. |
| Bandeja aberta | A bandeja está para fora e não tem nenhuma mídia para gravar. |
| Buscando | O codificador está procurando o lugar na mídia. |
| Gravando, depois o nome do arquivo | O arquivo do padrão está sendo gravado. |
| Verificando, depois o nome do arquivo | O arquivo gravado está sendo lido de volta e comparado. |
| Concluído, depois o nome do arquivo | O arquivo está na mídia. |
| Cancelado | A gravação foi interrompida antes de terminar. |
| Falha na gravação, com o motivo | A gravação não terminou: a mídia está cheia, ou não há mídia. |

Table: Mensagens do codificador

| Mensagem | O que significa |
| --- | --- |
| A bandeja está fechada: pressione o botão de ejetar para abri-la. | A bandeja não abre nem aceita um disco até você pressionar o botão de ejetar. |
| A baia já tem um disco: agache e clique para ejetá-lo. | A baia tem um disco só, então tire-o primeiro. |
| O codificador está gravando: espere terminar. | Uma gravação está em andamento, então a baia e o botão de ejetar estão travados. |

## Se algo der errado

> [!PROBLEM] O Pattern Studio não vê nenhum codificador.
> Ele não está conectado a esse computador. Conecte-o com um Cabo Periférico.

> [!PROBLEM] O disco não é lido, e a gravação não começa.
> O disco está em uma bandeja aberta. Feche a bandeja com o botão de ejetar, ou empurre-a para dentro com a mão vazia.

> [!PROBLEM] O codificador diz "Um codificador Vintage grava apenas disquetes."
> Coloque uma mídia que a tabela acima lista para este codificador.

> [!PROBLEM] O codificador diz "Um codificador Legacy grava apenas CDs."
> Coloque uma mídia que a tabela acima lista para este codificador.

> [!PROBLEM] O codificador diz "Um codificador Transition grava apenas DVDs e CDs."
> Coloque uma mídia que a tabela acima lista para este codificador.

> [!PROBLEM] O codificador diz "Este codificador grava DVDs, CDs e pen drives, não isso."
> Coloque uma mídia que a tabela acima lista para este codificador.

> [!PROBLEM] O codificador diz "Um codificador Advanced grava apenas discos Blu-ray e pen drives."
> Coloque uma mídia que a tabela acima lista para este codificador.

> [!PROBLEM] O codificador diz "Esse disco é somente leitura e não pode ser gravado."
> Use um disco que possa ser gravado, como a tabela acima mostra.

> [!SEE] jsc:peripheral_cables, jsc:drives, jsc:pattern_studio, jsc:crafting_manager
