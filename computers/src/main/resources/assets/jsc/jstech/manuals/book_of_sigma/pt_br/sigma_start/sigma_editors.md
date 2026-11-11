---
section: sigma_start
order: 410
---
# Os editores

Você escreve um programa em Sigma em um editor de texto. Cinco programas abrem código Sigma: o Editor, o vim, o Virtual Studio, o Virtual Studio Code e o Exposure. Eles diferem em onde rodam e em quanto ajudam você.

Table: Onde cada editor roda

| Programa | Onde roda | Disco e memória |
| --- | --- | --- |
| Editor | As áreas de trabalho de Frames, Linux, FreeBSD e UNIX | Vem pré-instalado |
| vim | O terminal de todos os sistemas | 8 MB de disco, 4 MB de memória |
| Virtual Studio | Somente Frames | 512 MB de disco, 256 MB de memória |
| Virtual Studio Code | Frames, Linux e FreeBSD | 128 MB de disco, 48 MB de memória |
| Exposure | Frames, Linux e FreeBSD | 192 MB de disco, 64 MB de memória |

O vim assume o terminal de onde foi iniciado, então funciona onde não há área de trabalho. O Virtual Studio, o Virtual Studio Code e o Exposure são instalados como qualquer outro programa ([](jsc:programs)). Os três precisam de um computador Legacy ou posterior. No Frames, também precisam do Frames XP ou posterior.

O Virtual Studio é a oficina completa do Sigma# em uma só janela, e roda somente no Frames. Ele trabalha com projetos e soluções, e você pode ler sobre ele em [](jsc:virtual_studio).

> [!DEFINE] solução
> Uma pasta que agrupa vários projetos, com um arquivo que os lista.

Table: Teclas do Virtual Studio

| Tecla | O que faz |
| --- | --- |
| `F5` | Start: compila e executa o arquivo aberto, ou o projeto de inicialização quando há uma solução aberta. |
| `Ctrl+Shift+B` | Compila, em sequência, cada projeto da solução que gera uma listagem, e mostra a saída na aba Output; sem solução, compila o arquivo aberto. |
| `Ctrl+Shift+N` | Abre o assistente de Novo Projeto. |
| `Ctrl+S` | Salva o arquivo. |
| `Ctrl+F` | Procura um texto no arquivo. |
| `Ctrl+G` | Vai até uma linha. |
| `Ctrl+W` | Fecha a aba. |

O Virtual Studio Code é um editor leve com o terminal integrado, então você pode escrever, compilar e executar sem sair dele. Roda no Frames, no Linux e no FreeBSD.

> [!DEFINE] Virtual Studio Code
> Um editor leve com um terminal integrado, onde você escreve, compila e executa um programa em uma única janela.

Table: Teclas do Virtual Studio Code

| Tecla | O que faz |
| --- | --- |
| `F5` | Run File: compila o arquivo, depois digita sigma run no terminal, com a listagem na pasta de compilação. |
| `Ctrl+Shift+B` | Build File: salva o arquivo, depois digita o comando do compilador, que escreve a sua listagem na pasta de compilação ao lado do arquivo. |
| `Ctrl+P` | Vai até um arquivo pelo nome. |
| `Ctrl+S` | Salva o arquivo. |

O Exposure compila todos os programas de uma pasta de uma vez, então uma mudança mostra o que mais ela quebrou. Ele não oferece nada enquanto você digita, e essa é a sua troca.

> [!DEFINE] Exposure
> Um editor que compila todos os programas de uma pasta de uma vez, para você ver o que mais a sua mudança quebrou.

Table: Teclas do Exposure

| Tecla | O que faz |
| --- | --- |
| `F5` | Faz o levantamento da pasta: pede todos os programas dela de uma vez e lista os problemas. |
| `F6` | Salva, compila e executa o arquivo no terminal, com a listagem escrita ao lado do arquivo-fonte. |

A janela do Exposure mostra PROBLEMS, a contagem dos problemas que o levantamento encontrou, e WHAT THE COMPILER SAID, as próprias palavras do compilador sobre o problema. O botão Rebuild all compila tudo de novo.

Table: Qual editor abre qual arquivo

| Arquivo | Aberto primeiro por |
| --- | --- |
| `.sgs and .sg` | Virtual Studio Code, depois Virtual Studio, Exposure e o Editor |
| `.asm` | O runtime do Sigma, que o executa, depois o Virtual Studio Code e os outros |
| `.sgsproj, .sgproj and .sln` | Virtual Studio, depois o Editor |
| `.txt, .cfg, .csv, .cmd and .log` | O Editor |

## Se algo der errado

> [!PROBLEM] O Virtual Studio Code diz que o arquivo não é um programa para compilar.
> Abra um arquivo que termina em .sg ou .sgs, depois pressione [[Ctrl]]+[[Shift]]+[[B]] ou [[F5]].

> [!PROBLEM] "Nenhum projeto para iniciar: nenhum gera uma listagem" aparece no Virtual Studio.
> Escolha um projeto de inicialização na solução, um que gere uma listagem.

> [!PROBLEM] O Exposure diz que o arquivo não é um programa para executar.
> Abra um arquivo-fonte do Sigma, que termina em .sg ou .sgs.

> [!SEE] jsc:sigma_projects, jsc:sigma_compile
