package com.jobify.api.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name="companies")
@Data
@NoArgsConstructor

@Tag(name = "Company", description = "Company entity representing a company record")
public class Company implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    String name;

    String source;
}
