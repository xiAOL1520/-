import llsymtable.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import symtable.FunctionScope;

import java.util.ArrayList;
import java.util.Stack;

import static org.bytedeco.llvm.global.LLVM.*;

public class MyVisitor extends SysYParserBaseVisitor<LLVMValueRef>{



   public MyVisitor(){
       //初始化LLVM
       LLVMInitializeCore(LLVMGetGlobalPassRegistry());
       LLVMLinkInMCJIT();
       LLVMInitializeNativeAsmPrinter();
       LLVMInitializeNativeAsmParser();
       LLVMInitializeNativeTarget();
   }

    LLScope currentScope=null;
    LLGlobalScope globalScope=null;
    int localScopeCount=0;



    //创建module
    LLVMModuleRef module = LLVMModuleCreateWithName("moudle");

    public LLVMModuleRef getModule(){
        return module;
    }

    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    LLVMBuilderRef builder = LLVMCreateBuilder();

    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    LLVMTypeRef i32Type = LLVMInt32Type();

    LLVMTypeRef voidType = LLVMVoidType();
    //创建一个常量,这里是常数0
    LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);

    LLVMValueRef one = LLVMConstInt(i32Type, 1, /* signExtend */ 0);

    //创建名为globalVar的全局变量
    LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/"globalVar");

    LLWhileStack whileStack=new LLWhileStack();

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope=new LLGlobalScope();
        currentScope=globalScope;
        return super.visitProgram(ctx);
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        LLLocalScope localScope=new LLLocalScope();
        localScope.setEnclosingScope(currentScope);
        String name=localScope.getName()+localScopeCount;
        localScope.setName(name);
        localScopeCount++;

        currentScope=localScope;
        return super.visitBlock(ctx);
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //生成返回值类型
        LLVMTypeRef returnType = voidType;
        if(ctx.funcType().INT()!=null){
           returnType =i32Type;
        }

        int paramCount=0;
        if(ctx.funcFParams()!=null){paramCount=ctx.funcFParams().funcFParam().size();}
        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(paramCount);
        for(int i=0;i<paramCount;i++){
            argumentTypes.put(i,i32Type);
        }
        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ paramCount, /* isVariadic */ 0);

        //生成函数，即向之前创建的module中添加函数
        String functionName=ctx.IDENT().getText();
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/functionName, ft);

        LLSymbol functionsymbol=new LLSymbol();
        functionsymbol.setLlvmValueRef(function);
        functionsymbol.setName(functionName);
        functionsymbol.setIsfunc(true);
        functionsymbol.setReType(returnType);


        currentScope.define(functionsymbol);

        LLFunctionScope functionScope=new LLFunctionScope();
        functionScope.setName(functionName);
        functionScope.setFuncitonSymbol(functionsymbol);
        functionScope.setEnclosingScope(currentScope);
        functionScope.setReType(returnType);

        currentScope=functionScope;


        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/functionName+"Entry");
        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block1);

        /*if(returnType==voidType){
            LLVMBuildRetVoid(builder);
        }*/

        return super.visitFuncDef(ctx);
    }

    @Override
    public LLVMValueRef visitTerminal(TerminalNode node) {
        String temp=node.getText();
        String parent=node.getParent().getText();
        if(temp.equals("}")){
            currentScope=currentScope.getEnclosingScope();
            if(currentScope.isFunc()){

                LLFunctionScope functionScope= (LLFunctionScope) currentScope;

                LLVMTypeRef reType=functionScope.getReType();
                if(reType==voidType){
                    LLVMBuildRetVoid(builder);
                }

                currentScope=currentScope.getEnclosingScope();
            }
        }
        return super.visitTerminal(node);
    }

    @Override
    public LLVMValueRef visitConstDef(SysYParser.ConstDefContext ctx) {
        LLSymbol symbol=new LLSymbol();
        String constName=ctx.IDENT().getText();
        if(ctx.constExp().size()!=0){// if array
            int elementCount= (int) LLVMConstIntGetSExtValue(visit(ctx.constExp(0)));
            LLVMTypeRef vectorType = LLVMVectorType(i32Type, elementCount);
            if(currentScope==globalScope){
                LLVMValueRef globalponiter=LLVMAddGlobal(module,vectorType,constName);
                int initCount=ctx.constInitVal().constInitVal().size();
                PointerPointer valuearray=new PointerPointer(elementCount);
                for(int i=0;i<initCount;i++){
                    LLVMValueRef value=visit(ctx.constInitVal().constInitVal(i));
                    valuearray.put(i,value);
                }
                for(int i=initCount;i<elementCount;i++){
                    valuearray.put(i,zero);
                }
                LLVMSetInitializer(globalponiter,LLVMConstVector(valuearray,elementCount));

                symbol.setName(constName);
                symbol.setLlvmValueRef(globalponiter);
            }else{
            LLVMValueRef pointer = LLVMBuildAlloca(builder, vectorType, constName);

            int initCount=ctx.constInitVal().constInitVal().size();
            for(int i=0;i<initCount;i++){
                LLVMValueRef value=visit(ctx.constInitVal().constInitVal(i));
                PointerPointer valuePointer =new PointerPointer(new LLVMValueRef[]{zero,LLVMConstInt(i32Type,i,0)});
                LLVMValueRef res = LLVMBuildGEP(builder, pointer, valuePointer, 2, constName);
                LLVMBuildStore(builder,value,res);
            }
            for(int i=initCount;i<elementCount;i++){
                PointerPointer valuePointer =new PointerPointer(new LLVMValueRef[]{zero,LLVMConstInt(i32Type,i,0)});
                LLVMValueRef res = LLVMBuildGEP(builder, pointer, valuePointer, 2, constName);
                LLVMBuildStore(builder,zero,res);
            }

            symbol.setName(constName);
            symbol.setLlvmValueRef(pointer);
            }
        }else{
            LLVMValueRef value=visit(ctx.constInitVal());
            if(currentScope==globalScope){
                LLVMValueRef globalpointer=LLVMAddGlobal(module,i32Type,constName);
                LLVMSetInitializer(globalpointer,value);
                symbol.setName(constName);
                symbol.setLlvmValueRef(globalpointer);
            }else{
            LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/constName);
            LLVMBuildStore(builder, value, pointer);
            symbol.setName(constName);
            symbol.setLlvmValueRef(pointer);
            }
        }

        currentScope.define(symbol);

        return super.visitConstDef(ctx);
    }

    @Override
    public LLVMValueRef visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        LLVMValueRef res=new LLVMValueRef();
        if(ctx.constExp()!=null){
            res=visit(ctx.constExp());
        }
        return res;
    }

    @Override
    public LLVMValueRef visitVarDef(SysYParser.VarDefContext ctx) {
        LLSymbol symbol=new LLSymbol();
        String varName=ctx.IDENT().getText();
        if(ctx.constExp().size()!=0){//if array

            int elementCount= (int) LLVMConstIntGetSExtValue(visit(ctx.constExp(0)));
            LLVMTypeRef vectorType = LLVMVectorType(i32Type, elementCount);
            if(currentScope==globalScope){
                LLVMValueRef globalponiter=LLVMAddGlobal(module,vectorType,varName);
                if(ctx.ASSIGN()!=null){
                int initCount=ctx.initVal().initVal().size();
                PointerPointer valuearray=new PointerPointer(elementCount);
                for(int i=0;i<initCount;i++){
                    LLVMValueRef value=visit(ctx.initVal().initVal(i));
                    valuearray.put(i,value);
                }
                for(int i=initCount;i<elementCount;i++){
                    valuearray.put(i,zero);
                }
                LLVMSetInitializer(globalponiter,LLVMConstVector(valuearray,elementCount));
                }else{
                    PointerPointer valuearray=new PointerPointer(elementCount);
                    for(int i=0;i<elementCount;i++){
                        valuearray.put(i,zero);
                    }
                    LLVMSetInitializer(globalponiter,LLVMConstVector(valuearray,elementCount));
                }

                symbol.setName(varName);
                symbol.setLlvmValueRef(globalponiter);
            }else{
            LLVMValueRef pointer = LLVMBuildAlloca(builder, vectorType, varName);

            if(ctx.ASSIGN()!=null){
                int initCount=ctx.initVal().initVal().size();
                for(int i=0;i<initCount;i++){
                    LLVMValueRef value=visit(ctx.initVal().initVal(i));
                    PointerPointer valuePointer =new PointerPointer(new LLVMValueRef[]{zero,LLVMConstInt(i32Type,i,0)});
                    LLVMValueRef res = LLVMBuildGEP(builder, pointer, valuePointer, 2, varName);
                    LLVMBuildStore(builder,value,res);
                }
                for(int i=initCount;i<elementCount;i++){
                    PointerPointer valuePointer =new PointerPointer(new LLVMValueRef[]{zero,LLVMConstInt(i32Type,i,0)});
                    LLVMValueRef res = LLVMBuildGEP(builder, pointer, valuePointer, 2, varName);
                    LLVMBuildStore(builder,zero,res);
                }
            }
            symbol.setName(varName);
            symbol.setLlvmValueRef(pointer);
            }
        }else {
            if(currentScope==globalScope){
                LLVMValueRef globalpointer=LLVMAddGlobal(module,i32Type,varName);
                if(ctx.ASSIGN()!=null){
                    LLVMValueRef value=visit(ctx.initVal());
                    LLVMSetInitializer(globalpointer,value);
                }else{
                    LLVMSetInitializer(globalpointer,zero);
                }
                symbol.setName(varName);
                symbol.setLlvmValueRef(globalpointer);
            }else{
                LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
                if(ctx.ASSIGN()!=null){
                LLVMValueRef value=visit(ctx.initVal());
                LLVMBuildStore(builder, value, pointer);
                }
                symbol.setName(varName);
                symbol.setLlvmValueRef(pointer);
            }

        }


        currentScope.define(symbol);

        return super.visitVarDef(ctx);
    }

    @Override
    public LLVMValueRef visitInitVal(SysYParser.InitValContext ctx) {
        LLVMValueRef res=new LLVMValueRef();
        if(ctx.exp()!=null){
            res=visit(ctx.exp());
        }
        return res;
    }

    @Override
    public LLVMValueRef visitConstExp(SysYParser.ConstExpContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        int length=ctx.funcFParam().size();
        ArrayList<LLVMValueRef> fParams=new ArrayList<>();
        for(int i=0;i<length;i++){
            fParams.add(visit(ctx.funcFParam(i)));
        }

        LLFunctionScope functionScope= (LLFunctionScope) currentScope;
        LLSymbol functionSymbol=functionScope.getFuncitonSymbol();
        LLVMValueRef function=functionSymbol.getLlvmValueRef();



        for(int i=0;i<length;i++){
            LLVMValueRef n=LLVMGetParam(function,i);
            LLVMBuildStore(builder,n,fParams.get(i));
        }
        return null;
    }

    @Override
    public LLVMValueRef visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        String fParamName=ctx.IDENT().getText();

        LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/fParamName);

        LLSymbol symbol=new LLSymbol();
        symbol.setName(fParamName);
        symbol.setLlvmValueRef(pointer);

        currentScope.define(symbol);

        return pointer;
    }


    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        String name=ctx.IDENT().getText();
        LLVMValueRef res=currentScope.resolve(name).getLlvmValueRef();
        if(ctx.L_BRACKT().size()!=0){
            LLVMValueRef index=visit(ctx.exp(0));
            PointerPointer valuePointer =new PointerPointer(new LLVMValueRef[]{zero,index});
            res = LLVMBuildGEP(builder, res, valuePointer, 2, name);
        }
        return res;
    }

    @Override
    public LLVMValueRef visitLvalExp(SysYParser.LvalExpContext ctx) {
        LLVMValueRef value=visit(ctx.lVal());
        String name=ctx.lVal().IDENT().getText();
        return LLVMBuildLoad(builder,value,name);
    }

    @Override
    public LLVMValueRef visitFunctionExp(SysYParser.FunctionExpContext ctx) {
        String funcName=ctx.IDENT().getText();
        LLVMValueRef function=currentScope.resolve(funcName).getLlvmValueRef();
        LLVMValueRef functioncall=zero;
        if(ctx.funcRParams()==null){
            if(currentScope.resolve(funcName).getReType()==voidType){
                functioncall=LLVMBuildCall(builder,function,null,0,"");
            }else{
                functioncall=LLVMBuildCall(builder,function,null,0,"returnValue");
            }
        }
        else{
            int paramCount=ctx.funcRParams().param().size();
            PointerPointer paramPointer=new PointerPointer(paramCount);
            for(int i=0;i<paramCount;i++){
                paramPointer.put(i,visit(ctx.funcRParams().param(i)));
            }
            if(currentScope.resolve(funcName).getReType()==voidType){
                functioncall=LLVMBuildCall(builder,function,paramPointer,paramCount,"");
            }else{
                functioncall=LLVMBuildCall(builder,function,paramPointer,paramCount,"returnValue");
            }

        }
        return functioncall;
    }

    @Override
    public LLVMValueRef visitParam(SysYParser.ParamContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        if(ctx.exp()!=null){
            return LLVMBuildRet(builder,visit(ctx.exp()));
        }
        return LLVMBuildRetVoid(builder);
    }

    @Override
    public LLVMValueRef visitAssignStmt(SysYParser.AssignStmtContext ctx) {
        LLVMValueRef left=visit(ctx.lVal());
        LLVMValueRef right=visit(ctx.exp());
        return LLVMBuildStore(builder,right,left);
    }

    @Override
    public LLVMValueRef visitWhileStmt(SysYParser.WhileStmtContext ctx) {
        LLScope temp=currentScope.getEnclosingScope();
        while(temp.getName().startsWith("local")){
            temp=temp.getEnclosingScope();
        }
        LLFunctionScope functionscope= (LLFunctionScope) temp;
        LLSymbol functionsymbol=functionscope.getFuncitonSymbol();
        LLVMValueRef function=functionsymbol.getLlvmValueRef();



        LLVMBasicBlockRef whileCondition = LLVMAppendBasicBlock(function,"whileCondition");
        LLVMBuildBr(builder,whileCondition);
        LLVMPositionBuilderAtEnd(builder,whileCondition);

        LLVMValueRef cond=visit(ctx.cond());
        LLVMValueRef condition = LLVMBuildICmp(builder, /*这是个int型常量，表示比较的方式*/LLVMIntNE, cond, zero, "cond");

        LLVMBasicBlockRef whileBody= LLVMAppendBasicBlock(function,"whileBody");
        LLVMBasicBlockRef entry= LLVMAppendBasicBlock(function,"entry");

        whileStack.push(whileCondition,entry);

        //条件跳转指令，选择跳转到哪个块
        LLVMBuildCondBr(builder,
                /*condition:LLVMValueRef*/ condition,
                /*ifTrue:LLVMBasicBlockRef*/ whileBody,
                /*ifFalse:LLVMBasicBlockRef*/ entry);


        LLVMPositionBuilderAtEnd(builder,whileBody);
        visit(ctx.stmt());
        LLVMBuildBr(builder,whileCondition);

        LLVMPositionBuilderAtEnd(builder,entry);
        whileStack.pop();
        return null;
    }

    @Override
    public LLVMValueRef visitBreakStmt(SysYParser.BreakStmtContext ctx) {
        LLVMBuildBr(builder,whileStack.peekEntry());
        return null;
    }

    @Override
    public LLVMValueRef visitContinueStmt(SysYParser.ContinueStmtContext ctx) {
        LLVMBuildBr(builder,whileStack.peekCondition());
        return null;
    }

    @Override
    public LLVMValueRef visitIfStmt(SysYParser.IfStmtContext ctx) {
        LLScope temp=currentScope.getEnclosingScope();
        while(temp.getName().startsWith("local")){
            temp=temp.getEnclosingScope();
        }
        LLFunctionScope functionscope= (LLFunctionScope) temp;
        LLSymbol functionsymbol=functionscope.getFuncitonSymbol();
        LLVMValueRef function=functionsymbol.getLlvmValueRef();

        LLVMValueRef cond=visit(ctx.cond());
        LLVMValueRef condition = LLVMBuildICmp(builder, /*这是个int型常量，表示比较的方式*/LLVMIntNE, cond, zero, "cond");

        LLVMBasicBlockRef ifTrue = LLVMAppendBasicBlock(function,"ifTrue");
        LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(function,"ifFalse");
        //条件跳转指令，选择跳转到哪个块
        LLVMBuildCondBr(builder,
                /*condition:LLVMValueRef*/ condition,
                /*ifTrue:LLVMBasicBlockRef*/ ifTrue,
                /*ifFalse:LLVMBasicBlockRef*/ ifFalse);


        LLVMBasicBlockRef entry= LLVMAppendBasicBlock(function,"entry");

        LLVMPositionBuilderAtEnd(builder,ifTrue);
        visit(ctx.stmt(0));
        LLVMBuildBr(builder,entry);

        LLVMPositionBuilderAtEnd(builder,ifFalse);
        if(ctx.stmt(1)!=null){visit(ctx.stmt(1));}
        LLVMBuildBr(builder,entry);

        LLVMPositionBuilderAtEnd(builder,entry);
        return null;
    }

    @Override
    public LLVMValueRef visitExpCond(SysYParser.ExpCondContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitEQNEQCond(SysYParser.EQNEQCondContext ctx) {
        LLVMValueRef left=visit(ctx.cond(0));
        LLVMValueRef right=visit(ctx.cond(1));
        LLVMValueRef condres=new LLVMValueRef();
        if(ctx.EQ()!=null){//if ==
            LLVMValueRef cond=LLVMBuildICmp(builder,LLVMIntEQ,left,right,"cond");
            condres=LLVMBuildZExt(builder,cond,i32Type,"cond");
        }else{
            LLVMValueRef cond=LLVMBuildICmp(builder,LLVMIntNE,left,right,"cond");
            condres=LLVMBuildZExt(builder,cond,i32Type,"cond");
        }
        return condres;
    }

    @Override
    public LLVMValueRef visitLTGTLEGECond(SysYParser.LTGTLEGECondContext ctx) {
        LLVMValueRef left=visit(ctx.cond(0));
        LLVMValueRef right=visit(ctx.cond(1));
        LLVMValueRef condres=new LLVMValueRef();
        if(ctx.LT()!=null){
            LLVMValueRef cond=LLVMBuildICmp(builder,LLVMIntSLT,left,right,"cond");
            condres=LLVMBuildZExt(builder,cond,i32Type,"cond");
        }else if(ctx.GT()!=null){
            LLVMValueRef cond=LLVMBuildICmp(builder,LLVMIntSGT,left,right,"cond");
            condres=LLVMBuildZExt(builder,cond,i32Type,"cond");
        }else if(ctx.LE()!=null){
            LLVMValueRef cond=LLVMBuildICmp(builder,LLVMIntSLE,left,right,"cond");
            condres=LLVMBuildZExt(builder,cond,i32Type,"cond");
        } else if(ctx.GE()!=null){
            LLVMValueRef cond=LLVMBuildICmp(builder,LLVMIntSGE,left,right,"cond");
            condres=LLVMBuildZExt(builder,cond,i32Type,"cond");
        }
        return condres;
    }

    @Override
    public LLVMValueRef visitAndCond(SysYParser.AndCondContext ctx) {
        LLVMValueRef left=visit(ctx.cond(0));
        LLVMValueRef right=visit(ctx.cond(1));
        LLVMValueRef cond=LLVMBuildAnd(builder,left,right,"cond");
        LLVMValueRef condres=LLVMBuildZExt(builder,cond,i32Type,"cond");
        return condres;
    }

    @Override
    public LLVMValueRef visitOrCond(SysYParser.OrCondContext ctx) {
        LLVMValueRef left=visit(ctx.cond(0));
        LLVMValueRef right=visit(ctx.cond(1));
        LLVMValueRef cond=LLVMBuildOr(builder,left,right,"cond");
        LLVMValueRef condres=LLVMBuildZExt(builder,cond,i32Type,"cond");
        return condres;
    }

    @Override
    public LLVMValueRef visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        String operator=ctx.unaryOp().getText();
        LLVMValueRef value_org= visit(ctx.exp());
        LLVMValueRef res=zero;
        switch (operator){
            case "+":
                res=value_org;
                break;
            case "-":
                res=LLVMBuildSub(builder,zero,value_org,"result");
                break;
            case "!":
                if(value_org==zero){res=one;}
                break;
            default:
                break;
        }

        return res;
    }

    @Override
    public LLVMValueRef visitNumberExp(SysYParser.NumberExpContext ctx) {

        String number_str=ctx.number().getText();
        int number_int=0;
        if(number_str.startsWith("0")&&!number_str.equals("0")){
            if(number_str.startsWith("0X")||number_str.startsWith("0x")){
                number_int=Integer.parseInt(number_str.substring(2),16);
            }else{
                number_int=Integer.parseInt(number_str.substring(1),8);
            }
        }else{
            number_int=Integer.parseInt(number_str,10);
        }

        LLVMValueRef number=LLVMConstInt(i32Type,number_int,0);

        return number;
    }

    @Override
    public LLVMValueRef visitPlusExp(SysYParser.PlusExpContext ctx) {
        LLVMValueRef left=visit(ctx.exp(0));
        LLVMValueRef right=visit(ctx.exp(1));
        LLVMValueRef res=zero;
        if(ctx.PLUS()!=null){
            res=LLVMBuildAdd(builder,left,right,"result");
        }else{
            res=LLVMBuildSub(builder,left,right,"result");
        }

        return res;
    }

    @Override
    public LLVMValueRef visitMulExp(SysYParser.MulExpContext ctx) {
        LLVMValueRef left=visit(ctx.exp(0));
        LLVMValueRef right=visit(ctx.exp(1));
        LLVMValueRef res=zero;
        if(ctx.MUL()!=null){
            res=LLVMBuildMul(builder,left,right,"result");
        }else if(ctx.DIV()!=null){
            res=LLVMBuildSDiv(builder,left,right,"result");
        }else{
            res=LLVMBuildSRem(builder,left,right,"result");
        }
        return res;
    }

    @Override
    public LLVMValueRef visitParenExp(SysYParser.ParenExpContext ctx) {
        return visit(ctx.exp());
    }
}
