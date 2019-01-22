package de.ovgu.skunk.detection.data;

@FunctionalInterface
public interface IMethodFactory {
    Method create(Context ctx, String signature, String filePath, int start1, int grossLoc,
                  int signatureGrossLinesOfCode, String sourceCode
    );
}
