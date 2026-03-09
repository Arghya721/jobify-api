package com.jobify.api.controller;

import com.google.protobuf.Empty;
import com.jobify.api.grpc.CompanyMessage;
import com.jobify.api.grpc.CompanyServiceGrpc;
import com.jobify.api.grpc.GetAllCompaniesResponse;
import com.jobify.api.model.Company;
import com.jobify.api.service.CompanyService;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class CompanyGrpcService extends CompanyServiceGrpc.CompanyServiceImplBase {

    private final CompanyService companyService;

    public CompanyGrpcService(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Override
    public void getAllCompanies(Empty request, StreamObserver<GetAllCompaniesResponse> responseObserver) {
        try {
            GetAllCompaniesResponse.Builder responseBuilder = GetAllCompaniesResponse.newBuilder();

            for (Company company : companyService.getAllCompanies()) {
                CompanyMessage message = CompanyMessage.newBuilder()
                        .setId(company.getId())
                        .setName(company.getName())
                        .setSource(company.getSource())
                        .build();
                responseBuilder.addCompanies(message);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
