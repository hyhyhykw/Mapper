package com.mapper.compiler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created time : 2022/3/28 16:52.
 *
 * @author 10585
 */
public class ConfigUtils {

    private static final String TAG_ISSUE = "issue";
    private static final String TAG_MAPPER = "mapper";
    private static final String TAG_NAME = "name";

    private static final String ATTR_ID = "id";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_APPCOMPAT = "appcompat";


    private static final String EXCLUDE = "exclude";
    private static final String LAYOUT_DIR = "layoutDir";
    private static final String CREATOR_CLASS = "creatorClass";
    private static final String MAPPER = "mapper";
    private static final String MODULE = "module";


    public static Config parseConfig(File configXml) {

        BufferedInputStream input = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            input = new BufferedInputStream(new FileInputStream(configXml));

            InputSource source = new InputSource(input);
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(source);
            NodeList issues = document.getElementsByTagName(TAG_ISSUE);

            Config config = new Config();
            int length = issues.getLength();
            for (int i = 0; i < length; i++) {
                Node node = issues.item(i);

                Element element = (Element) node;
                String id = element.getAttribute(ATTR_ID);

                NodeList childNodes = node.getChildNodes();

                switch (id) {
                    case CREATOR_CLASS: {
                        String creatorClass = element.getAttribute(ATTR_VALUE);
                        if (creatorClass == null || creatorClass.isEmpty()) {
                            throw new UnsupportedOperationException(
                                    "Must define creator class"
                            );
                        }
                        config.setCreatorClass(creatorClass);
                    }
                    break;
                    case MAPPER:
                        readMapper(childNodes, config);
                        break;
                    default:
                        readProperty(childNodes, config, id);
                        break;
                }
            }
            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    System.exit(-1);
                }
            }
        }

    }

    private static void readMapper(NodeList childNodes, Config config) {
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                String key = element.getAttribute(ATTR_KEY);
                String value = element.getAttribute(ATTR_VALUE);

                String appcompat = element.getAttribute(ATTR_APPCOMPAT);

                if (appcompat == null || appcompat.isEmpty()) {
                    config.mapper(key, value);
                } else {
                    config.mapper(appcompat, value);
                    config.replace(appcompat, value);
                }

                config.replace(key, value);
            }
        }
    }

    private static void readProperty(NodeList childNodes, Config config, String id) {
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                String value = element.getAttribute(ATTR_VALUE);
                switch (id) {
                    case MODULE:
                        config.module(value);
                        break;
                    case EXCLUDE:
                        config.exclude(value);
                        break;
                    case LAYOUT_DIR:
                        config.layoutDir(value);
                        break;
                }
            }
        }
    }

}