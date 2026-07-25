package com.jee.services;

import com.jee.dto.ContractDto;
import jakarta.ejb.Remote;

/**
 *
 * @author pranavsudhir
 */
@Remote
public interface ContractService {

    public ContractDto getContractById(Long id);
}
