package io.eel.common.model.query_result;

import com.opencsv.CSVWriter;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class QueryResultUtils {

    private final static Logger log = Logger.getLogger(QueryResultUtils.class.getName());

    public static byte[] convertResultSetToCsvBytes(ResultSet rs) {
        try (StringWriter sw = new StringWriter();
             CSVWriter writer = new CSVWriter(sw)) {

            // writeAll(rs, includeHeaders) handles metadata automatically
            writer.writeAll(rs, true);

            return sw.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Throwable t) {
            log.severe(t.getMessage());
            t.printStackTrace();

            throw new RuntimeException(t);
        }
    }

}
