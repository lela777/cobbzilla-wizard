package org.cobbzilla.wizard.util;

import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor @ToString(callSuper=false)
public class RestResponse {

    public int status;
    public String json;
    public String location;

    public String getLocationUuid () { return location.substring(location.lastIndexOf("/")+1); }

    public boolean isSuccess () { return status/100 == 2; }

}
