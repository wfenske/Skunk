package de.ovgu.skunk.detection.data;

import com.thoughtworks.xstream.XStream;
import de.ovgu.skunk.util.FileUtils;

import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;

public class FileCollection {
    private final Context ctx;

    /**
     * The methods per file.
     */
    private Map<String, File> Files;

    /**
     * Instantiates a new method collection.
     */
    public FileCollection(Context ctx) {
        this.ctx = ctx;
        Files = new LinkedHashMap<>();
    }

    /**
     * Adds the file or gets it if already inside the list
     *
     * @param srcMlFilePath the file path
     * @return the Skunk file
     */
    public File InternFile(String srcMlFilePath) {
        String keyPath = KeyFromFilePath(srcMlFilePath);
        File existingFile = Files.get(keyPath);
        if (existingFile != null) return existingFile;
        File newFile = new File(ctx, srcMlFilePath);
        Files.put(keyPath, newFile);
        // System.out.println("Added file #" + Files.size() + ": " + keyPath + "
        // (" + srcMlFilePath + ")");
        return newFile;
    }

    /**
     * Gets the file.
     *
     * @param actualFilePath a string denoting the source file. This can either be the name
     *                       of the SrcML file or the key generated from this file name,
     *                       pointing to the actual C file from which the SrcML was
     *                       generated.
     * @return the file or null, if it does not exist
     */
    public File FindFile(String actualFilePath) {
        FilePath fp = ctx.internFilePath(actualFilePath);
        return FindFile(fp);
    }

    /**
     * Gets the file.
     *
     * @param fp a string denoting the source file. This can either be the name
     *           of the SrcML file or the key generated from this file name,
     *           pointing to the actual C file from which the SrcML was
     *           generated.
     * @return the file or null, if it does not exist
     */
    public File FindFile(FilePath fp) {
        return Files.get(fp.pathKey);
    }

    /**
     * Intern a Skunk function into a known file.  If the file is not yet known, a {@link RuntimeException} is thrown.
     *
     * @param fp     a string denoting the source file. This can either be the name
     *               of the SrcML file or the key generated from this file name,
     *               pointing to the actual C file from which the SrcML was
     *               generated.
     * @param method The Skunk function object to intern
     */
    public void InternFunctionIntoExistingFile(FilePath fp, Method method) {
        File file = FindFile(fp);
        if (file == null) {
            throw new RuntimeException("Unknown file `" + fp.pathKey + "'.");
        }
        file.InternFunction(method);
    }

    public static String KeyFromFilePath(String filePath) {
        return FileUtils.coerceCppStatsPathToRelSourcePath(filePath);
    }

    /**
     * Calculate metrics for all metrics after finishing the collection
     */
    public void PostAction() {
        for (File file : Files.values()) {
            file.SetNegationCount();
            file.SetNumberOfFeatureConstants();
            file.SetNumberOfFeatureLocations();
            file.SetNestingSum();
        }
    }

    /**
     * Serialize the features into a xml representation
     *
     * @return A xml representation of this object.
     */
    public Consumer<Writer> SerializeFiles() {
        // nullify already processed data for memory reasons
        List<File> fileList = new ArrayList<>(Files.values());
        for (File file : fileList) {
            file.emptyLines.clear();
            file.loac.clear();
        }
        XStream stream = new XStream();
        return (writer -> stream.toXML(fileList, writer));
    }

    /**
     * Deserializes the XML provided by the reader into the collection.
     *
     * @param xmlFileReader reader providing the serialized XML representation
     */
    public void DeserializeFiles(Reader xmlFileReader) {
        XStream stream = new XStream();
        List<File> fileList = (List<File>) stream.fromXML(xmlFileReader);
        for (File f : fileList) {
            String key = KeyFromFilePath(f.filePath);
            Files.put(key, f);
        }
    }

    /**
     * @return All files, in the order they have been added
     */
    public Collection<File> AllFiles() {
        return Files.values();
    }
}
