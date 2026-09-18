const { chromium } = require('playwright');
const path = require('path');

const ROOT = 'http://localhost:8080/after-sales';
const OUT = path.resolve(__dirname, '..', 'figure');
const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

async function settle(page) {
  await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.fonts && document.fonts.ready);
  await page.waitForTimeout(250);
}

async function login(page, edition, account) {
  await page.goto(`${ROOT}/logout`);
  await page.goto(`${ROOT}/${edition}/login`);
  await page.locator('input[name="account"]').fill(account);
  await page.locator('input[name="password"]').fill('123456');
  await Promise.all([
    page.waitForURL(/\/dashboard/),
    page.locator('button').filter({ hasText: '进入' }).click(),
  ]);
  await settle(page);
}

async function capture(page, name) {
  await settle(page);
  await page.screenshot({
    path: path.join(OUT, name),
    fullPage: false,
    animations: 'disabled',
  });
}

async function openOrder(page, orderNo) {
  await page.goto(`${ROOT}/orders`);
  const orderLink = page.locator('a[href*="/order/detail?id="]').filter({ hasText: orderNo });
  await Promise.all([
    page.waitForURL(/\/order\/detail\?id=\d+/),
    orderLink.click(),
  ]);
  await settle(page);
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: CHROME,
    args: ['--hide-scrollbars', '--force-device-scale-factor=1'],
  });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    screen: { width: 1920, height: 1080 },
    deviceScaleFactor: 1,
    locale: 'zh-CN',
    colorScheme: 'light',
  });
  const page = await context.newPage();

  await login(page, 'client', '13900004001');
  await page.goto(`${ROOT}/dashboard`);
  await capture(page, '图4-1 客户登录后的个人工作台与业务入口.png');

  await page.goto(`${ROOT}/customer/request`);
  await page.locator('select[name="deviceId"]').selectOption('1');
  await page.locator('select[name="faultId"]').selectOption('1');
  await page.locator('textarea[name="description"]').fill('台式电脑按下电源键无反应，指示灯不亮；已检查插座、电源线并尝试重新插拔，仍无法启动。');
  await page.locator('select[name="areaId"]').selectOption('2');
  await page.locator('input[name="address"]').fill('高新区创新大道88号 3栋1202室');
  await page.locator('input[name="expectedDate"]').fill('2026-09-15');
  await page.locator('select[name="slotId"]').selectOption('1');
  await page.locator('input[name="phone"]').fill('13900004001');
  await capture(page, '图4-2 客户维修需求填写页面.png');

  await page.goto(`${ROOT}/customer/candidates?requestId=3&sort=rating`);
  await capture(page, '图4-3 候选工程师筛选排序与自主预约.png');

  await page.goto(`${ROOT}/appointments`);
  await capture(page, '图4-4 客户预约管理取消与改约.png');

  await login(page, 'client', '13900004011');
  await page.goto(`${ROOT}/engineer/schedule`);
  await capture(page, '图4-5 工程师档案与固定标准时段排班.png');

  await openOrder(page, 'WO-CH4-REPAIR');
  await capture(page, '图4-6 工程师维修工单执行与过程记录.png');

  await page.locator('a[href*="/engineer/part/request?orderId="]').click();
  await settle(page);
  const parts = page.locator('select[name="partId"]');
  const quantities = page.locator('input[name="quantity"]');
  await parts.nth(0).selectOption('1');
  await quantities.nth(0).fill('1');
  await parts.nth(1).selectOption('2');
  await quantities.nth(1).fill('1');
  await page.locator('textarea[name="reason"]').fill('诊断确认 ATX 电源输出异常且 CPU 散热风扇轴承磨损，需更换电源模块和散热风扇后完成整机复测。');
  await capture(page, '图4-7 工程师从维修工单发起配件申请.png');

  await login(page, 'warehouse', 'warehouse');
  await page.goto(`${ROOT}/warehouse/requests`);
  await capture(page, '图4-8 仓库配件申请审核锁定与出库.png');

  await page.goto(`${ROOT}/warehouse/inventory`);
  await page.getByRole('heading', { name: '最近库存流水' }).evaluate((el) => el.scrollIntoView({ block: 'end' }));
  await capture(page, '图4-9 配件库存数量与库存流水.png');

  await login(page, 'admin', 'admin');
  await page.goto(`${ROOT}/admin`);
  await capture(page, '图4-10A 平台管理端认证审核与用户管理.png');
  await page.evaluate(() => window.scrollTo(0, 1550));
  await capture(page, '图4-10B 平台管理端SLA异常预约与操作日志.png');

  await login(page, 'client', '13900004001');
  await openOrder(page, 'WO-CH4-ACCEPT');
  await capture(page, '图4-11 客户验收与服务评价.png');

  await page.goto(`${ROOT}/notifications`);
  await capture(page, '图4-12 站内通知与跨角色业务协同.png');

  await browser.close();
  console.log('CHAPTER4_CAPTURE_OK');
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
