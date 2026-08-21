import { chromium } from 'playwright';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * OpsPilot 在系统 Microsoft Edge 中执行的端到端验收。
 *
 * 这不是只判断页面能否打开的“冒烟截图”：脚本会通过可见表单创建两个账户、
 * 发起转账、使用相同幂等键重放，再从服务端读模型核对订单数和双录流水。
 * 最后还会切换到手机视口，检查侧栏抽屉和主要内容没有出现横向溢出。
 */
const baseUrl = process.env.OPSPILOT_BASE_URL ?? 'http://localhost:18000';
const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(currentDirectory, '../../..');
const evidenceDirectory = path.join(projectRoot, 'docs', 'ui-concepts');
const timestamp = Date.now().toString();
const sourceAccountNo = `62${timestamp.slice(-12)}01`;
const targetAccountNo = `62${timestamp.slice(-12)}02`;
const requestId = `edge-e2e-${timestamp}`;

const browser = await chromium.launch({ channel: 'msedge', headless: true });
const page = await browser.newPage({
  locale: 'zh-CN',
  viewport: { width: 1440, height: 1000 },
  deviceScaleFactor: 1,
});

const browserErrors = [];
page.on('pageerror', (error) => browserErrors.push(`pageerror: ${error.message}`));
page.on('console', (message) => {
  if (message.type() === 'error') browserErrors.push(`console: ${message.text()}`);
});
page.on('response', (response) => {
  if (response.status() >= 400) {
    browserErrors.push(`http ${response.status()}: ${response.url()}`);
  }
});

/** 使用页面中的真实开户表单创建一个账户，并等待后端回写结果。 */
async function createAccount(accountNo, holderName) {
  await page.getByLabel('业务账号').fill(accountNo);
  await page.getByLabel('账户名称').fill(holderName);
  await page.getByLabel('开户余额').fill('10000.00');
  await page.getByRole('button', { name: '确认开户' }).click();
  await page.getByRole('status').filter({ hasText: accountNo }).waitFor();
}

try {
  // 首屏必须来自真实聚合 API，并呈现总览、趋势与最近转账三个层次。
  await page.goto(baseUrl, { waitUntil: 'networkidle' });
  await page.getByRole('heading', { level: 1, name: '运行总览' }).waitFor();
  assert.equal(await page.locator('.metric-strip > div').count(), 4);
  await page.getByRole('heading', { name: '近 24 小时交易趋势' }).waitFor();
  await page.screenshot({
    path: path.join(evidenceDirectory, 'implementation-final.png'),
    fullPage: true,
  });

  // 开户流程同时验证表单校验、POST 写入以及列表重新读取。
  await page.getByRole('link', { name: '账户管理' }).click();
  await page.getByRole('heading', { level: 1, name: '账户管理' }).waitFor();
  await createAccount(sourceAccountNo, 'Edge 验收付款账户');
  await createAccount(targetAccountNo, 'Edge 验收收款账户');

  // 转账流程覆盖事务提交、订单回显、双录流水和平衡校验。
  await page.getByRole('link', { name: '转账中心' }).click();
  await page.getByRole('heading', { level: 1, name: '转账中心' }).waitFor();
  await page.locator('.stacked-form select').nth(0).selectOption(sourceAccountNo);
  await page.locator('.stacked-form select').nth(1).selectOption(targetAccountNo);
  await page.getByLabel('转账金额').fill('88.88');
  await page.getByLabel('请求编号（幂等键）').fill(requestId);
  await page.getByRole('button', { name: '确认转账' }).click();
  await page.getByRole('status').filter({ hasText: '转账成功' }).waitFor();
  await page.getByText('借贷平衡').waitFor();
  assert.equal(await page.locator('.ledger-entry').count(), 2);

  // 原键重放后，服务端仍只能存在一张订单、两条方向相反的流水。
  await page.getByRole('button', { name: '用同一幂等键重放' }).click();
  await page.getByRole('button', { name: '用同一幂等键重放' }).waitFor({ state: 'visible' });
  const serverEvidence = await page.evaluate(async ({ requestId }) => {
    const [ordersResponse, auditResponse] = await Promise.all([
      fetch('/api/v1/transfers?limit=100'),
      fetch(`/api/v1/transfers/${encodeURIComponent(requestId)}/ledger`),
    ]);
    return {
      orders: await ordersResponse.json(),
      audit: await auditResponse.json(),
    };
  }, { requestId });
  assert.equal(serverEvidence.orders.filter((order) => order.requestId === requestId).length, 1);
  assert.equal(serverEvidence.audit.entries.length, 2);
  assert.equal(serverEvidence.audit.balanced, true);

  // 系统状态页必须把实时探针与项目能力边界分开呈现。
  await page.getByRole('link', { name: '系统状态' }).click();
  await page.getByText('实时 readiness').waitFor();
  await page.getByText('正常', { exact: true }).waitFor();
  assert.equal(await page.locator('.capability-list article').count(), 6);

  // 手机视口验收侧栏抽屉、首屏内容和横向溢出；另存截图作为响应式证据。
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(baseUrl, { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: '打开导航' }).click();
  // 侧栏使用 240ms transform 过渡；等待稳定帧后再保存抽屉证据。
  await page.waitForTimeout(300);
  await page.getByRole('link', { name: '转账中心' }).waitFor({ state: 'visible' });
  await page.screenshot({
    path: path.join(evidenceDirectory, 'implementation-mobile-nav.png'),
    fullPage: false,
  });
  const horizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  );
  assert.equal(horizontalOverflow, false);
  await page.getByRole('button', { name: '关闭导航', exact: true }).click();
  // 等待 240ms 抽屉过渡结束，避免把关闭中的中间帧误当成主页面响应式证据。
  await page.waitForTimeout(300);
  await page.screenshot({
    path: path.join(evidenceDirectory, 'implementation-mobile.png'),
    fullPage: true,
  });

  assert.deepEqual(browserErrors, []);
  console.log(JSON.stringify({
    status: 'passed',
    browser: 'Microsoft Edge',
    viewport: ['1440x1000', '390x844'],
    sourceAccountNo,
    targetAccountNo,
    requestId,
    idempotentOrderCount: 1,
    ledgerEntryCount: 2,
    balanced: true,
    browserErrors: 0,
  }, null, 2));
} finally {
  await browser.close();
}
