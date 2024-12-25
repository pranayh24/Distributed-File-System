package org.prh.dfs.previous;

import java.io.File;

public class FilePermissionChecker {

    public static boolean hasReadWritePermissions(String path) {
        File file = new File(path);
        return file.canRead() && file.canWrite();
    }

    public static void main(String[] args) {
        String path = "D:\\seriess\\file.txt";
        if (hasReadWritePermissions(path)) {
            System.out.println("Read and write permissions are available for the path: " + path);
        } else {
            System.out.println("Read and/or write permissions are not available for the path: " + path);
        }
    }
}
