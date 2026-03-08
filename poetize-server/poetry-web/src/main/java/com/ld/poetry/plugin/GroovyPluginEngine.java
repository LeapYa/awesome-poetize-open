package com.ld.poetry.plugin;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groovy 后端插件引擎
 * <p>
 * 负责编译、缓存和执行 Groovy 后端插件脚本。
 * 每个插件脚本通过 pluginKey 注册，执行时传入上下文 Map。
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-19
 */
@Slf4j
@Component
public class GroovyPluginEngine {

    /** 已编译的脚本类缓存：pluginKey -> Script Class */
    private final Map<String, Class<?>> compiledScripts = new ConcurrentHashMap<>();

    /** 插件的钩子注册：hookName -> List<pluginKey> */
    private final Map<String, java.util.List<String>> hookRegistry = new ConcurrentHashMap<>();

    private final GroovyClassLoader groovyClassLoader;

    public GroovyPluginEngine() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceEncoding("UTF-8");
        // 允许导入常用类
        ImportCustomizer imports = new ImportCustomizer();
        imports.addImports("java.time.LocalDateTime", "java.util.Map", "java.util.List");
        config.addCompilationCustomizers(imports);
        this.groovyClassLoader = new GroovyClassLoader(
                Thread.currentThread().getContextClassLoader(), config);
    }

    /**
     * 加载（编译并缓存）插件脚本
     *
     * @param pluginKey    插件 key
     * @param groovySource Groovy 源码
     */
    public void loadPlugin(String pluginKey, String groovySource) {
        try {
            Class<?> scriptClass = groovyClassLoader.parseClass(groovySource, pluginKey + ".groovy");
            compiledScripts.put(pluginKey, scriptClass);
            log.info("Groovy 插件 [{}] 编译成功", pluginKey);

            // 执行 register(hookRegistry) 让插件声明它监听哪些钩子
            Object instance = scriptClass.getDeclaredConstructor().newInstance();
            if (instance instanceof Script script) {
                Binding binding = new Binding();
                binding.setVariable("pluginKey", pluginKey);
                binding.setVariable("hookRegistry", hookRegistry);
                script.setBinding(binding);
                // 调用 register 方法（插件可通过 register 方法注册钩子）
                try {
                    scriptClass.getMethod("register").invoke(instance);
                } catch (NoSuchMethodException ignored) {
                    // 插件没有 register 方法，跳过
                }
            }
        } catch (Exception e) {
            log.error("Groovy 插件 [{}] 编译失败", pluginKey, e);
            throw new RuntimeException("插件 Groovy 脚本编译失败: " + e.getMessage(), e);
        }
    }

    /**
     * 卸载插件
     */
    public void unloadPlugin(String pluginKey) {
        compiledScripts.remove(pluginKey);
        // 从所有钩子中移除此插件
        hookRegistry.values().forEach(list -> list.remove(pluginKey));
        log.info("Groovy 插件 [{}] 已卸载", pluginKey);
    }

    /**
     * 触发钩子，执行所有注册此钩子的插件
     *
     * @param hookName 钩子名称
     * @param context  上下文数据
     */
    public void triggerHook(String hookName, Map<String, Object> context) {
        java.util.List<String> plugins = hookRegistry.getOrDefault(hookName, java.util.List.of());
        for (String pluginKey : plugins) {
            Class<?> scriptClass = compiledScripts.get(pluginKey);
            if (scriptClass == null)
                continue;
            try {
                Object instance = scriptClass.getDeclaredConstructor().newInstance();
                if (instance instanceof Script script) {
                    Binding binding = new Binding();
                    binding.setVariable("context", context);
                    binding.setVariable("pluginKey", pluginKey);
                    script.setBinding(binding);
                    // 调用插件的 on{HookName} 方法，如 onArticleCreate
                    String methodName = "on" + hookName.substring(0, 1).toUpperCase() + hookName.substring(1);
                    try {
                        scriptClass.getMethod(methodName, Map.class).invoke(instance, context);
                    } catch (NoSuchMethodException ignored) {
                        // 插件没有处理此钩子的方法
                    }
                }
            } catch (Exception e) {
                log.error("Groovy 插件 [{}] 执行钩子 [{}] 失败", pluginKey, hookName, e);
                // 插件执行失败不影响主流程
            }
        }
    }

    /**
     * 调用插件的指定方法并返回结果
     * <p>
     * 供 GroovyPaymentAdapter 等桥接器使用，与 triggerHook 的区别是本方法有返回值。
     * </p>
     *
     * @param pluginKey  插件 key
     * @param methodName 要调用的方法名（如 getPaymentUrl、verifyCallback）
     * @param context    传入的上下文参数
     * @return 方法返回值，若方法不存在则返回 null
     * @throws IllegalStateException 插件未加载时抛出
     */
    public Object invokeMethod(String pluginKey, String methodName, java.util.Map<String, Object> context) {
        Class<?> scriptClass = compiledScripts.get(pluginKey);
        if (scriptClass == null) {
            throw new IllegalStateException("Groovy 插件 [" + pluginKey + "] 未加载，请先安装并启用该插件");
        }
        try {
            Object instance = scriptClass.getDeclaredConstructor().newInstance();
            if (instance instanceof Script script) {
                Binding binding = new Binding();
                binding.setVariable("ctx", context);
                binding.setVariable("log", LoggerFactory.getLogger("plugin." + pluginKey));
                script.setBinding(binding);
                try {
                    return scriptClass.getMethod(methodName, java.util.Map.class).invoke(instance, context);
                } catch (NoSuchMethodException e) {
                    log.warn("Groovy 插件 [{}] 未实现方法 [{}]", pluginKey, methodName);
                    return null;
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException("Groovy 插件 [" + pluginKey + "] 方法 [" + methodName + "] 执行失败: "
                    + (cause != null ? cause.getMessage() : e.getMessage()), cause != null ? cause : e);
        } catch (Exception e) {
            throw new RuntimeException("调用 Groovy 插件 [" + pluginKey + "] 方法 [" + methodName + "] 失败", e);
        }
        return null;
    }

    /**
     * 判断是否有指定 key 的插件已加载
     */
    public boolean isLoaded(String pluginKey) {
        return compiledScripts.containsKey(pluginKey);
    }

    /**
     * 获取所有已加载的插件 key 列表
     */
    public java.util.Set<String> getLoadedPlugins() {
        return compiledScripts.keySet();
    }
}
