package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");

        newline(0);
        newline(++indent);
        for(int i = 0; i < ast.getGlobals().size(); i++) {
            print(ast.getGlobals().get(i));

            if(i + 1 == ast.getGlobals().size()) {
                newline(0);
            }
        }

        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(0);
        newline(indent);

        for(int i = 0; i < ast.getFunctions().size(); i++) {
            print(ast.getFunctions().get(i));
        }

        newline(0);
        newline(0);
        print("}");

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        print(ast.getVariable().getType().getJvmName());

        if (ast.getValue().isPresent()) {
            if(ast.getValue().get() instanceof Ast.Expression.PlcList) {
                print("[]");
            }
            print(" ", ast.getName(),
                " = ", ast.getValue().get());
        }
        else {
            print(" ", ast.getName());
        }

        print(";");

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName(), " ",
                ast.getName(), "(");

        for(int i = 0; i < ast.getParameters().size(); i++) {
            print(ast.getFunction().getParameterTypes().get(i).getJvmName(), " ",
                    ast.getParameters().get(i));

            if(i + 1 != ast.getParameters().size()) {
                print(", ");
            }
        }

        print(") {");

        newline(++indent);
        for(int i = 0; i < ast.getStatements().size(); i++) {
            if(i != 0) {
                newline(indent);
            }
            print(ast.getStatements().get(i));
        }

        newline(--indent);
        print("}");

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        switch(ast.getExpression()) {
            case Ast.Expression.Literal l -> visit(l);
            case Ast.Expression.Binary b -> visit(b);
            case Ast.Expression.Group g -> visit(g);
            case Ast.Expression.Access a -> visit(a);
            case Ast.Expression.Function f -> visit(f);
            case Ast.Expression.PlcList p -> visit(p);
            default -> throw new RuntimeException("Invalid expression statement given.");
        }

        print(";");
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(),
            " ",
            ast.getVariable().getJvmName());

        if(ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(),
            " = ",
            ast.getValue(),
            ";");

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (",
            ast.getCondition(),
            ") {");

        newline(++indent);
        for(int i = 0; i < ast.getThenStatements().size(); i++) {
            if(i != 0) {
                newline(indent);
            }
            print(ast.getThenStatements().get(i));
        }

        newline(--indent);
        print("}");

        if(!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(++indent);
            for(int i = 0; i < ast.getElseStatements().size(); i++) {
                if(i != 0) {
                    newline(indent);
                }
                print(ast.getElseStatements().get(i));
            }
            newline(--indent);
            print("}");
        }

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (",
            ast.getCondition(),
            ") {");

        newline(++indent);
        for(int i = 0; i < ast.getCases().size(); i++) {
            visit(ast.getCases().get(i));

            if(i + 1 != ast.getCases().size()) {
                newline(--indent);
            }
            else {
                --indent;
            }
        }

        newline(--indent);
        print("}");
        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if(ast.getValue().isPresent()) {
            print("case ", ast.getValue().get(), ":");
        }
        else {
            print("default:");
        }

        newline(++indent);
        for(int i = 0; i < ast.getStatements().size(); i++) {
            if(i != 0) {
                newline(indent);
            }
            print(ast.getStatements().get(i));
        }

        if(ast.getValue().isPresent()) {
            newline(indent);
            print("break;");
        }

        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");

        if(!ast.getStatements().isEmpty()) {
            newline(++indent);
            for(int i = 0; i < ast.getStatements().size(); i++) {
                if(i != 0) {
                    newline(indent);
                }
                visit(ast.getStatements().get(i));
                newline(indent);
            }
            newline(--indent);
        }

        print("}");
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        switch(ast.getLiteral()) {
            case Boolean b -> print(b);
            case Character c -> print("'", c, "'");
            case String s -> print("\"", s, "\"");
            case BigInteger bi -> print(bi);
            case BigDecimal bd -> print(bd);
            case null, default -> print("null");
        }

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (ast.getOperator().equals("^")) {
            print("Math.pow(");
            visit(ast.getLeft());
            visit(ast.getRight());
        } else {
            visit(ast.getLeft());
            print(" ", ast.getOperator(), " ");
            visit(ast.getRight());
        }

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getName());
        if(ast.getOffset().isPresent()) {
            print("[");
            visit(ast.getOffset().get());
            print("]");
        }

        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        if(ast.getName().equals("print")) {
            print("System.out.println(");
        }
        else {
            print(ast.getName(), "(");
        }

        for(int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if(i + 1 != ast.getArguments().size()) {
                print(", ");
            }
        }

        print(")");
        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        for(int i = 0; i < ast.getValues().size(); i++) {
            visit(ast.getValues().get(i));
            if(i + 1 != ast.getValues().size()) {
                print(", ");
            }
        }
        print("}");
        return null;
        // throw new UnsupportedOperationException(); //TODO
    }

}
