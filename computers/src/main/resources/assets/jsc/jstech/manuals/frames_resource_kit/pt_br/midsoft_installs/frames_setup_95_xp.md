---
section: midsoft_installs
order: 198
icon: jsc:cd_rom
---
# Instalação do Frames 95 e XP

O Frames 95 e o Frames XP vêm em CD, então você precisa de uma Unidade de CD conectada ao computador ([](jsc:drives)). O disco traz o programa de instalação, SETUP.EXE, que copia o sistema para um disco. O Frames 95 e o XP têm páginas de instalação próprias, e as teclas abaixo importam na página de disco do Frames XP.

## Frames 95

O firmware oferece o disco como "Frames 95 installer", e escolhê-lo inicia a instalação ([](jsc:firmware)).

1. Coloque o CD do Frames 95 em uma Unidade de CD conectada ao computador.
2. Ligue o computador e escolha "Frames 95 installer" no firmware.
3. Leia a verificação na primeira página e pressione [[Enter]] para continuar, ou [[F3]] para sair.
4. Escolha o disco do sistema e avance para a próxima página.
5. Digite o nome do computador e avance.
6. Aguarde enquanto os arquivos são copiados.
7. Quando a última página disser "O Setup terminou.", a cópia está concluída.
8. Depois disso, você pode tirar o CD da unidade.

Table: As páginas do Frames 95

| Página | O que mostra |
| --- | --- |
| Boas-vindas | A verificação: Geração, Processador, Memória, o disco e a mídia de instalação, cada uma com sua resposta. |
| Disco | "Escolha um Disco", com os discos da máquina. |
| Nome | "Nome do Computador": o nome é cortado em 256 letras. |
| Cópia | "Copiando Arquivos". |
| Concluído | "O Setup terminou." |

## Frames XP

O Frames XP vem no mesmo CD e inicia do mesmo jeito: escolha "Frames XP installer" no firmware. Suas páginas são diferentes, e sua página de disco lista as teclas de que você precisa.

Table: As teclas da página de disco do Frames XP

| Chave | O que faz |
| --- | --- |
| `ENTER` | Instala o sistema no disco escolhido. |
| `E` | Apaga o disco escolhido, depois que você confirmar duas vezes. |
| `F3` | Sai da instalação. |

Table: As páginas do Frames XP

| Página | O que mostra |
| --- | --- |
| Disco | "A lista a seguir mostra os discos deste computador." |
| Nome | "Nome do Computador". |
| Cópia | "Aguarde enquanto o Setup copia os arquivos para o disco." |
| Instalação | "Instalando o Frames", com as etapas Instalando o Frames e Finalizando a instalação. |
| Concluído | "O Setup terminou.", e a tecla ENTER reinicia o computador. |

## Escolhendo o disco

As duas instalações usam as mesmas regras para o disco. A instalação propõe o primeiro disco com espaço para o sistema. Um disco tem espaço quando seu espaço livre é pelo menos a ocupação do sistema.

Se o disco escolhido for pequeno demais, a instalação diz "Sem espaço aqui. Apague este disco, ou escolha outro." Apagar é pedido duas vezes: a primeira resposta só pergunta, e a segunda apaga o disco. A pergunta traz "Apagar Disco" com o número do disco. O apagamento acontece antes da cópia, e escolher outro disco antes de a cópia começar cancela o apagamento.

> [!DEFINE] Setup
> O programa instalador que copia um sistema para um disco.

## Se algo der errado

> [!PROBLEM] A tela diz: "Nenhum disco desta máquina tem espaço para ele."
> Apague um disco que você não precisa mais, ou adicione um disco maior ([](jsc:disks)).

> [!PROBLEM] A tela diz: "Sem espaço aqui. Apague este disco, ou escolha outro."
> Escolha outro disco da lista, ou apague este se os arquivos dele não forem necessários.

> [!PROBLEM] O firmware não oferece o instalador e diz: "Nenhuma mídia de instalação está em uma unidade conectada a esta máquina."
> Coloque o CD em uma Unidade de CD conectada ao computador ([](jsc:peripheral_cables)).

> [!PROBLEM] A cópia para e diz: "A mídia foi retirada antes que o sistema fosse gravado."
> Coloque o CD de volta na unidade e inicie a instalação de novo. Deixe o CD na unidade até a cópia terminar.

> [!SEE] jsc:frames_setup_7_10_11, jsc:frames_95, jsc:frames_xp, jsc:installing
