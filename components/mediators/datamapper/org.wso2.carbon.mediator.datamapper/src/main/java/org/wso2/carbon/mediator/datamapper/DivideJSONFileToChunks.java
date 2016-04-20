package org.wso2.carbon.mediator.datamapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DivideJSONFileToChunks {
    private final Log log;
    private InputStream inputStream;
    private JsonFactory factory;
    private JsonParser jp;
    private ObjectMapper mapper;
    private String parentTagNameToDivideBy;
    private int numberInOneChunk;
    private JsonToken token;
    private JsonNode node;

    public DivideJSONFileToChunks(InputStream json, String parentTagNameToDivideBy, int numberInOneChunk)
            throws IOException {
        this.numberInOneChunk = numberInOneChunk;
        this.inputStream = json;
        this.parentTagNameToDivideBy = parentTagNameToDivideBy;
        factory = new JsonFactory();
        mapper = new ObjectMapper();
        log = LogFactory.getLog(DataMapperMediator.class);
        init();
    }

    public void init() throws IOException {
        jp = factory.createParser(inputStream);

        while (!jp.isClosed()) {
            token = jp.nextToken();
            if (JsonToken.FIELD_NAME.equals(token)) {
                parentTagNameToDivideBy.equals(jp.getCurrentName());
                break;
            }

        }
        jp.nextToken();

    }

    public boolean hasNext() throws IOException {
        token = jp.nextToken();
        return token == JsonToken.START_OBJECT;
    }

    public InputStream next() throws IOException {
        String result = "";

        result = "{\"" + parentTagNameToDivideBy + "\":[";

        node = mapper.readValue(jp, JsonNode.class);
        result += node.toString();
        for (int i = 1; i < numberInOneChunk; i++) {
            if (hasNext()) {
                node = mapper.readValue(jp, JsonNode.class);
                result += "," + node.toString();
            }
        }
        result += "]}";
        return new ByteArrayInputStream(result.getBytes());
    }
}
