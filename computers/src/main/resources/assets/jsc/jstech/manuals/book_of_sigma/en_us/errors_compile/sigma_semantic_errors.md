---
section: errors_compile
order: 489
---
# Meaning errors

A meaning error is a line whose shape is right but whose names or types do not fit. The compiler checks meaning once the shape of the file is clean, so these codes usually appear after the syntax errors are gone. The editor's check while you type is the exception.

The words in angle brackets stand for what the compiler found in your file: a name, a type, a member of a type, a number or a token. Your message shows the real words in their place.

> [!DEFINE] hole
> A place inside a text where a value is put in: the braces of an interpolated string, or a marker of a printf format that the format fills with a value.

Table: Names and declarations

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S2012` | '<name>' names a namespace; write 'using <name>.\*;' to bring in everything in it, or name one of its types | A namespace was named where a type is needed. Write the using line with a star, or name one type of the namespace. |
| `S3001` | '<name>' does not name anything here | The name is not declared where you use it. Check its spelling, or declare it above this line. |
| `S3002` | '<name>' is already declared here | Two things have the same name in one place. Give one of them another name. |
| `S3040` | '<name>' is in <namespace>; add 'using <namespace>.\*;' or 'using <namespace>.<name>;' at the top of the file | The name lives in a namespace you did not bring in. Add the using line the message gives, at the top of the file. |
| `S3059` | '<name>' already stands behind '<name>', so it cannot also stand in front of it | Two types each name the other as their base, in a circle. Remove the base that closes the circle. |
| `S3060` | '<name>' is <private or protected> to '<type>', so it cannot be reached from here | The member is private to its type, which is what a member written with no word of access is, or protected to it and the types that stand on it. Mark it public, or reach it from inside its type. |
| `S3031` | only a class or an interface can stand behind the colon, and '<name>' is neither | A type takes a base after its colon that is not a class or an interface. Name a class or an interface there. |
| `S3041` | a struct can implement interfaces but stands on no class, and '<name>' is a class | A struct names a class as its base, but a struct can only implement interfaces. Remove the class, or make the type a class. |
| `S3015` | '<name>' has no base class | The program uses a base-class member where its type has no base class. Check the type, or remove that use. |
| `S3057` | '<name>' needs <level> <version>; this project is <level> <version> | A name is newer than the language version your project holds to. Raise the project's version, or do not use the name. |
| `S3028` | '<name>' does not name a type | This message is no longer shown, so you should not meet it. If you do, write the name of a type in that place. |

Table: Types and conversions

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3003` | cannot convert '<type>' to '<type>' | A value of one type is used where another type is needed, with no safe conversion. Write a value of the right type. |
| `S3019` | '<name>' takes <count> type arguments | A generic type got the wrong number of types. Give it the count the message says. |
| `S3039` | an enum's number has to be written as a number | An enum value gives a number that is not written as a plain number. Write the number itself. |
| `S3009` | a condition is a bool, not '<type>' | An if or a loop tests something that is not true or false. Write a test that gives true or false. |
| `S3022` | only an object can be disposed, not '<type>' | Dispose works on objects only. Dispose an object, not a plain value. |
| `S3042` | only an object can be locked, not '<type>' | Lock works on objects only. Lock an object. |
| `S3023` | foreach walks an array or a list, not '<type>' | A foreach loop needs an array or a list. Give it one of them. |

Table: Operators, calls and members

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3007` | '<operator>' cannot be applied to '<type>' and '<type>' | The two values do not work together with this operator. Use values of a type the operator takes. |
| `S3008` | '<operator>' cannot be applied to '<type>' | The operator does not take this type. Use a value of a type it takes. |
| `S3024` | '<type>' cannot be indexed by '<type>' | The index is the wrong kind of value for this type. Give an index of the kind it takes. |
| `S3025` | '<name>' is not something that can be called | You called a name that is not a method or a delegate. Check the spelling, or call a method. |
| `S3026` | '<type>' cannot be made with new | new was used with a type that cannot be made that way. Make an object of a type that can. |
| `S3050` | '<type>' is abstract, so it cannot be made with new | An abstract type has no objects of its own. Make an object of a type that is not abstract. |
| `S3005` | no version of '<name>' takes those arguments | The values you give fit no version of the method. Give values of the types one version takes. |
| `S3006` | the call to '<name>' fits more than one version of it | More than one version fits the values. Give a value of a type that fits only one version. |
| `S3004` | '<type>' has no member called '<name>' | The name is not a member of that type. Check the spelling, or use a member the type has. |
| `S3020` | '<member>' belongs to the type, not to one of its objects | Write the type's name before the dot, not the name of one of its objects. |
| `S3021` | '<member>' belongs to an object, not to the type | A member of an object is used through the type's name. Use an object of the type. |
| `S3014` | '<name>' is not available in a static member | A static member has no object, so this value does not exist there. Use a value that does not need an object. |
| `S3033` | '<name>' is a method: call it, or hand it over where a delegate of its shape is wanted | A method is used as a value without its brackets. Call it, or give it where a delegate is expected. |
| `S3027` | this lambda does not have the shape '<shape>' takes | The lambda's values do not match the delegate it is given to. Make it take the values the delegate takes. |

Table: Loops, switches and returns

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3029` | this switch already has that label | Two cases of the switch have the same label. Keep one of them. |
| `S3010` | 'break' only means something inside a loop or a switch | break stands outside any loop or switch. Move it inside one, or delete it. |
| `S3011` | 'continue' only means something inside a loop | continue stands outside any loop. Move it inside a loop, or delete it. |
| `S3012` | a method that gives back '<type>' must return a value | The method ends without giving back a value. Return a value of that type. |
| `S3013` | a method that gives back nothing cannot return a value | The method returns a value, but it gives back nothing. Remove the value after return. |

Table: Out, ref and events

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3034` | '<name>' is filled in by the method, so the argument is written with out | The method fills in this argument. Write out before it. |
| `S3035` | '<name>' is read by the method, so the argument cannot be written with out | The method only reads this argument. Remove out. |
| `S3036` | an out argument is exactly '<type>', with no conversion on the way | The out variable must be exactly the type the method fills. Declare it with that type. |
| `S3037` | '<name>' is filled in by this method, so it must be given a value before every way out | One way out of the method leaves the out value empty. Give it a value before every return. |
| `S3038` | '<name>' is not somewhere a method can write | The method can only write into a place. Give it a place, not a plain value. |
| `S3030` | an event can only be raised inside the type that declares it | The event is raised from outside its type. Raise it inside the type. |
| `S3032` | an event's type must be a delegate, and '<type>' is not one | An event needs a delegate type. Give it a delegate. |
| `S3058` | 'ref' is written only for strcat, which joins onto the text '<name>' holds; a method fills in with out | ref is only for joining text. Use out for a method that fills a value in. |

Table: Overrides, abstract and modifiers

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3016` | '<name>' is readonly, so it can only be written where it is declared or in a constructor | A readonly value is written only where it is declared or in a constructor. Move the write there. |
| `S3043` | '<method>' is already on '<type>', so write it with override to replace it | The method replaces one from a base type. Write override before it. |
| `S3044` | '<method>' on '<type>' is not virtual, so nothing can replace it; mark it virtual there | The base method cannot be replaced. Mark it virtual in its own type. |
| `S3045` | '<method>' is written with override but nothing above '<type>' has it | override names a method that no base type has. Remove override, or check the name. |
| `S3046` | '<method>' has no body, so '<type>' has to be declared abstract | A method without a body makes its type abstract. Declare the type abstract. |
| `S3047` | '<method>' is abstract, so it is written with a semicolon and no body | An abstract method ends with a semicolon and has no body. Remove the body. |
| `S3048` | '<method>' has no body, so it is written with abstract | A method without a body must be abstract. Write abstract before it. |
| `S3049` | '<type>' is not abstract, so it has to give '<method>' a body | A type that is not abstract must give a body to each abstract member it has. Give the method a body. |
| `S3018` | '<type>' says it is a '<kind>' but does not have '<member>' | The type says it implements something but misses one of its members. Write the missing member. |
| `S3051` | '<name>' cannot be written with <modifier> | The modifier does not fit this kind of member. Remove it. |

Table: The entry point and the subset

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3017` | a program needs exactly one place to start, either a class that implements IScript or a class with a static Main, and this one has <count> | The program has no place to start, or more than one. Keep exactly one class that implements IScript or has a static Main. |
| `S3052` | Sigma has no <feature>; <alternative> | A Sigma file uses a feature only Sigma# has. Write the file in Sigma#, or use the alternative the message names. |

Table: Printf formats

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3053` | <name>'s format has to be written out where it is used, in quotes, so its holes can be read | A printf format is kept in a name. Write the format as text in quotes, where it is used. |
| `S3054` | <name>: <problem> | The format text is wrong. The second part of the message says what is wrong with it. |
| `S3055` | <name>: the format has <count> and the call gives <count> | The format has a different number of holes than the call has values. Give one value for each hole. |
| `S3056` | <name>: '%<letter>' takes <type>, and this is <type> | A hole takes one kind of value, and the call gives another. Give a value of the kind the hole takes. |

Table: A system and a desktop of your own

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3061` | '<name>' is written with =>, so its value has to be one the compiler knows: one written out, or one the library names | A property written with => holds a value worked out while the program runs. Write the value out, such as "Lantern OS" or 48, or name one of the library's, such as Era.Legacy. |
| `S3062` | '<type>' says what it is with '<property>', written with => and a value the compiler knows, and has none | A system or a desktop leaves out one of the properties it has to declare. Add the property the message names, written with =>. |
| `S3063` | '<value>' is not one of <choices> | A word a system or a desktop declares is not one the mod knows, such as a scheduler or a panel place. Use one of the words the message lists. |
| `S3064` | a program holds at most one system and one desktop, and this one has <count> <kind> | Two classes in one build implement ISystem, or two implement IDesktop. Keep one of each, and build the other in a folder of its own. |
| `S3065` | '<name>' chooses for a kernel of the system's own: start it with Kernel.Own(), since Kernel.Of takes one of the mod's kernels as it is | A policy such as Scheduler is chosen after Kernel.Of. Start the kernel with Kernel.Own() to choose its policies, or keep Kernel.Of alone. |

Table: Version 3 of the language

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `S3066` | '<name>' is a const, so its value never changes | Something writes to a const. Use a variable for a value that changes. |
| `S3067` | '<name>' is a const, so its value is one known when compiling | The value calls a method or reads a variable. Make it of literals, consts and operators. |
| `S3068` | the value of the const '<name>' is worked out from itself | Two consts are worked out from each other. Give one of them a value of its own. |
| `S3069` | '<name>' is a const, so it is given its value where it is declared | Write the value after an = on the line of the const. |
| `S3070` | a const cannot be a '<type>' unless it is null | A const holds a number, a bool, a char, a string or an enum's value. Use a static readonly field for anything else. |
| `S3071` | the label '<name>' is written twice in this method | Give each label of a method a name of its own. |
| `S3072` | there is no label '<name>' in this method | A goto, a comefrom or an abstainfrom names a label the method lacks. Check the spelling. |
| `S3073` | '<name>' is inside a block this cannot enter | A jump may leave a block, never enter one. Move the label out of the block. |
| `S3074` | the switch this stands in has no <arm> | A goto case or goto default names an arm the switch lacks. Add the arm. |
| `S3075` | 'goto <arm>' jumps inside the switch it stands in, and there is none | Write goto case and goto default only inside a switch. |
| `S3076` | the label '<name>' already has a comefrom | A label takes one comefrom. Keep one. |
| `S3077` | a comefrom stands in a block that holds its label | Move the comefrom to the label's block or a block around it. |
| `S3078` | only a local, a field or a static field can be ignored or remembered | Name a variable or a field after ignore and remember. |
| `S3079` | '<name>' is sealed, so nothing can stand on it or replace it | Take sealed off the class or the method, or stand on another one. |
| `S3080` | '<name>' replaces nothing: sealed goes with override | Write sealed only on a method that overrides. |
| `S3081` | '<name>' is in a static class, so it has to be static as well | Write static on the member. |
| `S3082` | '<name>' is a static class, so it is never made with new | Call its members through the class's name. |
| `S3083` | a static class stands on nothing | Take the base class off the static class. |
| `S3084` | params goes on the last parameter | Move the params parameter to the end. |
| `S3085` | a params parameter is an array the method reads | Write the params parameter as an array, and not with out. |
| `S3086` | nameof takes one name | Hand nameof a local, a member or a type, written as you read it. |
| `S3087` | '<name>' is a local function, so nothing can be written to it | Use another name for the variable. |
| `S3088` | a tuple has from two to eight places | Keep between two and eight values, or write a struct. |
| `S3089` | a tuple's place holds a value, and '<type>' is none | Give the place a value, or write the tuple's type. |
| `S3090` | '<type>' is not a tuple of <count> places | Take the tuple apart into as many places as it has. |
| `S3091` | 'this' goes on the first parameter of an extension method | Write this on the first parameter only, a plain one. |
| `S3092` | an extension method is a static method of a static class at the top of a namespace | Move the method to such a class, and make it static. |
| `S3093` | '<sign>' is not an operator a type can declare | An operator is public and static, and takes as many values as its sign has sides. |
| `S3094` | an operator of '<type>' takes a '<type>' as one of its values | Give the operator a parameter of its own type. |
| `S3095` | '<type>' declares the operator <sign>, so it declares <sign> as well | Declare the partner comparison, taking the same values. |
| `S3096` | an indexer is of an object, and takes one value in its brackets | Give the indexer one parameter, and take static off it. |
| `S3097` | a catch takes an exception, and '<type>' is not one | Catch Exception or a class standing on it. |
| `S3098` | a catch before this one already takes every '<type>' | Put the catch of the narrower type first. |
| `S3099` | nothing leaves a finally but its own end | Take the return, break, continue or jump out of the finally. |
| `S3100` | only an exception can be thrown, and '<type>' is not one | Throw an object of Exception or of a class standing on it. |
| `S3101` | 'throw;' throws on the exception a catch took | Write throw alone only in a catch, not in a finally inside one. |
| `S3102` | a pointer is held only in unsafe code | Write unsafe on the method, or put the lines in an unsafe block. |
| `S3103` | '<type>\*' is no pointer the language has | Point at an int, a long, a byte, void or a struct that holds only values. |
| `S3104` | & takes a local variable, a parameter, or a place a pointer reaches | Take the address of a variable, or of what a pointer reaches, and not of a field of an object or a value. |
| `S3105` | the address of '<name>' cannot be taken: a lambda or a local function keeps it | Copy the value into a variable of its own, and take that one's address. |
| `S3106` | '<name>' lives in raw memory once its address is taken | Do not ignore a variable whose address you took. |
| `S3107` | sizeof takes a type that lies in raw memory, and '<type>' does not | Ask sizeof of a number, a bool, a char, a pointer or a struct of values. |

## If something goes wrong

> [!PROBLEM] It says '<name>' does not name anything here.
> The name is spelled wrong, or it is declared after this line. Check its spelling, or declare it above.

> [!PROBLEM] It says cannot convert '<type>' to '<type>'.
> Write the value as the type the message names, or change the type of the place it goes.

> [!PROBLEM] It says no version of '<name>' takes those arguments.
> Give the values the method takes, in the order it takes them.

> [!PROBLEM] It says a program needs exactly one place to start.
> Keep exactly one class that implements IScript or has a static Main.

> [!SEE] jsc:sigma_compile_errors
