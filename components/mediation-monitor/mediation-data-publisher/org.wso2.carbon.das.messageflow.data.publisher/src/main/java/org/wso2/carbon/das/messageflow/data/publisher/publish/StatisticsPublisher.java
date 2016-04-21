/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
package org.wso2.carbon.das.messageflow.data.publisher.publish;

import net.minidev.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.wso2.carbon.das.data.publisher.util.DASDataPublisherConstants;
import org.wso2.carbon.das.data.publisher.util.PublisherUtil;
import org.wso2.carbon.das.messageflow.data.publisher.conf.EventPublisherConfig;
import org.wso2.carbon.das.messageflow.data.publisher.conf.PublisherConfig;
import org.wso2.carbon.das.messageflow.data.publisher.conf.Property;
import org.wso2.carbon.das.messageflow.data.publisher.util.MediationDataPublisherConstants;
import org.wso2.carbon.das.messageflow.data.publisher.util.PublisherUtils;
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class StatisticsPublisher {
	private static Log log = LogFactory.getLog(StatisticsPublisher.class);

	public static void process(PublishingFlow publishingFlow, PublisherConfig PublisherConfig) {
		List<String> metaDataKeyList = new ArrayList<String>();
		List<Object> metaDataValueList = new ArrayList<Object>();
		List<Object> eventData = new ArrayList<Object>();

		addMetaData(metaDataKeyList, metaDataValueList, PublisherConfig);

		try {
			if (PublisherConfig.isMessageFlowPublishingEnabled()) {
				addEventData(eventData, publishingFlow);
				StreamDefinition streamDef = getComponentStreamDefinition(metaDataKeyList.toArray());
				publishToAgent(eventData, metaDataValueList, PublisherConfig, streamDef);
			}
		} catch (MalformedStreamDefinitionException e) {
			log.error("Error while creating stream definition object", e);
		}

	}

	private static void addMetaData(List<String> metaDataKeyList, List<Object> metaDataValueList,
	                                PublisherConfig PublisherConfig) {
		//        metaDataValueList.add(PublisherUtil.getHostAddress());
		metaDataValueList.add(true); // payload-data is in compressed form

		Property[] properties = PublisherConfig.getProperties();
		if (properties != null) {
			for (Property property : properties) {
				if (property.getKey() != null && !property.getKey().isEmpty()) {
					metaDataKeyList.add(property.getKey());
					metaDataValueList.add(property.getValue());
				}
			}
		}
	}

	private static void addEventData(List<Object> eventData, PublishingFlow publishingFlow) {
		eventData.add(publishingFlow.getMessageFlowId());

		Map<String, Object> mapping = publishingFlow.getObjectAsMap();
		mapping.put("host", PublisherUtil.getHostAddress()); // Adding host

		String jsonString = JSONObject.toJSONString(mapping);

		eventData.add(compress(jsonString));
		//        eventData.add((jsonString));
	}

	private static void publishToAgent(List<Object> eventData, List<Object> metaDataValueList,
	                                   PublisherConfig PublisherConfig, StreamDefinition streamDef) {

		String serverUrl = PublisherConfig.getUrl();
		String userName = PublisherConfig.getUserName();
		String password = PublisherConfig.getPassword();

		String key = serverUrl + "_" + userName + "_" + password + "_" + streamDef.getName();
		EventPublisherConfig eventPublisherConfig = PublisherUtils.getEventPublisherConfig(key);
		if (!PublisherConfig.isLoadBalancingEnabled()) {
			DataPublisher dataPublisher = null;
			if (eventPublisherConfig == null) {
				synchronized (StatisticsPublisher.class) {
					eventPublisherConfig = new EventPublisherConfig();
					DataPublisher asyncDataPublisher = null;
					try {
						asyncDataPublisher = new DataPublisher(serverUrl, userName, password);
					} catch (DataEndpointAgentConfigurationException | DataEndpointException |
							DataEndpointConfigurationException | DataEndpointAuthenticationException |
							TransportException e) {
						log.error("Error occurred while sending the event", e);
					}
					eventPublisherConfig.setDataPublisher(asyncDataPublisher);
					PublisherUtils.getEventPublisherConfigMap().put(key, eventPublisherConfig);
				}
			}
			dataPublisher = eventPublisherConfig.getDataPublisher();
			dataPublisher.publish(DataBridgeCommonsUtils.generateStreamId(streamDef.getName(), streamDef.getVersion()),
			                      metaDataValueList.toArray(), null, eventData.toArray());
		} else {
			DataPublisher dataPublisher = null;
			if (eventPublisherConfig == null) {
				synchronized (StatisticsPublisher.class) {
					eventPublisherConfig = new EventPublisherConfig();

					DataPublisher loadBalancingDataPublisher = null;
					try {
						loadBalancingDataPublisher = new DataPublisher(serverUrl, userName, password);
					} catch (DataEndpointAgentConfigurationException | DataEndpointException |
							DataEndpointConfigurationException | DataEndpointAuthenticationException |
							TransportException e) {
						log.error("Error occurred while sending the event", e);
					}
					eventPublisherConfig.setLoadBalancingDataPublisher(loadBalancingDataPublisher);
					PublisherUtils.getEventPublisherConfigMap().put(key, eventPublisherConfig);
				}
			}
			dataPublisher = eventPublisherConfig.getLoadBalancingDataPublisher();

			dataPublisher.publish(DataBridgeCommonsUtils.generateStreamId(streamDef.getName(), streamDef.getVersion()),
			                      metaDataValueList.toArray(), null, eventData.toArray());
		}
	}

	public static StreamDefinition getComponentStreamDefinition(Object[] metaData)
			throws MalformedStreamDefinitionException {
		StreamDefinition eventStreamDefinition = new StreamDefinition(MediationDataPublisherConstants.STREAM_NAME,
		                                                              MediationDataPublisherConstants.STREAM_VERSION);
		eventStreamDefinition.setNickName("");
		eventStreamDefinition
				.setDescription("This stream is use by WSO2 ESB to publish component specific data for tracing");
		eventStreamDefinition.addMetaData(DASDataPublisherConstants.DAS_COMPRESSED, AttributeType.BOOL);
		//        eventStreamDefinition.addMetaData(DASDataPublisherConstants.DAS_HOST, AttributeType.STRING);
		for (Object aMetaData : metaData) {
			eventStreamDefinition.addMetaData(aMetaData.toString(), AttributeType.STRING);
		}

		eventStreamDefinition.addPayloadData(MediationDataPublisherConstants.MESSAGE_ID, AttributeType.STRING);
		eventStreamDefinition.addPayloadData(MediationDataPublisherConstants.FLOW_DATA, AttributeType.STRING);
		return eventStreamDefinition;
	}

	/**
	 * Compress the payload
	 *
	 * @param str
	 * @return
	 */
	private static String compress(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			GZIPOutputStream gzip = new GZIPOutputStream(out);
			gzip.write(str.getBytes());
			gzip.close();
			return DatatypeConverter.printBase64Binary(out.toByteArray());
		} catch (IOException e) {
			log.error("Unable to compress data", e);
		}

		return str;
	}
}
