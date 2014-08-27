package org.cobbzilla.wizard.thrift;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
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

//    public SearchResults<T> findPage(tResultPage page) throws TException {
//        final SearchResults<E> results = dao().search(fromThrift(page));
//        return toThrift(results);
//    }

    public T findByUuid(String uuid) throws TException {
        return toThrift(dao().findByUuid(uuid));
    }

    public T create(T thriftThing) throws TException, tValidationException {
        try {
            return toThrift(dao().create(fromThrift(thriftThing)));
        } catch (ConstraintViolationException e) {
            throw toThrift(e);
        }
    }

    public T update(T thriftThing) throws TException, tValidationException {
        return toThrift(dao().update(fromThrift(thriftThing)));
    }

    public void remove(String uuid) throws TException {
        dao().delete(uuid);
    }

    protected ResultPage fromThrift(tResultPage page) {
        ResultPage r = new ResultPage();
        try {
            BeanUtils.copyProperties(r, page);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    protected tValidationException toThrift(ConstraintViolationException e) {
        tValidationException validationException = new tValidationException();
        final List<tValidationFailure> errors = new ArrayList<>();
        for (ConstraintViolation v : e.getConstraintViolations()) {
            tValidationFailure failure = new tValidationFailure();
            failure.setMessageTemplate(v.getMessageTemplate());
            failure.setMessage(v.getMessage());
            if (v.getInvalidValue() != null) failure.setInvalidValue(v.getInvalidValue().toString());
            errors.add(failure);
        }
        validationException.setErrors(errors);
        return validationException;
    }

}
