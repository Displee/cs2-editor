package dawn.cs2;

import dawn.cs2.ast.ExpressionNode;

public class CS2Stack {

    private ExpressionNode[] stack;
    private int size;
    public static final int BUFFER_SIZE = 200;

    public CS2Stack() {
        stack = new ExpressionNode[BUFFER_SIZE];
    }

    public ExpressionNode pop() {
        if (getSize() <= 0)
            throw new RuntimeException("Stack underflow");
        return stack[--size];
    }

    public ExpressionNode peek() {
        if (getSize() <= 0)
            throw new RuntimeException("Stack underflow");
        return stack[size - 1];
    }

    public void push(ExpressionNode expr) {
        stack[size++] = expr;
    }

    public CS2Stack copy() {
        CS2Stack stack = new CS2Stack();
        stack.size = this.size;
        for (int i = 0; i < size; i++) {
            if (this.stack[i] != null)
                stack.stack[i] = this.stack[i].copy();
        }
        return stack;
    }


    public int getSize() {
        return size;
    }


    public void clear() {
        size = 0;
    }


}
