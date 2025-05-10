package karm.van.service.utils;

import karm.van.dto.response.SubscriberDto;
import karm.van.dto.response.UserDtoForSearchResponse;
import karm.van.model.MyUser;
import karm.van.model.MyUserDocument;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserMapper {

    public static List<UserDtoForSearchResponse> toDto(List<MyUserDocument> myUserDocuments){
        return myUserDocuments.stream().map(o->new UserDtoForSearchResponse(
                o.getId(),
                o.getName(),
                o.getFirstName(),
                o.getLastName(),
                o.getDescription(),
                o.getCountry(),
                o.getRoleInCommand(),
                o.getSkills()
        )).toList();
    }

    public static List<SubscriberDto> toSubscriberDto(List<MyUser> users){
        return users.stream()
                .map(subscriber -> new SubscriberDto(
                        subscriber.getId(),
                        subscriber.getName(),
                        subscriber.getFirstName(),
                        subscriber.getLastName()

                ))
                .collect(Collectors.toList());
    }
}
