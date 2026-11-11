---
section: sigma_objects
order: 420
---
# Propriedades, eventos, delegates e lambdas

O Sigma não tem nenhum dos recursos desta página. O Sigma# tem todos eles. O Sigma também recusa lambdas, por isso esta página é para o Sigma#.

Uma propriedade é um valor nomeado de um objeto que executa código quando é lido ou escrito. Você a escreve com os acessores get e set, como em int Size { get; set; }. Uma propriedade só com get pode ser lida, mas não escrita. O Sigma recusa propriedades: escreva um campo, ou um método que devolva o valor.

Um delegate é um formato nomeado de método. Um valor de um tipo delegate pode guardar qualquer método desse formato, e você o chama como se fosse um método. A linha delegate int Pick(int a, int b); declara um formato que recebe dois ints e devolve um int. O namespace System tem três delegates prontos: Action não recebe nada e não devolve nada, Action<T> recebe um valor e não devolve nada, e Func<T, R> recebe um valor e devolve um.

```sgs Pick.sgs
using System.*;
using System.IO.*;

namespace Shop;

delegate int Pick(int a, int b);

class Chooser
{
    static void Main()
    {
        Pick biggest = (int a, int b) => a > b ? a : b;
        Console.PrintLine("" + biggest(3, 9));
    }
}
```

Uma classe declara um evento com a palavra event e um tipo delegate, como em event Action Changed;. Outro código se inscreve com += e cancela a inscrição com -=. Só a classe que declara o evento pode dispará-lo, chamando-o.

Uma lambda é um método curto escrito no lugar. x => x \* 2 recebe um valor, e () => { instruções } não recebe nenhum, com um bloco de instruções como corpo. O formato vem do delegate ao qual a lambda é entregue, então os parâmetros e o resultado dela devem coincidir com esse delegate. Uma lambda pode usar as variáveis locais do método onde está, e pode usar this.

```sgs Count.sgs
using System.*;
using System.IO.*;

namespace Shop;

class Till
{
    static void Main()
    {
        int kept = 0;
        Action count = () => {
            kept++;
            Console.PrintLine("" + kept);
        };
        count();
    }
}
```

> [!DEFINE] Evento
> Uma forma de um objeto anunciar que algo aconteceu. Outros códigos se inscrevem nele com += e cancelam a inscrição com -=.

> [!DEFINE] Lambda
> Um método curto escrito no lugar, que assume o formato do delegate ao qual é entregue.

## Se algo der errado

> [!PROBLEM] O compilador diz que o tipo de um evento precisa ser um delegate.
> Dê ao evento um tipo delegate, como Action.

> [!PROBLEM] O compilador diz que um evento só pode ser disparado dentro do tipo que o declara.
> Chame um método da classe que declara o evento, e deixe esse método disparar o evento.

> [!PROBLEM] O compilador diz que esta lambda não tem o formato que 'Pick' recebe.
> Dê à lambda os mesmos parâmetros e o mesmo resultado do delegate.

> [!PROBLEM] O compilador diz que uma lambda que guarda uma variável declarada por um laço ainda não foi implementada.
> Declare a variável antes de o laço começar, para que a lambda guarde uma variável própria.

> [!PROBLEM] O compilador diz que 'Ring' é um método: chame-o, ou entregue-o onde se espera um delegate do mesmo formato.
> Escreva Ring() com os parênteses, ou entregue o método a um delegate do mesmo formato.

> [!SEE] jsc:sigma_methods_out_ref, jsc:sigma_namespaces_using
