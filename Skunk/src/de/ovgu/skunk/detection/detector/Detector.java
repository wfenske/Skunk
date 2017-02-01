package de.ovgu.skunk.detection.detector;

import de.ovgu.skunk.detection.data.Context;
import de.ovgu.skunk.detection.data.Feature;
import de.ovgu.skunk.detection.data.FeatureReference;
import de.ovgu.skunk.detection.data.Method;
import de.ovgu.skunk.util.FileUtils;

import java.util.*;
import java.util.Map.Entry;

/**
 * The Class Detector.
 */
public class Detector {
    /**
     * The config contains the definition of the code smell.
     */
    private Context ctx;
    /**
     * Fitting feature locations with an explanation.
     */
    private Map<FeatureReference, List<SmellReason>> featureResult;

    /**
     * Instantiates a new detector.
     *
     * @param ctx the codesmell configuration and context
     */
    public Detector(Context ctx) {
        this.ctx = ctx;
        this.featureResult = new HashMap<>();
    }

    /**
     * Perform the detection based on the configuration and return fitting
     * features.
     *
     * @return a list with fitting features
     */
    public Map<FeatureReference, List<SmellReason>> Perform() {
        System.out.println(
                "... Start detection based on the config file " + FileUtils.relPath(ctx.config.configFilePath()) + " ...");
        checkFeatureCollection();
        checkMethodCollection();
        checkFileCollection();
        filterResults();
        System.out.println("... detection done!");
        // return the result
        return this.featureResult;
    }

    /**
     * Filter results based on the mandatory values of the configuration.
     */
    private void filterResults() {
        final DetectionConfig config = ctx.config;
        // check for mandatory attributes in the detection configuration
        ArrayList<SmellReason> mandatories = new ArrayList<>();
        if (config.Feature_MeanLofcRatio_Mand) mandatories.add(SmellReason.LARGEFEATURE_LOFCTOMEANLOFC);
        if (config.Feature_ProjectLocRatio_Mand) mandatories.add(SmellReason.LARGEFEATURE_LOFCTOLOC);
        if (config.Feature_NumberLofc_Mand) mandatories.add(SmellReason.LARGEFEATURE_NUMBERLOFC);
        if (config.Feature_NumberNofc_Mand) mandatories.add(SmellReason.LARGEFEATURE_NUMBERNOFC);
        if (config.Feature_NoFeatureConstantsRatio_Mand) mandatories.add(SmellReason.SHOTGUNSURGERY_NOFCOSUMNOFC);
        if (config.Feature_NumberOfCompilUnits_Mand) mandatories.add(SmellReason.SHOTGUNSURGERY_NUMBERCOMPILATIONUNITS);
        if (config.Method_LoacToLocRatio_Mand) mandatories.add(SmellReason.ANNOTATIONBUNDLE_LOACTOLOC);
        if (config.Method_LofcToLocRatio_Mand) mandatories.add(SmellReason.ANNOTATIONBUNDLE_LOFCTOLOC);
        if (config.Method_NegationCount_Mand) mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERNEGATIONS);
        if (config.Method_NestingDepthMin_Mand) mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
        if (config.Method_NestingSum_Mand) mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGSUM);
        if (config.Method_NumberOfFeatureConstantsNonDup_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTNONDUP);
        if (config.Method_NumberOfFeatureConstants_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTANTS);
        if (config.Method_LoacToLocRatio_Mand) mandatories.add(SmellReason.ANNOTATIONFILE_LOACTOLOC);
        if (config.File_LofcToLocRatio_Mand) mandatories.add(SmellReason.ANNOTATIONFILE_LOFCTOLOC);
        if (config.File_NegationCount_Mand) mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERNEGATIONS);
        if (config.File_NestingDepthMin_Mand) mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERNESTINGDEPTHMIN);
        if (config.File_NestingSum_Mand) mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERNESTINGSUM);
        if (config.File_NumberOfFeatureConstantsNonDup_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERFEATURECONSTNONDUP);
        if (config.File_NumberOfFeatureConstants_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERFEATURECONSTANTS);
        // delete featurelocations from the result if it does not contain a
        // mandatory attribute
        ArrayList<FeatureReference> toDelete = new ArrayList<>();
        for (FeatureReference key : featureResult.keySet()) {
            for (SmellReason mandatory : mandatories) {
                if (!featureResult.get(key).contains(mandatory)) toDelete.add(key);
            }
        }
        for (FeatureReference key : toDelete)
            featureResult.remove(key);
    }

    /**
     * Checks the method collection for suitable locations in a method.
     */
    private void checkMethodCollection() {
        for (Method meth : ctx.functions.AllMethods()) {
            // sort functions
            // Collections.sort(meth.featureLocations);
            this.sortByValues(meth.featureReferences);
            // ratio lofc to loc
            checkForMethodLofcToLoc(meth);
            // ratio loac to loc
            checkForMethodLoacToLoc(meth);
            checkMethodForNumberOfFeatureConstants(meth);
            checkMethodForNumberOfFeatureLocations(meth);
            checkMethodForNumberFeatureConstantsNonDup(meth);
            checkMethodForNumberNegations(meth);
            checkForMethodNestingSum(meth);
            checkForMethodNestingDepthMax(meth);
        }
    }

    /**
     * Checks the file for suitable locations in a method.
     */
    private void checkFileCollection() {
        for (de.ovgu.skunk.detection.data.File file : ctx.files.AllFiles()) {
            // sort functions
            // Collections.sort(meth.featureLocations);
            this.sortByValues(file.featureConstants);
            // ratio lofc to loc
            checkForFileLofcToLoc(file);
            // ratio loac to loc
            checkForFileLoacToLoc(file);
            checkFileForNumberOfFeatureConstants(file);
            checkFileForNumberOfFeatureLocations(file);
            checkFileForNumberFeatureConstantsNonDup(file);
            checkFileForNumberNegations(file);
            checkForFileNestingSum(file);
            checkForFileNestingDepthMax(file);
        }
    }

    /**
     * Check the feature collection for suitable feature locations.
     */
    private void checkFeatureCollection() {
        // check each feature and location
        for (Feature feat : ctx.featureExpressions.GetFeatures()) {
            checkForFeatureNoFeatureConstantsToSum(feat);
            checkForFeatureCompilUnits(feat);
            checkForFeatureNofc(feat);
            checkForFeatureLofc(feat);
            for (FeatureReference constant : feat.getReferences()) {
                // check for features that take up a huge part of the project
                // loc
                checkForFeatureToProjectRatio(feat, constant);
                // check for features that are bigger than the mean lofc
                checkForFeatureToFeatureRatio(constant);
            }
        }
    }

    /**
     * Check the ratio between lofc and loc in a method. If the ratio exceeds
     * the configuration value, add all features with the annotationbundle
     * lofctoloc reason to the result
     *
     * @param meth the method
     */
    private void checkForMethodLofcToLoc(Method meth) {
        final DetectionConfig config = ctx.config;

        if (!Double.isNaN(config.Method_LofcToLocRatio)) {
            double minLofc = (config.Method_LofcToLocRatio * meth.loc);
            if (meth.lofc >= minLofc) {
                for (UUID id : meth.featureReferences.keySet()) {
                    FeatureReference loc = ctx.featureExpressions.GetFeatureConstant(meth.featureReferences.get(id),
                            id);
                    this.addFeatureLocWithReason(loc, SmellReason.ANNOTATIONBUNDLE_LOFCTOLOC);
                }
            }
        }
    }

    /**
     * Check the ratio between lofa and loc in a method. If the ratio exceeds
     * the configuration value, add all features with the annotationbundle
     * loactoloc reason to the result
     *
     * @param meth the method
     */
    private void checkForMethodLoacToLoc(Method meth) {
        final DetectionConfig config = ctx.config;

        if (!Double.isNaN(config.Method_LoacToLocRatio)) {
            double minLoac = (config.Method_LoacToLocRatio * meth.loc);
            if (meth.GetLinesOfAnnotatedCode() >= minLoac) {
                for (UUID id : meth.featureReferences.keySet()) {
                    FeatureReference loc = ctx.featureExpressions.GetFeatureConstant(meth.featureReferences.get(id),
                            id);
                    this.addFeatureLocWithReason(loc, SmellReason.ANNOTATIONBUNDLE_LOACTOLOC);
                }
            }
        }
    }

    /**
     * Check if the number of feature constants in the method exceeds the
     * configuration value. Add all feature locs to the result with the Number
     * of Feature Constants reason
     *
     * @param meth the meth
     */
    private void checkMethodForNumberOfFeatureConstants(Method meth) {
        final DetectionConfig config = ctx.config;

        if (config.Method_NumberOfFeatureConstants != -1) {
            if (meth.GetFeatureConstantCount() > config.Method_NumberOfFeatureConstants) {
                for (UUID id : meth.featureReferences.keySet()) {
                    FeatureReference constant = ctx.featureExpressions
                            .GetFeatureConstant(meth.featureReferences.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTANTS);
                }
            }
        }
    }

    /**
     * Check if the number of feature locations in the method exceeds the
     * configuration value. Add all feature constans to the result with the
     * Number of Feature Locations reason
     *
     * @param meth the meth
     */
    private void checkMethodForNumberOfFeatureLocations(Method meth) {
        final DetectionConfig config = ctx.config;

        if (config.Method_NumberOfFeatureLocations != -1) {
            if (meth.GetFeatureConstantCount() > config.Method_NumberOfFeatureLocations) {
                for (UUID id : meth.featureReferences.keySet()) {
                    FeatureReference constant = ctx.featureExpressions
                            .GetFeatureConstant(meth.featureReferences.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONBUNDLE_NUMBERFEATURELOC);
                }
            }
        }
    }

    /**
     * Check if the number of feature constants in the method exceeds the
     * configuration value. Add all feature constants to the result with the
     * number of feature constants reason
     *
     * @param meth the meth
     */
    private void checkMethodForNumberFeatureConstantsNonDup(Method meth) {
        final DetectionConfig config = ctx.config;

        if (config.Method_NumberOfFeatureConstantsNonDup != -1) {
            if (meth.numberFeatureConstantsNonDup > config.Method_NumberOfFeatureConstantsNonDup) {
                for (UUID id : meth.featureReferences.keySet()) {
                    FeatureReference constant = ctx.featureExpressions
                            .GetFeatureConstant(meth.featureReferences.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTNONDUP);
                }
            }
        }
    }

    /**
     * Check method for number negations. If it exceeds the configuration value,
     * add all feature constants with the specific reason
     *
     * @param meth the method
     */
    private void checkMethodForNumberNegations(Method meth) {
        final DetectionConfig config = ctx.config;

        if (config.Method_NegationCount != -1) {
            if (meth.negationCount > config.Method_NegationCount) for (UUID id : meth.featureReferences.keySet()) {
                FeatureReference constant = ctx.featureExpressions
                        .GetFeatureConstant(meth.featureReferences.get(id), id);
                this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONBUNDLE_NUMBERNEGATIONS);
            }
        }
    }

    /**
     * Check if the sum of nestings exceeds the code smell configuration value.
     * If yes, add all feature constants with the corresponding reason to the
     * result.
     *
     * @param meth the method
     */
    private void checkForMethodNestingSum(Method meth) {
        final DetectionConfig config = ctx.config;
        if (config.Method_NestingSum != -1) {
            if (meth.nestingSum >= config.Method_NestingSum) for (UUID id : meth.featureReferences.keySet()) {
                FeatureReference constant = ctx.featureExpressions
                        .GetFeatureConstant(meth.featureReferences.get(id), id);
                this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGSUM);
            }
        }
    }

    /**
     * Check if the max nesting depth exceeds the code smell configuration
     * value. If yes, add all feature constant with the corresponding reason to
     * the result.
     *
     * @param meth the method
     */
    private void checkForMethodNestingDepthMax(Method meth) {
        final DetectionConfig config = ctx.config;
        if (config.Method_NestingDepthMin != -1) {
            // check nesting via stacks and nesting depth
            Stack<FeatureReference> nestingStack = new Stack<>();
            int beginNesting = -1;
            for (UUID id : meth.featureReferences.keySet()) {
                FeatureReference constant = ctx.featureExpressions
                        .GetFeatureConstant(meth.featureReferences.get(id), id);
                // add the item instantly if the stack is empty, set the
                // beginning nesting depth to the nd of the loc (nesting depth
                // is file-based not method based)
                if (nestingStack.isEmpty()) {
                    beginNesting = constant.nestingDepth;
                    nestingStack.push(constant);
                } else {
                    // current nesting in consideration with starting location
                    int curNesting = constant.nestingDepth - beginNesting;
                    // 0 is the beginning nesting degree, everything higher than
                    // zero means it is a nested location
                    if (curNesting > 0)
                        nestingStack.push(constant);
                    else {
                        // calculate nestingdepth of bundle
                        int ndm = -1;
                        for (FeatureReference current : nestingStack)
                            if ((current.nestingDepth - beginNesting) > ndm) ndm = current.nestingDepth - beginNesting;
                        // if the ndm of the bundle is higher than the
                        // configuration add all to the result
                        if (ndm >= config.Method_NestingDepthMin) {
                            while (!nestingStack.isEmpty())
                                this.addFeatureLocWithReason(nestingStack.pop(),
                                        SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
                        } else nestingStack.empty();
                    }
                }
            }
            // final emptiing if something is left
            if (!nestingStack.isEmpty()) {
                // calculate nestingdepth of bundle
                int ndm = -1;
                for (FeatureReference current : nestingStack)
                    if ((current.nestingDepth - beginNesting) > ndm) ndm = current.nestingDepth - beginNesting;
                if (ndm >= config.Method_NestingDepthMin) {
                    while (!nestingStack.isEmpty())
                        this.addFeatureLocWithReason(nestingStack.pop(),
                                SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
                } else nestingStack.empty();
            }
        }
    }

    /**
     * Check the ratio between lofc and loc in a method. If the ratio exceeds
     * the configuration value, add all features with the annotationbundle
     * lofctoloc reason to the result
     *
     * @param file
     */
    private void checkForFileLofcToLoc(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig config = ctx.config;
        if (!Double.isNaN(config.File_LofcToLocRatio)) {
            double minLofc = (config.File_LofcToLocRatio * file.loc);
            if (file.lofc >= minLofc) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference loc = ctx.featureExpressions.GetFeatureConstant(file.featureConstants.get(id),
                            id);
                    this.addFeatureLocWithReason(loc, SmellReason.ANNOTATIONFILE_LOFCTOLOC);
                }
            }
        }
    }

    /**
     * Check the ratio between lofa and loc in a method. If the ratio exceeds
     * the configuration value, add all features with the annotationbundle
     * loactoloc reason to the result
     *
     * @param file
     */
    private void checkForFileLoacToLoc(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig config = ctx.config;
        if (!Double.isNaN(config.File_LoacToLocRatio)) {
            double minLoac = (config.File_LoacToLocRatio * file.loc);
            if (file.GetLinesOfAnnotatedCode() >= minLoac) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference loc = ctx.featureExpressions.GetFeatureConstant(file.featureConstants.get(id),
                            id);
                    this.addFeatureLocWithReason(loc, SmellReason.ANNOTATIONFILE_LOACTOLOC);
                }
            }
        }
    }

    /**
     * Check if the number of feature constants in the method exceeds the
     * configuration value. Add all feature locs to the result with the Number
     * of Feature Constants reason
     *
     * @param file
     */
    private void checkFileForNumberOfFeatureConstants(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig config = ctx.config;
        if (config.File_NumberOfFeatureConstants != -1) {
            if (file.GetFeatureConstantCount() > config.File_NumberOfFeatureConstants) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference constant = ctx.featureExpressions
                            .GetFeatureConstant(file.featureConstants.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONFILE_NUMBERFEATURECONSTANTS);
                }
            }
        }
    }

    /**
     * Check if the number of feature locations in the method exceeds the
     * configuration value. Add all feature constans to the result with the
     * Number of Feature Locations reason
     *
     * @param file
     */
    private void checkFileForNumberOfFeatureLocations(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig config = ctx.config;
        if (config.File_NumberOfFeatureLocations != -1) {
            if (file.GetFeatureConstantCount() > config.File_NumberOfFeatureLocations) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference constant = ctx.featureExpressions
                            .GetFeatureConstant(file.featureConstants.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONFILE_NUMBERFEATURELOC);
                }
            }
        }
    }

    /**
     * Check if the number of feature constants in the method exceeds the
     * configuration value. Add all feature constants to the result with the
     * number of feature constants reason
     *
     * @param file
     */
    private void checkFileForNumberFeatureConstantsNonDup(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig config = ctx.config;
        if (config.File_NumberOfFeatureConstantsNonDup != -1) {
            if (file.numberFeatureConstantsNonDup > config.File_NumberOfFeatureConstantsNonDup) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference constant = ctx.featureExpressions
                            .GetFeatureConstant(file.featureConstants.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONFILE_NUMBERFEATURECONSTNONDUP);
                }
            }
        }
    }

    /**
     * Check method for number negations. If it exceeds the configuration value,
     * add all feature constants with the specific reason
     *
     * @param file
     */
    private void checkFileForNumberNegations(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig config = ctx.config;
        if (config.File_NegationCount != -1) {
            if (file.negationCount > config.File_NegationCount) for (UUID id : file.featureConstants.keySet()) {
                FeatureReference constant = ctx.featureExpressions
                        .GetFeatureConstant(file.featureConstants.get(id), id);
                this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONFILE_NUMBERNEGATIONS);
            }
        }
    }

    /**
     * Check if the sum of nestings exceeds the code smell configuration value.
     * If yes, add all feature constants with the corresponding reason to the
     * result.
     *
     * @param file
     */
    private void checkForFileNestingSum(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig config = ctx.config;
        if (config.File_NestingSum != -1) {
            if (file.nestingSum >= config.File_NestingSum) for (UUID id : file.featureConstants.keySet()) {
                FeatureReference constant = ctx.featureExpressions
                        .GetFeatureConstant(file.featureConstants.get(id), id);
                this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONFILE_NUMBERNESTINGSUM);
            }
        }
    }

    /**
     * Check if the max nesting depth exceeds the code smell configuration
     * value. If yes, add all feature constant with the corresponding reason to
     * the result.
     *
     * @param file
     */
    private void checkForFileNestingDepthMax(de.ovgu.skunk.detection.data.File file) {
        final DetectionConfig config = ctx.config;
        if (config.File_NestingDepthMin != -1) {
            // check nesting via stacks and nesting depth
            Stack<FeatureReference> nestingStack = new Stack<>();
            int beginNesting = -1;
            for (UUID id : file.featureConstants.keySet()) {
                FeatureReference constant = ctx.featureExpressions
                        .GetFeatureConstant(file.featureConstants.get(id), id);
                // add the item instantly if the stack is empty, set the
                // beginning nesting depth to the nd of the loc (nesting depth
                // is file-based not method based)
                if (nestingStack.isEmpty()) {
                    beginNesting = constant.nestingDepth;
                    nestingStack.push(constant);
                } else {
                    // current nesting in consideration with starting location
                    int curNesting = constant.nestingDepth - beginNesting;
                    // 0 is the beginning nesting degree, everything higher than
                    // zero means it is a nested location
                    if (curNesting > 0)
                        nestingStack.push(constant);
                    else {
                        // calculate nestingdepth of bundle
                        int ndm = -1;
                        for (FeatureReference current : nestingStack)
                            if ((current.nestingDepth - beginNesting) > ndm) ndm = current.nestingDepth - beginNesting;
                        // if the ndm of the bundle is higher than the
                        // configuration add all to the result
                        if (ndm >= config.File_NestingDepthMin) {
                            while (!nestingStack.isEmpty())
                                this.addFeatureLocWithReason(nestingStack.pop(),
                                        SmellReason.ANNOTATIONFILE_NUMBERNESTINGDEPTHMIN);
                        } else nestingStack.empty();
                    }
                }
            }
            // final emptiing if something is left
            if (!nestingStack.isEmpty()) {
                // calculate nestingdepth of bundle
                int ndm = -1;
                for (FeatureReference current : nestingStack)
                    if ((current.nestingDepth - beginNesting) > ndm) ndm = current.nestingDepth - beginNesting;
                if (ndm >= config.File_NestingDepthMin) {
                    while (!nestingStack.isEmpty())
                        this.addFeatureLocWithReason(nestingStack.pop(),
                                SmellReason.ANNOTATIONFILE_NUMBERNESTINGDEPTHMIN);
                } else nestingStack.empty();
            }
        }
    }

    /**
     * Check if the feature constant is bigger than the mean value of feature
     * lofc Indicates a large feature.
     *
     * @param loc the feature constant to examine
     */
    private void checkForFeatureToFeatureRatio(FeatureReference loc) {
        final DetectionConfig config = ctx.config;
        if (!Double.isNaN(config.Feature_MeanLofcRatio)) {
            // calculate the minimal lofc a feature location should have to be
            // considered big
            int lofc = (loc.end - loc.start);
            double minLofc = (config.Feature_MeanLofcRatio * ctx.featureExpressions.GetMeanLofc());
            // add the feature location if the feature lofc is bigger than the
            // minimal
            if (lofc >= minLofc) this.addFeatureLocWithReason(loc, SmellReason.LARGEFEATURE_LOFCTOMEANLOFC);
        }
    }

    /**
     * Check if the feature takes up a huge percentage of the whole project.
     * Indicates a large feature.
     *
     * @param feat the feature
     * @param loc  the current location
     */
    private void checkForFeatureToProjectRatio(Feature feat, FeatureReference loc) {
        final DetectionConfig config = ctx.config;
        if (!Double.isNaN(config.Feature_ProjectLocRatio)) {
            // calculate the minimal lofc the feature must have to be a large
            // feature
            double minLofc = (ctx.featureExpressions.GetLoc() * config.Feature_ProjectLocRatio);
            // add the feature location
            if (feat.getLofc() >= minLofc) this.addFeatureLocWithReason(loc, SmellReason.LARGEFEATURE_LOFCTOLOC);
        }
    }

    /**
     * Check if the feature has more constants than ratio amount. If yes, add
     * all locs to the result with the corresponding reason.
     *
     * @param feat the feat
     */
    private void checkForFeatureNoFeatureConstantsToSum(Feature feat) {
        final DetectionConfig config = ctx.config;
        if (!Double.isNaN(config.Feature_NoFeatureConstantsRatio)) {
            // amount of nofls the feature has to exceed for a smell
            double minNofl = ctx.featureExpressions.numberOfFeatureConstantReferences
                    * config.Feature_NoFeatureConstantsRatio;
            if (feat.getReferences().size() > minNofl) {
                for (FeatureReference loc : feat.getReferences())
                    this.addFeatureLocWithReason(loc, SmellReason.SHOTGUNSURGERY_NOFCOSUMNOFC);
            }
        }
    }

    /**
     * Check if the feature exceeds the configuration value for compilation
     * units. If yes, add all constants with the corresponding reason to the
     * result.
     *
     * @param feat the feat
     */
    private void checkForFeatureCompilUnits(Feature feat) {
        final DetectionConfig config = ctx.config;

        if (config.Feature_NumberOfCompilUnits != -1) {
            if (feat.GetAmountCompilationFiles() > config.Feature_NumberOfCompilUnits) {
                for (FeatureReference loc : feat.getReferences())
                    this.addFeatureLocWithReason(loc, SmellReason.SHOTGUNSURGERY_NUMBERCOMPILATIONUNITS);
            }
        }
    }

    /**
     * Checks if the feature exceeds the threshold for lofc.
     *
     * @param feat the feat
     */
    private void checkForFeatureLofc(Feature feat) {
        final DetectionConfig config = ctx.config;
        if (config.Feature_NumberLofc != -1) {
            if (feat.getLofc() > config.Feature_NumberLofc) {
                for (FeatureReference loc : feat.getReferences())
                    this.addFeatureLocWithReason(loc, SmellReason.LARGEFEATURE_NUMBERLOFC);
            }
        }
    }

    /**
     * Checks if the feature exceeds the threshold for nofc.
     *
     * @param feat the feat
     */
    private void checkForFeatureNofc(Feature feat) {
        final DetectionConfig config = ctx.config;

        if (config.Feature_NumberNofc != -1) {
            if (feat.references.size() > config.Feature_NumberNofc) {
                for (FeatureReference loc : feat.getReferences())
                    this.addFeatureLocWithReason(loc, SmellReason.LARGEFEATURE_NUMBERNOFC);
            }
        }
    }

    /**
     * Adds the feature constant to the result list with the specified reason,
     * or appends another reason if the location is already inside the result
     * list.
     *
     * @param constant the feature constant to add
     * @param reason   the reason
     */
    private void addFeatureLocWithReason(FeatureReference constant, SmellReason reason) {
        if (this.featureResult.containsKey(constant))
            this.featureResult.get(constant).add(reason);
        else {
            List<SmellReason> enumReason = new ArrayList<>();
            enumReason.add(reason);
            this.featureResult.put(constant, enumReason);
        }
    }

    /**
     * Sort a hashmap by values
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map the map
     * @return A fresh copy of the given map, where entries are sorted by their
     * values
     */
    public <K extends Comparable<K>, V extends Comparable<V>> Map<K, V> sortByValues(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new ArrayList<>(map.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        // LinkedHashMap will keep the keys in the order they are inserted
        // which is currently sorted on natural ordering
        Map<K, V> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
}
