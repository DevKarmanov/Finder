package karm.van.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SubscriberDto {

    @Schema(description = "Unique identifier of the subscriber", example = "1")
    private Long id;

    @Schema(description = "Username of the subscriber", example = "user1")
    private String name;

    @Schema(description = "First name of the subscriber", example = "John")
    private String firstName;

    @Schema(description = "Last name of the subscriber", example = "Doe")
    private String lastName;

    @Schema(description = "Profile image of the subscriber")
    private ProfileImageDtoResponse profileImage;

    public SubscriberDto(Long id, String name, String firstName, String lastName) {
        this.id = id;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public SubscriberDto() {
    }

}


