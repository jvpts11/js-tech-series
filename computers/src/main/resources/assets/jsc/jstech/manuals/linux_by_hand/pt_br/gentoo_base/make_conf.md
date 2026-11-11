---
section: gentoo_base
order: 320
---
# make.conf e MAKEOPTS

O Portage lê um arquivo de configurações antes de compilar qualquer programa: /etc/portage/make.conf, no sistema novo. O sistema base vem com um. Ele guarda as opções do compilador, o idioma das mensagens de compilação, e a configuração que decide quantas compilações rodam ao mesmo tempo.

A sua unidade central de processamento (CPU) tem vários núcleos. Um núcleo é uma das suas unidades de processamento, e cada uma consegue rodar o seu próprio trabalho ao mesmo tempo.

> [!DEFINE] núcleo
> Uma das unidades de processamento dentro de um processador, cada uma capaz de rodar o seu próprio trabalho ao mesmo tempo.

Table: Linhas do make.conf no sistema base

| Linha | O que faz |
| --- | --- |
| `COMMON_FLAGS="-O2 -pipe"` | As opções do compilador para todos os programas. -O2 pede programas mais rápidos, e -pipe torna o próprio compilador mais rápido. |
| `LC_MESSAGES=POSIX.utf8` | Escreve as mensagens de compilação em inglês. |

O arquivo base não tem uma linha MAKEOPTS. Sem ela, cada compilação roda uma compilação por vez, o que é lento em uma máquina com vários núcleos. Adicione a linha você mesmo.

> [!DEFINE] MAKEOPTS
> A configuração em make.conf que diz quantas compilações rodam ao mesmo tempo.

## Adicionando a linha

1. Digite nano -w /mnt/gentoo/etc/portage/make.conf e pressione [[Enter]].
2. Adicione uma linha MAKEOPTS="-j4" sua, e deixe as outras linhas como estão.
3. Salve o arquivo e feche o nano.

Você também pode adicionar a linha em um único comando, de dentro do sistema novo: digite echo 'MAKEOPTS="-j4"' >> /etc/portage/make.conf e pressione [[Enter]].

O número segue -j. A contagem vem da linha MAKEOPTS que não está comentada, e uma linha que começa com # não define nada. Mais compilações ao mesmo tempo do que núcleos não traz ganho nenhum, então a compilação usa o menor dos dois números.

Table: Quantas compilações rodam ao mesmo tempo

| A linha no make.conf | Núcleos | Compilações ao mesmo tempo |
| --- | --- | --- |
| nenhuma | qualquer | 1 |
| `MAKEOPTS="-j4"` | 8 | 4 |
| `MAKEOPTS="-j16"` | 4 | 4 |
| `MAKEOPTS="-j"` | qualquer | 1 |
| `MAKEOPTS="-j0"` | qualquer | 1 |

Um -j sem número conta como nenhuma configuração, e uma contagem abaixo de 1 vira 1 compilação. Se você escrever várias linhas MAKEOPTS, a última é que conta.

## Se algo der errado

> [!PROBLEM] As compilações são lentas, mesmo em uma máquina potente.
> O arquivo base não tem uma linha MAKEOPTS, então cada compilação roda uma compilação por vez. Adicione uma, como mostrado acima.

> [!PROBLEM] Sua linha MAKEOPTS não faz nada.
> A linha começa com #, o que a torna um comentário. Remova o # do início da linha.

> [!PROBLEM] O nano diz "<name> é um diretório".
> O nome que você deu é uma pasta. Digite o nome completo do arquivo: /mnt/gentoo/etc/portage/make.conf.

> [!SEE] jsc:package_tree_and_world, jsc:building_from_source
