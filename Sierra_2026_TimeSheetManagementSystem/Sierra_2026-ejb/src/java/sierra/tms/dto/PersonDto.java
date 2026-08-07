package sierra.tms.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import sierra.tms.utils.enums.RoleType;

public class PersonDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public Long id;

    private String firstName;

    private String lastName;

    private String emailAddress;

    private LocalDate dateOfBirth;

    private Boolean consent = false;

    private String preferredLanguage = "en";

    private List<RoleType> roles;

    private boolean universityStaff;


    public PersonDto() {
    }


    public PersonDto(
            Long id,
            String firstName,
            String lastName,
            String emailAddress,
            LocalDate dateOfBirth,
            List<RoleType> roles) {

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
        this.dateOfBirth = dateOfBirth;
        this.roles = roles;
    }


    public PersonDto(
            Long id,
            String firstName,
            String lastName,
            String emailAddress,
            LocalDate dateOfBirth,
            Boolean consent,
            String preferredLanguage,
            List<RoleType> roles,
            boolean universityStaff) {

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
        this.dateOfBirth = dateOfBirth;
        this.consent = consent;
        this.preferredLanguage = preferredLanguage;
        this.roles = roles;
        this.universityStaff = universityStaff;
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


    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }


    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }


    public Boolean getConsent() {
        return consent;
    }

    public void setConsent(Boolean consent) {
        this.consent = consent;
    }


    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }


    public List<RoleType> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleType> roles) {
        this.roles = roles;
    }


    public boolean isUniversityStaff() {
        return universityStaff;
    }

    public void setUniversityStaff(boolean universityStaff) {
        this.universityStaff = universityStaff;
    }
}