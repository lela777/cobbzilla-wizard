package org.cobbzilla.wizard.util;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor @ToString(callSuper=false)
public class RestResponse {

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

}
