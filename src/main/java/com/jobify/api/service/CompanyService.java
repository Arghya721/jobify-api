package com.jobify.api.service;

import com.jobify.api.repository.CompanyRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.jobify.api.model.Company;
import java.util.List;

@Service
public class CompanyService {

    CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "companies", key = "'all'")
    public List<Company> getAllCompanies() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }
}
