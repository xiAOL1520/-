package symtable;

import java.util.ArrayList;

public class ArrayType extends Type{
    String typename="array";
    int elementCount=0;

    Type elementType;

    int dimensionCount=0;

    public void setElementCount(int count){
        this.elementCount=count;
    }

    public void setDimensionCount(int dimensionCount) {
        this.dimensionCount = dimensionCount;
    }

    public void setElementType(Type elementType) {
        this.elementType = elementType;
    }

    public Type getElementType() {
        return elementType;
    }

    public String getTypename() {
        return typename;
    }

    public int getDimensionCount() {
        return dimensionCount;
    }

    public int getElementCount() {
        return elementCount;
    }
}
