import grapesjs, { type Editor } from 'grapesjs';
import blocksBasic from 'grapesjs-blocks-basic';
import presetWebpage from 'grapesjs-preset-webpage';
import pluginForms from 'grapesjs-plugin-forms';
import pluginExport from 'grapesjs-plugin-export';
import pluginCountdown from 'grapesjs-component-countdown';
import pluginTabs from 'grapesjs-tabs';
import pluginCustomCode from 'grapesjs-custom-code';
import pluginTyped from 'grapesjs-typed';
import pluginTooltip from 'grapesjs-tooltip';
import pluginStyleBg from 'grapesjs-style-bg';
import parserPostCSS from 'grapesjs-parser-postcss';
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
const saveBtn = document.getElementById('save') as HTMLButtonElement;
const viewLink = document.getElementById('view-link') as HTMLAnchorElement;

function status(text: string): void {
  statusEl.textContent = text;
}

/** version hiện tại của page — dùng cho optimistic lock ở mỗi lần autosave. */
let version = 0;
let slug = '';

const editor: Editor = grapesjs.init({
  container: '#gjs',
  // GrapesJS ghi height thành inline style, đè mọi rule CSS cho #gjs — nên phải chốt
  // ở đây. '100%' sẽ ra 0 vì <body> không có chiều cao xác định.
  height: 'calc(100vh - 48px)',
  fromElement: false,
  selectorManager: { componentFirst: true },
  storageManager: {
    type: 'spring',
    autosave: true,
    autoload: true,
    stepsBeforeSave: 5, // gom thay đổi, tránh ghi liên tục
  },
  deviceManager: {
    devices: [
      { id: 'desktop', name: 'Desktop', width: '' },
      { id: 'tablet', name: 'Tablet', width: '768px', widthMedia: '992px' },
      // Tên phải đúng 'Mobile portrait': lệnh built-in `set-device-mobile` sau nút
      // Mobile gọi setDevice() với đúng chuỗi đó, đặt tên khác là nút mất tác dụng.
      { id: 'mobile', name: 'Mobile portrait', width: '320px', widthMedia: '480px' },
    ],
  },
  canvas: {
    // Trỏ tới CSS thật của Next.js để giảm lệch preview (demo dùng inline style nên để trống)
    styles: [],
  },
  // Bộ plugin của grapesjs.com/demo.html. Gọi dạng closure thay vì pluginsOpts để
  // truyền options mà vẫn giữ kiểu — pluginsOpts chỉ nhận key dạng chuỗi.
  plugins: [
    (e) => blocksBasic(e, { category: 'Cơ bản', flexGrid: true }),
    (e) =>
      presetWebpage(e, {
        modalImportTitle: 'Nhập HTML',
        modalImportButton: 'Nhập',
        modalImportLabel: 'Dán HTML/CSS vào đây rồi bấm Nhập',
        textCleanCanvas: 'Xoá sạch nội dung trang?',
      }),
    (e) => pluginForms(e, { category: 'Biểu mẫu' }),
    pluginExport,
    pluginCountdown,
    pluginCustomCode,
    pluginTooltip,
    // Tabs/Typed không tự gán category nên rơi ra ngoài mọi nhóm — xếp vào Extra cho khớp
    (e) => pluginTabs(e, { tabsBlock: { category: 'Extra' } }),
    (e) => pluginTyped(e, { block: { category: 'Extra' } }),
    pluginStyleBg,
    parserPostCSS,
  ],
});

registerBlocks(editor);

// tiện debug trong console
(window as unknown as { editor: Editor }).editor = editor;

interface ProjectData {
  pages?: Array<{ frames?: Array<{ component?: Record<string, unknown> }> }>;
}

/**
 * Bản cũ giới hạn wrapper chỉ nhận block riêng (`droppable: '[data-block]'`). Giới hạn
 * đó đã bỏ khỏi code nhưng nằm sẵn trong projectData đã lưu, nên autoload dựng lại mỗi
 * lần mở editor — hệ quả là mọi block mặc định của GrapesJS bị wrapper từ chối.
 *
 * Phải gỡ ngay trên dữ liệu: đặt lại wrapper sau khi load không ăn thua vì wrapper còn
 * được dựng lại lần nữa ở 'canvas:frame:load', lấy lại đúng giá trị cũ.
 */
function stripWrapperDroppable(data: Record<string, unknown>): Record<string, unknown> {
  for (const page of (data as ProjectData).pages ?? []) {
    for (const frame of page.frames ?? []) {
      if (frame.component) delete frame.component.droppable;
    }
  }
  return data;
}

editor.Storage.add('spring', {
  async load() {
    const page = await loadPage(pageId);
    version = page.version;
    slug = page.slug;
    nameEl.textContent = `${page.name} (/p/${page.slug})`;
    viewLink.href = `${WEB_URL}/p/${page.slug}`;
    status('Đã tải');
    return stripWrapperDroppable(page.projectData ?? {});
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

/**
 * Lưu ngay, không đợi autosave gom đủ stepsBeforeSave. Storage 'spring' đã tự báo
 * kết quả ra status bar nên ở đây chỉ cần chặn double-click và bắt lỗi mạng.
 */
async function save(): Promise<void> {
  if (saveBtn.disabled) return;
  saveBtn.disabled = true;
  try {
    await editor.store();
  } catch (e) {
    status(`Lưu lỗi: ${(e as Error).message}`);
  } finally {
    saveBtn.disabled = false;
  }
}

saveBtn.addEventListener('click', () => void save());

function onKeydown(e: KeyboardEvent): void {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault();
    void save();
  }
}

// Canvas là iframe riêng — phím bấm trong đó không nổi lên window cha, phải bind cả hai
window.addEventListener('keydown', onKeydown);

// Đóng tab lúc còn thay đổi chưa lưu thì mất trắng, vì autosave chỉ chạy mỗi 5 bước
window.addEventListener('beforeunload', (e) => {
  if (editor.getDirtyCount() > 0) e.preventDefault();
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

editor.on('load', () => {
  const doc = editor.Canvas.getDocument();
  doc.addEventListener('keydown', onKeydown);

  // Canvas trống thì nói cho admin biết phải làm gì, đừng để trắng trơn
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
