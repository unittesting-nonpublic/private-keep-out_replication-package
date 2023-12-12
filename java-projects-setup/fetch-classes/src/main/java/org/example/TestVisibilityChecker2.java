package org.example;

import com.opencsv.CSVWriter;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.code.*;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtConstructorImpl;
import spoon.support.reflect.declaration.CtFieldImpl;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class TestVisibilityChecker2 {
    private final String absPath;
    private Set<CtPackage> packageName;
    private Map<Pair<String,String>,ModifierKind> CUTAccessKindPrivate = new HashMap<>();
    private static String reportPath;


    private TestVisibilityChecker2(String absPath) {
        this.absPath = absPath;
        this.packageName = new HashSet<>();
    }

    public void run() {
        // get CUT access (it returns null if it's private-package)
        Map<Pair<String,String>,ModifierKind> CUTAccessKind = getClassUnderTestAccessKind(absPath);
//        writeCUTToCsv(CUTAccessKind);
//        HashMap<Pair<String, String>, ModifierKind> testAccessKind = getTestAccessKind(absPath, CUTAccessKind);

        // filter to only statements that calls non-public method
//        testAccessKind.entrySet().removeIf(entry -> entry.getValue() == ModifierKind.PUBLIC);
//        writeToCSV(testAccessKind);
    }

    private void writeCUTToCsv(Map<Pair<String,String>, ModifierKind> testAccessKind) {
//        Files.createDirectories(Paths.get(reportPath + absPath.split("/")[absPath.split("/").length - 1]));
        if(testAccessKind.size() == 0) {
            System.out.println();
            System.out.println("Project setup failed: " + absPath.split("/")[absPath.split("/").length - 1]);

            try (CSVWriter writer = new CSVWriter(new FileWriter(reportPath + "zzz_fail_project.csv", true))) {
                writer.writeNext(new String[]{String.valueOf(absPath.split("/")[absPath.split("/").length - 1]),
                        String.valueOf(Instant.now()),"No TestAccessKind"});
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.exit(0);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportPath
                + absPath.split("/")[absPath.split("/").length - 1]
                + "_all_method_visibility3.tsv"))) {
            System.out.println(reportPath
                    + absPath.split("/")[absPath.split("/").length - 1]
                    + "_all_method_visibility3.tsv");
            writer.println(String.join("\t", new String[]{"method", "type", "visibility"}));
            testAccessKind.forEach((key, value) -> {
                writer.println(key.getKey().replaceAll("\\r\\n|\\r|\\n", " ") + "\t" + key.getValue() + "\t" + ((value == null) ? "package-private" : value));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // remove failing tests from mvn test
    private List<String> readLogFailTest() {
        File logFile = new File(absPath + "mvn-test.log");
        List<String> failTest = new ArrayList<>();

        try {
            Scanner scanner = new Scanner(logFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("[ERROR] ") && line.matches(".*<<< FAILURE!$"))
                    failTest.add(line.replaceFirst("^\\[ERROR\\] ", "")
                            .replaceAll("  Time elapsed: .*", "()")
                            .replaceAll(" ",""));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return failTest;
    }

    private void writeToCSV(HashMap<Pair<String, String>, ModifierKind> testAccessKind) {
//        Files.createDirectories(Paths.get(reportPath + absPath.split("/")[absPath.split("/").length - 1]));
        List<String> failTests = readLogFailTest();
//        System.out.println("Remove method invoked by fail test: " + value.getRight() + " ---> " + value.getLeft());
        testAccessKind.entrySet().removeIf(entry -> failTests.contains(entry.getKey().getRight()));

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportPath
                + absPath.split("/")[absPath.split("/").length - 1]
                + "_method_visibility.tsv"))) {
            writer.println(String.join("\t", new String[]{"visibility", "method_called", "test"}));
            if (testAccessKind.size() == 0){
                try (CSVWriter writer2 = new CSVWriter(new FileWriter(reportPath + "zzz_fail_project.csv", true))) {
                    writer2.writeNext(new String[]{String.valueOf(absPath.split("/")[absPath.split("/").length - 1]),
                            String.valueOf(Instant.now()),"No Tests"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            testAccessKind.forEach((key, value) -> {
                writer.println(((value == null) ? "package-private" : value) + "\t" + key.getLeft() + "\t" + key.getRight());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

//        try (PrintWriter writer = new PrintWriter(new FileWriter(reportPath
//                + absPath.split("/")[absPath.split("/").length - 1]
//                + "_method_visibility_non_public.tsv"))) {
//            writer.println(String.join("\t", new String[]{"visibility", "method_called", "test"}));
//            testAccessKind.entrySet().removeIf(entry -> entry.getValue() == ModifierKind.PUBLIC);
//            testAccessKind.forEach((key, value) -> {
//                writer.println(((value == null) ? "package-private" : value) + "\t" + key.getLeft() + "\t" + key.getRight());
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    /*
        get tests local variables invocation and constructors (which method that each statement is calling).
        will only check methods that exists in the CUTAccessKind HashMap
    */
    // Creating Source Code Analysis for Test Class (DONE)
    // Creating Source Code Analysis for Multiple Test Classes (DONE)
    // TODO: Simplify this
    HashMap<Pair<String, String>, ModifierKind> testAccessKind = new HashMap<>();
    private HashMap<Pair<String, String>, ModifierKind> getTestAccessKind(String absPath, Map<String,ModifierKind> CUTAccessKind){
//        HashMap<Pair<String, String>, ModifierKind> testAccessKind = new HashMap<>();

        List<CtType<?>> testClasses = spoonLauncher(absPath,MavenLauncher.SOURCE_TYPE.TEST_SOURCE);

        testClasses.forEach(testClass -> {
            testClass.getNestedTypes().forEach(method -> {
                if (method instanceof CtClassImpl) {
                    ((CtClassImpl) method).getConstructors().forEach(constructor -> {
                        if (((CtConstructorImpl) constructor).getBody() != null && ((CtConstructorImpl) constructor).getBody().getStatements() != null ) {
                            ((CtConstructorImpl) constructor).getBody().getStatements().forEach(stmt -> {
                                statementsParser2(stmt, (CtConstructorImpl<?>) constructor, CUTAccessKind);
                            });
                        }
                    });
                }
            });

            testClass.getFields().forEach(field -> {
                if (field.getDefaultExpression() != null) {
                    if (field.getDefaultExpression() instanceof CtInvocationImpl){
                        String methodInvoke = ((CtInvocationImpl<?>) field.getDefaultExpression()).getTarget().toString()
                                + "." + ((CtInvocationImpl<?>) field.getDefaultExpression()).getExecutable().toString();
                        if (!methodInvoke.isEmpty() && CUTAccessKindPrivate.containsKey(methodInvoke)) {
                            testAccessKind.put(Pair.of(methodInvoke, testClass.getQualifiedName()), CUTAccessKindPrivate.get(field.getType().toString()));
                        }
                    } else if (field.getDefaultExpression() instanceof CtConstructorCallImpl) {
                        String constructorInvoked = ((CtConstructorCallImpl<?>)field.getDefaultExpression()).getExecutable().toString();
                        if (!constructorInvoked.isEmpty() && CUTAccessKindPrivate.containsKey(constructorInvoked)) {
                            testAccessKind.put(Pair.of(constructorInvoked, testClass.getQualifiedName()), CUTAccessKindPrivate.get(field.getType().toString()));
                        }
                    } else if (field.getDefaultExpression() instanceof CtNewArrayImpl){
                        String arrayCreation = field.getType().toString().replaceAll("\\[|\\]","");
                        if (!arrayCreation.isEmpty() && CUTAccessKindPrivate.containsKey(arrayCreation)) {
                            testAccessKind.put(Pair.of(arrayCreation, testClass.getQualifiedName()), CUTAccessKindPrivate.get(field.getType().toString()));
                        }
                    } else if (field.getDefaultExpression() instanceof CtBinaryOperatorImpl){
                        CtExpression<?> leftOperand = ((CtBinaryOperatorImpl<?>) field.getDefaultExpression()).getLeftHandOperand();
                        CtExpression<?> rightOperand = ((CtBinaryOperatorImpl<?>) field.getDefaultExpression()).getRightHandOperand();

                        List<String> leftInvokedMethods = ctOperation(leftOperand);
                        leftInvokedMethods.forEach(leftInvokedMethod -> {
                            if (!leftInvokedMethod.isEmpty() && CUTAccessKind.containsKey(leftInvokedMethod)) {
                                testAccessKind.put(Pair.of(leftInvokedMethod, testClass.getQualifiedName()), CUTAccessKind.get(leftInvokedMethod));
                            }
                        });

                        List<String> rightInvokedMethods = ctOperation(rightOperand);
                        rightInvokedMethods.forEach(rightInvokedMethod -> {
                            if (!rightInvokedMethod.isEmpty() && CUTAccessKind.containsKey(rightInvokedMethod)) {
                                testAccessKind.put(Pair.of(rightInvokedMethod, testClass.getQualifiedName()), CUTAccessKind.get(rightInvokedMethod));
                            }
                        });
                    } else if (field.getDefaultExpression() instanceof CtUnaryOperatorImpl){
                        CtExpression<?> operand = ((CtUnaryOperatorImpl<?>) field.getDefaultExpression()).getOperand();
                        List<String> invokedMethods = ctOperation(operand);
                        invokedMethods.forEach(invokedMethod -> {
                            if (!invokedMethod.isEmpty() && CUTAccessKind.containsKey(invokedMethod)) {
                                testAccessKind.put(Pair.of(invokedMethod, testClass.getQualifiedName()), CUTAccessKind.get(invokedMethod));
                            }
                        });
                    }
                } else {
                    if (!field.getType().toString().isEmpty() && CUTAccessKindPrivate.containsKey(field.getType().toString()))
                        testAccessKind.put(Pair.of(field.getType().toString(), testClass.getQualifiedName()), CUTAccessKindPrivate.get(field.getType().toString()));
                }
            });

            testClass.getMethods().forEach(method -> {
                // check if the method is a JUnit Test
                if (method.getAnnotations().stream().map(CtAnnotation::getAnnotationType)
                        .map(CtTypeReference::getSimpleName).noneMatch(name -> name.equals("Ignore"))) {
                    if (method.getBody() != null && method.getBody().getStatements() != null ) {
                        method.getBody().getStatements().forEach(stmt -> {
                            statementsParser(stmt, method, CUTAccessKind);
                        });
                    }
                }
            });
        });
        return this.testAccessKind;
    }

    private Pair<String,Boolean> isReflectionInvocation2(CtInvocation<?> executableRef, CtConstructorImpl<?> constructor) {
        String getDeclaredMethod = "";
        MutableBoolean invokeMethod = new MutableBoolean(false);
        CtTypeReference<?> declaringType = executableRef.getExecutable().getDeclaringType();
        // we assume invocation of method.invoke or method.setAccesible(true) to be calling the private method
        if (declaringType != null && (declaringType.getQualifiedName().equals("java.lang.reflect.AccessibleObject") &&
                executableRef.getExecutable().getSimpleName().equals("setAccessible") &&
                executableRef.getArguments().size() > 0 &&
                executableRef.getArguments().get(0).toString().contains("true")) &&
                executableRef.getParent() instanceof CtElement) {
            invokeMethod.setTrue();
            CtElement ctElement = executableRef;
            while(!(ctElement.getParent() instanceof CtBlockImpl)){
                ctElement = ctElement.getParent();
            }

            for (CtStatement stmt : ((CtBlockImpl<?>) ctElement.getParent()).getStatements()) {
                // we also check if there's a statement in the test case that is calling Method class
                // which is usually calling Method.getDeclaredMethod // Field.getDeclaredField
                if (stmt instanceof CtLocalVariableImpl &&
                        (((CtLocalVariableImpl<?>) stmt).getType().toString().equals("java.lang.reflect.Method") ||
                                ((CtLocalVariableImpl<?>) stmt).getType().toString().equals("java.lang.reflect.Field")) &&
                        !testAccessKind.containsKey(Pair.of(((CtLocalVariableImpl<?>) stmt).getDefaultExpression()
                                .toString(), fullConstructorName(constructor))))
                    return Pair.of(((CtLocalVariableImpl<?>) stmt).getDefaultExpression().toString(),true);
            }
        } // mockito / power mock
        else if (declaringType != null && ((declaringType.getQualifiedName().equals("org.powermock.reflect.Whitebox") &&
                executableRef.getExecutable().getSimpleName().equals("invokeMethod"))) &&
                executableRef.getParent() instanceof CtElement) {
            invokeMethod.setTrue();
            String packageName = executableRef.getExecutable().getParameters().get(0).toString() +
                    "." + executableRef.getArguments().get(1).toString().replace("\"","") + "(";
            for (int x = 2; x < executableRef.getExecutable().getParameters().size(); x++){
                packageName += executableRef.getExecutable().getParameters().get(x).toString();
                if (x < executableRef.getExecutable().getParameters().size() - 1) packageName += ",";
            }

            packageName += ")";
            if (!packageName.isEmpty() && CUTAccessKindPrivate.containsKey(packageName))
                testAccessKind.put(Pair.of(packageName, fullConstructorName(constructor)), CUTAccessKindPrivate.get(packageName));
            else {
                packageName = executableRef.getExecutable().getParameters().get(0).getActualTypeArguments().get(0) +
                        "." + executableRef.getArguments().get(1).toString().replace("\"","") + "(";
                for (int x = 2; x < executableRef.getExecutable().getParameters().size(); x++){
                    packageName += executableRef.getExecutable().getParameters().get(x);
                    if (x < executableRef.getExecutable().getParameters().size() - 1) packageName += ",";
                }
                packageName += ")";
                if (!packageName.isEmpty() && CUTAccessKindPrivate.containsKey(packageName))
                    testAccessKind.put(Pair.of(packageName, fullConstructorName(constructor)), CUTAccessKindPrivate.get(packageName));
            }
        }
        else if (declaringType != null && ((declaringType.getQualifiedName().equals("org.powermock.api.mockito.PowerMockito") &&
                executableRef.getExecutable().getSimpleName().equals("method"))) &&
                executableRef.getParent() instanceof CtElement) {
            invokeMethod.setTrue();
            if (executableRef.getExecutable().getParameters().get(0).toString().startsWith("java.lang.Class")) {
                String packageName = executableRef.getExecutable().getParameters().get(0).getActualTypeArguments().get(0) +
                        "." + executableRef.getArguments().get(1).toString().replace("\"", "") + "(";
                for (int x = 2; x < executableRef.getExecutable().getParameters().size(); x++) {
                    packageName += executableRef.getExecutable().getParameters().get(x).getActualTypeArguments().get(0);
                    if (x < executableRef.getExecutable().getParameters().size() - 1) packageName += ",";
                }
                packageName += ")";
                if (!packageName.isEmpty() && CUTAccessKindPrivate.containsKey(packageName))
                    testAccessKind.put(Pair.of(packageName, fullConstructorName(constructor)), CUTAccessKindPrivate.get(packageName));
            }
        }
        // stubbing
        return Pair.of(getDeclaredMethod, invokeMethod.booleanValue());
    }
    private Pair<String,Boolean> isReflectionInvocation(CtInvocation<?> executableRef, CtMethod<?> ctMethod) {
        String getDeclaredMethod = "";
        MutableBoolean invokeMethod = new MutableBoolean(false);
        CtTypeReference<?> declaringType = executableRef.getExecutable().getDeclaringType();
        // we assume invocation of method.invoke or method.setAccesible(true) to be calling the private method
        if (declaringType != null && (declaringType.getQualifiedName().equals("java.lang.reflect.AccessibleObject") &&
                executableRef.getExecutable().getSimpleName().equals("setAccessible") &&
                executableRef.getArguments().size() > 0 &&
                executableRef.getArguments().get(0).toString().contains("true")) &&
                executableRef.getParent() instanceof CtElement) {
            invokeMethod.setTrue();
            CtElement ctElement = executableRef;
            while(!(ctElement.getParent() instanceof CtBlockImpl)){
                ctElement = ctElement.getParent();
            }

            for (CtStatement stmt : ((CtBlockImpl<?>) ctElement.getParent()).getStatements()) {
                // we also check if there's a statement in the test case that is calling Method class
                // which is usually calling Method.getDeclaredMethod // Field.getDeclaredField
                if (stmt instanceof CtLocalVariableImpl &&
                        (((CtLocalVariableImpl<?>) stmt).getType().toString().equals("java.lang.reflect.Method") ||
                                ((CtLocalVariableImpl<?>) stmt).getType().toString().equals("java.lang.reflect.Field")) &&
                        !testAccessKind.containsKey(Pair.of(((CtLocalVariableImpl<?>) stmt).getDefaultExpression()
                                .toString(), fullMethodName(ctMethod))))
                    return Pair.of(((CtLocalVariableImpl<?>) stmt).getDefaultExpression().toString(),true);
            }
        } // mockito / power mock
        else if (declaringType != null && ((declaringType.getQualifiedName().equals("org.powermock.reflect.Whitebox") &&
                executableRef.getExecutable().getSimpleName().equals("invokeMethod"))) &&
                executableRef.getParent() instanceof CtElement) {
            invokeMethod.setTrue();
            String packageName = executableRef.getExecutable().getParameters().get(0).toString() +
                    "." + executableRef.getArguments().get(1).toString().replace("\"","") + "(";
            for (int x = 2; x < executableRef.getExecutable().getParameters().size(); x++){
                packageName += executableRef.getExecutable().getParameters().get(x).toString();
                if (x < executableRef.getExecutable().getParameters().size() - 1) packageName += ",";
            }

            packageName += ")";
            if (!packageName.isEmpty() && CUTAccessKindPrivate.containsKey(packageName))
                testAccessKind.put(Pair.of(packageName, fullMethodName(ctMethod)), CUTAccessKindPrivate.get(packageName));
            else {
                packageName = executableRef.getExecutable().getParameters().get(0).getActualTypeArguments().get(0) +
                        "." + executableRef.getArguments().get(1).toString().replace("\"","") + "(";
                for (int x = 2; x < executableRef.getExecutable().getParameters().size(); x++){
                    packageName += executableRef.getExecutable().getParameters().get(x);
                    if (x < executableRef.getExecutable().getParameters().size() - 1) packageName += ",";
                }
                packageName += ")";
                if (!packageName.isEmpty() && CUTAccessKindPrivate.containsKey(packageName))
                    testAccessKind.put(Pair.of(packageName, fullMethodName(ctMethod)), CUTAccessKindPrivate.get(packageName));
            }
        }
        else if (declaringType != null && ((declaringType.getQualifiedName().equals("org.powermock.api.mockito.PowerMockito") &&
                executableRef.getExecutable().getSimpleName().equals("method"))) &&
                executableRef.getParent() instanceof CtElement) {
            invokeMethod.setTrue();
            if (executableRef.getExecutable().getParameters().get(0).toString().startsWith("java.lang.Class")) {
                String packageName = executableRef.getExecutable().getParameters().get(0).getActualTypeArguments().get(0) +
                        "." + executableRef.getArguments().get(1).toString().replace("\"", "") + "(";
                for (int x = 2; x < executableRef.getExecutable().getParameters().size(); x++) {
                    packageName += executableRef.getExecutable().getParameters().get(x).getActualTypeArguments().get(0);
                    if (x < executableRef.getExecutable().getParameters().size() - 1) packageName += ",";
                }
                packageName += ")";
                if (!packageName.isEmpty() && CUTAccessKindPrivate.containsKey(packageName))
                    testAccessKind.put(Pair.of(packageName, fullMethodName(ctMethod)), CUTAccessKindPrivate.get(packageName));
            }
        } // easy mock can't mock private methods


        // stubbing
        return Pair.of(getDeclaredMethod, invokeMethod.booleanValue());
    }

    public static boolean isDotOutsideParentheses(String input) {
        int parenthesesCount = 0;
        for (char c : input.toCharArray()) {
            if (c == '(') {
                parenthesesCount++;
            } else if (c == ')') {
                parenthesesCount--;
            } else if (c == '.' && parenthesesCount == 0) {
                return true;
            }
        }
        return false;
    }

    private void statementsParser2(CtStatement stmt, CtConstructorImpl<?> constructor, Map<String, ModifierKind> CUTAccessKind) {
        String fullInvokedName = "";
        String fullMethodName = fullConstructorName(constructor);
        if (stmt instanceof CtLocalVariableImpl) {
            if (((CtLocalVariableImpl<?>) stmt).getDefaultExpression()
                    instanceof CtInvocationImpl) {
                CtInvocationImpl ctInvocation =
                        (CtInvocationImpl) ((CtLocalVariableImpl<?>) stmt).getDefaultExpression();
                fullInvokedName = ctInvocation.getExecutable().getDeclaringType() + "."
                        + ctInvocation.getExecutable().toString();
            }
            else if (((CtLocalVariableImpl<?>) stmt).getDefaultExpression()
                    instanceof CtConstructorCallImpl) {

                CtConstructorCallImpl ctConstructor =
                        (CtConstructorCallImpl) ((CtLocalVariableImpl<?>) stmt)
                                .getDefaultExpression();
                fullInvokedName = ctConstructor.getExecutable().toString();

            }
            else if (((CtLocalVariableImpl<?>) stmt).getDefaultExpression()
                    instanceof CtExecutableReferenceExpressionImpl) {

                CtExecutableReferenceExpressionImpl ctExecutable =
                        (CtExecutableReferenceExpressionImpl) ((CtLocalVariableImpl<?>) stmt)
                                .getDefaultExpression();
                fullInvokedName = ctExecutable.getExecutable().getDeclaringType() + "."
                        + ctExecutable.getExecutable().toString();

            }
            else {

                // pass it to ctOperationParser
                if (((CtLocalVariableImpl<?>) stmt).getDefaultExpression() != null) {
                    ctOperationParser2(((CtLocalVariableImpl<?>) stmt).getDefaultExpression(), constructor, CUTAccessKind);
                }
            }

            ctOperationParser2(((CtLocalVariableImpl<?>) stmt).getAssignment(), constructor, CUTAccessKind);
            ctOperationParser2(((CtLocalVariableImpl<?>) stmt).getDefaultExpression(), constructor, CUTAccessKind);
            if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
                testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
        }
        else if (stmt instanceof CtInvocationImpl) {

            // TODO: Update this part to also include checking for invocation of void method
            if (((CtInvocationImpl<?>) stmt).getExecutable().getDeclaringType() != null) {
                // TODO: Might need to add more in here in the future
                if (isReflectionInvocation2(((CtInvocation<?>) stmt), constructor).getRight()) {
                    String getDeclaredMethod = isReflectionInvocation2(((CtInvocation<?>) stmt), constructor).getLeft();
                    if (getDeclaredMethod.length() > 0)
                        testAccessKind.put(Pair.of(getDeclaredMethod.toString(), fullMethodName), ModifierKind.PRIVATE);
                } else {
                    // for void CUT method
                    if (stmt instanceof CtInvocationImpl) {
                        String fullInvoked = ((CtInvocationImpl<?>) stmt).getExecutable().getDeclaringType()
                                + "." + ((CtInvocationImpl<?>) stmt).getExecutable().toString();
                        if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                            testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                    }
                }
            }
            else if (((CtInvocationImpl<?>) stmt).getTarget().getType() != null) {
                if (stmt instanceof CtInvocationImpl) {
                    String fullInvoked = ((CtInvocationImpl<?>) stmt).getTarget().getType().toString() + "." + ((CtInvocationImpl<?>) stmt).getExecutable().toString();
                    if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                        testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                }
            }
            else {
                CtExpressionImpl ctExpression = (CtExpressionImpl) ((CtInvocationImpl<?>) stmt).getTarget();

                while (ctExpression.getType() == null) {
                    if (ctExpression instanceof CtTargetedExpressionImpl && ((CtTargetedExpressionImpl<?, ?>) ctExpression).getTarget().toString() != "")
                        ctExpression = (CtExpressionImpl) ((CtTargetedExpressionImpl<?, ?>) ctExpression).getTarget();
                }

                if (ctExpression instanceof CtInvocationImpl) {
                    String fullInvoked = ctExpression.getType().toString() + "." + ((CtInvocationImpl) ctExpression).getExecutable().toString();

                    if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                        testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                }
            }
        }
        else if (stmt instanceof CtTryImpl) {
            // try body
            statementsParser2(((CtTryImpl) stmt).getBody(), constructor, CUTAccessKind);
            ((CtTryImpl) stmt).getCatchers().forEach(ctCatch -> {
                statementsParser2(((CtTryImpl) stmt).getBody(), constructor, CUTAccessKind);
            });
        }
        else if (stmt instanceof CtIfImpl) {
            // will check again (a bit complicated to parse the condition)
            CtExpression<?> ctExpression = ((CtIfImpl) stmt).getCondition();
//            if (ctExpression instanceof CtUnaryOperatorImpl)
//                statementsParser(((CtUnaryOperatorImpl<?>) ctExpression).getOperand(), method, CUTAccessKind);
            statementsParser2(((CtIfImpl) stmt).getThenStatement(), constructor, CUTAccessKind);
        }
        else if (stmt instanceof CtAssignmentImpl) {
            ctOperationParser2(((CtAssignmentImpl<?, ?>) stmt).getAssignment(), constructor, CUTAccessKind);
        }
        else if (stmt instanceof CtBlockImpl) {
            ((CtBlockImpl<?>) stmt).getStatements().forEach(statement -> {
                statementsParser2(statement, constructor, CUTAccessKind);
            });
        }
        else if (stmt instanceof CtForImpl) {
            ((CtForImpl) stmt).getForUpdate().forEach(update -> {
                statementsParser2(update, constructor, CUTAccessKind);
            });
            ((CtForImpl) stmt).getForInit().forEach(init -> {
                statementsParser2(init, constructor, CUTAccessKind);
            });
            ctOperationParser2(((CtForImpl) stmt).getExpression(), constructor, CUTAccessKind);
            statementsParser2(((CtForImpl) stmt).getBody(), constructor, CUTAccessKind);
        }
        else if (stmt instanceof CtForEachImpl) {
            ctOperationParser2(((CtForEachImpl) stmt).getExpression(), constructor, CUTAccessKind);
            statementsParser2(((CtForEachImpl) stmt).getBody(), constructor, CUTAccessKind);
            String variable = ((CtForEachImpl) stmt).getVariable().getType().toString();
            if (!variable.isEmpty() && CUTAccessKind.containsKey(variable))
                testAccessKind.put(Pair.of(variable, fullMethodName), CUTAccessKind.get(variable));
        }
        else if (stmt instanceof CtCommentImpl) {
            // ignore
        }
        else if (stmt instanceof CtNewClassImpl) {
            // TODO
//            System.out.println("8: " + stmt.toString());
        }
        else if (stmt instanceof CtConstructorCallImpl) {
            String fullInvoked = ((CtConstructorCallImpl<?>) stmt).getExecutable().toString();
            if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
        }
        else if (stmt instanceof CtClassImpl) {
            //ignore CtClassImpl for now
//            System.out.println("10: " + stmt.getClass());
        }
        else if (stmt instanceof CtWhileImpl) {
            statementsParser2(((CtWhileImpl) stmt).getBody(), constructor, CUTAccessKind);
        }
        else if (stmt instanceof CtUnaryOperatorImpl) {
            // remove
        }
        else if (stmt instanceof CtDoImpl) {
            // read the do statement
            statementsParser2(((CtDoImpl) stmt).getBody(), constructor, CUTAccessKind);
        }
    }

    private void statementsParser(CtStatement stmt, CtMethod<?> method, Map<String, ModifierKind> CUTAccessKind) {
        String fullInvokedName = "";
        String fullMethodName = fullMethodName(method);
        if (stmt instanceof CtLocalVariableImpl) {
            if (((CtLocalVariableImpl<?>) stmt).getDefaultExpression()
                    instanceof CtInvocationImpl) {
                CtInvocationImpl ctInvocation =
                        (CtInvocationImpl) ((CtLocalVariableImpl<?>) stmt).getDefaultExpression();
                fullInvokedName = ctInvocation.getExecutable().getDeclaringType() + "."
                        + ctInvocation.getExecutable().toString();
            }
            else if (((CtLocalVariableImpl<?>) stmt).getDefaultExpression()
                    instanceof CtConstructorCallImpl) {

                CtConstructorCallImpl ctConstructor =
                        (CtConstructorCallImpl) ((CtLocalVariableImpl<?>) stmt)
                                .getDefaultExpression();
                fullInvokedName = ctConstructor.getExecutable().toString();

            }
            else if (((CtLocalVariableImpl<?>) stmt).getDefaultExpression()
                    instanceof CtExecutableReferenceExpressionImpl) {

                CtExecutableReferenceExpressionImpl ctExecutable =
                        (CtExecutableReferenceExpressionImpl) ((CtLocalVariableImpl<?>) stmt)
                                .getDefaultExpression();
                fullInvokedName = ctExecutable.getExecutable().getDeclaringType() + "."
                        + ctExecutable.getExecutable().toString();

            }
            else {

                // pass it to ctOperationParser
                if (((CtLocalVariableImpl<?>) stmt).getDefaultExpression() != null) {
                    ctOperationParser(((CtLocalVariableImpl<?>) stmt).getDefaultExpression(), method, CUTAccessKind);
                }
            }

            ctOperationParser(((CtLocalVariableImpl<?>) stmt).getAssignment(), method, CUTAccessKind);
            ctOperationParser(((CtLocalVariableImpl<?>) stmt).getDefaultExpression(), method, CUTAccessKind);
            if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
                testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
        }
        else if (stmt instanceof CtInvocationImpl) {

            // TODO: Update this part to also include checking for invocation of void method
            if (((CtInvocationImpl<?>) stmt).getExecutable().getDeclaringType() != null) {
                // TODO: Might need to add more in here in the future
                boolean isAssert = ((CtInvocationImpl<?>) stmt).getExecutable().getDeclaringType().toString().contains("Assert") ? true : false;
                boolean isMock = ((CtInvocationImpl<?>) stmt).getExecutable().getDeclaringType().toString().contains("org.easymock.EasyMock") ? true : false;
                boolean isExpect = ((CtInvocationImpl<?>) stmt).getExecutable().getDeclaringType().toString().contains("expect") ? true : false;
                boolean isReflection = ((CtInvocationImpl) stmt).getExecutable().getDeclaringType().toString().contains("java.lang.reflect") ? true : false;
                if (isReflectionInvocation(((CtInvocation<?>) stmt), method).getRight()) {
                    String getDeclaredMethod = isReflectionInvocation(((CtInvocation<?>) stmt), method).getLeft();
                    if (getDeclaredMethod.length() > 0)
                        testAccessKind.put(Pair.of(getDeclaredMethod.toString(), fullMethodName), ModifierKind.PRIVATE);
                } else if (isAssert || isMock || isExpect) {
                    // for assertion
                    ((CtInvocationImpl<?>) stmt).getArguments().forEach(argument -> {
                        // if a method is being called inside the assertion
                        if (argument instanceof CtInvocationImpl) {
                            if (((CtInvocationImpl<?>) argument).getArguments().size() == ((CtInvocationImpl<?>) argument).getExecutable().getParameters().size()) {
                                String fullInvoked = ((CtInvocationImpl<?>) argument).getExecutable().getDeclaringType()
                                        + "." + ((CtInvocationImpl<?>) argument).getExecutable().toString();

                                if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                                    testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                                ctOperationParser(argument, method, CUTAccessKind);
                            }
                        } else if (argument instanceof CtLambdaImpl) {
                            CtExpression lambdaImpl = ((CtLambdaImpl<?>) argument).getExpression();
                            if (lambdaImpl != null) {
                                if (lambdaImpl instanceof CtInvocationImpl) {
                                    String fullInvoked = ((CtInvocationImpl<?>) lambdaImpl).getExecutable().getDeclaringType()
                                            + "." + ((CtInvocationImpl<?>) lambdaImpl).getExecutable().toString();

                                    if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                                        testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                                } else if (lambdaImpl instanceof CtConstructorCallImpl) {
                                    String fullInvoked = ((CtConstructorCallImpl<?>) lambdaImpl).getExecutable().toString();

                                    if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                                        testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                                }
                            }
                            ctOperationParser(argument, method, CUTAccessKind);
                        } else if (argument instanceof CtConstructorCallImpl) {
                            String fullInvoked = ((CtConstructorCallImpl<?>) argument).getExecutable().toString();
                            if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                                testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                            ctOperationParser(argument, method, CUTAccessKind);
                        } else if (argument instanceof CtFieldReadImpl) {
                            String fullInvoked = ((CtFieldReadImpl<?>) argument).getVariable().toString();
                            if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                                testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                            ctOperationParser(argument, method, CUTAccessKind);
                        } else if (argument instanceof CtVariableReadImpl) {
                            // Ignore for now if reading a variable is part of assertion.
                            // To read a variable, you need to instantiate it first. It'll be a duplicate work if it's being check again.
                            ctOperationParser(argument, method, CUTAccessKind);
                        } else if (argument instanceof CtBinaryOperatorImpl) {

                            CtExpression<?> leftOperand = ((CtBinaryOperatorImpl<?>) argument).getLeftHandOperand();
                            CtExpression<?> rightOperand = ((CtBinaryOperatorImpl<?>) argument).getRightHandOperand();

                            List<String> leftInvokedMethods = ctOperation(leftOperand);
                            leftInvokedMethods.forEach(leftInvokedMethod -> {
                                if (!leftInvokedMethod.isEmpty() && CUTAccessKind.containsKey(leftInvokedMethod))
                                    testAccessKind.put(Pair.of(leftInvokedMethod, fullMethodName), CUTAccessKind.get(leftInvokedMethod));
                            });

                            List<String> rightInvokedMethods = ctOperation(rightOperand);
                            rightInvokedMethods.forEach(rightInvokedMethod -> {
                                if (!rightInvokedMethod.isEmpty() && CUTAccessKind.containsKey(rightInvokedMethod))
                                    testAccessKind.put(Pair.of(rightInvokedMethod, fullMethodName), CUTAccessKind.get(rightInvokedMethod));
                            });
                            ctOperationParser(argument, method, CUTAccessKind);
                        } else if (argument instanceof CtNewArrayImpl) {
                            // array
                            String fullInvoked = ((CtNewArrayImpl) argument).getType().toString().replace("[","").replace("]","");
                            if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                                testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                            // elements from the array
                            ((CtNewArrayImpl<?>) argument).getElements().forEach(ctElements -> {
                                List<String> ctElms = ctOperation(ctElements);
                                ctElms.forEach(ctElm -> {
                                    if (!ctElm.isEmpty() && CUTAccessKind.containsKey(ctElm))
                                        testAccessKind.put(Pair.of(ctElm, fullMethodName), CUTAccessKind.get(ctElm));
                                });
                            });
                            ctOperationParser(argument, method, CUTAccessKind);
                        } else if (argument instanceof CtExecutableReferenceExpressionImpl) {
                            String fullInvoked = ((CtExecutableReferenceExpressionImpl) argument).getTarget() + "." + ((CtExecutableReferenceExpressionImpl) argument).getExecutable();
                            if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                                testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                            ctOperationParser(argument, method, CUTAccessKind);
                        } else if (argument instanceof CtTypeAccessImpl){
                            recursiveInvokedOperation.add(argument.toString());
                            ctOperationParser(argument, method, CUTAccessKind);
                            // the class. Ignore it for now
                        } else if (argument instanceof CtLiteralImpl || argument instanceof CtUnaryOperatorImpl || argument instanceof CtArrayReadImpl) {
                        } else {
                            System.out.println(argument);
                        }
                    });
                } else {
                    // for void CUT method
                    if (stmt instanceof CtInvocationImpl) {
                        String fullInvoked = ((CtInvocationImpl<?>) stmt).getExecutable().getDeclaringType()
                                + "." + ((CtInvocationImpl<?>) stmt).getExecutable().toString();
                        if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                            testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                    }
                }
            }
            else if (((CtInvocationImpl<?>) stmt).getTarget().getType() != null) {
                if (stmt instanceof CtInvocationImpl) {
                    String fullInvoked = ((CtInvocationImpl<?>) stmt).getTarget().getType().toString() + "." + ((CtInvocationImpl<?>) stmt).getExecutable().toString();
                    if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                        testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                }
            }
            else {
                CtExpressionImpl ctExpression = (CtExpressionImpl) ((CtInvocationImpl<?>) stmt).getTarget();

                while (ctExpression.getType() == null) {
                    if (ctExpression instanceof CtTargetedExpressionImpl && ((CtTargetedExpressionImpl<?, ?>) ctExpression).getTarget().toString() != "")
                        ctExpression = (CtExpressionImpl) ((CtTargetedExpressionImpl<?, ?>) ctExpression).getTarget();
                }

                if (ctExpression instanceof CtInvocationImpl) {
                    String fullInvoked = ctExpression.getType().toString() + "." + ((CtInvocationImpl) ctExpression).getExecutable().toString();

                    if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                        testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
                }
            }
        }
        else if (stmt instanceof CtTryImpl) {
            // try body
            statementsParser(((CtTryImpl) stmt).getBody(), method, CUTAccessKind);
            ((CtTryImpl) stmt).getCatchers().forEach(ctCatch -> {
                statementsParser(((CtTryImpl) stmt).getBody(), method, CUTAccessKind);
            });
        }
        else if (stmt instanceof CtIfImpl) {
            // will check again (a bit complicated to parse the condition)
            CtExpression<?> ctExpression = ((CtIfImpl) stmt).getCondition();
//            if (ctExpression instanceof CtUnaryOperatorImpl)
//                statementsParser(((CtUnaryOperatorImpl<?>) ctExpression).getOperand(), method, CUTAccessKind);
            statementsParser(((CtIfImpl) stmt).getThenStatement(), method, CUTAccessKind);
        }
        else if (stmt instanceof CtAssignmentImpl) {
            ctOperationParser(((CtAssignmentImpl<?, ?>) stmt).getAssignment(), method, CUTAccessKind);
        }
        else if (stmt instanceof CtBlockImpl) {
            ((CtBlockImpl<?>) stmt).getStatements().forEach(statement -> {
                statementsParser(statement, method, CUTAccessKind);
            });
        }
        else if (stmt instanceof CtForImpl) {
            ((CtForImpl) stmt).getForUpdate().forEach(update -> {
                statementsParser(update, method, CUTAccessKind);
            });
            ((CtForImpl) stmt).getForInit().forEach(init -> {
                statementsParser(init, method, CUTAccessKind);
            });
            ctOperationParser(((CtForImpl) stmt).getExpression(), method, CUTAccessKind);
            statementsParser(((CtForImpl) stmt).getBody(), method, CUTAccessKind);
        }
        else if (stmt instanceof CtForEachImpl) {
            ctOperationParser(((CtForEachImpl) stmt).getExpression(), method, CUTAccessKind);
            statementsParser(((CtForEachImpl) stmt).getBody(), method, CUTAccessKind);
            String variable = ((CtForEachImpl) stmt).getVariable().getType().toString();
            if (!variable.isEmpty() && CUTAccessKind.containsKey(variable))
                testAccessKind.put(Pair.of(variable, fullMethodName), CUTAccessKind.get(variable));
        }
        else if (stmt instanceof CtCommentImpl) {
            // ignore
        }
        else if (stmt instanceof CtNewClassImpl) {
            // TODO
//            System.out.println("8: " + stmt.toString());
        }
        else if (stmt instanceof CtConstructorCallImpl) {
            String fullInvoked = ((CtConstructorCallImpl<?>) stmt).getExecutable().toString();
            if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
        }
        else if (stmt instanceof CtClassImpl) {
            //ignore CtClassImpl for now
//            System.out.println("10: " + stmt.getClass());
        }
        else if (stmt instanceof CtWhileImpl) {
            statementsParser(((CtWhileImpl) stmt).getBody(), method, CUTAccessKind);
        }
        else if (stmt instanceof CtUnaryOperatorImpl) {
            // remove
        }
        else if (stmt instanceof CtDoImpl) {
            // read the do statement
            statementsParser(((CtDoImpl) stmt).getBody(), method, CUTAccessKind);
        }
    }

    private void ctOperationParser2(CtExpression<?> expression,CtConstructorImpl<?> constructor, Map<String, ModifierKind> CUTAccessKind) {
        String fullInvokedName = "";
        String fullMethodName = fullConstructorName(constructor);
        if (expression instanceof CtFieldReadImpl) {
            fullInvokedName = ((CtFieldReadImpl<?>) expression).getVariable().toString();
            if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
                testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
        } else if (expression instanceof CtInvocationImpl) {
            if (isReflectionInvocation2(((CtInvocation<?>) expression), constructor).getRight()) {
                String getDeclaredMethod = isReflectionInvocation2(((CtInvocation<?>) expression), constructor).getLeft();
                if (getDeclaredMethod.length() > 0)
                    testAccessKind.put(Pair.of(getDeclaredMethod.toString(), fullMethodName), ModifierKind.PRIVATE);
            } else {
                fullInvokedName = ((CtInvocationImpl<?>) expression).getExecutable().getDeclaringType()
                        + "." + ((CtInvocationImpl<?>) expression).getExecutable().toString();

                if (((CtInvocationImpl<?>) expression).getTarget() != null)
                    ctOperationParser2(((CtInvocationImpl<?>) expression).getTarget(), constructor, CUTAccessKind);

                if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
                    testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
            }
        } else if (expression instanceof CtConstructorCallImpl) {
            fullInvokedName = ((CtConstructorCallImpl<?>) expression).getExecutable().getDeclaringType() + "."
                    + ((CtConstructorCallImpl<?>) expression).getExecutable().toString();

            if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
                testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
        } else if (expression instanceof CtAssignmentImpl) {
            ctOperationParser2(((CtAssignmentImpl<?, ?>) expression).getAssignment(), constructor, CUTAccessKind);
        } else if (expression instanceof CtNewArrayImpl) {
            String fullInvoked = ((CtNewArrayImpl) expression).getType().toString().replace("[","").replace("]","");
            if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
            // elements from the array
            ((CtNewArrayImpl<?>) expression).getElements().forEach(ctElements -> {
                List<String> ctElms = ctOperation(ctElements);
                ctElms.forEach(ctElm -> {
                    if (!ctElm.isEmpty() && CUTAccessKind.containsKey(ctElm))
                        testAccessKind.put(Pair.of(ctElm, fullMethodName), CUTAccessKind.get(ctElm));
                });
            });
        } else if (expression instanceof CtBinaryOperatorImpl) {
            CtExpression<?> leftOperand = ((CtBinaryOperatorImpl<?>) expression).getLeftHandOperand();
            CtExpression<?> rightOperand = ((CtBinaryOperatorImpl<?>) expression).getRightHandOperand();
            ctOperationParser2(leftOperand, constructor, CUTAccessKind);
            ctOperationParser2(rightOperand, constructor, CUTAccessKind);
        } else if (expression instanceof CtLiteralImpl ||
                expression instanceof CtArrayReadImpl ||
                expression instanceof CtThisAccessImpl ||
                expression instanceof CtUnaryOperatorImpl){
            // nothing important here
//            System.out.println("HELLO WORLD: " + expression.getClass() + ": " + expression);
        } else if (expression instanceof CtVariableReadImpl) {
            if (expression.getType() != null) {
                if (expression.getParent() instanceof CtInvocationImpl && ((CtInvocationImpl) expression.getParent()).getExecutable() != null) {
                    fullInvokedName = expression.getType().toString() + "." + ((CtInvocationImpl) expression.getParent()).getExecutable().toString();
//                    System.out.println(expression + ": CtVariableReadImpl");
                    if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName)) {
                        testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
                    }
                }
            }
        } else if (expression instanceof CtTypeAccessImpl) {
            //ignore
//            fullInvokedName = ((CtTypeAccessImpl) expression).getAccessedType().toString();
//            if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
//                testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
        } else if (expression instanceof CtLambdaImpl) {
            statementsParser2(((CtLambdaImpl<?>) expression).getBody(), constructor, CUTAccessKind);
        } else if (expression instanceof CtConditionalImpl) {
            ctOperationParser2(((CtConditionalImpl<?>) expression).getCondition(), constructor, CUTAccessKind);
            ctOperationParser2(((CtConditionalImpl<?>) expression).getThenExpression(), constructor, CUTAccessKind);
            ctOperationParser2(((CtConditionalImpl<?>) expression).getElseExpression(), constructor, CUTAccessKind);
        } else {
//            System.out.println("HELLO WORLD: " + expression.getClass() + ": " + expression);
        }
    }

    private void ctOperationParser(CtExpression<?> expression,CtMethod<?> method, Map<String, ModifierKind> CUTAccessKind) {
        String fullInvokedName = "";
        String fullMethodName = fullMethodName(method);
        if (expression instanceof CtFieldReadImpl) {
            fullInvokedName = ((CtFieldReadImpl<?>) expression).getVariable().toString();
            if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
                testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
        } else if (expression instanceof CtInvocationImpl) {
            if (isReflectionInvocation(((CtInvocation<?>) expression), method).getRight()) {
                String getDeclaredMethod = isReflectionInvocation(((CtInvocation<?>) expression), method).getLeft();
                if (getDeclaredMethod.length() > 0)
                    testAccessKind.put(Pair.of(getDeclaredMethod.toString(), fullMethodName), ModifierKind.PRIVATE);
            } else {
                fullInvokedName = ((CtInvocationImpl<?>) expression).getExecutable().getDeclaringType()
                        + "." + ((CtInvocationImpl<?>) expression).getExecutable().toString();

                if (((CtInvocationImpl<?>) expression).getTarget() != null)
                    ctOperationParser(((CtInvocationImpl<?>) expression).getTarget(), method, CUTAccessKind);

                if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
                    testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
            }
        } else if (expression instanceof CtConstructorCallImpl) {
            fullInvokedName = ((CtConstructorCallImpl<?>) expression).getExecutable().getDeclaringType() + "."
                    + ((CtConstructorCallImpl<?>) expression).getExecutable().toString();

            if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
                testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
        } else if (expression instanceof CtAssignmentImpl) {
            ctOperationParser(((CtAssignmentImpl<?, ?>) expression).getAssignment(), method, CUTAccessKind);
        } else if (expression instanceof CtNewArrayImpl) {
            String fullInvoked = ((CtNewArrayImpl) expression).getType().toString().replace("[","").replace("]","");
            if (!fullInvoked.isEmpty() && CUTAccessKind.containsKey(fullInvoked))
                testAccessKind.put(Pair.of(fullInvoked, fullMethodName), CUTAccessKind.get(fullInvoked));
            // elements from the array
            ((CtNewArrayImpl<?>) expression).getElements().forEach(ctElements -> {
                List<String> ctElms = ctOperation(ctElements);
                ctElms.forEach(ctElm -> {
                    if (!ctElm.isEmpty() && CUTAccessKind.containsKey(ctElm))
                        testAccessKind.put(Pair.of(ctElm, fullMethodName), CUTAccessKind.get(ctElm));
                });
            });
        } else if (expression instanceof CtBinaryOperatorImpl) {
            CtExpression<?> leftOperand = ((CtBinaryOperatorImpl<?>) expression).getLeftHandOperand();
            CtExpression<?> rightOperand = ((CtBinaryOperatorImpl<?>) expression).getRightHandOperand();
            ctOperationParser(leftOperand, method, CUTAccessKind);
            ctOperationParser(rightOperand, method, CUTAccessKind);
        } else if (expression instanceof CtLiteralImpl ||
                expression instanceof CtArrayReadImpl ||
                expression instanceof CtThisAccessImpl ||
                expression instanceof CtUnaryOperatorImpl){
            // nothing important here
//            System.out.println("HELLO WORLD: " + expression.getClass() + ": " + expression);
        } else if (expression instanceof CtVariableReadImpl) {
            if (expression.getType() != null) {
                if (expression.getParent() instanceof CtInvocationImpl && ((CtInvocationImpl) expression.getParent()).getExecutable() != null) {
                    fullInvokedName = expression.getType().toString() + "." + ((CtInvocationImpl) expression.getParent()).getExecutable().toString();
//                    System.out.println(expression + ": CtVariableReadImpl");
                    if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName)) {
                        testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
                    }
                }
            }
        } else if (expression instanceof CtTypeAccessImpl) {
            //ignore
//            fullInvokedName = ((CtTypeAccessImpl) expression).getAccessedType().toString();
//            if (!fullInvokedName.isEmpty() && CUTAccessKind.containsKey(fullInvokedName))
//                testAccessKind.put(Pair.of(fullInvokedName, fullMethodName), CUTAccessKind.get(fullInvokedName));
        } else if (expression instanceof CtLambdaImpl) {
            statementsParser(((CtLambdaImpl<?>) expression).getBody(), method, CUTAccessKind);
        } else if (expression instanceof CtConditionalImpl) {
            ctOperationParser(((CtConditionalImpl<?>) expression).getCondition(), method, CUTAccessKind);
            ctOperationParser(((CtConditionalImpl<?>) expression).getThenExpression(), method, CUTAccessKind);
            ctOperationParser(((CtConditionalImpl<?>) expression).getElseExpression(), method, CUTAccessKind);
        } else {
//            System.out.println("HELLO WORLD: " + expression.getClass() + ": " + expression);
        }
    }


    List<String> recursiveInvokedOperation = new ArrayList<>();
    private List<String> ctOperation(CtExpression<?> operand) {
        if (operand instanceof CtInvocationImpl){
            recursiveInvokedOperation.add(((CtInvocationImpl<?>) operand).getExecutable().getDeclaringType()
                    + "." + ((CtInvocationImpl<?>) operand).getExecutable().toString());

            if (((CtInvocationImpl<?>) operand).getTarget() != null) {
                List<String> recursiveInvoked = ctOperation(((CtInvocationImpl<?>) operand).getTarget());
                recursiveInvokedOperation.addAll(recursiveInvoked); // Add the recursive results to the main list
            }
        } else if (operand instanceof CtConstructorCallImpl){
            recursiveInvokedOperation.add(((CtConstructorCallImpl<?>) operand).getExecutable().toString());
        } else if (operand instanceof CtBinaryOperatorImpl){
            CtExpression<?> leftOperand = ((CtBinaryOperatorImpl<?>) operand).getLeftHandOperand();
            CtExpression<?> rightOperand = ((CtBinaryOperatorImpl<?>) operand).getRightHandOperand();
            ctOperation(leftOperand);
            ctOperation(rightOperand);
        } else if (operand instanceof CtFieldReadImpl){
            recursiveInvokedOperation.add(((CtFieldReadImpl<?>) operand).getVariable().toString());
        } else if (operand instanceof CtTypeAccessImpl){
            recursiveInvokedOperation.add(operand.toString());
            // the class. Ignore it for now
        } else if (operand instanceof CtLiteralImpl ||
                operand instanceof CtVariableReadImpl ||
                operand instanceof CtArrayReadImpl ||
                operand instanceof CtThisAccessImpl ||
                operand instanceof CtUnaryOperatorImpl){
        }
        List<String> tempList = new ArrayList<>(recursiveInvokedOperation);
        recursiveInvokedOperation.clear();
        return tempList;
    }

    private String fullMethodName(CtMethod<?> method) {
        if (method.getDeclaringType().getPackage() != null) {
            return method.getDeclaringType().getPackage().getQualifiedName() + "."
                    + method.getDeclaringType().getSimpleName() + "."
                    + method.getSimpleName() + getMethodParameters(method);
        }
        return method.getSimpleName();
    }

    private String fullConstructorName(CtConstructor<?> constructor) {
        if (constructor.getDeclaringType().getPackage() != null) {
            return constructor.getDeclaringType().getQualifiedName() + getConstructorParameters(constructor);
        }
        return constructor.getSimpleName();
    }

    private String getParametersAsString(List<CtParameter<?>> parameters) {
        return "(" + parameters.stream()
                .map(ctParameter -> ctParameter.getType().toString())
                .collect(Collectors.joining(",")) + ")";
    }

    private String getConstructorParameters(CtConstructor<?> constructor) {
        return getParametersAsString(constructor.getParameters());
    }
    private String getMethodParameters(CtMethod<?> method) {
        return getParametersAsString(method.getParameters());
    }

    // using Spoon to analyse source code (DONE)
    private List<CtType<?>> spoonLauncher(String path, MavenLauncher.SOURCE_TYPE testSource){
        MavenLauncher CUTLauncher = new MavenLauncher(path,testSource);
//        spoon.Launcher CUTLauncher = new spoon.Launcher();
//        CUTLauncher.addInputResource(path);
//        System.out.println(CUTLauncher.createCompiler().compile(SpoonModelBuilder.InputType.FILES));
        CUTLauncher.getEnvironment().setSourceClasspath(getPathToJunit(path, testSource));
        CtModel CUTModel = CUTLauncher.buildModel();

        Set<CtPackage> ctPackages = CUTModel.getRootPackage().getPackages();
//        boolean tFlag = true;
//        while (tFlag) {
////            ctPackages.forEach(ctPackage -> {
////
////            }
//            if (((CtPackageImpl) ctPackages.toArray()[0]).getPackages().size() == 1)
//                ctPackages = ((CtPackageImpl) ctPackages.toArray()[0]).getPackages();
//            else tFlag = false;
//        }

        packageName = ctPackages;
        ArrayList<CtType<?>> al = new ArrayList();
        CUTModel.getElements(new TypeFilter<>(CtClass.class)).forEach(ctClass -> al.add(ctClass));
        CUTModel.getElements(new TypeFilter<>(CtInterface.class)).forEach(ctInterface -> al.add(ctInterface));
        return al;
    }

    private static String[] getPathToJunit(String path, MavenLauncher.SOURCE_TYPE testSource) {
        File file = new File(path + testSource + ".cp");
        try {
            final String cmd;
            if (System.getProperty("os.name").startsWith("Windows")) {
                cmd = "cmd /C mvn dependency:build-classpath -Dmdep.outputFile=" + path + testSource + ".cp";
            } else {
                cmd = "mvn dependency:build-classpath -Dmdep.outputFile=" + path + testSource + ".cp";
            }
            Process p = Runtime.getRuntime().exec(cmd);
            new Thread() {
                @Override
                public void run() {
                    while (p.isAlive()) {
//                        try {
//                            System.out.print((char) p.getInputStream().read());
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }
                }
            }.start();
            p.waitFor();
            final String classpath;
            try (BufferedReader buffer = new BufferedReader(new FileReader(file))) {
                classpath = buffer.lines().collect(Collectors.joining(System.getProperty("path.separator")));
            }
//            file.delete();
            return classpath.split(System.getProperty("path.separator"));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


    private Map<Pair<String,String>,ModifierKind> getNestedClass(CtType<?> ctType) {
        Set<CtTypeReference> CUTTypeReferences = new HashSet<>();
        Map<Pair<String,String>,ModifierKind> NestedCUTAK = new HashMap<>();
        if (ctType instanceof CtClass){
            NestedCUTAK.put(Pair.of(ctType.getQualifiedName().replace("$","."),ctType.getClass().getSimpleName()), ctType.getVisibility());
            ((CtClass<?>) ctType).getConstructors().forEach(ctConstructor -> {
                if(ctConstructor.getBody() != null)
                    NestedCUTAK.put(Pair.of(fullConstructorName(ctConstructor),ctConstructor.getClass().getSimpleName()), ctConstructor.getVisibility());
            });

            ctType.getFields().forEach(ctField -> {
                NestedCUTAK.put(Pair.of(ctField.getReference().toString(),ctField.getClass().getSimpleName()), ctField.getVisibility());
            });

            ctType.getAllFields().forEach(ctFieldReference -> {
                if(!ctFieldReference.toString().contains("."))
                    NestedCUTAK.put(Pair.of(ctFieldReference.getDeclaringType() + "." + ctFieldReference.getSimpleName(),ctFieldReference.getClass().getSimpleName()),
                            ctFieldReference.getFieldDeclaration().getVisibility());
                else NestedCUTAK.put(Pair.of(ctFieldReference.toString(),ctFieldReference.getClass().getSimpleName()), ctFieldReference.getFieldDeclaration().getVisibility());
            });
            ctType.getMethods().forEach(method -> {
                if (method.getBody() != null) {
                    method.getBody().getStatements()
                            .stream()
                            .flatMap(statement -> statement.getReferencedTypes().stream())
                            .forEach(CUTTypeReferences::add);

                    NestedCUTAK.put(Pair.of(fullMethodName(method),method.getClass().getSimpleName()), method.getVisibility());
                }
            });
            ctType.getNestedTypes().forEach(nestedTypes -> {
                NestedCUTAK.putAll(getNestedClass(nestedTypes));
            });
        }

        Iterator<Map.Entry<Pair<String,String>, ModifierKind>> iterator = NestedCUTAK.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Pair<String,String>, ModifierKind> entry = iterator.next();
            if (!entry.getKey().getKey().contains(".")) {
                iterator.remove();
            }
        }
        return NestedCUTAK;
    }

    private Map<Pair<String,String>,ModifierKind> getCUTClasses(List<CtType<?>> CUTClasses){
        Set<CtTypeReference> CUTTypeReferences = new HashSet<>();
        Map<Pair<String,String>,ModifierKind> CUTAccessKind = new HashMap<>();
        CUTClasses.forEach(CUTClass -> {
//            CtClass<?> c = new ModifyCUT(CUTClass).packagePrivateToPublic();
            CUTAccessKind.put(Pair.of(CUTClass.getQualifiedName().replace("$","."),CUTClass.getClass().getSimpleName()), CUTClass.getVisibility());
            if (CUTClass instanceof CtClass) {
                ((CtClass<?>) CUTClass).getConstructors().forEach(ctConstructor -> {
                    if (ctConstructor.getBody() != null)
                        CUTAccessKind.put(Pair.of(fullConstructorName(ctConstructor),ctConstructor.getClass().getSimpleName()), ctConstructor.getVisibility());
                });
            }

            CUTClass.getFields().forEach(ctField -> {
                if (!(ctField instanceof CtFieldImpl))
                    CUTAccessKind.put(Pair.of(ctField.getReference().toString(),ctField.getClass().getSimpleName()),
                            ctField.getVisibility());
            });

            CUTClass.getAllFields().forEach(ctFieldReference -> {
                if(!ctFieldReference.toString().contains(".")){
                    CUTAccessKind.put(Pair.of(ctFieldReference.getDeclaringType() + "." + ctFieldReference.getSimpleName(), ctFieldReference.getClass().getSimpleName()),
                            ctFieldReference.getFieldDeclaration().getVisibility());
                } else CUTAccessKind.put(Pair.of(ctFieldReference.toString(),ctFieldReference.getClass().getSimpleName()), ctFieldReference.getFieldDeclaration().getVisibility());
            });

            CUTClass.getMethods().forEach(method -> {
                if (method.getBody() != null) {
                    method.getBody().getStatements()
                            .stream()
                            .flatMap(statement -> statement.getReferencedTypes().stream())
                            .forEach(CUTTypeReferences::add);

                    CUTAccessKind.put(Pair.of(fullMethodName(method),method.getClass().getSimpleName()), method.getVisibility());
                }
            });

            CUTClass.getNestedTypes().forEach(nestedTypes -> {
                CUTAccessKind.putAll(getNestedClass(nestedTypes));
            });
        });

        Iterator<Map.Entry<Pair<String,String>, ModifierKind>> iterator = CUTAccessKind.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Pair<String,String>, ModifierKind> entry = iterator.next();
            if (!entry.getKey().getKey().contains(".")) iterator.remove();
            else if (entry.getKey().getKey().startsWith("java.") ||
                    entry.getKey().getKey().startsWith("org.example.TestVisibilityChecker") ||
                    entry.getKey().getKey().startsWith("org.example.InvokeMaven") ||
                    entry.getKey().getKey().startsWith("org.example.ModifyCUT")){
                iterator.remove();
            }
//            else {
//                boolean token = packageName.stream()
//                        .anyMatch(ctPackage -> entry.getKey().startsWith(ctPackage.toString()));
//
//                if (!token) {
//                    iterator.remove();
//                }
//            }
        }

        // write into CSV
        writeCUTToCsv(CUTAccessKind);
        CUTAccessKindPrivate = new HashMap<Pair<String,String>,ModifierKind>(CUTAccessKind);
        Iterator<Map.Entry<Pair<String,String>, ModifierKind>> iterator2 = CUTAccessKind.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry<Pair<String,String>, ModifierKind> entry = iterator2.next();
            if (entry.getValue() != null && entry.getValue().toString().equals("private")) {
                    /*
                        remove private method from here as:
                        1. private method can't be invoked directly
                        2. it must be invoked using reflection (<--- mainly) / mocks (<--- rarely, but still possible)
                     */
                iterator2.remove();
            }
        }

        return CUTAccessKind;
    }

    // Creating Source Code Analysis for Class Under Test (DONE)
    // Creating Source Code Analysis for Multiple Classes Under Test (DONE)
    private Map<Pair<String,String>,ModifierKind> getClassUnderTestAccessKind(String absPath) {
        //src/main/java/org/apache/commons/lang3/builder/DiffBuilder.java
        List<CtType<?>> CUTClasses = spoonLauncher(absPath, MavenLauncher.SOURCE_TYPE.APP_SOURCE);
        return getCUTClasses(CUTClasses);
    }

    //TODO: Write transformed test file
    private static void saveTransformedCode(CtModel model, String outputPath) {
        // Create a SpoonResourceOutputHandler
//        SpoonResourceOutputHandler outputHandler = new SpoonResourceOutputHandler(outputPath);
//
//        // Iterate over the elements in the model
//        for (CtType<?> element : model.getAllTypes()) {
//            // Get the compilation unit for the element
//            CompilationUnit compilationUnit = element.getPosition().getCompilationUnit();
//
//            // Save the compilation unit using the output handler
//            outputHandler.createResource(compilationUnit);
//        }
//
//        // Save the transformed code
//        try {
//            outputHandler.process();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static void main(final String[] args) {
        try {
            String absPath = System.getProperty("absPath");
            reportPath = System.getProperty("reportPath");
            new InvokeMaven(absPath).run();

            /*
                TODO: Update "export CLASSPATH"
                      This is not needed if we're not executing the tests
             */
//            String testClassesPath = absPath + "target/test-classes";
//            String classesPath = absPath + "target/classes";
//            String dependencyPath = absPath + "target/dependency";
//            String classpath = System.getProperty("java.class.path") + ":" + testClassesPath + ":" + classesPath + ":" + dependencyPath;
//            System.setProperty("java.class.path", classpath);

            new TestVisibilityChecker2(absPath).run();

            try (CSVWriter writer = new CSVWriter(new FileWriter(reportPath + "zzz_successful_project.csv", true))) {
                writer.writeNext(new String[]{String.valueOf(absPath.split("/")[absPath.split("/").length - 1]),
                        String.valueOf(Instant.now())});
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}