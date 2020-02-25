package edu.ncsu.las.storage;

import java.io.Closeable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import kafka.admin.AdminOperationException;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;

public class KafkaQueue {
	private static Logger logger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());

	private static Producer<String, String> _kafkaProducer;
	
	private static synchronized Producer<String, String> getKafkaProducer(String domain) {
		if (_kafkaProducer == null) {
			Properties props = Configuration.getConfigurationPropertyAsProperties(domain, ConfigurationType.KAFKA_PROPERTIES);			        
			_kafkaProducer = new KafkaProducer<String,String>(props);
		}
		return _kafkaProducer;
	}	
	
	public static FileStorageStatusCode sendToQueue(FileStorageAreaType area, String domain, String UUID, JSONObject data) {
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.SYSTEMCOMPONENT_KAFKA) == false) {
			return FileStorageStatusCode.SKIPPED;
		}		
		
		if (Configuration.sendJSONToKafka(domain,area) == false) {
			return FileStorageStatusCode.SKIPPED;
		}
		
		String topic = Configuration.getDomainAndArea(domain, area);
		
		KafkaQueue.getKafkaProducer(domain).send(new ProducerRecord<String, String>(topic, UUID, data.toString()),
				                           new Callback() {
			                                 public void onCompletion(RecordMetadata metadata, Exception e) {
			                                	 if(e != null) {
			                                		 logger.log(Level.SEVERE, "Unable to send json document to kafka queue: "+e);
			                                	 }
			                                	 //System.out.println("The offset of the record we just sent is: " + metadata.offset());
                                             }});
		logger.log(Level.INFO, "Sent json document to kafka queue(async)");
		return FileStorageStatusCode.SUCCESS;
	}
	
	
	/**
	 * For all of the domains currently managed by the system, ensure their domains exist.
	 * 
	 * @param domainList
	 */
	public static void checkAllQueues(java.util.Collection<String> domainList) {
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.SYSTEMCOMPONENT_KAFKA) == false) {
			logger.log(Level.INFO, "System configured to not use Kafka - not checking queues");
			return;
		}
		
		for (String domainName: domainList) {
			if (domainName.equals(Domain.DOMAIN_SYSTEM)) { continue; }

			int numPartitions     = Configuration.getConfigurationPropertyAsInt(domainName, ConfigurationType.KAFKA_NUM_PARTITIONS);
			int replicationFactor = Configuration.getConfigurationPropertyAsInt(domainName, ConfigurationType.KAFKA_REPLICATION_FACTOR);
			String zookeeperServers = Configuration.getConfigurationProperty(domainName, ConfigurationType.KAFKA_ZOOKEEPER);
			
			if (Configuration.getConfigurationPropertyAsBoolean(domainName, ConfigurationType.KAFKA_SEND_NORMAL)) {
				KafkaQueue.checkQueueExistence(zookeeperServers, FileStorageAreaType.REGULAR,domainName, numPartitions, replicationFactor);
			}
			if (Configuration.getConfigurationPropertyAsBoolean(domainName, ConfigurationType.KAFKA_SEND_SANDBOX)) {
				KafkaQueue.checkQueueExistence(zookeeperServers, FileStorageAreaType.SANDBOX,domainName, numPartitions, replicationFactor);
			}
			if (Configuration.getConfigurationPropertyAsBoolean(domainName, ConfigurationType.KAFKA_SEND_ARCHIVE)) {
				KafkaQueue.checkQueueExistence(zookeeperServers, FileStorageAreaType.ARCHIVE,domainName, numPartitions, replicationFactor);
			}
		}		
	}
	
	
	
	/**
	 * Checks if the given topic/quue exists already within kafka.  If not, creates it.
	 * 
	 * @param zkServers
	 * @param area
	 * @param domain
	 * @param numPartitions
	 * @param numReplications
	 */
	public static void checkQueueExistence(String zkServers, FileStorageAreaType area, String domain, int numPartitions, int numReplications) {

		String topic = Configuration.getDomainAndArea(domain, area);
		
        try (AutoZkClient zkClient = new AutoZkClient(zkServers)) {
            ZkUtils zkUtils = ZkUtils.apply(zkClient, false);

            if (AdminUtils.topicExists(zkUtils, topic) == false) {
                logger.log(Level.INFO, "Creating topic: "+ topic); 
                try {
                    Properties topicConfiguration = new Properties();

                    AdminUtils.createTopic(zkUtils, topic, numPartitions, numReplications, topicConfiguration,RackAwareMode.Disabled$.MODULE$);
                } catch (AdminOperationException aoe) {
                    logger.log(Level.SEVERE, "Error while creating partitions for topic \""+topic+"\"", aoe); 
                } 
            } else {
                logger.log(Level.FINEST,"Topic already exists: "+ topic); 
            } 
        } 
	}


    private static final class AutoZkClient extends ZkClient implements Closeable { 

        static int sessionTimeout = 30_000;
        static int connectionTimeout = 6_000;

        AutoZkClient(String zkServers) {
            super(zkServers, sessionTimeout, connectionTimeout, ZKStringSerializer$.MODULE$);
        }
    }	
	
	
}
