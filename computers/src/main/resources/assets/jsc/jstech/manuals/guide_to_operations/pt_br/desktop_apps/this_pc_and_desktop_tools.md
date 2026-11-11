---
section: desktop_apps
order: 80
icon: jsc:monitor
---
# Este Computador, Arquivos, o Editor e Configurações

Alguns programas vêm em toda área de trabalho, com os nomes que o seu sistema lhes dá. Esta página cobre quatro deles: Este Computador, que mostra a máquina e as suas unidades; Arquivos, que guarda os seus arquivos; o Editor, que escreve texto; e Configurações, que altera o modo como a área de trabalho e a máquina se comportam. A janela de Rede tem a sua própria página ([](jsc:network_interactor)).

## Este Computador

Este Computador é a página da própria máquina. Um cartão mostra o nome do computador, o tipo, a era, o sistema e a rede. Abaixo estão os dispositivos e unidades: cada disco com uma barra que mostra o quanto ele está cheio, e cada unidade vinculada com o que há nela. Para um instalador em uma unidade vinculada, Este Computador mostra o nome do programa que ele instalaria.

Table: Partes do Este Computador

| Parte | O que faz |
| --- | --- |
| Cartão | O nome, o tipo, a era, o sistema e a rede do computador. |
| Dispositivos e unidades | Cada disco, e cada unidade vinculada com os seus botões Instalar, Abrir e Ejetar. |
| Hardware | As peças instaladas no computador. |
| Programas | Os programas instalados, em forma de grade. |

Este Computador existe no Frames, no Linux e no FreeBSD. No UNIX, ele é substituído por [Informações da Estação de Trabalho](jsc:cde_workstation_info).

## Arquivos

Arquivos é o programa para os seus arquivos. Sua barra de ferramentas tem botões de voltar, avançar e subir. Uma trilha de endereço mostra a pasta em que você está, e cada parte dela é clicável. Uma caixa de pesquisa fica ao lado.

A árvore de unidades à esquerda guarda os atalhos de acesso rápido e cada volume, cada um com a sua letra. Um disco removível tem um controle de ejetar. A lista do meio mostra os arquivos da pasta, com um ícone para cada tipo, e você a ordena por nome, tipo ou tamanho.

Table: Partes de Arquivos

| Parte | O que faz |
| --- | --- |
| Barra de ferramentas | Voltar, avançar e subir. |
| Trilha de endereço | A pasta em que você está, com cada parte clicável. |
| Árvore de unidades | Atalhos e cada volume, com a sua letra. |
| Lista | Os arquivos da pasta, ordenáveis por nome, tipo ou tamanho. |
| Barra de status | Ao longo da parte inferior da janela. |

Clique duas vezes em uma pasta para abri-la. Clique duas vezes em um instalador para executar a sua configuração, ou em um arquivo de texto para abri-lo no Editor. O menu do botão direito oferece recortar, copiar, colar, renomear, excluir, novo arquivo, nova pasta e propriedades, e deixa acinzentado o que o volume proíbe. Um arquivo compactado abre com o 67ark ([](jsc:archiver)).

## O Editor

O Editor edita texto puro. Ele não é um ambiente de desenvolvimento integrado (IDE). Não tem destaque de sintaxe, nem autocompletar, nem compilador, então ele apenas edita o texto.

Table: Partes do Editor

| Parte | O que faz |
| --- | --- |
| Números de linha | Ao lado de cada linha do texto. |
| Localizar e substituir | Encontra um texto e o altera. |
| Ir para linha | Pula para a linha que você indicar. |
| Quebra de linha | Quebra as linhas longas na largura da janela. Você pode desligar. |
| Barra no rodapé | A linha e a coluna em que você está. |

Você pode ter vários arquivos abertos ao mesmo tempo. Abrir e Salvar Como usam a janela de arquivos do próprio sistema. O Editor é o padrão para arquivos de texto, então um arquivo .txt abre ali.

## Configurações

Configurações é o painel de controle. Uma lista à esquerda guarda as suas páginas, e a página que você escolher preenche o painel à direita. Uma mudança entra em vigor assim que você a faz.

As oito páginas são Personalizar, Sistema, Rede, Armazenamento, Tela, Programas, Som e Usuários. A página Programas tem a linha Desinstalar um programa.

1. Abra Arquivos a partir da área de trabalho.
2. Clique duas vezes na pasta que contém o arquivo de texto.
3. Clique duas vezes no arquivo de texto. Ele abre no Editor.

## Se algo der errado

> [!PROBLEM] O Editor diz que o arquivo é grande demais para abrir aqui.
> Divida o texto em arquivos menores e abra cada um.

> [!PROBLEM] Um item do menu do botão direito está acinzentado.
> O volume não permite essa mudança. Escolha outra ação, ou outro volume.

> [!SEE] jsc:programs, jsc:archiver, jsc:help_on_a_computer
