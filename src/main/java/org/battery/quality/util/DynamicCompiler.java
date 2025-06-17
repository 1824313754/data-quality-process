package org.battery.quality.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态编译器，用于编译Java源代码字符串并加载生成的类
 */
public class DynamicCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCompiler.class);

    /**
     * 编译Java源代码并返回编译后的类
     * @param className 完整的类名（包含包名）
     * @param sourceCode 源代码内容
     * @return 编译后的类对象
     * @throws Exception 如果编译失败或加载类失败
     */
    public static Class<?> compile(String className, String sourceCode) throws Exception {
        // 获取系统Java编译器
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("找不到Java编译器，请确保运行在JDK环境下");
        }
        
        // 获取诊断收集器
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        
        // 获取标准文件管理器
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        
        // 创建内存文件管理器
        try (MemoryJavaFileManager fileManager = new MemoryJavaFileManager(standardFileManager)) {
            // 创建源代码对象
            JavaFileObject javaFileObject = new MemoryJavaFileObject(className, sourceCode);
            
            // 编译选项
            List<String> options = new ArrayList<>();
            options.add("-classpath");
            options.add(getClassPath());
            
            // 准备编译任务
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, Collections.singletonList(javaFileObject));
            
            // 执行编译
            boolean success = task.call();
            
            // 检查编译结果
            if (!success) {
                StringBuilder errorMsg = new StringBuilder("编译失败:\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errorMsg.append(String.format("第 %d 行, 位置 %d: %s%n", 
                            diagnostic.getLineNumber(), 
                            diagnostic.getColumnNumber(), 
                            diagnostic.getMessage(null)));
                }
                throw new Exception(errorMsg.toString());
            }
            
            // 获取编译后的类
            Map<String, byte[]> classBytes = fileManager.getClassBytes();
            byte[] compiled = classBytes.get(className);
            
            if (compiled == null) {
                // 尝试找出实际的类名（源代码中可能和提供的className不一致）
                String actualClassName = extractClassName(sourceCode);
                compiled = classBytes.get(actualClassName);
                if (compiled == null) {
                    throw new Exception("编译成功但找不到类文件 " + className);
                }
                className = actualClassName;
            }
            
            // 创建类加载器并加载类
            DynamicClassLoader classLoader = new DynamicClassLoader(Thread.currentThread().getContextClassLoader());
            return classLoader.defineClass(className, compiled);
        }
    }
    
    /**
     * 从源代码中提取类名（包含包名）
     */
    private static String extractClassName(String sourceCode) {
        // 匹配包名
        String packageName = "";
        Pattern packagePattern = Pattern.compile("\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;");
        Matcher packageMatcher = packagePattern.matcher(sourceCode);
        if (packageMatcher.find()) {
            packageName = packageMatcher.group(1) + ".";
        }
        
        // 匹配类名
        Pattern classPattern = Pattern.compile("\\s*(public|private|protected)?\\s*class\\s+([a-zA-Z0-9_]+)");
        Matcher classMatcher = classPattern.matcher(sourceCode);
        if (classMatcher.find()) {
            return packageName + classMatcher.group(2);
        }
        
        return null;
    }
    
    /**
     * 获取当前ClassPath
     */
    private static String getClassPath() {
        StringBuilder classpath = new StringBuilder();
        
        // 从系统属性获取classpath
        String cpFromProperty = System.getProperty("java.class.path");
        if (cpFromProperty != null && !cpFromProperty.isEmpty()) {
            classpath.append(cpFromProperty);
        }
        
        // 获取当前ClassLoader的URL
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        if (currentClassLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) currentClassLoader;
            for (URL url : urlClassLoader.getURLs()) {
                if (classpath.length() > 0) {
                    classpath.append(File.pathSeparator);
                }
                classpath.append(url.getFile());
            }
        }
        
        return classpath.toString();
    }
    
    /**
     * 动态类加载器，用于加载编译后的字节码
     */
    private static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }
        
        public Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
    
    /**
     * 内存中的Java源文件对象
     */
    private static class MemoryJavaFileObject extends SimpleJavaFileObject {
        private final String sourceCode;
        
        public MemoryJavaFileObject(String className, String sourceCode) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }
        
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }
    }
    
    /**
     * 内存中的Java文件管理器，用于保存编译后的字节码
     */
    private static class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        // 存储编译后的字节码，键为完整类名，值为字节码
        private final Map<String, ByteArrayOutputStream> classBytes = new HashMap<>();
        
        public MemoryJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }
        
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            if (kind == JavaFileObject.Kind.CLASS) {
                // 为该类创建一个字节输出流
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                classBytes.put(className, bos);
                return new MemoryOutputJavaFileObject(className, bos);
            }
            return super.getJavaFileForOutput(location, className, kind, sibling);
        }
        
        public Map<String, byte[]> getClassBytes() {
            Map<String, byte[]> result = new HashMap<>();
            for (Map.Entry<String, ByteArrayOutputStream> entry : classBytes.entrySet()) {
                result.put(entry.getKey(), entry.getValue().toByteArray());
            }
            return result;
        }
    }
    
    /**
     * 内存中的Java输出文件对象，用于存储编译后的字节码
     */
    private static class MemoryOutputJavaFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream byteStream;
        
        public MemoryOutputJavaFileObject(String className, ByteArrayOutputStream byteStream) {
            super(URI.create("byte:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
            this.byteStream = byteStream;
        }
        
        @Override
        public OutputStream openOutputStream() throws IOException {
            return byteStream;
        }
    }
} 