package com.shaadi.dto;

import lombok.Data;

@Data
public class UploadUrlRequest {
    private String fileName;
    private String contentType;
}
