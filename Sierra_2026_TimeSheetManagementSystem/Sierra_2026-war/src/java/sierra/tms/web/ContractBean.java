package sierra.tms.web;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import sierra.tms.dto.ContractDto;
import sierra.tms.services.ContractService;
import sierra.tms.utils.enums.ContractStatus;

@Named
@ViewScoped
public class ContractBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ContractService contractService;

    private ContractDto contract;

    private List<ContractDto> contracts;

    @PostConstruct
    public void init() {
        contract = new ContractDto();
        loadContracts();
    }

    public void create() {
        contractService.createContract(contract);
        contract = new ContractDto();
        loadContracts();
    }

    public void load(Long id) {
        contract = contractService.findById(id);
    }

    public void update() {
        contractService.update(contract);
        contract = new ContractDto();
        loadContracts();
    }

    public void delete(Long id) {
        contractService.delete(id);
        loadContracts();
    }

    public void loadContracts() {
        contracts = contractService.findAll();
    }

    public void clear() {
        contract = new ContractDto();
    }

    public ContractStatus[] getStatuses() {
        return ContractStatus.values();
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
}