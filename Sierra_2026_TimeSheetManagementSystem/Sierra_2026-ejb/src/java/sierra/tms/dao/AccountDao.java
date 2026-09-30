package sierra.tms.dao;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import sierra.tms.utils.enums.RoleType;

@LocalBean
@Stateless
public class AccountDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public boolean exists(String username) {
        return !em.createNativeQuery(
                "SELECT 1 FROM SIERRA_AUTH_USER WHERE username = ?1")
                .setParameter(1, username)
                .getResultList()
                .isEmpty();
    }

    public void create(String username, String password, RoleType role) {
        // Hashed by MariaDB so the digest matches the seed data and the sierraRealm JDBC realm.
        em.createNativeQuery(
                "INSERT INTO SIERRA_AUTH_USER (username, password_hash) "
                + "VALUES (?1, SHA2(?2, 256))")
                .setParameter(1, username)
                .setParameter(2, password)
                .executeUpdate();
        insertGroup(username, role);
    }

    public void assignRole(String username, RoleType role) {
        em.createNativeQuery(
                "DELETE FROM SIERRA_AUTH_GROUP WHERE username = ?1")
                .setParameter(1, username)
                .executeUpdate();
        insertGroup(username, role);
    }

    private void insertGroup(String username, RoleType role) {
        em.createNativeQuery(
                "INSERT INTO SIERRA_AUTH_GROUP (username, group_name) "
                + "VALUES (?1, ?2)")
                .setParameter(1, username)
                .setParameter(2, role.name())
                .executeUpdate();
    }

}
