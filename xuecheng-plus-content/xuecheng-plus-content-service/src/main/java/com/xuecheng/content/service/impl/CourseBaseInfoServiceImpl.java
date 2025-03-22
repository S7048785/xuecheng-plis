package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.constant.CourseConstant;
import com.xuecheng.base.constant.MessageConstant;
import com.xuecheng.base.exception.CommonException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {

        // 构建条件查询
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<CourseBase>()
                .like(StringUtils.isNotBlank(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName())
                .eq(StringUtils.isNotBlank(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus())
                .eq(StringUtils.isNotBlank(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());

        //构建分页查询对象
        Page<CourseBase> courseBasePage = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        courseBaseMapper.selectPage(courseBasePage, queryWrapper);

        List<CourseBase> records = courseBasePage.getRecords();
        long total = courseBasePage.getTotal();;
        return new PageResult<CourseBase>(records, total, pageParams.getPageNo(), pageParams.getPageSize());
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {

        // 新增对象
        CourseBase courseBaseNew = new CourseBase();
        // 将填写的课程赋值给新增对象
        BeanUtils.copyProperties(addCourseDto, courseBaseNew);
        // 设置审核状态
        courseBaseNew.setAuditStatus("202002");
        // 设置发布状态
        courseBaseNew.setStatus("203001");
        // 机构id
        courseBaseNew.setCompanyId(companyId);
        // 添加时间
        courseBaseNew.setCreateDate(LocalDateTime.now());
        // 插入课程基本信息表
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new RuntimeException("新增课程基本信息失败");
        }
        // TODO: 向课程营销表保存课程营销信息
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto, courseMarket);
        courseMarket.setId(courseBaseNew.getId());
        // 先查询课程营销信息，如果存在则更新，不存在则插入
        CourseMarket courseMarket1 = courseMarketMapper.selectById(courseMarket.getId());
        if (courseMarket1 == null) {
            // 不存在，插入
            courseMarketMapper.insert(courseMarket);
        } else {
            courseMarketMapper.updateById(courseMarket);
        }
        // TODO: 返回课程基本信息及营销信息
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        return getCourseBaseInfoDto(courseBaseNew, courseBaseInfoDto, courseMarket);
    }

    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long id) {
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (courseBase != null) {
            CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
            CourseMarket courseMarket = courseMarketMapper.selectById(id);
            return getCourseBaseInfoDto(courseBase, courseBaseInfoDto, courseMarket);
        }
        return null;
    }



    @Transactional
    @Override
    public CourseBaseInfoDto updateCourseBaseInfo(Long companyId, EditCourseDto editCourseDto) {
        // 先查询课表是否存在
        CourseBase courseBase = courseBaseMapper.selectById(editCourseDto.getId());
        if (courseBase == null)
            CommonException.cast(CourseConstant.COURSE_NULL);
        //校验本机构只能修改本机构的课程
        if(!courseBase.getCompanyId().equals(companyId)){
            CommonException.cast(CourseConstant.COURSE_NOT_BELONG_TO_COMPANY);
        }
        // 封装课程基本信息
        BeanUtils.copyProperties(editCourseDto, courseBase);
        // 更新课程基本信息
        courseBaseMapper.updateById(courseBase);
        // 封装营销信息数据
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        // 更新营销信息
        courseMarketMapper.updateById(courseMarket);
        return getCourseBaseInfoDto(courseBase, new CourseBaseInfoDto(), courseMarket);
    }

    @Transactional
    @Override
    public void deleteCourseBase(Long companyId, Long courseId) {
        // 验证课程状态是否是未提交
        CourseBase courseBase = courseBaseMapper.selectOne(new LambdaQueryWrapper<CourseBase>().eq(CourseBase::getId, courseId));
        if (courseBase == null || !"202002".equals(courseBase.getAuditStatus())) {
            CommonException.cast(CourseConstant.COURSE_NOT_SUBMIT);
        }
        if (!courseBase.getCompanyId().equals(companyId)) {
            CommonException.cast(CourseConstant.COURSE_NOT_BELONG_TO_COMPANY_DELETE);
        }
        // 删除课程相关的基本信息、营销信息、课程计划、课程教师信息。\
        courseMarketMapper.deleteById(courseId);
        teachplanMapper.delete(new LambdaQueryWrapper<Teachplan>().eq(Teachplan::getCourseId, courseId));
        courseTeacherMapper.delete(new LambdaQueryWrapper<CourseTeacher>().eq(CourseTeacher::getCourseId, courseId));
        courseBaseMapper.deleteById(courseId);
    }

    // 封装课程基本信息及营销信息
    private CourseBaseInfoDto getCourseBaseInfoDto(CourseBase courseBase, CourseBaseInfoDto courseBaseInfoDto, CourseMarket courseMarket) {
        // 课程基本信息
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        // 课程价格
        BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        // 课程分类名
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());
        return courseBaseInfoDto;
    }
}
