package de.ovgu.skunk.detection.output;

/**
 * Helps marshall a Java object into its representation as a CSV row.  Specifically, the object is converted into an array of objects, which are later converted into <code>String</code>s using #toString() and then CSV-escaped by some CSV library.
 *
 * @param <TInput>
 * @param <TContext>
 * @param <TEnum>
 * @see CsvColumnValueProvider the interface that the enum must implement
 * @see CsvFileWriterHelper a class whose implementation goes together well with this one
 * @see org.apache.commons.csv.CSVPrinter
 */
public class CsvRowProvider<TInput, TContext, TEnum extends Enum<?> & CsvColumnValueProvider<TInput, TContext>> {
    private final Class<? extends TEnum> columnsClass;
    private final TContext ctx;

    public CsvRowProvider(Class<? extends TEnum> columnsClass, TContext ctx) {
        this.columnsClass = columnsClass;
        this.ctx = ctx;
    }

    public Object[] headerRow() {
        return CsvEnumUtils.headerRow(columnsClass);
    }

    /**
     * Convert the given object into a list of column values for serialization into a CSv file
     *
     * @param o The input object
     * @return An array of objects, one for each column of the resulting CSV file
     */
    public Object[] dataRow(TInput o) {
        return CsvEnumUtils.dataRow(columnsClass, o, ctx);
    }
}
