package WordCountDynamic;

import static apgas.Constructs.finishAsyncAny;
import static apgas.Constructs.getThreadLocalResult;
import static apgas.Constructs.reduceAsyncAny;
import static apgas.Constructs.setThreadLocalResult;
import static apgas.Constructs.staticAsyncAny;
import static apgas.Constructs.staticInit;

import apgas.impl.ResultAsyncAny;
import apgas.impl.Worker;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordCountDynamic {
  public static String textFile;

  public static class WordCountResult extends ResultAsyncAny<HashMap<String, Integer>> {
    public WordCountResult() {
      this.result = new HashMap<>();
    }

    @Override
    public void mergeResult(ResultAsyncAny<HashMap<String, Integer>> result) {
      if (result == null) return;
      mergeResult(result.getResult());
    }

    @Override
    public void mergeResult(HashMap<String, Integer> result) {
      if (result == null) return;
      for (Map.Entry<String, Integer> entry : result.entrySet()) {
        int oldValue = this.result.getOrDefault(entry.getKey(), 0);
        this.result.put(entry.getKey(), oldValue + entry.getValue());
      }
    }

    @Override
    public void display() {
      long sum = 0;
      for (Map.Entry<String, Integer> entry : result.entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
        sum += entry.getValue();
      }
      System.out.println("Sum: " + sum);
    }

    @Override
    public ResultAsyncAny clone() {
      WordCountResult copy = new WordCountResult();
      copy.result = new HashMap<>(this.result);
      return copy;
    }
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: WordCountDynamic <textfile> <repetitions>");
      System.out.println("Repetitions should be divisible by place count");
      System.exit(1);
    }

    final String textFile = args[0];
    final int repetitions = Integer.parseInt(args[1]);

    final long before = System.nanoTime();
    staticInit(
        () -> {
          WordCountDynamic.textFile = textFile;
        });

    finishAsyncAny(
        () -> {
          staticAsyncAny(
              () -> {
                final Worker worker = (Worker) Thread.currentThread();
                final int id = worker.getMyID();
                WordCountResult result = getThreadLocalResult();
                if (result == null) {
                  result = new WordCountResult();
                  setThreadLocalResult(result);
                }
                final Map<String, Integer> occurences = result.getResult();
                final List<String> lines =
                    Files.readAllLines(
                        Paths.get(WordCountDynamic.textFile), StandardCharsets.UTF_8);
                for (String line : lines) {
                  String[] words = line.split(" ");
                  for (String word : words) {
                    int oldValue = occurences.getOrDefault(word, 0);
                    occurences.put(word, oldValue + 1);
                  }
                }
              },
              repetitions);
        });

    reduceAsyncAny().display();
    final long after = System.nanoTime();
    System.out.println("Times:" + ((after - before) / 1E9) + " sec");
  }
}
