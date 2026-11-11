---
section: sharing_packages
order: 496
---
# Taking a package off the Mirror

Unpublishing removes a package from the Mirror, so no computer can install it after that. You name the package, not the file. The command is sgpack unpublish, followed by the package's name.

1. Type sgpack unpublish mypkg and press [[Enter]].
2. Read the answer: took mypkg off the Mirror.

Any player on the network can take any package off the Mirror. The Mirror keeps no record of who published a package, so nothing stops one player from removing another's package. Check with the other players before you take away a package they use.

## If something goes wrong

> [!PROBLEM] The command says usage: sgpack unpublish <name>.
> Type the package's name after unpublish.

> [!PROBLEM] The command says the Mirror is not serving the name you typed.
> The name is not on the Mirror. Check its spelling, then try again.

> [!PROBLEM] The command says could not resolve mirror://.
> No Mirror serves this network. Connect to a network whose Mainframe runs the Mirror ([](jsc:mirror)).

> [!SEE] jsc:sigma_publish_mirror
