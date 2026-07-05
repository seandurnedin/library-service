package com.libraryapp.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Data REST wrapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HalPage<T> {

    @JsonProperty("_embedded")
    private Map<String, List<T>> embedded;

    private PageMetadata page;

    public List<T> content() {
        if (embedded == null || embedded.isEmpty()) {
            return Collections.emptyList();
        }
        return embedded.values().stream().findFirst().orElse(Collections.emptyList());
    }
}
