package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.constant.MessageConstant;
import com.xuecheng.base.exception.CommonException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.config.MinioConfig;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileVO;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.utils.MinioUtils;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    MediaProcessMapper mediaProcessMapper;
    @Autowired
    MinioUtils minioUtils;
    @Autowired
    MinioConfig minioConfig;
    @Autowired
    private MinioClient minioClient;

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
    @Transactional
    @Override
    public UploadFileVO upload(Long companyId, UploadFileParamsDto uploadFileParamsDto, MultipartFile upload) throws IOException {

        // 拿到文件的md5
        String fileMd5 = getFileMd5(upload.getInputStream());
        // 判断文件是否存在
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
//            throw new CommonException(MessageConstant.FILE_EXIST);
            log.error("文件已存在");
            UploadFileVO uploadFileVO = new UploadFileVO();
            BeanUtils.copyProperties(mediaFiles, uploadFileVO);
            return uploadFileVO;
        }

        // 上传文件
        String originalFilename = upload.getOriginalFilename();
        if (originalFilename == null)
            // 文件名为空
            throw new CommonException(MessageConstant.FILE_NAME_NULL);
        String fileUrl = minioUtils.uploadFile(minioConfig.getBucketFiles(), originalFilename, upload);
        log.info("文件上传成功: {}", fileUrl);

        mediaFiles = new MediaFiles();
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
        // 添加到待处理任务表
        addWaitingTask(mediaFiles);
        UploadFileVO uploadFileVO = new UploadFileVO();
        BeanUtils.copyProperties(mediaFiles, uploadFileVO);
        return uploadFileVO;
    }
    /**
     * 添加待处理任务
     * @param mediaFiles 媒资文件信息
     */
    @Transactional
    public void addWaitingTask(MediaFiles mediaFiles){
        //文件名称
        String filename = mediaFiles.getFilename();
        //文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //文件mimeType
        String mimeType = getMimeType(extension);
        //如果是avi视频添加到视频待处理表
        if("video/x-msvideo".equals(mimeType)){
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles,mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setFailCount(0);
            mediaProcessMapper.insert(mediaProcess);
        }
    }

    private String getMimeType(String extension){
        if(extension==null)
            extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    private String getFileMd5(InputStream file) throws IOException {
        return DigestUtils.md5Hex(file);
    }

    @Override
    public void deleteFile(String fileId) {
        minioUtils.removeFile(minioConfig.getBucketFiles(), fileId);
    }

    /**
     *  检查文件是否存在
     * @param fileMd5 文件的md5
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null)
            return RestResponse.success();
        // 查看文件是否在minio中
//        String bucket = mediaFiles.getBucket();
//        String filePath = mediaFiles.getFilePath();
//        InputStream fis = minioUtils.getFileInputStream(bucket, getChunkFileFolderPath(fileMd5));
//        if (fis == null)
//            return RestResponse.success(false);
        return RestResponse.validfail("文件已存在");
    }


    /**
     *  检查分块是否存在
     * @param fileMd5  文件的md5
     * @param chunkIndex  分块序号
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        // 获取分块文件路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunkIndex;
        // 获取文件流
        InputStream fis = minioUtils.getFileInputStream(minioConfig.getBucketVideoFiles(), chunkFilePath);
        if (fis == null)
            return RestResponse.success(false);
        return RestResponse.success(true);
    }

    /**
     * @description 上传分块
     * @param fileMd5  文件md5
     * @param chunk  分块序号
     */
    @Override
    public RestResponse<Boolean> uploadChunk(String fileMd5, int chunk, MultipartFile file) {
        // 拼接分片路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        // 上传分块
        boolean b = minioUtils.uploadChunkFile(minioConfig.getBucketVideoFiles(), chunkFilePath, file);
        if (!b) {
            log.debug("上传分块文件失败:{}", chunkFilePath);
            return RestResponse.validfail(false, "上传分块失败");
        }
        log.debug("上传分块文件成功:{}",chunkFilePath);
        return RestResponse.success(true);
    }


    /**
     * 合并文件
     * @param companyId
     * @param fileMd5
     * @param chunkTotal
     * @param uploadFileParamsDto
     * @return
     */
    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        List<ComposeSource> sources = new ArrayList<>(chunkTotal);
        for (int i = 0; i < chunkTotal; i++) {
            sources.add(
                    ComposeSource.builder()
                            .bucket(minioConfig.getBucketVideoFiles())
                            .object(getChunkFileFolderPath(fileMd5) + i)
                            .build()
            );
        }
        // 合并后的文件存储路径
        String filePath = fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + uploadFileParamsDto.getFilename().substring(uploadFileParamsDto.getFilename().lastIndexOf("."));
        // 合并文件分片
        try {
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(minioConfig.getBucketVideoFiles())
                            .sources(sources)
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return RestResponse.validfail(false, "合并文件分片失败");
        }
        // 验证MD5
        // 下载合并后的文件
        try {
            InputStream object = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketVideoFiles())
                            .object(filePath)
                            .build()
            );
            // 获取md5
            String minioFileMd5 = getFileMd5(object);
            // 比较
            if (minioFileMd5.equals(fileMd5)) {
                log.debug("合并文件分片成功:{}", filePath);
                // 删除分片文件
                minioUtils.removeChunks(minioConfig.getBucketVideoFiles(), getChunkFileFolderPath(fileMd5), chunkTotal);
                // 文件信息插入数据库中
                MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
                if (mediaFiles == null) {
                    mediaFiles = new MediaFiles();
                    BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
                    mediaFiles.setId(fileMd5);
                    mediaFiles.setFileId(fileMd5);
                    mediaFiles.setCompanyId(companyId);
                    mediaFiles.setBucket(minioConfig.getBucketVideoFiles());
                    mediaFiles.setCreateDate(LocalDateTime.now());
                    mediaFiles.setAuditStatus("002003");
                    mediaFiles.setStatus("1");
                    mediaFiles.setUrl("/" + minioConfig.getBucketVideoFiles() + "/" + filePath);
                    mediaFiles.setFilePath(filePath);
                    // 存入数据库
                    int insert = mediaFilesMapper.insert(mediaFiles);
                }
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RestResponse.validfail(false, "合并文件分片失败");
        }
        return null;
    }



    //得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

}
