package com.dotmarketing.compilers;

import java.io.FileNotFoundException;

import com.dotmarketing.exception.DotRuntimeException;

public class DotJdtCompiler {
	
       /** 
        * Compiles the given java file and generates the class file on the given output dir
        * @return A list of compilation problems if that list contains compilation errors it might be that the class
        * couldn't get compiled
        */
       public static DotCompilationProblems compileClass(String mySourceFile, String myClassName, String myOutputDir)
           throws FileNotFoundException, DotRuntimeException {
    	   return null;
       }
       
}
