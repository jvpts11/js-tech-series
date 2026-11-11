---
section: frames_help_and_studio
order: 226
---
# Virtual Studio

O Virtual Studio é a oficina dos programas Sigma#, em uma única janela. Você abre uma solução ou uma pasta, escreve o código-fonte, compila-o e inicia o programa. A própria linguagem é explicada em O Livro do Sigma ([](jsc:book_of_sigma)).

É um programa instalável, para o Frames XP ou uma versão mais recente. A instalação exige 512 MB de espaço livre em disco, e ele retém 256 MB de memória enquanto estiver aberto.

## Abrindo o seu trabalho

Table: Os cartões da Janela Inicial

| Cartão | O que abre |
| --- | --- |
| Abrir um projeto ou uma solução | Um arquivo .sln nesta máquina. |
| Abrir uma pasta local | Qualquer pasta de programas. |
| Abrir um arquivo | Um arquivo .sgs ou .sg, sem projeto. |

O Virtual Studio abre arquivos .sln, .sgproj, .sgsproj, .sg, .sgs e .asm.

## Compilando e iniciando

1. Pressione [[Ctrl]]+[[Shift]]+[[B]] para compilar a solução.
2. Leia o resultado: ele diz Compilação bem-sucedida, ou Falha na compilação com o número de erros.
3. Pressione [[F5]] para compilar o projeto de inicialização e executá-lo no terminal.

```table
{"caption": "Chaves no Virtual Studio", "heads": ["Chave", "O que faz"], "rows": [[{"literal": "Ctrl+Shift+N"}, {"words": "Abre o assistente Novo Projeto."}], [{"literal": "Ctrl+Shift+B"}, {"words": "Compila a solução."}], [{"literal": "Ctrl+S"}, {"words": "Salva o arquivo."}], [{"literal": "Ctrl+F"}, {"words": "Procura texto."}], [{"literal": "Ctrl+G"}, {"words": "Vai para uma linha."}], [{"literal": "F5"}, {"words": "Compila o projeto de inicialização e o executa."}], [{"literal": "Ctrl+W"}, {"words": "Fecha a aba atual."}], [{"literal": "Ctrl+."}, {"words": "Implementa uma interface."}], [{"literal": "Ctrl+`"}, {"words": "Abre o terminal, pronto para digitar."}], [{"literal": "Ctrl+Space"}, {"words": "Mostra as complementações, se estiverem ativadas."}]]}
```

A lista do que pode vir depois de um nome mostra quanto cada chamada custa ao programa, antes de você escrever a linha. O Livro do Sigma explica os editores ([](jsc:sigma_editors)).

> [!DEFINE] Solução
> Uma pasta que agrupa projetos. Ela tem um arquivo que termina em .sln.

> [!DEFINE] Projeto
> Os arquivos de código-fonte de um programa, e o arquivo de projeto que diz o que compilar.

> [!DEFINE] Modelo
> Um ponto de partida para um novo projeto, oferecido pelo assistente Novo Projeto.

> [!DEFINE] Compilação
> Transformar o código-fonte em um programa. Iniciar compila o projeto de inicialização e depois o executa.

## Se algo der errado

> [!PROBLEM] O Virtual Studio não pode ser instalado no Frames 95.
> Ele exige o Frames XP ou uma versão mais recente ([](jsc:frames_xp)).

> [!PROBLEM] O disco tem menos de 512 MB de espaço livre quando você o instala.
> Libere 512 MB de espaço em disco e depois instale-o.

> [!PROBLEM] Um balão diz "Pouca memória".
> A memória passou de nove décimos. Feche outros programas, ou adicione memória, antes que o sistema fique sem memória e caia ([](jsc:frames_memory)).

> [!PROBLEM] O resultado diz Falha na compilação.
> Corrija os erros que a compilação informa e pressione [[Ctrl]]+[[Shift]]+[[B]] de novo.

> [!SEE] jsc:book_of_sigma, jsc:sigma_editors
