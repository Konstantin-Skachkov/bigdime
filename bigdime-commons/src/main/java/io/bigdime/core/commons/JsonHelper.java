/**
 * Copyright (C) 2015 Stubhub.
 */
package io.bigdime.core.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import java.util.HashMap;
import java.util.List;


/**
 * Helper class to parse the {@code JsonNode} to get needed properties.
 *
 * @author Neeraj Jain
 */
@Component
public final class JsonHelper {
  public JsonNode readStream(InputStream is) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(is);
  }

  /**
   * If the node is present has the value as true,
   *
   * @param node
   * @param key
   * @return true if the node is present has the value as true, false
   * otherwise.
   */
  public boolean getBooleanProperty(final JsonNode node, final String key) {
    try {
      String stringValue = getRequiredStringProperty(node, key);
      return Boolean.valueOf(stringValue);
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  public String getRequiredStringProperty(final JsonNode node, final String key) {
    final JsonNode childNode = getRequiredNode(node, key);
    if (childNode.isTextual()) {
      return childNode.asText();
    }
    throw new IllegalArgumentException("no text node found with key=" + key);
  }

  /**
   * Return's string object if the required key presents in Json node.
   * if it is present, it will check the type of the object and return's corresponding object.
   * if not present, it throws IllegalArgumentException
   *
   * @param node
   * @param key
   * @return
   */
  public Object getRequiredProperty(final JsonNode node, final String key) {
    final JsonNode childNode = getRequiredNode(node, key);
    if (childNode == null)
      throw new IllegalArgumentException("no text node found with key=" + key);

    if (childNode.isBoolean()) {
      return childNode.asBoolean();
    }
    if (childNode.isNumber()) {
      return childNode.asLong();
    }
    if (childNode.isLong()) {
      return childNode.asLong();
    }
    if (childNode.isDouble()) {
      return childNode.asDouble();
    }
    if (childNode.isTextual()) {
      return childNode.asText();
    }
    // all others, send as a text
    return childNode.asText();
  }

  /**
   * Returns the Object node until it find's the node for corresponding fieldname.
   *
   * @param node
   * @param fieldName
   * @return
   */
  public ObjectNode find(JsonNode node, String fieldName) {
    Iterator<Entry<String, JsonNode>> iter = node.fields();
    ObjectNode on = null;
    while (iter.hasNext()) {
      Entry<String, JsonNode> entry = iter.next();
      if (entry.getKey().equals(fieldName)) {
        on = new ObjectMapper().createObjectNode();
        on.put(entry.getKey(), entry.getValue());
        break;
      } else if (entry.getValue().isArray()) {
        for (JsonNode jnode : entry.getValue()) {
          on = find(jnode, fieldName);
        }
      } else if (entry.getValue().isObject()) {
        JsonNode jnode = entry.getValue();
        on = find(jnode, fieldName);
      }
    }
    return on;
  }

  public JsonNode getRequiredNode(final JsonNode node, final String key) {
    final JsonNode childNode = getOptionalNodeOrNull(node, key);
    if (childNode == null) {
      throw new IllegalArgumentException("no node found with key=" + key);
    }
    return childNode;
  }

  public JsonNode getOptionalNodeOrNull(final JsonNode node, final String key) {
    return node.get(key);
  }

  public JsonNode getRequiredArrayNode(final JsonNode node, final String key) {
    final JsonNode childNode = getRequiredNode(node, key);
    if (childNode.isArray()) {
      return childNode;
    }
    throw new IllegalArgumentException("no array node found with key=" + key);
  }

  /*
   * 	@formatter:off
   * {
   *   "name" : "valueText",
   *   "arrayKey" : [
   *     "name" : "valueArray",
   *     "path" : "/tmp"
   *   ],
   *   "nodeKey" : {
   *     "name" : "valueNode",
   *     "id" : "p1"
   *   },
   *   "simpleKey" : "simpleValue"
   * }
   * 	@formatter:on
   *
   *
   */
  public Map<String, Object> getNodeTree(final JsonNode node) {
    Map<String, Object> propertyMap = new HashMap<>();
    final Iterator<Entry<String, JsonNode>> iter = node.fields();

    while ((iter != null) && iter.hasNext()) {
      final Entry<String, JsonNode> entry = iter.next();

      if (entry.getValue().isTextual()) {
        propertyMap.put(entry.getKey(), entry.getValue().asText());
      } else if (entry.getValue().isArray()) {
        Map<String, Object> innerMap = null;
        for (JsonNode arrayValue : entry.getValue()) {
          if (arrayValue.isTextual()) {
            @SuppressWarnings("unchecked")
            List<String> values = (List<String>) propertyMap.get(entry.getKey());
            if (values == null) {
              values = new ArrayList<>();
              propertyMap.put(entry.getKey(), values);
            }
            values.add(arrayValue.asText());
          } else {
            innerMap = getNodeTree(arrayValue);
            propertyMap.put(entry.getKey(), innerMap);
          }
        }

      } else {
        Map<String, Object> innerMap = getNodeTree(entry.getValue());
        propertyMap.put(entry.getKey(), innerMap);
      }
      // TODO: Setting the JsonNode as it is in the map, need to
      // come back to this and populate text values by drilling
      // down to all levels.
    }
    return propertyMap;
    // propertyMap.put(node., value)
  }

}
