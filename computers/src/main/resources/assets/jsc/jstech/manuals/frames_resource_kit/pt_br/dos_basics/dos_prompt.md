---
section: dos_basics
order: 203
---
# O prompt, as unidades e os caminhos

O MC-DOS espera você no prompt, a linha em que você digita um comando e pressiona [[Enter]]. O prompt mostra onde você está: a pasta em que está, seguida de um sinal de maior que. Na raiz do disco de sistema ele mostra C:\\>, e em uma pasta chamada SYSTEM ele mostra C:\\SYSTEM>. Um comando age sobre a pasta que o prompt mostra, a menos que você indique outro lugar.

Todo disco e toda unidade tem uma letra de unidade: uma letra e dois-pontos, como C:. Digite a letra e os dois-pontos para mudar de unidade, por exemplo D:. Você então fica na pasta raiz dessa unidade. O MC-DOS não guarda uma pasta atual para cada unidade, então uma unidade não lembra onde você estava.

Table: As letras de unidade

| Letra | Qual unidade é |
| --- | --- |
| `C:` | O disco de sistema, ou o primeiro disco de dados quando não há disco de sistema. |
| D: e as seguintes | Os discos de dados nos compartimentos de disco, na ordem dos compartimentos; o primeiro é D: quando há disco de sistema. |
| Depois dos discos | Os leitores, as baias da Estação de Dock e as unidades da frente do gabinete, cada um mantendo sua letra. |
| `A: and B:` | Nunca. A máquina não tem unidade A: nem unidade B:. |

As unidades removíveis são as que você liga ao computador ([](jsc:drives)). Os discos de dados são os discos nos seus compartimentos ([](jsc:disks)).

Table: Digitado no prompt

| Digitado | O que significa |
| --- | --- |
| `D:` | Muda para a unidade D: e sua pasta raiz. |
| `D:DIR` | A pasta DIR na raiz da unidade D:. |
| `\` | A pasta raiz da unidade atual. |
| `\DIR` | A pasta DIR na raiz da unidade atual. |
| `..` | A pasta acima; na pasta raiz ele continua no mesmo lugar, sem erro. |

Um caminho é o endereço completo de um arquivo ou de uma pasta: sua unidade, suas pastas e seu nome. Um caminho pode unir as pastas com uma barra invertida ou uma barra, então C:\\DOS\\README.TXT e C:/DOS/README.TXT nomeiam o mesmo arquivo.

Quando você digita apenas uma palavra, o MC-DOS a trata primeiro como um comando interno, e depois como um programa. Ele procura o programa na pasta em que você está, depois nas pastas do caminho de pesquisa, a lista guardada na configuração PATH.

Para executar um programa que está em outra pasta, digite seu caminho completo, como C:\\DOS\\HELLO.

Os comandos para pastas são dir, cd, mkdir e rmdir. dir lista uma pasta, cd mostra a pasta atual ou a muda, mkdir cria uma pasta e rmdir remove uma vazia. [Trabalhar com arquivos e pastas](jsc:working_with_files) explica todos eles.

Digite o nome de um comando, um espaço e /? para ler sua página. Digitar help dir abre a mesma página ([](jsc:dos_help)).

> [!DEFINE] Prompt
> A linha em que o MC-DOS aguarda um comando. Ela mostra a pasta atual e um sinal de maior que.

> [!DEFINE] Letra da unidade
> O nome que um disco ou uma unidade tem no MC-DOS: uma letra e dois-pontos, como C:.

> [!DEFINE] Caminho
> O endereço completo de um arquivo ou de uma pasta: sua unidade, suas pastas e seu nome.

> [!DEFINE] Caminho de pesquisa
> A lista de pastas em que o MC-DOS procura um programa quando você digita somente o nome do programa.

## Se algo der errado

> [!PROBLEM] O prompt mostra "comando não encontrado: HELLO".
> Nenhum comando ou programa com esse nome está na pasta em que você está nem no caminho de pesquisa. Verifique a grafia, ou digite o caminho completo, como C:\\DOS\\HELLO. [Como encontrar um comando](jsc:finding_a_command) explica o resto.

> [!PROBLEM] O prompt diz E:\\ O sistema não consegue encontrar a unidade especificada.
> A máquina não tem unidade com essa letra. Digite uma das letras da tabela acima.

> [!PROBLEM] O prompt diz "D:\\ O dispositivo não está pronto."
> A unidade não tem disco. Coloque um disco ou uma mídia na unidade ([](jsc:drives)).

> [!PROBLEM] O prompt diz nenhum disco de sistema ou sistema operacional instalado.
> O computador não tem um disco de sistema para iniciar. Coloque um disco em um compartimento de disco, ou instale o MC-DOS em um deles ([](jsc:dos_install)).

> [!PROBLEM] O prompt diz O sistema não consegue encontrar o caminho especificado.
> A pasta não existe. Liste a pasta com dir, depois digite o nome de novo.

> [!PROBLEM] O prompt diz O diretório não está vazio.
> Apague os arquivos da pasta primeiro, depois remova a pasta com rmdir.

> [!SEE] jsc:working_with_files, jsc:paths_and_drives
