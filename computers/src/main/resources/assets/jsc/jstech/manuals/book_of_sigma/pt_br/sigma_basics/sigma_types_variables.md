---
section: sigma_basics
order: 413
---
# Tipos e variáveis

Uma variável é uma posição com nome que guarda um valor. O seu tipo diz que tipo de valor pode entrar nela, e o compilador recusa qualquer outro. Você declara uma variável escrevendo o seu tipo, depois o seu nome e, se quiser, um primeiro valor.

Cada declaração nomeia exatamente uma variável. Escreva int total = 0; para declarar uma; uma linha como int a, b; é recusada.

Table: Tipos embutidos

| Tipo | Guarda | Exemplo |
| --- | --- | --- |
| `int` | Um número inteiro, em 32 bits. | `int count = 42;` |
| `long` | Um número inteiro, em 64 bits, para números maiores. | `long big = 42L;` |
| `float` | Um número decimal, menor que um double. | `float f = 1.5f;` |
| `double` | Um número decimal, com mais precisão. | `double half = 0.5;` |
| `bool` | true ou false. | `bool ok = true;` |
| `char` | Um caractere. Dentro da máquina, o seu número. | `char letter = 'A';` |
| `string` | Texto, que é um tipo de referência. | `string text = "hi";` |
| `object` | Um valor de qualquer tipo. | `object anything = 5;` |

Os tipos se dividem em dois grupos, e o grupo decide o que acontece quando você copia um valor. Um tipo de valor, como int, bool ou char, é copiado sempre que você o armazena ou passa adiante. Um tipo de referência, como texto ou um objeto de uma classe, é compartilhado: dois nomes podem apontar para o mesmo objeto, então uma mudança por um nome aparece no outro.

Um texto é um tipo de referência, então pode guardar null, o valor vazio.

Números entram em tipos numéricos maiores sem pedir nada. Um int entra em um long, um float ou um double. O contrário precisa de um cast: você escreve o novo tipo entre parênteses na frente do valor, como em (int) 2.5. O cast diz ao compilador que você aceita a mudança. Sem o cast, a linha é recusada.

```sg Converting.sg
int total = 0;
long big = total;          // widening: fine
int back = (int) big;      // narrowing needs the cast
double avg = total / 2.0;  // int divided by double gives a double
```

A palavra null é o valor vazio de um tipo de referência. Ela serve a qualquer tipo que guarde objetos, como texto ou um objeto de uma classe, mas não serve a um int.

O Sigma# tem a palavra var, que lê o tipo a partir do valor: var x = 5; faz x ser um int. O Sigma recusa var, e você escreve o tipo por extenso.

> [!DEFINE] Classe
> Um modelo com nome para objetos: lista os campos e métodos que cada objeto tem.

> [!DEFINE] Objeto
> Um valor criado a partir de uma classe com new. Ele guarda os próprios campos e executa os próprios métodos.

> [!DEFINE] Tipo de referência
> Um tipo cujos valores são objetos que podem ser compartilhados, de modo que dois nomes podem apontar para um mesmo objeto.

> [!DEFINE] Tipo de valor
> Um tipo cujos valores são copiados sempre que são armazenados ou passados adiante.

> [!DEFINE] Null
> O valor vazio de um tipo de referência, que não aponta para nenhum objeto.

## Se algo der errado

> [!PROBLEM] O compilador diz que não é possível converter 'double' em 'int'.
> Use um cast se o que você quer é perder a parte fracionária, ou declare a variável como double. O erro é explicado em [os erros de semântica](jsc:sigma_semantic_errors).

> [!PROBLEM] O compilador diz que 'total' não se refere a nada aqui.
> Confira a grafia, e confira se você declarou a variável acima da linha que a usa.

> [!PROBLEM] O compilador recusa var.
> O Sigma não tem var: escreva o tipo por extenso, como em int x = 5;

> [!SEE] jsc:sigma_operators, jsc:sigma_classes
