package dawn.cs2;

import dawn.cs2.ast.AbstractCodeNode;

import java.io.StringWriter;

public class CodePrinter {

    private StringWriter writer;
    private int tabs;


    public CodePrinter() {
        writer = new StringWriter();
        tabs = 0;
    }


    public void print(CharSequence str) {
        for (int i = 0; i < str.length(); i++)
            print(str.charAt(i));
    }

    public void print(char c) {
        writer.append(c);
        if (c == '\n')
            writer.append(getTabs());
    }

    private String getTabs() {
        StringBuilder tabs = new StringBuilder();
        for (int i = 0; i < this.tabs; i++)
            tabs.append('\t');
        return tabs.toString();
    }

    public void tab() {
        tabs++;
    }

    public void untab() {
        if (tabs <= 0)
            throw new RuntimeException("Not tabbed!");
        tabs--;
    }

    @Override
    public String toString() {
        writer.flush();
        return writer.toString();
    }

    public static String print(AbstractCodeNode node) {
        CodePrinter printer = new CodePrinter();
        node.print(printer);
        return printer.toString();
    }

}
