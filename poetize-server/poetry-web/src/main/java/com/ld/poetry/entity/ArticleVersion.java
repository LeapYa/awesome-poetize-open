package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 文章历史版本快照表
 * </p>
 *
 * <p>每次更新文章前自动落一份旧文全量快照（含全部语言翻译），恢复指定版本前
 * 也会先把当前版快照为 RESTORE 型，保证任何一步误操作都可回溯。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("article_version")
public class ArticleVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 快照类型：更新前
     */
    public static final String TYPE_UPDATE = "UPDATE";

    /**
     * 快照类型：恢复前（恢复操作前对当前版的快照，使恢复本身可撤销）
     */
    public static final String TYPE_RESTORE = "RESTORE";

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    @TableField("article_id")
    private Integer articleId;

    /**
     * 版本号（每篇文章自增）
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 快照类型 UPDATE:更新前/RESTORE:恢复前
     */
    @TableField("snapshot_type")
    private String snapshotType;

    /**
     * 操作人用户ID
     */
    @TableField("editor_user_id")
    private Integer editorUserId;

    /**
     * 操作人用户名
     */
    @TableField("editor_username")
    private String editorUsername;

    /**
     * 博文标题快照
     */
    @TableField("article_title")
    private String articleTitle;

    /**
     * URL别名快照
     */
    @TableField("article_slug")
    private String articleSlug;

    /**
     * 博文内容快照
     */
    @TableField("article_content")
    private String articleContent;

    /**
     * 文章摘要快照
     */
    @TableField("summary")
    private String summary;

    /**
     * 封面快照
     */
    @TableField("article_cover")
    private String articleCover;

    /**
     * 视频链接快照
     */
    @TableField("video_url")
    private String videoUrl;

    /**
     * 访问密码快照
     */
    @TableField("password")
    private String password;

    /**
     * 提示快照
     */
    @TableField("tips")
    private String tips;

    /**
     * 是否可见快照[0:否，1:是]
     */
    @TableField("view_status")
    private Boolean viewStatus;

    /**
     * 是否启用评论快照[0:否，1:是]
     */
    @TableField("comment_status")
    private Boolean commentStatus;

    /**
     * 是否推荐快照[0:否，1:是]
     */
    @TableField("recommend_status")
    private Boolean recommendStatus;

    /**
     * 是否推送搜索引擎快照[0:否，1:是]
     */
    @TableField("submit_to_search_engine")
    private Boolean submitToSearchEngine;

    /**
     * 付费类型快照
     */
    @TableField("pay_type")
    private Integer payType;

    /**
     * 付费金额快照(元)
     */
    @TableField("pay_amount")
    private BigDecimal payAmount;

    /**
     * 免费预览百分比快照(0-100)
     */
    @TableField("free_percent")
    private Integer freePercent;

    /**
     * 分类ID快照
     */
    @TableField("sort_id")
    private Integer sortId;

    /**
     * 标签ID快照
     */
    @TableField("label_id")
    private Integer labelId;

    /**
     * 首次公开发布时间快照
     */
    @TableField("publish_time")
    private LocalDateTime publishTime;

    /**
     * 全部语言翻译快照JSON[{language,title,content,summary}]
     */
    @TableField("translations_json")
    private String translationsJson;

    /**
     * 内容SHA-256（去重用）
     */
    @TableField("content_hash")
    private String contentHash;

    /**
     * 快照时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
