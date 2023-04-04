package com.soebes.duplicate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.lang.System.out;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

class DuplicateFinder {

  static Function<Path, ChecksumForFileResult> forFile = path -> {
    try {
      ChecksumResult checksumResult = new CalculateChecksum().forFile(path.toFile());
      return new ChecksumForFileResult(checksumResult.digest(), path.getFileName()
          .toString(), checksumResult.readBytes());
    } catch (IOException | NoSuchAlgorithmException e) {
      //Translate to RuntimeException.
      throw new RuntimeException(e.getClass().getName(), e);
    }
  };

  private static String formatting(Long readBytes) {
    return " (" + String.format(Locale.GERMANY, "%,d", readBytes) + " bytes.)";
  }

  public static void main(String[] args) throws IOException {
    try (var list = Files.list(Paths.get(args[0]))) {
      var fileCollection = list.filter(Files::isRegularFile).toList();

      var checkSumResults = fileCollection.parallelStream().map(forFile).toList();

      out.println("Total of found files:: " + checkSumResults.size());
      var duplicateFiles = checkSumResults.stream()
          .collect(groupingBy(ChecksumForFileResult::digest))
          .entrySet()
          .stream()
          .filter(s -> s.getValue().size() > 1)
          .collect(toMap(Entry::getKey, Entry::getValue));

      out.println("Number of duplicates:" + duplicateFiles.size());

      duplicateFiles.forEach((key, value) -> {
        out.println("CheckSum: " + HexFormat.of().formatHex(key.byteArray()).toUpperCase());
        for (var item : value) {
          out.print("  " + item.fileName() + " (");
          out.println(formatting(item.readBytes()));
        }
      });

      var totalNumberOfReadBytes = checkSumResults.stream().mapToLong(ChecksumForFileResult::readBytes).sum();
      out.println("totalNumberOfReadBytes = " + formatting(totalNumberOfReadBytes));
    }
  }
}
