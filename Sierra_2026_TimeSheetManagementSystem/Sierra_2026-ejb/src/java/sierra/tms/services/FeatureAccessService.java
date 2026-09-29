package sierra.tms.services;

import jakarta.ejb.Local;
import java.util.Set;
import sierra.tms.utils.enums.ApplicationFeature;
import sierra.tms.utils.enums.RoleType;

@Local
public interface FeatureAccessService {

    Set<ApplicationFeature> getAvailableFeatures();

    RoleType getPrimaryRole();

    boolean isViewContract();

    boolean isViewTimesheets();

    boolean isReportWork();

    boolean isSignTimesheet();

    boolean isCountersignTimesheet();

    boolean isRequestTimesheetChanges();

    boolean isViewStatistics();

    boolean isManageContract();

    boolean isStartContract();

    boolean isTerminateContract();

    boolean isPrintContract();

    boolean isPrintTimesheet();

    boolean isArchiveTimesheet();

    boolean isSystemOverview();
}
