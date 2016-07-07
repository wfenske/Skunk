package de.ovgu.skunk.detection.detector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.UUID;

import de.ovgu.skunk.detection.data.Feature;
import de.ovgu.skunk.detection.data.FeatureExpressionCollection;
import de.ovgu.skunk.detection.data.FeatureReference;
import de.ovgu.skunk.detection.data.FileCollection;
import de.ovgu.skunk.detection.data.Method;
import de.ovgu.skunk.detection.data.MethodCollection;

/**
 * The Class Detector.
 */
public class Detector {

    /** The config contains the definition of the code smell. */
    private DetectionConfig config;

    /** Fitting feature locations with an explanation. */
    private Map<FeatureReference, List<SmellReason>> featureResult;

    /**
     * Instantiates a new detector.
     *
     * @param config
     *            the codesmell configuration
     */
    public Detector(DetectionConfig config) {
        this.config = config;
        this.featureResult = new HashMap<>();
    }

    /**
     * Perform the detection based on the configuration and return fitting
     * features.
     *
     * @return a list with fitting features
     */
    public Map<FeatureReference, List<SmellReason>> Perform() {
        System.out.println("... Start detection based on the config file " + config.configFilePath() + " ...");

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
        // check for mandatory attributes in the detection configuration
        ArrayList<SmellReason> mandatories = new ArrayList<SmellReason>();

        if (config.Feature_MeanLofcRatio_Mand)
            mandatories.add(SmellReason.LARGEFEATURE_LOFCTOMEANLOFC);
        if (config.Feature_ProjectLocRatio_Mand)
            mandatories.add(SmellReason.LARGEFEATURE_LOFCTOLOC);
        if (config.Feature_NumberLofc_Mand)
            mandatories.add(SmellReason.LARGEFEATURE_NUMBERLOFC);
        if (config.Feature_NumberNofc_Mand)
            mandatories.add(SmellReason.LARGEFEATURE_NUMBERNOFC);
        if (config.Feature_NoFeatureConstantsRatio_Mand)
            mandatories.add(SmellReason.SHOTGUNSURGERY_NOFCOSUMNOFC);
        if (config.Feature_NumberOfCompilUnits_Mand)
            mandatories.add(SmellReason.SHOTGUNSURGERY_NUMBERCOMPILATIONUNITS);

        if (config.Method_LoacToLocRatio_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_LOACTOLOC);
        if (config.Method_LofcToLocRatio_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_LOFCTOLOC);
        if (config.Method_NegationCount_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERNEGATIONS);
        if (config.Method_NestingDepthMin_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
        if (config.Method_NestingSum_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGSUM);
        if (config.Method_NumberOfFeatureConstantsNonDup_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTNONDUP);
        if (config.Method_NumberOfFeatureConstants_Mand)
            mandatories.add(SmellReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTANTS);

        if (config.Method_LoacToLocRatio_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_LOACTOLOC);
        if (config.File_LofcToLocRatio_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_LOFCTOLOC);
        if (config.File_NegationCount_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERNEGATIONS);
        if (config.File_NestingDepthMin_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERNESTINGDEPTHMIN);
        if (config.File_NestingSum_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERNESTINGSUM);
        if (config.File_NumberOfFeatureConstantsNonDup_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERFEATURECONSTNONDUP);
        if (config.File_NumberOfFeatureConstants_Mand)
            mandatories.add(SmellReason.ANNOTATIONFILE_NUMBERFEATURECONSTANTS);

        // delete featurelocations from the result if it does not contain a
        // mandatory attribute
        ArrayList<FeatureReference> toDelete = new ArrayList<FeatureReference>();
        for (FeatureReference key : featureResult.keySet()) {
            for (SmellReason mandatory : mandatories) {
                if (!featureResult.get(key).contains(mandatory))
                    toDelete.add(key);
            }
        }

        for (FeatureReference key : toDelete)
            featureResult.remove(key);
    }

    /**
     * Checks the methodlocation for suitable locations in a method.
     */
    private void checkMethodCollection() {
        for (String file : MethodCollection.methodsPerFile.keySet()) {
            for (Method meth : MethodCollection.methodsPerFile.get(file)) {
                // sort functions
                // Collections.sort(meth.featureLocations);
                this.sortByValues(meth.featureConstants);

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
    }

    /**
     * Checks the file for suitable locations in a method.
     */
    private void checkFileCollection() {
        for (de.ovgu.skunk.detection.data.File file : FileCollection.Files) {
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
        for (Feature feat : FeatureExpressionCollection.GetFeatures()) {
            checkForFeatureNoFeatureConstantsToSum(feat);

            checkForFeatureCompilUnits(feat);

            checkForFeatureNofc(feat);

            checkForFeatureLofc(feat);

            for (FeatureReference constant : feat.getConstants()) {
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
     * @param meth
     *            the method
     */
    private void checkForMethodLofcToLoc(Method meth) {
        if (this.config.Method_LofcToLocRatio != -1000) {
            double minLofc = (this.config.Method_LofcToLocRatio * meth.loc);

            if (meth.lofc >= minLofc) {
                for (UUID id : meth.featureConstants.keySet()) {
                    FeatureReference loc = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id),
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
     * @param meth
     *            the method
     */
    private void checkForMethodLoacToLoc(Method meth) {
        if (this.config.Method_LoacToLocRatio != -1000) {
            double minLoac = (this.config.Method_LoacToLocRatio * meth.loc);

            if (meth.GetLinesOfAnnotatedCode() >= minLoac) {
                for (UUID id : meth.featureConstants.keySet()) {
                    FeatureReference loc = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id),
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
     * @param meth
     *            the meth
     */
    private void checkMethodForNumberOfFeatureConstants(Method meth) {
        if (this.config.Method_NumberOfFeatureConstants != -1) {
            if (meth.GetFeatureConstantCount() > this.config.Method_NumberOfFeatureConstants) {
                for (UUID id : meth.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
                            .GetFeatureConstant(meth.featureConstants.get(id), id);
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
     * @param meth
     *            the meth
     */
    private void checkMethodForNumberOfFeatureLocations(Method meth) {
        if (this.config.Method_NumberOfFeatureLocations != -1) {
            if (meth.GetFeatureConstantCount() > this.config.Method_NumberOfFeatureLocations) {
                for (UUID id : meth.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
                            .GetFeatureConstant(meth.featureConstants.get(id), id);
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
     * @param meth
     *            the meth
     */
    private void checkMethodForNumberFeatureConstantsNonDup(Method meth) {
        if (this.config.Method_NumberOfFeatureConstantsNonDup != -1) {
            if (meth.numberFeatureConstantsNonDup > this.config.Method_NumberOfFeatureConstantsNonDup) {
                for (UUID id : meth.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
                            .GetFeatureConstant(meth.featureConstants.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTNONDUP);
                }
            }
        }
    }

    /**
     * Check method for number negations. If it exceeds the configuration value,
     * add all feature constants with the specific reason
     *
     * @param meth
     *            the method
     */
    private void checkMethodForNumberNegations(Method meth) {
        if (this.config.Method_NegationCount != -1) {
            if (meth.negationCount > this.config.Method_NegationCount)
                for (UUID id : meth.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
                            .GetFeatureConstant(meth.featureConstants.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONBUNDLE_NUMBERNEGATIONS);
                }
        }
    }

    /**
     * Check if the sum of nestings exceeds the code smell configuration value.
     * If yes, add all feature constants with the corresponding reason to the
     * result.
     *
     * @param meth
     *            the method
     */
    private void checkForMethodNestingSum(Method meth) {
        if (this.config.Method_NestingSum != -1) {
            if (meth.nestingSum >= this.config.Method_NestingSum)
                for (UUID id : meth.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
                            .GetFeatureConstant(meth.featureConstants.get(id), id);
                    this.addFeatureLocWithReason(constant, SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGSUM);
                }
        }
    }

    /**
     * Check if the max nesting depth exceeds the code smell configuration
     * value. If yes, add all feature constant with the corresponding reason to
     * the result.
     *
     * @param meth
     *            the method
     */
    private void checkForMethodNestingDepthMax(Method meth) {
        if (this.config.Method_NestingDepthMin != -1) {
            // check nesting via stacks and nesting depth
            Stack<FeatureReference> nestingStack = new Stack<FeatureReference>();
            int beginNesting = -1;

            for (UUID id : meth.featureConstants.keySet()) {
                FeatureReference constant = FeatureExpressionCollection
                        .GetFeatureConstant(meth.featureConstants.get(id), id);

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
                            if ((current.nestingDepth - beginNesting) > ndm)
                                ndm = current.nestingDepth - beginNesting;

                        // if the ndm of the bundle is higher than the
                        // configuration add all to the result
                        if (ndm >= config.Method_NestingDepthMin) {
                            while (!nestingStack.isEmpty())
                                this.addFeatureLocWithReason(nestingStack.pop(),
                                        SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
                        } else
                            nestingStack.empty();
                    }
                }
            }

            // final emptiing if something is left
            if (!nestingStack.isEmpty()) {
                // calculate nestingdepth of bundle
                int ndm = -1;
                for (FeatureReference current : nestingStack)
                    if ((current.nestingDepth - beginNesting) > ndm)
                        ndm = current.nestingDepth - beginNesting;

                if (ndm >= config.Method_NestingDepthMin) {
                    while (!nestingStack.isEmpty())
                        this.addFeatureLocWithReason(nestingStack.pop(),
                                SmellReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
                } else
                    nestingStack.empty();
            }
        }
    }

    /**
     * Check the ratio between lofc and loc in a method. If the ratio exceeds
     * the configuration value, add all features with the annotationbundle
     * lofctoloc reason to the result
     *
     * @param meth
     *            the method
     */
    private void checkForFileLofcToLoc(de.ovgu.skunk.detection.data.File file) {
        if (this.config.File_LofcToLocRatio != -1000) {
            double minLofc = (this.config.File_LofcToLocRatio * file.loc);

            if (file.lofc >= minLofc) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference loc = FeatureExpressionCollection.GetFeatureConstant(file.featureConstants.get(id),
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
     * @param meth
     *            the method
     */
    private void checkForFileLoacToLoc(de.ovgu.skunk.detection.data.File file) {
        if (this.config.File_LoacToLocRatio != -1000) {
            double minLoac = (this.config.File_LoacToLocRatio * file.loc);

            if (file.GetLinesOfAnnotatedCode() >= minLoac) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference loc = FeatureExpressionCollection.GetFeatureConstant(file.featureConstants.get(id),
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
     * @param meth
     *            the meth
     */
    private void checkFileForNumberOfFeatureConstants(de.ovgu.skunk.detection.data.File file) {
        if (this.config.File_NumberOfFeatureConstants != -1) {
            if (file.GetFeatureConstantCount() > this.config.File_NumberOfFeatureConstants) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
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
     * @param meth
     *            the meth
     */
    private void checkFileForNumberOfFeatureLocations(de.ovgu.skunk.detection.data.File file) {
        if (this.config.File_NumberOfFeatureLocations != -1) {
            if (file.GetFeatureConstantCount() > this.config.File_NumberOfFeatureLocations) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
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
     * @param meth
     *            the meth
     */
    private void checkFileForNumberFeatureConstantsNonDup(de.ovgu.skunk.detection.data.File file) {
        if (this.config.File_NumberOfFeatureConstantsNonDup != -1) {
            if (file.numberFeatureConstantsNonDup > this.config.File_NumberOfFeatureConstantsNonDup) {
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
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
     * @param meth
     *            the method
     */
    private void checkFileForNumberNegations(de.ovgu.skunk.detection.data.File file) {
        if (this.config.File_NegationCount != -1) {
            if (file.negationCount > this.config.File_NegationCount)
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
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
     * @param meth
     *            the method
     */
    private void checkForFileNestingSum(de.ovgu.skunk.detection.data.File file) {
        if (this.config.File_NestingSum != -1) {
            if (file.nestingSum >= this.config.File_NestingSum)
                for (UUID id : file.featureConstants.keySet()) {
                    FeatureReference constant = FeatureExpressionCollection
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
     * @param meth
     *            the method
     */
    private void checkForFileNestingDepthMax(de.ovgu.skunk.detection.data.File file) {
        if (this.config.File_NestingDepthMin != -1) {
            // check nesting via stacks and nesting depth
            Stack<FeatureReference> nestingStack = new Stack<FeatureReference>();
            int beginNesting = -1;

            for (UUID id : file.featureConstants.keySet()) {
                FeatureReference constant = FeatureExpressionCollection
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
                            if ((current.nestingDepth - beginNesting) > ndm)
                                ndm = current.nestingDepth - beginNesting;

                        // if the ndm of the bundle is higher than the
                        // configuration add all to the result
                        if (ndm >= config.File_NestingDepthMin) {
                            while (!nestingStack.isEmpty())
                                this.addFeatureLocWithReason(nestingStack.pop(),
                                        SmellReason.ANNOTATIONFILE_NUMBERNESTINGDEPTHMIN);
                        } else
                            nestingStack.empty();
                    }
                }
            }

            // final emptiing if something is left
            if (!nestingStack.isEmpty()) {
                // calculate nestingdepth of bundle
                int ndm = -1;
                for (FeatureReference current : nestingStack)
                    if ((current.nestingDepth - beginNesting) > ndm)
                        ndm = current.nestingDepth - beginNesting;

                if (ndm >= config.File_NestingDepthMin) {
                    while (!nestingStack.isEmpty())
                        this.addFeatureLocWithReason(nestingStack.pop(),
                                SmellReason.ANNOTATIONFILE_NUMBERNESTINGDEPTHMIN);
                } else
                    nestingStack.empty();
            }
        }
    }

    /**
     * Check if the feature constant is bigger than the mean value of feature
     * lofc Indicates a large feature.
     *
     * @param loc
     *            the feature constant to examine
     */
    private void checkForFeatureToFeatureRatio(FeatureReference loc) {
        // if the value is not set, it is -1000
        if (this.config.Feature_MeanLofcRatio != -1000) {
            // calculate the minimal lofc a feature location should have to be
            // considered big
            int lofc = (loc.end - loc.start);
            double minLofc = (this.config.Feature_MeanLofcRatio * FeatureExpressionCollection.GetMeanLofc());

            // add the feature location if the feature lofc is bigger than the
            // minimal
            if (lofc >= minLofc)
                this.addFeatureLocWithReason(loc, SmellReason.LARGEFEATURE_LOFCTOMEANLOFC);
        }
    }

    /**
     * Check if the feature takes up a huge percentage of the whole project.
     * Indicates a large feature.
     *
     * @param feat
     *            the feature
     * @param loc
     *            the current location
     */
    private void checkForFeatureToProjectRatio(Feature feat, FeatureReference loc) {
        // value is set if it is not -1000
        if (this.config.Feature_ProjectLocRatio != -1000) {
            // calculate the minimal lofc the feature must have to be a large
            // feature
            double minLofc = (FeatureExpressionCollection.GetLoc() * this.config.Feature_ProjectLocRatio);

            // add the feature location
            if (feat.getLofc() >= minLofc)
                this.addFeatureLocWithReason(loc, SmellReason.LARGEFEATURE_LOFCTOLOC);
        }
    }

    /**
     * Check if the feature has more constants than ratio amount. If yes, add
     * all locs to the result with the corresponding reason.
     *
     * @param feat
     *            the feat
     */
    private void checkForFeatureNoFeatureConstantsToSum(Feature feat) {
        if (this.config.Feature_NoFeatureConstantsRatio != -1000) {
            // amount of nofls the feature has to exceed for a smell
            double minNofl = FeatureExpressionCollection.numberOfFeatureConstantReferences
                    * this.config.Feature_NoFeatureConstantsRatio;

            if (feat.getConstants().size() > minNofl) {
                for (FeatureReference loc : feat.getConstants())
                    this.addFeatureLocWithReason(loc, SmellReason.SHOTGUNSURGERY_NOFCOSUMNOFC);
            }
        }
    }

    /**
     * Check if the feature exceeds the configuration value for compilation
     * units. If yes, add all constants with the corresponding reason to the
     * result.
     *
     * @param feat
     *            the feat
     */
    private void checkForFeatureCompilUnits(Feature feat) {
        if (this.config.Feature_NumberOfCompilUnits != -1) {
            if (feat.GetAmountCompilationFiles() > this.config.Feature_NumberOfCompilUnits) {
                for (FeatureReference loc : feat.getConstants())
                    this.addFeatureLocWithReason(loc, SmellReason.SHOTGUNSURGERY_NUMBERCOMPILATIONUNITS);
            }
        }
    }

    /**
     * Checks if the feature exceeds the threshold for lofc.
     *
     * @param feat
     *            the feat
     */
    private void checkForFeatureLofc(Feature feat) {
        if (this.config.Feature_NumberLofc != -1) {
            if (feat.getLofc() > this.config.Feature_NumberLofc) {
                for (FeatureReference loc : feat.getConstants())
                    this.addFeatureLocWithReason(loc, SmellReason.LARGEFEATURE_NUMBERLOFC);
            }
        }
    }

    /**
     * Checks if the feature exceeds the threshold for nofc.
     *
     * @param feat
     *            the feat
     */
    private void checkForFeatureNofc(Feature feat) {
        if (this.config.Feature_NumberNofc != -1) {
            if (feat.constants.size() > this.config.Feature_NumberNofc) {
                for (FeatureReference loc : feat.getConstants())
                    this.addFeatureLocWithReason(loc, SmellReason.LARGEFEATURE_NUMBERNOFC);
            }
        }
    }

    /**
     * Adds the feature constant to the result list with the specified reason,
     * or appends another reason if the location is already inside the result
     * list.
     *
     * @param constant
     *            the feature constant to add
     * @param reason
     *            the reason
     */
    private void addFeatureLocWithReason(FeatureReference constant, SmellReason reason) {
        if (this.featureResult.containsKey(constant))
            this.featureResult.get(constant).add(reason);
        else {
            List<SmellReason> enumReason = new ArrayList<SmellReason>();
            enumReason.add(reason);
            this.featureResult.put(constant, enumReason);
        }
    }

    /**
     * Sort a hashmap by values
     *
     * @param <K>
     *            the key type
     * @param <V>
     *            the value type
     * @param map
     *            the map
     * @return the linked hash map
     */
    public <K extends Comparable<K>, V extends Comparable<V>> LinkedHashMap<K, V> sortByValues(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {

            @Override
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        // LinkedHashMap will keep the keys in the order they are inserted
        // which is currently sorted on natural ordering
        LinkedHashMap<K, V> sortedMap = new LinkedHashMap<K, V>();

        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
