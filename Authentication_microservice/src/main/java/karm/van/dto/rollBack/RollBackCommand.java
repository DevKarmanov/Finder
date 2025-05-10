package karm.van.dto.rollBack;

import lombok.Builder;

import java.util.Map;

@Builder
public record RollBackCommand(String rollbackType, Map<String, Object> params) {
}
