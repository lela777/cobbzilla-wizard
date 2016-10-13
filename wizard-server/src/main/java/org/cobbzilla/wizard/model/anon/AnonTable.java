package org.cobbzilla.wizard.model.anon;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Accessors(chain = true) @ToString(of="table")
public class AnonTable {

    @Getter @Setter private String table;
    @Getter @Setter private String id = "uuid";
    @Getter @Setter private AnonColumn[] columns;
    @Getter @Setter private boolean truncate = false;

    public List<String> getColumnNames () {
        final List<String> names = new ArrayList<>();
        for (AnonColumn column : columns) names.add(column.getName());
        return names;
    }

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

    public void retainColumns(Set<String> retain) {
        int removed = 0;
        for (int i=0; i<columns.length; i++) {
            if (!retain.contains(columns[i].getName())) {
                columns[i] = null;
                removed++;
            }
        }
        final AnonColumn[] newColumns = new AnonColumn[columns.length-removed];
        int newIndex = 0;
        for (AnonColumn column : columns) {
            if (column == null) continue;
            newColumns[newIndex++] = column;
        }
        columns = newColumns;
    }

}
