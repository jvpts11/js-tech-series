---
section: prompt_basics
order: 234
---
# Caminhos, unidades e as opções

Um caminho é o endereço de um arquivo ou de uma pasta: os nomes das pastas, com uma barra invertida entre eles, começando por uma letra de unidade. C:\\DOS é a pasta que o MC-DOS procura os seus programas por padrão. As regras dos caminhos estão em [o prompt, as unidades e os caminhos](jsc:dos_prompt); esta página mostra as formas que o prompt entende.

> [!DEFINE] Pasta
> Um lugar com nome em um disco, que guarda arquivos e outros lugares do mesmo tipo.

> [!DEFINE] Unidade
> Um disco que o computador consegue ler, identificado por uma letra. C: é o disco do sistema.

Leia um caminho da esquerda para a direita. As formas abaixo são as que o prompt entende.

Table: Formas de um caminho

| Forma | O que significa |
| --- | --- |
| `..` | Sobe uma pasta. |
| `\DOS` | Começa na raiz da unidade atual, a pasta de cima, e não na pasta em que você está. |
| `D:\WORK` | Começa na raiz da unidade D:, qualquer que seja a pasta em que você estava. |
| `D:` | Vai para a raiz da unidade D:, e não para a pasta que você usou ali antes. |
| `.` | É ignorado, então não acrescenta nada ao caminho. |

O prompt mostra onde você está: a pasta atual, depois um sinal de maior. No topo da unidade C: ele mostra C:\\>. Depois que você digitar cd C:\\DOS, ele mostra C:\\DOS>.

## Trocando de unidade

Digite uma letra de unidade e dois-pontos, e nada mais, e o prompt vai para essa unidade. Digite D: para ir à unidade D:. As unidades que você pode usar são as conectadas ao computador.

```session
{"lines": [{"prompt": "C:\\DOS> ", "text": "cd", "typed": true}, {"text": "C:\\DOS"}], "look": "jsc:guide/terminal/dos", "title": "MC-DOS"}
```

Sem palavras depois dele, cd imprime a pasta em que você está, como C:\\DOS.

## Opções

Uma opção muda o que o comando faz. Você a escreve do jeito que aquele comando a recebe, como /y. Cada comando lê apenas as opções que declara; o próprio shell não conhece nenhuma delas.

> [!DEFINE] Opção
> Uma palavra de opção depois do nome de um comando, escrita do jeito que aquele comando a recebe.

Table: Opções da família DOS

| Opção | Comando e o que ele faz |
| --- | --- |
| `/y` | format: confirma que você quer apagar a unidade. |
| `/PID` | taskkill: informa a tarefa a parar, pelo número dela. |
| `/EVERY:` | at: executa a tarefa somente nos dias nomeados depois dela. |
| `/DELETE` | at: tira uma tarefa da lista. |
| `/I` | find: ignora letras maiúsculas. |
| `/V` | find: mostra as linhas que não contêm o texto. |
| `/R` | sort: inverte a ordem das linhas. |
| `/LOCAL` | interac: deixa o que ele pega neste computador, e não nas suas mãos. |
| `/S:COUNT` | interac: ordena uma listagem pela quantidade que há. |

## Pedindo ajuda

Table: Dois jeitos de pedir ajuda

| Forma | Onde funciona |
| --- | --- |
| `--help` | Depois do nome do comando, em todas as famílias: MC-DOS, MC-NET, Frames e os sistemas Unix. |
| `/?` | Depois do nome do comando, somente na família DOS: MC-DOS e as edições do Frames. |

As duas formas mostram a página do comando, o mesmo texto que help <command> mostra. Você pode ler a mesma página aqui, nas páginas de comandos deste livro.

## Aspas e nomes

Palavras entre aspas duplas ficam juntas como uma só palavra. O comando find recebe o seu texto desse jeito, então find "oak log" procura a frase inteira.

Na família DOS, um nome entre sinais de porcentagem, como %USERNAME%, é substituído pelo seu valor antes de o comando ser executado. Os nomes embutidos são HOSTNAME, COMPUTERNAME, USER, USERNAME, OS, CD (o prompt) e PATH. Você dá um valor a um nome com set ([](jsc:shell_verbs)).

Um asterisco (\*) em um nome é passado ao comando do jeito que está. O comando decide o que o asterisco vai corresponder.

Se a palavra que você digitar não for um comando, o shell procura um programa com esse nome no PATH. O PATH é a lista de pastas em que ele procura. No MC-DOS ele vem da linha PATH de C:\\AUTOEXEC.BAT, e C:\\DOS é o padrão.

## Se algo der errado

> [!PROBLEM] O sistema não pode encontrar o caminho especificado.
> Confira o nome da pasta com dir. Uma letra de unidade sozinha, como C:, leva à raiz daquela unidade, e não à pasta em que você estava.

> [!PROBLEM] O sistema não pode encontrar o arquivo especificado.
> O arquivo não está na pasta em que você está. Digite o caminho completo, como C:\\DOS\\NAME, ou vá até a pasta dele primeiro com cd.

> [!PROBLEM] MYFILE.TXT: arquivo não encontrado
> O nome do arquivo está errado, ou o arquivo está em outra pasta. Confira o nome com dir.

> [!PROBLEM] D:\\ O sistema não pode encontrar a unidade especificada.
> Nenhuma unidade D: está conectada a este computador. Escolha uma unidade que o computador tenha, ou conecte uma unidade a ele.

> [!PROBLEM] D:\\ O dispositivo não está pronto.
> A unidade está sem disco. Coloque um disco na unidade e tente de novo.

> [!PROBLEM] Um comando digitado com /? no MC-NET ou em um sistema Unix não mostra a sua ajuda.
> Digite --help depois do nome do comando nesses sistemas.

> [!SEE] jsc:dos_prompt, jsc:finding_a_command
