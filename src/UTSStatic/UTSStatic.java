package UTSStatic;

import static apgas.Constructs.asyncAt;
import static apgas.Constructs.finish;
import static apgas.Constructs.place;
import static apgas.Constructs.places;
import static apgas.Constructs.staticInit;

import UTSDynamic.SHA1Rand;
import UTSDynamic.TreeNode;
import apgas.Place;
import java.util.ArrayList;
import java.util.LinkedList;

public class UTSStatic {

  public static double den;
  public static long tasksCount[];
  public static long placeCounts[];

  public static void main(String[] args) {
    if (args.length != 5) {
      System.out.println(
          "Usage: UTSStatic <totalTasks> <seed> <depth> <sequential_depth> <branching_factor>");
      System.exit(1);
    }

    final int tasksPerPlace = Integer.parseInt(args[0]);
    final int seed = Integer.parseInt(args[1]);
    final int d = Integer.parseInt(args[2]);
    final int seqDepth = Integer.parseInt(args[3]);
    final int b = Integer.parseInt(args[4]);

    final long before = System.nanoTime();
    staticInit(
        () -> {
          UTSStatic.den = Math.log(b / (1.0 + b));
          UTSStatic.tasksCount = new long[tasksPerPlace];
        });
    placeCounts = new long[places().size()];

    final LinkedList<TreeNode> nodes = new LinkedList<>();
    TreeNode.push(new SHA1Rand(seed, d), nodes, den);
    long count = TreeNode.processSequential(nodes, den, seqDepth) + 1;

    final int nodesPerWorker = nodes.size() / (tasksPerPlace * places().size());
    finish(
        () -> {
          for (int w = 0; w < tasksPerPlace; ++w) {
            final int wid = w;
            for (Place p : places()) {
              asyncAt(
                  p,
                  () -> {
                    final int fromIndex = (p.id * tasksPerPlace + wid) * nodesPerWorker;
                    int toIndex = fromIndex + nodesPerWorker - 1;
                    if (p.id == places().size() - 1
                        && wid == tasksPerPlace - 1) { // last one gets remainder
                      toIndex = nodes.size() - 1;
                    }
                    final ArrayList<TreeNode> workerNodes = new ArrayList<>();
                    for (int i = fromIndex; i <= toIndex; ++i) {
                      workerNodes.add(nodes.get(i));
                    }
                    tasksCount[wid] = TreeNode.processDFS(workerNodes, den);
                  });
            }
          }
        });

    finish(
        () -> {
          for (Place p : places()) {
            asyncAt(
                p,
                () -> {
                  long placeCount = 0;
                  for (int i = 0; i < tasksPerPlace; ++i) {
                    placeCount += UTSStatic.tasksCount[i];
                  }
                  final long _placeCount = placeCount;
                  asyncAt(
                      place(0),
                      () -> {
                        UTSStatic.placeCounts[p.id] = _placeCount;
                      });
                });
          }
        });

    for (int i = 0; i < places().size(); ++i) {
      count += UTSStatic.placeCounts[i];
    }

    System.out.println("Result: " + count);
    final long after = System.nanoTime();
    System.out.println("Times:" + ((after - before) / 1E9) + " sec");
  }
}
