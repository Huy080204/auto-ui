# auto-ui — demo GrapesJS → page_config → Next.js

Chi tiết thiết kế xem [PLAN.md](PLAN.md). File này chỉ nói cách chạy.

```
auto-ui/
├── shared/blocks.ts   nguồn sự thật về block (editor + web + whitelist backend cùng đọc)
├── editor/            GrapesJS (Vite + TS, :5173) — admin kéo thả
├── web/               Next.js App Router (:3000) — render trang công khai
└── auto-ui-api/       Spring Boot + MySQL (:1000) — lưu trữ + API
```

## Chạy demo

Cần MySQL đang chạy ở `localhost:3306` với user `root/root` (xem
`auto-ui-api/source/auto-ui-api/src/main/resources/application-local.properties`).
Liquibase tự tạo bảng `db_page` và seed sẵn một trang `slug = "demo"` lúc khởi động.

**1. Backend** — cổng 1000:

```bash
cd auto-ui-api/source/auto-ui-api && mvn spring-boot:run
```

> `spring-boot-maven-plugin` trong `pom.xml` đang để `<skip>true</skip>` nên goal `run` không
> khởi động app. Chạy từ IDE (run class `auto.ui.api.Application`), hoặc bỏ dòng `<skip>` đó,
> hoặc dùng classpath thủ công:
> `mvn dependency:build-classpath -Dmdep.outputFile=target/cp.txt` rồi
> `java -cp "target/classes;$(cat target/cp.txt)" auto.ui.api.Application`.

**2. Web** — cổng 3000:

```bash
cd web && npm install && npm run dev
```

**3. Editor** — cổng 5173:

```bash
cd editor && npm install && npm run dev
```

Rồi mở:

- <http://localhost:5173> — editor: kéo Hero/CTA từ sidebar phải vào canvas, chọn block rồi sửa
  nội dung ở ô **Thuộc tính** ngay dưới, bấm **Publish**
- <http://localhost:3000/p/demo> — trang công khai, render bằng `Hero.tsx` / `CTA.tsx`

Trang seed có `slug = "demo"`, `id = 1`. Đổi page bằng query: `http://localhost:5173/?pageId=<id>`.

## API

Base path `/v1/page`, tất cả trả về envelope `ApiMessageDto` (`{ result, code, message, data }`).
Lưu ý `GlobalExceptionHandler` trả **HTTP 200 kèm `result: false`** cho `NotFoundException` —
FE phải đọc `result`/`code`, đừng tin `res.ok`.

| Endpoint | Dùng để | Ghi chú |
|---|---|---|
| `GET /v1/page/get/{id}` | editor load | trả `projectData` + `version` |
| `PUT /v1/page/autosave` | editor autosave | body `{ id, projectData, version }`; version lệch → `code = ERROR-PAGE-001` |
| `POST /v1/page/publish` | editor publish | body `{ id, config }`; block lạ → `code = ERROR-PAGE-002` |
| `GET /v1/page/public/get/{slug}` | Next.js render | chỉ trả trang đã publish |

Cả 4 endpoint đều **không có `@PreAuthorize`** — bản demo bỏ auth theo PLAN.md mục 4, và
`/v1/page/**` được mở trong `ResourceServerConfig`. Muốn siết lại thì thêm `@PreAuthorize` với
`{PREFIX} = PAGE` rồi gỡ dòng permitAll đó, và editor phải làm thêm màn login.

`project_data` và `page_config` được lưu dạng chuỗi opaque — backend không parse. Ngoại lệ duy
nhất là validate `type` lúc publish (`PageConstant.ALLOWED_BLOCK_TYPES`).

## Thêm block mới = sửa 3 nơi

1. `shared/blocks.ts` — khai báo `label` + `props`
2. `web/src/components/<Ten>.tsx` + đăng ký trong `web/src/components/registry.ts`
   (thiếu → TypeScript báo lỗi ngay)
3. `PageConstant.ALLOWED_BLOCK_TYPES` phía backend

Editor tự sinh block, trait và preview từ `BLOCKS`, không phải sửa gì thêm.
Preview trong canvas là bản dựng lại bằng inline style ở
[gjs-blocks.ts](editor/src/gjs-blocks.ts) — cố tình gần giống, không bao giờ khớp 100% với
component thật.

## Cache trang công khai

`/p/[slug]` dùng `revalidate: 60` + tag `page-{slug}`. Editor gọi `POST /api/revalidate?slug=`
ngay sau khi publish để trang cập nhật tức thì. Bản production nên để Spring bắn webhook này
(PLAN.md mục 12) và thêm secret.
