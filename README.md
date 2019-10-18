# osoclient

Asynchronous client with cache for http service running on localhost:5000
accepting queries like:

`GET http://localhost:5000/squareme?num=9`

Above should return:
`{msg: 9
ttl: 200}``

## Installation

#### Leiningen

Run with `lein run 10000`
Run tests with `lein test`

#### Standalone JAR

Build jar with `lein uberjar`, or

Download jar, then follow Usage instructions

## Usage

Run 10000 randomly generated requests
    `$ java -jar osoclient-0.1.0-standalone.jar 10000`

## License

Copyright © 2019 Miguel Mejia

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
