package de.ovgu.skunk.detection.input;

import de.ovgu.skunk.detection.data.Context;
import de.ovgu.skunk.util.FileUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Stack;

/**
 * The Class CppStatsFolderReader for reading and processing csv files.
 */
public class CppStatsFolderReader {
    private final Context ctx;

    /**
     * The CppStats results folder.
     */
    private String pathToCppStatsFolder;

    /**
     * Instantiates a new CppStatsFolderReader
     *
     * @param ctx
     * @param pathToCppStatsFolder the path of the folder
     */
    public CppStatsFolderReader(Context ctx, String pathToCppStatsFolder) {
        this.ctx = ctx;
        this.pathToCppStatsFolder = pathToCppStatsFolder;
    }

    /**
     * Processes all CppStatsFiles
     */
    public void ProcessFiles() {
        System.out.println("Processing CppStats CSV files in folder "
                + FileUtils.relPathForDisplay(pathToCppStatsFolder) + " ...");
        // this.getFeatureNames(new File(this.pathToCppStatsFolder +
        // "/merged_scattering_degrees.csv"));
        this.getFeatureConstants(new File(this.pathToCppStatsFolder + "/cppstats_featurelocations.csv"));
        this.internRemainingFilesAndCalculateProjectLoc(new File(this.pathToCppStatsFolder + "/cppstats.csv"));
        System.out.println("... CppStats processing done. Found (non-header) " + ctx.files.AllFiles().size()
                + " source files.");
    }

    /**
     * Get feature constants and lofc from file "cppstats_featurelocations.csv"
     *
     * @param csvFile the csv file
     */
    private void getFeatureConstants(File csvFile) {
        System.out.print("... getting feature position metrics  ...");
        try {
            Stack<CppStatsFeatureConstant> constants = new Stack<>();
            CSVParser parser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.DEFAULT);
            for (CSVRecord rec : parser) {
                // first lines are not necessary
                if ((rec.get(0).equals("sep=,")) || (rec.get(0).equals("FILENAME")))
                    continue;
                else {
                    // assemble feature information
                    String filePath = rec.get(0);
                    // don't use header files
                    if (filePath.endsWith(".h.xml")) {
                        continue;
                    }
                    ctx.files.InternFile(filePath);
                    int start = Integer.parseInt(rec.get(1));
                    int end = Integer.parseInt(rec.get(2));
                    String type = rec.get(3);
                    String entry = rec.get(4);
                    // if file changes, empty stack and save all information
                    if ((constants.size() > 0) && (!constants.peek().filePath.equals(filePath))) {
                        while (constants.size() > 0)
                            constants.pop().SaveFeatureConstantInformation(constants.size() + 1);
                    }
                    // if stack is empty, add feature constant without parent
                    if (constants.size() == 0) {
                        CppStatsFeatureConstant constant = new CppStatsFeatureConstant(ctx, entry, filePath, type, start,
                                end, null);
                        if (constant.featureExpressions.size() != 0) constants.push(constant);
                    } else {
                        // if end of top element is bigger than start, the
                        // current element is nested in the top element --> push
                        // on stack
                        if (constants.peek().end > start) {
                            CppStatsFeatureConstant constant = new CppStatsFeatureConstant(ctx, entry, filePath, type, start,
                                    end, constants.peek());
                            if (constant.featureExpressions.size() != 0) constants.push(constant);
                        } else {
                            // save feature constant if the endline of the top
                            // element is lower than the curent start location
                            while ((constants.size() > 0) && (constants.peek().end <= start))
                                constants.pop().SaveFeatureConstantInformation(constants.size() + 1);
                            // item has to be put on stack, use top as reference
                            // for current feature constant, else push first
                            // element
                            if (constants.size() > 0) {
                                CppStatsFeatureConstant fl = new CppStatsFeatureConstant(ctx, entry, filePath, type, start,
                                        end, constants.peek());
                                if (fl.featureExpressions.size() != 0) constants.push(fl);
                            } else {
                                CppStatsFeatureConstant fl = new CppStatsFeatureConstant(ctx, entry, filePath, type, start,
                                        end, null);
                                if (fl.featureExpressions.size() != 0) constants.push(fl);
                            }
                        }
                    }
                }
            }
            // if there is still an element
            while (constants.size() > 0)
                constants.pop().SaveFeatureConstantInformation(constants.size() + 1);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read feature constants from CSV file " + csvFile.getAbsolutePath(),
                    e);
        }
        ctx.featureExpressions.PostAction();
        System.out.println(" done.");
    }

    /**
     * Gets the lines of code for the project from file "cppstats.csv"
     *
     * @param csvFile the csv file
     * @return the LOC project
     */
    private void internRemainingFilesAndCalculateProjectLoc(File csvFile) {
        System.out.print("... getting lines of code ...");
        // Parse CSV and get lines of code from aggregation line
        try {
            CSVParser parser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.RFC4180);
            for (CSVRecord rec : parser) {
                final String filename = rec.get(0);
                switch (filename) {
                    case "sep=,":
                    case "FILENAME":
                    case "FUNCTIONS":
                    case "ALL - MERGED":
                        continue;
                }
                ctx.files.InternFile(filename);
                ctx.featureExpressions.AddLoc(Integer.parseInt(rec.get(1)));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project LOC metrics from CSV file " + csvFile.getAbsolutePath(),
                    e);
        }
        System.out.println(" done.");
    }
}
