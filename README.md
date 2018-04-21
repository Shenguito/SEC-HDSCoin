# SEC-HDSCoin

HDS Coin is a projet introduced by Highly Dependable Systems course at IST. The main goal is to implement a secure and distributed server-client system.

The 1st stage's description is defined at SEC-1718Project-stage1.pdf

==== HOW TO RUN ====

On the root folder run:

mvn clean package

First run the server:

java -jar Server/target/Server-1.0-SNAPSHOT-jar-with-dependencies.jar

Then you can run the client:

java -jar Client/target/Client-1.0-SNAPSHOT-jar-with-dependencies.jar


Both Client and Server will show a custom shell that let's you execute the commands from the project statement
The server also has commands to crash and recover from a crash.

We have added three test users Alice, Bob and Charly, to skip the need to generate RSA Keys.

To run the tests first run the server, then execute

mvn test


Bugs: negative value, transaction signature, crash server may cause file corruption.

The 2nd stage's description is defined at SEC-1718Project-stage2.pdf
