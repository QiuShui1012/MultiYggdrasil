package com.qiushui1012.mod.multiyggdrasil.config;

import com.qiushui1012.mod.multiyggdrasil.util.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class IniParser {
    public static Map<String, Map<String, String>> parseRaw(Path configPath) throws IOException {
        try (Scanner scanner = new Scanner(configPath, StandardCharsets.UTF_8)) {
            Map<String, Map<String, String>> raw = new HashMap<>();
            Pair<String, Map<String, String>> data = new Pair<>();
            while (scanner.hasNext()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.contains("[") && line.contains("]")) {
                    String name = line.substring(line.indexOf('[') + 1, line.lastIndexOf(']'));
                    if (data.getK() != null) {
                        if (data.getV() == null || data.getV().isEmpty()) {
                            throw new IllegalArgumentException("Unexpected empty section '" + data.getK() + "'");
                        }
                        raw.put(data.getK(), data.getV());
                        data = new Pair<>();
                    }
                    if (raw.containsKey(name)) throw new IllegalArgumentException("Unexpected duplicate name '" + name + "'");
                    data.setK(name);
                    continue;
                }
                if (data.getK() == null) continue;

                if (line.contains("=")) {
                    String[] value = line.split("=", 2);
                    String key = value[0].trim();
                    String val = value[1].trim();
                    int index = findCommentIndex(val);

                    if (index != -1) {
                        val = val.substring(0, index).trim();
                    }

                    data.getOrCompute(LinkedHashMap::new).put(key, val);
                }
            }
            if (data.getK() != null) {
                if (data.getV() == null || data.getV().isEmpty()) {
                    throw new IllegalArgumentException("Unexpected empty section '" + data.getK() + "'");
                }
                raw.put(data.getK(), data.getV());
            }
            return raw;
        }
    }

    private static int findCommentIndex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '#' || c == ';') && (i == 0 || s.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }
}