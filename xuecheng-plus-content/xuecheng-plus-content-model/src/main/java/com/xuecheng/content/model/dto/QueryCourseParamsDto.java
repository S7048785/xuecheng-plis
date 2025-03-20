package com.xuecheng.content.model.dto;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程查询条件模型类
 * @date 2023/2/11 15:37
 */
@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryCourseParamsDto {

    //审核状态
    private String auditStatus;
    //课程名称
    private String courseName;
    //发布状态
    private String publishStatus;

}
