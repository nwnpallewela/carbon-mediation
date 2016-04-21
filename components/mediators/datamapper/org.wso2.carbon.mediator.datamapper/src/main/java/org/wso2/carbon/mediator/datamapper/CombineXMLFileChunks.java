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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

/**
 *
 */
public class CombineXMLFileChunks {
    private static final Log log = LogFactory.getLog(DataMapperMediator.class);
    private OMElement message;
    private String parentTagName = null;
    private OMElement root;

    public CombineXMLFileChunks(OMElement message, String parentTagName) {
        this.message = message;
        this.parentTagName = parentTagName;
        root = init();
    }

    private OMElement init() {
        OMElement element = message;
        while (!element.getLocalName().equals(parentTagName)) {
            element = element.getFirstElement();
        }

        return element;
    }

    public void add(OMElement outputMessage) {
        OMElement element = outputMessage;
        while (!element.getLocalName().equals(parentTagName)) {
            element = element.getFirstElement();
        }
        Iterator iterator = element.getChildElements();
        while (iterator.hasNext()) {
            root.addChild((OMElement) iterator.next());
        }
    }

    public OMElement getRoot() {
        return root;
    }
}
