package com.popcorn.demo.chaos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 🐒 Chaos Monkey 설정 변경 요청 DTO
 */
@Data
public class ChaosConfigRequest {

    @JsonProperty("latencyActive")
    private Boolean latencyActive;

    @JsonProperty("latencyRangeStart")
    private Integer latencyRangeStart;

    @JsonProperty("latencyRangeEnd")
    private Integer latencyRangeEnd;

    @JsonProperty("exceptionsActive")
    private Boolean exceptionsActive;

    @JsonProperty("memoryActive")
    private Boolean memoryActive;

    @JsonProperty("level")
    private Integer level;

    @JsonProperty("enabled")
    private Boolean enabled;
}