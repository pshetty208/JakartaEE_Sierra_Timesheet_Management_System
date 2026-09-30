package sierra.tms.dao;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import sierra.tms.utils.enums.RoleType;

@LocalBean
@Stateless
public class ApplicationRoleAssignmentDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public Set<RoleType> findByUsername(String username) {
        List<?> groupNames = em.createNativeQuery(
                "SELECT group_name FROM SIERRA_AUTH_GROUP WHERE username = :username")
                .setParameter("username", username)
                .getResultList();

        Set<RoleType> roles = EnumSet.noneOf(RoleType.class);
        for (Object queryResult : groupNames) {
            String groupName = String.class.cast(queryResult);
            roles.add(RoleType.valueOf(groupName));
        }
        return roles;
    }

    public void assign(String username, RoleType role) {
        em.createNativeQuery(
                "DELETE FROM SIERRA_AUTH_GROUP WHERE username = :username")
                .setParameter("username", username)
                .executeUpdate();
        em.createNativeQuery(
                "INSERT INTO SIERRA_AUTH_GROUP (username, group_name) "
                + "VALUES (:username, :role)")
                .setParameter("username", username)
                .setParameter("role", role.name())
                .executeUpdate();
    }

}
