package org.cobbzilla.wizard.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.cobbzilla.util.io.TempDir;

import javax.persistence.Transient;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.io.FileUtil.toFileOrDie;

@AllArgsConstructor @EqualsAndHashCode(of={"status","json","location"})
public class RestResponse {

    public static Integer defaultWriteToFileLimit = 500;
    public static File defaultLogDir = new TempDir();

    @JsonIgnore @Transient public Integer writeToFileLimit;
    @JsonIgnore @Transient public File logDir;

    public int status;
    public String json;
    public String location;

    public RestResponse(int status) { this.status = status; }

    public RestResponse(int statusCode, String responseJson, String locationHeader) {
        this.status = statusCode;
        this.json = responseJson;
        this.location = locationHeader;
    }

    public String getLocationUuid () { return location.substring(location.lastIndexOf("/")+1); }

    public boolean isSuccess () { return isSuccess(status); }
    public boolean isInvalid () { return isInvalid(status); }

    public static boolean isSuccess (int code) { return code/100 == 2; }
    public static boolean isInvalid (int code) { return code == 422; }

    public List<RestResponseHeader> headers;
    public void addHeader(String name, String value) {
        if (headers == null) headers = new ArrayList<>();
        headers.add(new RestResponseHeader(name, value));
    }
    public String header (String name) {
        if (headers != null) {
            for (RestResponseHeader header : headers) {
                if (header.getName().equalsIgnoreCase(name)) return header.getValue();
            }
        }
        return null;
    }
    public int intHeader (String name) { return Integer.parseInt(header(name)); }

    @Override public String toString() {
        File jsonFile;
        String displayJson = json;
        if ((writeToFileLimit != null && json.length() > writeToFileLimit)
            || (defaultWriteToFileLimit != null && json.length() > defaultWriteToFileLimit)) {
            int limit = writeToFileLimit != null ? writeToFileLimit : defaultWriteToFileLimit;
            jsonFile = new File(logDir != null ? logDir : defaultLogDir, "restResponse"+hashCode()+".json");
            if (!jsonFile.exists()) toFileOrDie(jsonFile, json);
            displayJson = json.substring(0, limit) + " ... (full JSON: "+jsonFile+")";
        }
        return "RestResponse{" +
                "status=" + status +
                ", json='" + displayJson + '\'' +
                ", location='" + location + '\'' +
                ", headers=" + headers +
                '}';
    }
}
