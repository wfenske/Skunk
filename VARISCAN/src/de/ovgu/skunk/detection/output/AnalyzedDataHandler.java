package de.ovgu.skunk.detection.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import de.ovgu.skunk.detection.data.Feature;
import de.ovgu.skunk.detection.data.FeatureExpressionCollection;
import de.ovgu.skunk.detection.data.FeatureReference;
import de.ovgu.skunk.detection.data.FileCollection;
import de.ovgu.skunk.detection.data.Method;
import de.ovgu.skunk.detection.data.MethodCollection;
import de.ovgu.skunk.detection.detector.DetectionConfig;
import de.ovgu.skunk.detection.detector.SmellReason;

public class AnalyzedDataHandler {

	private DetectionConfig conf = null;
	private String currentDate = "";
	
    static abstract class CsvWriterHelper {
        public void write(String fileName) {
            FileWriter writer = null;
            CSVPrinter csv = null;

            try {
                writer = new FileWriter(fileName);

                try {
                    csv = new CSVPrinter(writer, CSVFormat.EXCEL);
                    actuallyDoStuff(csv);
                } catch (Exception e) {
                    throw new RuntimeException("Error writing CSV file `" + fileName + "'", e);
                } finally {
                    try {
                        if (csv != null)
                            csv.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Error closing CSV printer for file `" + fileName + "'", e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error writing CSV file `" + fileName + "'", e);
            } finally {
                try {
                    if (writer != null) {
                        writer.flush();
                        writer.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error flushing/closing file writer for CSV file `" + fileName + "'", e);
                }
            }
        }

        protected abstract void actuallyDoStuff(CSVPrinter csv) throws IOException;
    }

	/** A comparator that compares featurenames of feature constants. */
	public final static Comparator<FeatureReference> FEATURECONSTANT_FEATURENAME_COMPARATOR = new Comparator<FeatureReference>()
	{
		@Override public int compare(FeatureReference f1, FeatureReference f2)
		{
			return f1.feature.Name.compareTo(f2.feature.Name);
		}
	};
	
	/** A comparator that compares the filepath of feature constant*/
	public final static Comparator<FeatureReference> FEATURECONSTANT_FILEPATH_COMPARATOR = new Comparator<FeatureReference>()
	{
		@Override public int compare(FeatureReference f1, FeatureReference f2)
		{
			return f1.filePath.compareTo(f2.filePath);
		}
	};
	
	/** A comparator that compares startposition of feature constants. */
	public final static Comparator<FeatureReference> FEATURECONSTANT_START_COMPARATOR = new Comparator<FeatureReference>()
	{
		@Override public int compare(FeatureReference f1, FeatureReference f2)
		{	
			return Integer.compare(f1.start,f2.start);
		}
	};
	
	/** A comparator that compares startposition of feature constants methods. */
	public final static Comparator<FeatureReference>  FEATURECONSTANT_METHOD_COMPARATOR = new Comparator<FeatureReference>()
	{
		@Override public int compare(FeatureReference f1, FeatureReference f2)
		{
			if (f1.inMethod == null)
			{
				if (f2.inMethod == null)
					return 0;
				return -1;
			}
			if (f2.inMethod == null)
				return 1;
			
			int s1 = f1.inMethod.start;
			int s2 = f2.inMethod.start;
			
			return Integer.compare(s1, s2);
		}
	};
	
	/** The comparator that compares the smell value of the csv records. */
	public final static Comparator<Object[]> ABSmellComparator = new Comparator<Object[]>()
	{
		@Override public int compare(Object[] f1, Object[] f2)
		{
			float s1 = (float) f1[3];
			float s2 = (float) f2[3];
			
			return Float.compare(s2, s1);
		}
	};
	
	/** The comparator that compares the smell value of the csv records. */
	public final static Comparator<Object[]> AFSmellComparator = new Comparator<Object[]>()
	{
		@Override public int compare(Object[] f1, Object[] f2)
		{
			float s1 = (float) f1[1];
			float s2 = (float) f2[1];
			
			return Float.compare(s2, s1);
		}
	};
	
	/** The comparator that compares the smell value of the csv records. */
	public final static Comparator<Object[]> LGSmellComparator = new Comparator<Object[]>()
	{
		@Override public int compare(Object[] f1, Object[] f2)
		{
			float s1 = (float) f1[1];
			float s2 = (float) f2[1];
			
			return Float.compare(s2, s1);
		}
	};
	
	
	/**
	 * 	 Instantiates a new presenter.
	 *
	 * @param conf the detection configuration
	 */
	public AnalyzedDataHandler(DetectionConfig conf)
	{
		this.conf = conf;
	}
	
	
	
	
	
	
	
	/**** TXT Start End Saving *****/
	
    public void SaveTextResults(Map<FeatureReference, List<SmellReason>> results)
	{			
		// get the results of the complete detection process and the whole project
		String overview = this.getOverviewResults(results);
		
		// get overview per attribute
		String attributes = this.getAttributeOverviewResults(results);
		
		// Sortiert nach Location und file
		String files = this.getFileSortedRestults(results);
		
		String methods = this.getMethodSortedResults(results);
		
		// get the results sorted per feature
		String features = this.getFeatureSortedResults(results);
		
		currentDate = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
        String fileNamePrefix = currentDate + "_detection_";

        SimpleFileWriter writer = new SimpleFileWriter();
		
		try 
		{
            writer.write(new File(fileNamePrefix + "overview.txt"), overview);
            writer.write(new File(fileNamePrefix + "attributes.txt"), attributes);
            writer.write(new File(fileNamePrefix + "files.txt"), files);
            writer.write(new File(fileNamePrefix + "methods.txt"), methods);
            writer.write(new File(fileNamePrefix + "features.txt"), features);

            System.out.println("Detection result files (" + writer.prettyFileNameList() + ") saved to directory `"
                    + writer.getDir() + "'");
		} catch (IOException e) {
            throw new RuntimeException("I/O error while saving detection results as text.", e);
		}
	}

	/**
	 * Creates the overview metrics for each attribute, and saves it to the output result
	 *
	 * @param results the results
	 * @return the attribute overview results
	 */
    private String getAttributeOverviewResults(Map<FeatureReference, List<SmellReason>> results)
	{
		String res = conf.toString() +"\r\n\r\n\r\n\r\n\r\n";

        List<AttributeOverview> attributes = new ArrayList<AttributeOverview>();
		
		for (FeatureReference key : results.keySet())
		{
			for (SmellReason reason : results.get(key))
			{
				// get fitting attribute or create one
				boolean add = true;
				for (AttributeOverview overview : attributes)
				{
					if (overview.Reason.equals(reason))
						add = false;
				}
				if (add)
					attributes.add(new AttributeOverview(reason));
				
				// add location information
				for (AttributeOverview overview : attributes)
					if (overview.Reason.equals(reason))
						overview.AddFeatureLocationInfo(key);
			}
		}
		
		// add attribute overview to output
		for (AttributeOverview attr : attributes)
			res += attr.toString();
		
		return res;
	}
	
	/**
	 * Sorts the result per file and start and adds it to the resulting file
	 *
	 * @param results the results
	 * @return the location results
	 */
    private String getFileSortedRestults(Map<FeatureReference, List<SmellReason>> results)
	{
		String res = conf.toString() + "\r\n\r\n\r\n\r\n\r\n\r\n";
		
		// sort the keys after featurename, filepath and start
        List<FeatureReference> sortedKeys = new ArrayList<FeatureReference>(results.keySet());
		Collections.sort(sortedKeys, new ComparatorChain<FeatureReference>(FEATURECONSTANT_FILEPATH_COMPARATOR, FEATURECONSTANT_START_COMPARATOR));
		
		res += ">>> File-Sorted Results:\r\n";
		
		String currentPath = "";
		
		// print the the locations and reasons sorted after feature
		for (FeatureReference key : sortedKeys)
		{		
			if (!key.filePath.equals(currentPath))
			{
				currentPath = key.filePath;
				res += "\r\n\r\n\r\n[File: " + currentPath + "]\r\n";
				res += "Start\t\tEnd\t\tFeature\t\tReason\r\n";
			}
			
			res += key.start + "\t\t" + key.end + "\t\t" + key.feature.Name + "\t\t"+results.get(key).toString() + "\r\n";
		}
		
		return res;
	}
	
	/**
	 * Sorts the results per feature, and presents the locations and reason for each corresponding feature
	 *
	 * @param results the detection results
	 * @return the results per feature
	 */
    private String getFeatureSortedResults(Map<FeatureReference, List<SmellReason>> results)
	{
		String res = conf.toString() + "\r\n\r\n\r\n\r\n\r\n";
		
		// sort the keys after featurename, filepath and start
        List<FeatureReference> sortedKeys = new ArrayList<FeatureReference>(results.keySet());
		Collections.sort(sortedKeys, new ComparatorChain<FeatureReference>(FEATURECONSTANT_FEATURENAME_COMPARATOR, FEATURECONSTANT_FILEPATH_COMPARATOR, FEATURECONSTANT_START_COMPARATOR));
		
		res += ">>> Feature-Sorted Results";
		
		String currentName = "";
		String currentPath = "";
		
		// print the the locations and reasons sorted after feature
		for (FeatureReference key : sortedKeys)
		{
			if (!key.feature.Name.equals(currentName))
			{
				currentName = key.feature.Name;
				res += "\r\n\r\n\r\n[Feature: " + currentName + "]\r\n"; 
				
				// reset filepath
				currentPath = "";
			}
			
			if (!key.filePath.equals(currentPath))
			{
				currentPath = key.filePath;
				res += "File: " + currentPath + "\r\n";
				res += "Start\t\tEnd\t\tReason\r\n";
			}
			
			
			res += key.start + "\t\t" + key.end + "\t\t" + results.get(key).toString() + "\r\n";
		}
		
		return res;
	}
	
	/**
	 * Sorts the results per Method and returns it in a string per file/method/cnstant
	 *
	 * @param results the detection results
	 * @return the results per feature
	 */
    private String getMethodSortedResults(Map<FeatureReference, List<SmellReason>> results)
	{
		String res = conf.toString() + "\r\n\r\n\r\n\r\n\r\n";
        List<FeatureReference> sortedKeys = new ArrayList<FeatureReference>(results.keySet());
		Collections.sort(sortedKeys, new ComparatorChain<FeatureReference>(FEATURECONSTANT_FILEPATH_COMPARATOR, FEATURECONSTANT_METHOD_COMPARATOR, FEATURECONSTANT_START_COMPARATOR));
		
		res += ">>> Method-Sorted Results";
		
		Method currentMethod = null;
		String currentPath = "";
		
		// print feature constants with reason per File and Method
		for (FeatureReference key : sortedKeys)
		{
			// don't display feature that are not in a method
			if (key.inMethod == null)
				continue;
						
			if (!key.filePath.equals(currentPath))
			{
				currentPath = key.filePath;
				res += "\r\n\r\nFile: " + currentPath;
			}
			
			if (!key.inMethod.equals(currentMethod))
			{
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
     * @param results
     *            the result hash map from the detection process
     */
    private String getOverviewResults(Map<FeatureReference, List<SmellReason>> results) 
	{
 		String res = conf.toString();
		// amount of feature constants
        List<String> constants = new ArrayList<String>();
		float percentOfConstants = 0;
		
		// amount of feature constants
		int countLocations = results.entrySet().size();
		float percentOfLocations = 0;
		
		// lofcs in project
		int completeLofc = 0;
		
		// loac in project
        HashMap<String, List<Integer>> loacs = new HashMap<>();
		int completeLoac = 0;
		float percentOfLoc = 0;
		
		for (FeatureReference constant : results.keySet())
		{
			// get the amount of feature constants by saving each feature constant name
			if (!constants.contains(constant.feature.Name))
				constants.add(constant.feature.Name);
			
			// add lines of code to result
			completeLofc += constant.end-constant.start;
			
			// add all lines per file to the data structure, that are part of the feature constant... no doubling for loac calculation
			if (!loacs.keySet().contains(constant.filePath))
				loacs.put(constant.filePath, new ArrayList<Integer>());
			
			for (int i = constant.start; i <= constant.end; i++)
			{
				if (!loacs.get(constant.filePath).contains(i))
					loacs.get(constant.filePath).add(i);
			}
		}
		
		// calculate max loac
		for (String file : loacs.keySet())
			completeLoac += loacs.get(file).size();
		
		// calculate percentages
		percentOfLoc = completeLoac * 100 / FeatureExpressionCollection.GetLoc();
		percentOfLocations = countLocations * 100 / FeatureExpressionCollection.numberOfFeatureConstantReferences;
		percentOfConstants = constants.size() * 100 / FeatureExpressionCollection.GetFeatures().size();
		
		// Complete overview
		res += "\r\n\r\n\r\n>>> Complete Overview\r\n";
		res += "Number of features: \t" + constants.size() + " (" + percentOfConstants + "% of " + FeatureExpressionCollection.GetFeatures().size() + " constants)\r\n";
		res += "Number of feature constants: \t" + countLocations  + " (" + percentOfLocations + "% of " + FeatureExpressionCollection.numberOfFeatureConstantReferences + " locations)\r\n";
		res += "Lines of annotated Code: \t" + completeLoac + " (" + percentOfLoc + "% of " + FeatureExpressionCollection.GetLoc() + " LOC)\r\n";
		res += "Lines of feature code: \t\t" + completeLofc + "\r\n";
		
		res += "Mean LOFC per feature: \t\t" + FeatureExpressionCollection.GetMeanLofc() + "\r\n\r\n\r\n";
		
		return res;
	}
 	
 	/**** TXT Start End Saving *****/
 	
 	
 	
 	
 	
 	
 	
 	
 	
 	
 	/**** CSV Smell Value Saving ****/
 	
 	public void SaveCsvResults()
 	{
 		// ensure consistent filenaming
 		if (this.currentDate.equals(""))
 			currentDate = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
        String fileNamePrefix = this.currentDate + "_metrics_";
 		
        String fnMethods = fileNamePrefix + "methods.csv";
        String fnFeatures = fileNamePrefix + "features.csv";
        String fnFiles = fileNamePrefix + "files.csv";

        String dirName;
        try {
            dirName = (new File(fnMethods)).getCanonicalFile().getParent();
        } catch (IOException e) {
            throw new RuntimeException("I/O error writing CSV results", e);
        }

        this.createMethodCSV(fnMethods);
        this.createFeatureCSV(fnFeatures);
        this.createFileCSV(fnFiles);
 		
        System.out.printf("Metric files (%s, %s, %s) saved in directory %s\n", fnFeatures, fnFiles, fnMethods, dirName);
 	}

 	/**
	  * Creates the file metric csv.
	  *
	  * @param fileName the file name
	  * @param writer the writer
	  * @param csv the csv printer
	  */
    private void createFileCSV(String fileName) {
        CsvWriterHelper h = new CsvWriterHelper() {
            @Override
            protected void actuallyDoStuff(CSVPrinter csv) throws IOException {
                // add the header for the CSV file
                Object[] FileHeader = { "File", "AFSmell", "LocationSmell", "ConstantsSmell", "NestingSmell", "LOC",
                        "LOAC", "LOFC", "NOFC_Dup", "NOFC_NonDup", "NOFL", "NONEST" };
                csv.printRecord(FileHeader);

                // calculate values and add records
                List<Object[]> fileData = new ArrayList<Object[]>();
                for (de.ovgu.skunk.detection.data.File file : FileCollection.Files) {
                    if (skipFile(file))
                        continue;

                    fileData.add(createFileRecord(file));
                }

                // sort by smell value
                Collections.sort(fileData, new ComparatorChain<Object[]>(AFSmellComparator));

                for (Object[] record : fileData)
                    csv.printRecord(record);
            }
        };
        h.write(fileName);
 	}
 	
 	/**
	  * Creates the file csv record
	  *
	  * @param file the file
	  * @return the object[] a csv record as array
	  */
	private Object[] createFileRecord(de.ovgu.skunk.detection.data.File file)
 	{
		// calculate smell values
		// Loac/Loc * #FeatLocs
		float featLocSmell = conf.File_LoacToLocRatio_Weight * (((float) file.GetLinesOfAnnotatedCode() / (float) file.loc) * file.numberOfFeatureLocations);
		
		// #Constants/#FeatLocs
		float featConstSmell = conf.File_NumberOfFeatureConstants_Weight * ((float) file.GetFeatureConstantCount() / (float) file.numberOfFeatureLocations);
		
		// Loac/Loc * #FeatLocs
		float nestSumSmell = conf.Method_NestingSum_Weight * ((float) file.nestingSum / (float) file.numberOfFeatureLocations);
		float sum = (featLocSmell + featConstSmell + nestSumSmell);
		
 		Object[] result = new Object[12];
 		result[0] = file.filePath;
 		result[1] = sum;
 		result[2] = featLocSmell;
 		result[3] = featConstSmell;
 		result[4] = nestSumSmell;
 		result[5] = file.loc;
 		result[6] = file.GetLinesOfAnnotatedCode();
 		result[7] = file.lofc;
 		result[8] = file.GetFeatureConstantCount();
 		result[9] = file.numberFeatureConstantsNonDup;
 		result[10] = file.numberOfFeatureLocations;
 		result[11] = file.nestingSum;
 		
 		return result;
 	}
 	
 	/**
	 * Creates the method csv.
	 *
	 * @param writer the writer
	 * @param csv the csv
	 */
    private void createFeatureCSV(String fileName) {
        CsvWriterHelper h = new CsvWriterHelper() {
            @Override
            protected void actuallyDoStuff(CSVPrinter csv) throws IOException {
                // TODO Wieviele NOFC in Kombination
                // add the header for the csv file
                Object[] FeatureHeader = { "Name", "LGSmell", "SSSmell ", "ConstantsSmell", "LOFCSmell", "CUSmell",
                        "NOFC", "MAXNOFC", "LOFC", "ProjectLOC", "NOCU" };
                csv.printRecord(FeatureHeader);

                // calculate values and add records
                List<Object[]> featureData = new ArrayList<Object[]>();
                for (Feature feat : FeatureExpressionCollection.GetFeatures()) {
                    if (skipFeature(feat))
                        continue;

                    featureData.add(createFeatureRecord(feat));
                }

                // sort after smellvalue
                Collections.sort(featureData, new ComparatorChain<Object[]>(LGSmellComparator));

                for (Object[] record : featureData)
                    csv.printRecord(record);
            }
        };
        h.write(fileName);
	}
 	
 	/**
	  * Creates the feature csv record.
	  *
	  * @param feat the feat
	  * @return the object[]
	  */
	 private Object[] createFeatureRecord(Feature feat)
 	{
 		// # featureConstants/#TotalLocations
 		float constSmell = this.conf.Feature_NumberNofc_Weight * (((float) feat.getConstants().size()) / ((float) FeatureExpressionCollection.numberOfFeatureConstantReferences));
 		
 		// LOFC/TotalLoc   																				
 		float lofcSmell = this.conf.Feature_NumberLofc_Weight * (((float)feat.getLofc()) / ((float) FeatureExpressionCollection.GetLoc()));
 		
 		// CompilUnit/MaxCompilUnits
 		float compilUnitsSmell = ((float) feat.compilationFiles.size()) / ((float) FileCollection.Files.size());
 		
 		float sumLG = constSmell + lofcSmell;
 		float sumSS = constSmell + compilUnitsSmell;
 		
 		Object[] result = new Object[11];
 		result[0] = feat.Name;
 		result[1] = sumLG;
 		result[2] = sumSS;
 		result[3] = constSmell;
 		result[4] = lofcSmell;
 		result[5] = compilUnitsSmell;
 		
 		result[6] = feat.getConstants().size();
 		result[7] = FeatureExpressionCollection.numberOfFeatureConstantReferences;
 		result[8] = feat.getLofc();
 		result[9] = FeatureExpressionCollection.GetLoc();
 		result[10] = feat.compilationFiles.size();
 		
 		return result;
 	}
	
	/**
	 * Creates the method csv.
	 *
	 * @param writer the writer
	 * @param csv the csv
	 */
    private void createMethodCSV(String fileName) {
        CsvWriterHelper h = new CsvWriterHelper() {
            @Override
            protected void actuallyDoStuff(CSVPrinter csv) throws IOException {
                // add the header for the csv file
                Object[] MethodHeader = { "File", "Start", "Method", "ABSmell", "LocationSmell", "ConstantsSmell",
                        "NestingSmell", "LOC", "LOAC", "LOFC", "NOFL", "NOFC_Dup", "NOFC_NonDup", "NONEST" };
                csv.printRecord(MethodHeader);

                // calculate values and add records
                List<Object[]> methodData = new ArrayList<Object[]>();
                for (List<Method> methods : MethodCollection.methodsPerFile.values()) {
                    for (Method meth : methods) {
                        if (skipMethod(meth))
                            continue;

                        methodData.add(createMethodRecord(meth));
                    }
                }

                // sort by smell value
                Collections.sort(methodData, new ComparatorChain<Object[]>(ABSmellComparator));

                for (Object[] record : methodData)
                    csv.printRecord(record);
            }
        };
        h.write(fileName);
	}
 	
 	
 	/**
	  * GCalculate the smell value of the current method and return data record as list
	  *
	  * @param currentMethod the current method
	  * @return the list the data record for the csv file
	  */
	private Object[] createMethodRecord(Method currentMethod) 
	{
		// calculate smell values
		// Loac/Loc * #FeatLocs
		float featLocSmell = conf.Method_LoacToLocRatio_Weight * (((float) currentMethod.GetLinesOfAnnotatedCode() / (float) currentMethod.loc) * currentMethod.numberFeatureLocations);
		
		// #Constants/#FeatLocs
		float featConstSmell = conf.Method_NumberOfFeatureConstants_Weight * ((float) currentMethod.GetFeatureConstantCount() / (float) currentMethod.numberFeatureLocations);
		
		// Loac/Loc * #FeatLocs
		float nestSumSmell = conf.Method_NestingSum_Weight * ((float) currentMethod.nestingSum / (float) currentMethod.numberFeatureLocations);
		float sum = (featLocSmell + featConstSmell + nestSumSmell);
	
		Object[] result = new Object[14];
		
		// File Start Method
		result[0] = currentMethod.filePath;
		result[1] = currentMethod.start;
		result[2] = currentMethod.functionSignatureXml;
		
		// SmellValue, LocationSmell, ConstantSmell, NestingSmell
		result[3] = sum;
		result[4] = featLocSmell;
		result[5] = featConstSmell;
		result[6] = nestSumSmell;
		
		// Loc Loac Lofc NoLocs NoConst NoNestings
		result[7] = currentMethod.loc;
		result[8] = currentMethod.GetLinesOfAnnotatedCode();
		result[9] = currentMethod.lofc;
		result[10] = currentMethod.numberFeatureLocations;
		result[11] = currentMethod.GetFeatureConstantCount();
		result[12] = currentMethod.numberFeatureConstantsNonDup;
		result[13] = currentMethod.nestingSum;
		
		return result;
	}
	
	/**
	 * Skip the method for csv file creation depending on the mandatory settings of the configuration
	 *
	 * @param method the method
	 * @return true, if method does not fulfill mandatory settings
	 */
	private boolean skipMethod(Method method)
	{
		boolean result = false;
	
		if (conf.Method_LoacToLocRatio_Mand && ((float) method.GetLinesOfAnnotatedCode() / (float) method.loc) < conf.Method_LoacToLocRatio)
			result = true;
		if (conf.Method_NumberOfFeatureConstants_Mand && method.GetFeatureConstantCount() < conf.Method_NumberOfFeatureConstants)
			result = true;
		if (conf.Method_NestingSum_Mand && method.nestingSum < conf.Method_NestingSum)
			result = true;
		
		return result;
	}
	
	/**
	 * Skip the method for csv file creation depending on the mandatory settings of the configuration
	 *
	 * @param method the method
	 * @return true, if method does not fulfill mandatory settings
	 */
	private boolean skipFile(de.ovgu.skunk.detection.data.File file)
	{
		boolean result = false;
	
		if (conf.File_LoacToLocRatio_Mand && ((float) file.GetLinesOfAnnotatedCode() / (float) file.loc) < conf.File_LoacToLocRatio)
			result = true;
		if (conf.File_NumberOfFeatureConstants_Mand && file.GetFeatureConstantCount() < conf.File_NumberOfFeatureConstants)
			result = true;
		if (conf.File_NestingSum_Mand && file.nestingSum < conf.File_NestingSum)
			result = true;
		
		return result;
	}
	
	/**
	 * Skip the feature for csv file creation depending on the mandatory settings of the configuration
	 *
	 * @param feat the feature
	 * @return true, if feature does not fulfill mandatory settings
	 */
	private boolean skipFeature(Feature feat)
	{
		boolean result = false;
		
		if (conf.Feature_NumberNofc_Mand && (feat.getConstants().size() < conf.Feature_NumberNofc))
			result = true;
		if (conf.Feature_NumberLofc_Mand && (feat.getLofc() < conf.Feature_NumberLofc))
			result = true;
		
		return result;
	}
	
	/**** CSV Start End Saving *****/
}