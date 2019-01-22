package de.ovgu.skunk.detection.main;

import de.ovgu.skunk.detection.data.Context;
import de.ovgu.skunk.detection.data.FeatureReference;
import de.ovgu.skunk.detection.detector.DetectionConfig;
import de.ovgu.skunk.detection.detector.Detector;
import de.ovgu.skunk.detection.detector.SmellReason;
import de.ovgu.skunk.detection.input.CppStatsFolderReader;
import de.ovgu.skunk.detection.input.SrcMlFolderReader;
import de.ovgu.skunk.detection.output.AnalyzedDataHandler;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skunk main class
 *
 * @author wfenske
 */
public class Skunk {
    private static final char OPT_HELP = 'h';
    private static final char OPT_SAVE_INTERMEDIATE = 'm';
    private static final char OPT_SOURCE = 's';
    private static final char OPT_PROCESSED = 'p';
    private static final char OPT_CONFIG = 'c';
    /**
     * The code smell configuration.
     */
    private DetectionConfig conf = null;

    private Context ctx = null;

    /**
     * The path of the source folder.  Mutually exclusive with {@link #processedDataDir}.
     */
    private Optional<String> sourcePath = Optional.empty();

    /**
     * Directory from which preprocessed data will be loaded.  Mutually exclusive with {@link #sourcePath}.
     */
    private Optional<String> processedDataDir = Optional.empty();
    /**
     * A flag that defines if intermediate formats will be saved.
     */
    private boolean saveIntermediate = false;

    /**
     * The main method.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Skunk().run(args);
    }

    private void run(String[] args) {
        String cwd;
        try {
            cwd = new java.io.File(".").getCanonicalPath();
        } catch (IOException e1) {
            cwd = System.getProperty("user.dir");
        }
        System.out.println("Starting Skunk in  `" + cwd + "'.");
        try {
            parseCommandLineArgs(args);
        } catch (UsageError ue) {
            System.err.flush();
            System.out.flush();
            System.err.println(ue.getMessage());
            System.err.flush();
            System.out.flush();
            System.exit(1);
        } catch (Exception e) {
            System.err.flush();
            System.out.flush();
            System.err.println("Error while processing command line arguments: " + e);
            e.printStackTrace();
            System.err.flush();
            System.out.flush();
            System.exit(1);
        }

        ctx = new Context(conf);

        if (sourcePath.isPresent()) {
            // process necessary csv files in project folder
            CppStatsFolderReader cppReader = new CppStatsFolderReader(ctx, sourcePath.get());
            cppReader.ProcessFiles();
            // process srcML files
            SrcMlFolderReader mlReader = new SrcMlFolderReader(ctx);
            mlReader.ProcessFiles();
            // do post actions
            ctx.functions.PostAction();
            ctx.files.PostAction();
            // save processed data
            if (saveIntermediate) ctx.processedDataHandler.SaveProcessedData();
        } else if (processedDataDir.isPresent()) {
            ctx.processedDataHandler.LoadProcessedData(processedDataDir.get());
        } else {
            throw new IllegalStateException("Exactly one of --sourcePath or --processedData must be specified!");
        }

        // display loc, loac, #feat, NOFL and NOFC
        System.out.println();
        System.out.println("LOC: " + ctx.featureExpressions.GetLoc());
        System.out.println("Number of features: " + ctx.featureExpressions.GetCount());
        System.out.println("Number of feature constant references: "
                + ctx.featureExpressions.numberOfFeatureConstantReferences);
        int loac = 0;
        int nofl = 0;
        for (de.ovgu.skunk.detection.data.File f : ctx.files.AllFiles()) {
            loac += f.GetLinesOfAnnotatedCode();
            nofl += f.numberOfFeatureLocations;
        }
        System.out.printf("LOAC: %d (%.0f%% of all lines of code)\n", loac,
                (loac * 100.0) / ctx.featureExpressions.GetLoc());
        System.out.println("NOFL: " + nofl);
        // run detection with current configuration (if present)
        if (conf != null) {
            Detector detector = new Detector(ctx);
            Map<FeatureReference, List<SmellReason>> res = detector.Perform();
            AnalyzedDataHandler presenter = new AnalyzedDataHandler(ctx);
            presenter.SaveTextResults(res);
            presenter.SaveCsvResults();
        }
        System.out.println("Exiting Skunk.");
    }

    /**
     * Analyze input to decide what to do during runtime
     *
     * @param args the input arguments
     */
    private void parseCommandLineArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options fakeOptionsForHelp = makeOptions(true);
        Options actualOptions = makeOptions(false);
        CommandLine line;
        try {
            CommandLine dummyLine = parser.parse(fakeOptionsForHelp, args);
            if (dummyLine.hasOption('h')) {
                HelpFormatter formatter = new HelpFormatter();
                System.err.flush();
                formatter.printHelp(progName() + " [OPTIONS]", actualOptions);
                System.out.flush();
                System.exit(0);
                return;
            }
            line = parser.parse(actualOptions, args);
        } catch (ParseException e) {
            System.err.println("Error in command line: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printUsage(new PrintWriter(System.err, true), 80, progName(), actualOptions);
            System.exit(1);
            return;
        }
        // --config=... get the path to the code smell configuration
        if (line.hasOption(OPT_CONFIG)) {
            String configPath = line.getOptionValue('c');
            File fConfig = new File(configPath);
            if (fConfig.exists() && !fConfig.isDirectory()) {
                try {
                    conf = new DetectionConfig(configPath);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
                        | IOException e) {
                    throw new RuntimeException("Error opening smell configuration file " + configPath, e);
                }
            } else {
                throw new UsageError("The configuration file, " + configPath + ", does not exist or is a directory.");
            }
        }
        // Get the input (--source= or --processed= option)
        if (line.hasOption(OPT_SOURCE)) {
            String path = line.getOptionValue(OPT_SOURCE);
            try {
                File fSource = new File(path);
                if (fSource.exists() && fSource.isDirectory()) {
                    sourcePath = Optional.of(path);
                } else {
                    throw new UsageError("The source path, " + path + ", does not exist or is not a directory.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Error reading source path, " + path, e);
            }
        } else if (line.hasOption(OPT_PROCESSED)) {
            this.processedDataDir = Optional.of(line.getOptionValue(OPT_PROCESSED));
        } else {
            throw new UsageError(
                    "Either need to set a source folder (--source=DIR) or a processed data folder (--processed=DIR)!");
        }
        // --save-intermediate
        if (line.hasOption(OPT_SAVE_INTERMEDIATE)) {
            saveIntermediate = true;
            if (!this.sourcePath.isPresent()) {
                System.err.println("Save intermdiate was requested (option `-" + OPT_SAVE_INTERMEDIATE
                        + "'), but no source path has been specified (option `-" + OPT_SOURCE
                        + "). Intermediates will NOT be saved.");
            }
        }
    }

    private Options makeOptions(boolean forHelp) {
        boolean required = !forHelp;
        Options options = new Options();
        //@formatter:off
        // --help= option
        options.addOption(Option.builder(String.valueOf(OPT_HELP))
                .longOpt("help")
                .desc("print this help sceen and exit")
                .build());
        // --config= option
        options.addOption(Option.builder(String.valueOf(OPT_CONFIG))
                .longOpt("config")
                .desc("code smell detection configuration")
                .hasArg()
                .argName("FILE")
                .type(PatternOptionBuilder.EXISTING_FILE_VALUE)
                .build());
        // --save-intermediate flag
        options.addOption(Option.builder(String.valueOf(OPT_SAVE_INTERMEDIATE))
                .longOpt("save-intermediate")
                .desc("save intermediate analysis results to speed up future detection runs")
                .build());

        // --source= and --processed= options
        OptionGroup inputOptions = new OptionGroup();
        inputOptions.setRequired(required);

        inputOptions.addOption(Option.builder(String.valueOf(OPT_SOURCE))
                .longOpt("source")
                .desc("path to source directory")
                .hasArg()
                .argName("DIR")
                .build());
        inputOptions.addOption(Option.builder(String.valueOf(OPT_PROCESSED))
                .longOpt("processed")
                .desc("read preprocessed data saved during a previous run")
                .hasArg()
                .argName("DIR")
                .build());

        options.addOptionGroup(inputOptions);

        //@formatter:on
        return options;
    }

    private String progName() {
        return this.getClass().getSimpleName();
    }
}

class UsageError extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public UsageError(String message) {
        super("Usage error: " + message);
    }
}
