---
section: dos_programs
order: 206
---
# DOSSHELL e suas teclas

O MC-DOS Shell é um programa de menus para os arquivos do sistema. Você vê as pastas e os arquivos, copia, move ou apaga, e inicia programas com as teclas, sem digitar cada comando. Cada ação do seu menu Arquivo executa um dos próprios comandos do MC-DOS.

Digite dosshell no prompt para abri-lo. Ele roda somente no MC-DOS, ocupa a tela inteira e abre na pasta raiz de C:.

1. Digite dosshell no prompt e pressione [[Enter]].
2. Pressione [[Tab]] para ir de uma área à seguinte, e as setas do teclado para se mover dentro de uma área.
3. Pressione [[Enter]] sobre um programa para iniciá-lo como tarefa.
4. Segure [[Alt]] e pressione [[Tab]] para escolher a próxima tarefa, e solte [[Alt]] para trocar para ela.
5. Pressione [[F3]] para sair do shell, quando nenhuma tarefa estiver listada.

Table: As áreas da tela

| Área | O que mostra |
| --- | --- |
| Árvore de Diretórios | As pastas do disco em que você está. |
| Unidades | As unidades da máquina, exibidas somente quando há alguma. |
| Main | Os arquivos da pasta escolhida. |
| Lista de Tarefas Ativas | As tarefas que você iniciou, exibidas enquanto o Alternador de Tarefas estiver ativo. |

Table: As visões

| Visão | O que mostra |
| --- | --- |
| Listas de Programas/Arquivos | A árvore e os arquivos, acima dos programas e das tarefas, do jeito que o shell abre. |
| Lista Única de Arquivos | A árvore e a lista de arquivos, até a parte de baixo da tela. |
| Lista de Programas | Somente os programas e as tarefas. |

Listas Duplas de Arquivos e Todos os Arquivos respondem "Não disponível nesta máquina." Selecionar Entre Diretórios faz o mesmo.

A lista de programas tem um grupo chamado Main. Ele contém o Prompt de Comando, que inicia um prompt de comando como tarefa, e o Sigma quando o Sigma está instalado. Ele contém os grupos [Programas] e [Utilitários de Disco]. [Programas] lista os programas que você instalou, exceto o Prompt de Comando.

Table: Teclas no shell

| Tecla | O que faz |
| --- | --- |
| `Tab` | Vai para a próxima área; Shift com Tab volta. |
| `Arrows` | Move-se dentro de uma área. |
| `Page Up` | Sobe uma página na lista. |
| `Page Down` | Desce uma página na lista. |
| `Home` | Vai para o primeiro item. |
| `End` | Vai para o último item. |
| `Enter` | Abre o item escolhido, ou inicia um programa como tarefa. |
| `F10` | Abre o menu Arquivo. |
| `Alt+F` | Abre o menu Arquivo. |
| `Alt+O` | Abre o menu Opções. |
| `Alt+V` | Abre o menu Exibir. |
| `Alt+T` | Abre o menu Árvore. |
| `Alt+H` | Abre o menu Ajuda. |
| `F3` | Sai do shell, o que é recusado enquanto houver uma tarefa listada. |
| `Alt+F4` | Sai do shell, como faz o F3. |
| `F5` | Lê o disco novamente. |
| `Shift+F5` | Redesenha a tela. |
| `F7` | Move o arquivo selecionado. |
| `F8` | Copia o arquivo selecionado. |
| `F9` | Mostra o conteúdo do arquivo selecionado, que Esc fecha. |
| `Shift+F9` | Abre um Prompt de Comando como tarefa. |
| `Del` | Apaga o arquivo selecionado, ou a pasta vazia selecionada na árvore. |
| `Alt+Tab` | Troca para a próxima tarefa; Shift+Alt+Tab volta. |
| `Escape` | Sai de um grupo da lista de programas, ou fecha um menu. |
| `+` | Na árvore: mostra as pastas dentro da pasta selecionada. |
| `*` | Na árvore: mostra todas as pastas abaixo da pasta selecionada. |
| `-` | Na árvore: recolhe as pastas abaixo da pasta selecionada. |

Table: O menu Arquivo

| Item | Tecla | O que faz |
| --- | --- | --- |
| Abrir | `Enter` | Inicia um programa ou abre a pasta escolhida. |
| Exibir Conteúdo do Arquivo | `F9` | Mostra o arquivo, como faz o comando TYPE. |
| Mover..., com nomes de origem e destino | `F7` | Move um arquivo, como faz o MOVE. |
| Copiar..., com nomes de origem e destino | `F8` | Copia um arquivo, como faz o COPY. |
| Apagar..., com uma pergunta quando a Confirmação estiver ativa | `Del` | Apaga o arquivo, como faz o DEL, ou a pasta, como faz o RMDIR. |
| Renomear..., com um novo nome | nenhuma | Dá outro nome ao arquivo, como faz o REN. |
| Criar Diretório..., com a pasta pai e um nome | nenhuma | Cria uma pasta, como faz o MKDIR. |
| Sair | `F3` | Sai do shell e volta para o prompt. |

O menu Opções contém Confirmação, que pergunta antes de apagar, e Ativar Alternador de Tarefas. O Alternador de Tarefas mantém cada programa que você inicia como tarefa, para que você possa trocar entre eles. Mostrar Informações exibe o arquivo selecionado, a pasta dele e o disco dele.

Utilitários de Disco contém Formatar. A caixa de diálogo dele tem um campo que começa com o texto "a:". Substitua o texto pela unidade desejada e acrescente /y, como em D: /y. Sem /y, Formatar exibe o seu aviso e não apaga nada.

Imprimir envia o arquivo selecionado para uma impressora ([](jsc:printers)). Ele fica acinzentado quando nenhuma impressora está ligada.

A linha de baixo mostra as teclas: F10=Ações, Shift+F9=Prompt de Comando e, enquanto o Alternador de Tarefas estiver ativo, Alt+Tab=Próxima Tarefa.

> [!DEFINE] MC-DOS Shell
> O programa de menus do MC-DOS para os seus arquivos e programas, aberto com o comando dosshell.

> [!DEFINE] tarefa
> Um programa ou um Prompt de Comando que roda a partir do shell e fica guardado na Lista de Tarefas Ativas.

```command
id: jsc:command/mc_dos/dosshell
```

## Se algo der errado

> [!PROBLEM] Enter em um arquivo não faz nada.
> Somente os arquivos que terminam em .EXE, .COM e .BAT são iniciados pelo shell. Pressione [[F9]] para ler um arquivo de texto.

> [!PROBLEM] A tela diz: Você não pode sair do MC-DOS Shell com programas na Lista de Tarefas Ativas; encerre esses programas primeiro.
> Encerre cada tarefa e depois pressione [[F3]]. As tarefas são os programas listados na Lista de Tarefas Ativas.

> [!PROBLEM] A tela diz: Nenhum arquivo corresponde ao especificador de arquivo.
> O padrão em Opções de Exibição de Arquivos não mostra nenhum arquivo. Altere-o, por exemplo para \*.\*, para ver todos os arquivos.

> [!PROBLEM] A tela diz: Unidade não está pronta.
> A unidade não tem disco. Coloque um disco nela ou escolha outra unidade.

> [!PROBLEM] A tela diz: Não disponível nesta máquina.
> O shell não oferece essa opção. Use os comandos do prompt para fazer a mesma tarefa.

> [!SEE] jsc:dos_msd, jsc:dos_help
