package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("logarithm", 1, args -> {
            BigDecimal bd = requireType(BigDecimal.class, Environment.create(args.getFirst().getValue()));
            BigDecimal result = BigDecimal.valueOf(Math.log(bd.doubleValue()));
            return Environment.create(result);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Ast.Global> globals = ast.getGlobals();
        List<Ast.Function> funcs = ast.getFunctions();
        for(Ast.Global var : globals) {
            scope.defineVariable(var.getName(), var.getMutable(), visit(var.getValue().orElse(null)));
        }

        for(Ast.Function fun : funcs) {
            visit(fun);
        }

        Environment.Function main = scope.lookupFunction("main", 0);
        Environment.PlcObject r = main.invoke(new ArrayList<>());
        if(!r.getValue().equals(Environment.NIL.getValue())) {
            return r;
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        Environment.PlcObject temp;
        if(ast.getValue().isPresent()) {
            temp = visit(ast.getValue().get());
        }
        else {
            temp = Environment.NIL;
        }
        scope.defineVariable(ast.getName(), ast.getMutable(), temp);
        return Environment.NIL;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope parent = scope;
        try {
            scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
                scope = new Scope(scope);
                if(args.size() != ast.getParameters().size()) {
                    throw new RuntimeException("Expected " + ast.getParameters().size() + ", received " + args.size());
                }
                for(int i = 0; i < args.size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                }

                for(int i = 0; i < ast.getStatements().size() - 1; i++) {
                    visit(ast.getStatements().get(i));
                }

                if(ast.getName().equals("main")) {
                    if(!(ast.getStatements().getLast() instanceof Ast.Statement.Return)) {
                        return Environment.NIL;
                    }
                }

                return Environment.create(visit(ast.getStatements().getLast()).getValue());
            });
        }
        finally {
            scope = parent;
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) throws RuntimeException {
        return switch(ast.getExpression()) {
            case Ast.Expression.Literal literal -> visit(literal);
            case Ast.Expression.Group group -> visit(group);
            case Ast.Expression.Binary binary -> visit(binary);
            case Ast.Expression.Access access -> visit(access);
            case Ast.Expression.Function function -> visit(function);
            case Ast.Expression.PlcList PlcList -> visit(PlcList);
            default -> throw new RuntimeException("Invalid Expression given.");
        };
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if(ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }

        return Environment.NIL;
        // throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Ast.Expression.Access rec = requireType(Ast.Expression.Access.class, Environment.create(ast.getReceiver()));
        Environment.Variable var = scope.lookupVariable(rec.getName());
        if(var.getMutable())
            if(rec.getOffset().isPresent()) {
                List<Object> list = requireType(List.class, var.getValue());
                list.set(requireType(BigInteger.class, visit(rec.getOffset().get())).intValue(), visit(ast.getValue()).getValue());
                var.setValue(Environment.create(list));
            }
            else {
                var.setValue(visit(ast.getValue()));
            }
        else {
            throw new RuntimeException("Attempted to assign a value to an immutable variable.");
        }

        return Environment.NIL;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Boolean condition = requireType(Boolean.class, visit(ast.getCondition()));

        try {
            scope = new Scope(scope);
            if(condition) {
                for(Ast.Statement stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            }
            else {
                for(Ast.Statement stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            }
        } finally {
            scope = scope.getParent();
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        try {
            scope = new Scope(scope);
            for(Ast.Statement.Case c : ast.getCases()) {
                if(c.getValue().isPresent()) {
                    if(visit(ast.getCondition()).getValue().equals(visit(c.getValue().get()).getValue())) {
                        visit(c);
                        break;
                    }
                }
                else {
                    visit(c);
                }
            }
        } finally {
            scope = scope.getParent();
        }

        return Environment.NIL;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        for(Ast.Statement e : ast.getStatements()) {
            visit(e);
        }

        return Environment.NIL;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))) {
            Scope parent = scope;
            try {
                scope = new Scope(scope);
                for(int i = 0; i < ast.getStatements().size(); i++) {
                    visit(ast.getStatements().get(i));
                }
            } finally {
                scope = parent;
            }
        }

        return Environment.NIL;
        // throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        return new Return(visit(ast.getValue())).value;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        return Environment.create(switch(ast.getOperator()) {
            case "+" -> {
               if(visit(ast.getLeft()).getValue() instanceof BigDecimal && visit(ast.getRight()).getValue() instanceof BigDecimal) {
                   yield requireType(BigDecimal.class, visit(ast.getLeft())).add(requireType(BigDecimal.class, visit(ast.getRight())));
               }

               if(visit(ast.getLeft()).getValue() instanceof BigInteger && visit(ast.getRight()).getValue() instanceof BigInteger) {
                   yield requireType(BigInteger.class, visit(ast.getLeft())).add(requireType(BigInteger.class, visit(ast.getRight())));
               }

               if(visit(ast.getLeft()).getValue() instanceof String && visit(ast.getRight()).getValue() instanceof String) {
                   yield requireType(String.class, visit(ast.getLeft())) + requireType(String.class, visit(ast.getRight()));
               }

               throw new RuntimeException("Invalid additive operation, values are not of the same class");
            }
            case "-" -> requireType(BigDecimal.class, visit(ast.getLeft())).subtract(requireType(BigDecimal.class, visit(ast.getRight())));
            case "*" -> {
                if(visit(ast.getLeft()).getValue() instanceof BigDecimal && visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    yield requireType(BigDecimal.class, visit(ast.getLeft())).multiply(requireType(BigDecimal.class, visit(ast.getRight())));
                }

                if(visit(ast.getLeft()).getValue() instanceof BigInteger && visit(ast.getRight()).getValue() instanceof BigInteger) {
                    yield requireType(BigInteger.class, visit(ast.getLeft())).multiply(requireType(BigInteger.class, visit(ast.getRight())));
                }

                throw new RuntimeException("Invalid multiplicative operation, values are not of the same class");
            }
            case "&&" -> requireType(Boolean.class, visit(ast.getLeft())) && requireType(Boolean.class, visit(ast.getRight()));
            case "||" -> {
                Boolean l = requireType(Boolean.class, visit(ast.getLeft()));
                if(l) {
                    yield true;
                }
                else{
                    yield requireType(Boolean.class, visit(ast.getRight()));
                }
            }
            case "<" -> requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(visit(ast.getLeft()).getValue().getClass(), visit(ast.getRight()))) < 0;
            case ">" -> requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(visit(ast.getLeft()).getValue().getClass(), visit(ast.getRight()))) > 0;
            case "==" -> visit(ast.getLeft()).equals(requireType(visit(ast.getLeft()).getValue().getClass(), visit(ast.getRight())));
            case "!=" -> !visit(ast.getLeft()).equals(requireType(visit(ast.getLeft()).getValue().getClass(), visit(ast.getRight())));
            case "/" -> {
                BigDecimal l = requireType(BigDecimal.class, visit(ast.getLeft())), r = requireType(BigDecimal.class, visit(ast.getRight()));
                try {
                    yield l.divide(r, RoundingMode.HALF_EVEN);
                }
                catch(ArithmeticException err) {
                    throw new RuntimeException("Division error: " + err.getMessage());
                }
            }
            case "^" -> {
                BigInteger l = requireType(BigInteger.class, visit(ast.getLeft())), r = requireType(BigInteger.class, visit(ast.getRight()));
                BigInteger result = l;
                for(int i = 0; i < r.intValue(); i++) {
                    result = result.multiply(l);
                }
                yield result;
            }
            default -> throw new RuntimeException("Invalid binary operation.");
        });
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.PlcObject var = scope.lookupVariable(ast.getName()).getValue();
        if(ast.getOffset().isPresent()) {
            List<?> list = requireType(List.class, var);
            BigInteger offset = requireType(BigInteger.class, visit(ast.getOffset().get()));
            return Environment.create(list.get(offset.intValue()));
        }
        else {
            return Environment.create(var.getValue());
        }
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        Environment.Function func = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        List<Environment.PlcObject> args = new ArrayList<>();
        for(Ast.Expression e : ast.getArguments()) {
            args.add(visit(e));
        }

        return func.invoke(args);
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> list = new ArrayList<>();
        for(Ast.Expression e : ast.getValues()) {
            list.add(visit(e).getValue());
        }

        return Environment.create(list);
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
