package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.*;
import de.ovgu.skunk.detection.detector.DetectionConfig;
import de.ovgu.skunk.detection.detector.SmellReason;
import de.ovgu.skunk.util.FileUtils;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.*;

public class AnalyzedDataHandler {
    private final Context ctx;

    /**
     * A comparator that compares featurenames of feature constants.
     */
    public final static Comparator<FeatureReference> FEATURECONSTANT_FEATURENAME_COMPARATOR = new Comparator<FeatureReference>() {
        @Override
        public int compare(FeatureReference f1, FeatureReference f2) {
            return f1.feature.Name.compareTo(f2.feature.Name);
        }
    };
    /**
     * A comparator that compares the filepath of feature constant
     */
    public final static Comparator<FeatureReference> FEATURECONSTANT_FILEPATH_COMPARATOR = new Comparator<FeatureReference>() {
        @Override
        public int compare(FeatureReference f1, FeatureReference f2) {
            return f1.filePath.compareTo(f2.filePath);
        }
    };
    /**
     * A comparator that compares startposition of feature constants.
     */
    public final static Comparator<FeatureReference> FEATURECONSTANT_START_COMPARATOR = new Comparator<FeatureReference>() {
        @Override
        public int compare(FeatureReference f1, FeatureReference f2) {
            return Integer.compare(f1.start, f2.start);
        }
    };
    /**
     * A comparator that compares startposition of feature constants methods.
     */
    public final static Comparator<FeatureReference> FEATURECONSTANT_METHOD_COMPARATOR = new Comparator<FeatureReference>() {
        @Override
        public int compare(FeatureReference f1, FeatureReference f2) {
            if (f1.inMethod == null) {
                if (f2.inMethod == null) return 0;
                return -1;
            }
            if (f2.inMethod == null) return 1;
            int s1 = f1.inMethod.start1;
            int s2 = f2.inMethod.start1;
            return Integer.compare(s1, s2);
        }
    };
    /**
     * The comparator that compares the smell value of the csv records.
     */
    public final static Comparator<Object[]> ABSmellComparator = new Comparator<Object[]>() {
        @Override
        public int compare(Object[] f1, Object[] f2) {
            final int ixAbSmell = MethodMetricsColumns.ABSmell.ordinal();
            float s1 = (float) f1[ixAbSmell];
            float s2 = (float) f2[ixAbSmell];
            return Float.compare(s2, s1);
        }
    };
    /**
     * The comparator that compares the smell value of the csv records.
     */
    public final static Comparator<Object[]> AFSmellComparator = new Comparator<Object[]>() {
        @Override
        public int compare(Object[] f1, Object[] f2) {
            final int ixAfSmell = FileMetricsColumns.AFSmell.ordinal();
            float s1 = (float) f1[ixAfSmell];
            float s2 = (float) f2[ixAfSmell];
            return Float.compare(s2, s1);
        }
    };
    /**
     * The comparator that compares the smell value of the csv records.
     */
    public final static Comparator<Object[]> LGSmellComparator = new Comparator<Object[]>() {
        @Override
        public int compare(Object[] f1, Object[] f2) {
            final int ixLgSmell = FeatureMetricsColumns.LGSmell.ordinal();
            float s1 = (float) f1[ixLgSmell];
            float s2 = (float) f2[ixLgSmell];
            return Float.compare(s2, s1);
        }
    };

    /**
     * Instantiates a new presenter.
     *
     * @param ctx
     */
    public AnalyzedDataHandler(Context ctx) {
        this.ctx = ctx;
    }

    /**** TXT Start End Saving *****/
    public void SaveTextResults(Map<FeatureReference, List<SmellReason>> results) {
        // get the results of the complete detection process and the whole
        // project
        String overview = this.getOverviewResults(results);
        // get overview per attribute
        String attributes = this.getAttributeOverviewResults(results);
        // Sortiert nach location und file
        String files = this.getFileSortedResults(results);
        String methods = this.getMethodSortedResults(results);
        // get the results sorted per feature
        String features = this.getFeatureSortedResults(results);
        String fileNamePrefix = ctx.getDetectionOutputFilenamePrefix();
        SimpleFileWriter writer = new SimpleFileWriter();
        try {
            writer.write(new java.io.File(fileNamePrefix + "overview.txt"), overview);
            writer.write(new java.io.File(fileNamePrefix + "attributes.txt"), attributes);
            writer.write(new java.io.File(fileNamePrefix + "files.txt"), files);
            writer.write(new java.io.File(fileNamePrefix + "methods.txt"), methods);
            writer.write(new java.io.File(fileNamePrefix + "features.txt"), features);
            System.out.println("Detection result files (" + writer.prettyFileNameList() + ") saved in `"
                    + writer.getDirForDisplay() + "'");
        } catch (IOException e) {
            throw new RuntimeException("I/O error while saving detection results as text.", e);
        }
    }

    /**
     * Creates the overview metrics for each attribute, and saves it to the
     * output result
     *
     * @param results the results
     * @return the attribute overview results
     */
    private String getAttributeOverviewResults(Map<FeatureReference, List<SmellReason>> results) {
        String res = ctx.config.toString() + "\r\n\r\n\r\n\r\n\r\n";
        List<AttributeOverview> attributes = new ArrayList<>();
        for (FeatureReference key : results.keySet()) {
            for (SmellReason reason : results.get(key)) {
                // get fitting attribute or create one
                boolean add = true;
                for (AttributeOverview overview : attributes) {
                    if (overview.Reason.equals(reason)) add = false;
                }
                if (add) attributes.add(new AttributeOverview(ctx, reason));
                // add location information
                for (AttributeOverview overview : attributes)
                    if (overview.Reason.equals(reason)) overview.AddFeatureLocationInfo(key);
            }
        }
        // add attribute overview to output
        for (AttributeOverview attr : attributes)
            res += attr.toString();
        return res;
    }

    /**
     * Sorts the result per file and start1 and adds it to the resulting file
     *
     * @param results the results
     * @return the location results
     */
    private String getFileSortedResults(Map<FeatureReference, List<SmellReason>> results) {
        String res = ctx.config.toString() + "\r\n\r\n\r\n\r\n\r\n\r\n";
        // sort the keys after featurename, filepath and start1
        List<FeatureReference> sortedKeys = new ArrayList<>(results.keySet());
        Collections.sort(sortedKeys, new ComparatorChain<>(FEATURECONSTANT_FILEPATH_COMPARATOR,
                FEATURECONSTANT_START_COMPARATOR));
        res += ">>> File-Sorted Results:\r\n";
        String currentPath = "";
        // print the the locations and reasons sorted after feature
        for (FeatureReference key : sortedKeys) {
            if (!key.filePath.equals(currentPath)) {
                currentPath = key.filePath;
                res += "\r\n\r\n\r\n[File: " + currentPath + "]\r\n";
                res += "Start\t\tEnd\t\tFeature\t\tReason\r\n";
            }
            res += key.start + "\t\t" + key.end + "\t\t" + key.feature.Name + "\t\t" + results.get(key).toString()
                    + "\r\n";
        }
        return res;
    }

    /**
     * Sorts the results per feature, and presents the locations and reason for
     * each corresponding feature
     *
     * @param results the detection results
     * @return the results per feature
     */
    private String getFeatureSortedResults(Map<FeatureReference, List<SmellReason>> results) {
        String res = ctx.config.toString() + "\r\n\r\n\r\n\r\n\r\n";
        // sort the keys after featurename, filepath and start1
        List<FeatureReference> sortedKeys = new ArrayList<>(results.keySet());
        Collections.sort(sortedKeys, new ComparatorChain<>(FEATURECONSTANT_FEATURENAME_COMPARATOR,
                FEATURECONSTANT_FILEPATH_COMPARATOR, FEATURECONSTANT_START_COMPARATOR));
        res += ">>> Feature-Sorted Results";
        String currentName = "";
        String currentPath = "";
        // print the the locations and reasons sorted after feature
        for (FeatureReference key : sortedKeys) {
            if (!key.feature.Name.equals(currentName)) {
                currentName = key.feature.Name;
                res += "\r\n\r\n\r\n[Feature: " + currentName + "]\r\n";
                // reset filepath
                currentPath = "";
            }
            if (!key.filePath.equals(currentPath)) {
                currentPath = key.filePath;
                res += "File: " + currentPath + "\r\n";
                res += "Start\t\tEnd\t\tReason\r\n";
            }
            res += key.start + "\t\t" + key.end + "\t\t" + results.get(key).toString() + "\r\n";
        }
        return res;
    }

    /**
     * Sorts the results per Method and returns it in a string per
     * file/method/cnstant
     *
     * @param results the detection results
     * @return the results per feature
     */
    private String getMethodSortedResults(Map<FeatureReference, List<SmellReason>> results) {
        String res = ctx.config.toString() + "\r\n\r\n\r\n\r\n\r\n";
        List<FeatureReference> sortedKeys = new ArrayList<>(results.keySet());
        Collections.sort(sortedKeys, new ComparatorChain<>(FEATURECONSTANT_FILEPATH_COMPARATOR,
                FEATURECONSTANT_METHOD_COMPARATOR, FEATURECONSTANT_START_COMPARATOR));
        res += ">>> Method-Sorted Results";
        Method currentMethod = null;
        String currentPath = "";
        // print feature constants with reason per File and Method
        for (FeatureReference key : sortedKeys) {
            // don't display feature that are not in a method
            if (key.inMethod == null) continue;
            if (!key.filePath.equals(currentPath)) {
                currentPath = key.filePath;
                res += "\r\n\r\nFile: " + key.FilePathForDisplay();
            }
            if (!key.inMethod.equals(currentMethod)) {
                currentMethod = key.inMethod;
                res += "\r\nMethod: " + currentMethod.functionSignatureXml + "\r\n";
                res += "Start\t\tEnd\t\tReason\r\n";
            }
            res += key.start + "\t\t" + key.end + "\t\t" + results.get(key).toString() + "\r\n";
        }
        return res;
    }

    /**
     * Get the results of the complete set.
     *
     * @param results the result hash map from the detection process
     */
    private String getOverviewResults(Map<FeatureReference, List<SmellReason>> results) {
        String res = ctx.config.toString();
        // amount of feature constants
        List<String> constants = new ArrayList<>();
        float percentOfConstants = 0;
        // amount of feature constants
        int countLocations = results.entrySet().size();
        float percentOfLocations = 0;
        // lofcs in project
        int completeLofc = 0;
        // loac in project
        Map<String, List<Integer>> loacs = new HashMap<>();
        int completeLoac = 0;
        float loacPercentage = 0;
        for (FeatureReference constant : results.keySet()) {
            // get the amount of feature constants by saving each feature
            // constant name
            if (!constants.contains(constant.feature.Name)) constants.add(constant.feature.Name);
            // add lines of code to result
            completeLofc += constant.end - constant.start;
            // add all lines per file to the data structure, that are part of
            // the feature constant... no doubling for loac calculation
            if (!loacs.keySet().contains(constant.filePath)) loacs.put(constant.filePath, new ArrayList<>());
            for (int i = constant.start; i <= constant.end; i++) {
                if (!loacs.get(constant.filePath).contains(i)) loacs.get(constant.filePath).add(i);
            }
        }
        // calculate max loac
        for (String file : loacs.keySet())
            completeLoac += loacs.get(file).size();
        // calculate percentages
        loacPercentage = completeLoac * 100.0f / ctx.featureExpressions.GetLoc();
        percentOfLocations = countLocations * 100 / ctx.featureExpressions.numberOfFeatureConstantReferences;
        percentOfConstants = constants.size() * 100 / ctx.featureExpressions.GetFeatures().size();
        // Complete overview
        res += "\r\n\r\n\r\n>>> Complete Overview\r\n";
        res += "Number of features: \t" + constants.size() + " (" + percentOfConstants + "% of "
                + ctx.featureExpressions.GetFeatures().size() + " constants)\r\n";
        res += "Number of feature constants: \t" + countLocations + " (" + percentOfLocations + "% of "
                + ctx.featureExpressions.numberOfFeatureConstantReferences + " locations)\r\n";
        res += "Lines of annotated Code: \t" + completeLoac + " (" + loacPercentage + "% of "
                + ctx.featureExpressions.GetLoc() + " LOC)\r\n";
        res += "Lines of feature code: \t\t" + completeLofc + "\r\n";
        res += "Mean LOFC per feature: \t\t" + ctx.featureExpressions.GetMeanLofc() + "\r\n\r\n\r\n";
        return res;
    }

    /**** TXT Start End Saving *****/
    /**** CSV Smell Value Saving ****/
    public void SaveCsvResults() {
        // ensure consistent file naming
        String fileNamePrefix = ctx.getMetricsOutputFilenamePrefix();
        String fnMethods = fileNamePrefix + "functions.csv";
        String fnFeatures = fileNamePrefix + "features.csv";
        String fnFiles = fileNamePrefix + "files.csv";
        String dirName;
        try {
            dirName = (new java.io.File(fnMethods)).getCanonicalFile().getParent();
        } catch (IOException e) {
            throw new RuntimeException("I/O error writing CSV results", e);
        }
        this.createFunctionCSV(fnMethods);
        this.createFeatureCSV(fnFeatures);
        this.createFileCSV(fnFiles);
        String outDir = FileUtils.relPath(dirName);
        if (outDir.isEmpty()) outDir = ".";
        System.out.printf("Metric files (%s, %s, %s) saved in `%s'\n", fnFeatures, fnFiles, fnMethods, outDir);
    }

    /**
     * Creates the file metric csv.
     *
     * @param fileName the file name
     */
    private void createFileCSV(String fileName) {
        CsvFileWriterHelper h = new CsvFileWriterHelper() {
            @Override
            protected void actuallyDoStuff(CSVPrinter csv) throws IOException {
                // add the header for the CSV file
                CsvRowProvider<File, Context, FileMetricsColumns> p = new CsvRowProvider<>(FileMetricsColumns.class, ctx);
                csv.printRecord(p.headerRow());
                // calculate values and add records
                List<Object[]> fileData = new ArrayList<>();
                for (File file : ctx.files.AllFiles()) {
                    if (skipFile(file)) continue;
                    fileData.add(p.dataRow(file));
                }
                // sort by smell value
                Collections.sort(fileData, new ComparatorChain<>(AFSmellComparator));
                for (Object[] record : fileData)
                    csv.printRecord(record);
            }
        };
        h.write(fileName);
    }

    /**
     * Creates the method csv.
     *
     * @param fileName Name of the output CSV file
     */
    private void createFeatureCSV(String fileName) {
        CsvFileWriterHelper h = new CsvFileWriterHelper() {
            @Override
            protected void actuallyDoStuff(CSVPrinter csv) throws IOException {
                CsvRowProvider<Feature, Context, FeatureMetricsColumns> p = new CsvRowProvider<>(FeatureMetricsColumns.class,
                        ctx);
                csv.printRecord(p.headerRow());
                List<Object[]> featureData = new ArrayList<>();
                for (Feature feat : ctx.featureExpressions.GetFeatures()) {
                    if (skipFeature(feat)) continue;
                    featureData.add(p.dataRow(feat));
                }
                // sort by smell value
                Collections.sort(featureData, new ComparatorChain<>(LGSmellComparator));
                for (Object[] record : featureData)
                    csv.printRecord(record);
            }
        };
        h.write(fileName);
    }

    /**
     * Creates the method csv.
     *
     * @param fileName Name of the output CSV file
     */
    private void createFunctionCSV(final String fileName) {
        CsvFileWriterHelper h = new CsvFileWriterHelper() {
            @Override
            protected void actuallyDoStuff(CSVPrinter csv) throws IOException {
                CsvRowProvider<Method, Context, MethodMetricsColumns> p = new CsvRowProvider<>(MethodMetricsColumns.class, ctx);
                // add the header for the csv file
                csv.printRecord(p.headerRow());
                // calculate values and add records
                List<Object[]> methodData = new ArrayList<>();
                for (Method meth : ctx.functions.AllMethods()) {
                    if (skipMethod(meth)) continue;
                    Object[] row = p.dataRow(meth);
                    methodData.add(row);
                }
                // sort by smell value
                Collections.sort(methodData, new ComparatorChain<>(ABSmellComparator));
                for (Object[] record : methodData)
                    csv.printRecord(record);
            }
        };
        h.write(fileName);
    }

    /**
     * Skip the method for csv file creation depending on the mandatory settings
     * of the configuration
     *
     * @param method the method
     * @return true, if method does not fulfill mandatory settings
     */
    private boolean skipMethod(Method method) {
        final DetectionConfig conf = ctx.config;
        if (conf.Method_LoacToLocRatio_Mand
                && ((float) method.GetLinesOfAnnotatedCode() / (float) method.getNetLoc()) < conf.Method_LoacToLocRatio) {
            return true;
        }
        if (conf.Method_NumberOfFeatureConstants_Mand
                && method.GetFeatureConstantCount() < conf.Method_NumberOfFeatureConstants) {
            return true;
        }
        if (conf.Method_NestingSum_Mand && method.nestingSum < conf.Method_NestingSum) {
            return true;
        }
        return false;
    }

    /**
     * Skip the method for CSV file creation depending on the mandatory settings
     * of the configuration
     *
     * @param file the file to test
     * @return true, if method does not fulfill mandatory settings
     */
    private boolean skipFile(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig conf = ctx.config;
        if (conf.File_LoacToLocRatio_Mand
                && ((float) file.GetLinesOfAnnotatedCode() / (float) file.loc) < conf.File_LoacToLocRatio)
            return true;
        if (conf.File_NumberOfFeatureConstants_Mand
                && file.GetFeatureConstantCount() < conf.File_NumberOfFeatureConstants)
            return true;
        if (conf.File_NestingSum_Mand && file.nestingSum < conf.File_NestingSum) return true;
        return false;
    }

    /**
     * Skip the feature for csv file creation depending on the mandatory
     * settings of the configuration
     *
     * @param feat the feature
     * @return true, if feature does not fulfill mandatory settings
     */
    private boolean skipFeature(Feature feat) {
        final DetectionConfig conf = ctx.config;
        if (conf.Feature_NumberNofc_Mand && (feat.getReferences().size() < conf.Feature_NumberNofc)) return true;
        if (conf.Feature_NumberLofc_Mand && (feat.getLofc() < conf.Feature_NumberLofc)) return true;
        return false;
    }
    /**** CSV Start End Saving *****/
}
