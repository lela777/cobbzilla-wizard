package org.cobbzilla.wizard.dao;

import lombok.Getter;
import org.apache.commons.collections.Transformer;
import org.cobbzilla.util.collection.FieldTransfomer;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;
import org.cobbzilla.wizard.validation.UniqueValidatorDaoHelper;

import java.util.Map;

public abstract class UniquelyNamedEntityDAO<E extends UniquelyNamedEntity> extends AbstractUniqueCRUDDAO<E> {

    public static final Transformer TO_NAME = new FieldTransfomer("name");

    public boolean forceLowercase () { return getEntityProto().forceLowercase(); }

    public E findByName (String name) { return findByUniqueField("name", nameValue(name)); }

    protected String nameValue(String name) { return forceLowercase() ? name.toLowerCase() : name; }

    protected Map<String, UniqueValidatorDaoHelper.Finder<E>> getUniqueHelpers() {
        return MapBuilder.build(new Object[][]{
            {"name", new UniqueValidatorDaoHelper.Finder<E>() { @Override public E find(Object query) { return findByName(query.toString()); } }}
        });
    }
}
