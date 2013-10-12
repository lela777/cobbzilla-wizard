package org.cobbzilla.wizard.dao;

import java.util.List;

public class DAOUtil {

    public static <E> E uniqueResult(List<E> found) {
        if (found == null || found.size() == 0) return null;
        if (found.size() > 1) throw new DAOException("nonunique result: "+found);
        return found.get(0);
    }

}
