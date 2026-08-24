package auto.ui.api.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PageConstant {
    /**
     * Whitelist block type được phép publish — phải khớp shared/blocks.ts của FE.
     * Thêm block mới là phải sửa cả 3 nơi: shared/blocks.ts, component .tsx, và danh sách này.
     */
    public static final Set<String> ALLOWED_BLOCK_TYPES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("hero", "cta")));

    private PageConstant() {
        throw new IllegalStateException("Utility class");
    }
}
