package karm.van.dto.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record UserDtoRequest(String name,
                             String password,
                             String email,
                             List<String> role,
                             String firstName,
                             String lastName,
                             String description,
                             String country,
                             String roleInCommand,
                             String skills,
                             String adminKey) {
}
