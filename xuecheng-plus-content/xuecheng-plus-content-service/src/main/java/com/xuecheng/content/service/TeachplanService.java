package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

public interface TeachplanService {
    /**
     * @description 查询某课程的课程计划，组成树型结构
     * @param courseId
     * @return com.xuecheng.content.model.dto.TeachplanDto
     * @author Mr.M
     * @date 2022/9/9 11:10
     */
    List<TeachplanDto> findTeachplanTree(long courseId);

    void saveTeachplan(SaveTeachplanDto teachplan);

    void deleteTeachplan(Long id);

    void moveDown(Long id);

    void moveUp(Long id);

    List<CourseTeacher> getCourseTeacher(Long courseId);

    CourseTeacher addCourseTeacher(CourseTeacher courseTeacher);

    void deleteCourseTeacher(Long id);
}
