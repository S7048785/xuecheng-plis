package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {

        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        // 拿到二级节点
        List<CourseCategoryTreeDto> categoryParentNode = courseCategoryTreeDtos.stream().filter(item -> id.equals(item.getParentid())).collect(Collectors.toList());
        // 为二级节点添加三级节点
        categoryParentNode.forEach(item -> {
            item.setChildrenTreeNodes(courseCategoryTreeDtos.stream().filter(item2 -> item.getId().equals(item2.getParentid())).collect(Collectors.toList()));
        });
        return categoryParentNode;
    }
}
