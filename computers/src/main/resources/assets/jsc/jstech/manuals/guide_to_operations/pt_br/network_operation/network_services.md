---
section: network_operation
order: 106
icon: jsc:server
---
# Serviços de uma baia de servidores

Um servidor em um rack recebe a sua função dos serviços instalados nele. O hardware decide quanto um servidor consegue guardar; os serviços decidem qual trabalho ele faz. Você escolhe os serviços que cada servidor roda.

Um serviço só responde enquanto a sua máquina está em funcionamento. Desligue o servidor, e os seus serviços param junto. Cada servidor mantém a sua própria lista de serviços ([](jsc:server_racks)).

Table: Os serviços de uma baia de servidores

| Serviço | O que faz | Era |
| --- | --- | --- |
| Cache Preditivo | Mantém os itens mais procurados da baia preparados na memória, reduzindo a latência de leitura em 15%. | Standard |
| Balanceador de Carga | Distribui as escritas pelos discos da baia, em vez de enchê-los um após o outro. | Legacy |
| Monitor de Integridade | Relê a baia depois de uma troca a quente, para que o índice da rede não duvide dela. | Standard |
| Serviço de Mensagens | Mantém as conversas da rede. | Standard |
| KnotHub | Guarda a fonte em que esta rede ainda trabalha, revisão por revisão. | Standard |
| Soundfoundry Server | Transmite o catálogo de músicas para todos os Soundfoundry da rede. | Standard |
| Compressor de Dados | Aperta os itens da baia: os seus discos guardam um quinto a mais, e cada leitura espera dois ticks a mais. | Standard |
| Serviço de Backup | Faz o backup da rede numa Biblioteca de Fitas ou no disco de outro servidor, e a devolve. | Legacy |

O Cache Preditivo reduz a latência de leitura da sua baia em 15%, e uma leitura nunca leva menos de um tick. Uma placa de cache de leitura no mesmo servidor soma o seu próprio corte a esse.

> [!DEFINE] Cache Preditivo
> Um serviço de uma baia de servidores que mantém os itens mais procurados da baia prontos na memória, para que você os leia 15% mais rápido.

O Monitor de Integridade importa quando você tira um disco de uma baia em funcionamento. Sem ele, o índice deixa de confiar nessa baia até alguém reconstruí-lo com o comando reindex. Com ele, a baia é relida depois da troca, e o índice não fica na dúvida.

> [!DEFINE] Monitor de Integridade
> Um serviço de uma baia de servidores que relê a baia depois que você troca um disco, para que o índice da rede continue confiando nela.

O Balanceador de Carga muda para onde os seus novos itens vão dentro da sua baia. Sem ele, o servidor enche os discos um após o outro. Com ele, cada escrita vai primeiro para o disco mais vazio, para que nenhum disco fique cheio enquanto os outros estão vazios.

> [!DEFINE] Balanceador de Carga
> Um serviço de uma baia de servidores que envia cada escrita para o disco mais vazio da baia, em vez de encher os discos um após o outro.

O Compressor de Dados aperta os itens da sua baia, para que os seus discos guardem um quinto a mais. O preço é tempo: cada leitura da baia espera dois ticks a mais. Num disco rígido isso é uma parte pequena da espera e vale a pena; no NVMe, cujas leituras são rápidas, triplica a espera. As etiquetas dos discos não mudam: o espaço vem do que cada item ocupa neles, por isso um disco comprimido tirado da sua baia pode aparecer mais que cheio.

> [!DEFINE] Compressor de Dados
> Um serviço de uma baia de servidor que cabe um quinto a mais de itens nos seus discos, ao custo de dois ticks a mais em cada leitura.

O Serviço de Backup faz o backup da rede e a devolve. Ele tem a sua própria página ([](jsc:backups)).

## Se algo der errado

> [!PROBLEM] Um serviço não responde.
> O servidor dele está desligado. Ligue o servidor.

## Se algo der errado

> [!PROBLEM] O índice diz que uma baia não está confirmada depois de uma troca a quente.
> A baia não tem um Monitor de Integridade. Instale-o nessa baia, ou reconstrua o índice manualmente com o comando reindex.
