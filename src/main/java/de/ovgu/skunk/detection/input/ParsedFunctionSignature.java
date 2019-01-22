package de.ovgu.skunk.detection.input;

/**
 * Created by wfenske on 28.03.18.
 */
public class ParsedFunctionSignature {
    /**
     * Name of function along with return type and parameter list
     */
    public final String signature;
    /**
     * Number of lines the function signature originally comprised
     */
    public final int originalLinesOfCode;

    /**
     * Line number in the XML file.  Note, this count starts from 1, not from 0.
     */
    public final int cStartLoc;

    public ParsedFunctionSignature(String signature, int cStartLoc, int originalLinesOfCode) {
        this.signature = signature;
        this.cStartLoc = cStartLoc;
        this.originalLinesOfCode = originalLinesOfCode;
    }
}
