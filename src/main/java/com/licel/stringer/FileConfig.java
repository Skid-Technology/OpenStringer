package com.licel.stringer;

import com.licel.stringer.config.PathFilter;
import com.licel.stringer.config.ProtectionConfigHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class FileConfig extends Config {
    private SAXParserFactory saxParserFactory = null;

    public FileConfig() {
        this.saxParserFactory = this.createSAXParserFactory();
    }

    public FileConfig(File file) throws IOException, ParserConfigurationException, SAXException {
        this.saxParserFactory = this.createSAXParserFactory();
        this.parseXmlFile(file);
    }

    private Document createXmlDoc() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = document.createElement("stringer-v3");
        root.appendChild(this.createTag(document, "signMode", this.signMode));
        root.appendChild(this.createTag(document, "verbose", Boolean.toString(this.verbose)));
        root.appendChild(this.createTag(document, "optimize", Boolean.toString(this.optimize)));

        try {
            if (this.proguardMapFile != null) {
                root.appendChild(this.createTag(document, "proguardMapFile", this.proguardMapFile.getCanonicalPath()));
            }
        } catch (Exception ignored) {
        }

        document.appendChild(root);
        if (this.isReleaseMode()) {
            if (this.keystore != null && !this.keystore.trim().isEmpty()) {
                root.appendChild(this.createTag(document, "keystore", this.keystore));
            }
            if (this.alias != null && !this.alias.trim().isEmpty()) {
                root.appendChild(this.createTag(document, "alias", this.alias));
            }
            if (this.storepass != null && !this.storepass.trim().isEmpty()) {
                root.appendChild(this.createTag(document, "storepass", this.storepass));
            }
            if (this.keystoreType != null && !this.keystoreType.trim().isEmpty()) {
                root.appendChild(this.createTag(document, "storetype", this.keystoreType));
            }
            if (this.keypass != null && !this.keypass.trim().isEmpty()) {
                root.appendChild(this.createTag(document, "keypass", this.keypass));
            }
        }

        try {
            if (this.tmpDir != null) {
                root.appendChild(this.createTag(document, "tmpDir", this.tmpDir.getCanonicalPath()));
            }
        } catch (Exception ignored) {
        }

        ArrayList<Config> elementConfigs = this.generateProtectionElementsConfigs();
        Element protectionElementsNode = document.createElement("protectionElements");
        for (Config elementConfig : elementConfigs) {
            protectionElementsNode.appendChild(this.createProtectionElementParams(document, elementConfig));
        }

        root.appendChild(protectionElementsNode);
        return document;
    }

    public void store(File file) {
        try {
            Document document = this.createXmlDoc();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        } catch (ParserConfigurationException | TransformerException ignored) {
        }
    }

    private Element createTag(Document document, String tagName, String value) {
        Element element = document.createElement(tagName);
        element.setTextContent(value);
        return element;
    }

    private Element createParams(Document document, String tagName, FilteredParams params) {
        Element element = document.createElement(tagName);
        Element modeElement = document.createElement("mode");
        modeElement.setTextContent(params.getMode());
        element.appendChild(modeElement);
        if (params.filtered && params.filters != null && !params.filters.isEmpty()) {
            Element filtersElement = document.createElement("filters");
            for (PathFilter filter : params.filters) {
                Element filterElement = document.createElement("filter");
                filterElement.appendChild(document.createCDATASection(filter.toString()));
                filtersElement.appendChild(filterElement);
            }
            element.appendChild(filtersElement);
        }
        return element;
    }

    private Element createProtectionElementParams(Document document, ProtectionElementParams params) {
        Element element = document.createElement("protectionElement");
        if (this.target != null) {
            element.appendChild(this.createTag(document, "target", this.target));
        }

        if (params.getPaths() != null) {
            Element pathsElement = document.createElement("paths");
            for (String path : params.getPaths()) {
                Element pathElement = document.createElement("path");
                pathElement.appendChild(document.createCDATASection(path));
                pathsElement.appendChild(pathElement);
            }
            element.appendChild(pathsElement);
        }

        if (params.getStringEncryption().isEnabled()) {
            Element stringEnc = this.createParams(document, "stringEncryption", params.getStringEncryption());
            stringEnc.appendChild(this.createTag(document, "checkCaller", Boolean.toString(params.getStringEncryption().isCheckCaller())));
            stringEnc.appendChild(this.createTag(document, "equalsIntrinsicEnabled", Boolean.toString(params.getStringEncryption().isEqualsIntrinsicEnabled())));
            element.appendChild(stringEnc);
        }

        if (params.getHideAccess().isEnabled()) {
            element.appendChild(this.createParams(document, "hideAccess", params.getHideAccess()));
        }

        if (params.getResourceEncryption().isEnabled()) {
            element.appendChild(this.createParams(document, "resourceEncryption", params.getResourceEncryption()));
        }

        if (params.getIntegrityControl().isEnabled()) {
            Element integrity = this.createParams(document, "integrityControl", params.getIntegrityControl());
            integrity.appendChild(this.createTag(document, "checkBytecode", Boolean.toString(params.getIntegrityControl().checkBytecode().isEnabled())));
            integrity.appendChild(this.createTag(document, "checkJar", Boolean.toString(params.getIntegrityControl().checkJar().isEnabled())));
            integrity.appendChild(this.createTag(document, "sealJar", Boolean.toString(params.getIntegrityControl().sealJar().isEnabled())));
            integrity.appendChild(this.createTag(document, "osgiCompatibility", Boolean.toString(params.getIntegrityControl().osgiCompatibility().isEnabled())));
            element.appendChild(integrity);
        }

        return element;
    }

    private SAXParserFactory createSAXParserFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        try {
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
        }
        return factory;
    }

    private void parseXmlFile(File file) throws IOException, ParserConfigurationException, SAXException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(reader);
            ProtectionConfigHandler handler = new ProtectionConfigHandler(this, this);
            SAXParser parser = this.saxParserFactory.newSAXParser();
            parser.parse(inputSource, handler);
        }
    }
}
