const { chromium } = require('playwright');
const path = require('path');

const ROOT = 'http://localhost:8080/after-sales';
const OUT = path.resolve(__dirname, '..', 'figure');
const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

async function settle(page) {
  await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.fonts && document.fonts.ready);
  await page.waitForTimeout(350);
}

async function loginCustomer(page) {
  await page.goto(`${ROOT}/client/login`);
  await page.locator('input[name="account"]').fill('13900004001');
  await page.locator('input[name="password"]').fill('123456');
  await Promise.all([
    page.waitForURL(/\/dashboard/),
    page.locator('button').filter({ hasText: '进入' }).click(),
  ]);
  await settle(page);
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: CHROME,
    args: ['--hide-scrollbars'],
  });
  const context = await browser.newContext({
    viewport: { width: 400, height: 890 },
    screen: { width: 400, height: 890 },
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    locale: 'zh-CN',
    colorScheme: 'light',
  });
  const page = await context.newPage();

  await loginCustomer(page);
  await page.goto(`${ROOT}/dashboard`);
  await settle(page);
  await page.locator('[data-nav-toggle]').click();
  await page.waitForTimeout(450);
  await page.screenshot({
    path: path.join(OUT, '图4-13 小米14客户工作台与移动端抽屉导航.png'),
    fullPage: false,
    animations: 'disabled',
    scale: 'device',
  });

  await page.goto(`${ROOT}/customer/request`);
  await settle(page);
  await page.locator('select[name="deviceId"]').selectOption('1');
  await page.locator('select[name="faultId"]').selectOption('1');
  await page.locator('textarea[name="description"]').fill('台式电脑按下电源键无反应，指示灯不亮；已检查插座、电源线并重新插拔，仍无法启动。');
  await page.locator('select[name="areaId"]').selectOption('2');
  await page.locator('input[name="address"]').fill('高新区创新大道88号 3栋1202室');
  await page.locator('input[name="expectedDate"]').fill('2026-09-16');
  await page.locator('select[name="slotId"]').selectOption('1');
  await page.locator('input[name="phone"]').fill('13900004001');
  await page.evaluate(() => {
    if (document.activeElement) document.activeElement.blur();
    window.scrollTo(0, 0);
  });
  await page.waitForTimeout(300);
  await page.screenshot({
    path: path.join(OUT, '图4-14 小米14客户报修纵向表单.png'),
    fullPage: false,
    animations: 'disabled',
    scale: 'device',
  });

  await browser.close();
  console.log('MOBILE_CHAPTER4_CAPTURE_OK');
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
