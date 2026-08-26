package com.bng.marketcoordination.integration;

import com.bng.marketcoordination.MarketCoordinationMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class ReflectionHelper {
    private ReflectionHelper() {}

    public static Optional<Method> findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            Method method = type.getDeclaredMethod(name, params);
            method.setAccessible(true);
            return Optional.of(method);
        } catch (ReflectiveOperationException ex) {
            MarketCoordinationMod.LOGGER.debug("Method {}#{} not found", type.getName(), name);
            return Optional.empty();
        }
    }

    public static Optional<Field> findField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return Optional.of(field);
        } catch (ReflectiveOperationException ex) {
            MarketCoordinationMod.LOGGER.debug("Field {}#{} not found", type.getName(), name);
            return Optional.empty();
        }
    }
}
