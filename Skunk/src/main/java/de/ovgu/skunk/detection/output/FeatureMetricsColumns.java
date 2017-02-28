/**
 *
 */
package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.Context;
import de.ovgu.skunk.detection.data.Feature;

/**
 * Columns and value providers for the &quot;features&quot; CSV output file
 *
 * @author wfenske
 */
public enum FeatureMetricsColumns implements CsvColumnValueProvider<Feature, Context> {
    Name {
        @Override
        public Object csvColumnValue(Feature f, Context ctx) {
            return f.Name;
        }
    },
    LGSmell {
        @Override
        public Float csvColumnValue(Feature f, Context ctx) {
            // # featureConstants/#TotalLocations
            float constSmell = (float) FeatureMetricsColumns.ConstantsSmell.csvColumnValue(f, ctx);
            // LOFC/TotalLoc
            float lofcSmell = (float) FeatureMetricsColumns.LOFCSmell.csvColumnValue(f, ctx);
            float sumLg = constSmell + lofcSmell;
            return sumLg;
        }
    },
    SSSmell {
        @Override
        public Object csvColumnValue(Feature f, Context ctx) {
            float constSmell = (float) FeatureMetricsColumns.ConstantsSmell.csvColumnValue(f, ctx);
            // CompilUnit/MaxCompilUnits
            float compilUnitsSmell = (float) FeatureMetricsColumns.CUSmell.csvColumnValue(f, ctx);
            float sumSS = constSmell + compilUnitsSmell;
            return sumSS;
        }
    },
    ConstantsSmell {
        @Override
        public Float csvColumnValue(Feature f, Context ctx) {
            float constSmell = ctx.config.Feature_NumberNofc_Weight * (((float) f.getReferences().size())
                    / (ctx.featureExpressions.numberOfFeatureConstantReferences));
            return constSmell;
        }
    },
    LOFCSmell {
        @Override
        public Float csvColumnValue(Feature f, Context ctx) {
            float lofcSmell = ctx.config.Feature_NumberLofc_Weight
                    * (((float) f.getLofc()) / (ctx.featureExpressions.GetLoc()));
            return lofcSmell;
        }
    },
    CUSmell {
        @Override
        public Float csvColumnValue(Feature f, Context ctx) {
            float compilUnitsSmell = (f.compilationFiles.size()) / ((float) ctx.files.AllFiles().size());
            return compilUnitsSmell;
        }
    },
    NOFC {
        @Override
        public Integer csvColumnValue(Feature f, Context ctx) {
            return f.getReferences().size();
        }
    },
    /**
     * Number of times any feature constant has been mentioned
     */
    MAXNOFC {
        @Override
        public Object csvColumnValue(Feature f, Context ctx) {
            return ctx.featureExpressions.numberOfFeatureConstantReferences;
        }
    },
    LOFC {
        @Override
        public Object csvColumnValue(Feature f, Context ctx) {
            return f.getLofc();
        }
    },
    ProjectLOC {
        @Override
        public Object csvColumnValue(Feature f, Context ctx) {
            return ctx.featureExpressions.GetLoc();
        }
    },
    NOCU {
        @Override
        public Object csvColumnValue(Feature f, Context ctx) {
            return f.compilationFiles.size();
        }
    };
}
