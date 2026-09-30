package com.ld.poetry.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleDraftDetailVO {
    private String id;
    private String draftType;
    private Integer articleId;
    private String status;
    private String titleCache;
    private String crdtSnapshotBase64;
    private Integer ownerUserId;
    private String ownerUsername;
    private Integer lastEditorId;
    private String lastEditorUsername;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String sourceArticleTitle;
    private ArticleVO sourceArticle;
    /**
     * 文章最后修改时间是否晚于草稿最后编辑时间。
     *
     * <p>为 true 表示草稿可能已过期：例如自动化工具 / 其他设备 / API 直接改过文章，
     * 编辑页需要提示用户，避免静默用旧草稿覆盖这些更新。</p>
     */
    private Boolean articleUpdatedAfterDraft;
    private List<ArticleDraftCollaboratorVO> collaborators;
}
