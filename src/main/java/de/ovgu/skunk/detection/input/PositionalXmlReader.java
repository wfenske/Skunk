package de.ovgu.skunk.detection.input;

//PositionalXMLReader.java taken from http://stackoverflow.com/questions/4915422/get-line-number-from-xml-node-java

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * The Class PositionalXmlReader.
 */
public class PositionalXmlReader {

    /**
     * The Constant LINE_NUMBER_KEY_NAME.
     */
    private final static String LINE_NUMBER_KEY_NAME = "lineNumber";
    private SAXParser parser;
    private DocumentBuilder docBuilder;
    private Document doc;

    public static int getElementLineNumberAsIs(Element element) {
        int xmlStartLoc = (Integer) element.getUserData(PositionalXmlReader.LINE_NUMBER_KEY_NAME);
        return xmlStartLoc;
    }

    private static class SkunkXmlHandler extends DefaultHandler {
        private final Stack<Element> elementStack = new Stack<Element>();
        private final StringBuilder textBuffer = new StringBuilder();
        private final Document doc;

        private Locator locator;

        public SkunkXmlHandler(Document doc) {
            this.doc = doc;
        }

        @Override
        public void setDocumentLocator(final Locator locator) {
            this.locator = locator; // Save the locator, so that it can be
            // used later for line tracking when
            // traversing nodes.
        }

        @Override
        public void startElement(final String uri, final String localName,
                                 final String qName, final Attributes attributes)
                throws SAXException {
            addTextIfNeeded();
            final Element el = doc.createElement(qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                final String attrQName = attributes.getQName(i);
                final String attrValue = attributes.getValue(i);
                el.setAttribute(attrQName, attrValue);
            }

            el.setUserData(LINE_NUMBER_KEY_NAME, this.locator.getLineNumber(), null);
            elementStack.push(el);
        }

        @Override
        public void endElement(final String uri, final String localName,
                               final String qName) {
            addTextIfNeeded();
            final Element closedEl = elementStack.pop();
            if (elementStack.isEmpty()) { // Is this the root element?
                doc.appendChild(closedEl);
            } else {
                final Element parentEl = elementStack.peek();
                parentEl.appendChild(closedEl);
            }
        }

        @Override
        public void characters(final char ch[], final int start,
                               final int length) throws SAXException {
            textBuffer.append(ch, start, length);
        }

        // Outputs text accumulated under the current node
        private void addTextIfNeeded() {
            if (textBuffer.length() > 0) {
                final Element el = elementStack.peek();
                final Node textNode = doc.createTextNode(textBuffer
                        .toString());
                el.appendChild(textNode);
                textBuffer.delete(0, textBuffer.length());
            }
        }
    }

    public PositionalXmlReader() {

    }

    /**
     * Read xml.
     *
     * @param is the input
     * @return the document
     * @throws IOException  Signals that an I/O exception has occurred.
     * @throws SAXException the SAX exception
     */
    public Document readXML(final InputStream is) throws IOException, SAXException {
        ensureInitialized();
        Document doc = docBuilder.newDocument();
        DefaultHandler handler = new SkunkXmlHandler(doc);
        try {
            parser.parse(is, handler);
        } finally {
            parser.reset();
        }
        return doc;
    }

    private boolean initialized = false;

    private synchronized void ensureInitialized() throws SAXException {
        if (initialized) return;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(
                    "Can't create SAX parser / DOM builder.", e);
        }
        initialized = true;
    }
}
