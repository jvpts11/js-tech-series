---
section: computers
order: 53
icon: jsc:wan_gateway
covers_family: jsc:wan_gateways
---
# Computadores Gateway WAN

Um Computador Gateway WAN fica em cada ponta de uma linha de longa distância: a Linha Telefônica, a Linha Dedicada, a Linha T3, o VLDC e a Fibra Escura, uma para cada era ([Cabos de dados](jsc:data_cables)). É um computador como qualquer outro. Aceita uma placa-mãe, processadores, memória, uma fonte de alimentação e discos, e roda um sistema. O que faz dele um gateway são as suas Placas de Interface WAN.

> [!DEFINE] Computador Gateway WAN
> O computador em cada ponta de uma linha de longa distância, com uma porta para a linha para cada uma das suas Placas de Interface WAN.

Um gateway é um bloco mais largo que um Mainframe: quatro blocos de largura, dois de altura e dois de profundidade. Coloque-o na fileira da frente do térreo, o segundo a partir da esquerda olhando de frente. Se todo o espaço de 4 por 2 por 2 estiver livre, o gabinete se forma. Se não, o item fica na sua mão, e partículas delineiam os espaços bloqueados.

Há um gateway para cada era: o Computador Gateway WAN (Standard), e os Vintage, Legacy, Transition e Advanced. Cada um aceita uma placa-mãe MTX da sua própria era, como um Mainframe.

```table
{"caption": "O que um gateway guarda", "rows": [[{"words": "Placa-mãe"}, {"literal": "1 MTX"}], [{"words": "Processadores"}, {"literal": "up to 4"}], [{"words": "Memória"}, {"literal": "up to 8"}], [{"words": "Placas de expansão"}, {"literal": "up to 6, of them up to 4 WAN Interface Cards"}], [{"words": "Discos"}, {"literal": "up to 4"}], [{"words": "Fonte de alimentação"}, {"literal": "1"}]]}
```

As suas quatro portas para a linha de longa distância ficam atrás, uma no meio de cada bloco de baixo, numeradas de 1 a 4 a partir da esquerda quando você está atrás dele. Cada porta aceita a linha da era do gateway e de todas as eras anteriores. A primeira Placa de Interface WAN nos slots de placa opera a porta 1, a segunda opera a porta 2, e assim por diante, então uma porta sem placa não faz nada.

Uma linha está ativa enquanto os gateways das suas duas pontas rodam o Nouvell BorderLink, as duas portas estão ligadas, e cada placa aceita a linha entre eles. Uma placa aceita a linha da sua própria era e as mais antigas, como uma porta.

> [!DEFINE] Placa de Interface WAN
> A placa que opera uma porta de um Computador Gateway WAN, para a linha de longa distância da sua era e as mais antigas.

Um gateway não é um orquestrador. Ele entra na rede onde está pelo backbone, em qualquer face, como um Mainframe, e só aceita fibra com uma Placa de Rede Óptica. Ele nunca entra numa rede por uma linha de longa distância: essa linha vai para a outra ponta.

A tela de montagem é a do Mainframe, com as linhas no lugar das operações:

Table: A tela de montagem

| Peça | O que mostra |
| --- | --- |
| CAPACIDADE (it/t) | Os itens por tick que os processadores conseguem tratar, somados. |
| LINHAS | Quantas das suas linhas estão ativas, das portas com placa, como "2 / 3 ATIVAS". |
| REDE | VINCULADO enquanto está em uma rede. |
| INÍCIO AUTO | Liga o gateway sozinho quando a sua montagem está de pé. |
| Luzes de LINHAS 1 a 4 | A linha de cada porta: verde quando ativa, vermelha quando caída, apagada sem placa. |
| PWR, DISK e NET | Três luzes: energia, disco e rede. |

```scene
{"balloons": [{"detail": "Um para cada Placa de Interface WAN, com as luzes da sua linha descendo pela frente.", "number": 1, "part": {"box": [0, 6, 11, 13, 16, 13], "x": 18, "y": 0, "z": 16}, "text": "Módulos de linha"}, {"detail": "Clique nele para ligar ou desligar o gateway.", "number": 2, "part": {"box": [7, 3, 14, 9, 6, 16], "x": 16, "y": 0, "z": 16}, "text": "Botão de energia"}, {"detail": "Elas mostram que ele está em execução, que está em uma rede e quando tem uma falha.", "number": 3, "part": {"box": [0, 9, 14, 11, 11, 16], "x": 17, "y": 1, "z": 16}, "text": "Luzes de estado"}], "blocks": [{"data": "{VisualSystems:0,VisualFlags:15,Lines:37}", "state": "jsc:wan_gateway[facing=south,optical=false]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 18, "y": 0, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 17, "y": 0, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=1]", "x": 18, "y": 0, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=2]", "x": 17, "y": 0, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=3]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=4]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 18, "y": 1, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 17, "y": 1, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 18, "y": 1, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 17, "y": 1, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 15, "y": 1, "z": 15}], "caption": "Um Computador Gateway WAN Standard, em execução, com três Placas de Interface WAN: duas linhas ativas e uma caída. Arraste para girá-lo.", "height": 130}
```

O gabinete também mostra as suas linhas. A sua baia de linhas tem um módulo de linha para cada placa, com o número da porta em cima e as suas luzes descendo pela frente, e uma tampa onde não há placa.

O seu botão de energia fica no peitoril, no meio do bloco que você colocou. Um clique nele liga ou desliga o gateway. Um clique em qualquer outro ponto do gabinete abre a sua tela de montagem, e um clique agachado abre ou fecha o painel de serviço.

Se você quebra um gateway, ele dropa o gateway da sua própria era, e os itens do seu inventário se espalham.

O Nouvell BorderLink é o serviço próprio do gateway, com uma versão para cada era. Sem ele, as linhas do gateway ficam inativas. Numa área de trabalho Frames ele é uma janela. No MC-NET ele ocupa a tela, com Portas, Log e Console no seu trilho. No prompt, o comando borderlink faz as mesmas coisas.

O BorderLink lista as quatro portas: a linha em cada uma, o gateway na outra ponta, o que a linha faz, e se ela está ativa. Escolha uma porta para ver o que a outra ponta pode fazer aqui, se a porta retransmite para outras redes, e se a porta está ligada.

O que uma linha faz depende de onde estão os Mainframes. Com um Mainframe de um lado só, a linha estende aquela rede até o outro lugar: os computadores, racks e cabos de lá entram nela, como se um único cabo corresse entre eles. Com um Mainframe de cada lado, as duas redes se federam. Elas continuam duas redes, cada uma com o seu próprio armazenamento, e cada uma trabalha com a outra até onde a outra permite.

> [!DEFINE] Extensão
> Uma linha de longa distância com um Mainframe de um lado só: a rede passa pela linha e chega ao outro lugar.

> [!DEFINE] Federação
> Uma linha de longa distância com um Mainframe de cada lado: duas redes que continuam duas, e trabalham uma com a outra até onde cada uma permite.

> [!DEFINE] Nouvell BorderLink
> O serviço próprio do Computador Gateway WAN, que opera as suas linhas de longa distância e diz o que cada outra ponta pode fazer aqui.

Cada porta decide o que a rede na sua outra ponta pode fazer aqui: ver os nossos itens, pegar itens, enviar itens e fabricar aqui. Uma porta nova deixa a outra ponta ver, e nada mais. Retransmitir deixa as redes atrás da outra ponta passarem por esta, até três linhas de distância.

Uma rede federada aparece na aba Federated do Interator de Rede, numa vista só dela. Dali você pode trazer os itens dela para o seu computador, soltar uma pilha na grade dela para enviá-la para lá, e fazer ela fabricar algo, que fica naquela rede. Cada uma dessas coisas atravessa a linha como uma Operation, na velocidade da linha mais lenta do caminho, e a aba Operations diz até onde ela chegou.

Uma linha que cai enquanto itens a atravessam os perde, quando o servidor está ajustado para perder itens quando uma rede se divide. Quando não está, a Operation espera a linha voltar. Uma Operation cuja outra ponta não está carregada espera pelos chunks dela.

## Se algo der errado

> [!PROBLEM] O gateway não se forma.
> Limpe todo o espaço de 4 por 2 por 2, e depois coloque-o de novo. Os espaços bloqueados mostram partículas.

> [!PROBLEM] Uma Placa de Interface WAN não entra num Mainframe nem num Computador Pessoal.
> Só um Computador Gateway WAN aceita uma.

> [!PROBLEM] Uma quinta Placa de Interface WAN não entra.
> O gateway tem quatro portas, então aceita quatro placas. Ponha outro tipo de placa no resto.

> [!PROBLEM] Uma linha continua caída.
> Verifique que o gateway está ligado, que o cabo está na porta daquela placa, e que a placa é tão nova quanto a linha. Os dois gateways precisam rodar o BorderLink, com a porta ligada.

> [!PROBLEM] A linha está ativa, mas nenhuma das redes vê os itens da outra.
> As duas só se federam com um Mainframe de cada lado. Depois verifique, no BorderLink do gateway de lá, o que a porta dele deixa esta rede fazer.

> [!PROBLEM] Um Mainframe do outro lado de uma extensão não causa conflito de rede.
> É assim que deve ser: isso forma uma federação. Um conflito de rede só vem de dois Mainframes no mesmo lugar.

> [!SEE] jsc:mainframes, jsc:expansion_cards, jsc:network_interactor
