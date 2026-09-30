package sierra.tms.web;

import static sierra.tms.web.ApplicationFeature.ARCHIVE_TIMESHEET;
import static sierra.tms.web.ApplicationFeature.COUNTERSIGN_TIMESHEET;
import static sierra.tms.web.ApplicationFeature.CREATE_CONTRACT;
import static sierra.tms.web.ApplicationFeature.MANAGE_CONTRACT;
import static sierra.tms.web.ApplicationFeature.PRINT_CONTRACT;
import static sierra.tms.web.ApplicationFeature.PRINT_TIMESHEET;
import static sierra.tms.web.ApplicationFeature.REPORT_WORK;
import static sierra.tms.web.ApplicationFeature.REQUEST_TIMESHEET_CHANGES;
import static sierra.tms.web.ApplicationFeature.REVOKE_SIGNATURE;
import static sierra.tms.web.ApplicationFeature.SIGN_TIMESHEET;
import static sierra.tms.web.ApplicationFeature.START_CONTRACT;
import static sierra.tms.web.ApplicationFeature.SYSTEM_OVERVIEW;
import static sierra.tms.web.ApplicationFeature.TERMINATE_CONTRACT;
import static sierra.tms.web.ApplicationFeature.VIEW_CONTRACT;
import static sierra.tms.web.ApplicationFeature.VIEW_STATISTICS;
import static sierra.tms.web.ApplicationFeature.VIEW_TIMESHEETS;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import sierra.tms.utils.enums.RoleType;


@Named("featureAccess")
@RequestScoped
public class FeatureAccessBean {
    /*
    MANAGE_CONTRACT - CN1: only supervisor/assistant manage contracts
    CREATE_CONTRACT - only assistants and secretaries create contracts
    PRINT_CONTRACT - CN2: only the secretary prints contracts
    REVOKE_SIGNATURE - SG1a
    */

    private static final Map<RoleType, Set<ApplicationFeature>> FEATURES_BY_ROLE =
            new EnumMap<>(RoleType.class);

    static {
        FEATURES_BY_ROLE.put(
                RoleType.EMPLOYEE, EnumSet.of(
                VIEW_CONTRACT, 
                VIEW_STATISTICS, 
                VIEW_TIMESHEETS,
                REPORT_WORK, 
                SIGN_TIMESHEET, 
                REVOKE_SIGNATURE));
        FEATURES_BY_ROLE.put(
                RoleType.SUPERVISOR, EnumSet.of(
                VIEW_CONTRACT, 
                VIEW_STATISTICS, 
                VIEW_TIMESHEETS,
                MANAGE_CONTRACT, 
                START_CONTRACT, 
                TERMINATE_CONTRACT,
                COUNTERSIGN_TIMESHEET, 
                REQUEST_TIMESHEET_CHANGES));
        FEATURES_BY_ROLE.put(
                RoleType.ASSISTANT, EnumSet.of(
                VIEW_CONTRACT, 
                VIEW_STATISTICS, 
                VIEW_TIMESHEETS,
                MANAGE_CONTRACT, 
                CREATE_CONTRACT,
                START_CONTRACT, 
                TERMINATE_CONTRACT,
                REQUEST_TIMESHEET_CHANGES));
        FEATURES_BY_ROLE.put(RoleType.SECRETARY, EnumSet.of(
                VIEW_CONTRACT, 
                VIEW_STATISTICS, 
                VIEW_TIMESHEETS,
                CREATE_CONTRACT,
                PRINT_CONTRACT, 
                PRINT_TIMESHEET, 
                ARCHIVE_TIMESHEET));
        FEATURES_BY_ROLE.put(RoleType.ADMIN, EnumSet.of(
                VIEW_CONTRACT, 
                VIEW_STATISTICS, 
                VIEW_TIMESHEETS, 
                SYSTEM_OVERVIEW));
    }

    private Set<ApplicationFeature> availableFeatures;

    public Set<ApplicationFeature> getAvailableFeatures() {
        if (availableFeatures == null) {
            EnumSet<ApplicationFeature> features = EnumSet.noneOf(ApplicationFeature.class);
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                ExternalContext externalContext = context.getExternalContext();
                FEATURES_BY_ROLE.forEach((role, roleFeatures) -> {
                    if (externalContext.isUserInRole(role.name())) {
                        features.addAll(roleFeatures);
                    }
                });
            }
            availableFeatures = Collections.unmodifiableSet(features);
        }
        return availableFeatures;
    }

    public boolean has(ApplicationFeature feature) {
        return getAvailableFeatures().contains(feature);
    }

    public boolean isViewContract() {
        return has(VIEW_CONTRACT);
    }

    public boolean isManageContract() {
        return has(MANAGE_CONTRACT);
    }

    public boolean isCreateContract() {
        return has(CREATE_CONTRACT);
    }

    public boolean isStartContract() {
        return has(START_CONTRACT);
    }

    public boolean isTerminateContract() {
        return has(TERMINATE_CONTRACT);
    }

    public boolean isPrintContract() {
        return has(PRINT_CONTRACT);
    }

    public boolean isViewTimesheets() {
        return has(VIEW_TIMESHEETS);
    }

    public boolean isReportWork() {
        return has(REPORT_WORK);
    }

    public boolean isSignTimesheet() {
        return has(SIGN_TIMESHEET);
    }

    public boolean isRevokeSignature() {
        return has(REVOKE_SIGNATURE);
    }

    public boolean isCountersignTimesheet() {
        return has(COUNTERSIGN_TIMESHEET);
    }

    public boolean isRequestTimesheetChanges() {
        return has(REQUEST_TIMESHEET_CHANGES);
    }

    public boolean isPrintTimesheet() {
        return has(PRINT_TIMESHEET);
    }

    public boolean isArchiveTimesheet() {
        return has(ARCHIVE_TIMESHEET);
    }

    public boolean isViewStatistics() {
        return has(VIEW_STATISTICS);
    }

    public boolean isSystemOverview() {
        return has(SYSTEM_OVERVIEW);
    }
}
