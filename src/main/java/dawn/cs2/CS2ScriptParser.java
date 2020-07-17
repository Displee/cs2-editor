package dawn.cs2;


import dawn.cs2.ast.*;
import dawn.cs2.util.FunctionDatabase;
import dawn.cs2.util.FunctionInfo;
import dawn.cs2.util.TextUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static dawn.cs2.ast.LocalVariable.CHILD;
import static dawn.cs2.ast.LocalVariable._CHILD;
import static dawn.cs2.ast.LoopNode.LOOPTYPE_WHILE;

public class CS2ScriptParser {

    private FunctionDatabase opcodesDatabase;
    private FunctionDatabase scriptsDatabase;

//    public static void main(String[] args) throws IOException {
//
////        String input = new String(Files.readAllBytes(Paths.get("test2/2643.cs2")));
//        int id = 6;
//        String input = new String(Files.readAllBytes(Paths.get("test2/" + id + ".cs2")));
//        FunctionNode func = CS2ScriptParser.parse(input, new FunctionDatabase(new File("rs2_new.ini"), false), new FunctionDatabase(new File("scripts_db.ini"), true));
//        if (func != null) {
//            CodePrinter printer = new CodePrinter();
//            func.print(printer);
//            System.err.println(printer.toString());
//        }
//        CS2Compiler compiler = new CS2Compiler(func);
//        compiler.compile(new File("compiled/" + id));
//    }

    private StringTokenizer lexer;
    private ScopeNode currentScope;
    private Map<String, Variable> declared = new HashMap<>();


    private CS2ScriptParser(String input, FunctionDatabase opcodesDatabase, FunctionDatabase scriptsDatabase) {
        lexer = new StringTokenizer(TextUtils.unescapeUnicode(input), "{}()[] \t\r\n\f#.=+-*/%;,!:><&|'\"\\", true);
        this.opcodesDatabase = opcodesDatabase;
        this.scriptsDatabase = scriptsDatabase;
        //TODO: Abstract LocalVariable away etc
        declared.put("CHILD", CHILD);
        declared.put("_CHILD", _CHILD);
        declared.put("_", Underscore.UNDERSCORE);
    }

    public static FunctionNode parse(String input, FunctionDatabase opcodesDatabase, FunctionDatabase scriptsDatabase) {
        CS2ScriptParser parser = new CS2ScriptParser(input, opcodesDatabase, scriptsDatabase);
        try {
            return parser.parseScriptDefinition();
        } catch (Throwable t) {
            parser.currentLine.append(parser.curr);
            while (parser.lexer.hasMoreTokens()) {
                String n = parser.lexer.nextToken();
                if (n.charAt(0) == '\n') {
                    break;
                }
                parser.currentLine.append(n);
            }
            String msg = parser.currentLine.toString().replace("\t", "").replace("\r", "") + "\r\n";
//            System.err.println(parser.currentLine.toString().replace('\t', ' '));
            for (int i = 0; i < parser.linePos; i++) {
//                System.err.print(' ');
                msg += ' ';
            }
            msg += "^\r\n";
//            System.err.println("^");
//            System.err.println("Error on line " + parser.lineNumber + ", position " + parser.linePos);
            msg += "Error on line " + parser.lineNumber + ", position " + parser.linePos;
//            t.printStackTrace();
//            return null;
            throw new DecompilerException(t.getMessage() + "\r\n" + msg);
        }
    }

    int intlocals;
    int stringlocals;
    int longlocals;

    public FunctionNode parseScriptDefinition() {
        advance();
        boolean copied = false;
        while (curr.equals("define")) {
            advance();
            FunctionNode sig = parseSignature(false);
            if (REG_INT.test(curr)) {
                int id = Integer.parseInt(curr);
                advance();
                if (!copied) {
                    scriptsDatabase = scriptsDatabase.copy();
                    copied = true;
                }
                scriptsDatabase.putInfo(id, new FunctionInfo(id, sig));
            } else {
                throw new DecompilerException("define void signature() ID expected");
            }
        }

        //Parse signature
        FunctionNode root = parseSignature(true);
        assert curr.equals(SCOPE_START) : "{ expected: " + curr;
        advance();
        //TODO:
        currentScope = root.getMainScope();
        readScope(root.getMainScope());
        try {
            advance();
            assert false : "Script finished, end of file expected! " + curr;
        } catch (DecompilerException e) {
            //ok
        }
        return root;
    }

    private FunctionNode parseSignature(boolean names) {
        List<CS2Type> returnType = new ArrayList<>();
        while (true) {
            CS2Type type = parseType();
            assert type != null : "Return type expected: " + curr;
            returnType.add(type);
            if (curr.equals(COMMA)) {
                advance();
            } else {
                break;
            }
        }
        String scriptName = curr;
        assert isIdentifier(scriptName) : "Script name must be valid identifier: " + scriptName;
        advance();
        assert curr.equals(LEFT_PAREN) : "Argument list expected: " + curr;
        List<LocalVariable> arguments = new ArrayList<>();
        List<CS2Type> argTypes = new ArrayList<>();
        advance();
        ScopeNode scope = new ScopeNode();
        while (!curr.equals(RIGHT_PAREN)) {
            CS2Type type = parseType();
            assert type != null : "Type expected in arg list: " + curr;
            if (names) {
                String identifier = curr;
                assert declared.get(identifier) == null : "Variable already declared: " + curr;
                assert isIdentifier(identifier) : "Not a valid identifier: " + curr;
                LocalVariable var = new LocalVariable(identifier, type, true);
                if (type.isCompatible(CS2Type.INT)) {
                    var.setIdentifier(LocalVariable.makeIdentifier(intlocals++, 0));
                } else if (type.isCompatible(CS2Type.STRING)) {
                    var.setIdentifier(LocalVariable.makeIdentifier(stringlocals++, 1));
                } else {
                    var.setIdentifier(LocalVariable.makeIdentifier(longlocals++, 2));
                }
                arguments.add(var);
                scope.declare(var);
                declared.put(identifier, var);
                advance();
            }
            argTypes.add(type);
            assert curr.equals(RIGHT_PAREN) || curr.equals(COMMA) : ", or ) expected: " + curr;
            if (curr.equals(COMMA)) {
                advance();
            }
        }
        advance(); //right paren
        //Parse main scope
        FunctionNode root = new FunctionNode(scriptName, argTypes.toArray(new CS2Type[0]), CS2Type.of(returnType), scope, arguments);

        return root;
    }

    //args dynamic = look for last string literal arg
    //return type dynamic = datamap? second char arg ELSE attr id lookup

    private FunctionInfo resolveCall(String name, List<ExpressionNode> args) {
        List<CS2Type> signature = new ArrayList<>();
        for (ExpressionNode arg : args) {
//            if (arg instanceof CallbackExpressionNode) {
            //TODO:
//                signature.add(CS2Type.INT); //
//                signature.addAll(Arrays.asList(((CallbackExpressionNode) arg).call.info.getArgumentTypes()));
//            } else {
            if (arg instanceof VariableLoadNode) {
                if (((VariableLoadNode) arg).getVariable() == CHILD || ((VariableLoadNode) arg).getVariable() == _CHILD) {
                    continue;
                }
            }
            signature.addAll(arg.getType().composite);
//            }
        }

        search:
        for (FunctionInfo op : opcodesDatabase.getByName(name)) {
            if (signature.size() != op.getArgumentTypes().length) {
                continue;
            }

            for (int i = 0; i < signature.size(); i++) {
                if (op.getArgumentTypes()[i] == CS2Type.CALLBACK && args.get(i) instanceof NullableIntExpressionNode) {
                    args.set(i, new CallbackExpressionNode(null /* new CallExpressionNode(new FunctionInfo("null (unhook)", -1, new CS2Type[0], CS2Type.VOID, new String[0], true), new ExpressionNode[0], false)*/, null));
                    continue;
                }
                if (!signature.get(i).isCompatible(op.getArgumentTypes()[i])) {
                    continue search;
                }
            }
            if (op.getReturnType().equals(CS2Type.UNKNOWN)) {
                if (op.id == 3408) {
                    assert args.get(1) instanceof CharExpressionNode;
                    //TODO:
                    return new FunctionInfo(name, op.id, op.argumentTypes, CS2Type.forJagexDesc(((CharExpressionNode) args.get(1)).getData()), op.argumentNames, false);
                } else {
                    if (op.id == 1613) {
                        return new FunctionInfo(name, op.id, op.argumentTypes, CS2Type.attrTypes.get(((IntExpressionNode) args.get(0)).getData()), op.argumentNames, false);
                    } else {
                        //
                        return new FunctionInfo(name, op.id, op.argumentTypes, CS2Type.attrTypes.get(((IntExpressionNode) args.get(args.size() - 1)).getData()), op.argumentNames, false);
                    }
                }
            }
            return op;
        }
        search:
        for (FunctionInfo script : scriptsDatabase.getByName(name)) {
            if (signature.size() != script.getArgumentTypes().length) {
                continue;
            }
            for (int i = 0; i < signature.size(); i++) {
                if (!signature.get(i).isCompatible(script.getArgumentTypes()[i])) {
                    continue search;
                }
            }
            return script;
        }
        return null;
    }

    public void readSwitchScope(SwitchNode sw) {
        ScopeNode scope = sw.getScope();
        HashMap<String, Variable> beforeScope = new HashMap<>(declared);
        while (!curr.equals(SCOPE_END)) {
            while (curr.equals("case")) {
                advance();
                ExpressionNode number = parseExpression(); //We have some special syntax for primitives disguised as an expression (widget, location)
                //assert subtype of intexpressionnode (after refactoring widget syntax)
                if (number instanceof NewWidgetPointerNode) {
                    number = ((NewWidgetPointerNode) number).getExpression();
                }
                if (number instanceof NewColorNode) {
                    number = ((NewColorNode) number).getExpression();
                }
                assert number instanceof IIntConstantNode : "Can only switch on constants";
//                scope.write(number);
                assert curr.equals(":");
                advance();
                scope.write(new CaseAnnotation(((IIntConstantNode) number).getConst(), sw.getExpression()));
            }
            if (curr.equals("break")) {
                advance();
                advance();
                scope.write(new BreakNode());
                continue;
            }
            if (curr.equals("default")) {
                advance();
                advance();
                scope.write(new CaseAnnotation());
            }

            AbstractCodeNode node = parseBlockStatement();
            if (node != null) {
                scope.write(node);
            } else {
                throw new DecompilerException("rip");
            }
        }
        declared = beforeScope;
    }

    public void readScope(ScopeNode scope) {
        HashMap<String, Variable> beforeScope = new HashMap<>(declared);
        while (!curr.equals(SCOPE_END)) {
            AbstractCodeNode node = parseBlockStatement();
//            System.err.println("NODE: " + node);
            if (node != null) {
                scope.write(node);
            } else {
                throw new DecompilerException("rip");
//                assert curr.equals(SCOPE_END);
//                advance();
//                break;
            }
        }
        declared = beforeScope;
    }

    public AbstractCodeNode parseBlockStatement() {
        if (curr.equals(SCOPE_END)) {
            return null;
        }

        ExpressionNode statement = parseVariableDeclaration();
        if (statement != null) {
            advance();
            return new PopableNode(statement);
        }
        if (curr.equals(SEMI)) {
            advance();
            return parseBlockStatement();
        }

        //TODO:
        //break
        //comment
        AbstractCodeNode control = parseFlowConstruct();
        if (control != null) {
            return control;
        }


        if (statement == null)
            statement = parseExpressionStatement();

        if (statement instanceof VariableAssignationNode) {
//            checkTypes((VariableAssignationNode)statement);
        }

        assert curr.equals(SEMI);
        advance();
        return new PopableNode(statement);
    }

    private AbstractCodeNode parseFlowConstruct() {
        if (curr.equals("return")) {
            advance();
            ExpressionNode values = parseExpressionList();
            assert curr.equals(SEMI);
            advance();
            return new ReturnNode(values);
        }

        if (curr.equals(SWITCH)) {
            advance();
            assert curr.equals(LEFT_PAREN);
            ExpressionNode on = parseExpression();
            SwitchNode sw = new SwitchNode(new ScopeNode(), on);
            if (curr.equals(SCOPE_START)) {
                advance();
                readSwitchScope(sw);
                advance();
            }
            return sw;
        }

        AbstractCodeNode flow = null;
        ScopeNode scope = null;
        if (curr.equals(WHILE)) {
            advance();
            assert curr.equals(LEFT_PAREN);
            ExpressionNode condition = parseExpression();
            scope = new ScopeNode();
            flow = new LoopNode(LOOPTYPE_WHILE, scope, condition);
        }
        if (curr.equals("if")) {
            advance();
            assert curr.equals(LEFT_PAREN);
            ExpressionNode condition = parseExpression();
            scope = new ScopeNode();
            flow = new IfElseNode(condition, scope, new ScopeNode());
        }

        if (flow != null) {
            if (curr.equals(SCOPE_START)) {
                advance();
                readScope(scope);
                advance();
            } else {
                AbstractCodeNode n = parseBlockStatement(); //Only returns null on }
                assert n != null : "Single statement or start of scope expected";
                scope.write(n);
            }
            if (flow instanceof IfElseNode && curr.equals("else")) {
                advance();
                IfElseNode ifelse = (IfElseNode) flow;
                if (curr.equals(SCOPE_START)) {
                    advance();
                    readScope(ifelse.getElseScope());
                    assert curr.equals(SCOPE_END);
                    advance();
                } else {
                    AbstractCodeNode elseIf = parseBlockStatement();
                    if (elseIf != null) {
                        ifelse.getElseScope().write(elseIf);
                    }
                }
            }
            return flow;
        }
        return null;
    }

    public VariableAssignationNode parseVariableDeclaration() {
        CS2Type type = parseType();
        if (type == null) {
            return null;
        }
        LinkedList<Variable> vars = new LinkedList<>();
        do {
            assert isIdentifier(curr) : "Not a valid identifier";
            assert declared.get(curr) == null : "Variable already declared";
            LocalVariable var = new LocalVariable(curr, type);
            //TODO: Need to rewrite alot of the variable stuff. counters can be reused between scope (force initialization??)
            if (type.isCompatible(CS2Type.INT)) {
                var.setIdentifier(LocalVariable.makeIdentifier(intlocals++, 0));
            } else if (type.isCompatible(CS2Type.STRING)) {
                var.setIdentifier(LocalVariable.makeIdentifier(stringlocals++, 1));
            } else {
                var.setIdentifier(LocalVariable.makeIdentifier(longlocals++, 2));
            }
            currentScope.declare(var);
            declared.put(curr, var);
            vars.add(var);
            advance();
        } while (curr.equals(COMMA) && advance() != null);
        if (curr.equals("=")) {
            advance();
            return new VariableAssignationNode(vars, parseExpressionList());
        }
        assert curr.equals(SEMI) : "Initialization not yet supported";
        return null;
    }


    public ExpressionNode parseExpressionStatement() {
        ExpressionList list = parseExpressionList();
        if (list != null) {
            int assignIdx = -1;
            LinkedList<Variable> vars = new LinkedList<>();
            ExpressionList actualAssign = new ExpressionList(new ArrayList<>());

            List<CS2Type> assignedTypes = new ArrayList<>();

            for (int i = 0; i < list.arguments.size(); i++) {
                ExpressionNode arg = list.arguments.get(i);
                if (arg instanceof VariableAssignationNode) {
                    if (assignIdx != -1) {
                        assert false : "Syntax error: Multiple assignments in expressionlist";
                    }
                    assignIdx = i;
                    assignedTypes.addAll(((VariableAssignationNode) arg).getExpression().getType().composite);
                } else if (assignIdx != -1) {
                    assignedTypes.addAll(arg.getType().composite);
                }
            }
            if (assignIdx != -1) {
                for (int i = 0; i < list.arguments.size(); i++) {
                    ExpressionNode arg = list.arguments.get(i);
                    if (assignIdx > i) {
                        //Identifiers would have been parsed as load expressions. convert them to variables
                        assert arg instanceof VariableLoadNode : "LHS is not assignable";
                        Variable var = declared.get(arg.toString());
                        if (var == null) {
                            var = GlobalVariable.parse(arg.toString());
                        }
                        if (var == Underscore.UNDERSCORE) {
                            var = Underscore.forType(assignedTypes.get(i));
                        }
                        assert var.getType().isCompatible(assignedTypes.get(i));
                        vars.add(var);
                    } else if (assignIdx == i) {
                        List<Variable> to = ((VariableAssignationNode) arg).variables;
                        assert to.size() == 1 : "expected only 1 var from parser";
                        Variable var = to.get(0);
                        if (var == Underscore.UNDERSCORE) {
                            var = Underscore.forType(assignedTypes.get(i));
                        }
                        assert var.getType().isCompatible(assignedTypes.get(i));
                        vars.add(var);
                        actualAssign.arguments.add(((VariableAssignationNode) arg).getExpression());
                    } else {
                        actualAssign.arguments.add(arg);
                    }
                }
                return new VariableAssignationNode(vars, actualAssign);
            }
        }
        assert !curr.equals(RIGHT_PAREN) : "Mismatched parenthesis";
        assert curr.equals(SEMI) : "Expected ; got " + curr;
        //assert size 1?
        return list;
    }

    public ExpressionList parseExpressionList() {
        ExpressionNode first = parseExpression();
        if (first == null) {
            return null;
        }
        if (curr.equals(COMMA)) {
            List<ExpressionNode> expressions = new ArrayList<>();
            expressions.add(first);
            while (curr.equals(COMMA)) {
                advance();
                ExpressionNode next = parseExpression();
                expressions.add(next);
            }
            return new ExpressionList(expressions);
        }
        return new ExpressionList(Arrays.asList(first));
    }

    public ExpressionNode parseExpression() {
        ExpressionNode operand = parseOperand();
        if (operand == null) {
            return null;
        }
        //if curr == ,

        Stack<ExpressionNode> operands = new Stack<>();
        Stack<Operator> operators = new Stack<>();
        operators.push(Operator.DUMMY_OP); //Simplifies algorithm. allows us to compare freely
        operands.push(operand);
        do {
            if (curr.equals(SEMI) || curr.equals(COMMA) || curr.equals(RIGHT_PAREN) || curr.equals(ARRAY_IDX_END)) {
                while (operators.size() > 1) {
                    consumeOp(operands, operators);
                }
                assert operands.size() == 1 : "Got some leftovers operands";
                return operands.pop();
            }
            Operator operator = parseOperator(false);
            if ((operator == null) && operands.size() == 1) {
                return operands.pop();
            }
            assert operator != null : "Operator expected";
            while (operators.peek().assocRight ? operator.priority > operators.peek().priority : operator.priority >= operators.peek().priority) { //> if right assoc
                consumeOp(operands, operators);
            }
            operators.push(operator);
            operands.push(parseOperand());
        } while (true);
    }

    private void consumeOp(Stack<ExpressionNode> operands, Stack<Operator> operators) {
        //Pop an operator from the stack, pop as many operands as needed, and push the result back as an operand
        if (operators.peek().type == MathExpressionNode.class) {
            ExpressionNode r = operands.pop();
            ExpressionNode l = operands.pop();

            //Implicit int -> str conversion when concatenating. We can't do long because there is no opcode. for that
            if (operators.peek() == Operator.PLUS) {
                if (l.getType() == CS2Type.STRING && r.getType().isCompatible(CS2Type.INT)) {
                    r = new CallExpressionNode(opcodesDatabase.getInfo(4106), new ExpressionNode[]{r}, false);
                }
                if (l.getType().isCompatible(CS2Type.INT) && r.getType() == CS2Type.STRING) {
                    l = new CallExpressionNode(opcodesDatabase.getInfo(4106), new ExpressionNode[]{l}, false);
                }
            }

            assert r.getType().isCompatible(l.getType()) : "Incompatible types " + l.getType() + " and " + r.getType() + " (" + l + ") " + operators.peek() + " (" + r + ")";
            if (l.getType() == CS2Type.STRING) {
                BuildStringNode builder;
                if (l instanceof BuildStringNode) {
                    builder = (BuildStringNode) l;
                } else {
                    builder = new BuildStringNode(new ArrayList<>());
                    builder.arguments.add(l);
                }
                builder.arguments.add(r);
                //TODO implement == and != via compare OP 4107?
                assert operators.peek() == Operator.PLUS : "Unknown string operator " + operators.peek();
                operators.pop();
                operands.push(builder);
            } else {
                operands.push(new MathExpressionNode(l, r, operators.pop()).simplify());
            }
        } else if (operators.peek().type == ConditionalExpressionNode.class) {
            ExpressionNode r = operands.pop();
            operands.push(new ConditionalExpressionNode(operands.pop(), r, operators.pop()));
        } else if (operators.peek().type == VariableAssignationNode.class) {
            ExpressionNode expr = operands.pop();
            ExpressionNode assignTo = operands.pop();
            if (assignTo instanceof ArrayLoadNode) {
                ArrayLoadNode load = (ArrayLoadNode) assignTo;
                operators.pop();
                operands.push(new ArrayStoreNode(load.arrayId, load.getIndex(), expr));
            } else {
                assert assignTo instanceof VariableLoadNode : "LHS is not assignable";
                operators.pop(); //Always =


                Variable var = declared.get(assignTo.toString());
                if (var == null) {
                    var = GlobalVariable.parse(assignTo.toString());
                }

                //TODO: Support += etc? probably not a good idea with multiple return values
                operands.push(new VariableAssignationNode(var, expr));
            }
        } else {
            throw new DecompilerException("Operator not yet supported " + operators.peek());
        }
    }

    private Operator parseOperator(boolean prefix) {
        String tok = curr;
        boolean isOp = false;
        for (Operator op : Operator.values()) {
            if (op.text.startsWith(tok) && (!prefix || op.prefix)) {
                isOp = true;
            }
        }
        if (!isOp) {
            return null;
        }
        //Check if next character is also an operator, whitespace is important here!
        returnWhitespace = true;
        String combinedOp = curr + advance().trim();
        for (Operator op : Operator.values()) {
            if (op.text.equals(combinedOp) && (!prefix || op.prefix)) {
                returnWhitespace = false;
                advance();
                return op;
            }
        }
        //It might have been something like -( or -int1, try just using the first token.
        for (Operator op : Operator.values()) {
            if (op.text.equals(tok) && (!prefix || op.prefix)) {
                returnWhitespace = false;
                return op;
            }
        }
        throw new DecompilerException("Invalid operator");
    }

    public ExpressionNode parseOperand() {
        Operator prefix = parseOperator(true);
        if (prefix != null) {
            //These unary operators have priority over anything, so we can safely do this i guess. If this is not enough move the parseoperator in a while loop before parseoperand calls in shunting yard alg
            if (prefix == Operator.UNARYNOT) {
                ExpressionNode expr = parseOperand();
                assert expr.getType() == CS2Type.BOOLEAN : "Expected a boolean expression";
                return new BooleanConditionalExpressionNode(true, expr);
            } else if (prefix == Operator.UNARYMINUS) {
                ExpressionNode rec = parseOperand();
                if (rec instanceof IntExpressionNode) {
                    ((IntExpressionNode) rec).data = -((IntExpressionNode) rec).data;
                    return rec;
                } else if (rec instanceof LongExpressionNode) {
                    ((LongExpressionNode) rec).data = -((LongExpressionNode) rec).data;
                    return rec;
                }
            }
//                ExpressionNode expr = parseOperand();
            //TODO: Make AST for this
//            return new UnaryExpr(prefix, expr);
            throw new DecompilerException("This unary operator is not supported yet " + prefix);
        }

        if (curr.equals(LEFT_PAREN)) {
            advance();
            CS2Type cast = parseType();
            if (cast != null) {
                List<CS2Type> castTo = new ArrayList<>();
                castTo.add(cast);
                while (curr.equals(COMMA)) {
                    advance();
                    CS2Type second = parseType();
                    assert second != null : "Type expected";
                    castTo.add(second);
                }
                assert curr.equals(RIGHT_PAREN);
                advance();
                return CS2Type.cast(parseOperand(), CS2Type.of(castTo));
            }
            ExpressionNode expr = parseExpression();
            assert curr.equals(RIGHT_PAREN) : "Mismatched parenthesis";
            advance();
            return expr;
        }


        ExpressionNode prim = parsePrimitive();
        if (prim != null) {
            return prim;
        }

        if (isIdentifier(curr)) {
            String symbol = curr;
            advance();

            ExpressionNode call = parseCall(symbol);
            if (call != null) {
                while (curr.equals(DOT)) {
                    String chain = advance();
                    advance();
                    assert curr.equals(LEFT_PAREN) : "Argument list expected";
                    call = parseArgList(chain, call);
                }
                return call;
            }

            if (curr.equals(ARRAY_IDX_START)) {
                advance();
                ExpressionNode idx = parseExpression();
                assert idx != null : "";
                assert curr.equals(ARRAY_IDX_END);
                advance();
                if (symbol.startsWith("ARRAY")) {
                    assert symbol.length() == 6 : "Only global array 0-4 are valid";
                    int ga = symbol.charAt(5) - '0';
                    assert ga >= 0 && ga < 5 : "Only global array 0-4 are valid";
                    return new ArrayLoadNode(ga, CS2Type.INT, idx);
                } else {
                    //TODO: abstract things like widget/location node behind intexpressionnode
                    assert idx instanceof IntExpressionNode : "Configuration access requires constant index";
                    return new VariableLoadNode(GlobalVariable.parse(symbol + "[" + ((IntExpressionNode) idx).getData() + "]"));
                }
            } else if (symbol.startsWith("ARRAY")) {
                assert symbol.length() == 6 : "Only global array 0-4 are valid";
                int ga = symbol.charAt(5) - '0';
                assert ga >= 0 && ga < 5 : "Only global array 0-4 are valid";

                assert curr.equals("=") : "Global array can not be directly referenced outside of initialization";
                advance();
                assert curr.equals("new") : "use new operator to initialize array";
                advance();
                CS2Type arrayType = parseType();
                assert arrayType != null && arrayType.isCompatible(CS2Type.INT) : "Valid int based type expected";
                assert curr.equals(ARRAY_IDX_START) : "[ expected";
                advance();
                ExpressionNode size = parseExpression();
                assert size != null && size.getType().isCompatible(CS2Type.INT) : "Not a valid size expression";
                assert curr.equals(ARRAY_IDX_END) : "] expected";
                advance();
                return new NewArrayNode(ga, size, arrayType);
            } else {

                assert declared.get(symbol) != null : "Variable not declared: " + symbol;
                //Not a call. Just a variable load.
                return new VariableLoadNode(declared.get(symbol));
            }
        }

        //End of expression
        return null;
    }

    private ExpressionNode parseCall(String symbol) {
        VariableLoadNode var = null;
        if (curr.equals(DOT)) { //call on 'object' syntax
            //ASSERT IDENTIFIER/primitive hidden by widget(.., ..)
            assert declared.get(symbol) != null : "Variable not declared";

            var = new VariableLoadNode(declared.get(symbol));
            advance();
            symbol = curr;
            advance();
            assert curr.equals(LEFT_PAREN);
        }
        return parseArgList(symbol, var);
    }

    private ExpressionNode parseArgList(String symbol, ExpressionNode invokeOn) {
        List<ExpressionNode> args = new ArrayList<>();
        if (curr.equals(LEFT_PAREN)) {
            advance();
            while (true) {


                ExpressionNode arg = parseScriptCallback();
                if (arg == null)
                    arg = parseExpression();

//                assert arg != null : "Argument expected";
                if (arg != null) {
                    args.add(arg);
                }
                if (curr.equals(RIGHT_PAREN)) {
                    advance();
                    break;
                }

                if (curr.equals(COMMA)) {
                    advance();
                } else {
                    throw new DecompilerException("bad arg list");
                }
            }
            if (invokeOn != null) {
                args.add(invokeOn);
            }

            if (symbol.equals("color")) {
                if (args.size() == 1) {
                    return new NewColorNode(args.get(0));
                }
            }
            if (symbol.equals("widget")) {
                if (args.size() == 2) {
                    assert args.get(0) instanceof IntExpressionNode && args.get(1) instanceof IntExpressionNode : "widget(parent, child) requires constants";
                    return new NewWidgetPointerNode(new IntExpressionNode(((IntExpressionNode) args.get(0)).getData() << 16 | ((IntExpressionNode) args.get(1)).getData()));
                } else if (args.size() == 1) {
                    return new NewWidgetPointerNode(args.get(0));
                }
            }
            if (symbol.equals("location")) {
                if (args.size() == 3) {
                    assert args.get(0) instanceof IntExpressionNode && args.get(1) instanceof IntExpressionNode && args.get(2) instanceof IntExpressionNode : "location(x, y, z) requires constants";
                    return new NewLocationNode(new IntExpressionNode(((IntExpressionNode) args.get(2)).getData() << 28 | ((IntExpressionNode) args.get(0)).getData() << 14 | ((IntExpressionNode) args.get(1)).getData()));
                } else if (args.size() == 1) {
                    return new NewLocationNode(args.get(0));
                }
            }
            FunctionInfo info = resolveCall(symbol, args);
            assert info != null : "Could not resolve method call " + symbol + " " + args;
            return new CallExpressionNode(info, args.toArray(new ExpressionNode[0]), invokeOn != null);
        }
        return null;
    }

    private ExpressionNode parseScriptCallback() {
        if (curr.equals("&")) {
            String func = advance();
            advance();
            assert curr.equals(LEFT_PAREN) : "Callback requires argument list";
            ExpressionNode call = parseArgList(func, null);
            assert call != null && call instanceof CallExpressionNode : "Invalid callback supplied: " + call;
            assert ((CallExpressionNode) call).info.isScript : "Callback must be a parameterized script";
            ExpressionList trigger = null;
            if (curr.equals(COMMA)) {
                advance();
                //NOTE: This make it so dot syntax is required though
                trigger = parseExpressionList();
            }
            return new CallbackExpressionNode((CallExpressionNode) call, trigger);
        }
        return null;
    }


    private String curr = "";
    private boolean returnWhitespace = false;
    private int lineNumber = 1;
    private int linePos = 0;
    private StringBuilder currentLine = new StringBuilder();

    public String advance() {
        if (lexer.hasMoreTokens()) {
            if (curr.length() >= 1 && curr.charAt(0) != '\n') {
                linePos += curr.length();
                currentLine.append(curr);
            }
            String next = curr = lexer.nextToken();
            if (next.charAt(0) == '#' && !returnWhitespace) {
                while (next.charAt(0) != '\n') {
                    next = curr = lexer.nextToken();
                }
            }
            if (next.charAt(0) == '\n') {
                lineNumber++;
                linePos = 0;
                currentLine = new StringBuilder();
            }
            if (!returnWhitespace && (Character.isWhitespace(next.charAt(0)))) {
                return advance();
            }
            return (curr = next);
        }
        throw new DecompilerException("Unexpected EOF");
    }


    public CS2Type parseType() {
        for (CS2Type t : CS2Type.TYPE_LIST) {
            if (t.getName().equals(curr)) {
                advance();
                return t;
            }
        }
        return null;
    }

    private boolean isType(String type) {
        for (CS2Type t : CS2Type.TYPE_LIST) {
            if (t.getName().equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIdentifier(String name) {
        if (isType(name)) {
            return false;
        }
        //TODO: placeholders/keywords are not valid identifiers

        char[] c = name.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (i == 0 && !Character.isJavaIdentifierStart(c[i]) && c[i] != '@') {
                return false;
            }
            if (i > 0 && !Character.isJavaIdentifierPart(c[i])) {
                return false;
            }
        }
        return true;
    }

    //primitive-ish...
    private ExpressionNode parsePrimitive() {
        String value = curr;
        if (REG_INT.test(value)) {
            advance();
            return new IntExpressionNode((int) Long.parseLong(value)); // -2147483648 We try to parse the number part of this while we already parsed the - as unary minus
        }
        if (REG_LONG.test(value)) {
            advance();
            return new LongExpressionNode(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (REG_COLOR.test(value)) {
            advance();
            return new NewColorNode(new IntExpressionNode(Integer.decode(value)));
        }
        if (value.equals("true")) {
            advance();
            return new BooleanExpressionNode(true);
        }
        if (value.equals("false")) {
            advance();
            return new BooleanExpressionNode(false);
        }

        switch (value) {
            case "MOUSE_X":
                advance();
                return new PlaceholderValueNode(value, -2147483647, CS2Type.INT);
            case "MOUSE_Y":
                advance();
                return new PlaceholderValueNode(value, -2147483646, CS2Type.INT);
            case "CTX_WIDGET":
                advance();
                return new PlaceholderValueNode(value, -2147483645, CS2Type.WIDGET_PTR);
            case "CTX_MENU_OPTION":
                advance();
                return new PlaceholderValueNode(value, -2147483644, CS2Type.INT);
            case "CTX_WIDGET_CHILD":
                advance();
                return new PlaceholderValueNode(value, -2147483643, CS2Type.INT);
            case "DRAG_WIDGET":
                advance();
                return new PlaceholderValueNode(value, -2147483642, CS2Type.WIDGET_PTR);
            case "DRAG_WIDGET_CHILD":
                advance();
                return new PlaceholderValueNode(value, -2147483641, CS2Type.INT);
            case "KEY_TYPED":
                advance();
                return new PlaceholderValueNode(value, -2147483640, CS2Type.INT);
            case "KEY_PRESSED":
                advance();
                return new PlaceholderValueNode(value, -2147483639, CS2Type.CHAR);
        }

        if (value.equals("'")) {
            advance();
            assert curr.length() == 1 : "Single char expected";
            CharExpressionNode c = new CharExpressionNode(curr.charAt(0));
            advance();
            assert curr.equals("'") : "Single quote expected";
            advance();
            return c;
        }

        if (value.equals("null")) {
            advance();
            return new NullableIntExpressionNode(-1);
        }
        //TODO: placeholders like CTX_MENU_OPTION
        if (value.equals("\"")) {
            returnWhitespace = true;
            advance();
            StringBuilder literal = new StringBuilder();
            while (true) {
                if (curr.equals("\\")) {
                    advance();
                    //Only escape sequences are \\ for \ and \" for "
                    if (!curr.equals("\"") && !curr.equals("\\")) {
                        literal.append("\\");
                    }
                    literal.append(curr);
                } else if (!curr.equals("\"")) {
                    literal.append(curr);
                    advance();
                } else {
                    returnWhitespace = false;
                    advance();
                    break;
                }
            }
            return new StringExpressionNode(literal.toString());
        }
        return null;
    }


    public Predicate<String> REG_INT = Pattern.compile("^\\d+$").asPredicate();
    public Predicate<String> REG_LONG = Pattern.compile("^\\d+[lL]$").asPredicate();
    public Predicate<String> REG_COLOR = Pattern.compile("^0x([A-Fa-f0-9]{6})$").asPredicate();

    public String COMMA = ",";
    public String LEFT_PAREN = "(";
    public String RIGHT_PAREN = ")";
    public String SCOPE_START = "{";
    public String SCOPE_END = "}";
    public String ARRAY_IDX_START = "[";
    public String ARRAY_IDX_END = "]";
    public String SEMI = ";";
    public String DOT = ".";

    public String SWITCH = "switch";
    public String WHILE = "while";
    public String CASE = "case";
    public String DEFAULT = "default";

    public String TRUE = "true";
    public String FALSE = "false";
    public String NULL = "null";
    //TODO: Int literals widget(0,0) new Location(0,0,0) THESE CAN ALSO BE CALLED WITH AN EXPRESSION!! == cast


}
