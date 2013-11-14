package org.cobbzilla.wizard.thrift;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;

import java.util.ArrayList;
import java.util.List;

public abstract class ThriftCRUDHandlerBase<E extends Identifiable, T extends TBase> {

    protected abstract DAO<E> dao ();

    public List<T> findAll() throws TException {
        return toThrift(dao().findAll());
    }

    protected List<T> toThrift(List<E> list) {
        List<T> thrifts = new ArrayList<>(list.size());
        for (E entity : list) thrifts.add(toThrift(entity));
        return thrifts;
    }

    protected abstract T toThrift(E entity);
    protected abstract E fromThrift(T thrift);

    public List<T> findPage(tResultPage page) throws TException {
        return toThrift(dao().search(fromThrift(page)));
    }

    public T findByUuid(String uuid) throws TException {
        return toThrift(dao().findByUuid(uuid));
    }

    public T create(T patron) throws TException {
        return toThrift(dao().create(fromThrift(patron)));
    }

    public T update(T patron) throws TException {
        return toThrift(dao().update(fromThrift(patron)));
    }

    public void remove(String uuid) throws TException {
        dao().delete(uuid);
    }

    private ResultPage fromThrift(tResultPage page) {
        ResultPage r = new ResultPage();
        try {
            BeanUtils.copyProperties(r, page);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }

}
