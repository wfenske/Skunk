package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.Context;
import de.ovgu.skunk.detection.data.FeatureExpressionCollection;
import de.ovgu.skunk.detection.data.FeatureReference;
import de.ovgu.skunk.detection.detector.SmellReason;

import java.util.ArrayList;
import java.util.HashMap;

public class AttributeOverview {

    private final Context ctx;
    public SmellReason Reason = null;
    private ArrayList<String> featureConstants = null;
    private int noFeatureLocs = 0;
    private int lofc = 0;
    private HashMap<String, ArrayList<Integer>> loacs = null;

    /**
     * Instantiates a new attribute overview.
     *
     * @param ctx
     * @param reason the reason
     */
    public AttributeOverview(Context ctx, SmellReason reason) {
        this.ctx = ctx;
        this.Reason = reason;
        this.loacs = new HashMap<>();
        this.featureConstants = new ArrayList<>();
    }

    /**
     * Adds the feature constant information to the attribute overview
     *
     * @param constant the loc
     */
    public void AddFeatureLocationInfo(FeatureReference constant) {
        // add metrics
        this.noFeatureLocs++;
        this.lofc = constant.end - constant.start;

        // add feature constant if not already part of it
        if (!this.featureConstants.contains(constant.feature.Name))
            this.featureConstants.add(constant.feature.Name);

        // add all lines per file to the data structure, that are part of the feature constant... no doubling for loac calculation
        if (!loacs.keySet().contains(constant.filePath))
            loacs.put(constant.filePath, new ArrayList<>());

        for (int i = constant.start; i <= constant.end; i++) {
            if (!loacs.get(constant.filePath).contains(i))
                loacs.get(constant.filePath).add(i);
        }
    }

    @Override
    public String toString() {
        FeatureExpressionCollection featureExpressions = ctx.featureExpressions;

        // calculate max loac
        int completeLoac = 0;
        for (String file : loacs.keySet())
            completeLoac += loacs.get(file).size();

        // calculate percentages
        float percentOfLoc = completeLoac * 100 / featureExpressions.GetLoc();
        float percentOfLocations = this.noFeatureLocs * 100 / featureExpressions.numberOfFeatureConstantReferences;
        float percentOfConstants = this.featureConstants.size() * 100 / featureExpressions.GetFeatures().size();

        // Complete overview
        String res = ">>> Overview " + Reason + "\r\n";
        res += "Number of features: \t" + this.featureConstants.size() + " (" + percentOfConstants + "% of " + featureExpressions.GetFeatures().size() + " constants)\r\n";
        res += "Number of feature constants: \t" + this.noFeatureLocs + " (" + percentOfLocations + "% of " + featureExpressions.numberOfFeatureConstantReferences + " locations)\r\n";
        res += "Lines of annotated Code: \t" + completeLoac + " (" + percentOfLoc + "% of " + featureExpressions.GetLoc() + " LOC)\r\n";
        res += "Lines of feature code: \t\t" + this.lofc + "\r\n\r\n";

        return res;
    }
}
