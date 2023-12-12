package org.example;
import spoon.processing.*;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtThrow;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.CtScanner;

public class AddStackTraceLogging extends AbstractProcessor<CtClass<?>> {
    @Override
    public void process(CtClass<?> ctClass) {
        Factory factory = getFactory(); // Obtain the Spoon factory
        addPrint(factory,ctClass);

    }

    private void addPrint(Factory factory, CtClass<?> ctClass){
//        ((CtClass<?>) ctClass).getConstructors().forEach(ctConstructor -> {
//            CtBlock<?> body = ctConstructor.getBody();
//            if (ctConstructor.getBody() != null) {
//                body.insertBegin(factory.Code().createCodeSnippetStatement("StackTraceElement[] _AddStackTraceLogging = Thread.currentThread().getStackTrace();\n" +
//                        "System.out.println(\"Method invoked from: \" + _AddStackTraceLogging[2].getMethodName());"));
////                System.out.println(body);
//            }
//        });

        ((CtClass<?>) ctClass).getMethods().forEach(ctMethod -> {
//            System.out.println(ctMethod.getSimpleName());
            // Ensure the method has a body before adding the statement
            CtBlock<?> body = ctMethod.getBody();
            if (body != null) {
//                body.insertBegin(factory.Code().createCodeSnippetStatement(
//                        "System.out.print(\"start-trace,\");\n" +
//                                "for (StackTraceElement _log_stack_element : Thread.currentThread().getStackTrace()) {\n" +
//                                "   if (_log_stack_element.toString().startsWith(\"java.\")) { continue; }\n" +
//                                "   if (_log_stack_element.toString().startsWith(\"org.junit.platform\")) { break; }\n" +
//                                "   System.out.print(_log_stack_element.toString() + \",\");\n" +
//                                "}\n" +
//                                "System.out.println(\"end-trace\");"));
                body.insertBegin(factory.Code().createCodeSnippetStatement(
                        "System.out.print(\"entered " + fullMethodName(ctMethod) + ",\")"));

//                body.accept(new CtScanner() {
//                    @Override
//                    public <T> void visitCtReturn(CtReturn<T> returnStatement) {
//                        super.visitCtReturn(returnStatement);
//                        // Add the new statement before every return statement
//                        returnStatement.insertBefore(factory.Code().createCodeSnippetStatement(
//                                "System.out.print(\"exited " + fullMethodName(ctMethod) + ",\")"));
//                    }
//
//                    @Override
//                    public void visitCtThrow(CtThrow throwStatement) {
//                        super.visitCtThrow(throwStatement);
//                        // Add the new statement before every return statement
//                        throwStatement.insertBefore(factory.Code().createCodeSnippetStatement(
//                                "System.out.print(\"exited " + fullMethodName(ctMethod) + ",\")"));
//                    }
//                });
//
//                if(ctMethod.getType().getSimpleName().equals("void")) {
//                    if (!(body.getLastStatement() instanceof CtThrow))
//                        body.insertEnd(factory.Code().createCodeSnippetStatement(
//                            "System.out.print(\"exited " + fullMethodName(ctMethod) + ",\")"));
//                }
            }
        });
    }

    private String fullMethodName(CtMethod<?> method) {
//        System.out.println(method.getSimpleName() + "(" + method.getPosition().getFile().getName() + ":" +
//                method.getPosition().getLine() + "-" + method.getPosition().getEndLine() + ")");
        if (method.getDeclaringType().getPackage() != null) {
            return method.getDeclaringType().getPackage().getQualifiedName() + "."
                    + method.getDeclaringType().getSimpleName() + "."
                    + method.getSimpleName() + "(" + method.getPosition().getFile().getName() + ")";
        }
        return method.getSimpleName() + "(" + method.getPosition().getFile().getName() + ")";
    }
}
