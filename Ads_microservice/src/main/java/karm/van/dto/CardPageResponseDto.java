package karm.van.dto;

import java.util.List;

public record CardPageResponseDto(List<FullCardDtoForOutput> cards,
                                  boolean last,
                                  int totalPages,
                                  long totalElements,
                                  boolean first,
                                  int numberOfElements) {
}
