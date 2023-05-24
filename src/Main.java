import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import symtable.LocationList;

import java.io.IOException;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.*;

public class Main
{
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        String target=args[1];//res path

        CharStream input = CharStreams.fromFileName(source);// get input

        SysYLexer sysYLexer = new SysYLexer(input);//create lexer
        CommonTokenStream tokenStream = new CommonTokenStream(sysYLexer);//get tokenstream
        SysYParser sysYParser=new SysYParser(tokenStream);//create parser

        MyErrorListener myErrorListener=new MyErrorListener();
        sysYParser.removeErrorListeners();
        sysYParser.addErrorListener(myErrorListener);//add my error listener



        String[] parserRuleNames=sysYParser.getRuleNames();//get parserRuleNames
        String[] lexerRuleNames=sysYLexer.getRuleNames();//get lexerRuleNames





        ParseTree tree = sysYParser.program();
        if(myErrorListener.isIfHasError()) return;//if has any error then return

/*        ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
        MyListener listener= new MyListener();

        walker.walk(listener, tree); // initiate walk of tree with listener
        LocationList renameTargetLocationList=listener.getResLocationList();

        if(listener.isErrorExit())return;*/

        MyVisitor visitor = new MyVisitor();//my visitor
        visitor.visit(tree);

        final BytePointer error = new BytePointer();
        LLVMModuleRef module=visitor.getModule();
        //LLVMDumpModule(module);
        if (LLVMPrintModuleToFile(module, target, error) != 0) {    // moudle是你自定义的LLVMModuleRef对象
            LLVMDisposeMessage(error);
        }


    }

}