package symtable;

public class FunctionScope extends BaseScope{
    BaseSymbol functionSymbol;

    public void setFunctionType(BaseSymbol functionSymbol){
        this.functionSymbol=functionSymbol;
        }

    public BaseSymbol getFunctionSymbol() {
        return functionSymbol;
    }
}
