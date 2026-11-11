---
section: social_and_media
order: 90
icon: jsc:cd_rom
---
# Soundfoundry

O Soundfoundry toca músicas no computador, a partir de um arquivo ou da rede. Ele está nas áreas de trabalho do Frames, do Linux e do FreeBSD, mas não no UNIX. Ele precisa de um computador Legacy ou mais recente.

O Soundfoundry precisa de uma placa de som. Sem ela, a linha da música mostra NENHUMA PLACA DE SOM INSTALADA, e os botões de volume, balanço e playlist ficam em cinza. Instale uma placa de som no computador ([](jsc:sound_cards)), e escolha para onde vai o som ([](jsc:speakers)).

## O tocador

Table: Botão de saída

| Rótulo | Toca o som por |
| --- | --- |
| `OUT -` | Nada. |
| `OUT MON` | O monitor. |
| `OUT SPK` | Os alto-falantes. |
| `OUT MON+SPK` | O monitor e os alto-falantes. |

A janela também tem os botões SHUFFLE e REPEAT, e um botão PL que mostra a playlist. ADD, REM e SEL atuam sobre a playlist. NET abre a janela de compartilhamento da rede. O volume e o balanço são controles deslizantes, e a linha da música mostra MONO ou STEREO, além da sua taxa de bits e a taxa de amostragem.

O menu reúne as formas de preencher a playlist: abrir um arquivo ou uma pasta, adicionar à playlist, remover ou manter as músicas selecionadas, ordenar a lista, e salvar ou abrir uma playlist. Uma playlist guarda no máximo 500 músicas, e o nome dela tem no máximo 32 caracteres.

> [!DEFINE] Playlist
> A lista de músicas na fila para tocar, em ordem.

## Músicas da rede

As abas da rede são SEARCH, DOWNLOADS e SHARED. Uma pesquisa encontra cada palavra que você digita no título, no artista, no álbum ou no nome de arquivo de uma música, em qualquer ordem e com qualquer uso de maiúsculas e minúsculas. Uma consulta tem no máximo 64 caracteres. As músicas da rede chegam na velocidade do cabo mais lento do caminho, e vários downloads ao mesmo tempo dividem essa velocidade.

Uma música toca a partir de um de três lugares: um arquivo no disco deste computador, uma música da biblioteca que este computador já tem no próprio disco, ou uma transmissão do Soundfoundry Server escolhido pelo computador. Uma música baixada ou importada fica guardada na pasta de música do computador.

> [!DEFINE] Transmissão
> Uma música transmitida enquanto toca, em vez de guardada antes no computador.

> [!DEFINE] Compartilhamento
> Uma música enviada para a biblioteca do servidor, para que os outros da rede possam tocá-la.

## Soundfoundry Server

O Soundfoundry Server roda em um servidor de rack. Ele transmite o catálogo de músicas para cada Soundfoundry da rede ([](jsc:server_racks)). A biblioteca dele são as músicas da sua própria pasta de música, e as músicas compartilhadas de cada computador ficam em uma pasta com o nome desse computador.

> [!DEFINE] Soundfoundry Server
> O serviço de um servidor de rack que transmite a música da rede para cada Soundfoundry em que você a tocar.

O servidor só transmite enquanto tiver memória para manter a transmissão aberta. Por isso, um computador pede para ouvir antes de tocar, e pede de novo de tempos em tempos.

## Vinil

Com uma Unidade de Vinil RW no cabo do computador ([](jsc:vinyl_rw_drives)), o Soundfoundry grava os discos de música que uma jukebox toca e os corta em vinil. Nos visuais Legacy e Transition, o botão VNL abre o painel da unidade, com Gravar, Parar, Cortar e Ejetar e a lista de gravações; no Standard e no Advanced, a página Vinil da barra lateral faz o mesmo. As gravações ficam em Music/Vinyl.

## Se algo der errado

> [!PROBLEM] A linha da música mostra NENHUMA PLACA DE SOM INSTALADA.
> Instale uma placa de som no computador.

> [!PROBLEM] Uma música da rede não toca.
> Nenhum Soundfoundry Server está na rede, ou o servidor não tem memória livre para a transmissão. Instale um Soundfoundry Server em um servidor de um rack da rede.

> [!SEE] jsc:sound_cards, jsc:speakers
