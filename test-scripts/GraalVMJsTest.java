///usr/bin/env jbang "$0" "$@" ; exit $?
// Standalone Java code (not part of main project) - replaces bash/python/batch scripts with IDE-friendly, maintainable code using JDK 11/21/25 enhancements. To know why, refer to Cay Horstmann's JavaOne 2025 talk "Java for Small Coding Tasks" (https://youtu.be/04wFgshWMdA)

//DEPS org.graalvm.sdk:nativeimage:24.1.1
//DEPS org.graalvm.truffle:truffle-api:24.1.1

//FILES ../xyz-jphil-datahelper-test/target/generated/js/map-support-test

import org.graalvm.polyglot.*;
import java.io.IOException;
import java.nio.file.*;

public class GraalVMJsTest {

    public static void main(String[] args) {
        System.out.println("=== GraalVM JavaScript Test ===\n");

        Path jsFile = Paths.get("../xyz-jphil-datahelper-test/target/generated/js/map-support-test/test.js");

        if (!Files.exists(jsFile)) {
            System.err.println("❌ Error: test.js not found at " + jsFile.toAbsolutePath());
            System.err.println("   Run 'mvn compile' in xyz-jphil-datahelper-test first");
            System.exit(1);
        }

        try {
            System.out.println("📂 Loading JavaScript from: " + jsFile.toAbsolutePath());
            String jsCode = Files.readString(jsFile);

            System.out.println("🚀 Creating GraalVM JavaScript context...");
            try (Context context = Context.newBuilder("js")
                    .allowAllAccess(true)
                    .option("js.ecmascript-version", "2022")
                    .build()) {

                // Provide console.log implementation
                context.eval("js", "var console = { log: function(...args) { print(args.join(' ')); } };");

                System.out.println("📜 Evaluating TeaVM JavaScript...\n");
                context.eval("js", jsCode);

                System.out.println("\n📞 Calling main()...\n");

                // Call the main function
                Value mainFunction = context.getBindings("js").getMember("main");
                if (mainFunction != null && mainFunction.canExecute()) {
                    mainFunction.execute();
                    System.out.println("\n✅ Test execution completed");
                } else {
                    System.err.println("❌ main() function not found or not executable");
                    System.exit(1);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error reading JavaScript file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (PolyglotException e) {
            System.err.println("❌ JavaScript execution error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
