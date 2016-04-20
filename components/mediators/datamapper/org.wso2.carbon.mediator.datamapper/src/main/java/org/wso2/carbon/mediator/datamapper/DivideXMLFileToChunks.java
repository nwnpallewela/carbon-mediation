/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.mediator.datamapper;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.AXIOMUtils;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 *
 */
public class DivideXMLFileToChunks implements Iterator {
    private static final Log log = LogFactory.getLog(DataMapperMediator.class);
    private MessageContext messageContext;
    private String parentTagNameToDivideBy = null;
    private int numberInOneChunk = 0;
    private QName parentTag;
    private OMElement theRoot;
    private OMElement content;
    private SOAPEnvelope envelope;
    private QName envName;
    private SOAPEnvelope theEnvelope;

    public DivideXMLFileToChunks(MessageContext messageContext, String parentTagNameToDivideBy, String namespaceURI,
            String prefix, int numberInOneChunk) {
        this.messageContext = messageContext;
        this.parentTagNameToDivideBy = parentTagNameToDivideBy;
        this.numberInOneChunk = numberInOneChunk;
        if (namespaceURI == null || namespaceURI.equals("")) {
            parentTag = new QName(XMLConstants.NULL_NS_URI, parentTagNameToDivideBy, XMLConstants.DEFAULT_NS_PREFIX);
        } else {
            parentTag = new QName(namespaceURI, parentTagNameToDivideBy, prefix);
        }
        envelope = messageContext.getEnvelope();
        content = envelope.getBody();
        envName = envelope.getFirstElement().getQName();
        theRoot = getTheRoot();
    }

    @Override
    public boolean hasNext() {
        return theRoot.getFirstElement() != null;
    }

    @Override
    public MessageContext next() {
        OMElement returnVal = OMAbstractFactory.getOMFactory().createOMElement(parentTag);
        OMElement child;
        for (int i = 1; i <= numberInOneChunk; i++) {
            child = theRoot.getFirstElement();
            if (child != null) {
                returnVal.addChild(child);
            }
        }
        try {
            if (envName.getNamespaceURI().equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                theEnvelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
            } else {
                theEnvelope = OMAbstractFactory.getSOAP12Factory().getDefaultEnvelope();
            }
            theEnvelope.getBody().addChild(returnVal);
            messageContext.setEnvelope(AXIOMUtils.getSOAPEnvFromOM(theEnvelope));
        } catch (AxisFault axisFault) {
            log.error(axisFault);
        }

        return messageContext;
    }

    private OMElement getTheRoot() {
        OMElement element = content;
        while (!element.getLocalName().equals(parentTagNameToDivideBy)) {
            element = element.getFirstElement();
            if (element == null) {
                throw new SynapseException("The input value for  parent tag name is incorrect");
            }
        }
        return element;
    }

    public void remove() {

    }
}
