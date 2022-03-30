package com.mapper.compiler;

import com.mapper.annotation.Mapper;
import com.mapper.annotation.ViewCreator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Created time : 2022/3/30 21:26.
 *
 * @author 10585
 */
@SupportedOptions(value = {MapperProcessor.OPTION_CONFIG_PATH, MapperProcessor.OPTION_ROOT_PATH})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MapperProcessor extends AbstractProcessor {
    public static final String OPTION_CONFIG_PATH = "configPath";
    public static final String OPTION_ROOT_PATH = "rootProjectPath";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Mapper.class.getName());
        return types;
    }

    private boolean isProcessed = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        isProcessed = false;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (isProcessed) return false;
        isProcessed = true;

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Mapper.class);
        if (elements.isEmpty()) return false;

        String configPath = processingEnv.getOptions().get(OPTION_CONFIG_PATH);
        if (configPath == null) {
            return false;
        }
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            return false;
        }
        Config config = ConfigUtils.parseConfig(configFile);
        if (config == null) return false;
        if (config.mapper.isEmpty()) return false;

        Elements elementUtils = processingEnv.getElementUtils();

        TypeElement typeElement = (TypeElement) elements.iterator().next();

        /*获取包名*/
        String packageName = elementUtils
                .getPackageOf(typeElement).getQualifiedName().toString();

        String rootPath = processingEnv.getOptions().get(OPTION_ROOT_PATH);

        File root = new File(rootPath);

        try {
            mapper(root, config, packageName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static final class MapperFile {
        public final File debugDir;
        public final File releaseDir;

        public MapperFile(File debugDir, File releaseDir) {
            this.debugDir = debugDir;
            this.releaseDir = releaseDir;
        }
    }

    private static final String debugRes = "src" + File.separator + "debug" + File.separator + "res";
    private static final String releaseRes = "src" + File.separator + "release" + File.separator + "res";
    private static final String layout = File.separator + "layout";


    public void mapper(File root, Config config, String packageName) throws IOException {
        System.out.println("root====>"+root.getAbsolutePath());
        List<File> moduleDirs = new ArrayList<>();
        for (String module : config.module) {
            moduleDirs.add(new File(root.getAbsolutePath() + File.separator + module));
        }

        List<MapperFile> mapperDirs = new ArrayList<>();

        for (File moduleDir : moduleDirs) {
            String defaultLayoutDir = moduleDir.getAbsolutePath() + File.separator
                    + debugRes + layout;
            File layoutDirFile = new File(defaultLayoutDir);

            if (layoutDirFile.exists()) {
                String releaseDir = moduleDir.getAbsolutePath() + File.separator
                        + releaseRes + layout;

                mapperDirs.add(new MapperFile(layoutDirFile, new File(releaseDir)));
            }

            for (String layoutDir : config.layoutDir) {
                String dir = moduleDir.getAbsolutePath() + File.separator
                        + debugRes + File.separator
                        + layoutDir + layout;

                File expandDir = new File(dir);
                if (expandDir.exists()) {
                    String releaseDir = moduleDir.getAbsolutePath() + File.separator
                            + releaseRes + File.separator
                            + layoutDir + layout;
                    mapperDirs.add(new MapperFile(expandDir, new File(releaseDir)));
                }
            }
        }

        //create creator class
        createCreatorClass(config, packageName);

        //copy to release res folder and replace to custom tag
        copyAndReplaceTag(config, mapperDirs);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void copyAndReplaceTag(Config config, List<MapperFile> mapperDirs) throws IOException {
        Map<String, String> replace = config.replace;
        Set<String> exclude = config.exclude;

        for (MapperFile mapperDir : mapperDirs) {
            File debugDir = mapperDir.debugDir;

            String debugPath = debugDir.getAbsolutePath();

            File[] files = debugDir.listFiles(pathname -> !exclude.contains(pathname.getName()));
            if (files == null || files.length == 0) continue;

            File releaseDir = mapperDir.releaseDir;

            StringBuilder sbl = new StringBuilder();
            BufferedReader reader;
            BufferedWriter writer;

            Set<String> keySet = replace.keySet();

            for (File file : files) {
                sbl.setLength(0);

                String relativePath = file.getAbsolutePath().replace(debugPath, "");
                reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();

                while (line != null) {
                    String trim = line.trim();

                    if (trim.contains("<")) {
                        if (keySet.contains(trim)) {
                            String newLine = replace.get(trim);

                            sbl.append(line.replace(trim, newLine));
                        } else {
                            String[] split = trim.split("\\s+");

                            String s = split[0];
                            if (keySet.contains(s)) {
                                String newLine = replace.get(s);

                                sbl.append(line.replace(s, newLine));
                            } else {
                                sbl.append(line);
                            }
                        }
                    } else {
                        sbl.append(line);
                    }

                    sbl.append("\n");
                    line = reader.readLine();
                }

                sbl.substring(0, sbl.length() - 1);
                reader.close();

                String destPath = releaseDir.getAbsolutePath() + File.separator + relativePath;
                File destFile = new File(destPath);
                File parentFile = destFile.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                if (!destFile.exists()) {
                    destFile.createNewFile();
                }

                writer = new BufferedWriter(new FileWriter(destFile));
                writer.write(sbl.toString());
                writer.close();
            }
        }
    }


    //create creator class
    private void createCreatorClass(Config config, String packageName) throws IOException {

        String creatorClass = config.creatorClass;

        Map<String, String> mapper = config.mapper;

        CodeBlock.Builder switchBuilder = CodeBlock.builder();

        switchBuilder.add("switch(name){\n");
        for (Map.Entry<String, String> entry : mapper.entrySet()) {
            String value = entry.getValue();

            String key = entry.getKey();

            String pkg;
            String claName;
            if (key.contains(".")) {
                pkg = key.substring(0, key.lastIndexOf("."));
                claName = key.substring(key.lastIndexOf(".") + 1);
            } else {
                pkg = getPackage(key);
                claName = key;
            }

//            String pkg = key.substring(0, key.lastIndexOf("."));
//            String claName = key.substring(key.lastIndexOf(".") + 1);

            ClassName clazz = ClassName.get(pkg, claName);
            switchBuilder
                    .indent()
                    .add("case $S:\n", value)
                    .indent()
                    .add("return new $T(context, attrs);\n", clazz)
                    .unindent()
                    .unindent();
        }
        switchBuilder
                .indent()
                .add("default:\n")
                .indent()
                .add("return null;\n")
                .unindent()
                .unindent()
                .add("}\n");

        ClassName view = ClassName.get("android.view", "View");

        ClassName context = ClassName.get("android.content", "Context");
        ClassName string = ClassName.get(String.class);
        ClassName attribute = ClassName.get("android.util", "AttributeSet");

        List<ParameterSpec> list = Arrays.asList(
                ParameterSpec.builder(context, "context").build(),
                ParameterSpec.builder(string, "name").build(),
                ParameterSpec.builder(attribute, "attrs").build()
        );

        ClassName nullable = ClassName.get("androidx.annotation", "Nullable");
        MethodSpec.Builder createViewMethod = MethodSpec.methodBuilder("createView")
                .addAnnotation(nullable)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(view)
                .addParameters(list)
                .addCode(switchBuilder.build());


        TypeSpec build = TypeSpec.classBuilder(creatorClass)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)

                .addSuperinterface(ViewCreator.class)
                .addMethod(createViewMethod.build())
                .build();

        JavaFile javaFile = JavaFile.builder(packageName, build)
                .indent("    ")
                .skipJavaLangImports(true)
                .build();


        javaFile.writeTo(processingEnv.getFiler());
//        javaFile.writeTo(System.out);
    }

    private static String getPackage(String viewNodeName) {
        switch (viewNodeName) {
            case "View":
            case "ViewGroup":
            case "ViewStub":
            case "TextureView":
            case "SurfaceView":
                return "android.view";
            case "WebView":
                return "android.webkit";
            default:
                return "android.widget";
        }
    }
}