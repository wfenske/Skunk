package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.Context;
import de.ovgu.skunk.detection.data.Method;

/**
 * Columns and value providers for the &quot;methods&quot; CSV output file
 *
 * @author wfenske
 */
public enum MethodMetricsColumns implements CsvColumnValueProvider<Method, Context> {
    FILE {
        @Override
        public String csvColumnValue(Method m, Context ctx) {
            //return m.FilePathForDisplay();
            return m.ProjectRelativeFilePath();
        }
    },
    Start {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.start1;
        }
    },
    FUNCTION_SIGNATURE {
        @Override
        public String csvColumnValue(Method m, Context ctx) {
            return m.functionSignatureXml;
        }
    },
    ABSmell {
        @Override
        public Float csvColumnValue(Method m, Context ctx) {
            float featLocSmell = (float) MethodMetricsColumns.LocationSmell.csvColumnValue(m, ctx);
            // #Constants/#FeatLocs
            float featConstSmell = (float) MethodMetricsColumns.ConstantsSmell.csvColumnValue(m, ctx);
            // Loac/Loc * #FeatLocs
            float nestSumSmell = (float) MethodMetricsColumns.NestingSmell.csvColumnValue(m, ctx);
            float sum = (featLocSmell + featConstSmell + nestSumSmell);
            return sum;
        }
    },
    LocationSmell {
        @Override
        public Float csvColumnValue(Method m, Context ctx) {
            float featLocSmell = ctx.config.Method_LoacToLocRatio_Weight
                    * (((float) m.GetLinesOfAnnotatedCode() / (float) m.loc) * m.numberFeatureLocations);
            return featLocSmell;
        }
    },
    ConstantsSmell {
        @Override
        public Float csvColumnValue(Method m, Context ctx) {
            float featConstSmell = ctx.config.Method_NumberOfFeatureConstants_Weight
                    * ((float) m.GetFeatureConstantCount() / (float) m.numberFeatureLocations);
            return featConstSmell;
        }
    },
    NestingSmell {
        @Override
        public Float csvColumnValue(Method m, Context ctx) {
            float nestSumSmell = ctx.config.Method_NestingSum_Weight
                    * ((float) m.nestingSum / (float) m.numberFeatureLocations);
            return nestSumSmell;
        }
    },
    LOC {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.loc;
        }
    },
    LOAC {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.GetLinesOfAnnotatedCode();
        }
    },
    LOFC {
        @Override
        public Long csvColumnValue(Method m, Context ctx) {
            return m.lofc;
        }
    },
    NOFL {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.numberFeatureLocations;
        }
    },
    NOFC_Dup {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.GetFeatureConstantCount();
        }
    },
    NOFC_NonDup {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.numberFeatureConstantsNonDup;
        }
    },
    NONEST {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.nestingSum;
        }
    };
}
