package net.rim.tools.compiler;

import java.io.*;

public class Compiler {
    private static final String IMPORT_PREFIX = "-import=";
    private static final String EXEPATH_PREFIX = "-exepath=";
    private static final String COMPONENT_PACK_PREFIX = "net.rim.ejde.componentpack";
    private static final String RIM_JAR = "net_rim_api.jar";
    private static final String RAPC_ORIG_JAR = "rapc-orig.jar";

    public static void main(String[] args) throws Exception {
        String[] updatedArgs = new String[args.length];
        String exePath = null;
        String componentPack = null;

        for(int i=0; i<args.length; i++) {
            if(args[i].startsWith(EXEPATH_PREFIX)) {
                exePath = args[i].substring(EXEPATH_PREFIX.length()) + "\\";

                int p = exePath.indexOf(COMPONENT_PACK_PREFIX);
                if(p == -1) { break; }
                int q = exePath.indexOf('\\', p);
                if(q == -1) { break; }
                componentPack = exePath.substring(p, q);

                break;
            }
        }

        if(exePath == null || componentPack == null) {
            System.err.println("Unable to parse execution path!");
            System.exit(1);
        }

        for(int i=0; i<args.length; i++) {
            if(args[i].startsWith(IMPORT_PREFIX)) {
                updatedArgs[i] = fixImports(componentPack, args[i]);
            }
            else {
                updatedArgs[i] = args[i];
            }
        }

        int result = runRapc(exePath, updatedArgs);
        System.exit(result);
    }

    private static String fixImports(String componentPack, String line) {
        String[] imports = line.substring(IMPORT_PREFIX.length()).split(";");
        StringBuilder buf = new StringBuilder();
        buf.append(IMPORT_PREFIX);

        for(int i=0; i<imports.length; i++) {
            // Only include an API JAR from this module's component pack
            if(imports[i].endsWith(RIM_JAR)) {
                if(imports[i].indexOf(componentPack) != -1) {
                    buf.append(imports[i]);
                    buf.append(';');
                }
            }
            else {
                buf.append(imports[i]);
                buf.append(';');
            }
        }

        String newLine = buf.toString();
        return newLine.substring(0, newLine.length() - 1);
    }

    private static int runRapc(String exePath, String[] args) throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append("java -jar ");
        buf.append(exePath);
        buf.append(RAPC_ORIG_JAR);
        for(int i=0; i<args.length; i++) {
            buf.append(' ').append(args[i]);
        }
        Process p = Runtime.getRuntime().exec(buf.toString());

        String s = null;
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        while ((s = stdError.readLine()) != null) {
            System.err.println(s);
        }

        int result;
        try {	
            result = p.waitFor();
        } catch (Exception e) {
            result = 1;
        }
        return result;
    }
}
