package de.ovgu.skunk.detection.data;

import de.ovgu.skunk.util.FileUtils;

import java.util.*;

/**
 * Representation of a function in the analyzed source code
 */
public class Method {
    /**
     * Compares functions by occurrence in their file, in ascending order.  Specifically, functions are first compared by the name of the file, then by line number.  Although unneccessary, they are also compared by function signature as a last resort.
     */
    public static final Comparator<? super Method> COMP_BY_OCCURRENCE = new Comparator<Method>() {
        @Override
        public int compare(Method f1, Method f2) {
            int cmp;
            cmp = f1.filePath.compareTo(f2.filePath);
            if (cmp != 0) return cmp;
            cmp = f1.start1 - f2.start1;
            if (cmp != 0) return cmp;
            return f1.functionSignatureXml.compareTo(f2.functionSignatureXml);
        }
    };

    private final Context ctx;
    /**
     * Source code of the functions, as returned from the srcml function node
     */
    private final String sourceCode;
    /**
     * The function signature.
     */
    public String functionSignatureXml;
    /**
     * The first line of the function in the file, counted from 1.
     */
    public int start1;
    /**
     * The last line of the function in the file, counted from 1.
     */
    public int end1;
    /**
     * The lines of code of the method.
     */
    public int loc;
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
     * The lines of visible annotated code. (amount of loc that is inside
     * annotations)
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
     * @param signature  the signature
     * @param start1     the start1 line of the function (first file in the file is counted as 1)
     * @param loc        lenght of the function in lines of code
     * @param sourceCode source code as returned from src2srcml
     */
    public Method(Context ctx, String signature, String filePath, int start1, int loc, String sourceCode) {
        this.ctx = ctx;
        this.functionSignatureXml = signature;
        this.start1 = start1;
        this.loc = loc;
        this.nestingSum = 0;
        this.nestingDepthMax = 0;
        // do not count start1 line while calculating the end1
        this.end1 = start1 + loc - 1;
        // initialize loc
        this.lofc = 0;
        this.featureReferences = new LinkedHashMap<>();
        this.loac = new ArrayList<>();
        this.numberFeatureConstantsNonDup = 0;
        this.numberFeatureLocations = 0;
        this.negationCount = 0;
        this.filePath = filePath;
        this.sourceCode = sourceCode;
    }

    /**
     * Adds the feature location if it is not already added.
     *
     * @param featureRef the loc
     */
    public void AddFeatureConstant(FeatureReference featureRef) {
        if (!this.featureReferences.containsKey(featureRef.id)) {
            // connect feature to the method
            this.featureReferences.put(featureRef.id, featureRef.feature.Name);
            featureRef.inMethod = this;
            // assign nesting depth values
            if (featureRef.nestingDepth > this.nestingDepthMax) this.nestingDepthMax = featureRef.nestingDepth;
            // calculate lines of feature code (if the feature is longer than
            // the method, use the method end1)
            if (featureRef.end > this.end1)
                this.lofc += this.end1 - featureRef.start + 1;
            else this.lofc += featureRef.end - featureRef.start + 1;
            de.ovgu.skunk.detection.data.File file = ctx.files.FindFile(featureRef.filePath);
            for (int current : file.emptyLines) {
                if (featureRef.end > this.end1) {
                    if (current > featureRef.start && current < this.end1) this.lofc--;
                } else if (current > featureRef.start && current < featureRef.end) this.lofc--;
            }
            // add lines of visibile annotated code (amount of loc that is
            // inside annotations) until end1 of feature constant or end1 of
            // method
            for (int current = featureRef.start; current <= featureRef.end; current++) {
                if (!(this.loac.contains(current))
                        && !ctx.files.FindFile(this.filePath).emptyLines.contains(current))
                    this.loac.add(current);
                if (current == this.end1) break;
            }
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
     * @return lines of visible annotated code (not counting doubles per
     * feature,..)
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
     * Gets the number of feature locations. A feature location is a complete
     * set of feature constants on one line.
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
        // substract the complete minNesting depth (for each added location)
        res -= this.featureReferences.size() * minNesting;
        this.nestingSum = res;
    }

    public void SetLoc() {
        de.ovgu.skunk.detection.data.File file = ctx.files.FindFile(this.filePath);
        for (int empty : file.emptyLines) {
            if (empty >= this.start1 && empty <= this.end1) this.loc--;
        }
    }

    public String FilePathForDisplay() {
        return FileUtils.displayPathFromCppstatsSrcMlPath(filePath);
    }

    @Override
    public String toString() {
        return String.format("Function [%s /* %s:%d,%d */]", functionSignatureXml, FilePathForDisplay(),
                start1, end1);
    }

    public String sourceCodeWithLineNumbers() {
        String[] lines = sourceCode.split("\n");
        StringBuilder r = new StringBuilder();
        int lineNo = start1;
        for (int iLine = 0; iLine < lines.length; iLine++, lineNo++) {
            r.append(String.format("% 5d: ", lineNo));
            r.append(lines[iLine]);
            r.append("\n");
        }
        return r.toString();
    }

    /*
     * NOTE, 2016-11-18, wf: Generated by Eclipse
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
        result = prime * result + ((functionSignatureXml == null) ? 0 : functionSignatureXml.hashCode());
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
        if (functionSignatureXml == null) {
            if (other.functionSignatureXml != null) return false;
        } else if (!functionSignatureXml.equals(other.functionSignatureXml)) return false;
        return true;
    }

    /**
     * @return Source code of the function as parsed by src2srcml
     */
    public String getSourceCode() {
        return sourceCode;
    }
}
