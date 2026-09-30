import request from './request';
import constant from './constant';

/**
 * 回收站与历史版本保留策略。
 *
 * 保留天数由后端下发（/admin/recycle/config），前端不再硬编码；
 * 请求失败或未返回时退回默认值，保证提示文案始终可用。
 */
const DEFAULT_RETENTION_CONFIG = Object.freeze({
  articleRetentionDays: 30,
  resourceRetentionDays: 30,
  backupRetentionDays: 30,
  versionRetentionDays: 90,
  maxVersionsPerArticle: 20
});

let retentionConfig = { ...DEFAULT_RETENTION_CONFIG };
let inflightRequest = null;

export function getRetentionConfig() {
  return retentionConfig;
}

/**
 * 拉取保留策略（并发复用同一请求，成功后缓存；失败保留默认值并允许下次重试）
 */
export function fetchRetentionConfig() {
  if (inflightRequest) {
    return inflightRequest;
  }
  inflightRequest = request
    .get(constant.baseURL + '/admin/recycle/config', {}, true)
    .then((res) => {
      const data = res && res.data ? res.data : null;
      if (data && typeof data === 'object') {
        retentionConfig = { ...DEFAULT_RETENTION_CONFIG, ...data };
      }
      return retentionConfig;
    })
    .catch(() => {
      inflightRequest = null;
      return retentionConfig;
    });
  return inflightRequest;
}
