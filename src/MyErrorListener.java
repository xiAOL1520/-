import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class MyErrorListener extends BaseErrorListener {
    boolean ifHasError=false;

    public boolean isIfHasError() {
        return ifHasError;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        ifHasError=true;
        System.err.println("Error type B at Line "+line+": "+msg);
    }
}
