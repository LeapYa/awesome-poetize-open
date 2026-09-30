package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 资源回收站（替换旧版备份）表
 * </p>
 *
 * <p>替换成功后旧文件备份不再直接删除，而是登记在此，保留期内可恢复误替换；
 * 超期由每日定时任务物理清理。</p>
 */
@Data
@TableName("resource_trash")
public class ResourceTrash implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条目类型：替换旧版备份
     */
    public static final String TYPE_REPLACEMENT_BACKUP = "REPLACEMENT_BACKUP";

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 资源ID
     */
    @TableField("resource_id")
    private Integer resourceId;

    /**
     * 资源公开ID
     */
    @TableField("public_id")
    private String publicId;

    /**
     * 条目类型
     */
    @TableField("item_type")
    private String itemType;

    /**
     * 被替换的目标文件路径（恢复目标）
     */
    @TableField("original_path")
    private String originalPath;

    /**
     * 备份文件路径
     */
    @TableField("backup_path")
    private String backupPath;

    /**
     * 备份路径SHA-256（唯一键用，规避长路径前缀索引冲突）
     */
    @TableField("backup_path_hash")
    private String backupPathHash;

    /**
     * 备份文件SHA-256
     */
    @TableField("file_hash")
    private String fileHash;

    /**
     * 备份文件大小(字节)
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 备份文件MIME类型
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 资源原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * 进入回收站时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
