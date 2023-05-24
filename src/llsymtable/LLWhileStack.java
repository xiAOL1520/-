package llsymtable;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;

import java.util.Stack;

public class LLWhileStack {
    Stack whileConditions=new Stack();
    Stack whileEntry=new Stack();

    public void push(LLVMBasicBlockRef whileConditon,LLVMBasicBlockRef entry){
        whileConditions.push(whileConditon);
        whileEntry.push(entry);
    }

    public LLVMBasicBlockRef peekCondition(){
        return (LLVMBasicBlockRef) whileConditions.peek();
    }

    public LLVMBasicBlockRef peekEntry(){
        return (LLVMBasicBlockRef) whileEntry.peek();
    }

    public void pop(){
        whileConditions.pop();
        whileEntry.pop();
    }
}
