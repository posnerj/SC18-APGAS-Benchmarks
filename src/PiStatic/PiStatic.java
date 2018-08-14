package PiStatic;

import static apgas.Constructs.asyncAt;
import static apgas.Constructs.finish;
import static apgas.Constructs.places;

import apgas.Place;
import apgas.util.GlobalRef;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class PiStatic {

  public static void main(String[] args) {
    if (args.length != 3) {
      System.out.println("Usage: PiStatic <workerPerPlace> <N> <tasksPerWorker>");
      System.out.println("N should be divisible by (workerPerPlace * places().size())");
      System.exit(1);
    }

    final int numPlaces = places().size();

    final int workerPerPlace = Integer.parseInt(args[0]);
    final int n = Integer.parseInt(args[1]);
    final int tasksPerWorker = Integer.parseInt(args[2]);
    final long POINTS = 1L << n; // POINTS = 2^n
    final long pointsPerTask = POINTS / (workerPerPlace * tasksPerWorker * places().size());

    GlobalRef<AtomicLong> result = new GlobalRef<>(new AtomicLong());

    final long before = System.nanoTime();
    finish(
        () -> {
          for (Place p : places()) {
            for (int j = 0; j < workerPerPlace; ++j) {
              for (int t = 0; t < tasksPerWorker; t++) {
                asyncAt(
                    p,
                    () -> {
                      long tmpCount = 0;
                      for (long i = 0; i < pointsPerTask; ++i) {
                        final double x = 2 * ThreadLocalRandom.current().nextDouble() - 1.0;
                        final double y = 2 * ThreadLocalRandom.current().nextDouble() - 1.0;
                        tmpCount += (x * x + y * y <= 1) ? 1 : 0;
                      }
                      final long transferCount = tmpCount;
                      asyncAt(
                          result.home(),
                          () -> {
                            result.get().addAndGet(transferCount);
                          });
                    });
              }
            }
          }
        });
    System.out.println("Pi is roughly " + 4.0 * result.get().get() / POINTS);
    final long after = System.nanoTime();
    System.out.println("Times:" + ((after - before) / 1E9) + " sec");
  }
}
