---
section: shell_basics
order: 249
---
# A árvore de pastas e caminhos

O Unix mostra todo o armazenamento de um computador como uma única árvore de pastas. A árvore começa na pasta raiz, escrita /, e cada arquivo fica em algum lugar dentro dela.

Por baixo dos panos, o armazenamento mantém o modelo DOS (Disk Operating System) de letras de unidade, e o shell faz a tradução. O disco do sistema é C: e é a pasta raiz. Outro disco aparece como uma pasta dentro de /media no Linux e no FreeBSD, ou dentro de /mnt no UNIX System V.

Table: Pastas que você usa

| Pasta | Linux e FreeBSD | UNIX System V |
| --- | --- | --- |
| Sua pasta pessoal | `/home/player` | `/usr/player` |
| Sua área de trabalho, cujos arquivos ela mostra como ícones | `/home/player/Desktop` | `/usr/player/Desktop` |
| Seus Documentos | `/home/player/Documents` | `/usr/player/Documents` |
| As outras unidades, uma pasta cada | `/media` | `/mnt` |

Você nomeia uma pasta ou um arquivo com um Caminho. O primeiro caractere diz ao shell por onde começar.

Table: Como um caminho começa

| O que você digita | O que significa |
| --- | --- |
| `/x/y` | Um caminho absoluto: começa na pasta raiz e nomeia o endereço inteiro. |
| `x/y` | Um caminho relativo: começa na pasta em que você está. |
| `~` | Sua pasta pessoal. A forma ~/x é uma pasta dentro dela. |
| `..` | A pasta acima da que você está. |
| `/media/d/x` | O arquivo x na unidade D:, no Linux e no FreeBSD. No UNIX System V, o mesmo arquivo é /mnt/d/x. |

Comandos que citam um disco usam o nome do dispositivo. O disco do sistema é sda1, a unidade D: é sdb, a unidade E: é sdc, e as seguintes avançam uma letra por vez.

> [!DEFINE] Caminho
> O endereço de um arquivo ou pasta: os nomes das pastas, com barras entre eles.

## Se algo der errado

> [!PROBLEM] Não consigo encontrar meu arquivo.
> Execute pwd para ver a pasta em que você está. Um caminho que começa com barra parte da pasta raiz, não da sua pasta.

> [!PROBLEM] O comando df mostra o dispositivo com "(sem mídia)" depois dele.
> Coloque um disco nessa unidade e depois execute df de novo ([](jsc:file_commands)).

> [!SEE] jsc:shell_prompt, jsc:file_commands
