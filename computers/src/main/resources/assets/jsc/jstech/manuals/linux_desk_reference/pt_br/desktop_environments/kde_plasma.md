---
section: desktop_environments
order: 264
icon: jsc:monitor
---
# KDE Plasma

O KDE Plasma é a área de trabalho do projeto KDE. Ele roda no Linux e no FreeBSD, em hardware Legacy ou mais novo, e o seu pacote se chama kde-plasma. A casa dele é a KDE Guild, uma software house do mundo do mod.

O KDE muda de aparência conforme a era. Em hardware Legacy você vê o KDE 2. Em Transition você vê o KDE 4, com o seu menu Kickoff 4. A partir da era Standard você vê o KDE Plasma.

> [!DEFINE] Kickoff
> O menu iniciar do KDE: um lançador que você abre pelo painel, com uma lista de programas ao lado dos seus lugares.

O painel fica ao longo da parte de baixo da tela. No KDE Plasma, o Kickoff é um lançador de dois painéis: uma coluna de lugares ao lado da lista de programas. Digite nele, e a busca procura em todos os programas. O KDE 4 mostra as abas Favoritos, Aplicativos, Computador, Recentes e Sair.

O KDE dá aos sete programas os seus próprios nomes, como mostra a tabela. Os demais mantêm os nomes que têm em todas as áreas de trabalho ([](jsc:desktop_program_names)).

Table: Nomes próprios do KDE

| Programa | Nome no KDE Plasma |
| --- | --- |
| Arquivos | Dolphin |
| Editor | Kate |
| Prompt de Comando | Konsole |
| Calculadora | KCalc |
| Este Computador | Centro de Informações |
| Configurações | Configurações do Sistema |
| Ajuda | Central de Ajuda |

Para instalá-lo, use o formulário do seu sistema em [](jsc:install_a_desktop). No Debian e no Ubuntu, o comando é apt install kde-plasma. As linhas de inicialização o chamam de kde plasma, como em "Iniciando o gerenciador de exibição kde plasma".

> [!DEFINE] Gerenciador de exibição
> A etapa de inicialização que inicia uma área de trabalho gráfica. As linhas de inicialização informam o nome da área de trabalho que ela inicia.

## Se algo der errado

> [!PROBLEM] O gerenciador de pacotes diz "kde-plasma exige hardware Legacy ou posterior".
> O computador é do tipo Vintage. Use um computador Legacy ou mais novo.

> [!PROBLEM] O gerenciador de pacotes diz "requisitos não atendidos (hardware ou espaço livre em disco)".
> O Plasma ocupa 256 MB de disco. Libere algum espaço em disco e tente de novo.

> [!SEE] jsc:install_a_desktop, jsc:desktop_program_names, jsc:eras
