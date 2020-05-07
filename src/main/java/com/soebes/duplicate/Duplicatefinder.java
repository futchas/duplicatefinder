package com.soebes.duplicate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Duplicatefinder {
  
  private static final Function<Path, ChecksumForFileResult> checksumForFile = CheckSum::forFile;
  
  private static String formatting(Long readBytes) {
    StringBuilder sb = new StringBuilder();
    sb.append(" (");
    
    sb.append(String.format(Locale.GERMANY, "%,d", readBytes));
    sb.append(" bytes.)");
    return sb.toString();
  }
  
  public static void main(String[] args) {
    
    try {
      
      List<Path> fileCollection = Files.list(Paths.get(args[0]))
        .filter(Files::isRegularFile)
        .collect(toList());
      
      List<ChecksumForFileResult> checkSumResults = fileCollection.parallelStream()
        .map(checksumForFile)
        .collect(toList());
  
      System.out.println("Total of found files:: " + checkSumResults.size());
      checkSumResults.stream().forEach(item -> {
        System.out.print(Convert.toHex(item.getDigest()));
        System.out.print(formatting(item.getReadBytes()));
        System.out.println(" " + item.getFileName());
      });
  
      Map<ByteArrayWrapper, List<ChecksumForFileResult>> duplicateFiles = checkSumResults.stream()
        .collect(groupingBy(ChecksumForFileResult::getDigest))
        .entrySet()
        .stream()
        .filter(s -> s.getValue().size() > 1)
        .collect(toMap(Entry::getKey, Entry::getValue));
  
      System.out.println("Number of duplicates:" + duplicateFiles.size());
  
      for (Entry<ByteArrayWrapper, List<ChecksumForFileResult>> element : duplicateFiles.entrySet()) {
        System.out.println("CheckSum: " + Convert.toHex(element.getKey()));
        for (ChecksumForFileResult item : element.getValue()) {
          System.out.print("  " + item.getFileName() + " (");
          System.out.println(formatting(item.getReadBytes()));
        }
      }

      long readBytesTotal = checkSumResults.stream().mapToLong(ChecksumForFileResult::getReadBytes).sum();
      System.out.println("readBytesTotal = " + formatting(readBytesTotal));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
