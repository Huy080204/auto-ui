# PLAN.md — Demo GrapesJS + Spring Boot + Next.js

> Tài liệu này là nguồn sự thật cho phiên làm việc với Claude Code.
> Mở dự án tại `D:\WORK_SPACE\ITZ\auto-ui` và làm theo phần **Thứ tự làm**.

---

## 1. Kiến trúc

Ba source độc lập, trao đổi qua REST:

```
auto-ui/
├── editor/     GrapesJS (Vite + React, :5173)   → admin dựng trang
├── backend/    Spring Boot + MySQL 8 (:8080)    → lưu trữ + API
├── web/        Next.js App Router (:3000)       → render trang công khai
└── shared/     blocks.ts (định nghĩa dùng chung)
```

**Nguyên tắc cốt lõi:** GrapesJS chỉ là công cụ nhập liệu. Nguồn sự thật về
giao diện là các component `.tsx` do developer viết. Admin chọn block và điền
props, **không sửa CSS**.

---

## 2. Hai loại dữ liệu — đừng nhầm

| Cột | Sinh khi | Ai đọc | Ghi chú |
|---|---|---|---|
| `project_data` | autosave | editor | ProjectData của GrapesJS. Backend coi là chuỗi opaque, **không parse** |
| `page_config` | publish | Next.js | Schema tự định nghĩa: `{ blocks: [{ type, props }] }` |

**Không lưu `published_html`.** Next.js render bằng React component, không dùng
`dangerouslySetInnerHTML`.

Lý do tách hai cột: `project_data` chứa cây DOM + style rule + trait metadata,
phụ thuộc schema nội bộ của GrapesJS (đổi khi nâng version). Next.js chỉ cần
biết *block gì, props gì*.

---

## 3. Nguồn sự thật: `shared/blocks.ts`

Một file khai báo, ba nơi dùng.

```ts
export const BLOCKS = {
  hero: {
    label: 'Hero',
    props: {
      title:    { type: 'text',   label: 'Tiêu đề', default: 'ITZ Solution' },
      subtitle: { type: 'text',   label: 'Mô tả' },
      variant:  { type: 'select', label: 'Kiểu', options: ['light', 'dark'] },
    },
  },
  cta: {
    label: 'CTA',
    props: {
      text: { type: 'text', label: 'Nội dung nút', default: 'Liên hệ' },
      href: { type: 'text', label: 'Liên kết',     default: '/contact' },
    },
  },
} as const;
```

- **editor** — sinh `DomComponents.addType` + `BlockManager.add` + traits tự động
- **web** — registry map `type → Component`
- **backend** — whitelist `type` để validate lúc publish

Copy file này sang cả `editor/` và `web/` (hoặc để ở `shared/` rồi import qua
alias). **Đừng viết tay hai lần.**

---

## 4. API

```
GET   /api/pages/{id}            → { projectData, version }              (editor load)
PUT   /api/pages/{id}            → { projectData, version } → 200 / 409  (autosave)
POST  /api/pages/{id}/publish    → { config } → cập nhật page_config + published_at
GET   /api/public/pages/{slug}   → { title, config, publishedAt }        (public, Next.js)
```

Demo **bỏ auth và asset upload**.

Optimistic lock bằng `@Version`; version lệch → trả `409` kèm version hiện tại.

---

## 5. Schema

```sql
CREATE TABLE page (
  id           BINARY(16)   NOT NULL PRIMARY KEY,
  name         VARCHAR(255) NOT NULL,
  slug         VARCHAR(190) NOT NULL,
  project_data LONGTEXT     NULL,
  page_config  LONGTEXT     NULL,
  version      BIGINT       NOT NULL DEFAULT 0,
  published_at DATETIME(6)  NULL,
  created_at   DATETIME(6)  NOT NULL,
  updated_at   DATETIME(6)  NOT NULL,
  UNIQUE KEY uk_page_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

`VARCHAR(190)` cho slug vì giới hạn index utf8mb4 (190 × 4 = 760 bytes).

**Entity:**

- Dùng `String` cho hai cột JSON, `columnDefinition = "longtext"`
- **Không** dùng `hibernate-types`, **không** dùng `@Lob`
- Màn hình danh sách phải dùng projection để không load blob

```java
@Column(name = "project_data", columnDefinition = "longtext")
private String projectData;

@Column(name = "page_config", columnDefinition = "longtext")
private String pageConfig;

@Version
private Long version;
```

Backend **không parse** hai chuỗi này — nhận sao lưu vậy, trả về nguyên văn.
Ngoại lệ duy nhất: validate `type` lúc publish (mục 8).

---

## 6. Ràng buộc phía editor

Đây là phần quyết định demo có chạy đúng không.

- `blockManager: { blocks: [] }` — xóa block mặc định
- `styleManager: { sectors: [] }` — admin không sửa CSS
- Mọi block `droppable: false` — không lồng nhau
- Wrapper chỉ nhận block hợp lệ:
  ```js
  editor.getWrapper().set({ droppable: '[data-block]' });
  ```
- Custom storage `editor.Storage.add('spring', {...})` — **không** dùng
  `type: 'remote'`, vì cần gửi kèm `version` cho optimistic lock
- Traits ghi vào `attributes`; `extractConfig` chỉ lấy đúng key có trong
  `BLOCKS`, loại bỏ `id` / `class` / `data-block`

---

## 7. Trích config lúc publish

```js
function extractConfig(editor) {
  return {
    blocks: editor.getWrapper().components()
      .map(c => {
        const type = c.get('type');
        const def = BLOCKS[type];
        if (!def) return null;                 // block lạ → loại

        const attrs = c.getAttributes();
        const props = Object.fromEntries(
          Object.keys(def.props)
            .filter(k => attrs[k] !== undefined)
            .map(k => [k, attrs[k]])
        );
        return { type, props };
      })
      .filter(Boolean),
  };
}
```

Chỉ duyệt **một tầng gốc** — nên mọi block phải phẳng. Nếu sau này cần lồng
nhau, viết đệ quy và thêm `children` vào schema; nhưng demo thì giữ phẳng.

---

## 8. Backend validate — đừng tin client

```java
private static final Set<String> ALLOWED = Set.of("hero", "cta");

public void publish(UUID id, JsonNode config) {
    for (JsonNode b : config.path("blocks")) {
        String type = b.path("type").asText();
        if (!ALLOWED.contains(type)) {
            throw new BadRequestException("Block không hợp lệ: " + type);
        }
    }
    // lưu config dưới dạng chuỗi
}
```

Whitelist này phải khớp với `BLOCKS`.

---

## 9. Next.js

```tsx
// app/p/[slug]/page.tsx
export const revalidate = 60;

export default async function Page({ params }) {
  const res = await fetch(`${API}/api/public/pages/${params.slug}`, {
    next: { revalidate: 60, tags: [`page-${params.slug}`] },
  });
  if (!res.ok) notFound();
  const { config } = await res.json();

  return (
    <>
      {config.blocks.map((b, i) => {
        const C = registry[b.type];
        if (!C) return null;          // block lạ → bỏ qua, không crash
        return <C key={i} {...b.props} />;
      })}
    </>
  );
}
```

Không `dangerouslySetInnerHTML` → không lo XSS từ nội dung admin, hydration đầy
đủ, animation chạy bình thường.

---

## 10. Thứ tự làm

1. **backend** — entity + repo + 4 endpoint + migration + seed sẵn một page
   `slug = "demo"` có `page_config` mẫu
2. **shared/blocks.ts** — 2 block: `hero`, `cta`
3. **web** — `Hero.tsx`, `CTA.tsx`, registry, route `/p/[slug]`
4. **editor** — init GrapesJS, sinh block từ `BLOCKS`, custom storage, nút Publish
5. **Chạy end-to-end** — dựng trang trong editor → publish → mở `/p/demo`

> Làm backend + web trước, editor sau cùng. Như vậy có thể test `/p/demo` bằng
> cách insert `page_config` thủ công vào DB, tách được lỗi editor ra khỏi lỗi render.

---

## 11. Điểm dễ sai

- **CORS** — 3 origin khác nhau (`:5173`, `:3000`, `:8080`). Cấu hình từ đầu,
  đừng để tới lúc gọi API mới phát hiện
- **Preview lệch** — trang trong editor trông khác trang thật. Cho
  `canvas.styles` trỏ tới CSS build của Next.js để giảm lệch. Không bao giờ
  khớp 100%
- **Thêm block mới = sửa 3 nơi** — `BLOCKS`, file `.tsx`, whitelist backend.
  Đây là chi phí cố định của mô hình; ghi vào README để người sau biết
- **Đừng load blob khi list** — projection hoặc DTO, không `findAll()` trả entity
- **Autosave ghi rất thường xuyên** — dùng `stepsBeforeSave` để gom thay đổi

---

## 12. Ghi chú cho tương lai (ngoài phạm vi demo)

- Auth + multi-tenant: phân quyền theo từng page, không chỉ theo endpoint
- Asset upload: `POST /api/assets` trả `{ "data": [{ src, name, type }] }`
  (format bắt buộc của GrapesJS AssetManager)
- Webhook `revalidateTag` từ Spring → Next.js sau khi publish, để cập nhật tức thì
- Bảng `page_revision` để rollback — chỉ ghi khi publish, không ghi mỗi autosave
- Nếu nâng Spring Boot lên 3.x: có `@JdbcTypeCode(SqlTypes.JSON)` sẵn, nhưng với
  cách lưu `String` opaque thì vẫn không cần dùng
