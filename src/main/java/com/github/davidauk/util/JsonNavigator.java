package com.github.davidauk.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JsonNavigator {

    private JsonNavigator() {
    }

    public static List<JsonNode> findAll(JsonNode root, String searchKey) {
        List<JsonNode> results = new ArrayList<>();
        ArrayDeque<JsonNode> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            JsonNode current = queue.removeFirst();

            if (current.isObject()) {
                Iterator<String> fieldNames = current.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode value = current.get(fieldName);

                    if (fieldName.equals(searchKey)) {
                        results.add(value);
                    } else {
                        queue.addLast(value);
                    }
                }
            } else if (current.isArray()) {
                for (JsonNode child : current) {
                    queue.addLast(child);
                }
            }
        }

        return results;
    }

    public static JsonNode findFirst(JsonNode root, String searchKey) {
        List<JsonNode> results = findAll(root, searchKey);
        return results.isEmpty() ? null : results.getFirst();
    }
}
