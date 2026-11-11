---
section: operations
order: 132
icon: jsc:mainframe
---
# O que é uma Operação

Em muitos mods de armazenamento, o armazenamento funciona como um serviço que está sempre lá. Você coloca um disco em uma unidade, e ela entrega o que estiver nele. Isso funciona, mas faz o sistema inteiro parecer um grande baú onde você põe e tira coisas.

J's Computers não funciona assim. Aqui a rede não se limita a servir você: os computadores dela trabalham para concluir cada coisa, e cada pedaço desse trabalho é uma Operação.

> [!DEFINE] Operação
> Uma ação que a rede executa, enfileirada e depois rodada ao longo dos ticks do jogo.

Então, o que é uma Operação? É uma ação que a rede está executando. Seus nomes vêm do SQL (Structured Query Language), a linguagem que os bancos de dados falam. Quando você tira itens do armazenamento, não os busca em um disco com as próprias mãos. Você envia à rede um SELECT, e a rede o executa.

O pedido entra em uma fila. O Mainframe o pega, encontra onde estão os itens e os move até você tão rápido quanto o hardware dele permite. É por isso que o hardware do Mainframe importa tanto ([](jsc:mainframes)).

> [!DEFINE] fila
> Uma faixa no Mainframe que move os itens de uma Operação de cada vez. Mais filas significam mais Operações ao mesmo tempo.

A maioria das Operações passa pelas mesmas etapas. Uma Operação espera numa fila, roda ao longo dos ticks do jogo (20 ticks fazem um segundo) e depois termina em um dos oito estados ([](jsc:operation_states)).

Uma Operação cronometrada não termina na hora. Um SELECT primeiro leva um momento para encontrar os itens e, depois, os transmite ao longo dos ticks. Por padrão, esse momento é de 10 ticks em um disco rígido, 3 em uma unidade de estado sólido e 1 em uma unidade non-volatile memory express (NVMe).

> [!DEFINE] índice
> A lista própria da rede do que cada um de seus servidores guarda, mantida atualizada para que a rede saiba onde está cada item.

Algumas Operações se concluem de imediato, na hora: a manutenção dos índices (ANALYZE, REINDEX e VACUUM), LOCK, UNLOCK e DROP.

As Operações precisam de um Mainframe. Todo computador da rede é ligado por cabo de dados a um Mainframe, que executa o trabalho e mantém a lista do que está rodando ([](jsc:what_a_network_is)).

## Se algo der errado

> [!PROBLEM] A resposta diz "a rede não tem um Mainframe em execução".
> O Mainframe está desligado, ou não está nesta rede. Ligue-o, ou junte-o à rede ([](jsc:first_network)).

> [!PROBLEM] O Mainframe não tem um sistema operacional instalado, então recusa novas Operações.
> Instale um sistema operacional nele ([](jsc:installing)).

> [!SEE] jsc:operations_catalogue, jsc:iql
