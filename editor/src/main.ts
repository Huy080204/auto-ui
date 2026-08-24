import grapesjs, { type Editor } from 'grapesjs';
import { loadPage, publish, revalidateWeb, storeProjectData, WEB_URL } from './api';
import { registerBlocks } from './gjs-blocks';
import { extractConfig } from './extract-config';
import './style.css';

// id do Snowflake sinh; bản seed trong Liquibase dùng id = 1
const DEFAULT_PAGE_ID = '1';
const pageId =
  new URLSearchParams(location.search).get('pageId') ??
  import.meta.env.VITE_PAGE_ID ??
  DEFAULT_PAGE_ID;

const statusEl = document.getElementById('status')!;
const nameEl = document.getElementById('page-name')!;
const publishBtn = document.getElementById('publish') as HTMLButtonElement;
const viewLink = document.getElementById('view-link') as HTMLAnchorElement;

function status(text: string): void {
  statusEl.textContent = text;
}

/** version hiện tại của page — dùng cho optimistic lock ở mỗi lần autosave. */
let version = 0;
let slug = '';

const editor: Editor = grapesjs.init({
  container: '#gjs',
  height: '100%',
  fromElement: false,
  blockManager: { blocks: [] }, // xoá block mặc định
  styleManager: { sectors: [] }, // admin không sửa CSS
  selectorManager: { componentFirst: true },
  panels: {},
  storageManager: {
    type: 'spring',
    autosave: true,
    autoload: true,
    stepsBeforeSave: 5, // gom thay đổi, tránh ghi liên tục
  },
  canvas: {
    // Trỏ tới CSS thật của Next.js để giảm lệch preview (demo dùng inline style nên để trống)
    styles: [],
  },
});

registerBlocks(editor);

// tiện debug trong console
(window as unknown as { editor: Editor }).editor = editor;

// Wrapper chỉ nhận block hợp lệ
editor.getWrapper()!.set({ droppable: '[data-block]' });

editor.Storage.add('spring', {
  async load() {
    const page = await loadPage(pageId);
    version = page.version;
    slug = page.slug;
    nameEl.textContent = `${page.name} (/p/${page.slug})`;
    viewLink.href = `${WEB_URL}/p/${page.slug}`;
    status('Đã tải');
    return page.projectData ?? {};
  },

  async store(data: unknown) {
    status('Đang lưu…');
    const res = await storeProjectData(pageId, data, version);

    if (res.ok) {
      version = res.version;
      status(`Đã lưu • v${version}`);
      return;
    }
    if (res.conflict) {
      status(`Xung đột version — ${res.message}. Tải lại trang trước khi sửa tiếp.`);
      return;
    }
    status(`Lưu lỗi: ${res.message}`);
  },
});

publishBtn.addEventListener('click', async () => {
  publishBtn.disabled = true;
  status('Đang publish…');
  try {
    await editor.store();
    const config = extractConfig(editor);
    if (config.blocks.length === 0) {
      status('Chưa có block nào để publish');
      return;
    }
    await publish(pageId, config);
    await revalidateWeb(slug);
    status(`Đã publish ${config.blocks.length} block • ${new Date().toLocaleTimeString('vi-VN')}`);
    window.open(`${WEB_URL}/p/${slug}`, '_blank');
  } catch (e) {
    status(`Publish lỗi: ${(e as Error).message}`);
  } finally {
    publishBtn.disabled = false;
  }
});

/**
 * Mở sẵn panel Blocks để admin kéo thả ngay, thay vì Style Manager (đang trống).
 * Phải đặt trong 'load' — gọi ngay sau init thì GrapesJS render panel xong mới set
 * button mặc định của nó, ghi đè lại lựa chọn này.
 */
editor.on('load', () => {
  editor.Panels.getButton('views', 'open-blocks')?.set('active', true);

  // Canvas trống thì nói cho admin biết phải làm gì, đừng để trắng trơn
  const doc = editor.Canvas.getDocument();
  const hint = doc.createElement('style');
  hint.textContent = `
    body:empty::before {
      content: 'Kéo block từ panel bên phải thả vào đây';
      display: block;
      padding: 80px 24px;
      text-align: center;
      color: #94a3b8;
      font: 16px system-ui, sans-serif;
    }`;
  doc.head.appendChild(hint);
});

// Load lần đầu (autoload chạy trước khi Storage 'spring' được add ở một số version).
// Backend chết là load reject — phải báo ra status bar, không thì editor trắng trơn
// mà không ai biết vì sao.
editor.load().catch((e: Error) => {
  // api.ts đã đổi lỗi mạng thành "không gọi được backend <url>", nên message tự đủ nghĩa
  status(`Không tải được page #${pageId}: ${e.message}`);
  nameEl.textContent = '(chưa tải được page)';
});
