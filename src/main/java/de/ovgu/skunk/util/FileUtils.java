/**
 *
 */
package de.ovgu.skunk.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility functions for dealing with file paths
 *
 * @author wfenske
 */
public final class FileUtils {
    private static Logger LOG = Logger.getLogger(FileUtils.class);

    public static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");

    /**
     * We don't want this class to be instantiated. It's supposed to be a
     * collection of static helper methods.
     */
    private FileUtils() {
    }

    /**
     * @param file A file object
     * @return Path of the given file relative to the current working directory
     */
    public static String relPath(File file) {
        return new File(".").toURI().relativize(file.toURI()).getPath();
    }

    /**
     * @param pathname Pathname of a file
     * @return Path of the given file relative to the current working directory
     */
    public static String relPath(String pathname) {
        return relPath(new File(pathname));
    }

    /**
     * @param pathname Pathname of a file or directory
     * @return A possibly shorter version of the pathname, possibly relative to
     * the current working directory
     */
    public static String relPathForDisplay(String pathname) {
        String r = relPath(pathname);
        if (r.isEmpty()) return ".";
        if (r.contains("..")) try {
            String rNormalized = new URI(r).normalize().getPath();
            r = rNormalized;
        } catch (URISyntaxException e) {
            // We just continue with `r', removing ".." is not our priority.
        }
        return (r.length() < pathname.length()) ? r : pathname;
    }

    private static boolean isXmlFilename(Path p) {
        return p.getFileName().toString().endsWith(".xml");
    }

    /**
     * @param cppstatsSrcMlFilePath Pathname of a SrcML source file that has been created by
     *                              cppStats. It usually looks something like
     *                              <code>&quot;/Users/me/subjects/apache/_cppstats/src/support/suexec.c.xml&quot;</code>
     *                              .
     * @return The pathname of the actual C source file, e.g.
     * <code>&quot;/Users/me/subjects/apache/source/src/support/suexec.c&quot;</code>
     */
    public static String actualSourceFilePathFromCppstatsSrcMlPath(String cppstatsSrcMlFilePath) {
        return actualSourceFilePathFromCppstatsSrcMlPath(Paths.get(cppstatsSrcMlFilePath));
    }

    private static String actualSourceFilePathFromCppstatsSrcMlPath(Path cppstatsSrcMlFilePath) {
        final String basename = cppstatsSrcMlFilePath.getFileName().toString();
        final int basenamePosXmlSuffix = basename.lastIndexOf(".xml");
        if (basenamePosXmlSuffix != -1) {
            int ixCppStatsFolder = posCppstatsFolder(cppstatsSrcMlFilePath);
            if (ixCppStatsFolder != -1) {
                // HINT, 2016-11-15, wf: We cannot simply call subpath here
                // because this will return a relative path, even if pOrig is an
                // absolute path ...
                Path pathBefore = cppstatsSrcMlFilePath;
                while (pathBefore.getNameCount() > ixCppStatsFolder) {
                    pathBefore = pathBefore.getParent();
                }
                final int pathAfterBegin = ixCppStatsFolder + 1;
                final int pathAfterEnd = cppstatsSrcMlFilePath.getNameCount() - 1;
                final String pathAfterString;
                if (pathAfterBegin != pathAfterEnd) {
                    Path pathAfter = cppstatsSrcMlFilePath.subpath(pathAfterBegin, pathAfterEnd);
                    pathAfterString = pathAfter.toString();
                } else {
                    pathAfterString = ".";
                }
                String basenameNoXml = basename.substring(0, basenamePosXmlSuffix);
                Path actualSourcePath = Paths.get(pathBefore.toString(), "source", pathAfterString, basenameNoXml);
                return actualSourcePath.normalize().toString();
            }
        }
        return cppstatsSrcMlFilePath.toString();
    }

    public static Path projectRelativePathFromCppstatsSrcMlPath(Path cppstatsSrcMlFilePath) {
        final String basename = cppstatsSrcMlFilePath.getFileName().toString();
        final int basenamePosXmlSuffix = basename.lastIndexOf(".xml");
        if (basenamePosXmlSuffix != -1) {
            int ixCppStatsFolder = posCppstatsFolder(cppstatsSrcMlFilePath);
            if (ixCppStatsFolder != -1) {
                String basenameNoXml = basename.substring(0, basenamePosXmlSuffix);

                int beginIndex = ixCppStatsFolder + 1;
                int endIndex = cppstatsSrcMlFilePath.getNameCount() - 1;
                if (beginIndex == endIndex) {
                    return Paths.get(basenameNoXml);
                } else {
                    Path projectRelativeDir = cppstatsSrcMlFilePath.subpath(beginIndex, endIndex);
                    Path actualSourcePath = Paths.get(projectRelativeDir.toString(), basenameNoXml);
                    return actualSourcePath;
                }
            }
        }
        throw new IllegalArgumentException("Not a proper cppstats XML source file path: " + cppstatsSrcMlFilePath);
    }

    /**
     * @param cppstatsSrcMlFilePath Pathname of a SrcML source file that has been created by
     *                              cppStats. It usually looks something like
     *                              <code>&quot;/Users/me/subjects/snapshots/apache/2004-01-23/_cppstats/src/support/suexec.c.xml&quot;</code>
     *                              .
     * @return The simplified name of the actual C source file, e.g.
     * <code>&quot;snapshots/apache/2004-01-23/support/suexec.c&quot;</code>
     */
    public static String displayPathFromCppstatsSrcMlPath(String cppstatsSrcMlFilePath) {
        String actualSourceFilePath = actualSourceFilePathFromCppstatsSrcMlPath(cppstatsSrcMlFilePath);
        return relPathForDisplay(actualSourceFilePath);
    }

    /**
     * @param cppstatsSrcMlFilePath Pathname of a SrcML source file that has been created by
     *                              cppStats. It usually looks something like
     *                              <code>&quot;/Users/me/subjects/apache/_cppstats/src/support/suexec.c.xml&quot;</code>
     *                              .
     * @return The name of the actual C source file relative to the project repository root, e.g.
     * <code>&quot;support/suexec.c&quot;</code>
     */
    public static String projectRelativePathFromCppstatsSrcMlPath(String cppstatsSrcMlFilePath) {
        return projectRelativePathFromCppstatsSrcMlPath(Paths.get(cppstatsSrcMlFilePath)).toString();
    }

    /**
     * Returns the index of the path component that denotes a cppStats directory
     * (e.g. <code>&quot;_cppstats&quot;</code>)
     *
     * @param cppstatsSrcMlFilePath
     * @return The index of the cppStats directory path component or -1, if such
     * a component does not exist
     */
    private static int posCppstatsFolder(Path cppstatsSrcMlFilePath) {
        final int nameCount = cppstatsSrcMlFilePath.getNameCount();
        for (int i = nameCount - 1; i >= 0; i--) {
            String name = cppstatsSrcMlFilePath.getName(i).toString();
            switch (name) {
                case "_cppstats_featurelocations":
                case "_cppstats":
                    return i;
            }
        }
        return -1;
    }

    private static boolean isCppStathPathname(Path p) {
        return posCppstatsFolder(p) != -1;
    }

    public static String coerceCppStatsPathToSourcePath(String filePath) {
        Path p = Paths.get(filePath);
        if (isCppStatsSrcMlFilePath(p)) {
            return actualSourceFilePathFromCppstatsSrcMlPath(p);
        } else {
            return filePath;
        }
    }

    public static String coerceCppStatsPathToRelSourcePath(String filePath) {
        return relPath(coerceCppStatsPathToSourcePath(filePath));
    }

    public static List<String> readLines(File file) throws IOException {
        return org.apache.commons.io.FileUtils.readLines(file, DEFAULT_CHARSET);
    }


    private static boolean isCppStatsSrcMlFilePath(Path p) {
        return isXmlFilename(p) && isCppStathPathname(p);
    }

    public static void write(File file, CharSequence contents) throws IOException {
        org.apache.commons.io.FileUtils.write(file, contents, DEFAULT_CHARSET);
    }

    public static void writeGzipped(File file, Consumer<Writer> dataProvider) throws IOException {
        try (Writer out = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file)), DEFAULT_CHARSET)) {
            dataProvider.accept(out);
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Exception while writing file " + file + ". Trying to remove file.", ex);
            try {
                boolean deleted = file.delete();
                if (!deleted) {
                    LOG.error("Failed to delete likely incomplete output file " + file + ". Delete file manually!");
                }
            } catch (RuntimeException deletionEx) {
                LOG.error("Exception while trying to delete likely incomplete output file " + file + "." +
                        " Delete file manually!", deletionEx);
            }
            throw ex;
        }
    }

    public static void readGzipped(File file, Consumer<Reader> dataSink) throws IOException {
        try (Reader r = new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), FileUtils.DEFAULT_CHARSET)) {
            dataSink.accept(r);
        }
    }
}
