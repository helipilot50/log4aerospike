# log4aerospike

Log4Aerospike is a log4j appender which allows log messages to be stored in an Aerospike NoSQL database

##Problem
Your Enterprise Java application uses the defacto industry standard logging facility Log4j. Log messages are generated through out your application and your application usage has become internet scale. As a result, your logging requirement has outstripped your capacity.

Additionally, you need now to query the logs in real-time to provide business intelligence and system diagnostics. 

##Solution
A solution is to use Aerospike NoSQL database as the log storage and a Log4j Appender that stores log messages in Aerospike.

The is no change to the application logic or code, the Appender is configured in the `log4j.properties` file.

### How to build
The source code for this solution is available on GitHub, and the README.md 
https://github.com/helipilot50/log4aerospike.git. 

This core requires a working Java development environment (Java 6 and above) including Maven (Maven 2). The Aerospike Java client, and Log4j, will be downloaded from Maven Central as part of the build.

After cloning the repository, use maven to build the jar files. From the root directory of the project, issue the following command:
```bash
mvn clean package
```
A JAR file will be produced in the directory 'target', `aerospike-top-10-1.0-full.jar`


##Discussion
