---
section: sigma_start
order: 407
---
# Seu primeiro programa

Um programa começa em um único lugar, e o compilador precisa encontrar esse lugar. O programa mais simples imprime uma linha e termina. No Frames, o Virtual Studio pode criar um projeto inicial com o seu assistente, e depois uma tecla o executa ([](jsc:virtual_studio)).

O assistente oferece quatro tipos de projeto, e cada um existe nas duas linguagens. Você escolhe o tipo e a linguagem e, em seguida, dá um nome ao projeto.

Table: Os quatro tipos de projeto

| Tipo | O que é |
| --- | --- |
| Aplicativo de Console | Um programa com um Main que roda no terminal de onde foi iniciado, imprime enquanto roda e desaparece quando retorna. |
| Script | Um programa que fica ativo. É configurado uma vez, é chamado repetidamente enquanto roda e é avisado quando é parado. |
| Biblioteca de Classes | Classes que outros projetos compartilham. Não tem início próprio, e é compilada junto com os projetos que a usam. |
| Projeto Vazio | Um projeto sem arquivos, para começar do zero. |

O nome pode usar apenas letras, dígitos e sublinhados, e não pode começar com dígito. O nome vira o [namespace](jsc:sigma_namespaces_using), o nome da classe e o texto que o programa imprime.

O assistente grava o arquivo de projeto, NAME.sgsproj para o Sigma# (NAME.sgproj para o Sigma), e o arquivo-fonte, NAME.sgs para o Sigma# (NAME.sg para o Sigma). Ao compilar, a listagem vai para build/NAME.asm.

> [!DEFINE] arquivo de projeto
> O arquivo de texto em uma pasta de projeto que dá nome ao projeto e lista os seus arquivos-fonte e configurações.

Aqui está o código inicial de um aplicativo de console em Sigma#, em um projeto chamado Hello. O nome Hello é usado para o namespace, para a classe e para o texto que o programa imprime.

```sgs Hello.sgs
using System.IO.*;

namespace Hello;

class Hello {
    static void Main() {
        Console.PrintLine("Hello from Hello");
    }
}
```

O mesmo programa em Sigma fica assim. O Sigma imprime com printf, e você mesmo termina o texto com uma quebra de linha, escrita como uma barra invertida seguida de n.

```sg Hello.sg
using Standard.*;

namespace Hello;

class Hello {
    static void Main() {
        printf("Hello from Hello\n");
    }
}
```

Um programa precisa de exatamente um ponto de início: uma classe que implemente IScript, ou uma classe com um Main estático. Um Main é estático, retorna void ou int, e não recebe nada ou recebe os argumentos como um array de strings. Cada linha using traz uma biblioteca para o programa, para que os seus nomes possam ser escritos de forma curta.

> [!DEFINE] Main
> O método onde um programa de console começa.

> [!DEFINE] IScript
> A interface que um script implementa, com os métodos que o runtime chama nela: OnInit, OnTick e OnDestroy.

> [!DEFINE] using
> Uma linha no topo de um arquivo-fonte que traz uma biblioteca para dentro do programa.

Retornar um int de Main, ou receber os argumentos, exige o Sigma# versão 2. Cada projeto guarda a sua própria versão ([](jsc:sigma_projects)).

## Sua primeira execução no Virtual Studio

1. Abra o assistente Novo Projeto com [[Ctrl]]+[[Shift]]+[[N]] no Virtual Studio.
2. Escolha a opção Aplicativo de Console para o Sigma#, e digite um nome, como Hello.
3. Pressione [[F5]] para compilar o projeto e executá-lo.
4. Leia a linha que o programa imprime no terminal.

O programa imprime Hello from Hello, e desaparece quando o Main retorna. A compilação pela linha de comando é mostrada em [](jsc:sigma_compile).

## Se algo der errado

> [!PROBLEM] Aparece "um programa precisa de exatamente um ponto de início", com a quantidade de inícios encontrados.
> Mantenha uma única classe com um Main estático, ou uma única classe que implemente IScript, e remova as outras.

> [!PROBLEM] A compilação imprime um erro que indica um arquivo e um ponto dentro dele, como hello.sgs(3,5): linha 3, quinto caractere.
> Leia o erro, corrija a linha que ele indica e compile de novo ([](jsc:sigma_errors)).

> [!SEE] jsc:sigma_compile, jsc:sigma_projects
