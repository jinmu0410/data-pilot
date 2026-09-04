package cn.datapilot.web.service;

import cn.datapilot.web.vo.file.FileData;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传
 *
 * @author jinmu
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param multipartFile 文件
     * @return 文件地址
     */
    FileData upload(MultipartFile multipartFile);

}
