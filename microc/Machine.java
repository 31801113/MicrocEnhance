/* File MicroC/Machine.java
   A unified-stack abstract machine for imperative programs.
   sestoft@itu.dk * 2001-03-21, 2009-09-24

   To execute a program file using this abstract machine, do:

      java Machine <programfile> <arg1> <arg2> ...

   or, to get a trace of the program execution:

      java Machinetrace <programfile> <arg1> <arg2> ...

*/
import java.io.*;
import java.util.*;
import java.lang.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

class Machine {
  public static void main(String[] args)        
    throws FileNotFoundException, IOException,OperatorError, ImcompatibleTypeError  {
    if (args.length == 0) 
      System.out.println("Usage: java Machine <programfile> <arg1> ...\n");
    else
      execute(args, false);
  }

  // These numeric instruction codes must agree with Machine.fs:

  final static int 
    CSTI = 0, ADD = 1, SUB = 2, MUL = 3, DIV = 4, MOD = 5, 
    EQ = 6, LT = 7, NOT = 8, 
    DUP = 9, SWAP = 10, 
    LDI = 11, STI = 12, 
    GETBP = 13, GETSP = 14, INCSP = 15, 
    GOTO = 16, IFZERO = 17, IFNZRO = 18, CALL = 19, TCALL = 20, RET = 21, 
    PRINTI = 22, PRINTC = 23, 
    LDARGS = 24,
    STOP = 25,CSTC = 26;


  final static int STACKSIZE = 1000;
  
  // Read code from file and execute it

  static void execute(String[] args, boolean trace) 
    throws FileNotFoundException, IOException, OperatorError, ImcompatibleTypeError{
    int[] p = readfile(args[0]);                // Read the program from file
    // int[] s = new int[STACKSIZE];               // The evaluation stack
    // int[] iargs = new int[args.length-1];
    // for (int i=1; i<args.length; i++)          // Push commandline arguments
    //   iargs[i-1] = Integer.parseInt(args[i]);

    BaseType[] s = new BaseType[STACKSIZE];               // The evaluation stack
    BaseType[] iargs = new BaseType[args.length-1];

    for (int i = 1; i < args.length; i++) {
            if(Pattern.compile("(?i)[a-z]").matcher(args[i]).find()){
                char[] input = args[i].toCharArray();
                CharType[] array = new CharType[input.length];
                for(int j = 0; j < input.length; ++j) {
                    array[j] = new CharType(input[j]);
                }
                iargs[i-1] = new ArrayType(array);
            }
            else if(args[i].contains(".")){
                iargs[i-1] = new FloatType(new Float(args[i]).floatValue());
            }
            else {
                iargs[i-1] = new IntType(new Integer(args[i]).intValue());
            }
        }

    long starttime = System.currentTimeMillis();
    execcode(p, s, iargs, trace);            // Execute program proper
    long runtime = System.currentTimeMillis() - starttime;
    System.err.println("\nRan " + runtime/1000.0 + " seconds");
  }

  // The machine: execute the code starting at p[pc] 

  static int execcode(int[] p, BaseType[] s, BaseType[] iargs, boolean trace) throws ImcompatibleTypeError, OperatorError {
    int bp = -999;	// Base pointer, for local variable access 
    int sp = -1;	// Stack top pointer
    int pc = 0;		// Program counter: next instruction
    int hr = -1;
    for (;;) {
      if (trace) 
        printsppc(s, bp, sp, p, pc);
      switch (p[pc++]) {
      case CSTI:
       s[sp+1] = new IntType(p[pc++]); sp++; break;
      case CSTC:
        s[sp+1] =new CharType((char)(p[pc++])); sp++; break;
      case ADD: 
        s[sp-1] = binaryOperator(s[sp-1], s[sp], "+"); sp--; break;
      case SUB: 
        s[sp-1] = binaryOperator(s[sp-1], s[sp], "-"); sp--; break;
      case MUL: 
        s[sp-1] = binaryOperator(s[sp-1], s[sp], "*"); sp--; break;
      case DIV: 
        if(((IntType)s[sp]).getValue()==0)
                    {
                        System.out.println("hr:"+hr+" exception:"+1);
                        while (hr != -1 && ((IntType)s[hr]).getValue() != 1 )
                        {
                            hr = ((IntType)s[hr+2]).getValue();
                            System.out.println("hr:"+hr+" exception:"+new IntType(p[pc]).getValue());
                        }
                            
                        if (hr != -1) { 
                            sp = hr-1;    
                            pc = ((IntType)s[hr+1]).getValue();
                            hr = ((IntType)s[hr+2]).getValue();    
                        } else {
                            System.out.print(hr+"not find exception");
                            return sp;
                        }
                    }
                    else{
                        s[sp - 1] = binaryOperator(s[sp-1], s[sp], "/");
                        sp--; 
                    }
                    break;
      case MOD: 
        s[sp-1] = binaryOperator(s[sp-1], s[sp], "%"); sp--; break;
      case EQ: 
        s[sp-1] = binaryOperator(s[sp-1], s[sp], "=="); sp--; break;
      case LT: 
        s[sp-1] = binaryOperator(s[sp-1], s[sp], "<"); sp--; break;
      case NOT: 
        Object result = null;
                    if(s[sp] instanceof FloatType){
                        result = ((FloatType)s[sp]).getValue();
                    }else if (s[sp] instanceof IntType){
                        result = ((IntType)s[sp]).getValue();
                    }
                    s[sp] = (Float.compare(new Float(result.toString()), 0.0f) == 0 ? new IntType(1) : new IntType(0));
                    break;
      case DUP: 
        s[sp+1] = s[sp]; sp++; break;
      case SWAP: 
        BaseType tmp = s[sp];  s[sp] = s[sp-1];  s[sp-1] = tmp;
                    break;
      case LDI:                 // load indirect
        s[sp] = s[((IntType)s[sp]).getValue()]; break;
      case STI:                 // store indirect, keep value on top
        s[((IntType)s[sp-1]).getValue()] = s[sp]; s[sp-1] = s[sp]; sp--; break;
      case GETBP:
        s[sp+1] = new IntType(bp); sp++; break;
      case GETSP:
        s[sp+1] = new IntType(sp); sp++; break;
      case INCSP:
        sp = sp+p[pc++]; break;
      case GOTO:
        pc = p[pc]; break;
      case IFZERO:
        Object result1 = null;
                    int index = sp--;
                    if(s[index] instanceof IntType){
                        result1 = ((IntType)s[index]).getValue();
                    }else if(s[index] instanceof FloatType){
                        result1 = ((FloatType)s[index]).getValue();
                    }
                    pc = (Float.compare(new Float(result1.toString()), 0.0f) == 0 ? p[pc] : pc + 1);
                    break;
      case IFNZRO:
        Object result2 = null;
                    int index1 = sp--;
                    if (s[index1] instanceof IntType) {
                        result2 = ((IntType) s[index1]).getValue();
                    } else if (s[index1] instanceof FloatType) {
                        result2 = ((FloatType) s[index1]).getValue();
                    }
                    pc = (Float.compare(new Float(result2.toString()), 0.0f) != 0 ? p[pc] : pc + 1);
                    break;
      case CALL: 
      int argc = p[pc++];
                    for (int i=0; i<argc; i++)
                        s[sp-i+2] = s[sp-i];
                    s[sp-argc+1] = new IntType(pc+1); sp++;
                    s[sp-argc+1] = new IntType(bp);   sp++;
                    bp = sp+1-argc;
                    pc = p[pc];
                    break;
      case TCALL: { 
        int argc1 = p[pc++];
                    int pop  = p[pc++];
                    for (int i=argc1-1; i>=0; i--)
                        s[sp-i-pop] = s[sp-i];
                    sp = sp - pop; pc = p[pc];
      } break; 
      case RET: { 
        BaseType res = s[sp];
                    sp = sp - p[pc]; bp = ((IntType)s[--sp]).getValue(); pc = ((IntType)s[--sp]).getValue();
                    s[sp] = res;
      } break; 
      case PRINTI:
        Object result3;
                    if(s[sp] instanceof IntType){
                        result3 = ((IntType)s[sp]).getValue();
                    }else if(s[sp] instanceof FloatType){
                        result3 = ((FloatType)s[sp]).getValue();
                    }else {
                        result3 = ((CharType)s[sp]).getValue();
                    }

                    System.out.print(String.valueOf(result3) + " ");
                    break;
      case PRINTC:
        System.out.print((((CharType)s[sp])).getValue()); break;
      case LDARGS:
	for (int i=0; i < iargs.length; i++) // Push commandline arguments
                        s[++sp] = iargs[i];
                    break;
      case STOP:
        return sp;
      default:                  
        throw new RuntimeException("Illegal instruction " + p[pc-1] 
                                   + " at address " + (pc-1));
      }
    }
  }
  
  public static BaseType binaryOperator(BaseType lhs, BaseType rhs, String operator) throws ImcompatibleTypeError, OperatorError {
        Object left;
        Object right;
        int flag = 0;
        if (lhs instanceof FloatType) {
            left = ((FloatType) lhs).getValue();
            flag = 1;
        } else if (lhs instanceof IntType) {
            left = ((IntType) lhs).getValue();
        } else {
            throw new ImcompatibleTypeError("ImcompatibleTypeError: Left type is not int or float");
        }

        if (rhs instanceof FloatType) {
            right = ((FloatType) rhs).getValue();
            flag = 1;
        } else if (rhs instanceof IntType) {
            right = ((IntType) rhs).getValue();
        } else {
            throw new ImcompatibleTypeError("ImcompatibleTypeError: Right type is not int or float");
        }
        BaseType result = null;

        switch(operator){
            case "+":{
                if (flag == 1) {
                    result =  new FloatType(Float.parseFloat(String.valueOf(left)) + Float.parseFloat(String.valueOf(right)));
                } else {
                    result = new IntType(Integer.parseInt(String.valueOf(left)) + Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "-":{
                if (flag == 1) {
                    result = new FloatType(Float.parseFloat(String.valueOf(left)) - Float.parseFloat(String.valueOf(right)));
                } else {
                    result = new IntType(Integer.parseInt(String.valueOf(left)) - Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "*":{
                if (flag == 1) {
                    result = new FloatType(Float.parseFloat(String.valueOf(left)) * Float.parseFloat(String.valueOf(right)));
                } else {
                    result = new IntType(Integer.parseInt(String.valueOf(left)) * Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "/":{
                if(Float.compare(Float.parseFloat(String.valueOf(right)), 0.0f) == 0){
                    throw new OperatorError("OpeatorError: Divisor can't not be zero");
                }
                if (flag == 1) {
                    result = new FloatType(Float.parseFloat(String.valueOf(left)) / Float.parseFloat(String.valueOf(right)));
                } else {
                    result = new IntType(Integer.parseInt(String.valueOf(left)) / Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "%":{
                if (flag == 1) {
                    throw new OperatorError("OpeatorError: Float can't mod");
                } else {
                    result = new IntType(Integer.parseInt(String.valueOf(left)) % Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "==":{
                if (flag == 1) {
                    if((float) left == (float) right){
                        result = new IntType(1);
                    }
                    else{
                        result = new IntType(0);
                    }
                } else {
                    if((int) left == (int) right){
                        result = new IntType(1);
                    }
                    else{
                        result = new IntType(0);
                    }
                }
                break;
            }
            case "<":{
                if (flag == 1) {
                    if((float) left < (float) right){
                        result = new IntType(1);
                    }
                    else{
                        result = new IntType(0);
                    }
                } else {
                    if((int) left < (int) right){
                        result = new IntType(1);
                    }
                    else{
                        result = new IntType(0);
                    }
                }
                break;
            }
        }
        return result;
    }

  // Print the stack machine instruction at p[pc]

  static String insname(int[] p, int pc) {
    switch (p[pc]) {
    case CSTI:   return "CSTI " + p[pc+1]; 
    case CSTC:   return "CSTC " + (char)(p[pc+1]); 
    case ADD:    return "ADD";
    case SUB:    return "SUB";
    case MUL:    return "MUL";
    case DIV:    return "DIV";
    case MOD:    return "MOD";
    case EQ:     return "EQ";
    case LT:     return "LT";
    case NOT:    return "NOT";
    case DUP:    return "DUP";
    case SWAP:   return "SWAP";
    case LDI:    return "LDI";
    case STI:    return "STI";
    case GETBP:  return "GETBP";
    case GETSP:  return "GETSP";
    case INCSP:  return "INCSP " + p[pc+1];
    case GOTO:   return "GOTO " + p[pc+1];
    case IFZERO: return "IFZERO " + p[pc+1];
    case IFNZRO: return "IFNZRO " + p[pc+1];
    case CALL:   return "CALL " + p[pc+1] + " " + p[pc+2];
    case TCALL:  return "TCALL " + p[pc+1] + " " + p[pc+2] + " " + p[pc+3];
    case RET:    return "RET " + p[pc+1];
    case PRINTI: return "PRINTI";
    case PRINTC: return "PRINTC";
    case LDARGS: return "LDARGS";
    case STOP:   return "STOP";
    default:     return "<unknown>";
    }
  }

  // Print current stack and current instruction

  static void printsppc(BaseType[] s, int bp, int sp, int[] p, int pc) {
    System.out.print("[ ");
        for (int i = 0; i <= sp; i++) {
            Object result = null;
            if(s[i] instanceof IntType){
                result = ((IntType)s[i]).getValue();
            }else if(s[i] instanceof FloatType){
                result = ((FloatType)s[i]).getValue();
            }else if(s[i] instanceof CharType){
                result = ((CharType)s[i]).getValue();
            }
            System.out.print(String.valueOf(result) + " ");
        }
        System.out.print("]");
        System.out.println("{" + pc + ": " + insname(p, pc) + "}");
  }


  // Read instructions from a file

  public static int[] readfile(String filename) 
    throws FileNotFoundException, IOException
  {
    ArrayList<Integer> rawprogram = new ArrayList<Integer>();
    Reader inp = new FileReader(filename);
    StreamTokenizer tstream = new StreamTokenizer(inp);
    tstream.parseNumbers();
    tstream.nextToken();
    while (tstream.ttype == StreamTokenizer.TT_NUMBER) {
      rawprogram.add(new Integer((int)tstream.nval));
      tstream.nextToken();
    }
    inp.close();
    final int programsize = rawprogram.size();
    int[] program = new int[programsize];
    for (int i=0; i<programsize; i++)
      program[i] = ((Integer)(rawprogram.get(i))).intValue();
    return program;
  }
}

// Run the machine with tracing: print each instruction as it is executed

class Machinetrace {
  public static void main(String[] args)        
    throws FileNotFoundException, IOException, OperatorError, ImcompatibleTypeError {
    if (args.length == 0) 
      System.out.println("Usage: java Machinetrace <programfile> <arg1> ...\n");
    else
      Machine.execute(args, true);
  }
}
