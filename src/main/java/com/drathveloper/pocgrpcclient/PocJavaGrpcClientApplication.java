package com.drathveloper.pocgrpcclient;

import com.drathveloper.grpc.User;
import com.drathveloper.grpc.UserAddress;
import com.drathveloper.grpc.UserBulkLoadRequest;
import com.drathveloper.grpc.UserServiceGrpc;
import com.drathveloper.pocgrpcclient.dto.AddressDto;
import com.drathveloper.pocgrpcclient.dto.BulkLoadUserRequest;
import com.drathveloper.pocgrpcclient.dto.BulkLoadUserResponse;
import com.drathveloper.pocgrpcclient.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class PocJavaGrpcClientApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(PocJavaGrpcClientApplication.class, args);
        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
        try {
            int[] sizes = new int[]{1, 5, 10, 25, 50, 100, 500, 1000};
            for (int i = 0; i < 50; i++) {
                log.info("Test {}", i + 1);
                List<Long> grpcTimes = new ArrayList<>();
                List<Long> restTimes = new ArrayList<>();
                List<Integer> grpcSerializedDataLength = new ArrayList<>();
                List<Integer> restSerializedDataLength = new ArrayList<>();
                for (int size : sizes) {
                    var bodyObject = generateBulkLoadRestRequest(size);
                    int serializedDataLengthRestCall = objectMapper.writeValueAsBytes(bodyObject).length;
                    restSerializedDataLength.add(serializedDataLengthRestCall);
                    restTimes.add(performRestCall(context, bodyObject));
                    var generatedGrpcRequest = generateBulkLoadGrpcRequest(size);
                    int serializedDataLengthGrpcCall = generatedGrpcRequest.getSerializedSize();
                    grpcSerializedDataLength.add(serializedDataLengthGrpcCall);
                    grpcTimes.add(performGrpcUnaryCall(context, generatedGrpcRequest));
                }
                for (int j = 0; j < sizes.length; j++) {
                    log.info("REST time in {} ms for {} bytes", restTimes.get(j), restSerializedDataLength.get(j));
                    log.info("GRPC time in {} ms for {} bytes", grpcTimes.get(j), grpcSerializedDataLength.get(j));
                    if (grpcTimes.get(j) != 0){
                        log.info("GRPC is faster {} times than REST", restTimes.get(j)/(float)grpcTimes.get(j));
                    }
                }
            }
        } catch (Exception exception) {
            log.error("error while performing test: {}", exception.getMessage());
        }
    }

    public static long performRestCall(ApplicationContext context, BulkLoadUserRequest bodyObject) {
        try {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            long startTime = System.currentTimeMillis();
            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/user/bulk"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(bodyObject)))
                    .build();
            var httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            var responseObject = objectMapper.readValue(httpResponse.body(), BulkLoadUserResponse.class);
            return System.currentTimeMillis() - startTime;
        } catch (InterruptedException | IOException exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }

    public static long performGrpcUnaryCall(ApplicationContext context, UserBulkLoadRequest generatedGrpcRequest) {
        var blockingStub = context.getBean(UserServiceGrpc.UserServiceBlockingStub.class);
        long startTime = System.currentTimeMillis();
        var response = blockingStub.bulkLoad(generatedGrpcRequest);
        return System.currentTimeMillis() - startTime;
    }

    public static UserBulkLoadRequest generateBulkLoadGrpcRequest(int numUsers) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < numUsers; i++) {
            users.add(
                    User.newBuilder()
                            .setUsername("someUsername" + Math.random())
                            .setFirstName("name" + Math.random())
                            .setLastName("lastName" + Math.random())
                            .setEmail("email" + Math.random() + "@email.com")
                            .setPhone("+34666" + Math.random())
                            .setBirthDate(Timestamp.newBuilder()
                                    .setSeconds(System.currentTimeMillis()/1000)
                                    .build())
                            .setAddress(UserAddress.newBuilder()
                                    .setCountry("Spain")
                                    .setCity("Madrid")
                                    .setState("Madrid")
                                    .setAddress("Avenida Ciudad de Barcelona 23, 4B")
                                    .setPostalCode("28007")
                                    .build())
                            .build());
        }
        return UserBulkLoadRequest.newBuilder().addAllUsers(users).build();
    }

    public static BulkLoadUserRequest generateBulkLoadRestRequest(int numUsers) {
        List<UserDto> users = new ArrayList<>();
        for (int i = 0; i < numUsers; i++) {
            users.add(
                    new UserDto(
                            "someUsername" + Math.random(),
                            "name" + Math.random(),
                            "lastName" + Math.random(),
                            "email" + Math.random() + "@email.com",
                            "+34666" + Math.random(),
                            LocalDate.now(),
                            new AddressDto(
                                    "Spain",
                                    "Madrid",
                                    "Madrid",
                                    "Avenida Ciudad de Barcelona 23, 4B",
                                    "28007")));
        }
        return new BulkLoadUserRequest(users);
    }
}