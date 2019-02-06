package de.ovgu.skunk.detection.data;

import com.thoughtworks.xstream.XStream;

import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;

/**
 * The Class FeatureExpressionCollection.
 */
public class FeatureExpressionCollection {
    private final Context ctx;
    private Map<String, Feature> _features;
    private int _loc;
    private int _meanLofc;
    /**
     * Number of times any feature constant has been mentioned
     */
    public int numberOfFeatureConstantReferences;

    /**
     * Gets the count of features.
     *
     * @return the amount of features in the collection
     */
    public int GetCount() {
        return _features.size();
    }

    /**
     * Gets the total number of lines of code of all features combined
     *
     * @return the number of feature LOC
     */
    public int GetLoc() {
        return _loc;
    }

    /**
     * Gets the mean value of lines of feature code.
     *
     * @return the amount of features in the collection
     */
    public int GetMeanLofc() {
        return _meanLofc;
    }

    /**
     * Sets the mean value of lines of feature code
     *
     * @param value the value
     */
    public void SetMeanLofc(int value) {
        _meanLofc = value;
    }

    /**
     * Sets the loc.
     *
     * @param loc the loc
     */
    public void AddLoc(int loc) {
        _loc += loc;
    }

    /**
     * Gets the feature with the input name
     *
     * @param name the name
     * @return the feature
     */
    public Feature InternFeature(String name) {
        Feature existingFeature = _features.get(name);
        if (existingFeature != null) return existingFeature;
        // feature missing --> add new
        Feature newFeature = new Feature(ctx, name);
        _features.put(name, newFeature);
        return newFeature;
    }

    /**
     * Gets the feature constant of the specified feature
     *
     * @param name the name
     * @param id   the id of the constant reference
     * @return the feature constant or <code>null</code>
     */
    public FeatureReference GetFeatureConstant(String name, UUID id) {
        for (Feature feature : _features.values()) {
            if (feature.references.containsKey(id)) return feature.references.get(id);
        }
        return null;
    }

    /**
     * Get all features.
     *
     * @return the list
     */
    public Collection<Feature> GetFeatures() {
        return _features.values();
    }

    /**
     * Initialize necessary components of the collection
     */
    public FeatureExpressionCollection(Context ctx) {
        this.ctx = ctx;
        _features = new LinkedHashMap<>();
        _loc = 0;
        numberOfFeatureConstantReferences = 0;
    }

    /**
     * Misc operations (calculate mean lofc)
     */
    public void PostAction() {
        for (Feature feat : _features.values())
            _meanLofc = _meanLofc + feat.getLofc();
        if (!_features.isEmpty()) _meanLofc = _meanLofc / _features.size();
    }

    /**
     * Serialize the features into a xml representation
     *
     * @return A xml representation of this object.
     */
    public Consumer<Writer> SerializeFeatures() {
        XStream stream = new XStream();
        ArrayList<Feature> listOfFeatures = new ArrayList<>(_features.values());

        return (writer -> stream.toXML(listOfFeatures, writer));
    }

    /**
     * Deserializes the XML provided by the reader into the collection.
     *
     * @param xmlFileReader reader providing the serialized XML representation
     */
    public void DeserializeFeatures(Reader xmlFileReader) {
        XStream stream = new XStream();
        List<Feature> listOfFeatures = (List<Feature>) stream.fromXML(xmlFileReader);
        for (Feature feature : listOfFeatures) {
            _features.put(feature.Name, feature);
        }
    }
}
