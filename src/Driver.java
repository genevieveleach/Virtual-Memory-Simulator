import java.util.*;
import java.io.*;

public class Driver {
  public static void main(String[] args) throws FileNotFoundException {
    String addr;
    int rw;
    String value;
    int soft;
    int hard;
    int hit;
    String evicted;
    String dirty;
    String path = args[0];
    File file = new File(path);
    Scanner input = new Scanner(file);
    PrintStream csv = new PrintStream(new FileOutputStream("Output.txt"));
    System.setOut(csv);
    System.out.println("Address,r/w,value,soft,hard,hit,evicted_pg#,dirty_evicted_page");
  }

}
