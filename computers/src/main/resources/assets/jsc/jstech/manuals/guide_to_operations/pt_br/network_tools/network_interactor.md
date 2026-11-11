---
section: network_tools
order: 86
icon: jsc:personal_computer
---
# A janela Rede

A janela Rede alcança o armazenamento da rede e o trabalho dela a partir deste computador. Seu ícone na área de trabalho se chama Rede, e sua janela se chama Interator de Rede. Ela vem em toda área de trabalho, e faz o que o terminal MC-NET faz ([](jsc:mc_net)).

Por ela, você pede itens à rede, tira itens do armazenamento local para o seu inventário, envia itens do armazenamento local de volta à rede e pede à rede que fabrique itens. Os itens que você pede chegam primeiro ao armazenamento local deste computador.

> [!DEFINE] Armazenamento local
> O armazenamento em disco deste computador, em oposição ao armazenamento que a rede guarda.

> [!DEFINE] Requisição
> Pedir à rede que coloque itens no armazenamento local deste computador.

> [!DEFINE] Favorito
> Um item que você favorita, para que ele fique na própria aba Favoritos.

## A janela

Table: Partes da janela Rede

| Parte | O que faz |
| --- | --- |
| Barra de ferramentas | O campo de busca, e os botões Ordenar, Mod e Categoria. |
| Abas | Seis abas, e a aba Favoritos no fim. |
| Grade | Os itens da aba, em uma área de células com uma legenda acima. |
| Painel de detalhes | O item que você escolheu, as suas ações e os seus detalhes. |
| Faixa do inventário | Os slots do seu inventário, na parte inferior da janela. |
| Alças | Dois handles que remodelam a janela: um adiciona colunas à grade, e o outro mostra de 1 a 4 linhas do seu inventário. |
| Barra de status | O estado do Mainframe, as contagens e um medidor de armazenamento. |

Arraste uma alça para mudar o tamanho. A janela lembra o seu tamanho.

## As abas

Table: Abas da janela Rede

| Aba | O que lista | Quando está vazia |
| --- | --- | --- |
| Status | O cartão de rede. | Nunca fica vazia. |
| Local | Itens nos discos deste computador. | Nada nos discos deste computador. |
| Rede | Todos os itens que a rede guarda. | Nada na rede. |
| Fabricação | Itens que a rede pode fabricar a partir dos seus padrões. | Nenhum padrão na rede. |
| Operações | As Operações da rede, as em execução primeiro. | Nenhuma operação na rede. |
| Favoritos | Os itens que você favoritou. | Nada favoritado ainda. |
| Federated | Os itens de uma rede federada com esta, depois da aba Rede. | Nenhuma rede está federada com esta. |

Pressione [[F]] sobre um item, ou sobre a estrela dele nos detalhes, para favoritá-lo. O cartão de status, na aba Status, mostra o estado da rede. O cabeçalho dele diz Esta rede, e depois Mainframe online ou Mainframe offline. Abaixo, um medidor STORAGE mostra o espaço de disco em uso e os itens guardados. Depois vêm as contagens de tipos de item, servidores, itens fabricáveis e favoritos, e a contagem de Operações em execução.

## Requisições e o inventário

Pressione [[Enter]] sobre um item, ou clique nele, para abrir a caixa de diálogo dele. Na aba Rede, Requisitar pede à rede que coloque os itens no armazenamento local deste computador. Na aba Local, Para o Inventário tira itens do armazenamento local e os põe no seu inventário, até onde o inventário tiver espaço. Para a Rede envia itens do armazenamento local à rede, e o que a rede não aceitar volta ao armazenamento local.

O botão Adv abre um painel avançado, onde você escolhe os servidores de onde puxar os itens e para onde eles vão. Fluidos e químicos não podem ir para o seu inventário.

Os botões de quantidade mudam o valor em 1, 10, 100 ou 1.000, para cima ou para baixo, e Máximo usa a maior quantidade permitida. Você também pode digitar números, e [[Backspace]] apaga o último. Com vários itens escolhidos, pergunta-se a quantidade de cada item, ou o que a rede tem dele, se for menos.

O botão Prio define a [prioridade](jsc:priorities) da requisição. Ele passa por LOW, MED-, MED, MED+ e HIGH, e começa no valor padrão, MEDIUM.

## Busca, filtro e ordenação

Pressione [[/]] na grade para ir ao campo de busca. Digite parte de um nome, e a lista mostra os itens cujos nomes combinam. [[Esc]] sai do campo. O botão Ordenar alterna entre A-Z, MOST e LEAST: em ordem alfabética, maior total primeiro ou menor total primeiro. Os botões Mod e Categoria abrem menus que reduzem a lista a um mod, ou a uma categoria de itens.

A busca e os filtros se aplicam à aba em que você está, e não às abas Status ou Operações.

Table: Teclas na grade

| Tecla | O que faz |
| --- | --- |
| `Arrow keys` | Move a célula pontilhada. |
| `Shift` | Com uma seta, estende a seleção. |
| `Enter` | Abre a caixa de diálogo dos itens selecionados. |
| `C` | Abre a caixa de diálogo de fabricação do item da célula, se ele puder ser fabricado. |
| `F` | Marca ou desmarca a seleção como favorita. Se todos estiverem favoritados, todos são desfavoritados. |
| `Ctrl+A` | Seleciona todos os itens da aba. |
| `/` | Move para o campo de busca. |

## Fabricação

A aba Fabricação lista os itens que a rede pode fabricar a partir dos seus [padrões](jsc:pattern_encoders). Um padrão é a receita que a rede conhece, e um [Computador de Fabricação](jsc:crafting_computers) os carrega.

Pressione [[C]], ou [[Enter]] sobre um item fabricável, para abrir a caixa de diálogo de fabricação. Ela mostra um plano: a receita usada, as máquinas e as etapas, e o que cada entrada custa em estoque. Fabricar faz a quantidade inteira, Parcial faz o que pode ser feito agora, e [[Enter]] também faz a quantidade inteira.

Quando um item pode ser fabricado de mais de uma forma, até três cartões de receita ficam lado a lado. [[Left]] e [[Right]] trocam o cartão, e o computador lembra a escolha de cada item. A estimativa, marcada EST, mostra um tempo, ou EST -- enquanto o plano ainda está sendo calculado.

## Atualizando um item

A caixa de diálogo de um item único também tem um botão Atualizar... Ele abre uma janela com três abas: Encantar, Fundir e Reparar. Uma aba fica apagada quando este computador não tem um cartão para ela, ou quando o item não a aceita. Os cartões são as placas de uso pessoal da [Workshop](jsc:workshop). A janela se fecha assim que a rede tiver aceito a atualização.

## Operações

A aba Operações lista as [Operações](jsc:what_operations_are) da rede. As que estão em execução vêm primeiro, depois o log recente, que guarda as últimas 32 Operações. Cada linha mostra o seu estado de forma abreviada:

Table: Estados na aba Operações

| Abreviado | Nome completo |
| --- | --- |
| `done` | Concluída |
| `part` | Concluída (parcial) |
| `fail` | Falhou |
| `run` | Processando |
| `wait` | Aguardando |
| `lock` | Recurso bloqueado |
| `pend` | Pendente |
| `drop` | Descartada |

Abra uma linha para ver quanto da Operação já andou, e as suas partes: uma sub-operação para cada máquina que trabalha nela, ou as suas fontes. Com um Supercomputador, a legenda também mostra quantas fabricações paralelas estão em andamento, em relação ao número que o Supercomputador tem.

## Redes federadas

Quando esta rede está federada com outra por [Computadores Gateway WAN](jsc:wan_gateways), a aba Federated mostra os itens daquela rede numa vista só deles, nunca misturados com os desta rede. O cartão dela diz por qual porta e linha ela é alcançada, o que esta rede pode fazer lá, e o que há nela. Com várias, Próxima rede passa de uma para a outra.

Pedir traz itens daquela rede para o armazenamento local deste computador, e Fabricar lá faz ela fabricar algo, que fica naquela rede. Solte uma pilha na grade dela para enviá-la para lá. A outra rede decide quais dessas coisas permite, no BorderLink do seu próprio gateway, e cada uma atravessa a linha como uma Operation. Na aba Operations, a linha dela diz para qual rede foi, e os detalhes dizem até onde ela chegou.

## Se algo der errado

> [!PROBLEM] O cartão de status diz Mainframe offline.
> Esta janela não consegue alcançar a rede. Coloque o computador em uma rede cujo Mainframe esteja em funcionamento.

> [!PROBLEM] A aba Rede diz Nada na rede.
> Os discos da rede ainda não guardam nada. Coloque itens na rede primeiro.

> [!PROBLEM] A aba Fabricação diz Nenhum padrão na rede.
> Nenhum Computador de Fabricação tem um padrão ainda. Carregue arquivos .craft em um Computador de Fabricação.

> [!PROBLEM] A aba Fabricação lista menos itens do que você espera.
> A aba mostra apenas o que os padrões permitem. Carregue mais padrões em um Computador de Fabricação.

> [!PROBLEM] A caixa de diálogo de fabricação mostra EST --.
> O plano ainda está sendo preparado, ou ainda não existe uma rota. Espere um momento e olhe de novo.

> [!PROBLEM] A aba Federated diz que a outra rede não deixa esta ver os itens dela.
> Quem decide isso é o gateway de lá. Ligue Ver os nossos itens na porta desta rede, no BorderLink daquele gateway.

> [!SEE] jsc:mc_net, jsc:what_a_network_is, jsc:states_and_queues, jsc:crafting_computers
