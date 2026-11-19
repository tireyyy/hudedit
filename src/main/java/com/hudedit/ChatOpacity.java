package com.hudedit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Временно умножает ванильную прозрачность чата и фона чата на нашу.
 * Значения подменяются напрямую в SimpleOption (без setValue, чтобы не дёргать callback'и)
 * и возвращаются обратно сразу после отрисовки чата.
 */
public final class ChatOpacity {
    private record Saved(Field field, Object owner, Object original) {}

    private static final List<Saved> SAVED = new ArrayList<>();

    private ChatOpacity() {}

    public static void begin(float k) {
        SAVED.clear();
        if (k >= 0.999f) return;
        GameOptions o = MinecraftClient.getInstance().options;
        scale(o.getChatOpacity(), k);
        scale(o.getTextBackgroundOpacity(), k);
    }

    public static void end() {
        for (Saved s : SAVED) {
            try {
                s.field().set(s.owner(), s.original());
            } catch (Exception ignored) {
            }
        }
        SAVED.clear();
    }

    private static void scale(SimpleOption<Double> opt, float k) {
        try {
            Double cur = opt.getValue();
            for (Field f : SimpleOption.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType() != Object.class) continue;
                f.setAccessible(true);
                if (f.get(opt) == cur) { // то самое поле "value" (сравнение по ссылке)
                    SAVED.add(new Saved(f, opt, cur));
                    f.set(opt, Double.valueOf(cur * k));
                }
            }
        } catch (Exception ignored) {
        }
    }
}
