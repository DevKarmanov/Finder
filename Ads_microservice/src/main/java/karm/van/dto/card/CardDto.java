package karm.van.dto.card;

import java.util.List;

public record CardDto(Long id, String title, String text, List<String> tags) {
}
