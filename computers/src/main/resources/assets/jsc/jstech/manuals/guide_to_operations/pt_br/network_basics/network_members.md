---
section: network_basics
order: 95
icon: jsc:server_rack
---
# Membros, e como a rede os vê

Nem todo bloco que encosta em um cabo é membro da rede. A rede classifica cada bloco que consegue ver em uma de três categorias, conforme o que ela pode fazer com ele.

Um bloco da Categoria A carrega a rede, mas não é membro dela, do mesmo jeito que um cabo. Um bloco da Categoria B é um que a rede consegue ler quanto ao seu estado, mas nunca lhe dá trabalho. Um bloco da Categoria C é onde o trabalho acontece: só estes recebem trabalho do Mainframe.

Table: As três categorias

| Categoria | Tem identidade | Recebe Operações | Exemplos |
| --- | --- | --- | --- |
| A | Não | Não | Cabos, o Rack de Servidores |
| B | Sim | Não | nenhum |
| C | Sim | Sim | Mainframe, Servidor, Subframe, Computador Pessoal, Computador de Fabricação |

O Rack de Servidores é um recipiente passivo. Ele guarda os servidores, e os servidores são os membros: cada um tem a sua própria identidade de rede e recebe trabalho ([](jsc:server_racks)).

Um membro é um bloco com identidade de rede, ou seja, um bloco da Categoria B ou C. A rede consegue dar-lhe um nome, lê-lo e, para a Categoria C, enviar-lhe trabalho. Você não precisa decorar as categorias para montar uma rede; a tabela diz o que cada bloco faz.

> [!DEFINE] Categoria de rede
> A regra da rede para um bloco: a Categoria A carrega a rede, a Categoria B é lida por ela, e a Categoria C executa as suas Operações.

> [!DEFINE] Identidade de rede
> A identidade de uma rede, que a distingue de todas as outras redes. Um Mainframe ligado a um segmento adota a rede desse segmento.

## Se algo der errado

> [!PROBLEM] Um computador não está na rede.
> O comando net diz "não está em uma rede". O cabo dele não chega ao Mainframe. Siga os cabos dele até o Mainframe, e verifique se nenhum trecho é mais longo que o seu alcance ([](jsc:data_cables)).
