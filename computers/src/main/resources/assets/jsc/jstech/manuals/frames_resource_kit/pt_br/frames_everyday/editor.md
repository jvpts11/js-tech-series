---
section: frames_everyday
order: 221
---
# O Editor

O Editor escreve e altera arquivos de texto. Cada arquivo que você abre ganha uma aba própria, e você pode ter até 8 arquivos abertos ao mesmo tempo. Ele mostra os números das linhas, para você encontrar o seu lugar, e a linha e a coluna do cursor na barra de status.

Linhas longas podem quebrar na linha seguinte da tela. Você pode desligar a quebra de linha, para que cada linha fique em uma única linha da tela.

## Teclas

Table: Teclas no Editor

| Tecla | O que faz |
| --- | --- |
| `Ctrl+S` | Salva o arquivo. Um arquivo sem nome pede um nome primeiro. |
| `Ctrl+O` | Abre um arquivo pela janela de arquivos do sistema. |
| `Ctrl+P` | Imprime o arquivo. |
| `Ctrl+F` | Abre a barra de busca. |
| `Ctrl+G` | Abre a barra de busca no modo Ir para linha. |
| `Ctrl+N` | Abre uma nova aba com um arquivo novo. |
| `Ctrl+W` | Fecha a aba atual. |
| `Ctrl+Tab` | Troca para a próxima aba. |
| `Enter` | Enquanto a barra de busca estiver com o foco, executa a ação dela. |
| `Esc` | Enquanto a barra de busca estiver aberta, fecha-a e volta ao texto. |

## A barra de busca

Pressione [[Ctrl]]+[[F]] para abrir a barra de busca, que oferece Substituir, Próximo e Substituir tudo. Pressione [[Ctrl]]+[[G]] para usar a mesma barra e ir para uma linha: a barra então oferece Ir para e Ir.

## Salvar e fechar

1. Pressione [[Ctrl]]+[[S]] para salvar o arquivo com o nome dele.
2. Se o arquivo não tiver nome, o Editor diz "Salvar Como primeiro". Use Salvar Como para dar um nome a ele e depois salve.
3. Para fechar uma aba com alterações não salvas, pressione [[Ctrl]]+[[W]] e leia a mensagem.
4. Salve com [[Ctrl]]+[[S]], ou pressione [[Ctrl]]+[[W]] de novo para descartar as alterações.

> [!DEFINE] Aba
> Um arquivo aberto no Editor. O Editor mantém até 8 abas ao mesmo tempo.

> [!DEFINE] Quebra de linha
> A quebra de linha é a forma como uma linha longa continua na linha seguinte da tela. Você pode desligá-la.

> [!DEFINE] Barra de busca
> A barra onde você digita o que procurar. Ela também executa a ação Ir para linha.

## Se algo der errado

> [!PROBLEM] O Editor diz "Salvar Como primeiro".
> O arquivo ainda não tem nome, então [[Ctrl]]+[[S]] não pode salvá-lo. Dê um nome a ele primeiro.

> [!PROBLEM] O Editor diz "Não é possível salvar um arquivo .dat".
> Um arquivo .dat é somente leitura: você pode lê-lo, mas o Editor não pode salvá-lo.

> [!PROBLEM] O Editor informa que o arquivo é longo demais para salvar, com o seu tamanho e o limite em caracteres.
> Encurte o texto. Um arquivo comporta até 32.767 caracteres.

> [!PROBLEM] O Editor informa que o caminho é longo demais para salvar, com o seu tamanho e o limite em caracteres.
> Use um nome mais curto ou menos pastas. Um caminho comporta até 160 caracteres.

> [!PROBLEM] O Editor diz "Apenas 8 arquivos ao mesmo tempo".
> Feche uma aba com [[Ctrl]]+[[W]] primeiro.

> [!PROBLEM] O Editor avisa sobre alterações não salvas quando você fecha uma aba, e pede que você feche de novo para descartá-las.
> Salve com [[Ctrl]]+[[S]], ou feche a aba de novo para descartar as alterações.

> [!PROBLEM] O Editor diz que um arquivo "é grande demais para abrir aqui".
> O Editor deixa o arquivo como está, então nada se perde.

> [!SEE] jsc:file_explorer
