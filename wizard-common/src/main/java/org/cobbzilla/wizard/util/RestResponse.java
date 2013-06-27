package org.cobbzilla.wizard.util;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RestResponse {

    public int status;
    public String json;
    public String location;

    public String getLocationUuid () { return location.substring(location.lastIndexOf("/")+1); }

}
