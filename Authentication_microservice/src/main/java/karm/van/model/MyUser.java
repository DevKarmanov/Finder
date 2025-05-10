package karm.van.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MyUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Description is required")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Size(max = 255, message = "Skills should not exceed 255 characters")
    private String skills;

    @NotBlank(message = "Password is required")
    private String password;

    private String roleInCommand;

    @ElementCollection
    @CollectionTable(name = "user_cards",joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "card_id")
    private List<Long> cards;

    @ElementCollection
    @CollectionTable(name = "user_comments",joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "comment_id")
    private List<Long> comments;

    @ElementCollection
    @CollectionTable(name = "user_favoriteCards",joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "card_id")
    private List<Long> favoriteCards;

    @ElementCollection
    @CollectionTable(name = "user_followers", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "follower_id")
    private List<Long> followers;

    @ElementCollection
    @CollectionTable(name = "user_following", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "following_id")
    private List<Long> following;

    private Long profileImage;

    private boolean isEnable;

    private LocalDateTime unlockAt;

    private String blockReason;

}
