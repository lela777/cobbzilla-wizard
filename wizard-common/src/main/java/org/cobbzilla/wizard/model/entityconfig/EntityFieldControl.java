package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Specifies which kind of UI control should be used.
 */
public enum EntityFieldControl {

    /** a standard text input field */
    text,

    /** a multi-line text input field */
    textarea,

    /** a large multi-line text input field */
    big_textarea,

    /** a yes/no field that also supports a 'no selection made' state */
    flag,

    /** select one item from a list */
    select,

    /** select multiple items from a list */
    multi_select,

    /** a date field (calendar) */
    date,

    /** a date and time field (calendar + time selection) */
    date_and_time,

    /** an auto-complete text field (WIP) */
    autocomplete,

    /** a hidden field (do not display to user) */
    hidden;

    /** Jackson-hook to create a new instance based on a string, case-insensitively */
    @JsonCreator public static EntityFieldControl create (String val) { return valueOf(val.toLowerCase()); }

}
