/**
 *
 */
package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.Context;

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

    public static <TInput, TEnum extends Enum<?> & CsvColumnValueProvider<TInput>> Object[] dataRow(
            Class<? extends TEnum> columnsClass, TInput o, Context ctx) {
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
