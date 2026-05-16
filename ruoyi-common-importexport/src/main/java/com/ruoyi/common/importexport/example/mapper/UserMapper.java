package com.ruoyi.common.importexport.example.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;

/**
 * 业务实体转换高级示例 Mapper
 */
@Mapper(componentModel = "spring")
public abstract class UserMapper {

    public static final UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Entity 转换为 DTO 高级用法
     */
    @Mappings({
            @Mapping(source = "id", target = "userId"),
            @Mapping(source = "userName", target = "name"),
            @Mapping(source = "userAge", target = "age"),
            @Mapping(source = "createTime", target = "createDate", dateFormat = "yyyy-MM-dd HH:mm:ss"),
            @Mapping(source = "jsonConfig", target = "configs", qualifiedByName = "jsonToList"),
            @Mapping(target = "uniqueCode", expression = "java(java.util.UUID.randomUUID().toString())")
    })
    public abstract UserDTO toDTO(UserEntity entity);

    /**
     * 批量转换
     */
    public abstract List<UserDTO> toDTOList(List<UserEntity> entities);

    /**
     * 自定义转换方法：JSON字符串转List
     */
    @Named("jsonToList")
    protected List<String> jsonToList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 映射完成后的后置处理钩子
     */
    @AfterMapping
    protected void afterMapping(UserEntity entity, @MappingTarget UserDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        // 复杂业务逻辑：根据 isDeleted 设置状态值
        if (Boolean.TRUE.equals(entity.getIsDeleted())) {
            dto.setStatus(-1);
        } else {
            dto.setStatus(1);
        }
    }
}
