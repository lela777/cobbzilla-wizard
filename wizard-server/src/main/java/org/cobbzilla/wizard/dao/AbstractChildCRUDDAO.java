package org.cobbzilla.wizard.dao;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.ChildEntity;
import org.cobbzilla.wizard.model.ChildEntity;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractChildCRUDDAO<C extends ChildEntity, P>
        extends AbstractDAO<C>
        implements AbstractCRUDDAOBase<C> {

    @Autowired protected SessionFactory sessionFactory;

    private final Class<P> parentEntityClass;

    public AbstractChildCRUDDAO(Class<P> parentEntityClass) {
        this.parentEntityClass = parentEntityClass;
    }

    public C find(Long id) { return get(id); }

    public C findByUuid(String uuid) {
        return uniqueResult(Restrictions.eq("uuid", uuid));
    }

    public C findByUniqueField(String field, Object value) {
        return uniqueResult(Restrictions.eq(field, value));
    }

    public List<C> findByParent(Long parentId) {
        final String queryString = "from " + getEntityClass().getSimpleName() + " x where x." + parentEntityClass.getSimpleName().toLowerCase() + ".id=? order by x.ctime";
        return hibernateTemplate.find(queryString, parentId);
    }

    public List<C> findByParentUuid(String parentUuid) {
        final String queryString = "from " + getEntityClass().getSimpleName() + " x where x." + parentEntityClass.getSimpleName().toLowerCase() + ".uuid=? order by x.ctime";
        return hibernateTemplate.find(queryString, parentUuid);
    }

    public Map<String, C> mapChildrenOfParentByUuid(String parentUuid) {
        return mapChildrenOfParentByUuid(findByParentUuid(parentUuid));
    }

    public Map<String, C> mapChildrenOfParentByUuid(List<C> recordList) {
        Map<String, C> records = new HashMap<>(recordList.size());
        for (C record : recordList) {
            records.put(record.getUuid(), record);
        }
        return records;
    }

    private P findParentByUuid(String parentId) {
        return (P) uniqueResult(criteria(parentEntityClass).add(Restrictions.eq("uuid", parentId)));
    }

    //    @Transactional
    public C create(String parentUuid, @Valid C child) {
        P parent = findParentByUuid(parentUuid);
        child.setParent(checkNotNull(parent));
        child.beforeCreate();
        child.setId((Long) hibernateTemplate.save(checkNotNull(child)));
        return child;
    }

//    @Transactional
    public C update(@Valid C child) {
        hibernateTemplate.update(checkNotNull(child));
        return child;
    }

//    @Transactional
    public void delete(Long id) {
        C found = get(checkNotNull(id));
        if (found != null) {
            hibernateTemplate.delete(found);
        }
    }

//    @Transactional
    public void delete(String uuid) {
        delete(checkNotNull(findByUuid(uuid).getId()));
    }

}
