---
section: computers
order: 52
icon: jsc:mainframe
covers_family: jsc:mainframes
---
# Mainframes

Toda rede tem exatamente um Mainframe, e ele é a sala de controle da rede. Só o Mainframe roda o Gerenciador de Rede. Dois Mainframes em uma mesma rede são um conflito, então dê a cada rede a sua própria.

> [!DEFINE] Mainframe
> O computador único de uma rede que roda o Gerenciador de Rede e orquestra o trabalho da rede.

Um Mainframe é grande: três blocos de largura, dois de altura e dois de profundidade. Coloque um bloco de Mainframe no meio do térreo, na fileira da frente. Se todo o espaço de 3 por 2 por 2 estiver livre, o gabinete se forma. Se não, o item fica na sua mão, e partículas delineiam os espaços bloqueados para você ver o que está no caminho.

O Mainframe aceita uma placa-mãe MTX da sua própria era. Há um Mainframe para cada era: o Mainframe (Standard), e os Mainframes Vintage, Legacy, Transition e Advanced.

Um Mainframe só executa a rede com um sistema em um dos seus discos, como o MC-NET ([](jsc:mc_net)). Instale-o como em qualquer computador ([](jsc:installing)); até lá, o Mainframe segura toda Operação que recebe. Da era Standard em diante, o backbone é de fibra, e o Mainframe só aceita fibra enquanto uma Placa de Rede Óptica está em um dos seus slots de placa ([](jsc:expansion_cards)).

```table
{"caption": "O que um Mainframe guarda", "rows": [[{"words": "Placa-mãe"}, {"literal": "1 MTX"}], [{"words": "Processadores"}, {"literal": "up to 4"}], [{"words": "Memória"}, {"literal": "up to 8"}], [{"words": "Placas de vídeo"}, {"literal": "up to 6"}], [{"words": "Discos"}, {"literal": "up to 4"}], [{"words": "Fonte de alimentação"}, {"literal": "1"}], [{"words": "Slots de armazenamento"}, {"literal": "up to 27"}]]}
```

O seu hardware define a velocidade da rede. Os seus processadores somam a sua capacidade, e os Subframes que trabalham para ele somam a isso ([](jsc:subframes)). A sua memória define um buffer de RAM, contado em itens: um módulo de memória Vintage dá de 1 a 16 itens, e um módulo Advanced dá de 2.048 a 32.768. O Mainframe trabalha no menor valor entre a sua capacidade e o seu buffer de RAM, então um buffer pequeno limita processadores rápidos.

> [!DEFINE] Subframe
> Um computador que empresta sua capacidade e suas filas ao Mainframe da sua rede.

> [!DEFINE] Capacidade
> Os itens por tick que os processadores do Mainframe conseguem tratar, somados.

Os processadores rodam uma fila, e cada placa de vídeo roda mais uma. As filas não dividem capacidade. Uma placa fraca pode segurar a sua fila para baixo, e a tela de montagem então diz "A fila de uma placa corre no máximo à velocidade da CPU, e não mais rápido que a placa".

A tela de montagem mostra estas partes do Mainframe:

Table: A tela de montagem

| Peça | O que mostra |
| --- | --- |
| CAPACITY (it/t) | Os itens por tick que os processadores conseguem tratar, somados. |
| QUEUES | As filas dos seus processadores e placas de vídeo. |
| RAM BUFFER (it) | Os itens que a memória consegue armazenar em buffer, somados entre os módulos. |
| AUTO START | Liga o Mainframe sozinho quando a sua montagem está de pé. |
| FAILOVER | Alterna o Mainframe entre proprietário e reserva. |
| PWR, DISK e NET | Três luzes: energia, disco e rede. |
| STANDBY | Este Mainframe espera como reserva. |
| CONFLICT | Dois proprietários compartilham um segmento da rede. |

O seu botão de energia fica na soleira, a borda na parte de baixo da frente, no meio. Um clique nele liga ou desliga o Mainframe, não importa o que você esteja segurando. Um clique em qualquer outro ponto do gabinete abre a sua tela de montagem. Agache e clique no gabinete para abrir ou fechar o painel de serviço. Os seus processadores, memória e placas são vistos só por essa abertura.

```scene
{"balloons": [{"detail": "A capacidade deles se soma à velocidade da rede.", "number": 1, "part": {"box": [4, 6, 5, 16, 8, 11], "x": 15, "y": 1, "z": 16}, "text": "Processadores"}, {"detail": "O tamanho dela define o buffer de RAM.", "number": 2, "part": {"box": [4, 1, 5, 15, 5, 11], "x": 15, "y": 1, "z": 16}, "text": "Memória"}, {"detail": "Cada placa executa mais uma fila.", "number": 3, "part": {"box": [4, 11, 5, 12, 16, 11], "x": 15, "y": 0, "z": 16}, "text": "Placas de vídeo"}, {"number": 4, "part": {"box": [4, 6, 5, 11, 11, 11], "x": 15, "y": 0, "z": 16}, "text": "Fonte de alimentação"}, {"number": 5, "part": {"box": [0, 7, 4, 12, 14, 11], "x": 17, "y": 0, "z": 16}, "text": "Discos"}, {"detail": "Clique nele para ligar ou desligar o Mainframe.", "number": 6, "part": {"box": [7, 3, 14, 9, 6, 16], "x": 16, "y": 0, "z": 16}, "text": "Botão de energia"}, {"detail": "Elas mostram que ele está em execução, que está em uma rede e quando tem uma falha.", "number": 7, "part": {"box": [0, 9, 14, 11, 11, 16], "x": 17, "y": 1, "z": 16}, "text": "Luzes de status"}], "blocks": [{"data": "{VisualHardware:3695079,VisualSystems:0,VisualFlags:15}", "state": "jsc:mainframe[facing=south,optical=false]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 17, "y": 0, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=true,era=3,optical=false]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 17, "y": 0, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=true,era=3,optical=false]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 17, "y": 1, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 15, "y": 1, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=true,era=3,optical=false]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 17, "y": 1, "z": 15}], "caption": "Um Mainframe Standard, em execução, com o painel de serviço removido para que suas peças apareçam. Arraste para girá-lo.", "height": 130}
```

O INÍCIO AUTO mantém o Mainframe pronto. Com ele ligado e uma montagem válida, o Mainframe inicia na hora, como um início a frio que roda o seu autoteste. Se uma mudança quebra a montagem, o Mainframe desliga. Quando a montagem volta a estar de pé, ele liga sozinho.

Com a opção [FAILOVER](jsc:failover) desligada, o Mainframe é um proprietário: ele assume o segmento de rede em que está, ou estabelece uma rede própria. Com o FAILOVER ligado, ele é uma reserva: quando um proprietário está na sua rede, ele se conecta a essa rede e não a executa.

> [!DEFINE] Proprietário
> O Mainframe que roda a sua rede, com FAILOVER desligado.

> [!DEFINE] Reserva
> Um Mainframe que espera em uma rede que um proprietário executa, pronto para assumi-la.

Uma reserva assume uma rede que não tem proprietário. Depois de 3 segundos, ou seja 60 ticks, uma reserva passa a rodar a rede. A contagem regressiva é salva com o mundo, então recarregar não a reinicia. Se dois proprietários ficam em um mesmo segmento de uma rede, a rede entra em conflito, e a tela mostra CONFLICT.

Se você quebrar um Mainframe, ele deixa cair o Mainframe da sua própria era, e os itens do seu inventário se espalham. Se ele era o último Mainframe da sua rede, a rede fica órfã, não é apagada, então o seu armazenamento sobrevive ([](jsc:network_identity)).

## Se algo der errado

> [!PROBLEM] O Mainframe não se forma.
> Limpe todo o espaço de 3 por 2 por 2, e depois coloque-o de novo. Os espaços bloqueados mostram partículas.

> [!PROBLEM] Toda Operação fica na fila.
> O Mainframe não tem sistema. Instale um nele ([](jsc:installing)).

> [!PROBLEM] A fibra não se liga ao Mainframe.
> Coloque uma Placa de Rede Óptica em um dos slots de placa dele ([](jsc:expansion_cards)).

> [!PROBLEM] A tela mostra CONFLICT.
> Dois proprietários compartilham um mesmo segmento. Tire um deles do segmento, ou ligue o FAILOVER em um deles.

> [!PROBLEM] A rede está lenta.
> Processadores mais rápidos aumentam a capacidade, e mais placas de vídeo dão mais filas. Verifique também a memória: um buffer pequeno limita a capacidade.

> [!PROBLEM] O chat informa um conflito de rede: dois Mainframes compartilham uma mesma rede.
> Mantenha um Mainframe em cada rede, e separe os cabos entre elas.

> [!SEE] jsc:states_and_queues, jsc:failover
