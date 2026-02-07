package com.ld.poetry.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.Resource;
import com.ld.poetry.enums.PoetryEnum;
import com.ld.poetry.service.ResourceService;
import com.ld.poetry.utils.storage.StoreService;
import com.ld.poetry.utils.*;
import com.ld.poetry.utils.storage.FileStorageService;
import com.ld.poetry.utils.image.ImageCompressUtil;
import com.ld.poetry.utils.image.IcoConvertUtil;
import com.ld.poetry.utils.security.FileSecurityValidator;
import com.ld.poetry.vo.BaseRequestVO;
import com.ld.poetry.vo.FileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 资源信息 前端控制器
 * </p>
 *
 * @author sara
 * @since 2022-03-06
 */
@SuppressWarnings("unchecked")
@RestController
@RequestMapping("/resource")
@Slf4j
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileSecurityValidator fileSecurityValidator;

    /**
     * 保存
     */
    @PostMapping("/saveResource")
    @LoginCheck
    public PoetryResult saveResource(@RequestBody Resource resource) {
        if (!StringUtils.hasText(resource.getType()) || !StringUtils.hasText(resource.getPath())) {
            return PoetryResult.fail("资源类型和资源路径不能为空！");
        }
        
        // 检查文件大小是否超过Integer.MAX_VALUE，防止溢出
        if (resource.getSize() != null && resource.getSize() > Integer.MAX_VALUE) {
            log.error("资源大小超过系统限制: {} bytes, 最大允许: {} bytes", resource.getSize(), Integer.MAX_VALUE);
            return PoetryResult.fail("资源大小超过系统限制(" + (Integer.MAX_VALUE / 1024 / 1024) + "MB)，请使用较小的文件");
        }
        
        Resource re = new Resource();
        re.setPath(resource.getPath());
        re.setType(resource.getType());
        re.setSize(resource.getSize());
        re.setOriginalName(resource.getOriginalName());
        re.setMimeType(resource.getMimeType());
        re.setStoreType(resource.getStoreType());
        re.setUserId(PoetryUtil.getUserId());
        
        try {
            // 先查询是否已存在相同路径的资源
            Resource existingResource = resourceService.lambdaQuery()
                .eq(Resource::getPath, resource.getPath())
                .one();
            
            if (existingResource != null) {
                // 如果存在，更新资源信息
                existingResource.setType(resource.getType());
                existingResource.setSize(resource.getSize());
                existingResource.setOriginalName(resource.getOriginalName());
                existingResource.setMimeType(resource.getMimeType());
                existingResource.setStoreType(resource.getStoreType());
                existingResource.setUserId(PoetryUtil.getUserId());
                resourceService.updateById(existingResource);
            } else {
                // 不存在则保存新记录
        resourceService.save(re);
            }
        } catch (Exception e) {
            log.error("保存资源信息失败: {}", e.getMessage(), e);
            return PoetryResult.fail("保存资源信息失败: " + e.getMessage());
        }
        
        return PoetryResult.success();
    }

    /**
     * 上传文件（支持智能图片压缩）
     */
    @PostMapping("/upload")
    @LoginCheck
    @Transactional(rollbackFor = Exception.class)
    public synchronized PoetryResult<String> upload(@RequestParam("file") MultipartFile file, FileVO fileVO) {
        if (file == null || !StringUtils.hasText(fileVO.getType()) || !StringUtils.hasText(fileVO.getRelativePath())) {
            return PoetryResult.fail("文件和资源类型和资源路径不能为空！");
        }

        try {
            // 验证文件安全性
            FileSecurityValidator.ValidationResult validationResult =
                    fileSecurityValidator.validateFile(file, file.getOriginalFilename(), file.getContentType());

            if (!validationResult.isSuccess()) {
                log.warn("文件安全验证失败: {}, 用户ID: {}", validationResult.getMessage(), PoetryUtil.getUserId());
                return PoetryResult.fail("文件验证失败: " + validationResult.getMessage());
            }

            log.info("文件安全验证通过: {}, Content-Type: {}", file.getOriginalFilename(), file.getContentType());
            MultipartFile processedFile = file;
            String originalFileName = file.getOriginalFilename();
            long originalSize = file.getSize();

            // 尝试智能压缩（仅对图片有效）
            // 网站标签页图标：无论上传 PNG/JPEG/WebP 等，统一转为 ICO 便于浏览器兼容
            // 包含手动上传(seoSiteIcon)与智能图标生成后上传(seoFavicon)
            boolean isSiteFavicon = "seoSiteIcon".equals(fileVO.getType()) || "seoFavicon".equals(fileVO.getType());
            if (isSiteFavicon) {
                byte[] icoBytes = IcoConvertUtil.convertToIco(file);
                if (icoBytes != null && icoBytes.length > 0) {
                    processedFile = new CompressedMultipartFile(
                            file.getName(),
                            originalFileName != null && originalFileName.endsWith(".ico") ? originalFileName : originalFileName != null ? originalFileName.replaceAll("\\.[^.]+$", "") + ".ico" : "favicon.ico",
                            "image/x-icon",
                            icoBytes
                    );
                    String oldRelativePath = fileVO.getRelativePath();
                    String newRelativePath = updateExtension(oldRelativePath, "ico");
                    if (!oldRelativePath.equals(newRelativePath)) {
                        fileVO.setRelativePath(newRelativePath);
                        log.info("网站图标已转为 ICO，路径: {} -> {}", oldRelativePath, newRelativePath);
                    }
                } else {
                    log.warn("网站图标转 ICO 失败，将按原文件保存: {}", originalFileName);
                }
            } else {
                try {
                    ImageCompressUtil.CompressResult compressResult = ImageCompressUtil.smartCompress(file);

                    // 创建压缩后的文件对象
                    processedFile = new CompressedMultipartFile(
                            file.getName(),
                            originalFileName,
                            compressResult.getContentType(),
                            compressResult.getData()
                    );

                    // 如果压缩后格式发生改变，更新文件路径的扩展名
                    String newExtension = getExtensionFromContentType(compressResult.getContentType());
                    if (!newExtension.isEmpty()) {
                        String oldRelativePath = fileVO.getRelativePath();
                        String newRelativePath = updateExtension(oldRelativePath, newExtension);
                        if (!oldRelativePath.equals(newRelativePath)) {
                            fileVO.setRelativePath(newRelativePath);
                            log.info("文件已转换格式，更新路径: {} -> {}", oldRelativePath, newRelativePath);
                        }
                    }

                } catch (IOException e) {
                    // 压缩失败时使用原文件（非图片文件会走到这里）
                }
            }

            // 在存储前检查文件大小是否超过Integer.MAX_VALUE，防止溢出
            long fileSize = processedFile.getSize();
            if (fileSize > Integer.MAX_VALUE) {
                log.error("文件大小超过系统限制: {} bytes, 最大允许: {} bytes", fileSize, Integer.MAX_VALUE);
                return PoetryResult.fail("文件大小超过系统限制(" + (Integer.MAX_VALUE / 1024 / 1024) + "MB)，请上传较小的文件");
            }

            fileVO.setFile(processedFile);
            StoreService storeService = fileStorageService.getFileStorage(fileVO.getStoreType());
            FileVO result = storeService.saveFile(fileVO);
            // log.info("文件上传成功 - 路径: {}", result.getVisitPath());

            Resource re = new Resource();
            re.setPath(result.getVisitPath());
            re.setType(fileVO.getType());
            re.setSize(Integer.valueOf(Long.toString(fileSize)));
            re.setMimeType(processedFile.getContentType());
            re.setStoreType(fileVO.getStoreType());
            re.setOriginalName(fileVO.getOriginalName());
            re.setUserId(PoetryUtil.getUserId());
            
            // 先查询是否已存在相同路径的资源
            Resource existingResource = resourceService.lambdaQuery()
                .eq(Resource::getPath, result.getVisitPath())
                .one();
            
            if (existingResource != null) {
                // 如果存在，更新资源信息
                existingResource.setType(fileVO.getType());
                existingResource.setSize(Integer.valueOf(Long.toString(fileSize)));
                existingResource.setOriginalName(fileVO.getOriginalName());
                existingResource.setMimeType(processedFile.getContentType());
                existingResource.setStoreType(fileVO.getStoreType());
                existingResource.setUserId(PoetryUtil.getUserId());
                resourceService.updateById(existingResource);
            } else {
                // 不存在则保存新记录
            resourceService.save(re);
            }
            
            return PoetryResult.success(result.getVisitPath());
            
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return PoetryResult.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 智能图片压缩上传（专用接口）
     */
    @PostMapping("/uploadImageWithCompress")
    @LoginCheck
    @Transactional(rollbackFor = Exception.class)
    public synchronized PoetryResult<Object> uploadImageWithCompress(
            @RequestParam("file") MultipartFile file,
            FileVO fileVO,
            @RequestParam(value = "maxWidth", defaultValue = "1920") int maxWidth,
            @RequestParam(value = "maxHeight", defaultValue = "1080") int maxHeight,
            @RequestParam(value = "quality", defaultValue = "0.85") float quality,
            @RequestParam(value = "targetSize", defaultValue = "512000") long targetSize) {

        if (file == null || !StringUtils.hasText(fileVO.getType()) || !StringUtils.hasText(fileVO.getRelativePath())) {
            return PoetryResult.fail("文件和资源类型和资源路径不能为空！");
        }

        try {
            // 验证文件安全性
            FileSecurityValidator.ValidationResult validationResult =
                    fileSecurityValidator.validateFile(file, file.getOriginalFilename(), file.getContentType());

            if (!validationResult.isSuccess()) {
                log.warn("文件安全验证失败: {}, 用户ID: {}", validationResult.getMessage(), PoetryUtil.getUserId());
                return PoetryResult.fail("文件验证失败: " + validationResult.getMessage());
            }

            log.info("智能压缩上传 - 文件安全验证通过: {}, Content-Type: {}", file.getOriginalFilename(), file.getContentType());

            // 执行智能压缩
            ImageCompressUtil.CompressResult compressResult =
                    ImageCompressUtil.smartCompress(file, maxWidth, maxHeight, quality, targetSize);

            // 创建压缩后的文件
            MultipartFile compressedFile = new CompressedMultipartFile(
                    file.getName(),
                    file.getOriginalFilename(),
                    compressResult.getContentType(),
                    compressResult.getData()
            );

            // 如果压缩后格式发生改变，更新文件路径的扩展名
            String newExtension = getExtensionFromContentType(compressResult.getContentType());
            if (!newExtension.isEmpty()) {
                String oldRelativePath = fileVO.getRelativePath();
                String newRelativePath = updateExtension(oldRelativePath, newExtension);
                if (!oldRelativePath.equals(newRelativePath)) {
                    fileVO.setRelativePath(newRelativePath);
                    log.info("文件已转换格式，更新路径: {} -> {}", oldRelativePath, newRelativePath);
                }
            }

            // 在存储前检查压缩后文件大小是否超过Integer.MAX_VALUE，防止溢出
            long fileSize = compressedFile.getSize();
            if (fileSize > Integer.MAX_VALUE) {
                log.error("压缩后文件大小超过系统限制: {} bytes, 最大允许: {} bytes", fileSize, Integer.MAX_VALUE);
                return PoetryResult.fail("压缩后文件大小超过系统限制(" + (Integer.MAX_VALUE / 1024 / 1024) + "MB)，请调整压缩参数");
            }

            fileVO.setFile(compressedFile);
            StoreService storeService = fileStorageService.getFileStorage(fileVO.getStoreType());
            FileVO result = storeService.saveFile(fileVO);

            Resource re = new Resource();
            re.setPath(result.getVisitPath());
            re.setType(fileVO.getType());
            re.setSize(Integer.valueOf(Long.toString(fileSize)));
            re.setMimeType(compressedFile.getContentType());
            re.setStoreType(fileVO.getStoreType());
            re.setOriginalName(fileVO.getOriginalName());
            re.setUserId(PoetryUtil.getUserId());
            
            // 先查询是否已存在相同路径的资源
            Resource existingResource = resourceService.lambdaQuery()
                .eq(Resource::getPath, result.getVisitPath())
                .one();
            
            if (existingResource != null) {
                // 如果存在，更新资源信息
                existingResource.setType(fileVO.getType());
                existingResource.setSize(Integer.valueOf(Long.toString(fileSize)));
                existingResource.setOriginalName(fileVO.getOriginalName());
                existingResource.setMimeType(compressedFile.getContentType());
                existingResource.setStoreType(fileVO.getStoreType());
                existingResource.setUserId(PoetryUtil.getUserId());
                resourceService.updateById(existingResource);
            } else {
                // 不存在则保存新记录
            resourceService.save(re);
            }

            log.info("智能压缩上传成功 - 路径: {}, 压缩率: {:.1f}%", 
                    result.getVisitPath(), compressResult.getCompressionRatio());

            // 返回详细的压缩信息
            return PoetryResult.success(new Object() {
                public final String visitPath = result.getVisitPath();
                public final long originalSize = compressResult.getOriginalSize();
                public final long compressedSize = compressResult.getCompressedSize();
                public final double compressionRatio = compressResult.getCompressionRatio();
                public final String contentType = compressResult.getContentType();
            });
            
        } catch (Exception e) {
            log.error("智能压缩上传失败: {}", e.getMessage(), e);
            return PoetryResult.fail("智能压缩上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传看板娘模型预览图（管理员专用）
     */
    @PostMapping("/uploadWaifuPreview")
    @LoginCheck(0)
    public PoetryResult<String> uploadWaifuPreview(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return PoetryResult.fail("请选择要上传的图片！");
        }
        
        try {
            // 验证是图片文件
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return PoetryResult.fail("只能上传图片文件！");
            }
            
            // 验证文件大小（2MB限制）
            if (file.getSize() > 2 * 1024 * 1024) {
                return PoetryResult.fail("图片大小不能超过2MB！");
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".png";
            String fileName = "waifu_preview_" + System.currentTimeMillis() + extension;
            
            // 使用FileVO存储
            FileVO fileVO = new FileVO();
            fileVO.setFile(file);
            fileVO.setType("waifuPreview");
            fileVO.setRelativePath("waifu_previews/" + fileName);
            fileVO.setOriginalName(originalFilename);
            
            StoreService storeService = fileStorageService.getFileStorage(fileVO.getStoreType());
            FileVO result = storeService.saveFile(fileVO);
            
            log.info("看板娘预览图上传成功: {}", result.getVisitPath());
            return PoetryResult.success(result.getVisitPath());
            
        } catch (Exception e) {
            log.error("看板娘预览图上传失败: {}", e.getMessage(), e);
            return PoetryResult.fail("上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除
     */
    @PostMapping("/deleteResource")
    @LoginCheck(0)
    public PoetryResult deleteResource(@RequestParam("path") String path) {
        Resource resource = resourceService.lambdaQuery().select(Resource::getStoreType).eq(Resource::getPath, path).one();
        if (resource == null) {
            return PoetryResult.fail("文件不存在：" + path);
        }

        StoreService storeService = fileStorageService.getFileStorageByStoreType(resource.getStoreType());
        storeService.deleteFile(Collections.singletonList(path));
        return PoetryResult.success();
    }

    /**
     * 查询表情包
     */
    @GetMapping("/getImageList")
    @LoginCheck
    public PoetryResult<List<String>> getImageList() {
        List<Resource> list = resourceService.lambdaQuery().select(Resource::getPath)
                .eq(Resource::getType, CommonConst.PATH_TYPE_INTERNET_MEME)
                .eq(Resource::getStatus, PoetryEnum.STATUS_ENABLE.getCode())
                .eq(Resource::getUserId, PoetryUtil.getAdminUser().getId())
                .orderByDesc(Resource::getCreateTime)
                .list();
        List<String> paths = list.stream().map(Resource::getPath).collect(Collectors.toList());
        return PoetryResult.success(paths);
    }

    /**
     * 查询资源
     */
    @PostMapping("/listResource")
    @LoginCheck(0)
    public PoetryResult<Page> listResource(@RequestBody BaseRequestVO baseRequestVO) {
        Page<Resource> page = new Page<>(baseRequestVO.getCurrent(), baseRequestVO.getSize());
        resourceService.lambdaQuery()
                .eq(StringUtils.hasText(baseRequestVO.getResourceType()), Resource::getType, baseRequestVO.getResourceType())
                .orderByDesc(Resource::getCreateTime).page(page);
        baseRequestVO.setRecords(page.getRecords());
        baseRequestVO.setTotal(page.getTotal());
        return PoetryResult.success(baseRequestVO);
    }

    /**
     * 修改资源状态
     */
    @GetMapping("/changeResourceStatus")
    @LoginCheck(0)
    public PoetryResult changeResourceStatus(@RequestParam("id") Integer id, @RequestParam("flag") Boolean flag) {
        resourceService.lambdaUpdate().eq(Resource::getId, id).set(Resource::getStatus, flag).update();
        return PoetryResult.success();
    }

    /**
     * 根据ContentType获取对应的文件扩展名
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        if (contentType.contains("webp")) {
            return "webp";
        } else if (contentType.contains("x-icon") || contentType.contains("ico")) {
            return "ico";
        } else if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return "jpg";
        } else if (contentType.contains("png")) {
            return "png";
        } else if (contentType.contains("gif")) {
            return "gif";
        }
        return "";
    }

    /**
     * 更新文件路径的扩展名
     */
    private String updateExtension(String filePath, String newExtension) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }

        // 移除原有扩展名
        int lastDotIndex = filePath.lastIndexOf('.');
        int lastSlashIndex = filePath.lastIndexOf('/');

        // 只有当点号在最后一个斜杠之后，才认为是文件扩展名
        if (lastDotIndex > lastSlashIndex) {
            String nameWithoutExt = filePath.substring(0, lastDotIndex);
            return nameWithoutExt + "." + newExtension;
        } else {
            // 没有扩展名，直接添加
            return filePath + "." + newExtension;
        }
    }

    /**
     * 自定义MultipartFile实现，用于压缩后的文件数据
     */
    private static class CompressedMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public CompressedMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.contentType = contentType;
            this.content = content;
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getOriginalFilename() {
            return this.originalFilename;
        }

        @Override
        public String getContentType() {
            return this.contentType;
        }

        @Override
        public boolean isEmpty() {
            return this.content.length == 0;
        }

        @Override
        public long getSize() {
            return this.content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return this.content;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(this.content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(this.content);
            }
        }
    }
}

