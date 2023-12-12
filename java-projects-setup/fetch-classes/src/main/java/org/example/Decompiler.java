package org.example;

import spoon.JarLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;

public class Decompiler {
    public static void main(String[] args) {
        JarLauncher launcher = new JarLauncher(
                "/home//Desktop/commons-math/commons-math-legacy/target/commons-math4-legacy-4.0-SNAPSHOT-tests.jar");
//        launcher.buildModel();
        CtModel model = launcher.buildModel();

        List<CtClass<?>> testClasses = model.getElements(new TypeFilter<>(CtClass.class));

        testClasses.forEach(testClass -> {
            testClass.getMethods().forEach(method -> {
                // check if the method is a JUnit Test
//                if (method.getAnnotations().stream().map(CtAnnotation::getAnnotationType)
//                        .map(CtTypeReference::getSimpleName).anyMatch(name -> name.equals("Test"))) {
                if (method.getBody() != null && method.getBody().getStatements() != null ) {
                    System.out.println(method.toString());
                }
//                } else {
//                    System.out.println("METHOD: " + method.getDeclaringType().getQualifiedName() + "." +method.getSimpleName());
//                }
            });
        });

        System.out.println("");
    }
}
