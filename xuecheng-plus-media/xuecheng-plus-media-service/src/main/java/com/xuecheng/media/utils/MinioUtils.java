package com.xuecheng.media.utils;

import com.xuecheng.base.constant.MessageConstant;
import com.xuecheng.base.exception.CommonException;
import com.xuecheng.media.config.MinioConfig;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class MinioUtils {

    @Autowired
    private MinioClient minioClient;
    /**
     * 判断bucket是否存在，不存在则创建
     */
    public boolean existBucket(String name) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build());
            if (!exists) {
                // bucket不存在，创建bucket
//                minioClient.makeBucket(MakeBucketArgs.builder().bucket(name).build());
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断文件是否存在
     * @param bucketName 桶名
     * @param objectName 文件名
     * @return 存在返回true
     */
    public boolean existFile(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            // 文件存在
            return true;
        } catch (Exception e) {
            if (e instanceof ErrorResponseException) {
                // 文件不存在
                return false;
            }
            // 其他异常
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 上传文件
     * @param bucketName 桶名
     * @param objectName 文件名
     * @param file 文件
     */
    public String uploadFile(String bucketName, String objectName, MultipartFile file) {
        // 获取当前年月
        String dir1 = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 文件名
        String fileName = dir1 + "/" + UUID.randomUUID() + objectName.substring(objectName.lastIndexOf("."));
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            // 存储桶名称
                            .bucket(bucketName)
                            //文件在MinIO中的名称
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            log.info("上传文件成功");
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件失败");
            throw new CommonException(MessageConstant.UPLOAD_ERROR);
        }
    }

    /**
     * 获取文件url
     * @param bucketName
     * @param objectName
     * @return
     */
    public String getFileUrl(String bucketName, String objectName) {
        // 获取文件url前判断是否存在
        boolean b = existFile(bucketName, objectName);
        if (!b) {
            // 文件不存在
            throw new CommonException(MessageConstant.FILE_NOT_EXIST);
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .method(Method.GET)
                            .object(objectName)
                            .expiry(60 * 60 * 24)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除文件
     */
    public void removeFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
