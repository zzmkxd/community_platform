package com.community.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScanSuccessMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
}
