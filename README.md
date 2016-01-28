# TWCore

## About TWCore

[Subspace Continuum][] is a 2D massive-multiplayer online game (MMOG) from the 90s that consists of a server and a client. Anyone can host their own [zone][Subspace Zone] (server) and allow clients to connect to it.

This is a project written in Java that emulates a client by sending the appropriate packets for authentication, registration, and any and all in-game player actions. When given moderator powers, these "bots" are able to perform a number of useful tasks within the game.

[Trench Wars][] is the particular zone for which TWCore was written, and has long been the most highly populated Subspace Continuum zone. Many of its bots and actions are geared towards competitive leagues or events within this zone. It has the capability of interfacing with MySQL databases so that statistics and other data can be displayed on easily accesible webpages.

Although TWCore was written for use in [Trench Wars][], it is open source and allowances were made for portability to other zones which may want to adapt their own version of TWCore. [Mervbot][] is an alternative bot core written in Visual C++ that some other zones utilize.

TWCore differs from Mervbot in that TWCore spawns a hub bot in the specified zone on startup which can then easily spawn other bots by command.

## Dependencies

TWCore runs on Java 7 and as such requires a [Java 7SE JDK][]. The build process utilizes [ant][]. The project is also fairly useless without access to a [Subspace Zone][].

Many developers choose to use [Eclipse][] as an interactive development environment (IDE); however, this is not a requirement.

## Building and Running

The simplest way to build and run TWCore is with [ant][]. In the directory with `build.xml` simply type `ant` which will create `bin/twcore.jar`. Before running the project, some configuration is necessary in the `bin/setup.cfg` file to specify which zone to connect to, what username to connect with, and so forth. After doing so the project can be run by typing `ant run` or executing the JAR file directly with the configuration file as an argument.

    java -jar bin/twcore.jar bin/setup.cfg

You can build the javadocs for the project with `ant javadoc`. See other build options with `ant -p`.

For further information, please visit our [wiki][].

   [Subspace Continuum]: http://www.getcontinuum.com/
   [Trench Wars]: http://www.trenchwars.org/
   [Mervbot]: http://mervbot.com/
   [Java 7SE JDK]: http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
   [ant]: https://ant.apache.org/
   [Subspace Zone]: http://www.minegoboom.com/server/index-768.html
   [Eclipse]: https://eclipse.org/
   [wiki]: https://github.com/Trench-Wars/twcore/wiki
