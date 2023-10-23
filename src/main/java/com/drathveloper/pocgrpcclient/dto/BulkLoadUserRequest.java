package com.drathveloper.pocgrpcclient.dto;

import java.util.List;

public record BulkLoadUserRequest(List<UserDto> users) {
}
