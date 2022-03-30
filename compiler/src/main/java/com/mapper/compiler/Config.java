package com.mapper.compiler;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created time : 2022/3/28 16:48.
 *
 * @author 10585
 */
public class Config {

    public String creatorClass;
    public final Set<String> exclude = new HashSet<>();
    public final Set<String> module = new HashSet<>(Collections.singletonList("app"));
    public final Set<String> layoutDir = new HashSet<>();

    public final Map<String, String> mapper = new HashMap<>();
    public final Map<String, String> replace = new HashMap<>();


    public void setCreatorClass(String creatorClass) {
        this.creatorClass = creatorClass;
    }

    public void exclude(String exclude) {
        if (exclude.contains(".")) {
            if (exclude.endsWith(".xml") && exclude.length() > 4) {
                this.exclude.add(exclude);
            }
            return;
        }
        this.exclude.add(exclude + ".xml");
    }

    public void module(String module) {
        this.module.add(module);
    }

    public void layoutDir(String layoutDir) {
        String replace = layoutDir.replace("/", File.separator);

        this.layoutDir.add(replace);
    }

    public void mapper(String key, String value) {
        this.mapper.put(key, value);
    }

    public void replace(String key, String value) {
        this.replace.put("<" + key, "<" + value);
        this.replace.put("</" + key + ">", "</" + value + ">");
    }
}