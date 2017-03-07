package de.ovgu.skunk.detection.input;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.util.*;

/**
 * Extracts a normalized function signature from a SrcML node representing a C function definition
 * <p>
 * Created by wfenske on 07.03.17.
 */
public class FunctionSignatureParser {
    private static Logger LOG = Logger.getLogger(FunctionSignatureParser.class);

    private static final ThreadLocal<XPath> tlXPath = ThreadLocal.withInitial(() -> XPathFactory.newInstance().newXPath());

    private final String functionNodeTextContent;
    private final Node functionNode;
    private final String filePath;
    StringBuilder result;
    boolean debugParseExceptions = false;

    public FunctionSignatureParser(Node functionNode, String filePath) {
        this.functionNode = functionNode;
        this.functionNodeTextContent = functionNode.getTextContent();
        this.filePath = filePath;
    }

    /**
     * Turns on log messages and more elaborate error reporting, but will also slow down the parser.
     */
    public void enableDebugParseExceptions() {
        this.debugParseExceptions = true;
    }

    /**
     * Parses K&R-style function definitions, such as
     * <p>
     * static int
     * newerf (f1, f2)
     * char *f1, *f2;
     * { ... }
     * <p>
     * which will be parsed into SrcML like this:
     * <p>
     * <pre>
     * <function>
     *      <type>
     *          <specifier>static</specifier>
     *          <name>int</name>
     *      </type>
     *      <name>newerf</name>
     *      <parameter_list>(
     *          <param><decl><type><name>f1</name></type></decl></param>,
     *          <param><decl><type><name>f2</name></type></decl></param>
     *      )</parameter_list>
     *      <decl_stmt>
     *          <decl><type><name>char</name> *</type><name>f1</name></decl>,
     *          <decl><type ref="prev"/>*<name>f2</name></decl>;
     *      </decl_stmt>
     * </function>
     * </pre>
     *
     * @return Normalized function signature representation
     */
//        private String parseKandRFunctionSignature() throws FunctionSignatureParseException {
//            result = new StringBuilder();
//
//            parseUpToIncludingFunctionName();
//
//            // Handle parameter list
//            result.append('(');
//            parseKandRFunctionParamList();
//            result.append(')');
//            // End parameter list
//
//            return postProcessSignature(result.toString());
//        }

    /**
     * <pre>
     * <function>
     *     <type>
     *         <specifier>static</specifier>
     *         <specifier>const</specifier>
     *         <name>char</name> *
     *     </type>
     *     <name>add_setenvif</name>
     *     <parameter_list>(
     *          <param>
     *              <decl>
     *                  <type><name>cmd_parms</name> *</type>
     *                  <name>cmd</name>
     *              </decl>
     *          </param>,
     *          <param>
     *              <decl>
     *                  <type>
     *                      <name>void</name> *
     *                  </type>
     *                  <name>mconfig</name>
     *              </decl>
     *          </param>,
     *          <param>
     *              <decl>
     *                  <type>
     *                      <specifier>const</specifier>
     *                      <name>char</name> *
     *                  </type>
     *                  <name>args</name>
     *              </decl>
     *          </param>)
     *     </parameter_list>
     * <block>{ ... }</block>
     * </function>
     * </pre>
     *
     * @return
     * @throws FunctionSignatureParseException
     */
    private String parseRegularFunctionSignature() throws FunctionSignatureParseException {
        result = new StringBuilder();
        parseUpToIncludingFunctionName();

        // Handle parameter list
        result.append('(');
        parseRegularFunctionParamList();
        result.append(')');
        // End parameter list

        return postProcessSignature(result.toString());
    }

    private void parseUpToIncludingFunctionName() throws FunctionSignatureParseException {
        Node returnType = getNodeOrDie(functionNode, "./type");
        List<Node> returnTypeSpecifiers = getPossiblyEmptyNodeList(returnType, "./type/specifier");
        for (Node returnTypeSpecifier : returnTypeSpecifiers) {
            String content = returnTypeSpecifier.getTextContent();
            result.append(' ').append(content);
        }
        Node returnTypeName = getNodeOrDie(returnType, "./name");
        String returnTypeNameString = returnTypeName.getTextContent();
        result.append(' ').append(returnTypeNameString);
        Node functionName = getNodeOrDie(functionNode, "./name");
        String functionNameString = functionName.getTextContent();
        result.append(' ').append(functionNameString);
    }

//        private void parseKandRFunctionParamList() throws FunctionSignatureParseException {
//            Node parameterList = getNodeOrDie(functionNode, "./parameter_list");
//            List<Node> params = getPossiblyEmptyNodeList(parameterList, "./param");
//            boolean firstParam = true;
//            for (Node param : params) {
//                if (firstParam) {
//                    firstParam = false;
//                } else {
//                    result.append(',').append(' ');
//                }
//
//                Node paramName = getNodeOrDie(param, "./decl/type/name");
//                String paramNameString = paramName.getTextContent();
//                result.append(paramNameString);
//            }
//        }

    private void parseRegularFunctionParamList() throws FunctionSignatureParseException {
        Node parameterList = getNodeOrDie(functionNode, "./parameter_list");
        List<Node> params = getPossiblyEmptyNodeList(parameterList, "./param");
        Iterator<Node> iParam = params.iterator();
        if (iParam.hasNext()) {
            // parse first parameter
            parseParam(iParam.next());
            // parse rest of parameters, if any
            while (iParam.hasNext()) {
                result.append(',').append(' ');
                parseParam(iParam.next());
            }
        }
    }

    private void parseParam(Node param) throws FunctionSignatureParseException {
        Node typeDeclarationNode = getNodeOrDie(param, "./decl/type");
        Optional<Node> optParamTypeName = getOptionalNode(typeDeclarationNode, "./name");
        if (optParamTypeName.isPresent()) {
            Node paramTypeName = optParamTypeName.get();
            String paramTypeNameString = paramTypeName.getTextContent();
            // NOTE, 2017-03-06, wf: By making the parameter name optional, we effectively also allow K&R style function definitions
            Optional<Node> paramName = getOptionalNode(param, "./decl/name");
            if (paramName.isPresent()) {
                String paramNameString = paramName.get().getTextContent();
                result.append(paramTypeNameString).append(' ').append(paramNameString);
            } else {
                result.append(paramTypeNameString);
            }
        } else {
            // May happen for function signature that include ellipses, e.g., printf(char *format, ...)
            String typeDeclarationText = typeDeclarationNode.getTextContent();
            result.append(typeDeclarationText);
        }
    }

    public String parseFunctionSignatureQuickAndDirty() {
        // get the text content of the node (signature + method content),
        // and remove method content until beginning of block
        //deleteComments();
        final String noBodyResult;
        final int openBraceIx = functionNodeTextContent.indexOf('{');
        if (openBraceIx == -1) {
            /*
             * This warning will also be triggered by K&R-style function definitions, such as
             *
             * static int
             * newerf (f1, f2)
             * char *f1, *f2;
             * { ... }
             *
             * which will be parsed into SrcML like this:
             *
             * <function>
             *     <type>
             *         <specifier>static</specifier>
             *         <name>int</name>
             *     </type>
             *     <name>newerf</name>
             *     <parameter_list>(
             *          <param><decl><type><name>f1</name></type></decl></param>,
             *          <param><decl><type><name>f2</name></type></decl></param>
             *     )</parameter_list>
             *     <decl_stmt>
             *          <decl><type><name>char</name> *</type><name>f1</name></decl>,
             *          <decl><type ref="prev"/>*<name>f2</name></decl>;
             *     </decl_stmt>
             * </function>
             */
            LOG.warn("Encountered strange function node (no opening `{' found) at " +
                    funcLocForReporting() +
                    ": " + functionNodeTextContent);
            noBodyResult = functionNodeTextContent;
        } else {
            noBodyResult = functionNodeTextContent.substring(0, openBraceIx);
        }
        // Delete block comments (yeah, there are some cases where these are part of the function signature ...)
        //String noBlockComments = noBodyResult.replaceAll("/\\*([^*]|\\*(?!/))*\\*/", "");
        String noBlockComments = removeBlockComments(noBodyResult);

        return postProcessSignature(noBlockComments);
    }

    private static String removeBlockComments(String input) {
        String result = input;
        while (true) {
            final int ixBlockCommentStart = result.indexOf("/*");
            if (ixBlockCommentStart == -1) {
                // No more beginning block comments. We can stop now.
                break;
            }

            final int len = result.length();
            int firstPossibleEndPosition = ixBlockCommentStart + 2;
            if (firstPossibleEndPosition >= len) {
                // no chance of the block comment being closed
                LOG.warn("Possibly malformed block comment in function " + input);
                break;
            }

            final int ixBlockCommentEnd = result.indexOf("*/", firstPossibleEndPosition);
            if (ixBlockCommentEnd == -1) {
                LOG.warn("Possibly malformed block comment in function " + input);
                break;
            }

            result = removeSubstring(result, ixBlockCommentStart, ixBlockCommentEnd + 2);
        }
        return result;
    }

    private static String removeSubstring(String s, int startIndex, int endIndex) {
        String partBefore = s.substring(0, startIndex);
        String partAfter = s.substring(endIndex);
        return partBefore + partAfter;
    }

    public String parseFunctionSignature() {
        // get the text content of the node (signature + method content),
        // and remove method content until beginning of block

        final int openBraceIx = functionNodeTextContent.indexOf('{');
        if (openBraceIx == -1) {
            /*
             * This may happen for K&R-style function definitions, such as
             *
             * static int
             * newerf (f1, f2)
             * char *f1, *f2;
             * { ... }
             */
            try {
                String signature = parseRegularFunctionSignature();
                if (debugParseExceptions) {
                    LOG.debug("Successfully parsed function signature using XPath: `" + signature + "' parsed from " + prettyPrintFunctionNodeOrChild(functionNode));
                }
                return signature;
            } catch (FunctionSignatureParseException parseEx) {
                if (debugParseExceptions) {
                    LOG.debug("Could not parse function signature (going to fallback): " + prettyPrintFunctionNodeOrChild(functionNode), parseEx);
                }
            }
        }

        return parseFunctionSignatureQuickAndDirty();
    }

    private void deleteFunctionBody(Node node) {
        try {
            NodeList blocks = (NodeList) ensureXPath().evaluate(".//block", node, XPathConstants.NODESET);
            if (blocks != null) {
                deleteNodes(blocks);
            }
        } catch (XPathExpressionException e) {
            // Don't care
        }
    }

    private void deleteNodes(NodeList nodeList) {
        final int len = nodeList.getLength();
        for (int i = 0; i < len; i++) {
            Node node = nodeList.item(i);
            if (node != null) {
                Node nodeParent = node.getParentNode();
                if (nodeParent != null) {
                    nodeParent.removeChild(node);
                }
            }
        }
    }

    private void deleteComments(Node node) {
        try {
            NodeList comments = (NodeList) ensureXPath().evaluate(".//comment", node, XPathConstants.NODESET);
            if (comments != null) {
                deleteNodes(comments);
            }
        } catch (XPathExpressionException e) {
            // Don't care
        }
    }

    private String prettyPrintFunctionNodeOrChild(Node node) {
        node = node.cloneNode(true);
        deleteComments(node);
        deleteFunctionBody(node);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tf.newTransformer();
        } catch (TransformerConfigurationException e) {
            LOG.warn("Failed to create transformer for pretty-printing XML", e);
            return node.getTextContent();
        }
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter result = new StringWriter();
        try {
            transformer.transform(new DOMSource(node), new StreamResult(result));
        } catch (TransformerException e) {
            LOG.warn("Problem while pretty-printing XML", e);
            return node.getTextContent();
        }
        return result.toString();
    }

    private String postProcessSignature(String signature) {
        // Squeeze multiple space signs into a single space
        String collapsedSpace = signature.replaceAll("\\s+", " ");
        String trimmed = collapsedSpace.trim();
        return trimmed;
    }

    private Node getNodeOrDie(Node nodeOfInterest, String xpathExpression) throws FunctionSignatureParseException {
        Optional<Node> r = getOptionalNode(nodeOfInterest, xpathExpression);
        if (r.isPresent()) return r.get();
        else {
            if (debugParseExceptions) {
                throw new FunctionSignatureParseException("Missing node `" + xpathExpression + "' in "
                        + prettyPrintFunctionNodeOrChild(nodeOfInterest) + " (" + funcLocForReporting() + ")");
            } else {
                throw new FunctionSignatureParseException();
            }
        }
    }

    private Optional<Node> getOptionalNode(Node nodeOfInterest, String xpathExpression) throws FunctionSignatureParseException {
        Node result = null;
        try {
            result = (Node) ensureXPath().evaluate(xpathExpression, nodeOfInterest, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            if (debugParseExceptions) {
                throw new FunctionSignatureParseException("Error finding node `" + xpathExpression + "' in "
                        + prettyPrintFunctionNodeOrChild(nodeOfInterest) + " (" + funcLocForReporting() + ")");
            } else {
                throw new FunctionSignatureParseException(e);
            }
        }

        return Optional.ofNullable(result);
    }

    private List<Node> getPossiblyEmptyNodeList(Node nodeOfInterest, String xpathExpression) throws FunctionSignatureParseException {
        NodeList result = null;
        try {
            result = (NodeList) ensureXPath().evaluate(xpathExpression,
                    nodeOfInterest, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            if (debugParseExceptions) {
                throw new FunctionSignatureParseException("Error finding nodes matching `" + xpathExpression + "' in " +
                        nodeOfInterest.getTextContent() + " (" + funcLocForReporting() + ")");
            } else {
                throw new FunctionSignatureParseException(e);
            }
        }

        if (result == null) return Collections.emptyList();

        List<Node> resultList = new ArrayList<>();
        for (int iNode = 0; iNode < result.getLength(); iNode++) {
            resultList.add(result.item(iNode));
        }

        return resultList;
    }

    private String funcLocForReporting() {
        int startLoc = parseFunctionStartLoc(functionNode);
        return filePath + ":" + startLoc;
    }

    public static int parseFunctionStartLoc(Node funcNode) {
        int xmlStartLoc = Integer.parseInt((String) funcNode.getUserData("lineNumber"));
        // The srcML representation starts with a one-line XML declaration, which we subtract here.
        return xmlStartLoc - 1;
    }

    //        private XPath ensureXPath() {
//            if (xPath == null) {
//                xPath = XPathFactory.newInstance().newXPath();
//            }
//            return xPath;
//        }
    private XPath ensureXPath() {
        return tlXPath.get();
    }
}
