package llsymtable;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class LLSymbol {
    String name="";
    LLVMValueRef llvmValueRef=null;

    boolean isfunc=false;

    LLVMTypeRef reType=null;

    public void setIsfunc(boolean isfunc) {
        this.isfunc = isfunc;
    }

    public boolean isIsfunc() {
        return isfunc;
    }

    public void setReType(LLVMTypeRef reType) {
        this.reType = reType;
    }

    public LLVMTypeRef getReType() {
        return reType;
    }

    public String getName() {
        return name;
    }

    public LLVMValueRef getLlvmValueRef() {
        return llvmValueRef;
    }

    public void setLlvmValueRef(LLVMValueRef llvmValueRef) {
        this.llvmValueRef = llvmValueRef;
    }

    public void setName(String name) {
        this.name = name;
    }
}
