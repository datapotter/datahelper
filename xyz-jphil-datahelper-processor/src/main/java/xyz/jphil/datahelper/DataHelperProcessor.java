package xyz.jphil.datahelper;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;
import xyz.jphil.datahelper.processor.util.CodeGeneratorUtils;
import xyz.jphil.datahelper.processor.util.FieldAnalyzer;
import xyz.jphil.datahelper.processor.util.FieldInfo;
import xyz.jphil.datahelper.processor.util.ProcessorUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor that generates {ClassName}_I interface with property accessors.
 *
 * Generated code includes:
 * - Field symbols ($fieldName constants)
 * - Fluent getters and setters
 * - 14 property accessor methods for DataHelper_I (8 for List/Object, 6 for Map)
 * - FIELDS list (immutable)
 *
 * <p>The generated interface extends DataHelper_I and implements all property accessor methods.
 * Serialization traits (JSON, ArcadeDB, etc.) use these property accessors to convert data.</p>
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("xyz.jphil.datahelper.DataHelper")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DataHelperProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.CLASS) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "Processing @DataHelper on " + element);
                    generateInterface((TypeElement) element);
                }
            }
        }
        return true;
    }

    private void generateInterface(TypeElement element) {
        String packageName = processingEnv.getElementUtils().getPackageOf(element).toString();
        String className = element.getSimpleName().toString();
        String interfaceName = className + "_I";

        // Initialize utilities
        ProcessorUtils utils = new ProcessorUtils(processingEnv);
        FieldAnalyzer analyzer = new FieldAnalyzer(processingEnv, utils);

        // Analyze fields (includes validation)
        List<FieldInfo> fields = analyzer.analyzeFields(element);
        if (fields == null) {
            return; // Validation errors found
        }

        // Build the interface
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("E",
                        ParameterizedTypeName.get(
                                ClassName.get(packageName, interfaceName),
                                TypeVariableName.get("E")
                        )));

        // Always extend DataHelper_I<E> as the base contract
        interfaceBuilder.addSuperinterface(ParameterizedTypeName.get(
                ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                TypeVariableName.get("E")
        ));

        // Add field symbols ($fieldName constants)
        CodeGeneratorUtils.addFieldSymbols(interfaceBuilder, fields, packageName, className);

        // Add FIELDS list
        CodeGeneratorUtils.addFieldsList(interfaceBuilder, fields, packageName, className);

        // Add verbose getter declarations
        for (FieldInfo field : fields) {
            // For boolean types, generate BOTH "is" and "get" getters for maximum compatibility
            if (ProcessorUtils.isBooleanType(field.type)) {
                // Generate abstract isXxx() method
                String isGetterName = "is" + ProcessorUtils.capitalize(field.name);
                MethodSpec isGetter = MethodSpec.methodBuilder(isGetterName)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(field.type)
                        .build();
                interfaceBuilder.addMethod(isGetter);

                // Generate default getXxx() method (delegates to isXxx)
                String getGetterName = "get" + ProcessorUtils.capitalize(field.name);
                MethodSpec getGetter = MethodSpec.methodBuilder(getGetterName)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(field.type)
                        .addStatement("return $N()", isGetterName)
                        .build();
                interfaceBuilder.addMethod(getGetter);
            } else {
                // For non-boolean types, use standard "get" prefix
                String getterName = "get" + ProcessorUtils.capitalize(field.name);
                MethodSpec getter = MethodSpec.methodBuilder(getterName)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(field.type)
                        .build();
                interfaceBuilder.addMethod(getter);
            }
        }

        // Add verbose setter declarations
        for (FieldInfo field : fields) {
            String setterName = "set" + ProcessorUtils.capitalize(field.name);
            MethodSpec setter = MethodSpec.methodBuilder(setterName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(field.type, field.name)
                    .build();
            interfaceBuilder.addMethod(setter);
        }

        // Add fluent getter defaults
        for (FieldInfo field : fields) {
            // Use "is" prefix for boolean types (primitive boolean and Boolean wrapper)
            String prefix = ProcessorUtils.isBooleanType(field.type) ? "is" : "get";
            String getterName = prefix + ProcessorUtils.capitalize(field.name);
            MethodSpec fluentGetter = MethodSpec.methodBuilder(field.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(field.type)
                    .addStatement("return $N()", getterName)
                    .build();
            interfaceBuilder.addMethod(fluentGetter);
        }

        // Add fluent setter defaults
        for (FieldInfo field : fields) {
            String setterName = "set" + ProcessorUtils.capitalize(field.name);
            MethodSpec fluentSetter = MethodSpec.methodBuilder(field.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                            .addMember("value", "$S", "unchecked")
                            .build())
                    .addParameter(field.type, field.name)
                    .returns(TypeVariableName.get("E"))
                    .addStatement("$N($N)", setterName, field.name)
                    .addStatement("return (E) this")
                    .build();
            interfaceBuilder.addMethod(fluentSetter);
        }

        // ========== Property Accessor Methods (15 methods for delegation) ==========
        // Using shared CodeGeneratorUtils

        // 0. dataClass()
        interfaceBuilder.addMethod(CodeGeneratorUtils.createDataClassMethod(packageName, className, true));

        // 1. fieldNames()
        interfaceBuilder.addMethod(CodeGeneratorUtils.createFieldNamesMethod(true));

        // 2. getPropertyByName(String)
        interfaceBuilder.addMethod(CodeGeneratorUtils.createGetPropertyByNameMethod(fields, utils, true));

        // 3. setPropertyByName(String, Object)
        interfaceBuilder.addMethod(CodeGeneratorUtils.createSetPropertyByNameMethod(fields, utils, true));

        // 4. getPropertyType(String)
        interfaceBuilder.addMethod(CodeGeneratorUtils.createGetPropertyTypeMethod(fields, true));

        // 5. createNestedObject(String)
        interfaceBuilder.addMethod(CodeGeneratorUtils.createNestedObjectMethod(fields, true));

        // 6. createListElement(String)
        interfaceBuilder.addMethod(CodeGeneratorUtils.createListElementMethod(fields, true));

        // 7. isListField(String)
        interfaceBuilder.addMethod(CodeGeneratorUtils.createIsListFieldMethod(fields, true));

        // 8. isNestedObjectField(String)
        interfaceBuilder.addMethod(CodeGeneratorUtils.createIsNestedObjectFieldMethod(fields, true));

        // 9-14. Map support methods
        CodeGeneratorUtils.addMapMethods(interfaceBuilder, fields, true);

        // Build and write the file
        TypeSpec interfaceSpec = interfaceBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, interfaceSpec)
                .indent("    ")
                .skipJavaLangImports(true)
                .addFileComment("Generated by DataHelperProcessor on " + LocalDateTime.now())
                .build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Generated interface: " + interfaceName + " for class: " + className);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate interface: " + e.getMessage());
        }
    }
}
