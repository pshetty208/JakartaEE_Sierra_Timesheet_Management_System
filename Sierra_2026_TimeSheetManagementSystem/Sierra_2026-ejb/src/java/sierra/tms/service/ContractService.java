package sierra.tms.service;

import jakarta.ejb.Remote;
import java.util.List;
import sierra.tms.dto.ContractDto;

/**
 *
 * @author prajnashetty
 */
@Remote
public interface ContractService {
    
    void createContract(ContractDto dto);

    ContractDto findById(Long id);

    List<ContractDto> findAll();

    ContractDto update(ContractDto dto);

    void delete(Long id);
    
}
