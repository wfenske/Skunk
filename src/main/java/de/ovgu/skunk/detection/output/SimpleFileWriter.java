package de.ovgu.skunk.detection.output;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

public class SimpleFileWriter {
    private SortedSet<String> fileNames = new TreeSet<>();
    private String dir = null;

    public void write(File f, String content) throws IOException {
        de.ovgu.skunk.util.FileUtils.write(f, content);
        rememberWrittenFile(f);
    }

    public void writeGzipped(File f, Consumer<Writer> dataProvider) throws IOException {
        de.ovgu.skunk.util.FileUtils.writeGzipped(f, dataProvider);
        rememberWrittenFile(f);
    }

    private void rememberWrittenFile(File f) throws IOException {
        if (dir == null) dir = f.getCanonicalFile().getParent();
        fileNames.add(f.getName());
    }

    public String prettyFileNameList() {
        StringBuilder b = new StringBuilder();
        for (String n : fileNames) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(n);
        }
        return b.toString();
    }

    /**
     * @return Name of the directory in which the first file handed to
     * {@link #write(File, String)} was located.
     */
    private String getDir() {
        if (dir == null) throw new NullPointerException("getDir is called before any files have been written.");
        return dir;
    }

    /**
     * @return Name of the directory in which the first file handed to
     * {@link #write(File, String)} was located.
     */
    public String getDirForDisplay() {
        return de.ovgu.skunk.util.FileUtils.relPathForDisplay(this.getDir());
    }
}
