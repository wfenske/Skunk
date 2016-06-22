package main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import data.FeatureConstant;
import data.FeatureExpressionCollection;
import data.FileCollection;
import data.MethodCollection;
import detection.DetectionConfig;
import detection.Detector;
import detection.EnumReason;
import input.CppStatsFolderReader;
import input.SrcMlFolderReader;
import output.AnalyzedDataHandler;
import output.ProcessedDataHandler;

/**
 * Skunk main class
 * 
 * @author wfenske
 *
 */
public class Program {

	/** The code smell configuration. */
	private static DetectionConfig conf = null;

	/** The path of the source folder. */
	private static String sourcePath = "";

	/** A flag that defines, if intermediate formats will be saved. */
	public static boolean saveIntermediate = false;

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		// Initialize Components
		FeatureExpressionCollection.Initialize();
		MethodCollection.Initialize();
		FileCollection.Initialize();

		// gather input
		try {
			parseCommandLineArgs(args);
		} catch (UsageError ue) {
			System.err.println(ue);
			System.exit(1);
		} catch (Exception e) {
			System.err.println("Error while processing command line arguments: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		if (!sourcePath.isEmpty()) {
			// process necessary csv files in project folder
			CppStatsFolderReader cppReader = new CppStatsFolderReader(sourcePath);
			cppReader.ProcessFiles();

			// process srcML files
			SrcMlFolderReader mlReader = new SrcMlFolderReader();
			mlReader.ProcessFiles();

			// do post actions
			MethodCollection.PostAction();
			FileCollection.PostAction();

			// save processed data
			if (saveIntermediate)
				ProcessedDataHandler.SaveProcessedData();
		}

		// display loc, loac, #feat, NOFL and NOFC
		System.out.println(FeatureExpressionCollection.GetLoc() + " Loc");
		System.out.println(FeatureExpressionCollection.GetCount() + " Features");
		System.out.println(FeatureExpressionCollection.numberOfFeatureConstants + " Feature Constants");

		int loac = 0;
		int nofl = 0;
		for (data.File f : FileCollection.Files) {
			loac += f.GetLinesOfAnnotatedCode();
			nofl += f.numberOfFeatureLocations;
		}

		System.out.println(loac + " Loac: " + (loac * 100) / FeatureExpressionCollection.GetLoc());
		System.out.println(nofl + " nofl");

		// run detection with current configuration
		if (conf != null) {
			Detector detector = new Detector(conf);
			HashMap<FeatureConstant, ArrayList<EnumReason>> res = (HashMap<FeatureConstant, ArrayList<EnumReason>>) detector
					.Perform();

			AnalyzedDataHandler presenter = new AnalyzedDataHandler(conf);
			presenter.SaveTextResults(res);
			presenter.SaveCsvResults();
		}
	}

	/**
	 * Analyze input to decide what to do during runtime
	 *
	 * @param args
	 *            the input arguments
	 * @return true, if input is correct
	 */
	private static void parseCommandLineArgs(String[] args) {
		boolean haveConfig = false;
		boolean haveInput = false;

		// for easier handling, transform to list
		int i = 0;
		while (i < args.length) {
			final String optName = args[i++];

			if (optName.equals("--")) {
				if (i == args.length)
					continue;
				else {
					// We don't expect any extra parameters
					String[] extraParameters = Arrays.copyOfRange(args, i, args.length);
					throw new UsageError(
							"Did not expect any positional arguments, got " + Arrays.toString(extraParameters));
				}
			}

			if (!optName.startsWith("--")) {
				throw new UnrecognizedOption(optName);
			}

			final boolean haveMoreArgs = i < args.length;

			// get the path to the code smell configuration
			if (optName.equals("--config")) {
				if (!haveMoreArgs) {
					throw new MissingOptionValue(optName);
				}

				String configPath = args[i++];
				File f = new File(configPath);

				if (f.exists() && !f.isDirectory()) {
					try {
						conf = new DetectionConfig(configPath);
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
							| IllegalAccessException | IOException e) {
						throw new RuntimeException("Error opening smell configuration file " + configPath, e);
					}
					haveConfig = true;
				} else {
					throw new UsageError(
							"The configuration file, " + configPath + ", does not exist or is a directory.");
				}
				continue;
			}
			else if (optName.equals("--saveintermediate")) {
				saveIntermediate = true;
				continue;
			}
			// get the path of the source folder
			else if (optName.equals("--source")) {
				if (!haveMoreArgs) {
					throw new MissingOptionValue(optName);
				}

				String path = args[i++];

				try {
					File f = new File(path);

					if (f.exists() && f.isDirectory()) {
						sourcePath = path;
					} else {
						throw new UsageError("The source path, " + path + ", does not exist or is not a directory.");
					}
				} catch (Exception e) {
					throw new RuntimeException("Error reading source path, " + path, e);
				}

				haveInput = true;
				continue;
			}

			// read previously processed data
			else if (optName.equals("--processed")) {
				if (!haveMoreArgs)
					throw new MissingOptionValue(optName);

				String path = args[i++];
				ProcessedDataHandler.LoadProcessedData(path);
				haveInput = true;
				continue;
			} else {
				throw new UnrecognizedOption(optName);
			}
		}

		if (!haveInput) {
			throw new UsageError(
					"Either need to set a source folder (--source) or a processed data folder (--processed)!");
		}
	}

}

class UsageError extends RuntimeException {
	public UsageError(String message) {
		super("Usage error: " + message);
	}
}

class MissingOptionValue extends UsageError {
	public MissingOptionValue(String optionName) {
		super("Option `" + optionName + "' requires a value (none given).");
	}
}

class UnrecognizedOption extends UsageError {
	public UnrecognizedOption(String optionName) {
		super("Unrecognized option `" + optionName + "'");
	}
}