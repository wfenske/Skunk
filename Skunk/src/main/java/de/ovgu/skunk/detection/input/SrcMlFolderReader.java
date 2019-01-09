package de.ovgu.skunk.detection.input;

import de.ovgu.skunk.detection.data.*;
import de.ovgu.skunk.util.GroupingListMap;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
import java.util.List;
import java.util.Map;

/**
 * The Class SrcMlFolderReader.
 */
public class SrcMlFolderReader {
    private static Logger LOG = Logger.getLogger(SrcMlFolderReader.class);

    private final Context ctx;
    private final PositionalXmlReader reader;
    Map<String, Document> srcmlFilesByFileKey = new HashMap<>();
    private final IMethodFactory methodFactory;

    /**
     * Instantiates a new srcML folder reader.
     *
     * @param ctx Context object
     */
    public SrcMlFolderReader(Context ctx) {
        this(ctx, new PositionalXmlReader());
    }

    /**
     * Instantiates a new srcML folder reader using the given XML reader
     *
     * @param ctx Context object
     */
    protected SrcMlFolderReader(Context ctx, PositionalXmlReader reader) {
        this(ctx, reader, Method::new);
    }

    /**
     * Instantiates a new srcML folder reader using the given XML reader and method factory
     *
     * @param ctx Context object
     */
    public SrcMlFolderReader(Context ctx, PositionalXmlReader reader, IMethodFactory methodFactory) {
        this.ctx = ctx;
        this.reader = reader;
        this.methodFactory = methodFactory;
    }

    /**
     * Process files to get metrics from srcMl
     */
    public void ProcessFiles() {
        LOG.info("Processing SrcML files ...");
        readAllSrcMlDocs();
        internAllFunctions();
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
                LOG.debug("Parsed SrcML file " + processed + "/" + numAllFiles + " (" + (numAllFiles - processed)
                        + " to go).");
            }
        }
        LOG.info("Parsed " + processed + " SrcML file(s).");
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
            Element current = (Element) directives.item(i);
            if (PositionalXmlReader.getNodeLineNumberAsIs(current) == (featureRef.start + 1)) {
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

    public Document readSrcmlFile(InputStream fileInput, String filePath) {
        try {
            return reader.readXML(fileInput);
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
        while (sibling != null && (PositionalXmlReader.getNodeLineNumberAsIs((Element) sibling) <= (featureRef.end + 1))) {
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
        // get or create function
        final Method function = findFunctionUsingNode(funcNode, filePath, fileDesignator);
        final int existingFunctionStartLoc = function.start1;
        final int actualFunctionStartLoc = FunctionSignatureParser.parseFunctionStartLoc(funcNode);

        if (existingFunctionStartLoc != actualFunctionStartLoc) {
            LOG.info("Ignoring feature reference " + featureRef + ". It refers to an alternative definition of the same function within the same file. We cannot currently handle this case. Existing function is " + function);
            return;
        }

        // add location to the function
        function.AddFeatureConstant(featureRef);
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

    private Method findFunctionUsingNode(Node funcNode, String filePath, String fileDesignator) {
        ParsedFunctionSignature functionSignature = parseFunctionSignature(funcNode, filePath);
        Method function = ctx.functions.FindFunction(fileDesignator, functionSignature);
        return function;
    }

    private Method parseFunction(Node funcNode, String filePath) {
        String fileDesignator = ctx.files.KeyFromFilePath(filePath);
        return parseFunction(funcNode, filePath, fileDesignator);
    }

    private Method parseFunction(Node funcNode, String filePath, String fileDesignator) {
        ParsedFunctionSignature functionSignature = parseFunctionSignature(funcNode, filePath);
        return parseFunctionUsingSignature(funcNode, filePath, fileDesignator, functionSignature);
    }

    /**
     * <p>Parses the function definition contained within the SrcML XML node,
     * creates a Skunk Method object from it and stores it properly in the respective collections that need to know
     * about the function.</p>
     * <p>
     * <p>What we want to parse here, has the following XML form.</p>
     *
     * @formatter:off <pre><function><type><name>int</name></type> <name>os_init_job_environment</name><parameter_list>(<param><decl><type><name>server_rec</name>
     * *</type><name>server</name></decl></param>, <param><decl><type><specifier>const</specifier> <name>char</name>
     * *</type><name>user_name</name></decl></param>, <param><decl><type><name>int</name></type>
     * <name>one_process</name></decl></param>)</parameter_list>
     * ... </function>
     * </pre>
     * <p>
     * The original C code looks like this:
     *
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
    private Method parseFunctionUsingSignature(Node funcNode, String filePath, String fileDesignator, ParsedFunctionSignature functionSignature) {
        String textContent = funcNode.getTextContent();
        int len = countLines(textContent);
        return methodFactory.create(ctx, functionSignature.signature, filePath, functionSignature.cStartLoc, len,
                functionSignature.originalLinesOfCode, textContent);
    }

    private void internAllFunctions() {
        LOG.debug("Parsing all functions in all files.");
        for (File file : ctx.files.AllFiles()) {
            internAllFunctionsInFile(file);
        }
    }

    public void internAllFunctionsInFile(File file) {
        LOG.debug("Parsing functions in file " + file);
        String filePath = file.filePath;
        String fileDesignator = ctx.files.KeyFromFilePath(filePath);
        Document doc = srcmlFilesByFileKey.get(fileDesignator);
        Method[] parsedFunctions = parseAllFunctionsInFile(doc, filePath);
        internNewlyReadFunctions(parsedFunctions, fileDesignator);
    }

    public Method[] parseAllFunctionsInFile(Document doc, String filePath) {
        NodeList functionNodes = doc.getElementsByTagName("function");
        final int numFunctions = functionNodes.getLength();
        Method[] result = new Method[numFunctions];
        for (int i = 0; i < numFunctions; i++) {
            Node funcNode = functionNodes.item(i);
            Method func = parseFunction(funcNode, filePath);
            result[i] = func;
        }
        LOG.debug("Found " + numFunctions + " functions in `" + filePath + "'.");
        adjustImprobableFunctionEndPositions(result);
        adjustDuplicateFunctionSignatures(result);
        return result;
    }

    private void internNewlyReadFunctions(Method[] functions, String fileDesignator) {
        for (Method function : functions) {
//            Method existingFunction = ctx.functions.FindFunction(fileDesignator, function.functionSignatureXml);
//            if (existingFunction != null) {
//                throw new RuntimeException("Internal error: Function already exists. New function: " + function + " Existing function: " + existingFunction);
//            }
            ctx.functions.AddFunctionToFile(fileDesignator, function);
            ctx.files.InternFunctionIntoExistingFile(fileDesignator, function);
        }
    }

    private static void adjustImprobableFunctionEndPositions(Method[] functionsByStartPos) {
        int len = functionsByStartPos.length;
        if (len < 2) return;
        Method previousFunc = functionsByStartPos[0];
        for (int i = 1; i < len; i++) {
            Method nextFunc = functionsByStartPos[i];
            previousFunc.maybeAdjustMethodEndBasedOnNextFunction(nextFunc);
            previousFunc = nextFunc;
        }
    }

    private void adjustDuplicateFunctionSignatures(Method[] functionsByOccurrence) {
        final boolean logDebug = LOG.isDebugEnabled();
        GroupingListMap<String, Method> functionsByOriginalSignature = new GroupingListMap<>();
        for (Method f : functionsByOccurrence) {
            functionsByOriginalSignature.put(f.originalFunctionSignature, f);
        }

        for (Map.Entry<String, List<Method>> e : functionsByOriginalSignature.getMap().entrySet()) {
            final List<Method> functionsWithSameSignature = e.getValue();
            final int len = functionsWithSameSignature.size();
            if (len == 1) continue;
            final String originalSignature = e.getKey();
            for (int i = 0, count = 1; i < len; i++, count++) {
                Method f = functionsWithSameSignature.get(i);
                final String uniqueFunctionSignature = originalSignature + " #" + count;
                if (logDebug) {
                    LOG.debug("Adjusting signature of " + f + " to " + uniqueFunctionSignature);
                }
                f.uniqueFunctionSignature = uniqueFunctionSignature;
            }
        }
    }


    /**
     * Extracts the function signature from a SrcML XML function node
     *
     * @param functionNode the SrcML XML node containing the function definition
     * @return the function's signature
     */
    public static ParsedFunctionSignature parseFunctionSignature(Node functionNode, String path) {
        FunctionSignatureParser parser = new FunctionSignatureParser(functionNode, path);
        ParsedFunctionSignature result = parser.parseFunctionSignature();
        return result;
    }

    /**
     * Count the number of lines in a string. A line is interpreted to end at a linefeed character (&quot;\n&quot;). An
     * empty string is defined to have no lines. If the last line does not end in a linefeed character, the return value
     * is nevertheless increased by 1.  In other words, both
     * <code>foo\nbar\n</code> and  <code>foo\nbar</code> are considered as having 2 lines.
     *
     * @param str the string
     * @return number of lines
     */
    public static int countLines(String str) {
        int result = 0;

        final int len = str.length();

        char lastChar = '\0';
        for (int i = 0; i < len; i++) {
            lastChar = str.charAt(i);
            if (lastChar == '\n') result++;
        }

        if ((len > 0) && (lastChar != '\n')) {
            result++;
        }

        return result;
    }

    public static int countSloc(String grossCode) {
        String noComments = FunctionSignatureParser.removeComments(grossCode, false);
        char[] charArray = noComments.toCharArray();
        int result = 0;
        boolean sawNonWhiteSpace = false;
        for (char cur : charArray) {
            if (!Character.isWhitespace(cur)) sawNonWhiteSpace = true;
            if (cur == '\n') {
                if (sawNonWhiteSpace) result++;
                sawNonWhiteSpace = false;
            }
        }
        final int len = charArray.length;
        if ((len > 0) && (charArray[len - 1] != '\n')) {
            result++;
        }
        return result;
    }
}
