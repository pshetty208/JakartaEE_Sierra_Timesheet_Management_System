package sierra.tms.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import sierra.tms.utils.enums.RoleType;

@Named("featureAccess")
@RequestScoped
public class FeatureAccessBean {

    @Inject
    private HttpServletRequest request;

    public RoleType getPrimaryRole() {
        if (request.isUserInRole("ADMIN")) {
            return RoleType.ADMIN;
        }
        if (request.isUserInRole("SECRETARY")) {
            return RoleType.SECRETARY;
        }
        if (request.isUserInRole("ASSISTANT")) {
            return RoleType.ASSISTANT;
        }
        if (request.isUserInRole("SUPERVISOR")) {
            return RoleType.SUPERVISOR;
        }
        return RoleType.EMPLOYEE;
    }

    public boolean isReportWork() {
        return request.isUserInRole("EMPLOYEE");
    }

    public boolean isViewContract() {
        return isAuthenticatedRole();
    }

    public boolean isViewTimesheets() {
        return isAuthenticatedRole();
    }

    public boolean isViewStatistics() {
        return isAuthenticatedRole();
    }

    public boolean isSignTimesheet() {
        return request.isUserInRole("EMPLOYEE");
    }

    public boolean isCountersignTimesheet() {
        return request.isUserInRole("SUPERVISOR");
    }

    public boolean isRequestTimesheetChanges() {
        return request.isUserInRole("SUPERVISOR")
                || request.isUserInRole("ASSISTANT");
    }

    public boolean isManageContract() {
        return request.isUserInRole("SUPERVISOR")
                || request.isUserInRole("ASSISTANT");
    }

    public boolean isStartContract() {
        return isManageContract();
    }

    public boolean isTerminateContract() {
        return isManageContract();
    }

    public boolean isPrintContract() {
        return request.isUserInRole("SECRETARY")
                || request.isUserInRole("ADMIN");
    }

    public boolean isPrintTimesheet() {
        return request.isUserInRole("SECRETARY");
    }

    public boolean isArchiveTimesheet() {
        return request.isUserInRole("SECRETARY");
    }

    public boolean isSystemOverview() {
        return request.isUserInRole("ADMIN");
    }

    private boolean isAuthenticatedRole() {
        return request.isUserInRole("EMPLOYEE")
                || request.isUserInRole("SUPERVISOR")
                || request.isUserInRole("ASSISTANT")
                || request.isUserInRole("SECRETARY")
                || request.isUserInRole("ADMIN");
    }
}
