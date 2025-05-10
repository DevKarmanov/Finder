package karm.van.utils;

import karm.van.dto.FullCommentDtoResponse;
import karm.van.model.CommentModel;

import java.util.*;
import java.util.stream.Collectors;

public class CommentMapper {

    public static List<CommentModel> toEntityList(List<FullCommentDtoResponse> dtos) {
        Map<Long, CommentModel> map = new HashMap<>();

        for (FullCommentDtoResponse dto : dtos) {
            map.put(dto.getId(), toEntityWithoutRelation(dto));
        }

        for (FullCommentDtoResponse dto : dtos) {
            CommentModel model = map.get(dto.getId());

            if (dto.getParentComment() != null) {
                model.setParentComment(map.get(dto.getParentComment().getId()));
            }

            if (dto.getReplyComments() != null && !dto.getReplyComments().isEmpty()) {
                List<CommentModel> replies = dto.getReplyComments().stream()
                        .map(reply -> map.get(reply.getId()))
                        .filter(Objects::nonNull)
                        .toList();
                model.setReplyComments(replies);
            }
        }

        return new ArrayList<>(map.values());
    }

    private static CommentModel toEntityWithoutRelation(FullCommentDtoResponse dto){
        return CommentModel.builder()
                .id(dto.getId())
                .text(dto.getText())
                .createdAt(dto.getCreatedAt())
                .cardId(dto.getCardId())
                .userId(dto.getUserId())
                .build();
    }



}
