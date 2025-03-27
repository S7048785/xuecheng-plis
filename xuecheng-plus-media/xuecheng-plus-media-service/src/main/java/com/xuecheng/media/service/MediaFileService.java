package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileVO;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

 /**
  * @description 媒资文件查询方法
  * @param pageParams 分页参数
  * @param queryMediaParamsDto 查询条件
  * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
  * @author Mr.M
  * @date 2022/9/10 8:57
 */
 PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    void deleteFile(String fileId);

    UploadFileVO upload(Long companyId, UploadFileParamsDto uploadFileParamsDto, MultipartFile upload) throws IOException;

    /**
     * @description 检查文件是否存在
     * @param fileMd5 文件的md5
     */
    RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * @description 检查分块是否存在
     * @param fileMd5  文件的md5
     * @param chunkIndex  分块序号
     */
    RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    RestResponse<Boolean> uploadChunk(String fileMd5, int chunk, MultipartFile file);

    RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);

}
