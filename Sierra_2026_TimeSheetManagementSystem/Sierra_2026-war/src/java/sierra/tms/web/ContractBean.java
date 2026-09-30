package sierra.tms.web;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import sierra.tms.dto.ContractDto;
import sierra.tms.dto.PersonDto;
import sierra.tms.exceptions.ContractRuleViolation;
import sierra.tms.exceptions.TerminationWarning;
import sierra.tms.i18n.UiMessages;
import sierra.tms.services.ContractService;
import sierra.tms.services.PersonService;
import sierra.tms.utils.enums.ContractStatus;
import sierra.tms.utils.enums.Frequency;
import sierra.tms.utils.enums.RoleType;

@Named
@ViewScoped
public class ContractBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_WORKING_DAYS_PER_WEEK = 5;
    private static final int DEFAULT_VACATION_DAYS_PER_YEAR = 20;

    @EJB
    private ContractService contractService;

    @EJB
    private PersonService personService;

    private ContractDto contract;

    private List<ContractDto> contracts;

    private List<PersonDto> employees = List.of();

    private List<PersonDto> supervisors = List.of();

    private List<PersonDto> assistants = List.of();

    private Long selectedAssistantId;

    private Long contractDetailId;

    private boolean terminationWarningActive;

    @PostConstruct
    public void init() {
        contract = newContractWithDefaults();
    }

    public void create() {
        try {
            contract.setAssistantRoleIds(selectedAssistantId == null
                    ? Set.of() : Set.of(selectedAssistantId));
            contractService.createContract(contract);
            contract = newContractWithDefaults();
            selectedAssistantId = null;
            loadContracts();
        } catch (ContractRuleViolation violation) {
            showRuleViolation(violation);
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "create contract",
                    "common.error.operationFailed", exception);
        }
    }

    public void loadCreateOptions() {
        try {
            List<PersonDto> people = personService.findAll();
            List<PersonDto> loadedEmployees = withRole(people, RoleType.EMPLOYEE);
            List<PersonDto> loadedSupervisors = withStaffRole(people, RoleType.SUPERVISOR);
            List<PersonDto> loadedAssistants = withStaffRole(people, RoleType.ASSISTANT);
            employees = loadedEmployees;
            supervisors = loadedSupervisors;
            assistants = loadedAssistants;
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "load contract participants",
                    "common.error.loadFailed", exception);
        }
    }

    private void showRuleViolation(ContractRuleViolation violation) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        UiMessages.get(violation.getMessageKey(), violation.getArguments()), null));
    }

    private static List<PersonDto> withRole(List<PersonDto> people, RoleType role) {
        return people.stream()
                .filter(person -> person.getRoles().contains(role))
                .toList();
    }

    private static List<PersonDto> withStaffRole(List<PersonDto> people, RoleType role) {
        return people.stream()
                .filter(person -> person.isUniversityStaff() && person.getRoles().contains(role))
                .toList();
    }

    public void load(Long id) {
        try {
            contract = contractService.findById(id);
        } catch (RuntimeException exception) {
            contract = null;
            WebExceptionHandler.handle(getClass(), "load contract",
                    "contract.message.detailsUnavailable", exception);
        }
    }

    public void loadContractDetails() {
        try {
            contract = contractService.findById(contractDetailId);
        } catch (RuntimeException exception) {
            contract = null;
            WebExceptionHandler.handle(getClass(), "load contract details",
                    "contract.message.detailsUnavailable", exception);
        }
    }

    public void loadContractForPrinting() {
        try {
            contractService.findById(contractDetailId);
            contract = contractService.getContractForPrinting(contractDetailId);
        } catch (RuntimeException exception) {
            contract = null;
            WebExceptionHandler.handle(getClass(), "load printable contract",
                    "contract.message.printUnavailable", exception);
        }
    }

    public void update() {
        try {
            contractService.update(contract);
            contract = newContractWithDefaults();
            loadContracts();
        } catch (ContractRuleViolation violation) {
            showRuleViolation(violation);
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "update contract",
                    "common.error.operationFailed", exception);
        }
    }

    public void delete(Long id) {
        try {
            contractService.delete(id);
            loadContracts();
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "delete contract",
                    "common.error.operationFailed", exception);
        }
    }

    public void startContract() {
        try {
            contractService.startContract(contractDetailId);
            loadContractDetails();
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "start contract",
                    "contract.message.startFailed", e);
        }
    }

    public void terminateContract() {
        try {
            contractService.terminateContract(contractDetailId, false);
            loadContractDetails();
        } catch (TerminationWarning warning) {
            terminationWarningActive = true;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                            UiMessages.get("contract.message.terminationWarning"), null));
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "terminate contract",
                    "contract.message.terminateFailed", exception);
        }
    }

    public void confirmTerminate() {
        try {
            contractService.terminateContract(contractDetailId, true);
            terminationWarningActive = false;
            loadContractDetails();
        } catch (RuntimeException e) {
            terminationWarningActive = false;
            WebExceptionHandler.handle(getClass(), "confirm contract termination",
                    "contract.message.terminateFailed", e);
        }
    }

    public void cancelTerminate() {
        terminationWarningActive = false;
    }

    public void loadContracts() {
        try {
            contracts = contractService.findAll();
        } catch (RuntimeException exception) {
            contracts = List.of();
            WebExceptionHandler.handle(getClass(), "load contracts",
                    "common.error.loadFailed", exception);
        }
    }

    public void clear() {
        contract = newContractWithDefaults();
    }

    private ContractDto newContractWithDefaults() {
        ContractDto newContract = new ContractDto();
        newContract.setWorkingDaysPerWeek(DEFAULT_WORKING_DAYS_PER_WEEK);
        newContract.setVacationDaysPerYear(DEFAULT_VACATION_DAYS_PER_YEAR);
        return newContract;
    }

    public ContractStatus[] getStatuses() {
        return ContractStatus.values();
    }

    public Frequency[] getFrequencies() {
        return Frequency.values();
    }

    public String frequencyLabel(Frequency frequency) {
        return UiMessages.get("frequency." + frequency.name().toLowerCase());
    }

    public String statusLabel(ContractStatus status) {
        return UiMessages.get("contract.status." + status.name().toLowerCase());
    }

    public String statusStyleClass(ContractStatus status) {
        return "contract-overview__status--" + status.name().toLowerCase();
    }

    public boolean isPrepared() {
      return ContractStatus.PREPARED == contract.getStatus();
    }

    public boolean isStarted() {
       return ContractStatus.STARTED == contract.getStatus();
    }

    public boolean isTerminationWarningActive() {
        return terminationWarningActive;
    }

    public ContractDto getContract() {
        return contract;
    }

    public void setContract(ContractDto contract) {
        this.contract = contract;
    }

    public List<ContractDto> getContracts() {
        return contracts;
    }

    public List<PersonDto> getEmployees() {
        return employees;
    }

    public List<PersonDto> getSupervisors() {
        return supervisors;
    }

    public List<PersonDto> getAssistants() {
        return assistants;
    }

    public Long getSelectedAssistantId() {
        return selectedAssistantId;
    }

    public void setSelectedAssistantId(Long selectedAssistantId) {
        this.selectedAssistantId = selectedAssistantId;
    }

    public void setContracts(List<ContractDto> contracts) {
        this.contracts = contracts;
    }

    public Long getContractDetailId() {
        return contractDetailId;
    }

    public void setContractDetailId(Long contractDetailId) {
        this.contractDetailId = contractDetailId;
    }
}
