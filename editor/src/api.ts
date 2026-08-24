import type { PageConfig } from '@shared/blocks';

/** Backend Spring Boot (auto-ui-api) — cổng 1000 theo application-local.properties. */
const API: string = import.meta.env.VITE_API_URL ?? 'http://localhost:1000';

/** Next.js — dùng để mở trang công khai và ping revalidate sau khi publish. */
export const WEB_URL: string = import.meta.env.VITE_WEB_URL ?? 'http://localhost:3000';

/** Optimistic lock: version editor gửi lệch với DB thì backend trả code này. */
const CODE_VERSION_CONFLICT = 'ERROR-PAGE-001';

/**
 * Envelope chung của auto-ui-api. GlobalExceptionHandler trả HTTP 200 kèm
 * result=false cho NotFoundException, nên luôn đọc `result` chứ đừng tin `res.ok`.
 */
interface ApiMessageDto<T> {
  result: boolean;
  code: string | null;
  message: string | null;
  data: T | null;
}

interface PageDto {
  id: number;
  name: string;
  slug: string;
  /** Hai cột JSON lưu dạng chuỗi opaque — backend trả nguyên văn, FE tự parse. */
  projectData: string | null;
  pageConfig: string | null;
  version: number;
}

async function call<T>(path: string, init?: RequestInit): Promise<ApiMessageDto<T>> {
  let res: Response;
  try {
    res = await fetch(`${API}${path}`, init);
  } catch {
    // "Failed to fetch" của trình duyệt không nói được gì — nêu thẳng địa chỉ backend
    throw new Error(`không gọi được backend ${API}`);
  }
  const body = (await res.json().catch(() => null)) as ApiMessageDto<T> | null;
  if (!body) throw new Error(`Backend trả về dữ liệu không đọc được (HTTP ${res.status})`);
  return body;
}

export interface PageState {
  id: string;
  name: string;
  slug: string;
  projectData: Record<string, unknown> | null;
  version: number;
}

export async function loadPage(id: string): Promise<PageState> {
  const body = await call<PageDto>(`/v1/page/get/${id}`);
  if (!body.result || !body.data) throw new Error(body.message ?? 'Không tải được page');

  const dto = body.data;
  return {
    id: String(dto.id),
    name: dto.name,
    slug: dto.slug,
    projectData: dto.projectData ? (JSON.parse(dto.projectData) as Record<string, unknown>) : null,
    version: dto.version,
  };
}

export type StoreResult =
  | { ok: true; version: number }
  | { ok: false; conflict: true; message: string }
  | { ok: false; conflict: false; message: string };

export async function storeProjectData(
  id: string,
  projectData: unknown,
  version: number,
): Promise<StoreResult> {
  const body = await call<PageDto>('/v1/page/autosave', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id, projectData, version }),
  });

  if (body.result && body.data) return { ok: true, version: body.data.version };
  return {
    ok: false,
    conflict: body.code === CODE_VERSION_CONFLICT,
    message: body.message ?? 'Lưu thất bại',
  };
}

export async function publish(id: string, config: PageConfig): Promise<void> {
  const body = await call<PageDto>('/v1/page/publish', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id, config }),
  });
  if (!body.result) throw new Error(body.message ?? 'Publish thất bại');
}

/**
 * Xoá cache trang công khai bên Next.js. Best-effort — publish đã thành công rồi,
 * hỏng bước này thì trang chỉ chậm cập nhật tối đa 60s chứ không sai dữ liệu.
 */
export async function revalidateWeb(slug: string): Promise<void> {
  try {
    await fetch(`${WEB_URL}/api/revalidate?slug=${encodeURIComponent(slug)}`, { method: 'POST' });
  } catch {
    // bỏ qua: web có thể chưa chạy
  }
}
