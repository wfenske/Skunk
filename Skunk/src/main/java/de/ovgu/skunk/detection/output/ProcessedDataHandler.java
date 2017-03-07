package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.Context;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Class ProcessedDataHandler saves the data that is created by the CppStats
 * and SrcML readers. The data that is saved by this class, can be reloaded
 * again.
 */
public class ProcessedDataHandler {
    private final Context ctx;

    private static enum ProcessedDataFile {
        FEATURES {
            @Override
            public String filename(Context ctx) {
                return ctx.getProcessedDataFilenamePrefix() + "features.xml";
            }

            @Override
            public void save(Context ctx, SimpleFileWriter writer) throws IOException {
                String contents = ctx.featureExpressions.SerializeFeatures();
                writer.write(new File(filename(ctx)), contents);
            }

            @Override
            public void load(Context ctx, File file) {
                ctx.featureExpressions.DeserialzeFeatures(file);
            }
        },
        FUNCTIONS {
            @Override
            public String filename(Context ctx) {
                return ctx.getProcessedDataFilenamePrefix() + "functions.xml";
            }

            @Override
            public void save(Context ctx, SimpleFileWriter writer) throws IOException {
                String contents = ctx.functions.SerializeMethods();
                writer.write(new File(filename(ctx)), contents);
            }

            @Override
            public void load(Context ctx, File file) {
                ctx.functions.deserializeMethods(file);
            }
        },
        GENERAL {
            @Override
            public String filename(Context ctx) {
                return ctx.getProcessedDataFilenamePrefix() + "general.txt";
            }

            @Override
            public void save(Context ctx, SimpleFileWriter writer) throws IOException {
                String generalInput = "FeatureExpressionCollection=" + ctx.featureExpressions.GetLoc() + ";"
                        + ctx.featureExpressions.GetMeanLofc() + ";"
                        + ctx.featureExpressions.numberOfFeatureConstantReferences;
                writer.write(new File(filename(ctx)), generalInput);
            }

            @Override
            public void load(Context ctx, File file) throws IOException {
                // read text file, first line is for feature expression collection
                List<String> lines = de.ovgu.skunk.util.FileUtils.readLines(file);
                String general = lines.get(0);
                // set non-serializable values
                String[] split = general.split("=")[1].split(";");
                ctx.featureExpressions.AddLoc(Integer.parseInt(split[0]));
                ctx.featureExpressions.SetMeanLofc(Integer.parseInt(split[1]));
                ctx.featureExpressions.numberOfFeatureConstantReferences = Integer
                        .parseInt(split[2]);
            }
        },
        FILES {
            @Override
            public String filename(Context ctx) {
                return ctx.getProcessedDataFilenamePrefix() + "files.xml";
            }

            @Override
            public void save(Context ctx, SimpleFileWriter writer) throws IOException {
                writer.write(new File(filename(ctx)), ctx.files.SerializeFiles());
            }

            @Override
            public void load(Context ctx, File file) {
                ctx.files.DeserialzeFiles(file);
            }
        };

        public abstract String filename(Context ctx);

        public abstract void save(Context ctx, SimpleFileWriter writer) throws IOException;

        public abstract void load(Context ctx, File file) throws IOException;

        public static Optional<ProcessedDataFile> findConstantByFile(Context ctx, File file) {
            String basename = file.getName();
            for (ProcessedDataFile c : values()) {
                if (basename.equals(c.filename(ctx))) {
                    return Optional.of(c);
                }
            }
            return Optional.empty();
        }
    }

    public ProcessedDataHandler(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Save the data processed during the operation into a general file,
     * features file and a method file
     */
    public void SaveProcessedData() {
        System.out.println();
        System.out.print("Saving processed data ...");

        // Save files
        final SimpleFileWriter writer = new SimpleFileWriter();
        try {
            for (ProcessedDataFile f : ProcessedDataFile.values()) {
                f.save(ctx, writer);
            }
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

        System.out.println();
        System.out.print("Loading processed data from folder `" + folderPath + "' ...");
        // open the directory
        File directory = new File(folderPath);

        Set<ProcessedDataFile> filesRead = EnumSet.noneOf(ProcessedDataFile.class);
        Set<ProcessedDataFile> filesToRead = EnumSet.allOf(ProcessedDataFile.class);

        if (directory.exists() && directory.isDirectory()) {
            // check for necessary files
            try {
                for (File current : directory.listFiles()) {
                    if (!current.isDirectory()) {
                        Optional<ProcessedDataFile> optConstant = ProcessedDataFile.findConstantByFile(ctx, current);
                        if (optConstant.isPresent()) {
                            //System.out.print(" reading `" + current.getAbsolutePath() + "' ...");
                            ProcessedDataFile constant = optConstant.get();
                            constant.load(ctx, current);
                            filesRead.add(constant);
                            filesToRead.remove(constant);
                        } else {
                            //System.out.print(" ignoring `" + current.getAbsolutePath() + "' ...");
                        }
                    }
                }
                System.out.println(" done.");
            } catch (Exception e) {
                throw new RuntimeException("Error loading processed data from " + directory, e);
            }
        } else {
            throw new RuntimeException("File does not exist or is not a directory `" + directory + "'");
        }

        if (!filesToRead.isEmpty()) {
            dieDueToMissingFilesToLoad(filesRead, filesToRead);
            // We never get here because the method above is supposed to throw an exception.
            return;
        }
    }

    private void dieDueToMissingFilesToLoad(Set<ProcessedDataFile> filesRead, Set<ProcessedDataFile> filesToRead) {
        StringBuilder present = new StringBuilder();
        StringBuilder missing = new StringBuilder();
        for (ProcessedDataFile c : filesRead) {
            if (present.length() > 0) {
                present.append(", ");
            }
            present.append(c.filename(ctx));
        }
        for (ProcessedDataFile c : filesToRead) {
            if (missing.length() > 0) {
                missing.append(", ");
            }
            missing.append(c.filename(ctx));
        }
        throw new RuntimeException("Not all processed data could be read. Read: " + present + ". Missing: " + missing);
    }
}
