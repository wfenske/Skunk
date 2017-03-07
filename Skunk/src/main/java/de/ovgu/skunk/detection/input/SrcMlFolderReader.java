package de.ovgu.skunk.detection.input;

import de.ovgu.skunk.detection.data.*;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class SrcMlFolderReader.
 */
public class SrcMlFolderReader {
    private static Logger LOG = Logger.getLogger(SrcMlFolderReader.class);
    private final Context ctx;
    Map<String, Document> srcmlFilesByFileKey = new HashMap<>();

    /**
     * Instantiates a new srcML folder reader.
     *
     * @param ctx
     */
    public SrcMlFolderReader(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Process files to get metrics from srcMl
     */
    public void ProcessFiles() {
        LOG.info("Processing SrcML files ...");
        readAllSrcMlDocs();
        readAllFunctions();
        processFeatureLocations();
        LOG.info("Done processing SrcML files.");
    }

    private void processFeatureLocations() {
        // go through each feature location and calculate granularity
        for (Feature feat : ctx.featureExpressions.GetFeatures()) {
            for (FeatureReference ref : feat.getReferences()) {
                this.processFeatureReference(ref);
            }
        }
    }

    private void readAllSrcMlDocs() {
        final Collection<File> allFiles = ctx.files.AllFiles();
        int processed = 0;
        int numAllFiles = allFiles.size();
        for (File file : allFiles) {
            readAndRememberSrcmlFile(file.filePath);
            if ((++processed) % 10 == 0) {
                LOG.info("Parsed SrcML file " + processed + "/" + numAllFiles + " (" + (numAllFiles - processed)
                        + " to go).");
            }
        }
    }

    public Document readAndRememberSrcmlFile(String filePath) {
        try (InputStream inputStream = new ByteArrayInputStream(getFileBytes(filePath))) {
            return readAndRememberSrcmlFile(inputStream, filePath);
        } catch (IOException e) {
            throw new RuntimeException("I/O exception closing srcml file " + filePath, e);
        }
    }

    public Document readAndRememberSrcmlFile(InputStream inputStream, String filePath) {
        String filePathKey = ctx.files.KeyFromFilePath(filePath);
        Document doc = readSrcmlFile(inputStream, filePath);
        srcmlFilesByFileKey.put(filePathKey, doc);
        return doc;
    }

    /**
     * Calculate granularity of the feature location by checking parent nodes
     *
     * @param featureRef the reference to a feature constant
     */
    private void processFeatureReference(FeatureReference featureRef) {
        final String filePath = featureRef.filePath;
        final String filePathKey = ctx.files.KeyFromFilePath(filePath);
        Document doc = srcmlFilesByFileKey.get(filePathKey);
        // Get all lines of the xml and open a positional xml reader
        if (doc == null) {
            throw new RuntimeException("Doc for " + filePath + " is missing.");
        }
        // Assign to file
        File file = ctx.files.InternFile(filePath);
        file.AddFeatureConstant(featureRef);
        Node correspondingCppDirective = findCppDirectiveForFeatureLocation(doc, featureRef);
        if (correspondingCppDirective != null) {
            // calculate the granularity by checking each sibling node
            // from start1 to end1 of the annotation
            this.calculateGranularityOfFeatureConstantReference(featureRef, correspondingCppDirective);
            // assign this location to its corresponding method
            this.assignFeatureConstantReferenceToMethod(featureRef, correspondingCppDirective);
        } else {
            LOG.warn("Failed to find the CPP directive for feature constant reference " + featureRef);
        }
    }

    private Node findCppDirectiveForFeatureLocation(Document doc, FeatureReference featureRef) {
        // go through each directive and find the directive of the specific
        // location by using the start1 position
        NodeList directives = doc.getElementsByTagName("cpp:directive");
        for (int i = 0; i < directives.getLength(); i++) {
            Node current = directives.item(i);
            if (Integer.parseInt((String) current.getUserData("lineNumber")) == featureRef.start + 1) {
                // parent contains the if/endif values
                return current.getParentNode();
            }
        }
        return null;
    }

    private Document readSrcmlFile(String filePath) {
        try (InputStream inputStream = new ByteArrayInputStream(getFileBytes(filePath))) {
            return readSrcmlFile(inputStream, filePath);
        } catch (IOException e) {
            throw new RuntimeException("I/O exception closing srcml file " + filePath, e);
        }
    }

    public static Document readSrcmlFile(InputStream fileInput, String filePath) {
        try {
            return PositionalXmlReader.readXML(fileInput);
        } catch (IOException e) {
            throw new RuntimeException("I/O exception reading stream of file " + filePath, e);
        } catch (SAXException e) {
            throw new RuntimeException("Cannot parse file " + filePath, e);
        }
    }

    private void calculateGranularityOfFeatureConstantReference(FeatureReference featureRef, Node current) {
        // check sibling nodes until a granularity defining tag is found or
        // until the end1 of the annotation
        Node sibling = current;
        while (sibling != null && Integer.parseInt((String) sibling.getUserData("lineNumber")) <= featureRef.end + 1) {
            // set granularity and try to assign a discipline
            featureRef.SetGranularity(sibling);
            featureRef.SetDiscipline(sibling);
            // text nodes do not contain line numbers --> next until not #text
            sibling = sibling.getNextSibling();
            while (sibling != null && sibling.getNodeName().equals("#text"))
                sibling = sibling.getNextSibling();
        }
    }

    /**
     * Gets the file contents as a byte stream.
     *
     * @param filePath the file path
     * @return the bytes in the file
     */
    private static byte[] getFileBytes(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("I/O exception reading contents of file " + filePath, e);
        }
    }

    /**
     * Assign feature constant reference to method.
     *
     * @param featureRef     the feature constant reference
     * @param annotationNode the annotation node where the reference occurred
     */
    private void assignFeatureConstantReferenceToMethod(FeatureReference featureRef, Node annotationNode) {
        // check parent nodes of the annotation until it is of type
        // function/unit
        Node funcNode = findParentFunctionNode(annotationNode);
        if (funcNode == null) {
            LOG.debug("Feature reference is not part of a function definition. Treated as a top-level reference: "
                    + featureRef);
            return;
        }
        String filePath = featureRef.filePath;
        String fileDesignator = ctx.files.KeyFromFilePath(filePath);
        // get or create method
        Method method = parseAndInternMethod(funcNode, filePath, fileDesignator);
        // add location to the method
        method.AddFeatureConstant(featureRef);
    }

    private Node findParentFunctionNode(Node annotationNode) {
        Node parent = annotationNode.getParentNode();
        while (!parent.getNodeName().equals("function")) {
            // if parent node is unit, it does not belong to a function
            if (parent.getNodeName().equals("unit")) {
                return null;
            }
            parent = parent.getParentNode();
        }
        return parent;
    }

    /**
     * <p>Parses the function definition contained within the SrcML XML node,
     * creates a Skunk Method object from it and stores it properly in the
     * respective collections that need to know about the function.</p>
     * <p>
     * <p>What we want to parse here, has the following XML form.</p>
     *
     * @formatter:off <pre><function><type><name>int</name></type> <name>os_init_job_environment</name><parameter_list>(<param><decl><type><name>server_rec</name> *</type><name>server</name></decl></param>, <param><decl><type><specifier>const</specifier> <name>char</name> *</type><name>user_name</name></decl></param>, <param><decl><type><name>int</name></type> <name>one_process</name></decl></param>)</parameter_list>
     * ... </function>
     * </pre>
     * <p>
     * The original C code looks like this:
     * <p>
     * <pre>
     * int os_init_job_environment(server_rec *server, const char *user_name, int one_process) { ... }
     *
     * @formatter:on
     *
     * @param funcNode
     * @param filePath
     * @param fileDesignator
     * @return the method parsed from this Node, never <code>null</code>
     */
    private Method parseAndInternMethod(Node funcNode, String filePath, String fileDesignator) {
        String functionSignature = parseFunctionSignature(funcNode, filePath);
        Method function = ctx.functions.FindFunction(fileDesignator, functionSignature);
        if (function == null) {
            function = parseFunctionUsingSignature(funcNode, filePath, fileDesignator, functionSignature);
            ctx.functions.AddFunctionToFile(fileDesignator, function);
        }
        ctx.files.InternFunctionIntoExistingFile(fileDesignator, function);
        return function;
    }

    public Method parseFunction(Node funcNode, String filePath) {
        String fileDesignator = ctx.files.KeyFromFilePath(filePath);
        return parseFunction(funcNode, filePath, fileDesignator);
    }

    private Method parseFunction(Node funcNode, String filePath, String fileDesignator) {
        String functionSignature = parseFunctionSignature(funcNode, filePath);
        return parseFunctionUsingSignature(funcNode, filePath, fileDesignator, functionSignature);
    }

    private Method parseFunctionUsingSignature(Node funcNode, String filePath, String fileDesignator, String functionSignature) {
        // Line number in the XML file.  Note, this count starts from 1, not from 0.
        int cStartLoc = FunctionSignatureParser.parseFunctionStartLoc(funcNode);
        String textContent = funcNode.getTextContent();
        int len = countLines(textContent);
        return new Method(ctx, functionSignature, filePath, cStartLoc, len, textContent);
    }

    private void readAllFunctions() {
        LOG.debug("Parsing all functions in all files.");
        for (File file : ctx.files.AllFiles()) {
            readAllFunctionsInFile(file);
        }
    }

    private void readAllFunctionsInFile(File file) {
        LOG.debug("Parsing functions in file " + file);
        String filePath = file.filePath;
        String fileDesignator = ctx.files.KeyFromFilePath(filePath);
        Document doc = srcmlFilesByFileKey.get(fileDesignator);
        NodeList functions = doc.getElementsByTagName("function");
        int numFunctions = functions.getLength();
        LOG.debug("Found " + numFunctions + " functions in `" + fileDesignator + "'.");
        for (int i = 0; i < numFunctions; i++) {
            Node funcNode = functions.item(i);
            parseAndInternMethod(funcNode, filePath, fileDesignator);
        }
    }

    /**
     * Extracts the function signature from a SrcML XML function node
     *
     * @param functionNode the SrcML XML node containing the function definition
     * @return the function's signature
     */
    public static String parseFunctionSignature(Node functionNode, String path) {
        FunctionSignatureParser parser = new FunctionSignatureParser(functionNode, path);
        if (LOG.isDebugEnabled()) {
            parser.enableDebugParseExceptions();
        }
        return parser.parseFunctionSignature();
    }

    /**
     * Count the number of lines in a string. A line is interpreted to end1 at
     * the first occurrence of either &quot;\r\n&quot;, &quot;\n&quot;, or
     * &quot;\r\n&quot;.
     *
     * @param str the string
     * @return number of lines
     */
    private static int countLines(String str) {
        String[] lines = str.split("\r\n|\r|\n");
        return lines.length;
    }


}
