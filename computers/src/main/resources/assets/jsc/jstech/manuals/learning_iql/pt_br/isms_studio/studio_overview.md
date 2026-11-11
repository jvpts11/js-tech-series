---
section: isms_studio
order: 391
---
# O IQL Server Management Studio

O IQL Server Management Studio (ISMS) é o programa onde você escreve IQL, roda e lê o que a rede responde. Use-o quando quiser consultar a rede à mão.

O estúdio é um programa de área de trabalho. Abra-o pelo ícone na área de trabalho: digitar um comando no prompt não o abre. O console responde "Abra o ISMS pelo ícone dele na área de trabalho."

O estúdio trabalha com o Midsoft IQL Server, o motor que roda no [Mainframe](jsc:mainframes) e lê a sua IQL. Você precisa de uma rede com esse motor antes que o estúdio possa funcionar: [o Midsoft IQL Server](jsc:midsoft_iql_server).

O estúdio roda a partir da era Legacy, e precisa de 128 MB de memória.

## Seu visual em cada era

O visual do estúdio segue a era do seu computador. Todos os visuais têm as mesmas funções. Só a aparência e alguns nomes mudam.

Table: Visual por era

| Era | Visual |
| --- | --- |
| Legacy | O visual de 2000. O nome na janela é "IQL Query Analyzer". |
| Transition | O visual de 2008. |
| Standard | O visual de 2012. |
| Advanced e todas as eras depois dela | O visual de 2022. |

## As partes da janela

Table: Parte da janela

| Parte | O que guarda |
| --- | --- |
| Barra de menus | Os menus Arquivo, Editar, Exibir, Consulta, Ferramentas, Janela e Ajuda. |
| Barra de ferramentas | Os botões Plano, Grade, Texto, Arquivo e Cancelar. Cancelar para a consulta em execução. |
| Pesquisador de Objetos | A árvore da rede, descrita na sua própria entrada. |
| Abas de consulta | Uma aba para cada consulta. Cada aba guarda um script e os seus resultados. |
| Barra de status | O motor, a versão dele, o computador e a hora da última execução. |

O Detalhe do Pesquisador de Objetos, o Monitor de Atividade, o Gerenciador de Modelos e a Referência da IQL abrem ao lado das consultas. O Profiler abre na sua própria janela.

## Configurações e início

O estúdio guarda as suas configurações no arquivo isms.cfg no topo do disco do computador. Ao iniciar, abre uma aba de consulta vazia, a menos que você desligue "Abrir uma nova consulta quando o estúdio começa" em Ferramentas > Opções.

> [!DEFINE] Script
> As instruções de uma aba de consulta. As instruções são separadas por ponto e vírgula e rodam uma de cada vez.

## Se algo der errado

> [!PROBLEM] O estúdio diz "Nenhum Midsoft IQL Server compatível foi encontrado nesta rede."
> Coloque um Mainframe que rode o Midsoft IQL Server nesta rede ([](jsc:midsoft_iql_server)).

> [!PROBLEM] O estúdio diz "<computer> roda <engine>. Use as ferramentas desse motor, ou peça a quem administra o Mainframe que o troque no Gerenciador de Rede."
> Use as ferramentas do motor que está rodando, ou troque o motor no Gerenciador de Rede ([](jsc:network_manager)).

> [!PROBLEM] O estúdio diz "<computer> não tem o Midsoft IQL Server instalado."
> Instale o motor pelos Serviços de Rede do Gerenciador de Rede ([](jsc:network_manager)).

> [!PROBLEM] O estúdio diz "Não conectado: <name>".
> Conecte o estúdio a uma rede cujo Mainframe rode o motor, e tente de novo.

> [!SEE] jsc:what_iql_is, jsc:first_queries
