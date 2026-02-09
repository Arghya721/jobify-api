package com.jobify.api.controller;

import com.jobify.api.model.Company;
import com.jobify.api.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Companies", description = "Company management APIs")
@RequiredArgsConstructor
public class CompanyController {

    private CompanyService service;

    @Operation(summary = "Get All Companies", description = "Returns a list of all companies sorted by ID.")
    @GetMapping("/companies")
    public org.springframework.http.ResponseEntity<List<Company>> getAllCompanies() {
        return org.springframework.http.ResponseEntity.ok(service.getAllCompanies());
    }
}
