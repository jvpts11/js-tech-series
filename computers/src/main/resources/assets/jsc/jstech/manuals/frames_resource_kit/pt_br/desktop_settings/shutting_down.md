---
section: desktop_settings
order: 217
icon: jsc:monitor
---
# Saindo da conta, desligando e reiniciando

Quando terminar, você pode encerrar a sessão, desligar a máquina ou reiniciá-la. Cada edição tem o seu próprio controle para isso, e a maioria deles abre a mesma caixa de energia.

Table: O controle de energia de cada edição

| Edição | Onde fica |
| --- | --- |
| Frames 95 | A linha Desligar, sob os programas em Iniciar. |
| Frames XP | Fazer Logoff e Desligar o Computador, no rodapé de Iniciar. |
| Frames 7 | A linha Desligar, no pé de Iniciar. |
| Frames 10 | O botão de energia, no pé do trilho em Iniciar. |
| Frames 11 | O botão de energia, no rodapé de Iniciar. |

## A caixa Energia

A caixa Energia é uma lista curta com o título "Energia". Ela tem três linhas, nesta ordem, e cada linha mostra uma dica curta abaixo do nome.

Table: As linhas da caixa Energia

| Linha | O que faz |
| --- | --- |
| Desligar | A máquina desliga. |
| Reiniciar | A máquina desliga e começa de novo. Se não houver mensagem de despedida para mostrar, a tela do autoteste de inicialização (POST) abre na hora. |
| Fazer Logoff | A tela fecha, e a máquina continua rodando. |

Um clique fora da lista fecha a caixa sem fazer nada. A tecla Escape também a fecha.

## Desligando pela área de trabalho

1. Abra o menu Iniciar.
2. Clique no controle de energia da sua edição, como mostra a tabela acima.
3. Clique em Desligar, Reiniciar ou Fazer Logoff na caixa Energia.

## Fazendo logoff no Frames XP

No Frames XP, o Fazer Logoff do rodapé fecha a área de trabalho na hora, sem a caixa Energia. A máquina continua rodando.

## O que o computador faz

A área de trabalho fecha primeiro. Depois a máquina age conforme a sua escolha: um desligamento a desliga, uma reinicialização a liga de novo, e um logoff a deixa rodando. Para abrir a configuração do firmware, use o botão Reiniciar para o firmware na página System de [Configurações](jsc:frames_settings).

## Se algo der errado

> [!PROBLEM] Você escolheu Desligar por engano.
> A caixa não pergunta nada depois que uma linha é escolhida, então a escolha é feita na hora. Clique fora da lista, ou pressione [[Esc]], antes de escolher.

> [!PROBLEM] O computador reinicia e mostra o autoteste de inicialização (POST) de novo.
> Isso é uma reinicialização, e é esperado. A verificação roda de novo a cada início.

> [!PROBLEM] O computador continua rodando depois de Fazer Logoff.
> Isso é esperado: Fazer Logoff fecha apenas a tela. Para desligar a máquina, abra a área de trabalho de novo e use a caixa Energia.

> [!DEFINE] Caixa de energia
> A lista curta de Desligar, Reiniciar e Fazer Logoff, com o título Energia.

> [!SEE] jsc:firmware, jsc:frames_settings
