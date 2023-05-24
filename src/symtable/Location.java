package symtable;

public class Location {
    int lineNo;
    int columnNo;

    public Location(int lineNo,int columnNo){
        this.lineNo=lineNo;
        this.columnNo=columnNo;
    }

    public boolean ifSame(int lineNo,int columnNo){
        if(lineNo==this.lineNo&&columnNo==this.columnNo){return true;}
        return false;
    }
}
