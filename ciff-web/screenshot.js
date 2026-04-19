/**
 * Screenshot script for Ciff product showcase.
 * Must run inside ciff-web directory (so it can resolve playwright).
 *
 * Usage:
 *   1. Ensure backend & frontend are running on http://localhost:3003
 *   2. npx playwright install chromium   (one-time)
 *   3. node screenshot.js
 */
import { chromium } from 'playwright';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const IMAGE_DIR = join(__dirname, '..', 'image');
const BASE_URL = 'http://localhost:3003';

const pages = [
  { path: '/provider',            file: 'provider.png' },
  { path: '/model',               file: 'model.png' },
  { path: '/tool',                file: 'tool.png' },
  { path: '/knowledge',           file: 'knowledge.png' },
  { path: '/knowledge-documents', file: 'knowledge-documents.png' },
  { path: '/recall-test',         file: 'recall-test.png' },
  { path: '/agent',               file: 'agent.png' },
  { path: '/chat',                file: 'chat.png' },
];

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
  });

  for (const { path, file } of pages) {
    const page = await context.newPage();
    try {
      await page.goto(`${BASE_URL}${path}`, { waitUntil: 'networkidle', timeout: 15000 });
      await page.waitForTimeout(800);
      const output = join(IMAGE_DIR, file);
      await page.screenshot({ path: output, fullPage: false });
      console.log(`✅ ${file}`);
    } catch (err) {
      console.error(`❌ ${file}: ${err.message}`);
    } finally {
      await page.close();
    }
  }

  await browser.close();
  console.log('\nDone. Images saved to', IMAGE_DIR);
})();
