package sierra.tms.services;

import jakarta.ejb.Local;
import java.util.List;
import sierra.tms.dto.ContractDto;

@Local
public interface ContractService {
    
    void createContract(ContractDto dto);

    ContractDto findById(Long id);

    List<ContractDto> findAll();

    ContractDto update(ContractDto dto);

    void delete(Long id);
    
    void startContract(Long id);
            
    void terminateContract(Long id);
    
    ContractDto getContractDetails(Long contractId);
    
    ContractDto getContractForPrinting(Long id);
    
}
