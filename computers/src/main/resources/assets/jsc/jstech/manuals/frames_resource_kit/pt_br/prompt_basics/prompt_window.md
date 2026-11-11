---
section: prompt_basics
order: 233
---
# A janela do Prompt de Comando e suas teclas

O Prompt de Comando é a janela onde você digita um comando e lê o que a máquina responde. Um comando é uma palavra que nomeia um programa, seguida das palavras que dizem sobre o que ele trabalha. Você pressiona [[Enter]] para executar a linha.

> [!DEFINE] Comando
> Um programa que você chama pelo nome, com as palavras depois do nome que dizem sobre o que ele trabalha.

> [!DEFINE] Programa
> Um conjunto de instruções que o computador executa quando você o chama pelo nome.

Um monitor abre a tela feita para o seu sistema. Um monitor do Frames, ou um monitor sem sistema, abre a tela do Prompt de Comando. O MC-DOS, o sistema operacional de disco (DOS), abre a própria tela de texto, o MC-NET abre o terminal de rede, e o Linux e os outros sistemas Unix abrem o próprio terminal.

Em uma área de trabalho do Frames, o Prompt de Comando é um dos aplicativos da área de trabalho, então você também pode abri-lo por ali.

Acima da linha que você digita, a janela mantém o que a máquina respondeu. Essa saída guardada é o histórico de rolagem, e a roda do mouse rola para trás por ele. Em um monitor, o histórico de rolagem comporta 512 linhas.

> [!DEFINE] Histórico de rolagem
> A saída que a janela mantém acima do prompt, para que você possa reler o que saiu pelo topo.

## As chaves

Table: Chaves no prompt

| Chave | O que faz |
| --- | --- |
| `Enter` | Executa a linha digitada. |
| `Tab` | Completa o resto do nome de um comando a partir das letras que você digitou. Quando vários nomes servem, um segundo Tab seguido lista todos os candidatos acima do prompt. |
| `Up` | Mostra a linha que você digitou antes, um passo para trás. |
| `Down` | Mostra a linha que você digitou depois, um passo para frente. |
| `Esc` | Fecha a janela. Um editor aberto recebe a tecla primeiro, quando o editor a usa. |
| `Ctrl+C` | Copia o texto selecionado. Sem nada selecionado, interrompe uma ferramenta em execução. |
| `Ctrl+V` | Cola texto: até 16 linhas, cada uma cortada em 512 caracteres. Todas as linhas, menos a última, são executadas conforme chegam. |
| Roda do mouse | Rola para trás pela saída. |

> [!DEFINE] Autocompletar com Tab
> Completar o resto do nome de um comando a partir das letras que você digitou, para que não precise digitar o nome inteiro.

> [!DEFINE] Interrupção
> Um sinal que pede a uma ferramenta em execução que pare.

```text
{"text": "Enquanto uma ferramenta roda, a janela lê apenas Ctrl+C. Espere a ferramenta terminar, ou pressione Ctrl+C para interrompê-la. A linha de baixo da janela lembra as teclas: enquanto você digita, ela mostra \"ENTER executar    CIMA/BAIXO histórico    roda para rolar    ESC fechar\", e enquanto uma ferramenta roda, mostra \"CTRL+C interromper    roda para rolar    ESC fechar\"."}
```

Quando você rolou para trás, a linha de baixo mostra "rolado +" e um número de linhas. Assim que a primeira palavra for um comando que a máquina conhece, o uso dele aparece em cinza à direita da sua linha, cortado com dois pontos se não couber.

## Limites e histórico

Uma linha comporta no máximo 256 caracteres. A janela guarda as linhas que você digita como histórico desta máquina, para que as teclas Cima e Baixo as tragam de volta. Uma linha vazia nunca é guardada, e uma linha digitada duas vezes seguidas é guardada uma vez só. Abrir a janela começa com um histórico de rolagem vazio, mas o histórico de linhas continua.

## Se algo der errado

> [!PROBLEM] A linha fica cortada.
> Uma linha comporta no máximo 256 caracteres. Divida-a em duas linhas.

> [!PROBLEM] Digitar não faz nada enquanto uma ferramenta roda.
> Enquanto uma ferramenta roda, a janela lê apenas Ctrl+C. Espere a ferramenta terminar, ou pressione Ctrl+C.

> [!PROBLEM] Ctrl+C não para uma ferramenta.
> A ferramenta precisa estar rodando nesta janela. Uma ferramenta deixada em execução em segundo plano não responde a Ctrl+C.

> [!PROBLEM] Esc não fecha um editor.
> Um editor que usa Esc mantém a tecla. Saia do editor com a tecla de saída própria ([](jsc:editors_overview)), depois pressione Esc.

> [!SEE] jsc:help_on_a_computer, jsc:the_systems
