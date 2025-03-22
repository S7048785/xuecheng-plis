package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
@RequiredArgsConstructor
public class TeachplanController {

    private final TeachplanService teachplanService;
    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId",name = "课程Id",required = true,dataType = "Long",paramType = "path")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
        return teachplanService.findTeachplanTree(courseId);
    }

    /**
     * 新增课程计划
     */
    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan( @RequestBody SaveTeachplanDto teachplan){
        teachplanService.saveTeachplan(teachplan);
    }

    /**
     * 删除课程计划
     */
    @ApiOperation("课程计划删除")
    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachplan(@PathVariable("id") Long id) {
        teachplanService.deleteTeachplan(id);
    }

    /**
     * 课程计划排序 下移
     */
    @ApiOperation("课程计划排序 上移")
    @PostMapping("/teachplan/movedown/{id}")
    public void moveDown(@PathVariable("id") Long id) {
        teachplanService.moveDown(id);
    }

    /**
     * 课程计划排序 上移
     */
    @ApiOperation("课程计划排序 下移")
    @PostMapping("/teachplan/moveup/{id}")
    public void moveUp(@PathVariable("id") Long id) {
        teachplanService.moveUp(id);
    }

    /**
     * 查询教师接口
     */
    @ApiOperation("查询教师接口")
    @GetMapping("/courseTeacher/list/{id}")
    public List<CourseTeacher> getCourseTeacher(@PathVariable("id") Long courseId) {
        return teachplanService.getCourseTeacher(courseId);
    }

    /**
     * 添加教师
     */
    @ApiOperation("添加教师")
    @PostMapping("/courseTeacher")
    public CourseTeacher addCourseTeacher(@RequestBody CourseTeacher courseTeacher) {
        return teachplanService.addCourseTeacher(courseTeacher);
    }

    /**
     * 修改教师
     */
    @ApiOperation("修改教师")
    @PutMapping("/courseTeacher")
    public CourseTeacher updateCourseTeacher(@RequestBody CourseTeacher courseTeacher) {
        return teachplanService.addCourseTeacher(courseTeacher);
    }

    /**
     * 删除教师
     */
    @ApiOperation("删除教师")
    @DeleteMapping("/courseTeacher/course/{id}/{teacherId}")
    public void deleteCourseTeacher(@PathVariable("id") Long teacherId) {
        teachplanService.deleteCourseTeacher(teacherId);
    }
}