const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const { parseComponent, compile } = require('vue-template-compiler');

const source = readFileSync(path.join(__dirname, '../src/components/admin/systemLog.vue'), 'utf8');
const sfc = parseComponent(source);
const component = new Function(sfc.script.content.replace('export default', 'return'))();

function setup() {
  const vm = component.data();
  for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm);
  return vm;
}

test('system log template compiles', () => {
  assert.deepEqual(compile(sfc.template.content).errors, []);
});

test('new API actions have readable labels', () => {
  const vm = setup();
  for (const action of ['API_AUTH_REJECTED', 'ARTICLE_SECTION_UPDATE', 'COMMENT_SAVE',
    'PAYMENT_CONFIG_UPDATE', 'PAYMENT_CONNECTION_TEST', 'SEO_SITEMAP_UPDATE']) {
    assert.notEqual(vm.getActionLabel(action), action);
    assert.match(vm.getActionLabel(action), /[\u4e00-\u9fff]/);
  }
});

test('API and admin actors remain distinguishable in list and detail', () => {
  const vm = setup();
  for (const username of ['开放 API', '站长']) {
    const row = { logType: 'OPERATION', username, maskedAccount: null };
    assert.equal(vm.formatPrincipal(row), username);
    assert.equal(vm.formatActor(row), username);
  }
  assert.equal(vm.formatPrincipal({ logType: 'SECURITY', username: null }), '-');
});

test('unknown actions retain their fallback labels', () => {
  assert.equal(setup().getActionLabel('FUTURE_ACTION'), 'FUTURE_ACTION');
});
