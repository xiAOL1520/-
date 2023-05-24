package llsymtable;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;

public class LLFunctionScope extends LLBaseScope{
    LLSymbol funcitonSymbol=null;

    LLVMTypeRef reType=new LLVMTypeRef();

    public LLFunctionScope(){
        super.setIsfunc(true);
    }
    public void setFuncitonSymbol(LLSymbol funcitonSymbol) {
        this.funcitonSymbol = funcitonSymbol;
    }

    public LLSymbol getFuncitonSymbol() {
        return funcitonSymbol;
    }

    public LLVMTypeRef getReType() {
        return reType;
    }

    public void setReType(LLVMTypeRef reType) {
        this.reType = reType;
    }
}
