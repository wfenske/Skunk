package de.ovgu.skunk.detection.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;

public class FileCollection 
{
	/** The methods per file. */
	public static List<de.ovgu.skunk.detection.data.File> Files;
	
	/**
	 * Instantiates a new method collection.
	 */
	public static void Initialize()
	{
		Files = new ArrayList<de.ovgu.skunk.detection.data.File>();
	}
	
	/**
	 * Adds the file or gets it if already inside the list
	 *
	 * @param filePath the file path
	 * @param doc the doc
	 * @return the data. file
	 */
	public static de.ovgu.skunk.detection.data.File GetOrAddFile(String filePath)
	{
		for (de.ovgu.skunk.detection.data.File file : Files)
		{
			if (file.filePath.equals(filePath))
				return file;
		}
		
		de.ovgu.skunk.detection.data.File newFile = new de.ovgu.skunk.detection.data.File(filePath);
		Files.add(newFile);
		
		return newFile;
	}
	
	/**
	 * Gets the file.
	 *
	 * @param filePath the file path
	 * @return the data. file
	 */
	public static de.ovgu.skunk.detection.data.File GetFile(String filePath)
	{
		for (de.ovgu.skunk.detection.data.File file : Files)
		{
			if (file.filePath.equals(filePath))
				return file;
		}
		
		return null;
	}

	
	/**
	 * Calculate metrics for all metrics after finishing the collection
	 */
	public static void PostAction()
	{
		for (de.ovgu.skunk.detection.data.File file : Files)
		{
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
	public static String SerializeFiles()
	{
		// nullify already processed data for memory reasons
		for (de.ovgu.skunk.detection.data.File file : Files)
		{
			file.emptyLines.clear();
			file.loac.clear();
		}
		
		XStream stream = new XStream();
		String xmlFeatures = stream.toXML(Files);
		
		return xmlFeatures;
	}
	
	/**
	 * Deserializes an xml string into the collection.
	 *
	 * @param xml the serialized xml representation
	 */
	@SuppressWarnings("unchecked")
	public static void DeserialzeFiles(File xmlFile)
	{
		XStream stream = new XStream();
		Files = (List<de.ovgu.skunk.detection.data.File>) stream.fromXML(xmlFile);
	}
}
