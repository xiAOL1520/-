package llsymtable;

public class LLLocalScope extends LLBaseScope{
    public LLLocalScope(){
        super.setIsfunc(false);
        super.setName("local");
    }
}
