package WordCountStatic;

import static apgas.Constructs.*;

import apgas.Place;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordCountStatic {
  public static HashMap<String, Integer> occurences[];
  public static HashMap results[];

  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      System.out.println("Usage: WordCountStatic <workerPerPlace> <textfile> <repetitions>");
      System.out.println("Repetitions should be divisible by (workerPerPlace * places count)");
      System.out.println("textfile must be UTF8 encoded");
      System.exit(1);
    }

    final int workerPerPlace = Integer.parseInt(args[0]);
    final String textFile = args[1];
    final int repetitions = Integer.parseInt(args[2]);
    final int repetitionsPerWorker = repetitions / (workerPerPlace * places().size());
    final int remainder = repetitions % (workerPerPlace * places().size());

    final long before = System.nanoTime();
    staticInit(
        () -> {
          WordCountStatic.occurences = new HashMap[workerPerPlace];
          for (int i = 0; i < workerPerPlace; ++i) {
            WordCountStatic.occurences[i] = new HashMap<>();
          }
        });

    finish(
        () -> {
          for (Place p : places()) {
            for (int j = 0; j < workerPerPlace; ++j) {
              final int aid = j;
              asyncAt(
                  p,
                  () -> {
                    int myRepetitions = repetitionsPerWorker;
                    if (p.id * workerPerPlace + aid < remainder) {
                      ++myRepetitions;
                    }
                    for (int i = 0; i < myRepetitions; ++i) {
                      final List<String> lines =
                          Files.readAllLines(Paths.get(textFile), StandardCharsets.UTF_8);
                      for (String line : lines) {
                        String[] words = line.split(" ");
                        for (String word : words) {
                          final int oldValue = occurences[aid].getOrDefault(word, 0);
                          occurences[aid].put(word, oldValue + 1);
                        }
                      }
                    }
                  });
            }
          }
        });

    results = new HashMap[places().size()];
    finish(
        () -> {
          for (Place p : places()) {
            asyncAt(
                p,
                () -> {
                  final HashMap<String, Integer> result = new HashMap<String, Integer>();
                  for (int i = 0; i < workerPerPlace; ++i) {
                    for (Map.Entry<String, Integer> entry :
                        WordCountStatic.occurences[i].entrySet()) {
                      final int tmpResult = result.getOrDefault(entry.getKey(), 0);
                      result.put(entry.getKey(), tmpResult + entry.getValue());
                    }
                  }
                  final int pid = here().id;
                  asyncAt(
                      place(0),
                      () -> {
                        WordCountStatic.results[pid] = result;
                      });
                });
          }
        });

    final Map<String, Integer> finalResult = new HashMap<>();
    for (Map<String, Integer> result : results) {
      for (Map.Entry<String, Integer> entry : result.entrySet()) {
        final int oldValue = finalResult.getOrDefault(entry.getKey(), 0);
        finalResult.put(entry.getKey(), oldValue + entry.getValue());
      }
    }

    long sum = 0;
    for (Map.Entry<String, Integer> entry : finalResult.entrySet()) {
      System.out.println(entry.getKey() + ": " + entry.getValue());
      sum += entry.getValue();
    }
    System.out.println("Sum: " + sum);
    final long after = System.nanoTime();
    System.out.println("Times:" + ((after - before) / 1E9) + " sec");
  }
}
