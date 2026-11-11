---
section: isms_studio
order: 394
---
# Escrevendo e rodando consultas

Cada aba de consulta guarda um script: as instruções que você escreve e os resultados da última execução. As abas se chamam Consulta 1, Consulta 2 e assim por diante. [[Ctrl]]+[[Tab]] vai para a próxima aba.

## Sua primeira execução

1. Pressione [[Ctrl]]+[[N]] para abrir uma nova aba de consulta.
2. Digite uma instrução e termine com ponto e vírgula, por exemplo QUERY items LIMIT 10;
3. Pressione [[F5]] para rodar o script inteiro.
4. Leia os resultados abaixo do script. Pressione [[Ctrl]]+[[D]] para vê-los em grade.

As instruções rodam uma de cada vez. O estúdio envia a próxima só quando a última voltou.

Cada Operação que uma execução inicia aparece na aba Mensagens, como "Operação #<id> iniciada - Cancelar Execução da Consulta a interrompe". Pressione [[Alt]]+[[Break]], ou o Cancelar da barra de ferramentas, para parar a execução. O estúdio então responde "A consulta foi cancelada pelo usuário."

## Instruções que não podem ser desfeitas

Um DROP roda assim que você o digita, sem pergunta, como num banco de dados de verdade: o que ele destrói não volta. Um DELETE de ALL manda tudo de um item para fora da rede, e antes de ele rodar o estúdio pergunta "Esta instrução não pode ser desfeita:", mostra a instrução e pergunta "Executar?". Os botões são Sim, Não e uma caixa chamada "Não perguntar de novo". Marcar essa caixa desliga a pergunta.

## Opções

Table: Opções do estúdio

| Opção | Padrão | O que faz |
| --- | --- | --- |
| Perguntar antes de uma instrução que não pode ser desfeita | ligado | Pergunta antes de cada DELETE de ALL. Ferramentas > Opções. |
| Abrir uma nova consulta quando o estúdio começa | ligado | Abre uma aba de consulta vazia no início. Ferramentas > Opções. |
| Mostrar números de linha | ligado | Mostra um número ao lado de cada linha de um script. Ferramentas > Opções. |
| Parar o script no primeiro erro | ligado | Para a execução na primeira instrução que falhar. Consulta > Opções de Consulta. |

## Limites

Uma instrução pode ter 512 caracteres. Uma mais longa é recusada, e o estúdio marca a linha onde ela começa.

## Resultados e mensagens

Os resultados aparecem numa grade por padrão. [[Ctrl]]+[[T]] os mostra como texto, e [[Ctrl]]+[[Shift]]+[[F]] os salva num arquivo como valores separados por vírgula.

A aba Mensagens, ao lado de Resultados, lista o que a execução fez. Ela guarda no máximo 200 mensagens por aba.

## Verificação e planejamento

[[Ctrl]]+[[E]] roda só o texto selecionado. [[Ctrl]]+[[F5]] verifica a sintaxe sem executar nada. Uma verificação sem erros responde "Comandos concluídos com êxito."

[[Ctrl]]+[[L]] mostra o plano estimado de uma instrução CRAFT. Para qualquer outra instrução, o estúdio responde "Exibir Plano Estimado funciona com um comando CRAFT." O plano lista os passos escolhidos para fazer a fabricação.

## Salvar e abrir

Salvar grava a aba no arquivo dela. Salvar Como... grava um novo arquivo de script. Os scripts são salvos com a extensão .iql, e um arquivo digitado como IQL abre no estúdio.

## Se algo der errado

> [!PROBLEM] O estúdio diz "Não há nada para executar."
> Digite uma instrução na aba primeiro, ou selecione o texto que você quer rodar.

> [!PROBLEM] O estúdio marca uma linha: "Linha <n>: a instrução tem <length> caracteres, mais do que os 512 que uma instrução pode ter."
> Divida a instrução em instruções mais curtas, cada uma terminando com ponto e vírgula.

> [!PROBLEM] O estúdio pergunta "<tab> tem alterações que não foram salvas. Fechar mesmo assim?"
> Pressione [[Ctrl]]+[[S]] para salvar a aba primeiro, depois feche-a.

> [!PROBLEM] A execução responde "Consulta concluída com erros".
> Leia a aba Mensagens, corrija a linha que ela indica, e rode o script de novo.

> [!PROBLEM] Analisar responde "A análise encontrou um erro" ao lado de uma linha.
> Corrija a linha que a mensagem indica, depois analise de novo com [[Ctrl]]+[[F5]].

> [!PROBLEM] Salvar responde "Não há resultados para salvar."
> Rode uma consulta primeiro, depois salve os resultados dela.

> [!PROBLEM] O estúdio responde "Exibir Plano Estimado funciona com um comando CRAFT."
> Coloque uma instrução CRAFT na aba, depois pressione [[Ctrl]]+[[L]].

> [!SEE] jsc:query_and_select, jsc:iql_limits
