package karm.van.dto.request;

import java.util.Optional;

public record UserPatchRequest(
        Optional<String> name,
        Optional<String> email,
        Optional<String> firstName,
        Optional<String> lastName,
        Optional<String> description,
        Optional<String> country,
        Optional<String> roleInCommand,
        Optional<String> skills
) {
}
