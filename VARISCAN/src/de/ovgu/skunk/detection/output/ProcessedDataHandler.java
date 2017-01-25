package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.Context;
import de.ovgu.skunk.detection.data.FeatureExpressionCollection;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * The Class ProcessedDataHandler saves the data that is created by the CppStats
 * and SrcML readers. The data that is saved by this class, can be reloaded
 * again.
 */
public class ProcessedDataHandler {
    private final Context ctx;

    private static final String featuresPath = "features.xml";
    private static final String methodsPath = "methods.xml";
    private static final String generalPath = "general.txt";
    private static final String filesPath = "files.xml";

    public ProcessedDataHandler(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Save the data processed during the operation into a general file,
     * features file and a method file
     */
    public void SaveProcessedData() {
        final FeatureExpressionCollection featureExpressions = ctx.featureExpressions;
        System.out.println();
        System.out.print("Saving processed data ...");
        String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        String generalInput = "FeatureExpressionCollection=" + featureExpressions.GetLoc() + ";"
                + featureExpressions.GetMeanLofc() + ";"
                + featureExpressions.numberOfFeatureConstantReferences;
        // Save files
        final SimpleFileWriter writer = new SimpleFileWriter();
        try {
            writer.write(new File(date + "_" + featuresPath), featureExpressions.SerializeFeatures());
            writer.write(new File(date + "_" + methodsPath), ctx.functions.SerializeMethods());
            writer.write(new File(date + "_" + filesPath), ctx.files.SerializeFiles());
            writer.write(new File(date + "_" + generalPath), generalInput);
        } catch (IOException e) {
            throw new RuntimeException("I/O exception while saving processed data files", e);
        }
        System.out.printf(" done. Files (%s) saved in `%s'.\n", writer.prettyFileNameList(), writer.getDirForDisplay());
    }

    /**
     * Load processed data from the given folder
     *
     * @param folderPath the path of the folder containing processed data files
     */
    public void LoadProcessedData(String folderPath) {
        final FeatureExpressionCollection featureExpressions = ctx.featureExpressions;

        System.out.println();
        System.out.print("Loading processed data from folder `" + folderPath + "' ...");
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
                                    ctx.featureExpressions.DeserialzeFeatures(current);
                                    break;
                                case methodsPath:
                                    ctx.functions.deserializeMethods(current);
                                    break;
                                case filesPath:
                                    ctx.files.DeserialzeFiles(current);
                                    break;
                                case generalPath: {
                                    // read text file, first line is for
                                    // featureexpressioncollection
                                    List<String> lines = FileUtils.readLines(current);
                                    String general = lines.get(0);
                                    // set unserializable values
                                    String[] split = general.split("=")[1].split(";");
                                    featureExpressions.AddLoc(Integer.parseInt(split[0]));
                                    featureExpressions.SetMeanLofc(Integer.parseInt(split[1]));
                                    featureExpressions.numberOfFeatureConstantReferences = Integer
                                            .parseInt(split[2]);
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
            throw new RuntimeException("No processed data in directory `" + directory + "'");
        }
    }
}
