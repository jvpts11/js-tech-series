---
section: frames_everyday
order: 220
---
# Arquivos e o Explorador de Arquivos

A janela Arquivos é onde você vê os arquivos dos seus discos e pode abrir, copiar, mover, renomear e excluir esses arquivos. No Frames 10, a mesma janela se chama Explorador de Arquivos. Nas outras edições, ela se chama Arquivos.

A árvore à esquerda agrupa os locais: Acesso rápido, Armazenamento, Rede e Compartilhamentos. O Acesso rápido guarda os atalhos. Os Compartilhamentos são as pastas de rede que você pode acessar a partir desta janela. A Lixeira também é um local: um arquivo solto nela é excluído.

## A barra acima da lista

O Frames 7 mostra uma barra de comandos acima da lista. O Frames 10 mostra uma faixa de opções com as abas Arquivo, Página Inicial e Exibir. O grupo Layout da faixa escolhe entre Detalhes e Ícones grandes. As outras edições não mostram barra. Pressione [[Esc]] para fechar uma faixa de opções aberta.

## Chaves

Table: Chaves na janela Arquivos

| Chave | O que faz |
| --- | --- |
| `Backspace` | Sobe uma pasta. |
| `Enter` | Abre a linha selecionada. |
| `Up, Down` | Move a seleção. |
| `Delete` | Exclui a linha selecionada, ou todas as linhas que você selecionou arrastando. |
| `F2` | Renomeia a linha selecionada. |
| `Ctrl+C` | Copia a linha selecionada. |
| `Ctrl+X` | Recorta a linha selecionada, a menos que ela esteja em uma mídia somente leitura. |
| `Ctrl+V` | Cola o que você copiou ou recortou. |
| `Ctrl+A` | Seleciona todos os arquivos e pastas da listagem. |
| `Esc` | Fecha a faixa de opções, se houver uma aberta. |

## Movendo um arquivo

1. Clique no arquivo para selecioná-lo.
2. Pressione [[Ctrl]]+[[X]] para recortá-lo, ou [[Ctrl]]+[[C]] para copiá-lo.
3. Abra a pasta desejada e pressione [[Ctrl]]+[[V]].

Clique com o botão direito em uma linha para ver o menu dela: Abrir, Executar, Abrir com, Escolher outro programa, Recortar, Copiar, Colar, Renomear, Excluir, Novo, Ejetar, Atualizar, Extrair aqui e Compactar para, com o nome do arquivo compactado.

## Excluindo e locais somente leitura

No disco do sistema, Excluir move o arquivo para a Lixeira. Em uma mídia ou em um compartilhamento de rede, Excluir remove o arquivo de vez, depois que você confirmar. Uma mídia somente leitura recusa Recortar e Excluir, e a barra diz "mídia somente leitura".

Um arquivo .dat é diferente: é uma lista somente leitura dos itens guardados em um disco. Você pode lê-la, mas ninguém pode alterá-la à mão. A janela diz que, para alterar esses itens, você usa o Interator de Rede ([](jsc:interactor_screen)).

> [!DEFINE] Seleção por arrasto
> Arrastar um retângulo de seleção sobre várias linhas para selecionar todas de uma vez.

> [!DEFINE] Arquivo .dat
> Um arquivo somente leitura que lista os itens guardados em um disco. Você pode lê-lo, mas não pode alterá-lo à mão.

## Se algo der errado

> [!PROBLEM] Um arquivo em uma mídia sumiu, e não há Lixeira para trazê-lo de volta.
> Excluir em uma mídia é definitivo. Copie o arquivo para o disco do sistema antes, se você puder precisar dele.

> [!PROBLEM] A janela diz "Este arquivo faz parte do instalador e não pode ser alterado, copiado nem excluído."
> Execute o programa de instalação para instalá-lo, como diz a mensagem.

> [!PROBLEM] A janela diz "Este arquivo é impossível de modificar, criar, excluir ou alterar manualmente; use o Interactor de Rede para isso."
> Leia o arquivo. Para alterar os itens dele, use o Interator de Rede ([](jsc:interactor_screen)).

> [!PROBLEM] Recortar e Excluir são recusados em uma mídia somente leitura.
> Copie o arquivo com [[Ctrl]]+[[C]] e cole-o em um disco que você possa alterar.

> [!PROBLEM] Abre uma caixa de diálogo com o título "Não foi possível fazer isso".
> Leia a mensagem na caixa de diálogo: ela diz o que foi recusado.

> [!SEE] jsc:drives, jsc:interactor_screen
