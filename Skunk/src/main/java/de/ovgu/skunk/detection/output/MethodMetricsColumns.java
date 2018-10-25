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

        @Override
        public String parseCsvColumnValue(String value) {
            return value;
        }
    },
    Start {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.start1;
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    },
    FUNCTION_SIGNATURE {
        @Override
        public String csvColumnValue(Method m, Context ctx) {
            return m.functionSignatureXml;
        }

        @Override
        public String parseCsvColumnValue(String value) {
            return value;
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

        @Override
        public Float parseCsvColumnValue(String value) {
            return Float.valueOf(value);
        }
    },
    LocationSmell {
        @Override
        public Float csvColumnValue(Method m, Context ctx) {
            float featLocSmell = ctx.config.Method_LoacToLocRatio_Weight
                    * (((float) m.GetLinesOfAnnotatedCode() / (float) m.getNetLoc()) * m.numberFeatureLocations);
            return featLocSmell;
        }

        @Override
        public Float parseCsvColumnValue(String value) {
            return Float.valueOf(value);
        }
    },
    ConstantsSmell {
        @Override
        public Float csvColumnValue(Method m, Context ctx) {
            float featConstSmell = ctx.config.Method_NumberOfFeatureConstants_Weight
                    * ((float) m.GetFeatureConstantCount() / (float) m.numberFeatureLocations);
            return featConstSmell;
        }

        @Override
        public Float parseCsvColumnValue(String value) {
            return Float.valueOf(value);
        }
    },
    NestingSmell {
        @Override
        public Float csvColumnValue(Method m, Context ctx) {
            float nestSumSmell = ctx.config.Method_NestingSum_Weight
                    * ((float) m.nestingSum / (float) m.numberFeatureLocations);
            return nestSumSmell;
        }

        @Override
        public Float parseCsvColumnValue(String value) {
            return Float.valueOf(value);
        }
    },
    LOC {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.getNetLoc();
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    },
    LOAC {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.GetLinesOfAnnotatedCode();
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    },
    LOFC {
        @Override
        public Long csvColumnValue(Method m, Context ctx) {
            return m.lofc;
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    },
    NOFL {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.numberFeatureLocations;
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    },
    NOFC_Dup {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.GetFeatureConstantCount();
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    },
    NOFC_NonDup {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.numberFeatureConstantsNonDup;
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    },
    NONEST {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.nestingSum;
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    },
    /**
     * Number of negated feature constants
     */
    NONEG {
        @Override
        public Integer csvColumnValue(Method m, Context ctx) {
            return m.negationCount;
        }

        @Override
        public Integer parseCsvColumnValue(String value) {
            return Integer.valueOf(value);
        }
    };

    public abstract <T> T parseCsvColumnValue(String value);

    /**
     * Basename of the CSV file that will hold this information.  It will be located within the results directory, in a
     * project- and snapshot-specific directory, such as <code>results/busybox/2000-04-08</code>
     */
    public static final String FILE_BASENAME = "ABRes.csv";
}
