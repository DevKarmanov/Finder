package karm.van.dto.card;

import karm.van.dto.image.ImageDto;

import java.time.LocalDate;
import java.util.List;

public record FullCardDtoForOutput(Long id,
                                   String title,
                                   String text,
                                   LocalDate createTime,
                                   List<String> tags,
                                   List<ImageDto> images,
                                   String authorName) {
}
