/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import java.util.HashMap;
import java.util.Map;

@Pseudo
@Mixin(targets = "com/oracle/truffle/host/HostClassDesc$Members")
public abstract class MixinHostClassDesc {

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
        remapEntries(methods, this::getMethod);
        remapEntries(fields, this::getField);
        remapEntries(staticFields, this::getField);
        remapEntries(staticMethods, this::getMethod);
    }

    @Unique
    private void remapEntries(Map<String, Object> map, MemberRetriever retriever) {
        var entries = new HashMap<>(map).entrySet();

        for (var entry : entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String remapped;

            try {
                Member member = retriever.getMember(value);
                remapped = remapDescriptor(member);
            } catch (ReflectiveOperationException e) {
                ClientUtilsKt.getLogger().error("Failed to remap: {}", key, e);
                continue;
            }

            if (remapped != null) {
                map.remove(key);
                map.put(remapped, value);
            }
        }
    }

    @Unique
    private Member getMethod(Object o) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            // If this works, it is likely a SingleMethod instance
            Method descMethod = o.getClass().getDeclaredMethod("getReflectionMethod");

            descMethod.setAccessible(true);
            return (Member) descMethod.invoke(o);
        } catch (NoSuchMethodException ignored) {
            try {
                var getOverloads = o.getClass().getDeclaredMethod("getOverloads");
                var overloads = (Object[]) getOverloads.invoke(o);

                return getMethod(overloads[0]);
            } catch (NoSuchMethodException ignored2) {
                ClientUtilsKt.getLogger().error("Unsupported method type: {}", o.getClass().getName());
            }

            return null;
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
