package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.instructions.AbstractInstruction;

public interface Variable {

    String getName();
    CS2Type getType();
    AbstractInstruction generateStoreInstruction();
    AbstractInstruction generateLoadInstruction();

}
