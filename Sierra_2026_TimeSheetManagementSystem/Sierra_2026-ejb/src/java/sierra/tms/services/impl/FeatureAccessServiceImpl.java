//Moved to frontend

//package sierra.tms.services.impl;
//
//import jakarta.annotation.Resource;
//import jakarta.annotation.security.RolesAllowed;
//import jakarta.ejb.SessionContext;
//import jakarta.ejb.Stateless;
//import jakarta.inject.Named;
//import java.util.EnumSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import sierra.tms.services.FeatureAccessService;
//import sierra.tms.utils.enums.ApplicationFeature;
//import sierra.tms.utils.enums.RoleType;
//
//@Named("featureAccess")
//@Stateless
//@RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMINISTRATOR"})
//public class FeatureAccessServiceImpl implements FeatureAccessService {
//
//    private static final List<RoleType> ROLE_PRECEDENCE = List.of(
//            RoleType.ADMIN,
//            RoleType.SECRETARY,
//            RoleType.ASSISTANT,
//            RoleType.SUPERVISOR,
//            RoleType.EMPLOYEE);
//
//    private static final Map<RoleType, Set<ApplicationFeature>> FEATURES_BY_ROLE = Map.of(
//            RoleType.EMPLOYEE, EnumSet.of(
//                    ApplicationFeature.VIEW_CONTRACT,
//                    ApplicationFeature.VIEW_TIMESHEETS,
//                    ApplicationFeature.REPORT_WORK,
//                    ApplicationFeature.SIGN_TIMESHEET,
//                    ApplicationFeature.VIEW_STATISTICS),
//            RoleType.SUPERVISOR, EnumSet.of(
//                    ApplicationFeature.VIEW_CONTRACT,
//                    ApplicationFeature.VIEW_TIMESHEETS,
//                    ApplicationFeature.COUNTERSIGN_TIMESHEET,
//                    ApplicationFeature.REQUEST_TIMESHEET_CHANGES,
//                    ApplicationFeature.VIEW_STATISTICS,
//                    ApplicationFeature.MANAGE_CONTRACT,
//                    ApplicationFeature.START_CONTRACT,
//                    ApplicationFeature.TERMINATE_CONTRACT),
//            RoleType.ASSISTANT, EnumSet.of(
//                    ApplicationFeature.VIEW_CONTRACT,
//                    ApplicationFeature.VIEW_TIMESHEETS,
//                    ApplicationFeature.REQUEST_TIMESHEET_CHANGES,
//                    ApplicationFeature.VIEW_STATISTICS,
//                    ApplicationFeature.MANAGE_CONTRACT,
//                    ApplicationFeature.START_CONTRACT,
//                    ApplicationFeature.TERMINATE_CONTRACT),
//            RoleType.SECRETARY, EnumSet.of(
//                    ApplicationFeature.VIEW_CONTRACT,
//                    ApplicationFeature.VIEW_TIMESHEETS,
//                    ApplicationFeature.VIEW_STATISTICS,
//                    ApplicationFeature.MANAGE_CONTRACT,
//                    ApplicationFeature.PRINT_CONTRACT,
//                    ApplicationFeature.PRINT_TIMESHEET,
//                    ApplicationFeature.ARCHIVE_TIMESHEET),
//            RoleType.ADMIN, EnumSet.of(
//                    ApplicationFeature.VIEW_CONTRACT,
//                    ApplicationFeature.VIEW_TIMESHEETS,
//                    ApplicationFeature.VIEW_STATISTICS,
//                    ApplicationFeature.PRINT_CONTRACT,
//                    ApplicationFeature.SYSTEM_OVERVIEW));
//
//    @Resource
//    private SessionContext sessionContext;
//
//    @Override
//    public Set<ApplicationFeature> getAvailableFeatures() {
//        EnumSet<ApplicationFeature> available = EnumSet.noneOf(ApplicationFeature.class);
//        FEATURES_BY_ROLE.forEach((role, features) -> {
//            if (sessionContext.isCallerInRole(role.name())) {
//                available.addAll(features);
//            }
//        });
//        return Set.copyOf(available);
//    }
//
//    @Override
//    public RoleType getPrimaryRole() {
//        return ROLE_PRECEDENCE.stream()
//                .filter(role -> sessionContext.isCallerInRole(role.name()))
//                .findFirst()
//                .orElse(RoleType.EMPLOYEE);
//    }
//
//    @Override
//    public boolean isViewContract() {
//        return has(ApplicationFeature.VIEW_CONTRACT);
//    }
//
//    @Override
//    public boolean isViewTimesheets() {
//        return has(ApplicationFeature.VIEW_TIMESHEETS);
//    }
//
//    @Override
//    public boolean isReportWork() {
//        return has(ApplicationFeature.REPORT_WORK);
//    }
//
//    @Override
//    public boolean isSignTimesheet() {
//        return has(ApplicationFeature.SIGN_TIMESHEET);
//    }
//
//    @Override
//    public boolean isCountersignTimesheet() {
//        return has(ApplicationFeature.COUNTERSIGN_TIMESHEET);
//    }
//
//    @Override
//    public boolean isRequestTimesheetChanges() {
//        return has(ApplicationFeature.REQUEST_TIMESHEET_CHANGES);
//    }
//
//    @Override
//    public boolean isViewStatistics() {
//        return has(ApplicationFeature.VIEW_STATISTICS);
//    }
//
//    @Override
//    public boolean isManageContract() {
//        return has(ApplicationFeature.MANAGE_CONTRACT);
//    }
//
//    @Override
//    public boolean isStartContract() {
//        return has(ApplicationFeature.START_CONTRACT);
//    }
//
//    @Override
//    public boolean isTerminateContract() {
//        return has(ApplicationFeature.TERMINATE_CONTRACT);
//    }
//
//    @Override
//    public boolean isPrintContract() {
//        return has(ApplicationFeature.PRINT_CONTRACT);
//    }
//
//    @Override
//    public boolean isPrintTimesheet() {
//        return has(ApplicationFeature.PRINT_TIMESHEET);
//    }
//
//    @Override
//    public boolean isArchiveTimesheet() {
//        return has(ApplicationFeature.ARCHIVE_TIMESHEET);
//    }
//
//    @Override
//    public boolean isSystemOverview() {
//        return has(ApplicationFeature.SYSTEM_OVERVIEW);
//    }
//
//    private boolean has(ApplicationFeature feature) {
//        return FEATURES_BY_ROLE.entrySet().stream()
//                .filter(entry -> sessionContext.isCallerInRole(entry.getKey().name()))
//                .anyMatch(entry -> entry.getValue().contains(feature));
//    }
//}
