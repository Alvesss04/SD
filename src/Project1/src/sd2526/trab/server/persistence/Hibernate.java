package sd2526.trab.server.persistence;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

/**
 * A helper class to perform POJO persistence using Hibernate and a backing relational database.
 */
public class Hibernate {

    private static final String HIBERNATE_CFG_FILE = "hibernate.cfg.xml";
    private SessionFactory sessionFactory;
    private static Hibernate instance;

    private Hibernate() {
        try {
            sessionFactory = new Configuration()
                    .configure(new File(HIBERNATE_CFG_FILE))
                    .buildSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the Hibernate singleton instance, initializing if necessary.
     */
    synchronized public static Hibernate getInstance() {
        if (instance == null)
            instance = new Hibernate();
        return instance;
    }

    /**
     * Persists one or more objects to storage.
     */
    public void persist(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects)
                session.persist(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Gets one object from storage by its identifier.
     */
    public <T> T get(Class<T> clazz, Object identifier) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            T element = session.get(clazz, identifier);
            tx.commit();
            return element;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Updates one or more previously persisted objects.
     */
    public void update(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects)
                session.merge(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Removes one or more objects from storage.
     */
    public void delete(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects)
                session.remove(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public <T> void delete(Class<T> clazz, Object identifier) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            T o = session.get(clazz, identifier);
            session.remove(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Executes a JPQL query and returns the matching objects.
     */
    public <T> List<T> jpql(String jpqlStatement, Class<T> clazz) {
        try (var session = sessionFactory.openSession()) {
            return session.createQuery(jpqlStatement, clazz).list();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Executes a JPQL query with named parameters and returns the matching objects.
     * Example: jpql("SELECT m FROM Message m WHERE m.id = :id", Message.class, Map.of("id", someId))
     */
    public <T> List<T> jpql(String jpqlStatement, Class<T> clazz, Map<String, Object> params) {
        try (var session = sessionFactory.openSession()) {
            var query = session.createQuery(jpqlStatement, clazz);
            params.forEach(query::setParameter);
            return query.list();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Executes a native SQL query and returns the matching objects.
     */
    public <T> List<T> sql(String sqlStatement, Class<T> clazz) {
        try (var session = sessionFactory.openSession()) {
            return session.createNativeQuery(sqlStatement, clazz).list();
        } catch (Exception e) {
            throw e;
        }
    }
}