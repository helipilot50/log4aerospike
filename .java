package com.aerospike.log4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;


/**
@author Peter Milne
*/
public class  {
	private AerospikeClient client;
	private String seedHost;
	private int port;
	private String namespace;
	private String set;
	private WritePolicy writePolicy;
	private Policy policy;

	private static Logger log = Logger.getLogger(.class);
	public (String host, int port, String namespace, String set) throws AerospikeException {
		this.client = new AerospikeClient(host, port);
		this.seedHost = host;
		this.port = port;
		this.namespace = namespace;
		this.set = set;
		this.writePolicy = new WritePolicy();
		this.policy = new Policy();
	}
	public (AerospikeClient client, String namespace, String set) throws AerospikeException {
		this.client = client;
		this.namespace = namespace;
		this.set = set;
		this.writePolicy = new WritePolicy();
		this.policy = new Policy();
	}

	public void work() throws Exception {
		// TODO add your code here
	}

}