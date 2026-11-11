---
section: errors_compile
order: 489
---
# Erros de semântica

Um erro de semântica é uma linha cuja forma está certa, mas cujos nomes ou tipos não encaixam. O compilador verifica a semântica depois que a forma do arquivo está correta, então esses códigos costumam aparecer depois que os erros de sintaxe somem. A verificação do editor enquanto você digita é a exceção.

As palavras entre sinais de menor e maior (< e >) representam o que o compilador encontrou no seu arquivo: um nome, um tipo, um membro de um tipo, um número ou um token. Sua mensagem mostra as palavras reais no lugar delas.

> [!DEFINE] lacuna
> Um lugar dentro de um texto onde se coloca um valor: as chaves de uma string interpolada, ou um marcador de formato printf que o formato preenche com um valor.

Table: Nomes e declarações

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S2012` | '<name>' é um namespace; escreva 'using <name>.\*;' para trazer tudo o que há nele, ou nomeie um dos seus tipos | Um namespace foi usado onde é preciso um tipo. Escreva a linha using com asterisco, ou nomeie um tipo do namespace. |
| `S3001` | '<name>' não se refere a nada aqui | O nome não está declarado no lugar onde você o usa. Confira a grafia, ou declare-o acima desta linha. |
| `S3002` | '<name>' já está declarado aqui | Duas coisas têm o mesmo nome no mesmo lugar. Dê a uma delas outro nome. |
| `S3040` | '<name>' está em <namespace>; adicione 'using <namespace>.\*;' ou 'using <namespace>.<name>;' no início do arquivo | O nome está em um namespace que você não trouxe. Adicione a linha using que a mensagem indica, no início do arquivo. |
| `S3059` | '<name>' já está atrás de '<name>', então não pode também estar na frente dele | Dois tipos citam um ao outro como base, formando um círculo. Remova a base que fecha o círculo. |
| `S3060` | '<name>' é <private ou protected> de '<type>', então não pode ser alcançado daqui | O membro é private do seu tipo, que é o que um membro escrito sem palavra de acesso é, ou protected dele e dos tipos que se apoiam nele. Marque-o como public, ou chegue a ele de dentro do seu tipo. |
| `S3031` | só uma classe ou uma interface pode vir depois dos dois pontos, e '<name>' não é nenhuma das duas | Um tipo recebe, depois dos dois pontos, uma base que não é classe nem interface. Coloque ali uma classe ou uma interface. |
| `S3041` | uma struct pode implementar interfaces, mas não fica sobre nenhuma classe, e '<name>' é uma classe | Uma struct cita uma classe como base, mas uma struct só pode implementar interfaces. Remova a classe, ou torne o tipo uma classe. |
| `S3015` | '<name>' não tem classe base | O programa usa um membro da classe base onde o tipo não tem classe base. Confira o tipo, ou remova esse uso. |
| `S3057` | '<name>' precisa de <level> <version>; este projeto está em <level> <version> | Um nome é mais novo que a versão da linguagem que o projeto usa. Aumente a versão do projeto, ou não use esse nome. |
| `S3028` | '<name>' não nomeia um tipo | Esta mensagem não é mais exibida, então você não deve encontrá-la. Se encontrar, escreva o nome de um tipo nesse lugar. |

Table: Tipos e conversões

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3003` | não é possível converter '<type>' em '<type>' | Um valor de um tipo está sendo usado onde outro tipo é necessário, sem conversão segura. Escreva um valor do tipo certo. |
| `S3019` | '<name>' recebe <count> argumentos de tipo | Um tipo genérico recebeu a quantidade errada de tipos. Dê a ele a quantidade que a mensagem indica. |
| `S3039` | o número de um enum precisa ser escrito como número | Um valor de enum dá um número que não está escrito como número simples. Escreva o próprio número. |
| `S3009` | uma condição é um bool, não '<type>' | Um if ou um laço testa algo que não é verdadeiro ou falso. Escreva um teste que dê verdadeiro ou falso. |
| `S3022` | só um objeto pode ser descartado, não '<type>' | Dispose funciona só com objetos. Descarte um objeto, não um valor simples. |
| `S3042` | só um objeto pode ser bloqueado, não '<type>' | Lock funciona só com objetos. Bloqueie um objeto. |
| `S3023` | foreach percorre um array ou uma lista, não '<type>' | Um laço foreach precisa de um array ou de uma lista. Dê a ele um dos dois. |

Table: Operadores, chamadas e membros

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3007` | '<operator>' não pode ser aplicado a '<type>' e '<type>' | Os dois valores não funcionam juntos com este operador. Use valores de um tipo que o operador aceita. |
| `S3008` | '<operator>' não pode ser aplicado a '<type>' | O operador não aceita este tipo. Use um valor de um tipo que ele aceita. |
| `S3024` | '<type>' não pode ser indexado por '<type>' | O índice é o tipo de valor errado para este tipo. Dê um índice do tipo que ele aceita. |
| `S3025` | '<name>' não é algo que pode ser chamado | Você chamou um nome que não é método nem delegate. Confira a grafia, ou chame um método. |
| `S3026` | '<type>' não pode ser criado com new | new foi usado com um tipo que não pode ser criado desse jeito. Crie um objeto de um tipo que possa. |
| `S3050` | '<type>' é abstrato, então não pode ser criado com new | Um tipo abstrato não tem objetos próprios. Crie um objeto de um tipo que não seja abstrato. |
| `S3005` | nenhuma versão de '<name>' aceita esses argumentos | Os valores que você passou não se encaixam em nenhuma versão do método. Passe valores dos tipos que uma versão aceita. |
| `S3006` | a chamada a '<name>' se encaixa em mais de uma versão dele | Mais de uma versão se encaixa nos valores. Dê um valor de um tipo que se encaixe em apenas uma versão. |
| `S3004` | '<type>' não tem membro chamado '<name>' | O nome não é membro desse tipo. Confira a grafia, ou use um membro que o tipo tenha. |
| `S3020` | '<member>' pertence ao tipo, não a um dos seus objetos | Escreva o nome do tipo antes do ponto, não o nome de um dos seus objetos. |
| `S3021` | '<member>' pertence a um objeto, não ao tipo | Um membro de um objeto está sendo usado pelo nome do tipo. Use um objeto do tipo. |
| `S3014` | '<name>' não está disponível em um membro estático | Um membro estático não tem objeto, então este valor não existe ali. Use um valor que não precise de objeto. |
| `S3033` | '<name>' é um método: chame-o, ou entregue-o onde se espera um delegate com a mesma forma | Um método está sendo usado como valor sem os seus parênteses. Chame-o, ou entregue-o onde um delegate é esperado. |
| `S3027` | esta lambda não tem a forma que '<shape>' recebe | Os valores da lambda não batem com o delegate a que ela é entregue. Faça-a receber os valores que o delegate recebe. |

Table: Laços, opções e retornos

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3029` | esta opção já tem esse rótulo | Dois casos da opção têm o mesmo rótulo. Mantenha apenas um deles. |
| `S3010` | 'break' só faz sentido dentro de um laço ou de uma opção | break está fora de qualquer laço ou opção. Mova-o para dentro de um deles, ou apague-o. |
| `S3011` | 'continue' só faz sentido dentro de um laço | continue está fora de qualquer laço. Mova-o para dentro de um laço, ou apague-o. |
| `S3012` | um método que devolve '<type>' precisa devolver um valor | O método termina sem devolver um valor. Devolva um valor desse tipo. |
| `S3013` | um método que não devolve nada não pode devolver um valor | O método devolve um valor, mas não devolve nada. Remova o valor depois de return. |

Table: Out, ref e eventos

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3034` | '<name>' é preenchido pelo método, então o argumento é escrito com out | O método preenche este argumento. Escreva out antes dele. |
| `S3035` | '<name>' é lido pelo método, então o argumento não pode ser escrito com out | O método só lê este argumento. Remova o out. |
| `S3036` | um argumento out é exatamente '<type>', sem nenhuma conversão no caminho | A variável out precisa ser exatamente do tipo que o método preenche. Declare-a com esse tipo. |
| `S3037` | '<name>' é preenchido por este método, então precisa receber um valor antes de cada saída | Uma das saídas do método deixa o valor out vazio. Dê a ele um valor antes de cada return. |
| `S3038` | '<name>' não é um lugar onde um método pode escrever | O método só pode escrever em um lugar. Dê a ele um lugar, não um valor simples. |
| `S3030` | um evento só pode ser disparado dentro do tipo que o declara | O evento é disparado de fora do seu tipo. Dispare-o dentro do tipo. |
| `S3032` | o tipo de um evento precisa ser um delegate, e '<type>' não é um | Um evento precisa de um tipo delegate. Dê a ele um delegate. |
| `S3058` | 'ref' só é escrito para strcat, que une o texto que '<name>' guarda; um método preenche com out | ref serve só para unir texto. Use out para um método que preenche um valor. |

Table: Sobrescritas, abstratos e modificadores

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3016` | '<name>' é readonly, então só pode ser escrito onde é declarado ou em um construtor | Um valor readonly só é escrito onde é declarado ou em um construtor. Mova a escrita para lá. |
| `S3043` | '<method>' já existe em '<type>', então escreva com override para substituí-lo | O método substitui um de um tipo base. Escreva override antes dele. |
| `S3044` | '<method>' em '<type>' não é virtual, então nada pode substituí-lo; marque-o como virtual ali | O método base não pode ser substituído. Marque-o como virtual no próprio tipo. |
| `S3045` | '<method>' está escrito com override, mas nada acima de '<type>' o tem | override indica um método que nenhum tipo base tem. Remova o override, ou confira o nome. |
| `S3046` | '<method>' não tem corpo, então '<type>' precisa ser declarado como abstrato | Um método sem corpo torna o seu tipo abstrato. Declare o tipo como abstrato. |
| `S3047` | '<method>' é abstrato, então é escrito com ponto e vírgula e sem corpo | Um método abstrato termina com ponto e vírgula e não tem corpo. Remova o corpo. |
| `S3048` | '<method>' não tem corpo, então é escrito com abstract | Um método sem corpo precisa ser abstrato. Escreva abstract antes dele. |
| `S3049` | '<type>' não é abstrato, então precisa dar um corpo a '<method>' | Um tipo que não é abstrato precisa dar corpo a cada membro abstrato que tem. Dê um corpo ao método. |
| `S3018` | '<type>' diz que é um '<kind>', mas não tem '<member>' | O tipo diz que implementa algo, mas falta um dos seus membros. Escreva o membro que falta. |
| `S3051` | '<name>' não pode ser escrito com <modifier> | O modificador não combina com este tipo de membro. Remova-o. |

Table: O ponto de entrada e o subconjunto

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3017` | um programa precisa de exatamente um ponto de entrada, seja uma classe que implemente IScript ou uma classe com Main estático, e este tem <count> | O programa não tem ponto de entrada, ou tem mais de um. Mantenha exatamente uma classe que implemente IScript ou tenha Main estático. |
| `S3052` | Sigma não tem <feature>; <alternative> | Um arquivo Sigma usa um recurso que só o Sigma# tem. Escreva o arquivo em Sigma#, ou use a alternativa que a mensagem indica. |

Table: Formatos printf

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3053` | o formato de <name> precisa ser escrito onde é usado, entre aspas, para que as suas lacunas possam ser lidas | Um formato printf fica guardado em um nome. Escreva o formato como texto entre aspas, onde é usado. |
| `S3054` | <name>: <problem> | O texto do formato está errado. A segunda parte da mensagem diz o que há de errado nele. |
| `S3055` | <name>: o formato tem <count> e a chamada passa <count> | O formato tem número de lacunas diferente do número de valores da chamada. Dê um valor para cada lacuna. |
| `S3056` | <name>: '%<letter>' recebe <type>, e isto é <type> | Uma lacuna recebe um tipo de valor, e a chamada passa outro. Dê um valor do tipo que a lacuna recebe. |

Table: Um sistema e um desktop próprios

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3061` | '<nome>' é escrito com =>, então o valor dele tem de ser um que o compilador conhece: um escrito por extenso, ou um que a biblioteca nomeia | Uma propriedade escrita com => guarda um valor calculado enquanto o programa roda. Escreva o valor por extenso, como "Lantern OS" ou 48, ou nomeie um da biblioteca, como Era.Legacy. |
| `S3062` | '<tipo>' diz o que é com '<propriedade>', escrito com => e um valor que o compilador conhece, e não tem nenhum | Um sistema ou um desktop deixa de fora uma das propriedades que tem de declarar. Acrescente a propriedade que a mensagem nomeia, escrita com =>. |
| `S3063` | '<valor>' não é um de <opções> | Uma palavra que um sistema ou um desktop declara não é uma que o mod conhece, como um escalonador ou um lugar de painel. Use uma das palavras que a mensagem lista. |
| `S3064` | um programa tem no máximo um sistema e um desktop, e este tem <quantidade> <tipo> | Duas classes de uma mesma compilação implementam ISystem, ou duas implementam IDesktop. Mantenha uma de cada, e compile a outra numa pasta própria. |
| `S3065` | '<nome>' escolhe para um kernel próprio do sistema: comece-o com Kernel.Own(), pois Kernel.Of toma um dos kernels do mod como ele é | Uma política, como Scheduler, é escolhida depois de Kernel.Of. Comece o kernel com Kernel.Own() para escolher as políticas dele, ou deixe Kernel.Of sozinho. |

Table: A versão 3 da linguagem

| Código | Mensagem | O que significa e a correção |
| --- | --- | --- |
| `S3066` | '<nome>' é uma const, então o valor dela nunca muda | Algo escreve numa const. Use uma variável para um valor que muda. |
| `S3067` | '<nome>' é uma const, então o valor dela é um conhecido ao compilar | O valor chama um método ou lê uma variável. Faça-o de literais, consts e operadores. |
| `S3068` | o valor da const '<nome>' é calculado a partir dela mesma | Duas consts são calculadas uma a partir da outra. Dê a uma delas um valor próprio. |
| `S3069` | '<nome>' é uma const, então recebe o seu valor onde é declarada | Escreva o valor depois de um = na linha da const. |
| `S3070` | uma const não pode ser um '<tipo>' a não ser que seja null | Uma const guarda um número, um bool, um char, uma string ou o valor de um enum. Use um campo static readonly para o resto. |
| `S3071` | o rótulo '<nome>' está escrito duas vezes neste método | Dê a cada rótulo de um método um nome próprio. |
| `S3072` | não há rótulo '<nome>' neste método | Um goto, um comefrom ou um abstainfrom nomeia um rótulo que o método não tem. Verifique a grafia. |
| `S3073` | '<nome>' está dentro de um bloco em que isto não pode entrar | Um salto pode sair de um bloco, nunca entrar num. Tire o rótulo do bloco. |
| `S3074` | o switch em que isto está não tem <braço> | Um goto case ou goto default nomeia um braço que o switch não tem. Acrescente o braço. |
| `S3075` | 'goto <braço>' salta dentro do switch em que está, e não há nenhum | Escreva goto case e goto default só dentro de um switch. |
| `S3076` | o rótulo '<nome>' já tem um comefrom | Um rótulo aceita um comefrom. Fique com um. |
| `S3077` | um comefrom fica num bloco que contém o seu rótulo | Mova o comefrom para o bloco do rótulo ou para um bloco em volta dele. |
| `S3078` | só uma variável local, um campo ou um campo estático podem ser ignorados ou lembrados | Nomeie uma variável ou um campo depois de ignore e remember. |
| `S3079` | '<nome>' é sealed, então nada pode se apoiar nele nem substituí-lo | Tire o sealed da classe ou do método, ou apoie-se em outro. |
| `S3080` | '<nome>' não substitui nada: sealed vai com override | Escreva sealed só num método que faz override. |
| `S3081` | '<nome>' está numa classe estática, então também tem de ser estático | Escreva static no membro. |
| `S3082` | '<nome>' é uma classe estática, então nunca é criada com new | Chame os membros dela pelo nome da classe. |
| `S3083` | uma classe estática não se apoia em nada | Tire a classe base da classe estática. |
| `S3084` | params vai no último parâmetro | Mova o parâmetro params para o fim. |
| `S3085` | um parâmetro params é um array que o método lê | Escreva o parâmetro params como um array, e não com out. |
| `S3086` | nameof recebe um nome | Entregue ao nameof uma variável local, um membro ou um tipo, escrito como você o lê. |
| `S3087` | '<nome>' é uma função local, então nada pode ser escrito nela | Use outro nome para a variável. |
| `S3088` | uma tupla tem de dois a oito lugares | Mantenha entre dois e oito valores, ou escreva um struct. |
| `S3089` | o lugar de uma tupla guarda um valor, e '<tipo>' não é nenhum | Dê um valor ao lugar, ou escreva o tipo da tupla. |
| `S3090` | '<tipo>' não é uma tupla de <contagem> lugares | Desmonte a tupla em tantos lugares quantos ela tem. |
| `S3091` | 'this' vai no primeiro parâmetro de um método de extensão | Escreva this só no primeiro parâmetro, um comum. |
| `S3092` | um método de extensão é um método estático de uma classe estática no topo de um namespace | Mova o método para uma classe assim, e faça-o estático. |
| `S3093` | '<sinal>' não é um operador que um tipo pode declarar | Um operador é public e static, e recebe tantos valores quantos lados o seu sinal tem. |
| `S3094` | um operador de '<tipo>' recebe um '<tipo>' como um dos seus valores | Dê ao operador um parâmetro do próprio tipo. |
| `S3095` | '<tipo>' declara o operador <sinal>, então também declara <sinal> | Declare a comparação parceira, recebendo os mesmos valores. |
| `S3096` | um indexador é de um objeto, e recebe um valor entre os seus colchetes | Dê ao indexador um parâmetro, e tire o static dele. |
| `S3097` | um catch recebe uma exceção, e '<tipo>' não é uma | Capture Exception ou uma classe que se apoia nela. |
| `S3098` | um catch antes deste já recebe todo '<tipo>' | Ponha primeiro o catch do tipo mais estreito. |
| `S3099` | nada sai de um finally a não ser o seu próprio fim | Tire o return, break, continue ou salto de dentro do finally. |
| `S3100` | só uma exceção pode ser lançada, e '<tipo>' não é uma | Lance um objeto de Exception ou de uma classe que se apoia nela. |
| `S3101` | 'throw;' relança a exceção que um catch recebeu | Escreva throw sozinho só num catch, não num finally dentro dele. |
| `S3102` | um ponteiro só é guardado em código unsafe | Escreva unsafe no método, ou ponha as linhas num bloco unsafe. |
| `S3103` | '<tipo>\*' não é um ponteiro que a linguagem tem | Aponte para um int, um long, um byte, void ou um struct que só guarda valores. |
| `S3104` | & recebe uma variável local, um parâmetro, ou um lugar que um ponteiro alcança | Tome o endereço de uma variável, ou do que um ponteiro alcança, e não de um campo de um objeto nem de um valor. |
| `S3105` | o endereço de '<nome>' não pode ser tomado: um lambda ou uma função local o guarda | Copie o valor para uma variável própria, e tome o endereço dela. |
| `S3106` | '<nome>' vive na memória crua quando o seu endereço é tomado | Não ignore uma variável cujo endereço você tomou. |
| `S3107` | sizeof recebe um tipo que fica na memória crua, e '<tipo>' não fica | Peça o sizeof de um número, um bool, um char, um ponteiro ou um struct de valores. |

## Se algo der errado

> [!PROBLEM] Diz que '<name>' não se refere a nada aqui.
> O nome está escrito errado, ou é declarado depois desta linha. Confira a grafia, ou declare-o acima.

> [!PROBLEM] Diz que não é possível converter '<type>' em '<type>'.
> Escreva o valor como o tipo que a mensagem indica, ou mude o tipo do lugar onde ele vai.

> [!PROBLEM] Diz que nenhuma versão de '<name>' aceita esses argumentos.
> Passe os valores que o método recebe, na ordem em que ele os recebe.

> [!PROBLEM] Diz que um programa precisa de exatamente um ponto de entrada.
> Mantenha exatamente uma classe que implemente IScript ou tenha um Main estático.

> [!SEE] jsc:sigma_compile_errors
