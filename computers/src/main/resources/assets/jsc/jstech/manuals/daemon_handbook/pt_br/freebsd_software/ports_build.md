---
section: freebsd_software
order: 338
---
# Compilando ports: make, e a compilação 10% mais leve

Você compila um port na própria pasta, com o comando make, que lê a receita do port. A compilação processa o código-fonte do programa neste computador. Leva um tempo, e o resultado é feito para este computador.

> [!DEFINE] Compilação
> Compilar o código-fonte do programa neste computador.

> [!DEFINE] Distfile
> O arquivo de código-fonte que uma compilação baixa do Mirror.

A linha de uso do make é: usage: make [build] [install | reinstall | deinstall] [clean].

1. Digite cd e a pasta do port, como /usr/ports/sysutils/screenfetch.
2. Digite make install clean para compilar, instalar e limpar o port.
3. Digite make deinstall para remover o programa do computador novamente.

```command
id: jsc:command/freebsd/make
```

Table: Os alvos do make

| Alvo | O que faz |
| --- | --- |
| `build` | Compila o port neste computador, que é o que o make faz quando você não dá alvo. |
| `install` | Compila o port se ainda não estiver compilado, e depois o instala. |
| `reinstall` | Compila e instala o port por cima do que já está instalado. |
| `deinstall` | Remove o port instalado do computador; com clean, limpa antes. |
| `clean` | Apaga a pasta de trabalho que a compilação deixou. |

Você combina alvos em uma só linha, como make install clean. Uma compilação leva entre 5 segundos e 30 minutos. Ela roda uma tarefa por núcleo do processador, e um processador mais rápido a encurta.

## A compilação 10% mais leve

Um programa que você compila neste computador precisa de 10% menos que o pacote do mesmo programa. Quando uma compilação termina, ela imprime Compilado para esta máquina:, seguido de uma linha como 10% menos memória, disco e processador que o pacote.

Table: O que uma compilação aqui economiza

| O que | Economizado por uma compilação aqui |
| --- | --- |
| Velocidade do processador para rodar | 10% a menos |
| Espaço em disco para instalar | 10% a menos |
| Memória enquanto roda | 10% a menos |
| Memória de vídeo | Não reduzida |

## Mensagens do make

```table
{"caption": "Mensagens do make", "heads": ["Mensagem", "O que significa"], "rows": [[{"literal": "make: no target to make."}, {"words": "A pasta não é a pasta de um port. Então o make para nessa pasta."}], [{"literal": "make: don't know how to make <target>. Stop"}, {"words": "O make não conhece esse alvo."}], [{"literal": "===>  <pkg> is already installed"}, {"words": "O port já está no computador; a mensagem sugere make deinstall e make install, ou make reinstall."}], [{"literal": "===>  <pkg> is marked as broken: <reason>."}, {"words": "Uma verificação da instalação bloqueia o port, como a era dele ou o tipo de computador."}], [{"literal": "===>  <pkg> cannot install: a Mainframe's services come prebuilt; use pkg install <name>."}, {"words": "O port é um serviço do Mainframe, então instale-o com pkg."}], [{"literal": "===>  <pkg> cannot install: this computer cannot store installed programs."}, {"words": "O computador não tem disco onde guardar programas."}], [{"literal": "===>  <pkg> cannot install: this machine is short of the processor or the free disk space it needs."}, {"words": "O computador é pequeno demais para o port."}], [{"literal": "fetch: mirror://mainframe/distfiles/<distfile>: No address record"}, {"words": "A compilação precisa do código-fonte, e nenhum Mirror responde."}]]}
```

## Se algo der errado

> [!PROBLEM] make diz que não há alvo a ser compilado.
> Entre antes na pasta do port, como /usr/ports/sysutils/screenfetch.

> [!PROBLEM] make diz que o programa já está instalado.
> Digite make reinstall, ou make deinstall e depois make install.

> [!PROBLEM] fetch diz que não há registro de endereço para o Mirror.
> Não é possível alcançar o Mirror. Conecte a rede a um Mirror e tente de novo.

> [!PROBLEM] make diz que o computador não tem processador ou espaço livre em disco suficiente.
> Use um computador com processador mais rápido, ou libere algum espaço em disco.

> [!SEE] jsc:ports_tree, jsc:pkg, jsc:processors
