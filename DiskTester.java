/*
	This is a very simplly application for testing disks. It is similar to memory testing software,
	but for disks. I am specifically making this because I keep getting odd I/O errors from three
	separate brand new USB external hard drives. I want this software to help me determine if there
	is really an issue with the USB controller or driver on the PC itself (aka, I'll try known-good
	flash drives and similar and then the hard disks).
*/

import java.util.*;
import java.io.*;

public class DiskTester {
    // Arguments to be filled in before starting
    private static int fileCount;
    private static int fileSize;
    private static String dirPath = "./";

    // Other stuff
    private static final Scanner scan = new Scanner(System.in);

    private static byte[][] filesInMemory;
    private static RandomAccessFile[] randomAccessFiles;

    private static long totalReadFailures = 0;
    private static long totalWriteFailures = 0;
    private static long totalOtherFailures = 0;
    private static long totalWrites = 0;
    private static long totalReads = 0;

    public static void main(String[] args) throws InterruptedException {
        prepareArguments(args);

        System.out.print("Allocating memory...");
        filesInMemory = new byte[fileCount][fileSize];
        System.out.println("OK");

        Random r = new Random();

        int iteration = 1;
        outer: while (true) {
            printSummary();
            Thread.sleep(5000);
            //TODO: cleanup previous iteration here

            System.out.println("Iteration " + iteration++);

            System.out.print("Determining file content...");
            for (int i = 0; i < fileCount; i++) {
                r.nextBytes(filesInMemory[i]);
            }
            System.out.println("OK");

            System.out.print("Creating files for writing...");
            randomAccessFiles = new RandomAccessFile[fileCount];
            for (int i = 0; i < fileCount; i++) {
                try {
                    randomAccessFiles[i] = new RandomAccessFile(dirPath + "DiskTester" + (i + 1) + ".dat", "rws");
                } catch (FileNotFoundException exc) {
                    System.out.println("(!!!) FAILED to create file " + (i+1) + "!");
                    totalOtherFailures++;
                    exc.printStackTrace();
                    continue outer;
                }
            }
            System.out.println("OK");

            System.out.println("Writing file content...");
            long start = System.currentTimeMillis();
            for (int i = 0; i < fileCount; i++) {
                try {
                    randomAccessFiles[i].seek(0);
                } catch (IOException exc) {
                    System.out.println("(!!!) FAILED to seek to position 0 in file " + (i+1) + "!");
                    totalOtherFailures++;
                    exc.printStackTrace();
                    continue outer;
                }

                totalWrites+=1;
                try {
                    randomAccessFiles[i].write(filesInMemory[i]);
                    randomAccessFiles[i].getFD().sync();
                    randomAccessFiles[i].close();
                } catch (IOException exc) {
                    System.out.println("(!!!) FAILED to write to file " + (i+1) + "!");
                    totalWriteFailures++;
                    exc.printStackTrace();
                    continue outer;
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("OK");
            System.out.println("Writing took " + (end-start) + "ms");

            System.out.println("Re-opening files for reading...");
            randomAccessFiles = new RandomAccessFile[fileCount];
            for (int i = 0; i < fileCount; i++) {
                try {
                    randomAccessFiles[i] = new RandomAccessFile(dirPath + "DiskTester" + (i + 1) + ".dat", "r");
                } catch (FileNotFoundException exc) {
                    System.out.println("(!!!) FAILED to open file " + (i+1) + "!");
                    totalOtherFailures++;
                    exc.printStackTrace();
                    continue outer;
                }
            }
            System.out.println("OK");

            // Read
            System.out.println("Reading file content...");
            start = System.currentTimeMillis();
            for (int i = 0; i < fileCount; i++) {
                try {
                    randomAccessFiles[i].seek(0);
                } catch (IOException exc) {
                    System.out.println("(!!!) FAILED to seek to position 0 in file " + (i+1) + "!");
                    totalOtherFailures++;
                    exc.printStackTrace();
                    continue;
                }

                byte[] readBuffer = new byte[4096];
                int bytesRead = -1;
                int placeInFile = 0;
                try {
                    while ((bytesRead = randomAccessFiles[i].read(readBuffer)) != -1) {
                        totalReads += bytesRead;

                        // Examine the bytes!
                        for (int b = 0; b < bytesRead; b++) {
                            if (filesInMemory[i][placeInFile] != readBuffer[b]) {
                                System.out.println("(!!!) FAILED! Should have been " + filesInMemory[i][placeInFile] + " but got " + readBuffer[b] + " instead!");
                                totalReadFailures++;
                                Thread.sleep(1000);
                            }
                            placeInFile++;
                        }
                    }
                } catch (IOException exc) {
                    System.out.println("(!!!) FAILED! Could not read byte!");
                    totalReadFailures++;
                    exc.printStackTrace();
                }

                // End of file reached but was that correct?
                if (placeInFile < fileSize) {
                    System.out.println("(!!!) FAILED! Premature end of file at " + placeInFile + " when it should have been at " + fileSize);
                    totalReadFailures++;
                    Thread.sleep(1000);
                }
            }
            end = System.currentTimeMillis();
            System.out.println("OK");
            System.out.println("Reading took " + (end-start) + "ms");
        }
    }

    private static void printSummary() {
        System.out.println("--------------------Summary--------------------");
        System.out.println("Total Reads = " + totalReads);
        System.out.println("Total Read Failures = " + totalReadFailures);
        System.out.println("Total Writes = " + totalWrites);
        System.out.println("Total Write Failures = " + totalWriteFailures);
        System.out.println("Total Other Failures = " + totalOtherFailures);
    }

    private static boolean isPositiveIntegerString(String s) {
        return s.matches("[0-9]+") && s.charAt(0) != '0';
    }

    private static void printUsage() {
        System.out.println("USAGE:");
        System.out.println("java DiskTester -fileSize (size in bytes) -fileCount (count integer) [-dirPath (path to working directory)]");
    }

    private static void prepareArguments(String[] args) {
        if (args.length > 0) {
            boolean fileCountSatisfied = false;
            boolean fileSizeSatisfied = false;

            for (int a = 0; a < args.length; a++) {
                if (args[a].equals("-fileSize")) {
                    if (isPositiveIntegerString(args[a+1])) {
                        fileSize = Integer.parseInt(args[a+1]);
                        a++;
                        fileSizeSatisfied = true;
                    } else {
                        System.out.println("File size must be a positive integer.");
                    }
                } else if (args[a].equals("-fileCount")) {
                    if (isPositiveIntegerString(args[a+1])) {
                        fileCount = Integer.parseInt(args[a+1]);
                        a++;
                        fileCountSatisfied = true;
                    } else {
                        System.out.println("File count must be a positive integer.");
                    }
                } else if (args[a].equals("-dirPath")) {
                    dirPath = args[a+1];
                    a++;
                    if (!dirPath.endsWith("/"))
                        dirPath += "/";
                } else {
                    System.out.println("Illegal argument at position " + a + ": " + args[0]);
                    printUsage();
                    System.exit(1);
                }
            }

            if (!(fileCountSatisfied && fileSizeSatisfied)) {
                System.out.println("One or more required arguments unsatisfied.");
                printUsage();
                System.exit(1);
            }
        } else {
            printUsage();

            System.out.println("Enter the desired file size to test with:");
            fileSize = scan.nextInt();
            System.out.println("Enter the desired file count to test with:");
            fileCount = scan.nextInt();
            System.out.println("Enter the desired working directory:");
            scan.nextLine();
            dirPath = scan.nextLine();
        }
    }
}
