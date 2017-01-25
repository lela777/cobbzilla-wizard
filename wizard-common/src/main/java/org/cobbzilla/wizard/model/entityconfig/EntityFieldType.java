package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EntityFieldType {

    /** a string of characters */
    string,

    /** an integer-valued number */
    integer,

    /** a real number */
    decimal,

    /** an integer-valued monetary amount */
    money_integer,

    /** a real-valued monetary amount */
    money_decimal,

    /** a boolean value */
    flag,

    /** a date value */
    date,

    /** a date or date/time value, represented as milliseconds since 1/1/1970 */
    epoch_time,

    /** a reference to another EntityConfig instance */
    reference,

    /** a base64-encoded PNG image  */
    base64_png,

    /** an embedded sub-object */
    embedded;

    /** Jackson-hook to create a new instance based on a string, case-insensitively */
    @JsonCreator public static EntityFieldType create (String val) { return valueOf(val.toLowerCase()); }

    public Object toObject(String value) {
        switch (this) {
            case decimal: return Double.parseDouble(value);

            case integer: case epoch_time: return Long.parseLong(value);

            case flag: return Boolean.valueOf(value);

            case string: case date: case reference: case base64_png: case embedded:
            default: return value;
        }
    }
}
