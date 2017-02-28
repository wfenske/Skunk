package de.ovgu.skunk.detection.output;

/**
 * Used to serialize an input object into its CSV representation.  The idea is that each input object is turned into
 * one row in a CSV file.  This interface here is meant to extract the object's value for a specific column within this
 * CSV row.
 *
 * @param <TInput>   Type of the input object
 * @param <TContext> Type of some additional context that is passed into the enum on each call
 */
public interface CsvColumnValueProvider<TInput, TContext> {
    /**
     * Returns the value of an input object <code>o</code> in the CSV file column
     *
     * @param o   an object
     * @param ctx the context holding the smell detection configuration and other stuff
     * @return the value of the given object according to the smell detection
     * configuration
     */
    Object csvColumnValue(TInput o, TContext ctx);
}
