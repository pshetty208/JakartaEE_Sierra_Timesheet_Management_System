package sierra.tms.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import sierra.tms.i18n.LanguageResolver;

@Entity
@Table(name = "Person")
public class PersonEntity implements Serializable {

    private static final long serialVersionUID = 1L;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;


    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;


    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;


    @Column(name = "email_address", nullable = false, unique = true, length = 255)
    private String emailAddress;


    @Column(name = "consent", nullable = false)
    private boolean consent = false;


    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage =
            LanguageResolver.DEFAULT_LANGUAGE_CODE;


    @Column(name = "university_staff", nullable = false)
    private boolean universityStaff = false;


    @OneToMany(
            mappedBy = "person",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<RoleEntity> roles = new ArrayList<>();


    public PersonEntity() {
    }


    public PersonEntity(
            Long id,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String emailAddress) {

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.emailAddress = emailAddress;
    }


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getFirstName() {
        return firstName;
    }


    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }


    public String getLastName() {
        return lastName;
    }


    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }


    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }


    public String getEmailAddress() {
        return emailAddress;
    }


    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }


    public boolean isUniversityStaff() {
        return universityStaff;
    }


    public void setUniversityStaff(boolean universityStaff) {
        this.universityStaff = universityStaff;
    }


    public boolean isConsent() {
        return consent;
    }


    public void setConsent(boolean consent) {
        this.consent = consent;
    }


    public String getPreferredLanguage() {
        return preferredLanguage;
    }


    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }


    public List<RoleEntity> getRoles() {
        return roles;
    }


    public void setRoles(List<RoleEntity> roles) {
        if (roles != null && roles.size() > 1) {
            throw new IllegalArgumentException("A person must have exactly one role.");
        }
        this.roles = roles;
    }


    public void addRole(RoleEntity role) {
        if (!roles.isEmpty()) {
            throw new IllegalStateException("A person must have exactly one role.");
        }
        roles.add(role);
        role.setPerson(this);
    }


    public void removeRole(RoleEntity role) {
        roles.remove(role);
        role.setPerson(null);
    }
}
