

package com.aylis.Common;

import android.os.Environment;
import java.io.File;
import java.util.regex.Pattern;

public class UtilsFileSys {

    static final char WINDOWS_SEPARATOR = '\\';
    static final char UNIX_SEPARATOR = '/';

    public static String extractFilenameExt(File file) {
        return extractFilenameExt(file.getName());
    }

    public static String extractFilenameExt(String path) {
        String ext = path;

        if (ext != null) {
            int index = ext.lastIndexOf(".");
            try {
                ext = ext.substring(index + 1);
                ext = ext.toLowerCase();
            } catch (Exception e) {
                ext = "";
            }

        } else {
            ext = "";
        }

        return ext;
    }

    public static String extractFilenameExtWithDot(String path) {
        String ext = path;

        if (ext != null) {
            int index = ext.lastIndexOf(".");
            try {
                ext = ext.substring(index);
            } catch (Exception e) {
                ext = "";
            }

            ext = ext.toLowerCase();
        } else {
            ext = "";
        }

        return ext;
    }

    public static String extractFilename(String path) {

        int index = path.lastIndexOf("/");
        String pathEnd = path;
        try {
            pathEnd = path.substring(index + 1);
        } catch (Exception ignored) {
        }

        return pathEnd;
    }

    public static String extractFilenameWithoutExt(String path) {

        int index = path.lastIndexOf("/");
        String pathEnd = path;
        try {
            pathEnd = path.substring(index + 1);
        } catch (Exception ignored) {
        }

        int index2 = pathEnd.lastIndexOf(".");
        if (index2 > 0) try {
            return pathEnd.substring(0, index2);
        } catch (Exception ignored) {
        }

        return pathEnd;
    }

    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static String getRelativePath(String targetPath, String basePath, String pathSeparator) {

        String normalizedTargetPath = targetPath;
        String normalizedBasePath = basePath;

        if (pathSeparator.equals("/")) {
            normalizedTargetPath = separatorsToUnix(normalizedTargetPath);
            normalizedBasePath = separatorsToUnix(normalizedBasePath);

        } else if (pathSeparator.equals("\\")) {
            normalizedTargetPath = separatorsToWindows(normalizedTargetPath);
            normalizedBasePath = separatorsToWindows(normalizedBasePath);

        } else {
            throw new IllegalArgumentException("Unrecognised dir separator '" + pathSeparator + "'");
        }

        String[] base = normalizedBasePath.split(Pattern.quote(pathSeparator));
        String[] target = normalizedTargetPath.split(Pattern.quote(pathSeparator));

        StringBuffer common = new StringBuffer();

        int commonIndex = 0;
        while (commonIndex < target.length && commonIndex < base.length
                && target[commonIndex].equals(base[commonIndex])) {
            common.append(target[commonIndex] + pathSeparator);
            commonIndex++;
        }

        if (commonIndex == 0) {

            throw new PathResolutionException("No common path element found for '" + normalizedTargetPath + "' and '" + normalizedBasePath
                    + "'");
        }

        boolean baseIsFile = true;

        File baseResource = new File(normalizedBasePath);

        if (baseResource.exists()) {
            baseIsFile = baseResource.isFile();

        } else if (basePath.endsWith(pathSeparator)) {
            baseIsFile = false;
        }

        StringBuilder relative = new StringBuilder();

        if (base.length != commonIndex) {
            int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

            for (int i = 0; i < numDirsUp; i++) {
                relative.append("..");
                relative.append(pathSeparator);
            }
        }
        relative.append(normalizedTargetPath.substring(common.length()));
        return relative.toString();
    }

    public static String separatorsToUnix(String path) {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR) == -1) {
            return path;
        }
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
    }

    public static String separatorsToWindows(String path) {
        if (path == null || path.indexOf(UNIX_SEPARATOR) == -1) {
            return path;
        }
        return path.replace(UNIX_SEPARATOR, WINDOWS_SEPARATOR);
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    static class PathResolutionException extends RuntimeException {
        PathResolutionException(String msg) {
            super(msg);
        }
    }
}
