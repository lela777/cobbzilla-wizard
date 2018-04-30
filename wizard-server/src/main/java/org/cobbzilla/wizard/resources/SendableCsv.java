package org.cobbzilla.wizard.resources;

import org.cobbzilla.util.http.HttpContentTypes;
import org.cobbzilla.wizard.util.CsvStreamingOutput;

import java.util.Collection;

public class SendableCsv extends SendableResource {

    public SendableCsv(Collection rows, String[] fields) {
        super(new CsvStreamingOutput(rows, fields));
    }

    @Override public String getContentType() { return HttpContentTypes.TEXT_CSV; }
    @Override public Boolean getForceDownload() { return true; }

}
