/**
 * Screenshot script for Ciff product showcase.
 * Captures both page views and modal dialogs.
 *
 * Usage:
 *   1. Ensure backend & frontend are running on http://localhost:3000
 *   2. npx playwright install chromium   (one-time)
 *   3. node screenshot.js
 */
import { chromium } from 'playwright';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const IMAGE_DIR = join(__dirname, '..', 'image');
const BASE_URL = 'http://localhost:3000';

// Page definitions: path + create button selector + dialog file name
const pages = [
  {
    path: '/provider',
    file: 'provider.png',
    dialog: {
      triggerSelector: '.page-header .el-button--primary',
      file: 'provider-create.png',
      closeSelector: '.el-dialog__footer .el-button:first-child',
    },
  },
  {
    path: '/model',
    file: 'model.png',
  },
  {
    path: '/tool',
    file: 'tool.png',
    dialog: {
      triggerSelector: '.page-header .el-button--primary',
      file: 'tool-create.png',
      closeSelector: '.el-dialog__footer .el-button:first-child',
    },
  },
  {
    path: '/knowledge',
    file: 'knowledge.png',
    dialog: {
      triggerSelector: '.page-header .el-button--primary',
      file: 'knowledge-create.png',
      closeSelector: '.el-dialog__footer .el-button:first-child',
    },
  },
  {
    path: '/knowledge-documents',
    file: 'knowledge-documents.png',
  },
  {
    path: '/recall-test',
    file: 'recall-test.png',
  },
  {
    path: '/agent',
    file: 'agent.png',
    dialog: {
      triggerSelector: '.page-header .el-button--primary',
      file: 'agent-create.png',
      closeSelector: '.el-dialog__footer .el-button:first-child',
    },
  },
  {
    path: '/chat',
    file: 'chat.png',
    dialog: {
      triggerSelector: '.sidebar-header .el-button--primary',
      file: 'chat-agent-selector.png',
      closeSelector: '.el-dialog__footer .el-button:first-child',
    },
  },
];

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
  });

  for (const { path, file, dialog } of pages) {
    const page = await context.newPage();
    try {
      // 1. Screenshot page
      await page.goto(`${BASE_URL}${path}`, { waitUntil: 'networkidle', timeout: 15000 });
      await page.waitForTimeout(800);
      const output = join(IMAGE_DIR, file);
      await page.screenshot({ path: output, fullPage: false });
      console.log(`✅ ${file}`);

      // 2. Screenshot dialog if defined
      if (dialog) {
        await page.waitForSelector(dialog.triggerSelector, { timeout: 5000 });
        await page.click(dialog.triggerSelector);
        await page.waitForTimeout(600);
        // Wait for dialog to appear
        await page.waitForSelector('.el-dialog', { timeout: 5000 });
        await page.waitForTimeout(400);
        const dialogOutput = join(IMAGE_DIR, dialog.file);
        await page.screenshot({ path: dialogOutput, fullPage: false });
        console.log(`✅ ${dialog.file}`);

        // Close dialog
        if (dialog.closeSelector) {
          await page.click(dialog.closeSelector);
        } else {
          // Press Escape to close
          await page.keyboard.press('Escape');
        }
        await page.waitForTimeout(300);
      }
    } catch (err) {
      console.error(`❌ ${file}: ${err.message}`);
    } finally {
      await page.close();
    }
  }

  await browser.close();
  console.log('\nDone. Images saved to', IMAGE_DIR);
})();
