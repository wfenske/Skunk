package de.ovgu.skunk.detection.input;

import de.ovgu.skunk.detection.data.Context;
import de.ovgu.skunk.detection.data.FeatureReference;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class CppStatsFeatureConstant.
 */
public class CppStatsFeatureConstant {
    private final Context ctx;
    public List<String> featureExpressions;
    public List<Boolean> notFlags;


    public String filePath;
    public String type;

    public int start;
    public int end;

    public CppStatsFeatureConstant parent;

    /**
     * Instantiates a new feature location.
     *
     * @param ctx      VARISCAN Context holding global data
     * @param entry    the entry
     * @param filePath the file path
     * @param type     the type
     * @param start    the start
     * @param end      the end
     * @param parent   the parent
     */
    public CppStatsFeatureConstant(Context ctx, String entry, String filePath, String type, int start, int end, CppStatsFeatureConstant parent) {
        this.ctx = ctx;
        this.filePath = filePath;
        this.type = type;

        this.start = start;
        this.end = end;

        this.parent = parent;

        // get features from entry
        this.featureExpressions = new LinkedList<>();
        this.notFlags = new LinkedList<>();

        this.getFeaturesFromEntry(entry);

        // remove features from parent;
        if (parent != null)
            this.removeFeaturesFromParents(this.parent);
    }

    /**
     * Save this feature constant information to the feature expression collection
     */
    public void SaveFeatureConstantInformation(int stackSize) {
        // stackSize 1 means nesting depth of 0;
        stackSize--;

        List<FeatureReference> references = new ArrayList<>();

        // search for the corresponding feature expression and save information
        for (String featureName : this.featureExpressions) {
            // end-1 = #endif does not belong to lines of code????
            FeatureReference ref = new FeatureReference(this.filePath, this.start, this.end, stackSize,
                    this.notFlags.get(this.featureExpressions.indexOf(featureName)));
            ctx.featureExpressions.InternFeature(featureName).AddReference(ref);

            // remember created locations for combinations
            references.add(ref);
        }

        // set combined feature constants
        for (FeatureReference current : references)
            for (FeatureReference other : references) {
                if (other != current)
                    current.combinedWith.add(other.id);
            }
    }

    /**
     * Gets the features from the entry
     *
     * @param entry the entry
     * @return the features from entry
     */
    private void getFeaturesFromEntry(String entry) {
        // remove comments from entry
        final int commentStart = entry.indexOf("/*");
        if (commentStart != -1) {
            String comment = entry.substring(commentStart, entry.indexOf("*/", commentStart + 2) + 2);
            entry = entry.replace(comment, "");
        }

        Pattern pattern = Pattern.compile("[\\w!]+");
        Matcher matcher = pattern.matcher(entry);

        // get each feature from entry
        boolean notFlag = false;
        while (matcher.find()) {
            String match = matcher.group();

            // set notFlag and replace !
            if (match.contains("!"))
                notFlag = true;

            // defined is not a feature
            if (match.contains("defined"))
                continue;

                // numbers only, or it begins with a number --> version numbers or whatever
            else if (match.matches("(\\d)+$") || Character.isDigit(match.charAt(0)) || match.charAt(0) == '!')
                continue;

            // save feature expression and save boolean
            this.featureExpressions.add(match);

            if (notFlag) {
                this.notFlags.add(true);
                notFlag = false;
            } else
                this.notFlags.add(false);

        }
    }

    /**
     * Removes features that are already included in the parent.
     *
     * @param parent the parent
     */
    private void removeFeaturesFromParents(final CppStatsFeatureConstant parent) {
        // get features that are in both collection
        List<String> toRemove = new LinkedList<>();
        List<Boolean> toRemoveFlags = new LinkedList<>();

        // remove features that are already included in of the item's parents
        CppStatsFeatureConstant nextParent = parent;
        while (nextParent != null) {
            for (String parentFeature : nextParent.featureExpressions) {
                for (String feature : this.featureExpressions) {
                    if (feature.equals(parentFeature) && (!toRemove.contains(feature))) {
                        toRemoveFlags.add(this.notFlags.get(this.featureExpressions.indexOf(feature)));
                        toRemove.add(feature);
                    }
                }
            }

            nextParent = nextParent.parent;
        }

        // remove doubled features from current set (and respective notFlag)
        for (String remove : toRemove)
            this.featureExpressions.remove(remove);

        for (Boolean remove : toRemoveFlags)
            this.notFlags.remove(remove);
    }
}
