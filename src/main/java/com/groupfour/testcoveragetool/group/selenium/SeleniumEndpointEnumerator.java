package com.groupfour.testcoveragetool.group.selenium;

import java.io.File;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Strings;
import com.groupfour.testcoveragetool.controller.CoverageController;
import com.groupfour.testcoveragetool.controller.EndpointInfo;
import com.groupfour.testcoveragetool.group.APIType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class SeleniumEndpointEnumerator {

    public static Set<String> validEndpoints = new HashSet<String>(Arrays.asList("GetMapping",
                                                                                "PostMapping",
                                                                                "PutMapping",
                                                                                "PatchMapping",
                                                                                //"RequestMapping",
                                                                                "DeleteMapping"));

    public static void listClasses(File projectDir) throws IOException {

        JavaParser x = new JavaParser();

        new DirectoryTraverser((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                        super.visit(n, arg);
                        System.out.println(" * " + n.getName());

                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (IOException e) {
                new RuntimeException(e);
            }
        }).explore(projectDir);
    }

    public static ArrayList<EndpointInfo> listApiAnnotations(File projectFile) throws IOException {
        ArrayList<EndpointInfo> toReturn = new ArrayList<>();

	    if(projectFile.isDirectory()) {
	        new DirectoryTraverser((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
	            System.out.println(path);
	            System.out.println(Strings.repeat("=", path.length()));
	            try {
	                new VoidVisitorAdapter<Object>() {
	                    @Override
	                    public void visit(MethodDeclaration n, Object arg) {
	                        super.visit(n, arg);
	
	                        SimpleName candidate = n.getName();
	
	                        if(validEndpoints.contains(candidate.toString())) {
	                            toReturn.add(printApiInformation(n));
	                        }
	                    }
	                }.visit(StaticJavaParser.parse(file), null);
	                System.out.println(); // empty line
	            } catch (IOException e) {
	                new RuntimeException(e);
	            }
	        }).explore(projectFile);
        }
	    else {
	    	try {
                new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(MethodDeclaration n, Object arg) {
                        super.visit(n, arg);

                        SimpleName candidate = n.getName();

                        if(validEndpoints.contains(candidate.toString())) {
                            toReturn.add(printApiInformation(n));
                        }
                    }
                }.visit(StaticJavaParser.parse(projectFile), null);
                System.out.println(); // empty line
            } catch (IOException e) {
                new RuntimeException(e);
            }
	    }

        CoverageController.setSelenium(toReturn);
        return toReturn;
    }

    @interface X { int id(); }

    public static EndpointInfo printApiInformation(MethodDeclaration n) {

        APIType type = APIType.UNDEFINED;

        //System.out.println(n.getName().toString());
        switch(n.getName().toString()) {
            case "GetMapping":
                type = APIType.GET;
                break;
            case "RequestBody":
//            case "RequestMapping":
//                type = APIType.REQUEST;
//                break;
            case "PostMapping":
                type = APIType.POST;
                break;
            case "PutMapping":
                type = APIType.PUT;
                break;
            case "DeleteMapping":
                type = APIType.DELETE;
                break;
            case "PatchMapping":
                type = APIType.PATCH;
                break;
        }

        String endpointPath = "";

        if(type != APIType.UNDEFINED) {
            System.out.println(type + " API call found through: " + n.getName());
            endpointPath = n.toString().substring(n.toString().indexOf("(") + 1, n.toString().length() - 1);
            System.out.println("\t- Call using HTTP Path extension: " + endpointPath + "\n");
        }
        else {
            System.out.println("*ISSUE*: " + n.getName() + " API call found which is undefined in the scope");
        }

        return new EndpointInfo(type.toString(), endpointPath);
    }

    @Deprecated
    public static void main(String[] args) throws IOException {
        File projectDir = new File("./../../SeleniumSample");
        //listClasses(projectDir);
        listApiAnnotations(projectDir);
    }
}
