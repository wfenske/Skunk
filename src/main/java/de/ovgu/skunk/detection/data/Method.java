package de.ovgu.skunk.detection.data;

import de.ovgu.skunk.util.FileUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a function in the analyzed source code
 */
public class Method {
    private static final Logger LOG = Logger.getLogger(Method.class);
    /**
     * Compares functions by occurrence in their file, in ascending order.  Specifically, functions are first compared
     * by the name of the file, then by line number.  Although unneccessary, they are also compared by function
     * signature as a last resort.
     */
    public static final Comparator<? super Method> COMP_BY_OCCURRENCE = new Comparator<Method>() {
        @Override
        public int compare(Method f1, Method f2) {
            int cmp;
            cmp = f1.filePath.compareTo(f2.filePath);
            if (cmp != 0) return cmp;
            cmp = f1.start1 - f2.start1;
            if (cmp != 0) return cmp;
            return f1.uniqueFunctionSignature.compareTo(f2.uniqueFunctionSignature);
        }
    };

    private static final Pattern FUNCTION_NAME = Pattern.compile("^[^(]*\\b(\\w+)\\s*\\(");

    private final Context ctx;
//    /**
//     * Source code of the functions, as returned from the srcml function node
//     */
//    private final String sourceCode;
    /**
     * The original function signature, as it appears in the file
     */
    public String originalFunctionSignature;

    /**
     * The function signature, possibly with a suffix in case that a function of the same name appears multiple times in
     * the same file
     */
    public String uniqueFunctionSignature;

    /**
     * The part of the function signature that constitutes the function's name
     */
    public String functionName;

    /**
     * The first line of the function in the file, counted from 1.
     */
    public int start1;
    /**
     * The last line of the function in the file, counted from 1.
     */
    public int end1;
    /**
     * The lines of code of the method, including empty lines.
     */
    private int grossLoc;

    /**
     * The lines of code of just the signature, including empty lines, line breaks, etc.
     */
    private int signatureGrossLinesOfCode;

    /**
     * The lines of code of the function, excluding empty lines.
     */
    protected int netLoc = -1;

    /**
     * The lines of feature code inside the method.
     */
    public long lofc;
    /**
     * The amount of nestings in the method (1 per nesting)
     */
    public int nestingSum;
    /**
     * The maximal nesting depth in the method
     */
    public int nestingDepthMax;
    /**
     * The lines of visible annotated code. (amount of loc that is inside annotations)
     */
    public List<Integer> loac;
    private int processedLoac;
    /**
     * The map of the feature constants, by order of appearance
     */
    public Map<UUID, String> featureReferences;
    /**
     * The number feature constants in the method (non-duplicated).
     */
    public int numberFeatureConstantsNonDup;
    /**
     * The number feature locations.
     */
    public int numberFeatureLocations;
    /**
     * The number of negations in the method
     */
    public int negationCount;
    /**
     * The file path.
     */
    public String filePath;

    /**
     * Method.
     *
     * @param signature                 the signature
     * @param start1                    the starting line of the function within it's file (first line in the file is
     *                                  counted as 1)
     * @param grossLoc                  length of the function in lines of code, may include empty lines
     * @param signatureGrossLinesOfCode length of the function signature in lines of code, as it appears in the file
     *                                  (including line breaks, comments, etc.)
     */
    public Method(Context ctx, String signature, String filePath, int start1, int grossLoc
            , int signatureGrossLinesOfCode, String fullFunctionCode) {
        this.ctx = ctx;
        this.originalFunctionSignature = signature;
        this.uniqueFunctionSignature = signature;
        this.start1 = start1;
        this.grossLoc = grossLoc;
        this.nestingSum = 0;
        this.nestingDepthMax = 0;
        // do not count start1 line while calculating the end1
        this.end1 = start1 + grossLoc - 1;
        // initialize loc
        this.lofc = 0;
        this.featureReferences = new LinkedHashMap<>();
        this.loac = new ArrayList<>();
        this.numberFeatureConstantsNonDup = 0;
        this.numberFeatureLocations = 0;
        this.negationCount = 0;
        this.filePath = filePath;
        //this.sourceCode = sourceCode;
        this.signatureGrossLinesOfCode = signatureGrossLinesOfCode;
        Matcher nameMatcher = FUNCTION_NAME.matcher(originalFunctionSignature);
        if (nameMatcher.find()) {
            this.functionName = nameMatcher.group(1);
        } else {
            this.functionName = originalFunctionSignature;
        }
    }

    public void maybeAdjustMethodEndBasedOnNextFunction(Method nextFunction) {
        final int nextStart = nextFunction.start1;
        if (this.end1 < nextStart) return;

        final int newEnd1 = nextStart - 2;

        LOG.debug("Adjusting improbable function end position of " + this + " from " + end1 + " to " + newEnd1);

        int lenReduction = end1 - newEnd1;
        this.end1 = newEnd1;
        this.grossLoc = Math.max(grossLoc - lenReduction, 1);
    }

    /**
     * Adds the feature location if it is not already added.
     *
     * @param featureRef the loc
     */
    public void AddFeatureConstant(FeatureReference featureRef) {
        if (this.featureReferences.containsKey(featureRef.id)) {
            return;
        }

        assertFeatureRefMatchesFile(featureRef);

        // connect feature to the method
        this.featureReferences.put(featureRef.id, featureRef.feature.Name);
        featureRef.inMethod = this;
        // assign nesting depth values
        if (featureRef.nestingDepth > this.nestingDepthMax) this.nestingDepthMax = featureRef.nestingDepth;
        // calculate lines of feature code (if the feature is longer than
        // the method, use the method end)
        final int lofcEnd = Math.min(featureRef.end, this.end1);
        if (featureRef.start > lofcEnd) {
            // NOTE, 2018-11-09, wf: It sometimes happens that src2srcml puts the end of a function way past its actual location.
            // We adjust for that, but the error error is still in the DOM, an may crop up again here.
            LOG.warn("Attempt to assign feature reference that starts behind the function's end. Might be due to a parsing error of the function's end. Aborting. function=" + this + "; featureRef=" + featureRef);
            return;
        }
        final int lofcStart = featureRef.start;
        if (lofcStart < this.start1) {
            throw new RuntimeException("Internal error: attempt to calculate LOCF for reference that starts before the function's start (LOFC count will be off). function=" + this + "; featureRef=" + featureRef);
        }
        if (lofcStart > lofcEnd) {
            // NOTE, 2018-11-09, wf: See my comment above.
            LOG.warn("Attempt to calculate LOCF where start > end. Aborting. function=" + this + "; featureRef=" + featureRef + "; lofcStart=" + lofcStart + "; lofcEnd=" + lofcEnd);
            return;
        }

        final int lofcIncrement = computeLofcIncrement(lofcStart, lofcEnd);
        this.lofc += lofcIncrement;
        updateLoac(lofcStart, lofcEnd);
    }

    private void updateLoac(int lofcStart, int lofcEnd) {
        // add lines of visible annotated code (amount of loc that is
        // inside annotations) until end of feature constant or end of
        // method
        File file = ctx.files.FindFile(this.filePath);
        for (int current = lofcStart; current <= lofcEnd; current++) {
            if (!(this.loac.contains(current)) && !file.emptyLines.contains(current))
                this.loac.add(current);
        }
    }

    private int computeLofcIncrement(int lofcStart, int lofcEnd) {
        int lofcIncrement = lofcEnd - lofcStart + 1;
        File file = ctx.files.FindFile(this.filePath);
        // Subtract empty lines (do not count them as feature code)
        for (int current : file.emptyLines) {
            if (current <= lofcStart) continue;
            if (current >= lofcEnd) break;
            lofcIncrement--;
        }
        return lofcIncrement;
    }

    private void assertFeatureRefMatchesFile(FeatureReference featureRef) {
        File file = ctx.files.FindFile(this.filePath);
        File fileOfFeatureRef = ctx.files.FindFile(featureRef.filePath);
        if (file != fileOfFeatureRef) {
            throw new RuntimeException("Looking at two different files (should be identical): " + file + ", " + fileOfFeatureRef);
        }
    }

    /**
     * Gets amount of feature constants (duplicated)
     *
     * @return the int
     */
    public int GetFeatureConstantCount() {
        return this.featureReferences.size();
    }

    /**
     * Gets the lines of annotated code.
     *
     * @return lines of visible annotated code (not counting doubles per feature,..)
     */
    public int GetLinesOfAnnotatedCode() {
        return this.processedLoac;
    }

    /**
     * Gets the number of feature constants of the method (non-duplicated)
     *
     * @return the int
     */
    public void SetNumberOfFeatureConstantsNonDup() {
        ArrayList<String> constants = new ArrayList<>();
        for (UUID id : featureReferences.keySet()) {
            FeatureReference constant = ctx.featureExpressions.GetFeatureConstant(featureReferences.get(id), id);
            if (!constants.contains(constant.feature.Name)) constants.add(constant.feature.Name);
        }
        this.numberFeatureConstantsNonDup = constants.size();
    }

    /**
     * Gets the number of feature locations. A feature location is a complete set of feature constants on one line.
     *
     * @return the number of feature occurences in the method
     */
    public void SetNumberOfFeatureLocations() {
        ArrayList<Integer> noLocs = new ArrayList<>();
        // remember the starting position of each feature location, but do not
        // add it twice
        for (UUID id : featureReferences.keySet()) {
            FeatureReference constant = ctx.featureExpressions.GetFeatureConstant(featureReferences.get(id), id);
            if (!noLocs.contains(constant.start)) noLocs.add(constant.start);
        }
        this.processedLoac = this.loac.size();
        this.numberFeatureLocations = noLocs.size();
    }

    /**
     * Cet the amount of negated annotations
     *
     * @return the amount of negated annotations
     */
    public void SetNegationCount() {
        int result = 0;
        for (UUID id : featureReferences.keySet()) {
            FeatureReference constant = ctx.featureExpressions.GetFeatureConstant(featureReferences.get(id), id);
            if (constant.notFlag) result++;
        }
        this.negationCount = result;
    }

    /**
     * Sets the nesting sum.
     */
    public void SetNestingSum() {
        // minNesting defines the lowest nesting depth of the method (nesting
        // depths are file based)
        int res = 0;
        int minNesting = 5000;
        // add each nesting to the nesting sum
        for (UUID id : featureReferences.keySet()) {
            FeatureReference constant = ctx.featureExpressions.GetFeatureConstant(featureReferences.get(id), id);
            res += constant.nestingDepth;
            if (constant.nestingDepth < minNesting) minNesting = constant.nestingDepth;
        }
        // subtract the complete minNesting depth (for each added location)
        res -= this.featureReferences.size() * minNesting;
        this.nestingSum = res;
    }

    public void InitializeNetLocMetric() {
        de.ovgu.skunk.detection.data.File file = ctx.files.FindFile(this.filePath);
        this.netLoc = this.grossLoc;
        for (int empty : file.emptyLines) {
            if (empty >= this.start1 && empty <= this.end1) this.netLoc--;
        }
    }

    public int getNetLoc() {
        int r = this.netLoc;
        if (r < 0) {
            throw new AssertionError("Attempt to read net LOC before initializing it.");
        }
        return this.netLoc;
    }

    public int getGrossLoc() {
        return this.grossLoc;
    }

    /**
     * @return The lines of code of just the signature, including empty lines, line breaks, etc.
     */
    public int getSignatureGrossLinesOfCode() {
        return this.signatureGrossLinesOfCode;
    }

    public String FilePathForDisplay() {
        return FileUtils.displayPathFromCppstatsSrcMlPath(filePath);
    }

    /**
     * @return The original source file's path, relative to the project's repository root.
     */
    public String ProjectRelativeFilePath() {
        return FileUtils.projectRelativePathFromCppstatsSrcMlPath(filePath);
    }

    /**
     * @return <code>true</code> iff there are other, alternative function definitions in the same file that have the
     * same signature
     */
    public boolean hasAlternativeDefinitions() {
        return !originalFunctionSignature.equals(uniqueFunctionSignature);
    }

    public boolean hasSameOriginalSignature(Method other) {
        return this.originalFunctionSignature.equals(other.originalFunctionSignature);
    }

    public boolean hasSameName(Method other) {
        return this.functionName.equals(other.functionName);
    }

    @Override
    public String toString() {
        return String.format("Function [%s /* %s:%d,%d */]", uniqueFunctionSignature, FilePathForDisplay(),
                start1, end1);
    }

//    public String sourceCodeWithLineNumbers() {
//        String[] lines = sourceCode.split("\n");
//        StringBuilder r = new StringBuilder();
//        int lineNo = start1;
//        for (int iLine = 0; iLine < lines.length; iLine++, lineNo++) {
//            r.append(String.format("% 5d: ", lineNo));
//            r.append(lines[iLine]);
//            r.append("\n");
//        }
//        return r.toString();
//    }

    /*
     * NOTE, 2016-11-18, wf: Generated by Eclipse
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + filePath.hashCode();
        result = prime * result + uniqueFunctionSignature.hashCode();
        return result;
    }

    /*
     * NOTE, 2016-11-18, wf: Generated by Eclipse
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Method)) return false;
        Method other = (Method) obj;
        if (filePath == null) {
            if (other.filePath != null) return false;
        } else if (!filePath.equals(other.filePath)) return false;
        if (!uniqueFunctionSignature.equals(other.uniqueFunctionSignature)) return false;
        return true;
    }

//    /**
//     * @return Source code of the function as parsed by src2srcml
//     */
//    public String getSourceCode() {
//        return sourceCode;
//    }
}
