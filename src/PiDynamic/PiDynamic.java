package PiDynamic;

import static apgas.Constructs.*;

import java.util.concurrent.ThreadLocalRandom;

public class PiDynamic {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: PiDynamic <N> <tasksPerWorker>");
      System.out.println("N should be divisible by (workerPerPlace * places().size())");
      System.exit(1);
    }

    final int numPlaces = places().size();
    final int numLocalWorker = numLocalWorkers();
    final int numAllWorker = numLocalWorker * numPlaces;
    final int n = Integer.parseInt(args[0]);
    final int tasksPerWorker = Integer.parseInt(args[1]);

    final long POINTS = 1L << n; // POINTS = 2^n
    final int numTasks = numAllWorker * tasksPerWorker;
    final long pointsPerTask = POINTS / numTasks;

    final long before = System.nanoTime();
    finishAsyncAny(
        () -> {
          staticAsyncAny(
              () -> {
                long tmpCount = 0;
                for (long j = 0; j < pointsPerTask; ++j) {
                  final double x = 2 * ThreadLocalRandom.current().nextDouble() - 1.0;
                  final double y = 2 * ThreadLocalRandom.current().nextDouble() - 1.0;
                  tmpCount += (x * x + y * y <= 1) ? 1 : 0;
                }
                mergeAsyncAny(tmpCount, PLUSLONG);
              },
              numTasks);
        });

    System.out.println("Pi is roughly " + 4.0 * reduceAsyncAnyLong((x, y) -> x + y) / POINTS);
    final long after = System.nanoTime();
    System.out.println("Times:" + ((after - before) / 1E9) + " sec");
  }
}
