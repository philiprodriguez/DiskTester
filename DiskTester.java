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

    private static final Scanner scan = new Scanner(System.in);
    private static int fileSize;
    private static byte[] fileInMemory;
    private static RandomAccessFile raf;
    
    private static int totalReadFailures = 0;
    private static int totalWriteFailures = 0;
    private static int totalOtherFailures = 0;
    private static int totalWrites = 0;
    private static int totalReads = 0;

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        prepareArguments(args);
        
        Random r = new Random();
        raf = new RandomAccessFile("DiskTester.dat", "rws");
        while (true) {
            fileInMemory = new byte[fileSize];
            r.nextBytes(fileInMemory); 
            // Write
            try {
                raf.seek(0);
            } catch (IOException exc) {
                System.out.println("Failed to seek to position 0!");
                totalOtherFailures++;
                exc.printStackTrace();
                continue;
            }
            
            long start = System.currentTimeMillis();
            System.out.println("Writing bytes to file...");
            for (int i = 0; i < fileSize; i++) {
                totalWrites++;
                try {
                    raf.write(fileInMemory[i]);
                } catch (IOException exc) {
                    totalWriteFailures++;
                    exc.printStackTrace();
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("Writing finished, took " + (end-start) + "ms");
            
            // Read
            try {
                raf.seek(0);
            } catch (IOException exc) {
                System.out.println("Failed to seek to position 0!");
                totalOtherFailures++;
                exc.printStackTrace();
                continue;
            }
            start = System.currentTimeMillis();
            System.out.println("Reading all indices...");
            for (int i = 0; i < fileSize; i++) {
                totalReads++;
                try {
                    byte read = raf.readByte();
                    if (fileInMemory[i] != read) {
                        System.out.println("FAILED! Should have been " + fileInMemory[i]);
                        Thread.sleep(1000);
                    } else {
                    }
                } catch (IOException exc) {
                    totalReadFailures++;
                    System.out.println("FAILED! Could not read!");
                    exc.printStackTrace();
                }
            }
            end = System.currentTimeMillis();
            System.out.println("Reading finished, took " + (end-start) + "ms");
            System.out.println("Total Reads = " + totalReads);
            System.out.println("Total Read Failures = " + totalReadFailures);
            System.out.println("Total Writes = " + totalWrites);
            System.out.println("Total Write Failures = " + totalWriteFailures);
            System.out.println("Total Other Failures = " + totalOtherFailures);
            
            Thread.sleep(5000);
        }
    }
    
    public static void prepareArguments(String[] args) {
        if (args.length > 0) {
            if (args[0].matches("[0-9]+")) {
                fileSize = Integer.parseInt(args[0]);
            } else {
                System.out.println("File size must be a positive integer!");
            }
        } else {
            System.out.println("Enter the desired file size to test with:");
            fileSize = scan.nextInt();
        }
    }
}
