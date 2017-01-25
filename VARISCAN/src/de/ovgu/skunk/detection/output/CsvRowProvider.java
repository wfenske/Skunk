package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.Context;

public class CsvRowProvider<TInput, TEnum extends Enum<?> & CsvColumnValueProvider<TInput>> {
    private final Class<? extends TEnum> columnsClass;
    private final Context ctx;

    public CsvRowProvider(Class<? extends TEnum> columnsClass, Context ctx) {
        this.columnsClass = columnsClass;
        this.ctx = ctx;
    }

    public Object[] headerRow() {
        return CsvEnumUtils.headerRow(columnsClass);
    }

    public Object[] dataRow(TInput o) {
        return CsvEnumUtils.dataRow(columnsClass, o, ctx);
    }
}
