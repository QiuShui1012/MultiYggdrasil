package zh.qiushui.mod.multiyggdrasil.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class OptionalUtil {
    public static <T> T ifPresentDoAndReturn(@Nullable T t, Consumer<T> ifPresent) {
        if (t != null) {
            ifPresent.accept(t);
        }
        return t;
    }
}
