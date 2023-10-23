package com.drathveloper.pocgrpcclient.dto;

import java.util.List;

public record BulkLoadUserResponse(List<CreatedUserDto> createdUsers) {
}
