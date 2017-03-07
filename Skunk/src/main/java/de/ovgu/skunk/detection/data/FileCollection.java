package de.ovgu.skunk.detection.data;

import com.thoughtworks.xstream.XStream;
import de.ovgu.skunk.util.FileUtils;

import java.util.*;

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
     * @param fileDesignator a string denoting the source file. This can either be the name
     *                       of the SrcML file or the key generated from this file name,
     *                       pointing to the actual C file from which the SrcML was
     *                       generated.
     * @return the file or null, if it does not exist
     */
    public File FindFile(String fileDesignator) {
        String keyPath = KeyFromFilePath(fileDesignator);
        return Files.get(keyPath);
    }

    /**
     * Intern a Skunk function into a known file.  If the file is not yet known, a {@link RuntimeException} is thrown.
     *
     * @param fileDesignator a string denoting the source file. This can either be the name
     *                       of the SrcML file or the key generated from this file name,
     *                       pointing to the actual C file from which the SrcML was
     *                       generated.
     * @param method         The Skunk function object to intern
     */
    public void InternFunctionIntoExistingFile(String fileDesignator, Method method) {
        File file = FindFile(fileDesignator);
        if (file == null) {
            throw new RuntimeException("Unknown file `" + fileDesignator + "'.");
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
    public String SerializeFiles() {
        // nullify already processed data for memory reasons
        List<File> fileList = new ArrayList<>(Files.values());
        for (File file : fileList) {
            file.emptyLines.clear();
            file.loac.clear();
        }
        XStream stream = new XStream();
        String xmlFeatures = stream.toXML(fileList);
        return xmlFeatures;
    }

    /**
     * Deserializes an xml string into the collection.
     *
     * @param xmlFile the serialized xml representation
     */
    public void DeserialzeFiles(java.io.File xmlFile) {
        XStream stream = new XStream();
        List<File> fileList = (List<File>) stream.fromXML(xmlFile);
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
