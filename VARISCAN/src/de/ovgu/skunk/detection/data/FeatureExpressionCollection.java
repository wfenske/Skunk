package de.ovgu.skunk.detection.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.thoughtworks.xstream.XStream;

/**
 * The Class FeatureExpressionCollection.
 */
public class FeatureExpressionCollection 
{

    private static Map<String, Feature> _features;
	private static int _count;
	private static int _loc;
	private static int _meanLofc;
    /**
     * Number of times any feature constant has been mentioned
     */
	public static int numberOfFeatureConstantReferences;
	
	/**
	 * Gets the count of features.
	 *
	 * @return the amount of features in the collection
	 */
	public static int GetCount()
	{
		return _count;
	}
	
	/**
	 * Sets the amount of features
	 *
	 * @param value the value
	 */
	public static void SetCount(int value)
	{
		_count = value;
	}
	
	/**
	 * Gets the amount of lines of code.
	 *
	 * @return the amount of loc
	 */
	public static int GetLoc()
	{
		return _loc;
	}
	
	/**
	 * Gets the mean value of lines of feature code.
	 *
	 * @return the amount of features in the collection
	 */
	public static int GetMeanLofc()
	{
		return _meanLofc;
	}
	
	/**
	 * Sets the mean value of lines of feature code
	 *
	 * @param value the value
	 */
	public static void SetMeanLofc(int value)
	{
		_meanLofc = value;
	}
	
	/**
	 * Sets the loc.
	 *
	 * @param loc the loc
	 */
	public static void AddLoc(int loc)
	{
		_loc += loc;
	}
	
	/**
	 * Gets the feature with the input name
	 *
	 * @param name the name
	 * @return the feature
	 */
	public static Feature InternFeature(String name)
	{
        Feature existingFeature = _features.get(name);
        if (existingFeature != null)
            return existingFeature;
		
		// feature missing --> add new
		Feature newFeature = new Feature(name);
        _features.put(name, newFeature);
        _count++;
		return newFeature;
	}
	
	/**
     * Gets the feature constant of the specified feature
     *
     * @param name
     *            the name
     * @param id
     *            the id of the constant reference
     * @return the feature constant or <code>null</code>
     */
	public static FeatureReference GetFeatureConstant(String name, UUID id)
	{
        for (Feature feature : _features.values())
		{
			if (feature.constants.containsKey(id))
				return feature.constants.get(id);
		}
		return null;
	}
	
	/**
	 * Get all features.
	 *
	 * @return the list
	 */
    public static Collection<Feature> GetFeatures()
	{
        return _features.values();
	}
	
	/**
	 * Initialize necessary components of the collection
	 */
	public static void Initialize()
	{
        _features = new LinkedHashMap<>();
		_count = 0;
		_loc = 0;
		numberOfFeatureConstantReferences = 0;
	}
	
	/**
	 * Misc operations (calculate mean lofc)
	 */
	public static void PostAction()
	{
        for (Feature feat : _features.values())
			_meanLofc = _meanLofc + feat.getLofc();
		
        if (!_features.isEmpty())
			_meanLofc = _meanLofc / _features.size();
	}
	
	/**
	 * Serialize the features into a xml representation
	 *
	 * @return A xml representation of this object.
	 */
	public static String SerializeFeatures()
	{
		XStream stream = new XStream();
        ArrayList<Feature> listOfFeatures = new ArrayList<>(_features.values());
        String xmlFeatures = stream.toXML(listOfFeatures);
		
		return xmlFeatures;
	}
	
	/**
	 * Deserializes an xml string into the collection.
	 *
	 * @param xml the serialized xml representation
	 */
	@SuppressWarnings("unchecked")
	public static void DeserialzeFeatures(File xmlFile)
	{
		XStream stream = new XStream();
        List<Feature> listOfFeatures = (List<Feature>) stream.fromXML(xmlFile);
        for (Feature feature : listOfFeatures) {
            _features.put(feature.Name, feature);
        }
	}
}
