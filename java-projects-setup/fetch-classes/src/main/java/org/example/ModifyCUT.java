package org.example;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.ModifierKind;

public class ModifyCUT {
    private CtClass<?> cutClass;

    public ModifyCUT(CtClass<?> cutClass) {
        this.cutClass = cutClass;
    }

    public CtClass<?> packagePrivateToPublic() {
        cutClass.getMethods().forEach(method -> {
            if (method.getVisibility() == null) {
                method.setVisibility(ModifierKind.PUBLIC);
            }
        });
        return cutClass;
    }
}
