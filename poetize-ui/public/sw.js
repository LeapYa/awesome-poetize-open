// POETIZE PWA Service Worker
// 提供智能缓存和PWA功能，无自定义离线页面

const CACHE_NAME = 'poetize-pwa-v1.0.0';

// 需要预缓存的关键资源
const PRECACHE_RESOURCES = [
  '/',
  '/static/css/inline-styles.css',
  '/libs/css/highlight.min.css',
  '/libs/js/anime.min.js',
  '/libs/js/highlight.min.js',
  '/poetize.jpg'
];

// 安装Service Worker时预缓存关键资源
self.addEventListener('install', event => {
  console.log('SW: Service Worker installing');

  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('SW: Pre-caching critical resources');
        return cache.addAll(PRECACHE_RESOURCES);
      })
      .then(() => {
        console.log('SW: Pre-cache complete');
        // 强制激活新的Service Worker
        return self.skipWaiting();
      })
      .catch(error => {
        console.error('SW: Pre-cache failed:', error);
      })
  );
});

// 激活Service Worker时清理旧缓存
self.addEventListener('activate', event => {
  console.log('SW: Service Worker activating');

  event.waitUntil(
    caches.keys()
      .then(cacheNames => {
        return Promise.all(
          cacheNames.map(cacheName => {
            if (cacheName !== CACHE_NAME) {
              console.log('SW: Deleting old cache:', cacheName);
              return caches.delete(cacheName);
            }
          })
        );
      })
      .then(() => {
        console.log('SW: Cache cleanup complete');
        // 立即控制所有页面
        return self.clients.claim();
      })
  );
});

// 网络请求拦截和缓存策略
self.addEventListener('fetch', event => {
  const request = event.request;
  const url = new URL(request.url);

  // 只处理GET请求
  if (request.method !== 'GET') {
    return;
  }

  // 跳过chrome-extension请求
  if (url.protocol === 'chrome-extension:') {
    return;
  }

  // 不同类型资源使用不同缓存策略
  if (isPageRequest(request)) {
    // 页面请求：网络优先，失败时使用缓存或离线页面
    event.respondWith(handlePageRequest(request));
  } else if (isStaticAsset(request)) {
    // 静态资源：缓存优先，失败时从网络获取
    event.respondWith(handleStaticAsset(request));
  } else if (isApiRequest(request)) {
    // API请求：网络优先，失败时返回离线提示
    event.respondWith(handleApiRequest(request));
  }
});

// 检查是否为页面请求
function isPageRequest(request) {
  return request.mode === 'navigate' ||
    (request.method === 'GET' && request.headers.get('accept').includes('text/html'));
}

// 检查是否为静态资源
function isStaticAsset(request) {
  const url = new URL(request.url);
  return url.pathname.match(/\.(css|js|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot)$/);
}

// 检查是否为API请求
function isApiRequest(request) {
  const url = new URL(request.url);
  return url.pathname.startsWith('/api/') ||
    url.pathname.startsWith('/webInfo/') ||
    url.pathname.startsWith('/seo/');
}

// 处理页面请求
async function handlePageRequest(request) {
  try {
    // 首先尝试网络请求
    const networkResponse = await fetch(request);

    // 如果成功，缓存页面（仅缓存成功的页面）
    if (networkResponse.ok) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log('SW: Network failed for page, trying cache:', request.url);

    // 网络失败，尝试从缓存获取
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      console.log('SW: Serving cached page:', request.url);
      return cachedResponse;
    }

    // 缓存也没有，让请求失败，浏览器会显示默认离线页面
    console.log('SW: No cache available, letting request fail for browser default offline page');
    throw error;
  }
}

// 处理静态资源
async function handleStaticAsset(request) {
  try {
    // 首先检查缓存
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }

    // 缓存未命中，从网络获取
    const networkResponse = await fetch(request);

    // 缓存成功的响应
    if (networkResponse.ok) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log('SW: Static asset request failed:', request.url);
    // 静态资源失败时，可以返回占位符或者让请求失败
    throw error;
  }
}

// 处理API请求
async function handleApiRequest(request) {
  try {
    // API请求始终尝试从网络获取最新数据
    const networkResponse = await fetch(request);

    // 可选：缓存某些API响应（如网站配置等）
    if (networkResponse.ok && shouldCacheApiResponse(request)) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log('SW: API request failed:', request.url);

    // 对于某些API，可以返回缓存的版本
    if (shouldReturnCachedApiResponse(request)) {
      const cachedResponse = await caches.match(request);
      if (cachedResponse) {
        return cachedResponse;
      }
    }

    // 默认情况下让API请求失败，由应用处理
    throw error;
  }
}

// 判断是否应该缓存API响应
function shouldCacheApiResponse(request) {
  const url = new URL(request.url);
  // 缓存网站配置等相对稳定的API
  return url.pathname.includes('/webInfo/getWebInfo') ||
    url.pathname.includes('/seo/getSeoConfig');
}

// 判断是否应该返回缓存的API响应
function shouldReturnCachedApiResponse(request) {
  const url = new URL(request.url);
  // 离线时可以使用缓存的网站配置
  return url.pathname.includes('/webInfo/getWebInfo');
}

// 监听消息（用于与主线程通信）
self.addEventListener('message', event => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

console.log('SW: Service Worker script loaded');
