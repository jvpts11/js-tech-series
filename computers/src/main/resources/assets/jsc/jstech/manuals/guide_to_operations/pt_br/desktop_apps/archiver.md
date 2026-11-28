---
section: desktop_apps
order: 83
icon: jsc:cd_rom
---
# 67ark

O 67ark empacota muitos arquivos em um Arquivo que pesa menos, e também os tira de volta. É assim que um disco pequeno consegue render mais: o Arquivo ocupa menos espaço que os arquivos que guarda.

> [!DEFINE] Arquivo
> Um Arquivo que guarda outros arquivos, compactado para pesar menos. Termina em .ark.

> [!DEFINE] Empacotar
> Colocar arquivos em um Arquivo, que os guarda em menos espaço.

> [!DEFINE] Extrair
> Copiar arquivos de volta para fora de um Arquivo.

A janela faz uma de duas coisas, e diz qual delas você está fazendo. Ou um Arquivo está aberto e o seu conteúdo é listado, ou você está montando um Arquivo a partir de arquivos que escolheu no disco.

Table: Colunas da lista do Arquivo

| Coluna | O que mostra |
| --- | --- |
| Nome | O nome do arquivo dentro do Arquivo. |
| Tamanho | O tamanho do arquivo, em bytes, quilobytes ou megabytes. |
| Economia | A porcentagem em que o arquivo compactado é menor que os arquivos originais. |

1. Abra um Arquivo, ou comece um novo com Novo.
2. Aperte Adicionar e escolha os arquivos que você quer empacotar.
3. Aperte Empacotar. O servidor faz o empacotamento, então a economia de disco é real.
4. Aperte Extrair para os arquivos escolhidos, Extrair tudo para todos os arquivos, ou Tirar para uma pasta.

Depois de empacotar, a janela pergunta se deve apagar os arquivos originais ou mantê-los.

## No prompt

O 67ark também funciona no prompt de comando, com as letras do 7-Zip. Um programa pode empacotar os próprios arquivos assim.

Table: O 67ark no prompt

| Comando | O que faz |
| --- | --- |
| 67ark a logs.ark ops.txt | Empacota ops.txt em logs.ark. Se logs.ark já existe, o arquivo é acrescentado a ele. |
| 67ark x logs.ark | Tira todos os arquivos onde o prompt está. Dê uma pasta depois para tirá-los lá. Um arquivo que já está lá fica como está. |
| 67ark l logs.ark | Lista os arquivos do pacote com os tamanhos. |

O pacote comprime os arquivos juntos, então só o pacote inteiro tem um tamanho comprimido. A lista o mostra ao lado do arquivo quando o pacote tem um só, e na linha do total nos outros casos.

## Se algo der errado

> [!PROBLEM] 67ark diz que esse arquivo não é um Arquivo.
> Abra apenas um arquivo que termina em .ark.

> [!PROBLEM] 67ark diz que um Arquivo guarda no máximo 512 arquivos.
> Empacote os arquivos em dois Arquivos.

> [!SEE] jsc:programs, jsc:this_pc_and_desktop_tools
