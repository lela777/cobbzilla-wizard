package org.cobbzilla.wizard.util;

import com.opencsv.CSVWriter;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.reflect.ReflectionUtil;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

import static org.apache.commons.lang3.StringEscapeUtils.escapeCsv;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@AllArgsConstructor @Slf4j
public class CsvStreamingOutput implements StreamingOutput {

    private final Collection rows;
    private final String[] fields;

    @Override public void write(OutputStream out) throws IOException, WebApplicationException {

        if (empty(fields)) die("write: no fields specified");

        @Cleanup CSVWriter writer = new CSVWriter(new OutputStreamWriter(out));

        final List<Map<String, Object>> data = new ArrayList<>();
        final Set<String> columns = new HashSet<>();
        for (Object row : rows) {
            final Map<String, Object> map = new HashMap<>();
            for (String field : fields) {
                map.put(field, ReflectionUtil.get(row, field));
            }
            data.add(map);
            columns.addAll(map.keySet());
        }

        final String[] fieldNames = columns.toArray(new String[columns.size()]);
        writer.writeNext(fieldNames); // header row
        for (Map<String, Object> row : data) {
            final String[] line = new String[fieldNames.length];
            for (int i = 0; i < line.length; i++) {
                final String field = fieldNames[i];
                final Object value = row.get(field);
                line[i] = empty(value) ? "" : escapeCsv(value.toString());
            }
            writer.writeNext(line, false);
        }
    }

}
