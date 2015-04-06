# log4aerospike

Log4Aerospike is a log4j appender which allows log messages to be stored in an Aerospike NoSQL database

##Problem
Your Enterprise Java application uses the defacto industry standard logging facility Log4j. Log messages are generated through out your application and your application usage has become internet scale. As a result, your logging requirement has outstripped your capacity.

Additionally, you need now to query the logs in real-time to provide business intelligence and system diagnostics. 

##Solution
A solution is to use Aerospike NoSQL database as the log storage and a Log4j Appender that stores log messages in Aerospike.

There is no change to the application logic or code, the Appender is configured in the `log4j.properties` file.

On of the nice benefits of using Aerospike as the log storage is that each log message can have a time-to-live. So if you set the TTL to 30 days, log entries older than 30 days will automatically be deleted.

### How to build
The source code for this solution is available on GitHub, and the README.md 
https://github.com/helipilot50/log4aerospike.git. 

This core requires a working Java development environment (Java 6 and above) including Maven (Maven 2). The Aerospike Java client, and Log4j, will be downloaded from Maven Central as part of the build.

After cloning the repository, use maven to build the jar files. From the root directory of the project, issue the following command:
```bash
mvn clean package
```
A JAR file will be produced in the directory 'target', `aerospike-top-10-1.0-full.jar` that contains the code for the appender and all it's dependencies including the Aerospike client

### How to use

Place the jar on the class path of your application so it is available to the class loader.


Add the appender configuration to your `log4j.properties` file similar to the following:

```
log4j.appender.stdout=com.aerospike.log4j.AerospikeLog4jAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d %p [%c] - <%m>%n
log4j.appender.stdout.namespace = test
log4j.appender.stdout.set = logs
log4j.appender.stdout.ttl = 300
log4j.appender.stdout.host = localhost
log4j.appender.stdout.port = 3000
log4j.appender.stdout.collectionPattern = %X{year}%X{month}
log4j.appender.stdout.applicationId = my.application
```
- The `log4j.appender.stdout.namespace` property configures the Aerospike Namespace to be used.
- The `log4j.appender.stdout.set` property configures the Aerospike Set to be used.
- The `log4j.appender.stdout.ttl` property configures the Aerospike time-to-live, in seconds, for each log entry.
- The properties `log4j.appender.stdout.host` and `log4j.appender.stdout.port` configure the seed host for the cluster.


## Discussion

This single class `AerospikeLog4jAppender`, that sub classes `AppenderSkeleton`, forms the actual appender. Two methods `append` and `close` are overridden to implement the functionality of the appender.

### Append
The `append()` method is where all the work is done. The elements of the logging event are stored in separate Bins, this allows indexes to created on them and value base queries to be run agains the log entries.

```java
	protected void append(final LoggingEvent event) {
		if (null == this.client) {
			try {
				connectToAerospike();
			} catch (UnknownHostException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		List<Bin> binList = new ArrayList<Bin>();
		StringBuilder keyString = new StringBuilder();
		if (null != applicationId) {
			MDC.put(APP_ID, applicationId);
			keyString.append(applicationId).append(":");
			binList.add(new Bin(APP_ID, applicationId));
		}
		String eventName = event.getLogger().getName();
		binList.add(new Bin(NAME, eventName));
		keyString.append(eventName).append(":");
		binList.add(new Bin(LEVEL, event.getLevel().toString()));
		keyString.append(event.getLevel().toString()).append(":");
		Calendar tstamp = Calendar.getInstance();
		tstamp.setTimeInMillis(event.getTimeStamp());
		binList.add(new Bin(TIMESTAMP, event.getTimeStamp()));
		keyString.append(event.getTimeStamp());
		
		// Copy properties into the record
		Map<Object, Object> props = event.getProperties();
		if (null != props && props.size() > 0) {
			Map<String, String> propsMap = new HashMap<String, String>();
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				propsMap.put(entry.getKey().toString(), entry.getValue().toString());
			}
			binList.add(new Bin(PROPERTIES, propsMap));
		}

		// Copy traceback info (if there is any) into the record
		String[] traceback = event.getThrowableStrRep();
		if (null != traceback && traceback.length > 0) {
			List<String> tb = new ArrayList<String>();
			tb.addAll(Arrays.asList(traceback));
			binList.add(new Bin(TRACEBACK, tb));
		}

		// Put the rendered message into the record
		binList.add(new Bin(MESSAGE, event.getRenderedMessage()));

		// Insert a record
		Calendar now = Calendar.getInstance();
		MDC.put(YEAR, now.get(Calendar.YEAR));
		MDC.put(MONTH, String.format("%1$02d", now.get(Calendar.MONTH) + 1));
		MDC.put(DAY, String.format("%1$02d", now.get(Calendar.DAY_OF_MONTH)));
		MDC.put(HOUR, String.format("%1$02d", now.get(Calendar.HOUR_OF_DAY)));

		MDC.remove(YEAR);
		MDC.remove(MONTH);
		MDC.remove(DAY);
		MDC.remove(HOUR);
		if (null != applicationId) {
			MDC.remove(APP_ID);
		}
		Key key = new Key(this.namespace, this.set, keyString.toString());
		this.client.put(null, key, binList.toArray(new Bin[0]));
	}

```

### Close
Overriding the `close()` method allows the appender to close the Aerospike client. This coses the socket connections in the client, terminates the cluster monitoring thread and destroys the worker thread pool.

```java
	public void close() {
		if (this.client != null) {
			this.client.close();
		}
	}
```
