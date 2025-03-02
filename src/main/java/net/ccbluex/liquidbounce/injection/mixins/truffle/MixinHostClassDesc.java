/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.injection.mixins.truffle;

import net.ccbluex.liquidbounce.interfaces.MemberRetriever;
import net.ccbluex.liquidbounce.utils.client.ClientUtilsKt;
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Pseudo
@Mixin(targets = "com/oracle/truffle/host/HostClassDesc$Members")
public abstract class MixinHostClassDesc {

    @Unique
    private static Method mergeMethod;

    @Unique
    private static Method getMergeMethod() {
        if (mergeMethod == null) {
            try {
                Class<?> hostMethodDescClass = Class.forName("com.oracle.truffle.host.HostMethodDesc");
                mergeMethod = Class.forName("com.oracle.truffle.host.HostClassDesc$Members")
                        .getDeclaredMethod("merge", hostMethodDescClass, hostMethodDescClass);
                mergeMethod.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get merge method", e);
            }
        }
        return mergeMethod;
    }

    @Shadow(remap = false)
    @Final
    Map<String, Object> methods;

    @Shadow(remap = false)
    @Final
    Map<String, Object> fields;

    @Shadow(remap = false)
    @Final
    Map<String, Object> staticFields;

    @Shadow(remap = false)
    @Final
    Map<String, Object> staticMethods;


    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void remapClassDesc(CallbackInfo ci) {
        remapFieldEntries(fields, this::getField);
        remapFieldEntries(staticFields, this::getField);

        remapMethodEntries(methods);
        remapMethodEntries(staticMethods);
    }

    @Unique
    private void remapMethodEntries(Map<String, Object> map) {
        final var entries = new HashMap<>(map).entrySet();

        for (var entry : entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            try {
                // this is a bit more complicated than fields because of overloads

                final var methodsToRemap = new ArrayList<Method>();

                try {
                    // value instanceof HostMethodDesc.SingleMethod
                    methodsToRemap.add(getReflectionMethodFromSingleMethod(value));
                } catch (NoSuchMethodException _ignored) {
                    // value instanceof HostMethodDesc.OverloadedMethod
                    // which probably should not happen because intermediary names have no overloads?
                    final Method getOverloads = value.getClass().getDeclaredMethod("getOverloads");
                    getOverloads.setAccessible(true);
                    final var overloads = (Object[]) getOverloads.invoke(value);

                    for (final var overload : overloads) {
                        methodsToRemap.add(getReflectionMethodFromSingleMethod(overload));
                    }
                }

                for (final Method method : methodsToRemap) {
                    final String remappedName = remapDescriptor(method);

                    if (remappedName != null) {
                        if (map.containsKey(remappedName)) {
                            final Object mergedMethod = getMergeMethod().invoke(null, map.get(remappedName), value);
                            map.remove(key);
                            map.put(remappedName, mergedMethod);

                        } else {
                            map.remove(key);
                            map.put(remappedName, value);
                        }
                    }
                }


            } catch (Exception e) {
                ClientUtilsKt.getLogger().error("Failed to remap method: {}", key, e);
            }
        }
    }

    @Unique
    private static Method getReflectionMethodFromSingleMethod(Object value)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Method descMethod = value.getClass().getDeclaredMethod("getReflectionMethod");
        descMethod.setAccessible(true);
        return (Method) descMethod.invoke(value);
    }

    @Unique
    private void remapFieldEntries(Map<String, Object> map, MemberRetriever retriever) {
        var entries = new HashMap<>(map).entrySet();

        for (var entry : entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String remapped;

            try {
                Member member = retriever.getMember(value);
                remapped = remapDescriptor(member);
            } catch (ReflectiveOperationException e) {
                ClientUtilsKt.getLogger().error("Failed to remap field: {}", key, e);
                continue;
            }

            if (remapped != null) {
                map.remove(key);
                map.put(remapped, value);
            }
        }
    }

    @Unique
    private Member getField(Object o) throws IllegalAccessException, NoSuchFieldException {
        var descField = o.getClass().getDeclaredField("field");
        descField.setAccessible(true);
        return (Member) descField.get(o);
    }

    @Unique
    private static String remapDescriptor(Member member) {
        var name = member.getName();

        String remapped;
        if (member instanceof java.lang.reflect.Method) {
            remapped = EnvironmentRemapper.INSTANCE.remapMethod(member.getDeclaringClass(), name);
        } else if (member instanceof java.lang.reflect.Field) {
            remapped = EnvironmentRemapper.INSTANCE.remapField(member.getDeclaringClass(), name);
        } else {
            ClientUtilsKt.getLogger().error("Unknown member type: {}", member.getClass().getName());
            return null;
        }

        // If the name is the same, return the original field
        if (name.equals(remapped)) {
            return null;
        }

//        ClientUtilsKt.getLogger().debug("Remapped descriptor: {} in {} to {}", name, member.getDeclaringClass().getName(), remapped);
        return remapped;
    }

}
