package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.base.exception.CommonException;
import com.xuecheng.base.exception.RestErrorResponse;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;
    @Override
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        Long id = teachplanDto.getId();
        if (id == null) {
            // 新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(teachplanDto, teachplan);
            Integer count = teachplanMapper.selectCount(
                    new LambdaQueryWrapper<Teachplan>()
                            .eq(Teachplan::getCourseId, teachplanDto.getCourseId())
                            .eq(Teachplan::getParentid, teachplanDto.getParentid())
            );
            teachplan.setOrderby(count + 1);
            teachplanMapper.insert(teachplan);
        } else {
            // 修改
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto, teachplan);

            teachplanMapper.updateById(teachplan);

        }
    }

    @Transactional
    @Override
    public void deleteTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (teachplan == null) {
            return;
        }
        if (teachplan.getParentid() == 0) {
            // 删除第一级别的大章节时 要求下面没有小章节时方可删除
            Integer count = teachplanMapper.selectCount(
                    new LambdaQueryWrapper<Teachplan>()
                            .eq(Teachplan::getParentid, id)
            );
            if (count > 0) {
                throw new CommonException(120409, "课程计划信息还有子级信息，无法操作");
            }
            // 删除大章节
        } else {
            // 删除第二级别的小章节时 需要将teachplan_media表关联的信息也删除
            teachplanMediaMapper.delete(
                    new LambdaQueryWrapper<TeachplanMedia>()
                            .eq(TeachplanMedia::getTeachplanId, id)
            );
        }
        teachplanMapper.deleteById(id);

    }

    @Transactional
    @Override
    public void moveDown(Long id) {
        // 判断一级课程还是二级课程
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (teachplan.getParentid() == 0) {
            // 一级课程 找到它的下面的一级课程 交换orderBy字段值
            List<Teachplan> teachplans = teachplanMapper.selectList(
                    new LambdaQueryWrapper<Teachplan>()
                            .eq(Teachplan::getCourseId, teachplan.getCourseId())
                            .eq(Teachplan::getParentid, 0)
                            .gt(Teachplan::getOrderby, teachplan.getOrderby())
            );
            if (teachplans == null) {
                // 未找到 已经是最后一个
                log.info("已经是最后一个");
                return;
            }
            Teachplan teachplan1 = teachplans.get(0);
            // 交换orderBy字段值
            Integer orderby = teachplan.getOrderby();
            teachplan.setOrderby(teachplan1.getOrderby());
            teachplan1.setOrderby(orderby);
            // 更新数据
            teachplanMapper.updateById(teachplan);
            teachplanMapper.updateById(teachplan1);
        }
    }

    @Override
    public void moveUp(Long id) {
        // 判断一级课程还是二级课程
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (teachplan.getParentid() == 0) {
            // 一级课程 找到它的下面的一级课程 交换orderBy字段值
            List<Teachplan> teachplans = teachplanMapper.selectList(
                    new LambdaQueryWrapper<Teachplan>()
                            .eq(Teachplan::getCourseId, teachplan.getCourseId())
                            .eq(Teachplan::getParentid, 0)
                            .lt(Teachplan::getOrderby, teachplan.getOrderby())
            );
            if (teachplans == null){
                log.info("已经是第一个");
                return;
            }
            Teachplan teachplan1 = teachplans.get(teachplans.size() - 1);
            // 交换orderBy字段值
            Integer orderby = teachplan.getOrderby();
            teachplan.setOrderby(teachplan1.getOrderby());
            teachplan1.setOrderby(orderby);
            // 更新数据
            teachplanMapper.updateById(teachplan);
            teachplanMapper.updateById(teachplan1);
        }
    }

    @Override
    public List<CourseTeacher> getCourseTeacher(Long courseId) {
        return courseTeacherMapper.selectList(
                new LambdaQueryWrapper<CourseTeacher>()
                        .eq(CourseTeacher::getCourseId, courseId)
        );
    }

    @Override
    public CourseTeacher addCourseTeacher(CourseTeacher courseTeacher) {
        courseTeacherMapper.insert(courseTeacher);
        return courseTeacher;
    }

    @Override
    public void deleteCourseTeacher(Long id) {
        courseTeacherMapper.deleteById(id);
    }
}
