package karm.van.dto.elastic;

import java.util.Optional;

public record UserRequest(
        Long id,
        Optional<String> name,
        Optional<String> firstName,
        Optional<String> lastName,
        Optional<String> description,
        Optional<String> country,
        Optional<String> roleInCommand,
        Optional<String> skills
) {
}
