package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.constant.MessageConstant;
import com.xuecheng.base.exception.CommonException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.config.MinioConfig;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileVO;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.utils.MinioUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MinioUtils minioUtils;
    @Autowired
    MinioConfig minioConfig;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileVO upload(Long companyId, UploadFileParamsDto uploadFileParamsDto, MultipartFile upload) throws IOException {

        // 拿到文件的md5
        String fileMd5 = getFileMd5(upload.getInputStream());
        // 判断文件是否存在
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null)
            throw new CommonException(MessageConstant.FILE_EXIST);

        // 上传文件
        String originalFilename = upload.getOriginalFilename();
        if (originalFilename == null)
            // 文件名为空
            throw new CommonException(MessageConstant.FILE_NAME_NULL);
        String fileUrl = minioUtils.uploadFile(minioConfig.getBucketFiles(), originalFilename, upload);
        log.info("文件上传成功: {}", fileUrl);

        mediaFiles = new MediaFiles();
        BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
        //拷贝基本信息
        BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
        mediaFiles.setId(fileMd5);
        mediaFiles.setFileId(fileMd5);
        mediaFiles.setCompanyId(companyId);
        mediaFiles.setBucket(minioConfig.getBucketFiles());
        mediaFiles.setCreateDate(LocalDateTime.now());
        mediaFiles.setAuditStatus("002003");
        mediaFiles.setStatus("1");
        mediaFiles.setUrl("/" + minioConfig.getBucketFiles() + "/" + fileUrl);
        mediaFiles.setFilePath(fileUrl);


        // 存入数据库
        int insert = mediaFilesMapper.insert(mediaFiles);
        if (insert < 0) {
            // 保存文件信息到数据库失败
            throw new CommonException("保存文件信息到数据库失败");
        }
        UploadFileVO uploadFileVO = new UploadFileVO();
        BeanUtils.copyProperties(mediaFiles, uploadFileVO);
        return uploadFileVO;
    }

    private String getFileMd5(InputStream file) throws IOException {
        return DigestUtils.md5Hex(file);
    }

    @Override
    public void deleteFile(String fileId) {
        minioUtils.removeFile(minioConfig.getBucketFiles(), fileId);
    }
}
