---
section: isms_studio
order: 393
---
# O Pesquisador de Objetos

O Pesquisador de Objetos é a árvore ao lado do estúdio. O nó do topo é a rede, e as pastas ficam abaixo dele. Quando você abre uma pasta, vê o que a rede guarda.

Cada pasta guarda um tipo de coisa. Clique com o botão direito num nó e você verá o menu desse tipo de nó. A tabela seguinte nomeia as pastas.

Table: Pastas da rede

| Pasta | O que guarda |
| --- | --- |
| Tabelas | As tabelas da rede. Cada tabela tem as suas colunas abaixo dela. |
| Visões | As visões que você salvou com CREATE VIEW. |
| Procedimentos Armazenados | Os procedimentos que você salvou com CREATE PROCEDURE, em Programabilidade. |
| Armazenamento | Um servidor para cada servidor de armazenamento da rede. |
| Gerenciamento | O Índice, os Bloqueios e o Registro de Operações. Na aparência Legacy esta pasta se chama Manutenção. |
| Agente de Automação | A pasta Jobs. |
| Jobs | As tarefas que você criou com CREATE JOB. Uma tarefa pausada mostra (pausada) ao lado do nome dela. |

A nota Index informa o estado de saúde do Índice. A tabela seguinte mostra cada nota e a instrução que a corrige.

Table: Notas do índice

| Nota | O que significa | Correção |
| --- | --- | --- |
| saudável | Consistente. | nenhuma |
| desatualizado | Eventos quentes deixaram entradas que o índice ainda não releu. | `REINDEX;` |
| fragmentado | Remoções em massa deixaram entradas fantasmas apontando para armazenamento que não existe mais. | `VACUUM;` |

Cada entrada em Bloqueios mostra o item e a quantidade, como um nome de item com x e um número.

> [!DEFINE] Índice
> O registro da rede sobre o que cada armazenamento guarda. ANALYZE, REINDEX e VACUUM o mantêm atualizado.

> [!DEFINE] Bloqueio
> Uma retenção de itens, para que outras Operações não possam pegá-los.

Table: O menu de cada nó

| Nó | Itens do menu | Instrução que executa |
| --- | --- | --- |
| Rede (a raiz) | Nova Consulta, Atualizar, Iniciar, Parar e Reiniciar o Motor, Propriedades | nenhuma |
| Tabela | Consultar as 100 Primeiras Linhas, Contar Linhas, Gerar Script da Tabela como, Propriedades, Atualizar | `QUERY <table> LIMIT 100;` |
| Visão | Consultar Exibição, Gerar Script da Visão como, Propriedades | `QUERY <view>;` |
| Procedimento Armazenado | Executar Procedimento Armazenado, Modificar, Gerar Script do Procedimento como, Propriedades | `EXEC <name>;` |
| Servidor | Consultar o Que Ele Guarda | `QUERY items WHERE server = "<name>";` |
| Índice | Analisar | `ANALYZE;` |
| Índice | Reindexar | `REINDEX;` |
| Índice | Vacuum | `VACUUM;` |
| Bloquear | Desbloquear | nenhuma |
| Registro de Operações | Consultar o Registro | `QUERY operations LIMIT 100;` |
| Job | Iniciar Trabalho, Pausar Trabalho, Excluir | nenhuma |
| Agente de Automação | Novo Trabalho | nenhuma |

Gerar Script da Tabela como e Gerar Script da Visão como abrem um submenu. Cada item escreve uma instrução numa nova aba de consulta, ou a copia para a área de transferência. Excluir numa tarefa pede confirmação antes: "Excluir o trabalho <name>?"

Os Detalhes do Pesquisador de Objetos (F7) listam os filhos do nó escolhido em colunas: Nome, Tipo, Linhas, Colunas, O que guarda, Última alteração, Estado, Gatilho, Quantidade e Versão. Ele tem uma caixa de pesquisa e conta os seus itens. Propriedades (F4) lista os dados do nó escolhido. Uma tarefa em execução aparece ali como em execução.

## Se algo der errado

> [!PROBLEM] Os detalhes dizem "Nada está escolhido no pesquisador."
> Clique num nó da árvore e depois abra os detalhes de novo com F7.

> [!PROBLEM] Parar e Reiniciar o motor precisam de uma conexão com a rede.
> Conecte o estúdio a uma rede antes. O estúdio diz "Não conectado: <name>" até isso acontecer.

> [!SEE] jsc:maintenance_analyze_vacuum_reindex, jsc:iql_jobs
