package de.ovgu.skunk.detection.output;

import de.ovgu.skunk.detection.data.Context;

public interface CsvColumnValueProvider<T> {
    /**
     * Returns the value of object <code>o</code> in the CSV file column
     *
     * @param o   an object
     * @param ctx the context holding the smell detection configuration and other stuff
     * @return the value of the given object according to the smell detection
     * configuration
     */
    Object csvColumnValue(T o, Context ctx);
}
