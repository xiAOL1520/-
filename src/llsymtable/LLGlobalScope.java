package llsymtable;

public class LLGlobalScope extends LLBaseScope{
    public LLGlobalScope(){
        super.setIsfunc(false);
        super.setName("global");
    }
}
