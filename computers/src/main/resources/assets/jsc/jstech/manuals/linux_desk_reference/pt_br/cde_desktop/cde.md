---
section: cde_desktop
order: 268
icon: jsc:monitor
---
# CDE

A CDE, o Common Desktop Environment, é a área de trabalho das estações de trabalho Unix, e a única que o UNIX tem. Ela roda em UNIX, FreeBSD e Linux. É leve: precisa de hardware Legacy ou mais recente, 32 MB de disco e 16 MB de memória enquanto roda.

A CDE não tem login nem logout. Não há ninguém para fazer login, então você nunca vê uma tela de login. Ela começa logo de imediato, com "Iniciando a área de trabalho" na sua tela de abertura.

Onde as outras áreas de trabalho têm um menu Iniciar, uma barra de tarefas e uma Central de Ações, a CDE tem uma faixa só: o Front Panel, ao longo do centro inferior da tela. Não tem menu Iniciar, barra de tarefas nem Central de Ações. Também não lista as janelas abertas, porque a CDE nunca fez isso: uma janela que você guarda vira um ícone no seu espaço de trabalho.

> [!DEFINE] Front Panel
> A faixa de controles da CDE na parte de baixo da tela, com a troca de espaço de trabalho no meio.

O Front Panel guarda os seus controles da esquerda para a direita: Relógio, Calendário, Gerenciador de Arquivos, Editor de Texto, Gerenciador de Estilos, Aplicativos, Lata de Lixo e Visualizador de Ajuda. A maioria deles abre alguma coisa, como lista [](jsc:cde_front_panel).

Um controle com uma pequena seta na cabeça abre um subpainel: uma bandeja que sobe do Front Panel. Um subpainel fica aberto até que você pressione a sua seta de novo, e apenas um fica aberto por vez.

> [!DEFINE] Subpainel
> Uma bandeja que sobe de um controle do Front Panel e fica aberta até que a sua seta seja pressionada de novo.

## Espaços de trabalho

A CDE tem quatro espaços de trabalho, chamados One, Two, Three e Four. Cada um é uma tela separada da mesma área de trabalho, e cada um pode ter o seu próprio plano de fundo ([](jsc:cde_style_manager)).

> [!DEFINE] Espaço de trabalho
> Uma de quatro telas separadas da mesma área de trabalho. Uma janela fica em uma delas, ou em todas.

## Janelas

Cada janela tem uma barra de título no estilo Motif. O botão à sua esquerda abre o menu da janela: Restaurar, Minimizar, Maximizar, Rebaixar, Ocupar Área de Trabalho..., Ocupar todos os espaços de trabalho e Fechar. Um duplo clique nesse botão fecha a janela sem menu.

> [!DEFINE] Motif
> O estilo de janela da CDE: uma barra de título, com um botão de menu à sua esquerda.

Para instalar a CDE, use o formulário do seu sistema em [](jsc:install_a_desktop). No Debian e no Ubuntu, é apt install cde.

## Se algo der errado

> [!PROBLEM] Você procura um Menu Iniciar, e não há nenhum.
> A CDE não tem Menu Iniciar. Abra Aplicativos no Front Panel para encontrar os seus programas.

> [!PROBLEM] O gerenciador de pacotes diz "cde precisa de hardware Legacy ou mais recente".
> O computador é um Vintage. Use um computador Legacy ou um mais novo.

> [!SEE] jsc:cde_front_panel, jsc:cde_style_manager, jsc:cde_application_manager, jsc:cde_workstation_info, jsc:cde_trash_can, jsc:install_a_desktop
