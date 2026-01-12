///usr/bin/env jbang "$0" "$@" ; exit $?
// Standalone Java code (not part of main project) - replaces bash/python/batch scripts with IDE-friendly, maintainable code using JDK 11/21/25 enhancements. To know why, refer to Cay Horstmann's JavaOne 2025 talk "Java for Small Coding Tasks" (https://youtu.be/04wFgshWMdA)

//JAVA 21+

import javax.script.*;
import java.nio.file.*;

public class GraalVMScriptEngineTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== JavaScript Test (ScriptEngine) ===\n");

        Path jsFile = Paths.get("../xyz-jphil-datahelper-test/target/generated/js/map-support-test/test.js");

        if (!Files.exists(jsFile)) {
            System.err.println("❌ Error: test.js not found at " + jsFile.toAbsolutePath());
            System.err.println("   Run 'mvn compile' in xyz-jphil-datahelper-test first");
            System.exit(1);
        }

        System.out.println("📂 Loading JavaScript from: " + jsFile.toAbsolutePath());
        String jsCode = Files.readString(jsFile);

        System.out.println("🚀 Creating JavaScript engine...");
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");

        if (engine == null) {
            // Try alternatives
            engine = manager.getEngineByName("javascript");
        }
        if (engine == null) {
            engine = manager.getEngineByName("nashorn");
        }
        if (engine == null) {
            engine = manager.getEngineByName("graal.js");
        }

        if (engine == null) {
            System.err.println("❌ No JavaScript engine found!");
            System.err.println("Available engines:");
            for (ScriptEngineFactory factory : manager.getEngineFactories()) {
                System.err.println("  - " + factory.getEngineName() + " (" +
                    String.join(", ", factory.getNames()) + ")");
            }
            System.err.println("\nTo use GraalVM JavaScript, you need GraalVM JDK installed.");
            System.err.println("Download from: https://www.graalvm.org/downloads/");
            System.exit(1);
        }

        System.out.println("✓ Using engine: " + engine.getFactory().getEngineName() + "\n");

        try {
            System.out.println("📜 Evaluating TeaVM JavaScript...\n");
            engine.eval(jsCode);

            System.out.println("📞 Calling main()...\n");

            // Call main function
            Invocable invocable = (Invocable) engine;
            Object mainFunc = engine.get("main");

            if (mainFunc != null) {
                invocable.invokeFunction("main");
                System.out.println("\n✅ Test execution completed");
            } else {
                System.err.println("❌ main() function not found");
                System.exit(1);
            }

        } catch (ScriptException e) {
            System.err.println("❌ JavaScript execution error at line " + e.getLineNumber() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchMethodException e) {
            System.err.println("❌ Could not invoke main(): " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
