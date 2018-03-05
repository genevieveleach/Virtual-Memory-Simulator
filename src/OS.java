import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;

public class OS {
  static int clockIndex;
  
  static void initPages() throws IOException {
      clockIndex = 0;
      String temp;
      for( int i = 0; i < 256; i++) {
          if(i < 16) {
            temp = "0" + Integer.toHexString(i).toUpperCase();
          }
          else {
            temp = Integer.toHexString(i).toUpperCase();
          }
          
          String src = "../page_files/original/" + temp + ".pg";
          String dest = "../page_files/copy/" + temp + ".pg";
          Files.copy(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
      }
  }

    //this function takes data from the file, then inputs it into physical memory
    private static void writeToPhysicalMem(int pageNum, int newOwnerOfPage) throws IOException {
        // open correct file from folder
        File inputPage = new File("../page_files/original/" + (pageNum < 16 ? "0":"") + Integer.toHexString(pageNum) + ".pg");
        BufferedReader reader = new BufferedReader( new FileReader(inputPage) );
        setAllBits(newOwnerOfPage, pageNum);
        //input data from file into physical memory
        for( int i = 0; i < 256; i++) {
            int data = reader.read();
            CPU.PM.setPhysicalMem(pageNum, i, data);
        }
    }


    private static void setAllBits(int newOwnerOfPage, int pageNum){
      for(int i=0; i< 8; i++)
          if( CPU.TLB[i].getvPageNum() == newOwnerOfPage){
              CPU.TLB[i].setV(1);
              CPU.TLB[i].setR(1);
              CPU.TLB[i].setD(0);
              CPU.TLB[i].setPageFrameNum(pageNum);
              CPU.TLB[i].setvPageNum(newOwnerOfPage);
          }
      CPU.vPT.setV(newOwnerOfPage, 1);
      CPU.vPT.setR(newOwnerOfPage, 1);
      CPU.vPT.setD(newOwnerOfPage, 1);
      CPU.vPT.setPageFrameNum(newOwnerOfPage, pageNum);
  }

  //this function, when the dirty bit was set, writes back into the files
  private static void writeToPGFile( int pageNum , int evictedOwnerOfPage) throws IOException {
      File outputPage = new File("../page_files/copy/" + (pageNum < 16 ? "0":"") + Integer.toHexString(pageNum) + ".pg");
      BufferedWriter writer = Files.newBufferedWriter(outputPage.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE);

      for( int i = 0; i < 256; i++ ) {
          //TODO: write to output file
          int data = CPU.PM.getPhysicalMem(pageNum, i);
      }
  }
  

  /*static int hardMiss( int pageNum ) throws IOException {

  static int hardMiss( int pageNum, VirtualPageTable vpt ) throws IOException {
      boolean check = true;
      int tempClock = 0;
      while(check) {
        if(CPU.vPT.getR(clockIndex) == 0) {
            //System.out.println("Evicted");
            Driver.evicted = Integer.toHexString(pageNum);
            Driver.dirty = 0;
            if(vpt.getD(pageNum) == 1) {
                writePage( pageNum );
                Driver.dirty = 1;
            }
            writeMemory( pageNum );
            check = false;
        }
        else {
            vpt.setR(clockIndex, 0);
            //System.out.println("Next");
        }
        tempClock = clockIndex;
        clockIndex++;
        if(clockIndex > 15)
            clockIndex = 0;
      }
      //System.out.println(tempClock + "");
      return tempClock;
  }*/
  

  static void resetR(TLBEntry[] TLB, VirtualPageTable pageTable) {
      for(int i = 0; i < TLB.length; i++) {
          TLB[i].setR(0);
      }
      pageTable.resetRBit();
  }


  ///testsss
    private static int numOfPagesLeft = 0;
    public static int hardMiss(int pageNum) throws IOException{
        if ( numOfPagesLeft < 16){  //there are still free pages available
            // just add them straight;
            writeToPhysicalMem(numOfPagesLeft, pageNum);
            numOfPagesLeft++;
            return numOfPagesLeft - 1;
        }
        else{   //so, all pages have been used up, so we need to activate the clock replacement algorithm
            for(int i = (clockIndex + 1) % 256; i != clockIndex; i = (i + 1) % 256){
                if (CPU.vPT.getV(i) == 1  && CPU.vPT.getR(i)==0){
                    clockIndex = i;
                    return evict(pageNum, i);
                }
            }
        }
        System.out.println("Your CPU has exploded, please ask for better value products!!");
        return -1;
    }
    //in here i will go to the page=pageNum and write to it the file [address].pg

    //in here i will do the same thing as above, but i will also reset all values in the
    //old vPT entry
    private static int evict(int newOwnerOfPageIndex, int evictedPageOwnerIndex) throws IOException{

        //if vPT ==> D bit = 1, then write
        int pageNum = CPU.vPT.getPageFrameNum(evictedPageOwnerIndex);
        if (CPU.vPT.getD(evictedPageOwnerIndex) == 1)
            writeToPGFile(pageNum, evictedPageOwnerIndex);
        resetVRDBits(evictedPageOwnerIndex);
        writeToPhysicalMem(pageNum, newOwnerOfPageIndex);
        return pageNum;
    }

    //this goes and resets the TLB and virutal page table values back to invalid 0|0|0
    private static void resetVRDBits(int evictedAddress){
        for(int i=0; i< 8; i++){
            if (CPU.TLB[i].getvPageNum() == evictedAddress){
                CPU.TLB[i].setV(0);
                CPU.TLB[i].setR(0);
                CPU.TLB[i].setPageFrameNum(0);
                CPU.TLB[i].setD(0);
                break;
            }
        }
        CPU.vPT.setR(evictedAddress, 0);
        CPU.vPT.setV(evictedAddress, 0);
        CPU.vPT.setD(evictedAddress, 0);
        CPU.vPT.setPageFrameNum(evictedAddress, -1);
    }
}
