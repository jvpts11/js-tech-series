---
section: freebsd_system
order: 333
---
# Instalando o FreeBSD, página por página

bsdinstall é o instalador próprio do FreeBSD. Ele faz as suas perguntas em páginas, sempre na mesma ordem, e depois copia o sistema para o disco que você escolher. Esta página percorre cada uma delas, para que você saiba o que ela pergunta antes de responder.

> [!DEFINE] bsdinstall
> O instalador próprio do FreeBSD, que faz as suas perguntas em páginas, em uma ordem fixa.

Você precisa de um computador com um disco e uma mídia de instalação: o CD do FreeBSD (disco compacto), colocado em uma Unidade de CD. Um Mirror na rede é opcional. Com um, o instalador também oferece áreas de trabalho e busca pacotes nele.

> [!DEFINE] Coleção de Ports
> A árvore de ports, a pasta de receitas que compila programas a partir do código-fonte.

## As páginas em ordem

Table: As páginas do bsdinstall

| Página | O que você faz |
| --- | --- |
| Boas-vindas | Pressione Install para começar. |
| Definir Hostname | Mantenha o nome que o campo mostra, ou digite outro. |
| Seleção de Distribuição | Marque ports para instalar a Coleção de Ports. Ela vem marcada por padrão. |
| Particionamento | Escolha o disco e como ele é usado. |
| Seleção de Mirror | Escolha o Mirror de onde buscar, ou none para instalar só pelo CD. |
| Extração de Arquivos | Espere enquanto o sistema é copiado. |
| Configuração do Sistema | Marque os serviços para iniciar na inicialização. cron vem marcado por padrão, e sshd não. |
| Área de trabalho | Só quando um Mirror responde: escolha uma área de trabalho, ou None. |
| Concluído | Retire o CD e reinicie. |

A página Boas-vindas tem o botão Install, e todas as outras páginas têm OK. O bsdinstall não tem botão Back: cada página oferece só Cancel e o próprio botão, então, para mudar uma resposta anterior, pressione Cancel e inicie o instalador de novo.

## Escolhendo o disco

A página Particionamento lista uma linha por disco. Os discos se chamam ada0, ada1 e assim por diante, na ordem das unidades. Cada linha oferece um modo de usar esse disco. Auto (UFS) coloca o UNIX File System (UFS) no disco inteiro.

Table: As escolhas para cada disco

| Escolha | Quando você a vê |
| --- | --- |
| Auto (UFS): usar o disco inteiro | O disco está vazio. |
| Auto (UFS): usar o disco inteiro, apagando o que há nele | O disco tem um sistema ou arquivos. Os arquivos dele são apagados. |
| Beside: dividir o disco com o sistema dele | O disco tem um sistema e espaço livre ao lado dele. |

> [!DEFINE] UFS
> O sistema de arquivos do disco que o bsdinstall grava no disco inteiro.

Antes de o instalador apagar um disco, ele pergunta "Apagar ada0?" e diz o que vai apagar. A caixa de diálogo diz "Isso não pode ser desfeito." Pressione Y para apagar, ou N para cancelar.

> [!WARNING] Apagar um disco remove todos os arquivos dele, e isso não pode ser desfeito.

## Os serviços e o Mirror

A página Seleção de Mirror pergunta de onde pkg e ports buscam. A linha mirror://mainframe aparece só quando um Mirror responde ([](jsc:mirror)). Caso contrário, você instala pelo CD.

Quando a cópia termina, as suas respostas viram configurações. Marcar cron liga o cron, e marcar sshd liga o remote. Ambas são configurações que você pode mudar depois ([](jsc:rc_services)). Com a caixa de ports marcada e um Mirror em uso, toda a árvore de ports é copiada para o disco agora. Sem um Mirror, o instalador deixa uma pasta /usr/ports vazia para o portsnap preencher.

No fim, o computador reinicia no sistema novo assim que você tirar o CD.

> [!DEFINE] Conjunto de distribuição
> Um pacote compactado de arquivos do sistema, como base.txz ou kernel.txz.

## Se algo der errado

> [!PROBLEM] A página Área de trabalho não mostra nenhuma área de trabalho na lista.
> Nenhum Mirror respondeu, ou a era deste computador é mais antiga que o mínimo da área de trabalho. Conecte-se a uma rede com um Mirror, ou use um computador mais novo.

> [!PROBLEM] A página Seleção de Mirror não tem nenhuma linha mirror://.
> Nenhum Mainframe nesta rede roda o serviço Mirror. Um Mainframe precisa rodá-lo ([](jsc:mirror)).

> [!PROBLEM] Você quer mudar uma resposta, e não há botão Back.
> Pressione Cancel, depois inicie o instalador de novo.

> [!SEE] jsc:disks, jsc:installing, jsc:rc_services, jsc:mirror
