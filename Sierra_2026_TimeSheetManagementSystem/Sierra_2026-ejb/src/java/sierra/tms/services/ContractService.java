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
    
    void archiveContract(Long contractId);
            
    void terminateContract(Long id, boolean confirmed);
    
    ContractDto getContractDetails(Long contractId);
    
    ContractDto getContractForPrinting(Long id);
    
}
