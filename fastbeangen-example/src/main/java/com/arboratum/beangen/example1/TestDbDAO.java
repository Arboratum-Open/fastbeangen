package com.arboratum.beangen.example1;

import com.arboratum.beangen.example1.model.Identifiable;
import com.arboratum.beangen.example1.model.Person;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

        for (E p : element) {
            entityManager.persist(p);
        }
        entityManager.flush();
        entityManager.clear();

        return element.stream().mapToLong(E::getId).summaryStatistics();

    }

    @jakarta.transaction.Transactional(value = jakarta.transaction.Transactional.TxType.NOT_SUPPORTED)
    public Person load(long id) {
        try {
            return (Person) entityManager.createQuery("select p from Person p  where p.id = :id").setParameter("id", id).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public void insert(Person person) {
        entityManager.persist(person);

    }

    public void delete(Person person) {
        entityManager.createNativeQuery("delete from Person where id = :id")
                .setParameter("id", person.getId()).executeUpdate();
    }

    public void update(Person person) {
        entityManager.merge(person);
    }
}
