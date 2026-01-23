<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        h2 { color: #006699; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .success { color: green; font-weight: bold; }
        .failure { color: red; }
        .summary-success { color: green; font-weight: bold; margin-top: 15px; }
        .summary-failure { color: red; margin-top: 15px; }
        .footer { margin-top: 20px; font-size: 12px; color: #777; }
    </style>
</head>
<body>
    <h2>搜索引擎推送结果通知</h2>
    <p>您的文章 <strong>"${title}"</strong> 已提交到搜索引擎。</p>
    <p>文章链接: <a href="${url}">${url}</a></p>
    <p>推送时间: ${timestamp}</p>
    
    <h3>推送结果详情:</h3>
    <table>
        <tr>
            <th>搜索引擎</th>
            <th>状态</th>
            <th>详情</th>
        </tr>
        <#if results?has_content>
            <#list results as engine, details>
                <tr>
                    <td>${getSearchEngineName(engine)}</td>
                    <#if details.success?string == "true">
                        <td class="success">成功</td>
                    <#else>
                        <td class="failure">失败</td>
                    </#if>
                    <td>
                        <#if details.result??>${details.result}<#elseif details.message??>${details.message}<#else></#if>
                    </td>
                </tr>
            </#list>
        <#else>
            <tr><td colspan="3">无推送结果数据</td></tr>
        </#if>
    </table>
    
    <#if success>
        <p class="summary-success">推送总结: 至少有一个搜索引擎推送成功。</p>
    <#else>
        <p class="summary-failure">推送总结: 所有搜索引擎推送均失败。</p>
    </#if>
    
    <div class="footer">
        <p>此邮件由系统自动发送，请勿回复。</p>
    </div>
</body>
</html>
