package de.ovgu.skunk.detection.input;

/**
 * Created by wfenske on 28.03.18.
 */
public class ParsedFunctionSignature {
    /**
     * Name of function along with return type and parameter list
     */
    final String signature;
    /**
     * Number of lines the function signature originally comprised
     */
    final int originalLinesOfCode;

    public ParsedFunctionSignature(String signature, int originalLinesOfCode) {
        this.signature = signature;
        this.originalLinesOfCode = originalLinesOfCode;
    }
}
