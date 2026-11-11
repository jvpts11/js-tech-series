---
section: running_limits
order: 482
---
# Os limites de um programa em execução

Um programa em execução tem limites. Eles impedem que um programa tome toda a memória ou todo o tempo de um computador. A maioria dos limites é fixa. Dois dos limites de tempo podem ser mudados no arquivo de configuração jstech-balance.toml.

A memória é o primeiro limite. Cada programa tem um heap: a memória que guarda os objetos que ele cria, como textos e listas. Cada objeto conta contra a memória dada ao programa. Sem --heap, um programa recebe 1 MB. O máximo que você pode dar é 64 MB.

Table: Memória

| Limite | Valor | O que acontece além dele |
| --- | --- | --- |
| Memória dada a um programa com --heap | 1 MB por padrão, no máximo 64 MB | Um valor maior é reduzido para 64 MB. |
| Memória livre no computador | A memória do computador, menos o que está em uso | O programa não inicia, e o prompt diz que ele não cabe. |
| Texto no heap | 16 bytes, mais 2 bytes por caractere | Conta no heap. |
| Cada objeto no heap | 16 bytes, mais 8 bytes por ligação a outro objeto | Conta no heap. |
| Uma mensagem ou um clique | 40 bytes, mais o seu texto | Conta no heap do programa que a recebe. |

O arquivo de configuração jstech-balance.toml tem duas chaves: balance.program_machine_micros, para um computador, e balance.program_server_micros, para todos os computadores juntos. Ambas são tempos em microssegundos.

Table: Tempo e turnos

| Limite | Valor | O que acontece além dele |
| --- | --- | --- |
| Instruções em um turno de uma thread | 64 | O turno passa para a próxima thread, o que não é uma falha. |
| Créditos em uma rodada de um tick | 64 | A rodada passa para o próximo programa, o que não é uma falha. |
| Trabalho entre duas verificações do relógio | 512 unidades | O tick termina cedo quando o seu tempo acaba. |
| Tempo por tick, para um computador | 1.000 microssegundos (1 milissegundo) por padrão; de 50 a 1.000.000 microssegundos na configuração | O computador para os seus programas pelo resto desse tick. |
| Tempo por tick, para todos os computadores juntos | 8.000 microssegundos (8 milissegundos) por padrão; de 100 a 10.000.000 microssegundos na configuração | Nenhum computador roda os seus programas nesse tick, e os computadores vão primeiro no tick seguinte. |
| Orçamento de despedida por tick | 4.096 instruções, compartilhadas | Um programa que passa da sua cota para antes de poder executar o seu código de despedida. |
| Chamadas uma dentro da outra | 1.024 | O programa para, e a mensagem diz que um método pode estar chamando a si mesmo sem fim. |

Table: Chamadas e consoles

| Limite | Valor | O que acontece além dele |
| --- | --- | --- |
| Chamadas esperando por um programa | 256 chamadas | A nova chamada ou mensagem é recusada. |
| Texto das chamadas esperando por um programa | 64 KB (65.536 bytes) | A nova chamada ou mensagem é recusada. |
| Linhas digitadas antecipadamente para um programa | 16 linhas | A linha digitada não é aceita. |
| Linhas guardadas em um console | 200 linhas | As linhas mais antigas são descartadas. |
| Caracteres guardados em um console | 64.000 caracteres | As linhas mais antigas são descartadas. |
| Tamanho de uma linha do console | 4.096 caracteres | A linha é cortada, e o corte é marcado com reticências. |
| Janelas que um programa pode ter abertas | 8 | O programa para, avisando que pode ter 8 janelas abertas ao mesmo tempo. |
| Pastas que um computador compartilha | 16 pastas | O prompt diz que o computador já compartilha essa quantidade de pastas. |

Table: Janelas

| Limite | Máximo permitido | O que acontece além dele |
| --- | --- | --- |
| Widgets em uma janela | 256 | A janela recusa a mudança. |
| Linhas em um widget de tabela | 1.024 | A janela recusa a mudança. |
| Colunas em um widget de tabela | 8 | A janela recusa a mudança. |
| Abas em uma visão de abas | 64 | A janela recusa a mudança. |
| Seções em um widget | 4 | A janela recusa a mudança. |
| Pontos em um desenho | 120 | A janela recusa a mudança. |
| Linhas em uma visão de log | 256 | A janela recusa a mudança. |
| Traços em uma área de desenho | 4.096 | A janela recusa a mudança. |
| Área de uma área de desenho | 4.096 | A janela recusa a mudança. |
| Entradas em um menu | 64 | A janela recusa a mudança. |
| Tamanho de uma janela | 640 de largura e 360 de altura no máximo; cada lado com pelo menos 60 | A janela recusa a mudança. |

> [!DEFINE] Heap
> A memória que um programa em execução guarda para os objetos que cria, como textos e listas. O tamanho dela é a memória que foi dada ao programa.

## Se algo der errado

> [!PROBLEM] O prompt diz que o programa não cabe na memória livre.
> Feche um programa ou uma janela, adicione memória ao computador ([Memória](jsc:memory)) ou inicie o programa com um --heap menor.

> [!PROBLEM] O programa para com uma mensagem que começa com “sem memória:” e indica as linhas que mais guardam memória.
> O programa guarda muita coisa de uma vez. Faça-o guardar menos, ou dê mais memória com --heap, até 64 MB.

> [!PROBLEM] Um console perde as linhas mais antigas.
> Um console guarda 200 linhas ou 64.000 caracteres. Imprima menos ou leia a saída mais cedo.

> [!PROBLEM] O programa para e avisa que pode ter 8 janelas abertas ao mesmo tempo.
> Feche uma das janelas antes de abrir outra.

> [!PROBLEM] Um programa para por alguns ticks em um servidor ocupado.
> O tempo permitido por tick acabou. Aumente as chaves de equilíbrio em jstech-balance.toml, se o servidor puder dar esse tempo.

> [!SEE] jsc:memory, jsc:credits_per_tick, jsc:sigma_runtime_stops
