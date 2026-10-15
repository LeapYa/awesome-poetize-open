const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const { parseComponent, compile } = require('vue-template-compiler');

const source = readFileSync(path.join(__dirname, '../src/components/admin/postEdit.vue'), 'utf8');
const sfc = parseComponent(source);
const script = sfc.script.content
  .replace(/^\s*import\s+.+?;\s*$/gm, '')
  .replaceAll('import.meta.env.PROD', 'false')
  .replace('export default', 'return');
const component = new Function('localStorage', 'getAdminLanguageName', script)(
  { getItem: () => null }, name => name
);

function setup({ entry = 'draft', action = 'confirm', overrides = {}, admin = { id: 7 } } = {}) {
  const detail = {
    id: 'old-draft', articleId: 42, ownerUserId: 7, draftType: 'REVISION',
    articleUpdatedAfterDraft: true, crdtSnapshotBase64: 'saved-snapshot',
    sourceArticleTitle: '更新后的文章', lastEditorUsername: '作者',
    updateTime: '2026-10-15T10:00:00', ...overrides
  };
  const latest = { ...detail, id: 'new-draft', crdtSnapshotBase64: '', articleUpdatedAfterDraft: false };
  const calls = [];
  const prompts = [];
  const routes = [];
  const loaded = [];
  const messages = [];
  const vm = {
    id: entry === 'article' ? 42 : null,
    draftId: entry === 'draft' ? detail.id : null,
    pageDataLoadToken: 1,
    mainStore: { currentAdmin: admin },
    $constant: { baseURL: '/api' },
    $route: { query: {} },
    $router: { replace: route => routes.push(route) },
    $message: message => messages.push(message),
    $confirm: async (message, title, options) => {
      prompts.push({ message, title, options });
      if (action !== 'confirm') throw action;
    },
    $http: {
      get: async url => {
        calls.push(['GET', url]);
        return { code: 200, data: detail };
      },
      delete: async url => {
        calls.push(['DELETE', url]);
        return { code: 200, data: true };
      },
      post: async url => {
        calls.push(['POST', url]);
        return { code: 200, data: calls.some(call => call[0] === 'DELETE') ? latest : detail };
      }
    }
  };
  for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm);
  vm.initializeDraftSession = async (data, token) => loaded.push({ data, token });
  const run = () => entry === 'article'
    ? vm.ensureRevisionDraftSessionCreated(1)
    : vm.ensureDraftSessionCreated(1);
  return { vm, detail, latest, calls, prompts, routes, loaded, messages, run };
}

test('editor template compiles', () => {
  assert.deepEqual(compile(sfc.template.content).errors, []);
});

for (const entry of ['draft', 'article']) {
  test(`${entry}: keeping the stale draft does not delete or replace it`, async () => {
    const ctx = setup({ entry });
    await ctx.run();
    assert.equal(ctx.prompts.length, 1);
    assert.equal(ctx.prompts[0].options.type, 'warning');
    assert.equal(ctx.loaded[0].data, ctx.detail);
    assert.equal(ctx.calls.some(call => call[0] === 'DELETE'), false);
  });

  test(`${entry}: latest article replaces the exact draft returned by the API`, async () => {
    const ctx = setup({ entry, action: 'cancel' });
    await ctx.run();
    assert.deepEqual(ctx.calls.slice(-2), [
      ['DELETE', '/api/admin/articleDraft/old-draft'],
      ['POST', '/api/admin/articleDraft/revision/42']
    ]);
    assert.equal(ctx.loaded[0].data, ctx.latest);
    assert.equal(ctx.vm.draftId, 'new-draft');
    assert.deepEqual(ctx.routes.at(-1), { path: '/postEdit', query: { draftId: 'new-draft' } });
    assert.equal(ctx.vm.suppressNextDraftRouteReload, true);
  });

  test(`${entry}: closing the dialog preserves the draft`, async () => {
    const ctx = setup({ entry, action: 'close' });
    await ctx.run();
    assert.equal(ctx.loaded[0].data, ctx.detail);
    assert.equal(ctx.calls.some(call => call[0] === 'DELETE'), false);
    assert.equal(ctx.prompts[0].options.distinguishCancelAndClose, true);
    assert.equal(ctx.prompts[0].options.closeOnClickModal, false);
  });

  test(`${entry}: a route change while awaiting detail cannot open a stale dialog`, async () => {
    const ctx = setup({ entry });
    const method = entry === 'article' ? 'post' : 'get';
    const original = ctx.vm.$http[method];
    ctx.vm.$http[method] = async (...args) => {
      ctx.vm.pageDataLoadToken++;
      return original(...args);
    };
    await ctx.run();
    assert.equal(ctx.prompts.length, 0);
    assert.equal(ctx.loaded.length, 0);
  });

  test(`${entry}: a route change while deciding cannot delete another draft`, async () => {
    const ctx = setup({ entry });
    ctx.vm.$confirm = async () => {
      ctx.vm.pageDataLoadToken++;
      ctx.vm.id = 99;
      ctx.vm.draftId = 'another-draft';
      throw 'cancel';
    };
    await ctx.run();
    assert.equal(ctx.calls.some(call => call[0] === 'DELETE'), false);
    assert.equal(ctx.loaded.length, 0);
    assert.equal(ctx.routes.length, 0);
  });

  test(`${entry}: switching away during replacement does not navigate back`, async () => {
    const ctx = setup({ entry, action: 'cancel' });
    const original = ctx.vm.$http.delete;
    ctx.vm.$http.delete = async (...args) => {
      ctx.vm.pageDataLoadToken++;
      ctx.vm.id = 99;
      ctx.vm.draftId = 'another-draft';
      return original(...args);
    };
    await ctx.run();
    assert.equal(ctx.calls.at(-1)[1], '/api/admin/articleDraft/revision/42');
    assert.equal(ctx.loaded.length, 0);
    assert.equal(ctx.routes.length, 0);
    assert.equal(ctx.vm.draftId, 'another-draft');
  });
}

for (const overrides of [
  { articleUpdatedAfterDraft: false },
  { draftType: 'CREATE', articleId: null },
  { crdtSnapshotBase64: '' }
]) {
  test(`direct draft entry does not interrupt ordinary drafts: ${JSON.stringify(overrides)}`, async () => {
    const ctx = setup({ overrides });
    await ctx.run();
    assert.equal(ctx.prompts.length, 0);
    assert.equal(ctx.loaded[0].data, ctx.detail);
  });
}

test('article entry still offers an existing non-stale revision draft', async () => {
  const ctx = setup({ entry: 'article', overrides: { articleUpdatedAfterDraft: false } });
  await ctx.run();
  assert.equal(ctx.prompts.length, 1);
  assert.equal(ctx.prompts[0].options.type, 'info');
});

test('collaborators see the warning without the destructive option', async () => {
  const ctx = setup({ admin: { id: 8 }, action: 'cancel' });
  await ctx.run();
  assert.equal(ctx.prompts[0].options.showCancelButton, false);
  assert.match(ctx.prompts[0].message, /联系作者或站长/);
  assert.equal(ctx.calls.some(call => call[0] === 'DELETE'), false);
  assert.equal(ctx.loaded[0].data, ctx.detail);
});

test('site owner can replace another author\'s draft', async () => {
  const ctx = setup({ admin: { id: 8, isBoss: true }, action: 'cancel' });
  await ctx.run();
  assert.equal(ctx.prompts[0].options.showCancelButton, true);
  assert.equal(ctx.loaded[0].data, ctx.latest);
});

test('numeric and string user IDs both recognize the owner', async () => {
  const ctx = setup({ admin: { id: '7' }, action: 'cancel' });
  await ctx.run();
  assert.equal(ctx.loaded[0].data, ctx.latest);
});

test('dialog keeps only concise plain-text warnings and actions', async () => {
  const ctx = setup({ overrides: {
    sourceArticleTitle: '<img src=x onerror=alert(1)>',
    lastEditorUsername: '<svg onload=alert(2)>', updateTime: '<b>unsafe</b>'
  } });
  await ctx.run();
  const { message, title, options } = ctx.prompts[0];
  assert.equal(title, '文章已更新');
  assert.equal(message, '文章可能已由其他设备或自动化工具修改，发布此草稿可能覆盖这些更新。使用最新版将丢弃草稿及协作设置。');
  assert.equal(options.confirmButtonText, '继续编辑');
  assert.equal(options.cancelButtonText, '使用最新版');
  assert.notEqual(options.dangerouslyUseHTMLString, true);
  assert.doesNotMatch(message, /<|最后编辑/);
});

test('failed deletion never creates or loads a replacement', async () => {
  const ctx = setup({ action: 'cancel' });
  ctx.vm.$http.delete = async () => { throw new Error('无权限删除'); };
  await assert.rejects(ctx.run(), /重置修订草稿失败/);
  assert.equal(ctx.calls.some(call => call[0] === 'POST'), false);
  assert.equal(ctx.loaded.length, 0);
  assert.match(ctx.messages[0].message, /无权限删除/);
});

test('failed replacement is not treated as a successful reload', async () => {
  const ctx = setup({ action: 'cancel' });
  ctx.vm.$http.post = async () => ({ code: 400, message: '文章不存在' });
  await assert.rejects(ctx.run(), /重置修订草稿失败/);
  assert.equal(ctx.loaded.length, 0);
  assert.equal(ctx.routes.length, 0);
});
