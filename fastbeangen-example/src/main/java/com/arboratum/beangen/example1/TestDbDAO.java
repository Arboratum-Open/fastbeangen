package com.arboratum.beangen.example1;

import com.arboratum.beangen.example1.model.Identifiable;
import com.arboratum.beangen.example1.model.Person;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.LongSummaryStatistics;

/**
 * Created by gpicron on 18/02/2017.
 */
@Repository
@Transactional
public class TestDbDAO {
    @PersistenceContext
    protected EntityManager entityManager;



    public <E extends Identifiable> LongSummaryStatistics store(List<E> element) {

        final Session delegate = (Session) entityManager.getDelegate();
        for (E p : element) {
            delegate.save(p);
        }
        entityManager.flush();
        entityManager.clear();

        return element.stream().mapToLong(E::getId).summaryStatistics();

    }

    @javax.transaction.Transactional(value = javax.transaction.Transactional.TxType.NOT_SUPPORTED)
    public Person load(long id) {
        try {
            return (Person) entityManager.createQuery("select p from Person p  where p.id = :id").setParameter("id", id).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public void insert(Person person) {
        final Session delegate = (Session) entityManager.getDelegate();
        delegate.save(person);

    }

    public void delete(Person person) {
        final Session delegate = (Session) entityManager.getDelegate();
        delegate.createSQLQuery("delete from Person where id = :id")
                .setLong("id", person.getId()).executeUpdate();
    }

    public void update(Person person) {
        final Session delegate = (Session) entityManager.getDelegate();
        delegate.saveOrUpdate(person);
    }
}
