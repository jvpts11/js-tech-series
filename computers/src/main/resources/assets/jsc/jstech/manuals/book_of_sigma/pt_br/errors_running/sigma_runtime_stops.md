---
section: errors_running
order: 492
---
# As paradas durante a execução de um programa

Um programa pode parar enquanto é executado, quando encontra um erro que o impede de continuar. Este livro chama esse tipo de término de parada, e há 15 delas. O programa encerra a sua execução de imediato.

> [!DEFINE] parada
> Um erro durante a execução de um programa, que para o programa de imediato e escreve uma única mensagem no seu console.

Quando uma parada acontece, as threads do programa param e o programa libera os seus bloqueios. A mensagem é tudo o que você vê da parada. A maioria das mensagens não diz qual linha do programa a causou.

A tabela abaixo numera as 15 paradas, para que você encontre cada uma. O jogo mostra apenas a mensagem, não o número. Neste capítulo, a memória de um programa é contada em megabytes (MB).

Table: As 15 paradas

| Parada | O que a causa | O que fazer |
| --- | --- | --- |
| 1 | Uma divisão ou um resto por zero, com números inteiros ou com números reais. | Verifique o número pelo qual você divide, antes da divisão. |
| 2 | Um valor é nada onde é preciso um objeto, uma lista, um mapa, um array, uma thread ou um processo. | Certifique-se de que o valor está definido antes de usá-lo. |
| 3 | Um objeto foi descartado e depois usado de novo. | Não use um objeto depois que ele for descartado. |
| 4 | O programa guarda mais memória do que recebeu. | Faça-o guardar menos valores ou valores menores, ou dê a ele mais memória ao iniciar, até 64 MB. |
| 5 | Uma conversão recebe um texto ou um valor do tipo errado. | Verifique o texto que você converte: uma conversão para número precisa de um número, e uma conversão para verdadeiro ou falso precisa de verdadeiro ou falso. |
| 6 | Um número, uma posição ou um tamanho está fora do intervalo permitido. | Use um valor dentro do limite que a mensagem indica. |
| 7 | Um nome está errado para esse tipo de valor: o valor não tem esse membro. | Use um membro que o valor tenha. A mensagem nomeia o valor e o membro. |
| 8 | Um programa chama a rede, mas o computador não está em uma. | Conecte primeiro o computador a uma rede. |
| 9 | A thread libera um bloqueio que não possui. | Adquira o bloqueio antes de liberá-lo. |
| 10 | A máquina não consegue iniciar o programa: não existe executor para esse tipo de arquivo, não há memória livre suficiente, ou o arquivo não pode ser lido ou iniciado. | A mensagem diz o motivo. Compile primeiro um arquivo-fonte, libere memória da máquina ou verifique o arquivo. |
| 11 | Um valor dado a um componente quebra uma regra, ou um computador está desligado. | A mensagem indica a regra. Mantenha o valor dentro dela, e verifique o arquivo ou o computador. |
| 12 | Um método chama a si mesmo sem fim, então as chamadas ficam profundas demais. | Dê um fim à recursão, ou use um laço. A mensagem nomeia o método. |
| 13 | O próprio runtime falhou ao executar um passo do programa. | Isto não é culpa do seu programa. Relate a linha do log do servidor. |
| 14 | Um programa Sigma# lançou uma exceção, e nada a capturou. | Capture-a onde o programa possa continuar. As linhas debaixo da mensagem dizem onde ela foi lançada. |
| 15 | Um ponteiro foi lido ou escrito fora de todo bloco de memória crua: depois do seu fim, num bloco já liberado, ou através de null. | Verifique o índice e o tamanho que você pediu, e use um bloco só até ele ser liberado. |

Nas mensagens abaixo, as palavras entre colchetes angulares representam os nomes e os números que o jogo preenche.

Table: As mensagens das paradas

| Parada | A mensagem como o jogador a vê |
| --- | --- |
| 1 | divisão por zero |
| 1 | dividiu o resto por zero |
| 2 | não há manipulador para chamar |
| 2 | não há lista aqui |
| 2 | não há mapa aqui |
| 2 | não há array aqui |
| 2 | não há objeto onde escrever <field> |
| 2 | não há nada aqui para acessar |
| 2 | não há corpo para executar na thread |
| 2 | não há manipulador para chamar <name> |
| 2 | não há <thing> aqui para <action> |
| 2 | não há processo aqui |
| 2 | não há thread aqui |
| 3 | isto foi descartado e não pode ser usado |
| 4 | sem memória: <live> de <total> bytes estão ativos e <asked> mais foram pedidos |
| 4 | a linha <line> guarda <bytes> bytes, uma dessas partes para cada linha que guarda memória, unidas por ponto e vírgula |
| 5 | isto não é um <type> |
| 5 | não há nada aqui para criar um <type> |
| 5 | '<text>' não é um número |
| 5 | '<text>' não é verdadeiro ou falso |
| 6 | não existe base <base>; um número é escrito em uma base de 2 a 36 |
| 6 | um array não pode ter <count> posições |
| 6 | um programa pode ter <count> janelas abertas ao mesmo tempo |
| 6 | não há posição <place> para começar em um texto de <length> |
| 6 | não há <count> caracteres a partir da posição <place> em um texto de <length> |
| 6 | uma janela contém no máximo <count> widgets |
| 6 | uma janela contém no máximo <count> widgets, contando os que estão dentro de linhas e colunas |
| 6 | uma linha ou uma coluna contém no máximo <count> widgets |
| 6 | uma lista contém no máximo <count> linhas |
| 6 | uma área de desenho contém no máximo <count> traços |
| 6 | um menu contém no máximo <count> entradas |
| 6 | uma visão de abas contém no máximo <count> páginas |
| 6 | uma tabela tem no máximo <count> colunas |
| 6 | uma barra de status mostra no máximo <count> seções |
| 7 | o runtime não responde por <name> |
| 7 | não há <name> para chamar |
| 7 | <name> não tem corpo para executar |
| 7 | <name> não tem <member> |
| 7 | não há <member> para ler aqui |
| 7 | <operator> não se aplica a um número real |
| 7 | <name> não tem <member> para executar |
| 7 | este computador não tem área de trabalho para abrir uma janela |
| 7 | não há <name> para executar na thread |
| 8 | este computador não está em uma rede |
| 9 | esta thread está liberando um bloqueio que não possui |
| 10 | <name>: nada instalado executa um programa desse tipo (compile primeiro um arquivo-fonte) |
| 10 | <name>: <size> MB não cabem em <free> MB de memória livre |
| 10 | <name> não pode executar programas |
| 11 | o valor de um componente aninha no máximo <count> níveis |
| 11 | o valor de um componente tem no máximo <count> partes |
| 11 | o valor de um componente pesa no máximo <count> bytes |
| 11 | um componente guarda valores, não manipuladores |
| 11 | um componente guarda valores, e um <type> não é um |
| 11 | um valor que guarda a si mesmo não pode ser entregue a um componente |
| 11 | '<name>' está fechado |
| 11 | '<name>' foi aberto com "<mode>", que não permite leitura |
| 11 | '<name>' foi aberto com "<mode>", que não permite escrita |
| 11 | '<text>' não é uma lacuna de printf que esta máquina escreve |
| 11 | <name> está desligado |
| 11 | <name> não aceita programas de outros computadores |
| 12 | as chamadas foram <count> níveis de profundidade chamando <name>: um método pode estar chamando a si mesmo sem fim |
| 13 | o runtime não pôde executar esta ação (<exception>) |
| 2 | não há nada aqui para lançar |
| 4 | um bloco de <bytes> bytes é mais do que um bloco pode guardar |
| 6 | uma contagem de bytes não pode ser negativa: <contagem> |
| 14 | uma exceção do tipo <tipo> foi lançada, e nada a capturou, e depois a mensagem dela e uma linha para cada chamada por que ela passou |
| 15 | tentativa de ler ou escrever memória protegida, ou no Sigma as próprias palavras do sistema: Falha de segmentação (imagem do núcleo gravada) no Linux, nos BSDs e no Quince OS X, Este programa executou uma operação ilegal e será fechado no Frames 95, e Desculpe, ocorreu um erro de sistema nos sistemas Quince clássicos |

As paradas 10 e 11 também podem mostrar uma mensagem que a própria máquina escreve, para um barramento, uma interface de fabricação, um arquivo ou uma consulta em [IQL](jsc:iql_getting_started). IQL, a Linguagem de Consulta de Itens, é a linguagem de consulta da rede. Essas mensagens vêm da parte da máquina que executou o passo, por isso não estão na tabela.

A parada 13 é a que não é culpa do seu programa. O runtime falhou, e uma linha vai para o log do servidor com a linha do programa onde aconteceu.

```table
{"caption": "Limites", "heads": ["Limite", "Quantidade"], "rows": [[{"words": "Chamadas em profundidade em uma thread"}, {"words": "1.024 quadros"}], [{"words": "Posições da pilha em uma thread"}, {"words": "1.048.576 posições"}], [{"words": "Memória de um programa, por padrão"}, {"amount": 1, "unit": "MB"}], [{"words": "Memória de um programa, no máximo"}, {"amount": 64, "unit": "MB"}], [{"words": "Aninhamento de um valor dado a um componente"}, {"words": "16 níveis"}], [{"words": "Partes de um valor dado a um componente"}, {"words": "4.096 partes"}], [{"words": "Tamanho de um valor dado a um componente"}, {"words": "32.768 bytes"}], [{"words": "Janelas abertas ao mesmo tempo em um programa"}, {"words": "8 janelas"}], [{"words": "Widgets em uma janela"}, {"words": "256 widgets"}]]}
```

As posições da pilha são contadas como os quadros vezes as posições que cada quadro usa. A memória que você pede fica entre 1 MB e 64 MB.

> [!DEFINE] quadro
> Uma chamada de método ativa em uma thread. Uma thread mantém um quadro para cada chamada que ainda não retornou.

## Se algo der errado

> [!PROBLEM] A mensagem diz divisão por zero.
> Confira o número pelo qual você divide, antes da divisão.

> [!PROBLEM] A mensagem diz sem memória.
> Faça o programa guardar menos valores ou valores menores, ou dê a ele mais memória ao iniciar, até 64 MB.

> [!PROBLEM] A mensagem diz que as chamadas foram profundas demais, e que um método pode estar chamando a si mesmo sem fim.
> Dê um fim ao método que chama, ou use um laço no lugar.

> [!PROBLEM] A mensagem diz que um texto não é um número, nem verdadeiro ou falso.
> O texto que você converte não é um número, nem verdadeiro ou falso. Verifique a entrada.

> [!PROBLEM] A mensagem diz que um número, uma posição ou um tamanho está fora do intervalo, como "uma janela contém no máximo 256 widgets".
> Use um valor dentro do limite que a mensagem indica.

> [!PROBLEM] A mensagem diz que não existe esse membro, ou que o runtime não responde por esse nome.
> O nome está errado para esse tipo de valor. Use um membro que o valor tenha.

> [!PROBLEM] A mensagem diz que este computador não está em uma rede.
> Conecte primeiro o computador a uma rede.

> [!PROBLEM] A mensagem diz que esta thread está liberando um bloqueio que ela não possui.
> Adquira o bloqueio antes de liberá-lo.

> [!PROBLEM] A mensagem diz que nada instalado executa um programa desse tipo.
> Compile primeiro o arquivo-fonte, para que exista uma listagem para executar.

> [!PROBLEM] A mensagem diz que o runtime não pôde executar esta ação.
> Isto não é culpa do seu programa. Relate a linha do log do servidor.

> [!SEE] jsc:sigma_listing_errors, jsc:sigma_compile_errors, jsc:sigma_running
