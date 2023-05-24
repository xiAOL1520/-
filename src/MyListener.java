import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import symtable.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MyListener extends SysYParserBaseListener{



    int renameLineNo=0;

    int renameColumnNo=0;
    Symbol targetSymbol=null;
    LocationList resLocationList=new LocationList();

    public void setRenameLineNo(int renameLineNo) {
        this.renameLineNo = renameLineNo;
    }

    public void setRenameColumnNo(int renameColumnNo) {
        this.renameColumnNo = renameColumnNo;
    }

    public LocationList getResLocationList() {
        return resLocationList;
    }

    ParseTreeProperty<Type> typeParseTreeProperty=new ParseTreeProperty<>();
    boolean errorExit=false;
    Scope currentScope=null;
    GlobalScope globalScope=null;
    int localScopeCount=0;

    public boolean isErrorExit() {
        return errorExit;
    }

    /**
     * 1.what/how to start/enter a new scope?
     */



    @Override
    public void enterProgram(SysYParser.ProgramContext ctx) {
        globalScope=new GlobalScope();
        currentScope=globalScope;
        super.enterProgram(ctx);
    }
    @Override
    public void enterBlock(SysYParser.BlockContext ctx) {
        LocalScope localScope=new LocalScope();
        localScope.setEnclosingScope(currentScope);
        String name=localScope.getName()+localScopeCount;
        localScope.setName(name);
        localScopeCount++;

        currentScope=localScope;
        super.enterBlock(ctx);
    }
    @Override
    public void enterFuncDef(SysYParser.FuncDefContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();
        int columnno=ctx.IDENT().getSymbol().getCharPositionInLine();

        String typeName=ctx.funcType().getText();//return type name
        BaseType retype=new BaseType(typeName);

        FunctionType functionType=new FunctionType();//function type
        functionType.setRetype(retype);//return type


        String funcName=ctx.IDENT().getText();//function name
        Symbol res=currentScope.resolveMyself(funcName);

        if(res!=null){
            errorExit=true;
            System.err.println("Error type 4 at Line "+lineno+": ……");
        }
            BaseSymbol functionsymbol = new BaseSymbol(funcName, functionType);//create function symbol

            functionsymbol.addLoc(lineno,columnno);
            if(lineno==renameLineNo&&columnno==renameColumnNo){targetSymbol=functionsymbol;}

            currentScope.define(functionsymbol);

            FunctionScope functionScope = new FunctionScope();//create function scope
            functionScope.setFunctionType(functionsymbol);
            functionScope.setEnclosingScope(currentScope);
            functionScope.setName(funcName);

            currentScope = functionScope;
            typeParseTreeProperty.put(ctx, retype);
            super.enterFuncDef(ctx);
    }

    /**
     *2.when/how to exit the current scope
     */
    @Override
    public void exitProgram(SysYParser.ProgramContext ctx) {
        currentScope=currentScope.getEnclosingScope();
        if(targetSymbol!=null){
            resLocationList=targetSymbol.getUseList();
        }
        super.exitProgram(ctx);
    }

    @Override
    public void exitFuncDef(SysYParser.FuncDefContext ctx) {
        currentScope=currentScope.getEnclosingScope();
        super.exitFuncDef(ctx);
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
        currentScope=currentScope.getEnclosingScope();
        super.exitBlock(ctx);
    }

    @Override
    public void exitConstDecl(SysYParser.ConstDeclContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();


        String typeName=ctx.bType().getText();
        //globalScope.resolve(typeName);
        BaseType type=new BaseType(typeName);

        //List tmp=ctx.varDef();
        int length=ctx.constDef().size();
        for(int i=0;i<length;i++){
            int columnno=ctx.constDef().get(i).IDENT().getSymbol().getCharPositionInLine();
            String varName=ctx.constDef().get(i).IDENT().getText();
            if(currentScope==globalScope){
                Symbol res=currentScope.resolve(varName);
                if(res!=null){
                    errorExit=true;
                    System.err.println("Error type 3 at Line "+lineno+": ……");
                }
            }else{
                Symbol res=currentScope.resolveMyself(varName);
                if(res!=null){
                    errorExit=true;
                    System.err.println("Error type 3 at Line "+lineno+": ……");
                }
            }
            BaseSymbol symbol=new BaseSymbol(varName,type);

            if(ctx.constDef().get(i).constExp().size()>=1){//array type
                int dimisionMax=ctx.constDef().get(i).constExp().size();
                Type array=new ArrayType();
                ArrayType current=new ArrayType();
                for(int j=dimisionMax;j>0;j--){
                    current.setElementCount(Integer.parseInt(ctx.constDef().get(i).constExp().get(dimisionMax-j).getText()));
                    current.setDimensionCount(j);
                    if(j==1){
                        current.setElementType(type);
                        if(j==dimisionMax){array=current;}
                    }else{
                        ArrayType temp=new ArrayType();
                        current.setElementType(temp);
                        if(j==dimisionMax){array=current;}
                        current=temp;
                    }
                }
                symbol=new BaseSymbol(varName,array);
            }

            symbol.addLoc(lineno,columnno);
            if(lineno==renameLineNo&&columnno==renameColumnNo){targetSymbol=symbol;}
            currentScope.define(symbol);
        }
    }

    /**
     *3.when to define a symbol
     */



    @Override
    public void exitVarDecl(SysYParser.VarDeclContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();


        String typeName=ctx.bType().getText();
        //globalScope.resolve(typeName);
        BaseType type=new BaseType(typeName);

        //List tmp=ctx.varDef();
        int length=ctx.varDef().size();
        for(int i=0;i<length;i++){
            int columnno=ctx.varDef().get(i).IDENT().getSymbol().getCharPositionInLine();
            String varName=ctx.varDef().get(i).IDENT().getText();
            if(currentScope==globalScope){
                Symbol res=currentScope.resolve(varName);
                if(res!=null){
                    errorExit=true;
                    System.err.println("Error type 3 at Line "+lineno+": ……");
                }
            }else{
                Symbol res=currentScope.resolveMyself(varName);
                if(res!=null){
                    errorExit=true;
                    System.err.println("Error type 3 at Line "+lineno+": ……");
                }
            }
            BaseSymbol symbol=new BaseSymbol(varName,type);


            if(ctx.varDef().get(i).constExp().size()>=1){//array type
                int dimisionMax=ctx.varDef().get(i).constExp().size();
                Type array=new ArrayType();
                ArrayType current=new ArrayType();
                for(int j=dimisionMax;j>0;j--){
                    current.setElementCount(Integer.parseInt(ctx.varDef().get(i).constExp().get(dimisionMax-j).getText()));
                    current.setDimensionCount(j);
                    if(j==1){
                        current.setElementType(type);
                        if(j==dimisionMax){array=current;}
                    }else{
                        ArrayType temp=new ArrayType();
                        current.setElementType(temp);
                        if(j==dimisionMax){array=current;}
                        current=temp;
                    }
                }
                symbol=new BaseSymbol(varName,array);
            }

            symbol.addLoc(lineno,columnno);
            if(lineno==renameLineNo&&columnno==renameColumnNo){targetSymbol=symbol;}
            currentScope.define(symbol);
        }

    }

    @Override
    public void exitFuncFParam(SysYParser.FuncFParamContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();
        int columnno=ctx.IDENT().getSymbol().getCharPositionInLine();

        String typeName=ctx.bType().getText();
        //globalScope.resolve(typeName);


            FunctionScope functionScope = (FunctionScope) currentScope;
            FunctionType function = (FunctionType) functionScope.getFunctionSymbol().getType();

            BaseType type = new BaseType(typeName);
            //List tmp=ctx.varDef();
            String varName = ctx.IDENT().getText();

        Symbol res=currentScope.resolve(varName);
        if(res!=null){
            errorExit=true;
            System.err.println("Error type 3 at Line "+lineno+": ……");
        }

            BaseSymbol symbol = new BaseSymbol(varName, type);
            if (ctx.L_BRACKT().size() != 0) {
                ArrayType arrayType = new ArrayType();
                arrayType.setElementType(type);
                arrayType.setElementCount(100);
                arrayType.setDimensionCount(1);
                function.addParamType(arrayType);
                symbol = new BaseSymbol(varName, arrayType);

            } else {
                function.addParamType(type);
            }


            symbol.addLoc(lineno,columnno);
            if(lineno==renameLineNo&&columnno==renameColumnNo){targetSymbol=symbol;}
            currentScope.define(symbol);
    }



    /**
     * 4.when to resolve a symbol
     */
    @Override
    public void exitLVal(SysYParser.LValContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();
        int columnno=ctx.IDENT().getSymbol().getCharPositionInLine();

        String varName=ctx.IDENT().getText();
        Symbol res=currentScope.resolve(varName);


        if(res==null){
            errorExit=true;
            System.err.println("Error type 1 at Line "+lineno+": ……");

            BaseType type=new BaseType("int");
            typeParseTreeProperty.put(ctx,type);

        }else if(!res.getType().getTypename().equals("array")){
            if(!ctx.L_BRACKT().isEmpty()){
                errorExit=true;
                System.err.println("Error type 9 at Line "+lineno+": ……");

                BaseType type=new BaseType("int");
                typeParseTreeProperty.put(ctx,type);
            }
            typeParseTreeProperty.put(ctx,res.getType());
        }else if(res.getType().getTypename().equals("array")){
            ArrayType array=(ArrayType)res.getType();
            int maxDemision=array.getDimensionCount();
            int currentDemision=ctx.L_BRACKT().size();
            if(currentDemision>maxDemision){
                errorExit=true;
                System.err.println("Error type 9 at Line "+lineno+": ……");

                BaseType type=new BaseType("int");
                typeParseTreeProperty.put(ctx,type);
            }else if(currentDemision==maxDemision){
                typeParseTreeProperty.put(ctx, new BaseType("int"));
            }else if(currentDemision==0){
                typeParseTreeProperty.put(ctx,res.getType());
            }else{
                for(int i=0;i<currentDemision;i++){
                    array= (ArrayType) array.getElementType();
                }
                typeParseTreeProperty.put(ctx,array);
            }
        }

        if(res!=null){
            res.addLoc(lineno,columnno);
            if(lineno==renameLineNo&&columnno==renameColumnNo){targetSymbol=res;}
        }
    }

    @Override
    public void exitFunctionExp(SysYParser.FunctionExpContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();
        int columnno=ctx.IDENT().getSymbol().getCharPositionInLine();

        String funcName=ctx.IDENT().getText();
        Symbol res=currentScope.resolve(funcName);
        if(res==null) {
            errorExit=true;
            System.err.println("Error type 2 at Line "+lineno+": ……");
            BaseType type=new BaseType("int");
            typeParseTreeProperty.put(ctx,type);
        }else if(!res.getType().getTypename().equals("function")){
            errorExit=true;
            System.err.println("Error type 10 at Line "+lineno+": ……");
            BaseType type=new BaseType("int");
            typeParseTreeProperty.put(ctx,type);
        }else{




            FunctionType functionType= (FunctionType) res.getType();

            ArrayList<Type> paramsTypes=functionType.getParamstype();
            int paramsLength=paramsTypes.size();

            int inputLength=0;
            if(ctx.funcRParams()!=null){inputLength =ctx.funcRParams().param().size();}
            ArrayList<Type> inputTypes=new ArrayList<Type>();
            for(int i=0;i<inputLength;i++){
                inputTypes.add(typeParseTreeProperty.get(ctx.funcRParams().param(i).exp()));
            }


            if(paramsLength!=inputLength){
                errorExit=true;
                System.err.println("Error type 8 at Line "+lineno+": ……");
            }else{
                for(int i=0;i<paramsLength;i++){
                    String fParamTypeName=paramsTypes.get(i).getTypename();
                    String rParamTypeName=inputTypes.get(i).getTypename();
                    if(!fParamTypeName.equals(rParamTypeName)){
                        errorExit=true;
                        System.err.println("Error type 8 at Line "+lineno+": ……");
                        break;
                    }
                }
            }


            Type type=functionType.getRetype();
            typeParseTreeProperty.put(ctx,type);
        }

        if(res!=null){
            res.addLoc(lineno,columnno);
            if(lineno==renameLineNo&&columnno==renameColumnNo){targetSymbol=res;}
        }

    }

    @Override
    public void exitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String expName=typeParseTreeProperty.get(ctx.exp()).getTypename();
        if(!expName.equals("int")){
            errorExit=true;
            System.err.println("Error type 6 at Line "+lineno+": ……");
        }

        BaseType type=new BaseType("int");
        typeParseTreeProperty.put(ctx,type);
    }

    @Override
    public void exitPlusExp(SysYParser.PlusExpContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String leftName=typeParseTreeProperty.get(ctx.exp(0)).getTypename();
        String rightName=typeParseTreeProperty.get(ctx.exp(1)).getTypename();
        if(!leftName.equals("int")||!rightName.equals("int")){
            errorExit=true;
            System.err.println("Error type 6 at Line "+lineno+": ……");
        }

        BaseType type=new BaseType("int");
        typeParseTreeProperty.put(ctx,type);
    }

    @Override
    public void exitMulExp(SysYParser.MulExpContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String leftName=typeParseTreeProperty.get(ctx.exp(0)).getTypename();
        String rightName=typeParseTreeProperty.get(ctx.exp(1)).getTypename();
        if(!leftName.equals("int")||!rightName.equals("int")){
            errorExit=true;
            System.err.println("Error type 6 at Line "+lineno+": ……");
        }
        BaseType type=new BaseType("int");
        typeParseTreeProperty.put(ctx,type);
    }

    @Override
    public void exitExpCond(SysYParser.ExpCondContext ctx) {
        typeParseTreeProperty.put(ctx,typeParseTreeProperty.get(ctx.exp()));
    }

    @Override
    public void exitOrCond(SysYParser.OrCondContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String leftName=typeParseTreeProperty.get(ctx.cond(0)).getTypename();
        String rightName=typeParseTreeProperty.get(ctx.cond(1)).getTypename();
        if(!leftName.equals("int")||!rightName.equals("int")){
            errorExit=true;
            System.err.println("Error type 6 at Line "+lineno+": ……");
        }
        BaseType type=new BaseType("int");
        typeParseTreeProperty.put(ctx,type);
    }


    @Override
    public void exitAndCond(SysYParser.AndCondContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String leftName=typeParseTreeProperty.get(ctx.cond(0)).getTypename();
        String rightName=typeParseTreeProperty.get(ctx.cond(1)).getTypename();
        if(!leftName.equals("int")||!rightName.equals("int")){
            errorExit=true;
            System.err.println("Error type 6 at Line "+lineno+": ……");
        }
        BaseType type=new BaseType("int");
        typeParseTreeProperty.put(ctx,type);
    }

    @Override
    public void exitLTGTLEGECond(SysYParser.LTGTLEGECondContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String leftName=typeParseTreeProperty.get(ctx.cond(0)).getTypename();
        String rightName=typeParseTreeProperty.get(ctx.cond(1)).getTypename();
        if(!leftName.equals("int")||!rightName.equals("int")){
            errorExit=true;
            System.err.println("Error type 6 at Line "+lineno+": ……");
        }
        BaseType type=new BaseType("int");
        typeParseTreeProperty.put(ctx,type);
    }

    @Override
    public void exitEQNEQCond(SysYParser.EQNEQCondContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String leftName=typeParseTreeProperty.get(ctx.cond(0)).getTypename();
        String rightName=typeParseTreeProperty.get(ctx.cond(1)).getTypename();
        if(!leftName.equals("int")||!rightName.equals("int")){
            errorExit=true;
            System.err.println("Error type 6 at Line "+lineno+": ……");
        }
        BaseType type=new BaseType("int");
        typeParseTreeProperty.put(ctx,type);
    }

    @Override
    public void exitParenExp(SysYParser.ParenExpContext ctx) {
        typeParseTreeProperty.put(ctx,typeParseTreeProperty.get(ctx.exp()));
    }

    @Override
    public void exitLvalExp(SysYParser.LvalExpContext ctx) {
        typeParseTreeProperty.put(ctx,typeParseTreeProperty.get(ctx.lVal()));
    }

    @Override
    public void exitNumberExp(SysYParser.NumberExpContext ctx) {
        BaseType type=new BaseType("int");
        typeParseTreeProperty.put(ctx,type);
    }

    @Override
    public void exitAssignStmt(SysYParser.AssignStmtContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String leftTypeName=typeParseTreeProperty.get(ctx.lVal()).getTypename();
        if(leftTypeName.equals("function")){
            errorExit=true;
            System.err.println("Error type 11 at Line "+lineno+": ……");
        }else{
            String rightTypeName=typeParseTreeProperty.get(ctx.exp()).getTypename();
            if(leftTypeName.equals("int")){
                if(!rightTypeName.equals("int")){
                    errorExit=true;
                    System.err.println("Error type 5 at Line "+lineno+": ……");
                }
            }else if(leftTypeName.equals("array")){
                if(rightTypeName.equals("int")){
                        errorExit=true;
                        System.err.println("Error type 5 at Line "+lineno+": ……");
                }else if(rightTypeName.equals("array")){
                    ArrayType left= (ArrayType) typeParseTreeProperty.get(ctx.lVal());
                    ArrayType right= (ArrayType) typeParseTreeProperty.get(ctx.exp());
                    int leftDemision=left.getDimensionCount();
                    int rightDemision=right.getDimensionCount();
                    if(leftDemision!=rightDemision){
                        errorExit=true;
                        System.err.println("Error type 5 at Line "+lineno+": ……");
                    }
                }
            }
        }
    }

    @Override
    public void exitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        Token t = ctx.getStart();
        int lineno = t.getLine();

        String reTypeName=typeParseTreeProperty.get(ctx.exp()).getTypename();

        Scope temp=currentScope.getEnclosingScope();
        while(temp.getName().startsWith("local")){
            temp=temp.getEnclosingScope();
        }
        FunctionScope function= (FunctionScope) temp;
        FunctionType functionType= (FunctionType) function.getFunctionSymbol().getType();
        String funcReTypeName=functionType.getRetype().getTypename();

        if(!reTypeName.equals(funcReTypeName)){
            errorExit=true;
            System.err.println("Error type 7 at Line "+lineno+": ……");
        }
    }
}
