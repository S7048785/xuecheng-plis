package com.xuecheng.media.model.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.xuecheng.media.model.po.MediaFiles;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description 上传普通文件成功响应结果
 * @author Mr.M
 * @date 2022/9/12 18:49
 * @version 1.0
 */
@Data
public class UploadFileVO {

    private String id;
    // 机构id
    private Long companyId;
    // 机构名称
    private String companyName;
    // 文件名称
    private String filename;
    // 文件类型（文档，音频，视频）
    private String fileType;
    // 标签
    private String tags;
    // 存储目录
    private String bucket;
    // 存储路径
    private String filePath;
    // 文件标识
    private String fileId;
    // 媒资文件访问地址
    private String url;
    // 上传人
    private String username;
    // 上传时间
    private LocalDateTime createDate;
    // 修改时间
    private LocalDateTime changeDate;
    // 状态,1:未处理，视频处理完成更新为2
    private String status;
    // 备注
    private String remark;
    // 审核状态
    private String auditStatus;
    // 审核意见
    private String auditMind;
    // 文件大小
    private Long fileSize;
}