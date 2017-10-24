package drools;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class LogToFile {
    private static final String FILENAME = "logs/droolsLog.log";
    public static void write(String s){
        try {
            s=s+"\n";
            Files.write(Paths.get(FILENAME), s.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }
}