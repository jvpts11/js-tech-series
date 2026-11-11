---
section: peripherals
order: 67
icon: jsc:monitor
covers_family: jsc:monitors
---
# Monitores

O monitor é a tela de um computador. Tudo o que você faz com os programas dele, você faz no monitor: o computador é o gabinete, e o monitor é onde você se senta. O monitor é um periférico, e você o liga ao seu computador com um cabo periférico, ou o coloca bem encostado no computador.

> [!DEFINE] Periférico
> Um dispositivo, como um monitor, uma unidade ou uma impressora, que o computador alcança por meio do próprio cabo.

> [!DEFINE] Cabo Periférico
> O cabo que liga um computador aos seus dispositivos. Ele não transporta rede: a rede tem os próprios cabos de dados.

Dez monitores vêm em cinco eras. O Vintage tem quatro: o Mono I com branco-papel, o Mono II com verde, o Âmbar e o CGA com dezesseis cores. O Legacy é o primeiro tubo colorido completo. Os monitores Transition, Standard e Advanced são telas planas coloridas, e o Standard e o Advanced também vêm como monitores de toque.

Table: Os monitores

| Monitor | Era | Imagem | Junta-se a outros |
| --- | --- | --- | --- |
| Monitor Mono I | Vintage | Branco-papel, todo o sistema em branco | Não |
| Monitor Mono II | Vintage | Verde | Não |
| Monitor Âmbar | Vintage | Âmbar | Não |
| Monitor CGA | Vintage | Dezesseis cores | Não |
| Monitor Legacy | Legacy | Tubo de imagem colorido | Não |
| Monitor Transition | Transition | Tela plana colorida | Sim |
| Monitor | Standard | Tela plana colorida | Sim |
| Monitor Colorido | Advanced | Tela plana colorida | Sim |
| Monitor de Toque | Standard | Tela plana colorida, de toque | Sim, com monitores de toque |
| Monitor de Toque Advanced | Advanced | Tela plana colorida, de toque | Sim, com monitores de toque |

> [!DEFINE] Tela plana
> Um monitor que pode se juntar a outros formando uma tela grande: os monitores Transition, Standard e Advanced.

Uma tela plana se junta aos vizinhos formando uma tela grande. Coloque monitores do mesmo tipo lado a lado, encostados, em uma mesma parede e voltados para o mesmo lado, e eles se tornam uma tela só. Um espaço vazio, ou um degrau na parede, os deixa como monitores separados. A dica da ferramenta do jogo diz "Junta-se aos monitores iguais ao lado numa tela só, de até 8 de largura e 6 de altura", então uma tela tem no máximo 48 monitores. Monitores Vintage e Legacy não se juntam.

```scene
{"balloons": [{"detail": "Monitores de um mesmo tipo, encostados e voltados para o mesmo lado, formam uma só tela.", "number": 1, "part": {"x": 15, "y": 0, "z": 14}, "text": "Um Monitor plano"}, {"detail": "Clique nele para ligar ou desligar a tela inteira.", "number": 2, "part": {"box": [13, 1, 15, 15, 3, 16], "x": 16, "y": 0, "z": 14}, "text": "Botão de energia"}], "blocks": [{"state": "jsc:monitor[facing=north,button=false,lit=true]", "x": 14, "y": 0, "z": 14}, {"state": "jsc:monitor[facing=north,button=false,lit=true]", "x": 15, "y": 0, "z": 14}, {"state": "jsc:monitor[facing=north,button=true,lit=true]", "x": 16, "y": 0, "z": 14}], "caption": "Três Monitores planos lado a lado em uma mesma parede, ligados. Só o monitor do canto mostra seu botão de energia. Arraste a cena para ver a parte de trás.", "height": 110}
```

Cada monitor ocupa uma saída de vídeo do seu computador. Uma saída de vídeo é um conector de uma placa de vídeo, ou de outra placa, e ela serve para um único monitor ([](jsc:graphics_cards)).

> [!DEFINE] Saída de vídeo
> Um conector de uma placa de vídeo, ou de outra placa, que aciona um monitor.

Um monitor aceso também ocupa memória de vídeo, um bloco para cada monitor da sua tela. Quanto maior a era, mais memória um bloco ocupa:

```table
{"caption": "Memória de vídeo por bloco", "heads": ["Monitor", "Memória por bloco"], "rows": [[{"words": "Vintage, monocromático"}, {"amount": 64, "unit": "KB"}], [{"words": "Vintage, dezesseis cores"}, {"amount": 256, "unit": "KB"}], [{"words": "Legacy"}, {"amount": 4, "unit": "MB"}], [{"words": "Transition"}, {"amount": 16, "unit": "MB"}], [{"words": "Standard"}, {"amount": 64, "unit": "MB"}], [{"words": "Advanced"}, {"amount": 256, "unit": "MB"}]]}
```

O computador dá primeiro a sua memória de vídeo às janelas gráficas, depois aos monitores, na ordem em que você os vinculou. Um monitor que não cabe fica apagado.

Todo monitor tem um botão de energia na moldura. Clique no botão para ligar ou desligar aquele monitor. Ele desliga só o monitor: o computador por trás continua funcionando, e você liga o computador pelo gabinete. Em uma tela grande, o botão no canto liga ou desliga a tela inteira. Todo monitor começa ligado.

A partir do Legacy, um monitor ligado sem imagem entra em repouso, e a luz dele fica âmbar. Um monitor Vintage mantém a luz acesa. Um monitor desligado não abre nada: um clique na tela diz "O monitor está desligado".

O monitor mostra o que o computador está fazendo em cada etapa: o autoteste quando o computador liga, o Menu de Inicialização, o Setup, o instalador e o andamento de uma instalação. Em um rack com um [KVM Switch](jsc:kvm_switch), ele também mostra o seletor dos canais.

Um jogador por vez se senta em um monitor. Se outro jogador estiver nele, você é avisado de que outro jogador está usando o monitor.

> [!DEFINE] KVM
> Teclado, vídeo e mouse: um monitor e um teclado que alcançam várias máquinas.

Um tubo de imagem, no Vintage ou no Legacy, faz um som quando liga ou desliga. Uma tela plana apaga sem nenhum som.

Um monitor de toque se usa onde ele está, sem sentar diante dele. O seu vidro fica rente à moldura, e o seu queixo traz a marca de toque. Mire no vidro e clique com o botão direito para tocar; segure o botão e mova para arrastar; segure parado um segundo para o botão direito. O primeiro toque o acorda: a tela do computador aparece no vidro, e cada toque depois disso chega a ela. Agache para usar o bloco como de costume, e clique na moldura para sentar diante dele.

Um toque em algo que recebe digitação, um campo de texto, um documento ou um prompt, traz a linha de digitação onde fica a linha do chat. O que você digita vai para a tela enquanto digita; Enter manda Enter, e Esc guarda a linha. Com a linha aberta, o mouse continua mirando e o botão direito continua tocando. O Frames 10 e o Frames 11 também põem o seu teclado virtual no vidro: o Frames 10 ao longo do pé da área de trabalho, o Frames 11 flutuando no meio. Ocultar o guarda até o próximo campo que você tocar. Os outros sistemas recebem o texto só pela linha.

Monitores de toque só se juntam a monitores de toque do seu próprio tipo, numa grande tela de toque, uma superfície só.

> [!DEFINE] Monitor de toque
> Um monitor plano que você usa tocando o seu vidro no mundo: o Monitor de Toque do Standard e o Monitor de Toque Advanced.

## Se algo der errado

> [!PROBLEM] Nenhum computador encontrado ao alcance por um Cabo Periférico
> Estenda o cabo periférico até o computador, dentro do alcance da sua era ([](jsc:peripheral_cables)), ou encoste o monitor diretamente no computador.

> [!PROBLEM] O computador não tem saída de vídeo: instale uma placa de vídeo
> Coloque uma placa de vídeo no computador, ou use um processador com gráficos integrados ([](jsc:graphics_cards)).

> [!PROBLEM] As saídas de vídeo do computador estão todas em uso
> Cada placa de vídeo tem seu próprio número de saídas. Adicione uma placa, ou remova um monitor.

> [!PROBLEM] O monitor diz "Memória de vídeo insuficiente" e informa quanto ele precisa.
> Use uma placa de vídeo com mais memória de vídeo, ou menos monitores, ou monitores menores em uma tela unida.

> [!PROBLEM] Sem sinal - o computador está desligado
> Ligue o computador pelo gabinete.

> [!PROBLEM] Sem sinal - o computador está ligado, mas as peças dele não formam um computador: a tela de montagem diz por quê
> Abra o computador e corrija o que a lista TESTES dele aponta ([](jsc:build_refusals)).

> [!PROBLEM] Sem sinal - o computador desativou este monitor
> Ative o monitor novamente ([](jsc:disable_device)).

> [!PROBLEM] O monitor está desligado
> Clique no botão de energia na moldura dele.

> [!PROBLEM] Este rack contém vários computadores - instale um KVM Switch para escolher um
> Instale um KVM Switch no rack ([](jsc:kvm_switch)).

> [!PROBLEM] O rack não contém nenhum computador para mostrar
> Coloque um computador no rack.

> [!PROBLEM] Nenhum computador vinculado
> Ligue o monitor a um computador com um cabo periférico ([](jsc:peripheral_cables)).

> [!PROBLEM] O monitor informa que outro jogador o está usando.
> O outro jogador mantém a tela até sair dela.

> [!SEE] jsc:peripheral_cables, jsc:graphics_cards, jsc:kvm_switch
