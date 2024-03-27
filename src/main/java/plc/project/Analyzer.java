package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        List<Ast.Global> globals = ast.getGlobals();
        List<Ast.Function> funcs = ast.getFunctions();
        for(Ast.Global var : globals) {
            visit(var);
            scope.defineVariable(var.getName(), var.getName(), var.getVariable().getType(), var.getMutable(), Environment.NIL);
        }

        for(Ast.Function fun : funcs) {
            visit(fun);
        }

        Environment.Function main = scope.lookupFunction("main", 0);
        Environment.PlcObject r = main.invoke(new ArrayList<>());
        //throw new UnsupportedOperationException();  // TODO
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(ast.getValue().isPresent()) {
            if(ast.getValue().get() instanceof Ast.Expression.PlcList list) {
                list.setType(Environment.getType(ast.getTypeName()));
            }
            visit(ast.getValue().get());
            requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(),
                Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL));

        //throw new UnsupportedOperationException();  // TODO
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> pTypes = new ArrayList<>();
        for(String pt : ast.getParameterTypeNames()) {
            pTypes.add(Environment.getType(pt));
        }

        if(ast.getReturnTypeName().isPresent()) {
            ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(),
                    pTypes, Environment.getType(ast.getReturnTypeName().get()), args -> Environment.NIL));
        }
        else {
            ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(),
                    pTypes, Environment.Type.NIL, args -> Environment.NIL));
        }

        Scope parent = scope;
        try {
            scope = new Scope(scope);
            for(int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), pTypes.get(i), true, Environment.NIL);
            }

            for(Ast.Statement s : ast.getStatements()) {
                visit(s);
                if(s instanceof Ast.Statement.Return r) {
                    if(ast.getReturnTypeName().isPresent())
                        requireAssignable(r.getValue().getType(), Environment.getType(ast.getReturnTypeName().get()));
                    else
                        requireAssignable(r.getValue().getType(), Environment.Type.NIL);

                    scope.defineVariable("return", "return", r.getValue().getType(), false, Environment.NIL);
                }
            }
        }
        finally {
            scope = parent;
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
         switch(ast.getExpression()) {
             case Ast.Expression.Literal literal -> visit(literal);
             case Ast.Expression.Group group -> visit(group);
             case Ast.Expression.Binary binary -> visit(binary);
             case Ast.Expression.Access access -> visit(access);
             case Ast.Expression.Function function -> visit(function);
             case Ast.Expression.PlcList PlcList -> visit(PlcList);
             default -> throw new RuntimeException("Invalid Expression given.");
         }

         return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if(ast.getTypeName().isEmpty() && ast.getValue().isEmpty()) {
            throw new RuntimeException("Variable declaration does not have a type or a value to assign type to.");
        }

        Environment.Type type = null;
        if(ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
        }

        if(!(type == null) && ast.getValue().isPresent()) {
            requireAssignable(type, ast.getValue().get().getType());}
        else if(type == null) {
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), true, Environment.NIL));
        } else {
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL));
        }

        return null;
        // throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if(!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Receiver of assignment must be an access expression.");
        }

        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getValue().getType(), ast.getReceiver().getType());
        return null;
        // throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);
        if(ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("'If' statement must be followed by a then statement(s).");
        }

        try {
            scope = new Scope(scope);
            for(Ast.Statement s : ast.getThenStatements()) {
                visit(s);
            }
        } finally {
            scope = scope.getParent();
        }

        try{
            scope = new Scope(scope);
            for(Ast.Statement s : ast.getElseStatements()) {
                visit(s);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        for(Ast.Statement.Case c : ast.getCases()) {
            if(c.getValue().isPresent()) {
                if(c.equals(ast.getCases().getLast())) {
                    throw new RuntimeException("'DEFAULT' case cannot contain a value.");
                }
                visit(c.getValue().get());
                requireAssignable(c.getValue().get().getType(), ast.getCondition().getType());
            }
        }

        for(Ast.Statement.Case c : ast.getCases()) {
            visit(c);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        try {
            scope = new Scope(scope);
            for(Ast.Statement s : ast.getStatements()) {
                visit(s);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);
        try{
            scope = new Scope(scope);
            for(Ast.Statement s : ast.getStatements()) {
                visit(s);
            }

        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        switch (ast.getLiteral()) {
            case Boolean b -> ast.setType(Environment.Type.BOOLEAN);
            case Character c -> ast.setType(Environment.Type.CHARACTER);
            case String s -> ast.setType(Environment.Type.STRING);
            case BigInteger bi -> {
                try {
                    bi.intValueExact();
                    ast.setType(Environment.Type.INTEGER);
                } catch(ArithmeticException err) {
                    throw new RuntimeException("Integer value is out of range.");
                }
            }
            case BigDecimal bd -> {
                Double d = bd.doubleValue();
                if(d.equals(Double.MAX_VALUE)) {
                    throw new RuntimeException("Decimal value is out of range.");
                }
                ast.setType(Environment.Type.DECIMAL);
            }
            case null, default -> ast.setType(Environment.Type.NIL);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if(!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("'GROUP' expression does not contain a 'BINARY' expression.");
        }

        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        switch(ast.getOperator()) {
            case "&&", "||" -> {
                requireAssignable(ast.getLeft().getType(), Environment.Type.BOOLEAN);
                requireAssignable(ast.getRight().getType(), Environment.Type.BOOLEAN);
                ast.setType(Environment.Type.BOOLEAN);
            }
            case "<", ">", "==", "!=" -> {
                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                requireAssignable(ast.getRight().getType(), ast.getLeft().getType());
                ast.setType(Environment.Type.BOOLEAN);
            }
            case "+" -> {
                if(ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                }
                else if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                    requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
                    ast.setType(ast.getLeft().getType());
                }
                else {
                    throw new RuntimeException("One or more operands is not a valid type for the operator '+'.");
                }
            }
            case "-", "*", "/" -> {
                if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                    requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
                    ast.setType(ast.getLeft().getType());
                }
                else {
                    throw new RuntimeException("One or more operands is not a valid type for the operator '" + ast.getOperator() + "'.");
                }
            }
            case "^" -> {
                requireAssignable(Environment.Type.INTEGER, ast.getLeft().getType());
                requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                ast.setType(Environment.Type.INTEGER);
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        Environment.Variable v = scope.lookupVariable(ast.getName());
        if(ast.getOffset().isPresent()) {
            visit(ast.getOffset().get());
            requireAssignable(ast.getOffset().get().getType(), Environment.Type.INTEGER);
        }
        ast.setVariable(v);

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function fun = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        ast.setFunction(fun);

        for(int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            requireAssignable(fun.getParameterTypes().get(i), ast.getArguments().get(i).getType());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        for(Ast.Expression e : ast.getValues()) {
            visit(e);
            requireAssignable(ast.getType(), e.getType());
        }

        return null;
        // throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(!target.equals(type)) {
            if(!target.equals(Environment.Type.COMPARABLE) && !target.equals(Environment.Type.ANY)) {
                throw new RuntimeException("Target type does not match type of variable. Target: " + target.getName() + ", Type: " + type.getName());
            }
        }
        // throw new UnsupportedOperationException();  // TODO
    }

}
