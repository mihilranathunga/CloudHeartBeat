package org.wso2.cloud.heartbeat.monitor.modules.bam;

import me.prettyprint.cassandra.model.AllOneConsistencyLevelPolicy;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.FailoverPolicy;
import me.prettyprint.hector.api.*;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;

import java.util.HashMap;
import java.util.List; 
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.cloud.heartbeat.monitor.modules.ues.UESTenantLoginTest;

public class CassandraLogColumnFamilyExistanceTestHector implements Job{

	/**
	 * @param args
	 */
	
	private static final Log log = LogFactory.getLog(CassandraLogColumnFamilyExistanceTestHector.class);

	private final String TEST_NAME = "CassandraLogColumnFamilyExistanceTest";

	
	
    private static String keyspaceName = "EVENT_KS";
    private static KeyspaceDefinition newKeyspaceDef;
    private static Cluster cluster;
    private static Keyspace ksp;
    private static String cassandraHost = "localhost:9160";
    
    
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		
		log.info(TEST_NAME +" - Executed.......: "+ cassandraHost);
		
		try {
	        Map<String, String> AccessMap = new HashMap<String, String>();
	        AccessMap.put("username", "admin");
	        AccessMap.put("password", "admin");

	        cluster = HFactory.getOrCreateCluster("test cluster",
	        new CassandraHostConfigurator(cassandraHost), AccessMap);
	        ConfigurableConsistencyLevel ccl = new ConfigurableConsistencyLevel();
	        ccl.setDefaultReadConsistencyLevel(HConsistencyLevel.ONE);

	        newKeyspaceDef = HFactory.createKeyspaceDefinition(keyspaceName);
	        ColumnFamilyDefinition cfDef = HFactory.createColumnFamilyDefinition("MyKeyspace",                              
	                "ColumnFamilyName", 
	                ComparatorType.BYTESTYPE);
	        List<ColumnFamilyDefinition> lCf = newKeyspaceDef.getCfDefs(); //= new ArrayList<ColumnFamilyDefinition>();
	        
	        for (ColumnFamilyDefinition columnFamily : lCf) {
	            System.out.println(columnFamily.getName());
	        }
	        if((cluster.describeKeyspace(keyspaceName)) == null){
	            createSchema();
	        }

	        ksp = HFactory.createKeyspace(keyspaceName, cluster , new AllOneConsistencyLevelPolicy(), FailoverPolicy.ON_FAIL_TRY_ALL_AVAILABLE, AccessMap);
	        //Articles art = new Articles(cluster, newKeyspaceDef);
	        //cluster.dropColumnFamily(keyspaceName, "Articles");
        } catch (HectorException e) {
	        log.error("Execution Error!!!");
	        e.printStackTrace();
        }
    }
	/**
	 * @param cassandraHost the cassandraHost to set
	 */
	public static void setCassandraHost(String cassandraHost) {
		CassandraLogColumnFamilyExistanceTestHector.cassandraHost = cassandraHost;
	}
	public static void createSchema(){
        cluster.addKeyspace(newKeyspaceDef,true);
    }

}
