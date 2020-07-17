package dawn.cs2.ast;

import dawn.cs2.CodePrinter;
import dawn.cs2.DecompilerException;

import java.util.ArrayList;
import java.util.List;

public class ScopeNode extends ParentAbstractCodeNode {


    /**
     * Contains scope in which this scope is
     * declared or null if this scope is first.
     */
    private ScopeNode parentScope;
    /**
     * Contains parent node or null if this scope doesn't have
     * parent node.
     */
    private AbstractCodeNode parent;
    /**
     * Contains list of declared local variables.
     */
    private List<LocalVariable> declaredLocalVariables;


    public ScopeNode() {
        this(null);
    }

    public ScopeNode(ScopeNode parent) {
        this.parentScope = parent;
        this.declaredLocalVariables = new ArrayList<LocalVariable>();
    }

    /**
     * Declare's given local variable to this scope.
     *
     * @param variable
     * @throws DecompilerException If variable there's variable with the same name declared.
     */
    public void declare(LocalVariable variable) throws DecompilerException {
        if (this.isDeclared(variable.getName())) {
            throw new DecompilerException("Variable (" + variable.toString() + ") is already declared!");
        }
        this.declaredLocalVariables.add(variable);
    }

    /**
     * Get's declared local variable from this scope or one of the parent scopes.
     * @param localName
     * Local name of the variable that should be returned.
     * @return
     * Returns local variable with given localName.
     * @throws CompilerException
     * If the given local variable is not declared.
     */
//    public LocalVariable getLocalVariable(String localName) throws DecompilerException {
//		for (LocalVariable var : this.declaredLocalVariables) {
//		    if (var.getName().equals(localName)) {
//		    	return var;
//		    }
//		}
//		if (this.parentScope != null) {
//		    return this.parentScope.getLocalVariable(localName);
//		}
//		throw new DecompilerException("Variable " + localName + " is not declared!");
//    }

    /**
     * Get's declared local variable from this scope or one of the parent scopes.
     *
     * @param identifier Identifier of the local variable
     * @return Returns local variable with given localName.
     * @throws DecompilerException If the given local variable is not declared.
     */
    public LocalVariable getLocalVariable(int identifier) throws DecompilerException {
        for (LocalVariable var : this.declaredLocalVariables) {
            if (var.getIdentifier() != -1 && var.getIdentifier() == identifier) {
                return var;
            }
        }
        if (this.parentScope != null) {
            return this.parentScope.getLocalVariable(identifier);
        }
        throw new DecompilerException("Variable " + identifier + " is not declared!");
    }

    public LocalVariable getLocalVariableByName(String name) {
        for (LocalVariable var : this.declaredLocalVariables) {
            if (var.getName().equals(name)) {
                return var;
            }
        }
        if (this.parentScope != null) {
            return this.parentScope.getLocalVariableByName(name);
        }
        return null;
    }

    /**
     * Get's if given local variable is declared in this
     * scope or in parent scopes.
     *
     * @param localName Name of the local variable
     * @return Wheter given local variable is declared.
     */
    public boolean isDeclared(String localName) {
        for (LocalVariable var : this.declaredLocalVariables) {
            if (var.getName().equals(localName)) {
                return true;
            }
        }
        if (this.parentScope != null) {
            return this.parentScope.isDeclared(localName);
        }
        return false;
    }


    /**
     * Get's if given local variable is declared in this
     * scope or in parent scopes.
     *
     * @param identifier Identifier of the local variable.
     * @return Wheter given local variable is declared.
     */
    public boolean isDeclared(int identifier) {
        for (LocalVariable var : this.declaredLocalVariables) {
            if (var.getIdentifier() != -1 && var.getIdentifier() == identifier) {
                return true;
            }
        }
        if (this.parentScope != null) {
            return this.parentScope.isDeclared(identifier);
        }
        return false;
    }

    /**
     * Copies declared variables in this scope only.
     */
    public List<LocalVariable> copyDeclaredVariables() {
        return new ArrayList<LocalVariable>(this.declaredLocalVariables);
    }

    /**
     * Get's if this scopeNode is empty.
     *
     * @return
     */
    public boolean isEmpty() {
        return this.size() <= 0;
    }

    /**
     * Get's root (first) scope.
     */
    public ScopeNode getRootScope() {
        if (this.parentScope != null)
            return this.parentScope.getRootScope();
        return this;
    }

    public ScopeNode getParentScope() {
        return parentScope;
    }

    /**
     * Find's controllable flow node to which target belongs.
     * Return's null if nothing was found.
     */
//    public IControllableFlowNode findControllableNode(FlowBlock target) {
//        if (this instanceof IControllableFlowNode) {
//            if (this instanceof IBreakableNode && ((IBreakableNode) this).canBreak() && ((IBreakableNode) this).getEnd() == target)
//                return (IControllableFlowNode) this;
//            else if (this instanceof IContinueableNode && ((IContinueableNode) this).canContinue() && ((IContinueableNode) this).getStart() == target)
//                return (IControllableFlowNode) this;
//        }
//        if (this.parent != null && this.parent instanceof IControllableFlowNode) {
//            IControllableFlowNode parent = (IControllableFlowNode) this.parent;
//            if (parent instanceof IBreakableNode && ((IBreakableNode) parent).canBreak() && ((IBreakableNode) parent).getEnd() == target)
//                return parent;
//            else if (parent instanceof IContinueableNode && ((IContinueableNode) parent).canContinue() && ((IContinueableNode) parent).getStart() == target)
//                return parent;
//        }
//        if (this.parentScope != null)
//            return this.parentScope.findControllableNode(target);
//        return null;
//    }
    public void setParent(AbstractCodeNode parentInstruction) {
        this.parent = parentInstruction;
    }

    public AbstractCodeNode getParent() {
        return parent;
    }

    @Override
    public void print(CodePrinter printer) {
        printer.tab();
        printer.print("{\r\n");
        printInline(printer);
        printer.untab();
        printer.print("\r\n}");
    }

    void printInline(CodePrinter printer) {
        for (LocalVariable var : this.declaredLocalVariables) {
            if (!var.isArgument())
                printer.print("" + var.toString() + ";\r\n");
        }
        List<AbstractCodeNode> childs = this.listChilds();
        boolean caseAnnotationTabbed = false;
        for (int i = 0; i < childs.size(); i++) {
            AbstractCodeNode node = childs.get(i);

            if (i != childs.size() - 1 && (childs.get(i + 1) instanceof CaseAnnotation)) {
                printer.untab();
                caseAnnotationTabbed = true;
            }
            if (node instanceof CaseAnnotation) {


                node.print(printer);

            } else {
                node.print(printer);
            }
            if (i != childs.size() - 1) {
                printer.print("\r\n");
            }
            if (caseAnnotationTabbed) {
                caseAnnotationTabbed = false;
                printer.tab();
            }

        }
        if (caseAnnotationTabbed)
            printer.untab();
    }

}
