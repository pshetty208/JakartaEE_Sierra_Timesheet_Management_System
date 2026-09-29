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
import java.util.logging.Level;
import java.util.logging.Logger;
import sierra.tms.dto.ContractDto;
import sierra.tms.exceptions.TerminationWarning;
import sierra.tms.i18n.UiMessages;
import sierra.tms.services.ContractService;
import sierra.tms.utils.enums.ContractStatus;
import sierra.tms.utils.enums.Frequency;

@Named
@ViewScoped
public class ContractBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_WORKING_DAYS_PER_WEEK = 5;
    private static final int DEFAULT_VACATION_DAYS_PER_YEAR = 20;

    @EJB
    private ContractService contractService;

    private ContractDto contract;

    private List<ContractDto> contracts;

    private Long contractDetailId;

    private boolean terminationWarningActive;

    @PostConstruct
    public void init() {
        contract = newContractWithDefaults();
    }

    public void create() {
        contractService.createContract(contract);
        contract = newContractWithDefaults();
        loadContracts();
    }

    public void load(Long id) {
        contract = contractService.findById(id);
    }

    public void loadContractDetails() {
        try {
            contract = contractService.findById(contractDetailId);
        } catch (EJBException exception) {
            contract = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            UiMessages.get("contract.message.detailsUnavailable"),
                            null));
        }
    }

    public void loadContractForPrinting() {
        try {
            contractService.findById(contractDetailId);
            contract = contractService.getContractForPrinting(contractDetailId);
        } catch (EJBException exception) {
            contract = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            UiMessages.get("contract.message.printUnavailable"),
                            null));
        }
    }

    public void update() {
        contractService.update(contract);
        contract = newContractWithDefaults();
        loadContracts();
    }

    public void delete(Long id) {
        contractService.delete(id);
        loadContracts();
    }

    public void startContract() {
        try {
            contractService.startContract(contractDetailId);
            loadContractDetails();
        } catch (EJBException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            UiMessages.get("contract.message.startFailed"), null));
        }
    }

    public void terminateContract() {
        try {
            contractService.terminateContract(contractDetailId, false);
            loadContractDetails();
        } catch (EJBException e) {
            if (e.getCause() instanceof TerminationWarning tw) {
                terminationWarningActive = true;
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                UiMessages.get("contract.message.terminationWarning"), null));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                UiMessages.get("contract.message.terminateFailed"), null));
            }
        }
    }

    public void confirmTerminate() {
        try {
            contractService.terminateContract(contractDetailId, true);
            terminationWarningActive = false;
            loadContractDetails();
        } catch (EJBException e) {
            terminationWarningActive = false;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            UiMessages.get("contract.message.terminateFailed"), null));
        }
    }

    public void cancelTerminate() {
        terminationWarningActive = false;
    }

    public void loadContracts() {
        try {
            contracts = contractService.findAll();
        } catch (EJBException exception) {
            contracts = null;
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
