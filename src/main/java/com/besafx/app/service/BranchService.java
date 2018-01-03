package com.besafx.app.service;

import com.besafx.app.entity.Branch;
import com.besafx.app.entity.Company;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public interface BranchService extends PagingAndSortingRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {

    Branch findTopByOrderByCodeDesc();

    Branch findByCodeAndIdIsNot(Integer code, Long id);

    Branch findByCode(Integer code);

    Branch findByName(String name);

    List<Branch> findByCompany(Company company);

}
