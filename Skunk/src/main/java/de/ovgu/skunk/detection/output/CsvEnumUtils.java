/**
 *
 */
package de.ovgu.skunk.detection.output;

/**
 * Static selection of functions on Enumerations that model CSV files
 *
 * @author wfenske
 */
public class CsvEnumUtils {
    /*
     * not meant to be instantiated
     */
    private CsvEnumUtils() {
    }

    public static <TEnum extends Enum<?>> Object[] headerRow(Class<? extends TEnum> columnsClass) {
        TEnum[] enumConstants = columnsClass.getEnumConstants();
        if (enumConstants == null) throw new IllegalArgumentException("Not an enum type: " + columnsClass);
        final int len = enumConstants.length;
        Object[] r = new Object[len];
        for (int i = 0; i < len; i++) {
            TEnum e = enumConstants[i];
            r[i] = e.name();
        }
        return r;
    }

    public static <TEnum extends Enum<?>> String[] headerRowStrings(Class<? extends TEnum> columnsClass) {
        Object[] names = headerRow(columnsClass);
        String[] result = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = names[i].toString();
        }
        return result;
    }

    /**
     * Convert the given object, <code>o</code>, into a list of column values for serialization into a CSv file
     *
     * @param columnsClass
     * @param o
     * @param ctx
     * @param <TInput>
     * @param <TEnum>
     * @return An array of objects, one for each column of the resulting CSV file
     */
    public static <TInput, TContext, TEnum extends Enum<?> & CsvColumnValueProvider<TInput, TContext>> Object[] dataRow(
            Class<? extends TEnum> columnsClass, TInput o, TContext ctx) {
        TEnum[] enumConstants = columnsClass.getEnumConstants();
        if (enumConstants == null) throw new IllegalArgumentException("Not an enum type: " + columnsClass);
        final int len = enumConstants.length;
        Object[] r = new Object[len];
        for (int i = 0; i < len; i++) {
            TEnum e = enumConstants[i];
            Object value = e.csvColumnValue(o, ctx);
            r[i] = value;
        }
        return r;
    }
}
