# clj_exploration_leaks

## conexp-clj

When running certain methods of conexp-clj I ran into compatibility problems:
While the highest supported Java version is 21, the library require a Java 23/ openjdk-23-jdk.
It is possible to overcome this issue by rather using a standalone jar 
than the default web repository from maven/ clojars.
In order to install the standalone jar with java 21 use the following command:
```shell
mvn install:install-file -Dfile=/home/kgutekunst/IdeaProjects/conexp-clj/builds/uberjar/conexp-clj-2.6.0-standalone.jar -DgroupId=conexp-clj -DartifactId=conexp-clj -Dversion=2.6.0+java21 -Dpackaging=jar
```



## Usage


## License
For open source projects, say how it is licensed.

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
