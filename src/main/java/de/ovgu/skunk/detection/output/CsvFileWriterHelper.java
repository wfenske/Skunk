package de.ovgu.skunk.detection.output;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by wfenske on 28.02.17.
 */
public abstract class CsvFileWriterHelper {
    public void write(File file) {
        FileWriter writer = null;
        final String fileName = file.getPath();
        CSVPrinter csv = null;
        try {
            writer = new FileWriter(file);
            try {
                csv = new CSVPrinter(writer, CSVFormat.EXCEL);
                actuallyDoStuff(csv);
            } catch (Exception e) {
                throw new RuntimeException("Error writing CSV file `" + fileName + "'", e);
            } finally {
                try {
                    writer.flush();
                    writer.close();
                    if (csv != null) csv.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error closing CSV printer for file `" + fileName + "'", e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing CSV file `" + fileName + "'", e);
        }
    }

    public void write(String fileName) {
        write(new File(fileName));
    }

    protected abstract void actuallyDoStuff(CSVPrinter csv) throws IOException;
}
