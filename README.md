# LibreBounce
A [libre software](https://www.gnu.org/philosophy/free-sw.html), mixin-based injection hacked client for the Forge Mod Loader, version 1.8.9; it's a fork of [LiquidBounce legacy](https://github.com/CCBlueX/LiquidBounce/tree/legacy), aiming to compete with paid & nonlibre hacked clients.

If anyone would like to contact me through Discord, my username is `thatonecoder_`. More contact methods may be found through my [Codeberg profile page](https://codeberg.org/thatonecoder), although I cannot guarantee any attempts will be successful.

## Issues
If you notice any bugs or would like a feature added, you can let us know by opening an issue [here](https://github.com/LibreBounce/LibreBounce/issues).

## License
This project is subject to the [GNU General Public License v3.0](LICENSE). This only applies for source code located directly in this clean repository. During the development and compilation process, additional source code may be used to which we have obtained no rights. Such code is not covered by the GPL license.
For those who are unfamiliar with the license, here is a summary of its main points. This is by no means legal advice nor legally binding.

You are allowed to use, share, or modify this project, entirely or partially, for free (and even commercially). However, please consider the following:

- **You must distribute the source code of your modified work and the source code you took from this project, along with any binary you distribute. This means you are not allowed to use code from this project (even partially) in a closed-source (or even obfuscated) application.**
- **Your modified application must also be licensed under the GPL.**

Do the above and share your source code with everyone, just like we do.

## Setting up a Workspace
LibreBounce is using Gradle, so make sure that it is installed properly. Instructions can be found on [Gradle's website](https://gradle.org/install/).
1. Clone the repository using `git clone https://github.com/LibreBounce/LibreBounce/`. 
2. CD into the local repository folder.
3. Depending on which IDE you are using execute either of the following commands:
    - For IntelliJ: `gradlew --debug setupDevWorkspace idea genIntellijRuns build`
    - For Eclipse: `gradlew --debug setupDevWorkspace eclipse build`
5. Open the folder as a Gradle project in your IDE.
6. Select either the Forge or Vanilla run configuration.

## Additional libraries
### Mixins
Mixins can be used to modify classes at runtime before they are loaded. LibreBounce is using it to inject its code into the Minecraft client. This way, none of Mojang's copyrighted code is shipped. If you'd like to learn more about it, check out its [documentation](https://docs.spongepowered.org/5.1.0/en/plugin/internals/mixins.html).

## Contributing
We highly appreciate contributions. If you would like to support us, feel free to make changes to LibreBounce's source code and submit a pull request. Currently, our main goals are the following, by order of priority:

1. Make rotation patterns that bypass advanced anti-cheats (such as Polar),
2. Fix a bug where rotation modules still affect the player after being turned off.
3. Make a centralized clicking system, with legit patterns.
4. Add full backwards-compatibility with historical LiquidBounce versions (b68, b72).

Any additional goals are easily found by code searching "TODO", and are equally as important, if not more.
If you have experience in one or more of these fields, we would highly appreciate your support.

## Stats
![Alt](https://repobeats.axiom.co/api/embed/9ba0cbee722c2c27fba8d83cfc0233dc430ea204.svg "Repobeats analytics image")
