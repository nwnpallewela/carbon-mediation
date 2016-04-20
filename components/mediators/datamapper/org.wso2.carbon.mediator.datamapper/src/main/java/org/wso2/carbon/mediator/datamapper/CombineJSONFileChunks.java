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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class CombineJSONFileChunks {
    private static final Log log = LogFactory.getLog(DataMapperMediator.class);
    private String message;
    private String parentTagName = null;
    private String root;

    public CombineJSONFileChunks(String message, String parentTagName) {
        this.message = message;
        this.parentTagName = parentTagName;
        root = message.substring(0, message.length() - 2) + ",";
    }

    public void add(String outputMessage) {
        String replace = "{\"" + parentTagName + "\":[";
        outputMessage = outputMessage.replace(replace, "");
        root += outputMessage.substring(0, outputMessage.length() - 2) + ",";
    }

    public String getRoot() {
        root = root.substring(0, root.length() - 1) + "]}";
        return root;
    }
}
