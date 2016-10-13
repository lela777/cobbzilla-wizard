package org.cobbzilla.wizard.model.anon;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Accessors(chain = true) @ToString(of="table")
public class AnonTable {

    @Getter @Setter private String table;
    @Getter @Setter private String id = "uuid";
    @Getter @Setter private AnonColumn[] columns;
    @Getter @Setter private boolean truncate = false;

    public static AnonTable table(String table, AnonColumn... columns) {
        return new AnonTable().setTable(table).setColumns(columns);
    }

    public String sqlSelect() {
        final StringBuilder b = new StringBuilder();
        for (AnonColumn col : columns) {
            if (b.length() > 0) b.append(", ");
            b.append(col.getName());
        }
        return "SELECT "+ getId()+", " + b.toString() + " FROM " + table;
    }

    public String sqlUpdate() {
        if (isTruncate()) {
            return "TRUNCATE TABLE "+table;

        } else {
            final StringBuilder b = new StringBuilder();
            for (AnonColumn col : columns) {
                if (b.length() > 0) b.append(", ");
                b.append(col.getName()).append(" = ?");
            }
            return "UPDATE " + table + " SET " + b.toString() + " WHERE "+ getId()+" = ?";
        }
    }

}
