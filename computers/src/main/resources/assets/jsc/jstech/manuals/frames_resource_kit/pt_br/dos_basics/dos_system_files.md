---
section: dos_basics
order: 205
---
# Os arquivos próprios do sistema

Quando o MC-DOS está instalado, o disco do sistema, C:, mostra alguns arquivos que nunca foram copiados para ele. O sistema os monta a partir do que está instalado cada vez que o disco é lido, por isso eles não ficam guardados no disco. Eles ficam na raiz de C: e na sua pasta DOS.

Table: Os arquivos do disco do sistema

| Arquivo ou pasta | O que é |
| --- | --- |
| `DOS\` | A pasta com os arquivos próprios do sistema. |
| `AUTOEXEC.BAT` | O arquivo de inicialização, com a linha PATH. |
| `CONFIG.SYS` | O arquivo de configuração, cujas linhas não têm efeito neste sistema. |
| `COMMAND.COM` | O interpretador de comandos: o programa que lê o que você digita e o executa. |
| `MCDOS.SYS` | O próprio sistema. |
| `DOS\HIMEM.SYS` | O driver de memória do sistema. |
| `DOS\README.TXT` | As notas do sistema, que mandam digitar HELP para ver os comandos. |
| `PROGRAM\` | A pasta de um programa instalado, com nome de até oito letras maiúsculas. |
| `PROGRAM\PROGRAM.EXE` | O próprio programa. |
| `PROGRAM\README.TXT` | As notas desse programa. |

O AUTOEXEC.BAT tem três linhas: @ECHO OFF, PROMPT $P$G e uma linha PATH. A linha PATH lista primeiro a pasta do DOS, depois a pasta de cada programa instalado, na ordem em que foram instalados. Quando você digita apenas o nome de um programa, o MC-DOS procura nas pastas dessa linha. Se o AUTOEXEC.BAT ou a sua linha PATH estiver ausente, ele procura apenas na pasta do DOS.

A linha PROMPT não define o seu prompt. O prompt sempre mostra a pasta em que você está e um sinal de maior que ([](jsc:dos_prompt)).

O CONFIG.SYS tem as linhas DEVICE=C:\\DOS\\HIMEM.SYS, DOS=HIGH, FILES=30 e BUFFERS=20. O arquivo está ali para você ler, mas suas linhas não têm efeito neste sistema.

## Se algo der errado

> [!PROBLEM] Você digita o nome de um programa que instalou, e o prompt diz comando não encontrado.
> Digite o caminho completo do arquivo .EXE do programa: C:\\ seguido da pasta e do nome do arquivo. Programas digitados pelo nome são encontrados na pasta em que você está e nas pastas da linha PATH.

> [!PROBLEM] Você espera que a linha PROMPT mude o prompt, mas ele não muda.
> O prompt sempre mostra a pasta e um sinal de maior que. Nenhuma linha do AUTOEXEC.BAT muda isso ([](jsc:dos_prompt)).

> [!SEE] jsc:dos_prompt, jsc:dos_shell
