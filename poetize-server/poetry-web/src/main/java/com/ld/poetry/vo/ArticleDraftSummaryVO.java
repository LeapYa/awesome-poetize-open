package com.ld.poetry.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleDraftSummaryVO {
    private String id;
    private String draftType;
    private Integer articleId;
    private String status;
    private String titleCache;
    private Integer ownerUserId;
    private String ownerUsername;
    private Integer lastEditorId;
    private String lastEditorUsername;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String sourceArticleTitle;
    /**
     * 草稿是否已有内容（快照非空）。
     * 未编辑过的空草稿（仅打开过编辑页）不应触发"是否继续编辑草稿"之类的询问。
     */
    private Boolean hasContent;
    private List<ArticleDraftCollaboratorVO> collaborators;
}
