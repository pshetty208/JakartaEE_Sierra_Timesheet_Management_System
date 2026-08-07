package sierra.tms.services;

import jakarta.ejb.Remote;
import java.util.List;
import sierra.tms.dto.ContractDto;

@Remote
public interface ContractService {
    
    void createContract(ContractDto dto);

    ContractDto findById(Long id);

    List<ContractDto> findAll();

    ContractDto update(ContractDto dto);

    void delete(Long id);

    ContractDto getContractById(Long id);
    
}
