package org.cobbzilla.wizard.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.json.JsonUtil;
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

@Slf4j
public class CsvStreamingOutput implements StreamingOutput {

    private final String[] fields;
    private final Collection rows;
    private String[] header;

    public CsvStreamingOutput(Collection rows, String[] fields, String[] header) {
        this.rows = rows;
        this.fields = fields;
        this.header = header;
    }

    public CsvStreamingOutput(Collection rows, String[] fields) {
        this(rows, fields, null);
    }

    @Override public void write(OutputStream out) throws IOException, WebApplicationException {

        if (empty(fields)) die("write: no fields specified");

        @Cleanup CSVWriter writer = new CSVWriter(new OutputStreamWriter(out));

        final List<Map<String, Object>> data = new ArrayList<>();
        final boolean defaultHeaders = empty(header);
        final Set<String> columns = defaultHeaders ? new HashSet<>() : null;
        for (Object row : rows) {
            final Map<String, Object> map = new HashMap<>();
            for (String field : fields) {
                if (field.contains("::")){
                    String[] path = field.split("::");
                    final JsonNode node = JsonUtil.json((String) ReflectionUtil.get(row, path[0], null),
                                                        JsonNode.class);
                    map.put(field, JsonUtil.json(JsonUtil.getNodeAsJava(node, path[1])));
                } else {
                    map.put(field, ReflectionUtil.get(row, field, null));
                }
            }
            data.add(map);
            if (defaultHeaders) columns.addAll(map.keySet());
        }

        if (defaultHeaders) header = columns.toArray(new String[columns.size()]);
        writer.writeNext(header); // header row

        for (Map<String, Object> row : data) {
            final String[] line = new String[fields.length];
            for (int i = 0; i < line.length; i++) {
                final String field = fields[i];
                final Object value = row.get(field);
                line[i] = empty(value) ? "" : escapeCsv(value.toString());
            }
            writer.writeNext(line, false);
        }
    }

}
