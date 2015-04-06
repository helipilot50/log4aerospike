/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aerospike.log4j;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.MDC;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;


/**
 * Log4j appender writing log entries into a Aerospike cluster.
 * 
 * @author Peter Milne
 */
public class AerospikeLog4jAppender extends AppenderSkeleton {

	public static final String LEVEL = "level";
	public static final String NAME = "name";
	public static final String APP_ID = "applicationId";
	public static final String TIMESTAMP = "timestamp";
	public static final String PROPERTIES = "properties";
	public static final String TRACEBACK = "traceback";
	public static final String MESSAGE = "message";
	public static final String YEAR = "year";
	public static final String MONTH = "month";
	public static final String DAY = "day";
	public static final String HOUR = "hour";

	protected String host = "localhost";
	protected int port = 3000;
	protected String namespace;
	protected String set = "logs";
	protected String collectionPattern = "%c";
	protected PatternLayout collectionLayout = new PatternLayout(collectionPattern);
	protected String applicationId = System.getProperty("APPLICATION_ID", null);
	protected AerospikeClient client;
	protected int TTL = 30 * 24 * 60 * 60; // Number of seconds in 30 Days

	public AerospikeLog4jAppender() {
	}

	public AerospikeLog4jAppender(boolean isActive) {
		super(isActive);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getSet() {
		return this.set;
	}

	public void setSet(String set) {
		this.set = set;
	}

	public int getTTL() {
		return TTL;
	}

	public void setTTL(int tTL) {
		TTL = tTL;
		if (this.client != null && this.client.isConnected()){
			this.client.writePolicyDefault.expiration = this.TTL;
		}
	}

	public String getCollectionPattern() {
		return collectionPattern;
	}

	public void setCollectionPattern(String collectionPattern) {
		this.collectionPattern = collectionPattern;
		this.collectionLayout = new PatternLayout(collectionPattern);
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public AerospikeClient getClient() {
		return client;
	}

	public void setClient(AerospikeClient client) {
		this.client = client;
	}

	protected void connectToAerospike() throws UnknownHostException {
		if (this.client == null || !this.client.isConnected()){
			this.client = new AerospikeClient(host, port);
			this.client.writePolicyDefault.expiration = this.TTL;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
	 */
	@Override
	@SuppressWarnings({ "unchecked" })
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

	/*
	 * (non-Javadoc)
	 * @see org.apache.log4j.AppenderSkeleton#close()
	 */
	public void close() {
		if (this.client != null) {
			this.client.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
	 */
	public boolean requiresLayout() {
		return true;
	}
}
