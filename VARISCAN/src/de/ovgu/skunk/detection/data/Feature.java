package de.ovgu.skunk.detection.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Class Feature.
 */
public class Feature implements Comparable<Feature>{

	/** The name of the feature. */
	public String Name;
	
	/** The lines of feature code. */
	private int _lofc;
	
	/** The feature constants that are part of the feature*/
    public Map<UUID, FeatureReference> constants;
	
	/** nesting Depth informations */
	public int minNestingDepth;
	public int maxNestingDepth;
	
	/** Granularity information*/
	public EnumGranularity maxGranularity = EnumGranularity.NOTDEFINED;
	public EnumGranularity minGranularity = EnumGranularity.NOTDEFINED;
	
	/* scattering information */
    public List<String> compilationFiles;
	
	/**
	 * Gets the lines of code.
	 *
	 * @return the lofc
	 */
	public int getLofc()
	{
		return this._lofc;
	}
	
	/**
	 * Gets all constants of the feature
	 *
	 * @return the locs
	 */
    public List<FeatureReference> getConstants()
	{
        return new ArrayList<>(this.constants.values());
	}
	
	/**
	 * Instantiates a new feature.
	 *
	 * @param name the name
	 */
	public Feature(String name)
	{
		this.Name = name;
        this.constants = new HashMap<UUID, FeatureReference>();
		this.compilationFiles = new ArrayList<String>();
		
		this.maxNestingDepth = -1;
		this.minNestingDepth = -1;
	}

	/**
     * Adds the feature constant and increases lines of feature code.
     *
     * @param ref
     *            the loc
     */
    public void AddReference(FeatureReference ref)
	{
		// connect constant with this feature (both directions)
        ref.feature = this;
		
		// set loc for the feature
        this.constants.put(ref.id, ref);
        this._lofc += ref.end - ref.start + 1;
		
        de.ovgu.skunk.detection.data.File file = FileCollection.GetFile((ref.filePath));
		for (int current : file.emptyLines)
            if (current > ref.start && current < ref.end)
				this._lofc--;
		
		// assign nesting depth
		if (this.minNestingDepth == -1)
            this.minNestingDepth = ref.nestingDepth;
		if (this.maxNestingDepth == -1)
            this.maxNestingDepth = ref.nestingDepth;
		
        if (this.maxNestingDepth < ref.nestingDepth)
            this.maxNestingDepth = ref.nestingDepth;
        if (this.minNestingDepth > ref.nestingDepth)
            this.minNestingDepth = ref.nestingDepth;
		
		// add cu if not already in the list
        if (!this.compilationFiles.contains(ref.filePath))
            this.compilationFiles.add(ref.filePath);
		
		FeatureExpressionCollection.numberOfFeatureConstantReferences++;
	}

	/**
	 * Gets the amount compilation files.
	 *
	 * @return the int
	 */
	public int GetAmountCompilationFiles()
	{
		return this.compilationFiles.size();
	}
	
	@Override
	public int compareTo(Feature o) {
		if (o.getLofc() > this.getLofc())
			return -1;
		else 
			return 1;
	}

	
}
