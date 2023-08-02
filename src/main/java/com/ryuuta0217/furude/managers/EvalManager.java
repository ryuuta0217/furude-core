/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package com.ryuuta0217.furude.managers;

import com.ryuuta0217.furude.FurudeCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.mozilla.javascript.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class EvalManager {
    private static final Map<String, Object> GLOBAL_STORAGE = new HashMap<>();
    private static final ContextFactory RHINO_CONTEXT_FACTORY = new ContextFactory();
    private static final Context RHINO_CONTEXT = RHINO_CONTEXT_FACTORY.enterContext();
    private static ScriptableObject GLOBAL_SCOPE = RHINO_CONTEXT.initStandardObjects();

    static {
        initGlobalScope();
    }

    public static void initGlobalScope() {
        importClassGlobally(EvalManager.class);
        importClassGlobally(RunnableManager.class);
        importClassGlobally(ListenerManager.class);

        // Java
        importClassGlobally(Class.class);
        importClassGlobally(Arrays.class);
        importClassGlobally(Stream.class);
        importClassGlobally(ArrayList.class);
        importClassGlobally(HashMap.class);
        importClassGlobally(HashSet.class);

        // Bukkit
        importClassGlobally(Bukkit.class);
        importClassGlobally(Material.class);
        importClassGlobally(EntityType.class);
        importClassGlobally(ChatColor.class);

        // Adventure
        importClassGlobally(Component.class);
        importClassGlobally(TextDecoration.class);
        importClassGlobally(TextColor.class);
        importClassGlobally(NamedTextColor.class);
        importClassGlobally(Style.class);

        putProperty("plugin", FurudeCore.getInstance());
        putProperty("Storage", GLOBAL_STORAGE);
    }

    public static void reload() {
        GLOBAL_SCOPE = RHINO_CONTEXT.initStandardObjects();
        initGlobalScope();
    }

    public static void defineFunction(String id, String script) {
        getRhinoContext().evaluateString(GLOBAL_SCOPE, script, id, 1, null);
    }

    public static Map<String, Object> getGlobalStorage() {
        return GLOBAL_STORAGE;
    }

    public static ContextFactory getRhinoContextFactory() {
        return RHINO_CONTEXT_FACTORY;
    }

    public static Context getRhinoContext() {
        return RHINO_CONTEXT;
    }

    public static ScriptableObject getGlobalScope() {
        return getRhinoContext().initStandardObjects(GLOBAL_SCOPE, false);
    }

    public static void importClassGlobally(Class<?> clazz) {
        importClassGlobally(clazz.getSimpleName(), clazz);
    }

    public static void importClassGlobally(String name, Class<?> clazz) {
        putProperty(name, new NativeJavaClass(GLOBAL_SCOPE, clazz));
    }

    public static void unImportClassGlobally(Class<?> clazz) {
        unImportClassGlobally(clazz.getSimpleName(), clazz);
    }

    public static void unImportClassGlobally(String name, Class<?> clazz) {
        if (ScriptableObject.hasProperty(GLOBAL_SCOPE, name) && ScriptableObject.getProperty(GLOBAL_SCOPE, name) instanceof NativeJavaClass nClass && nClass.getClassObject().equals(clazz)) {
            removeProperty(name);
        }
    }

    public static void putProperty(String name, Object value) {
        if (ScriptableObject.hasProperty(GLOBAL_SCOPE, name)) throw new IllegalArgumentException("Property name " + name + " already taken! remove Unregister");
        ScriptableObject.putProperty(GLOBAL_SCOPE, name, value);
    }

    public static void removeProperty(String name) {
        ScriptableObject.deleteProperty(GLOBAL_SCOPE, name);
    }

    public static Script compile(String sourceName, String script) {
        return RHINO_CONTEXT.compileString(script, sourceName, 1, null);
    }

    public static Function compileFunction(String sourceName, String script) {
        return RHINO_CONTEXT.compileFunction(GLOBAL_SCOPE, script, sourceName, 1, null);
    }

    public static Object exec(Script script, @Nullable Scriptable scope) {
        return script.exec(getRhinoContext(), scope == null ? GLOBAL_SCOPE : scope);
    }

    public static Object execFromString(String script, @Nullable Scriptable scope) {
        return execFromString("EvalManager#execFromString", script, scope);
    }

    public static Object execFromString(String sourceName, String script, @Nullable Scriptable scope) {
        return exec(compile(sourceName, script), scope);
    }
}
