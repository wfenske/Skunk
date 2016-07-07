package de.ovgu.skunk.detection.output;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.ovgu.skunk.detection.data.FeatureExpressionCollection;
import de.ovgu.skunk.detection.data.FileCollection;
import de.ovgu.skunk.detection.data.MethodCollection;

/**
 * The Class ProcessedDataHandler saves the data that is created by the CppStats
 * and SrcML readers. The data that is saved by this class, can be reloaded
 * again.
 */
public class ProcessedDataHandler {

    private static final String featuresPath = "features.xml";
    private static final String methodsPath = "methods.xml";
    private static final String generalPath = "general.txt";
    private static final String filesPath = "files.xml";

    /**
     * Save the data processed during the operation into a general file,
     * features file and a method file
     */
    public static void SaveProcessedData() {
        System.out.println();
        System.out.print("Saving processed data ...");

        String date = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
        String generalInput = "FeatureExpressionCollection=" + FeatureExpressionCollection.GetCount() + ";"
                + FeatureExpressionCollection.GetLoc() + ";" + FeatureExpressionCollection.GetMeanLofc() + ";"
                + FeatureExpressionCollection.numberOfFeatureConstantReferences;

        // Save files
        final SimpleFileWriter writer = new SimpleFileWriter();
        try {
            writer.write(new File(date + "_" + featuresPath), FeatureExpressionCollection.SerializeFeatures());
            writer.write(new File(date + "_" + methodsPath), MethodCollection.SerializeMethods());
            writer.write(new File(date + "_" + filesPath), FileCollection.SerializeFiles());
            writer.write(new File(date + "_" + generalPath), generalInput);
        } catch (IOException e) {
            throw new RuntimeException("I/O exception while saving processed data files", e);
        }

        System.out.printf(" done. Files (%s) saved in directory %s.\n", writer.prettyFileNameList(), writer.getDir());
    }

    /**
     * Load processed data from the given folder
     *
     * @param folderPath
     *            the path of the folder containing processed data files
     */
    public static void LoadProcessedData(String folderPath) {
        System.out.println();
        System.out.println("Loading processed data from folder " + folderPath + " ...");

        // open the directory
        File directory = new File(folderPath);
        if (directory.exists() && directory.isDirectory()) {
            // check for necessary files
            try {
                for (File current : directory.listFiles()) {
                    if (!current.isDirectory()) {
                        if (current.getName().contains("_")) {
                            switch (current.getName().split("_")[1]) {
                            case featuresPath:
                                FeatureExpressionCollection.DeserialzeFeatures(current);
                                break;
                            case methodsPath:
                                MethodCollection.DeserialzeMethods(current);
                                break;
                            case filesPath:
                                FileCollection.DeserialzeFiles(current);
                                break;
                            case generalPath: {
                                // read text file, first line is for
                                // featureexpressioncollection
                                List<String> lines = FileUtils.readLines(current);
                                String general = lines.get(0);

                                // set unserializable values
                                String[] split = general.split("=")[1].split(";");
                                FeatureExpressionCollection.SetCount(Integer.parseInt(split[0]));
                                FeatureExpressionCollection.AddLoc(Integer.parseInt(split[1]));
                                FeatureExpressionCollection.SetMeanLofc(Integer.parseInt(split[2]));
                                FeatureExpressionCollection.numberOfFeatureConstantReferences = Integer
                                        .parseInt(split[3]);
                                break;
                            }
                            }
                        }
                    }
                }
                System.out.println(" done.");
            } catch (Exception e) {
                throw new RuntimeException("Error loading processed data from " + directory, e);
            }
        } else {
            throw new RuntimeException("No processed data in directory " + directory);
        }
    }
}
