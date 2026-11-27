package com.licel.stringer.webapp;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.Opcodes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WebXmlCompiler implements Opcodes {
    public File webXmlFile;
    public File webRoot;
    public Document document;
    public WebApplicationDescriptor webApplication;
    public String contextLoaderListenerClass;

    public WebXmlCompiler(File webRoot)
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        this.webRoot = webRoot;
        this.webXmlFile = new File(webRoot, "web.xml");
        this.parseWebXml();
    }

    private void parseWebXml()
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        this.webApplication = new WebApplicationDescriptor(this);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new WebXmlEntityResolver(this));
        this.document = builder.parse(this.webXmlFile);

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        XPathExpression contextParamExpr = xPath.compile("/web-app/context-param");
        XPathExpression paramNameExpr = xPath.compile("param-name");
        XPathExpression paramValueExpr = xPath.compile("param-value");
        NodeList nodes = (NodeList) contextParamExpr.evaluate(this.document, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {
            Node paramNode = nodes.item(i);
            String paramName = (String) paramNameExpr.evaluate(paramNode, XPathConstants.STRING);
            String paramValue = (String) paramValueExpr.evaluate(paramNode, XPathConstants.STRING);
            if ("contextConfigLocation".equals(paramName)) {
                StringBuilder rewrittenLocations = new StringBuilder();
                String[] locations = paramValue.split(";");

                for (String location : locations) {
                    location = location.trim();
                    if (location.startsWith("classpath:")) {
                        String classRelativePath = "classes/" + location.substring(10);
                        if (new File(this.webRoot, classRelativePath.replace("/", File.separator)).exists()) {
                            rewrittenLocations.append("/WEB-INF/classes/").append(location.substring(10)).append(";");
                            continue;
                        }
                    }

                    rewrittenLocations.append(location).append(";");
                }

                paramValue = rewrittenLocations.toString();
                if (paramValue.endsWith(";")) {
                    paramValue = paramValue.substring(0, paramValue.length() - 1);
                }
            }

            this.webApplication.contextParameters.add(new WebInitParameter(this, paramName, paramValue));
        }

        XPathExpression listenerClassExpr = xPath.compile("/web-app/listener/listener-class");
        nodes = (NodeList) listenerClassExpr.evaluate(this.document, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {
            Node listenerNode = nodes.item(i);
            this.webApplication.listeners.add(new WebListenerDescriptor(this, listenerNode.getTextContent()));
        }

        XPathExpression filterExpr = xPath.compile("/web-app/filter");
        XPathExpression filterNameExpr = xPath.compile("filter-name");
        XPathExpression filterClassExpr = xPath.compile("filter-class");
        XPathExpression asyncSupportedExpr = xPath.compile("async-supported");
        XPathExpression initParamExpr = xPath.compile("init-param");
        XPathExpression urlPatternExpr = xPath.compile("url-pattern");
        XPathExpression servletRefExpr = xPath.compile("servlet");
        nodes = (NodeList) filterExpr.evaluate(this.document, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {
            Node filterNode = nodes.item(i);
            String filterName = (String) filterNameExpr.evaluate(filterNode, XPathConstants.STRING);
            String filterClass = (String) filterClassExpr.evaluate(filterNode, XPathConstants.STRING);
            WebFilterDescriptor filterDescriptor = new WebFilterDescriptor(this, filterName, filterClass);

            String asyncSupported = (String) asyncSupportedExpr.evaluate(filterNode, XPathConstants.STRING);
            if (asyncSupported != null) {
                filterDescriptor.asyncSupported = asyncSupported;
            }

            NodeList initParamNodes = (NodeList) initParamExpr.evaluate(filterNode, XPathConstants.NODESET);
            for (int j = 0; j < initParamNodes.getLength(); j++) {
                Node initParamNode = initParamNodes.item(j);
                String initParamName = (String) paramNameExpr.evaluate(initParamNode, XPathConstants.STRING);
                String initParamValue = (String) paramValueExpr.evaluate(initParamNode, XPathConstants.STRING);
                filterDescriptor.initParameters.add(new WebInitParameter(this, initParamName, initParamValue));
            }

            XPathExpression filterMappingExpr = xPath.compile("/web-app/filter-mapping[filter-name/text() = '" + filterName + "']");
            NodeList filterMappingNodes = (NodeList) filterMappingExpr.evaluate(this.document, XPathConstants.NODESET);
            for (int j = 0; j < filterMappingNodes.getLength(); j++) {
                Node mappingNode = filterMappingNodes.item(j);
                NodeList urlPatternNodes = (NodeList) urlPatternExpr.evaluate(mappingNode, XPathConstants.NODESET);
                for (int k = 0; k < urlPatternNodes.getLength(); k++) {
                    filterDescriptor.urlPatterns.add(urlPatternNodes.item(k).getTextContent());
                }

                String mappedServlet = (String) servletRefExpr.evaluate(mappingNode, XPathConstants.STRING);
                if (mappedServlet != null) {
                    filterDescriptor.servletName = mappedServlet;
                }
            }

            this.webApplication.filters.add(filterDescriptor);
        }

        XPathExpression servletExpr = xPath.compile("/web-app/servlet");
        XPathExpression servletNameExpr = xPath.compile("servlet-name");
        XPathExpression servletClassExpr = xPath.compile("servlet-class");
        XPathExpression loadOnStartupExpr = xPath.compile("load-on-startup");
        nodes = (NodeList) servletExpr.evaluate(this.document, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {
            Node servletNode = nodes.item(i);
            String servletName = (String) servletNameExpr.evaluate(servletNode, XPathConstants.STRING);
            String servletClass = (String) servletClassExpr.evaluate(servletNode, XPathConstants.STRING);
            WebServletDescriptor servletDescriptor = new WebServletDescriptor(this, servletName, servletClass);

            String loadOnStartup = (String) loadOnStartupExpr.evaluate(servletNode, XPathConstants.STRING);
            if (loadOnStartup != null) {
                servletDescriptor.loadOnStartup = loadOnStartup;
            }

            String asyncSupported = (String) asyncSupportedExpr.evaluate(servletNode, XPathConstants.STRING);
            if (asyncSupported != null) {
                servletDescriptor.asyncSupported = asyncSupported;
            }

            NodeList initParamNodes = (NodeList) initParamExpr.evaluate(servletNode, XPathConstants.NODESET);
            for (int j = 0; j < initParamNodes.getLength(); j++) {
                Node initParamNode = initParamNodes.item(j);
                String initParamName = (String) paramNameExpr.evaluate(initParamNode, XPathConstants.STRING);
                String initParamValue = (String) paramValueExpr.evaluate(initParamNode, XPathConstants.STRING);
                servletDescriptor.initParameters.add(new WebInitParameter(this, initParamName, initParamValue));
            }

            XPathExpression servletMappingExpr = xPath.compile("/web-app/servlet-mapping[servlet-name/text() = '" + servletName + "']");
            NodeList servletMappingNodes = (NodeList) servletMappingExpr.evaluate(this.document, XPathConstants.NODESET);
            for (int j = 0; j < servletMappingNodes.getLength(); j++) {
                Node mappingNode = servletMappingNodes.item(j);
                NodeList urlPatternNodes = (NodeList) urlPatternExpr.evaluate(mappingNode, XPathConstants.NODESET);
                for (int k = 0; k < urlPatternNodes.getLength(); k++) {
                    servletDescriptor.urlPatterns.add(urlPatternNodes.item(k).getTextContent());
                }
            }

            this.webApplication.servlets.add(servletDescriptor);
        }
    }

    public void compile() throws TransformerException, IOException {
        Node rootNode = this.document.getChildNodes().item(0);
        ArrayList<Node> nodesToRemove = new ArrayList<>();
        NodeList children = rootNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("context-param")
                    || child.getNodeName().equals("listener")
                    || child.getNodeName().equals("servlet")
                    || child.getNodeName().equals("servlet-mapping")
                    || child.getNodeName().equals("filter")
                    || child.getNodeName().equals("filter-mapping")
                    || (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().trim().isEmpty())) {
                nodesToRemove.add(child);
            }
        }

        for (Node node : nodesToRemove) {
            rootNode.removeChild(node);
        }

        Element listenerElement = this.document.createElement("listener");
        Element listenerClassElement = this.document.createElement("listener-class");
        listenerClassElement.setTextContent(this.contextLoaderListenerClass);
        listenerElement.appendChild(listenerClassElement);
        if (!rootNode.hasChildNodes()) {
            rootNode.appendChild(listenerElement);
        } else {
            children = rootNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (!child.getNodeName().equals("description")
                        && !child.getNodeName().equals("display-name")
                        && !child.getNodeName().equals("icon")
                        && !child.getNodeName().equals("distributable")) {
                    rootNode.insertBefore(listenerElement, child);
                    break;
                }
            }
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("indent", "false");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(this.document);
        transformer.transform(source, result);
        String xmlContent = result.getWriter().toString();
        FileUtils.writeStringToFile(this.webXmlFile, xmlContent, StandardCharsets.UTF_8);
    }

    public void setContextLoaderListenerClass(String contextLoaderListenerClass) {
        this.contextLoaderListenerClass = contextLoaderListenerClass;
    }

    public WebApplicationDescriptor getWebApplication() {
        return this.webApplication;
    }

    public Document getDocument() {
        return this.document;
    }

    public File getWebXmlFile() {
        return this.webXmlFile;
    }

    public File getWebRoot() {
        return this.webRoot;
    }

    public String getContextLoaderListenerClass() {
        return this.contextLoaderListenerClass;
    }
}
