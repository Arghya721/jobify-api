package com.jobify.api.controller;

import com.google.protobuf.Empty;
import com.jobify.api.dto.CountryDTO;
import com.jobify.api.dto.RegionDTO;
import com.jobify.api.grpc.*;
import com.jobify.api.service.GeoService;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;
import com.jobify.api.dto.CityDTO;
import com.jobify.api.dto.CountryDTO;

public class GeoGrpcController {

    @GrpcService
    public static class CountryGrpcService extends CountryServiceGrpc.CountryServiceImplBase {

        private final GeoService geoService;

        public CountryGrpcService(GeoService geoService) {
            this.geoService = geoService;
        }

        @Override
        public void getCountries(Empty request, StreamObserver<GetCountriesResponse> responseObserver) {
            try {
                GetCountriesResponse.Builder responseBuilder = GetCountriesResponse.newBuilder();

                for (CountryDTO country : geoService.getCountries()) {
                    CountryMessage countryMessage = CountryMessage.newBuilder()
                            .setId(country.getId())
                            .setName(country.getName())
                            .setIso2(country.getIso2())
                            .setIso3(country.getIso3())
                            .build();
                    responseBuilder.addData(countryMessage);
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

    @GrpcService
    public static class CityGrpcService extends CityServiceGrpc.CityServiceImplBase {

        private final GeoService geoService;

        public CityGrpcService(GeoService geoService) {
            this.geoService = geoService;
        }

        @Override
        public void getCities(CityRequest request, StreamObserver<GetCityResponse> responseObserver) {
            try {
                GetCityResponse.Builder responseBuilder = GetCityResponse.newBuilder();

                for (CityDTO city : geoService.getCities(request.getId())) {
                    CityMessage cityMessage = CityMessage.newBuilder()
                            .setId(city.getId())
                            .setName(city.getName())
                            .setLatitude(city.getLatitude())
                            .setLongitude(city.getLongitude())
                            .setPopulation(city.getPopulation())
                            .build();
                    responseBuilder.addData(cityMessage);
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

    @GrpcService
    public static class RegionGrpcService extends RegionServiceGrpc.RegionServiceImplBase {

        private final GeoService geoService;

        public RegionGrpcService(GeoService geoService) {
            this.geoService = geoService;
        }

        @Override
        public void getRegions(RegionRequest request, StreamObserver<GetRegionsResponse> responseObserver) {
            try {
                GetRegionsResponse.Builder responseBuilder = GetRegionsResponse.newBuilder();

                for (RegionDTO region : geoService.getRegions(request.getIso2Code())) {
                    RegionMessage regionMessage = RegionMessage.newBuilder()
                            .setId(region.getId())
                            .setName(region.getName())
                            .setCode(region.getCode())
                            .build();
                    responseBuilder.addData(regionMessage);
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
}
