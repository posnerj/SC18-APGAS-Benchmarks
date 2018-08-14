package UTSDynamic;

import static apgas.Constructs.PLUSLONG;
import static apgas.Constructs.asyncAny;
import static apgas.Constructs.finishAsyncAny;
import static apgas.Constructs.mergeAsyncAny;
import static apgas.Constructs.reduceAsyncAnyLong;
import static apgas.Constructs.staticInit;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UTSDynamic {
  public static double den;

  public static void run(ArrayList<TreeNode> nodes) {
    long count = 0;
    while (!nodes.isEmpty()) {
      if (nodes.size() > 1) {
        final int splitCount = nodes.size() / 2;
        final List<TreeNode> splittedList = nodes.subList(0, splitCount);
        final ArrayList<TreeNode> split = new ArrayList<>(splittedList);
        splittedList.clear();
        asyncAny(() -> UTSDynamic.run(split));
      }
      for (int i = 0; i < 512 && !nodes.isEmpty(); ++i) {
        TreeNode node = nodes.remove(nodes.size() - 1);
        ++count;
        count += node.expand(nodes, den);
      }
    }
    mergeAsyncAny(count, PLUSLONG);
  }

  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("Usage: UTSDynamic <seed> <depth> <sequential_depth> <branching_factor>");
      System.exit(1);
    }

    final int seed = Integer.parseInt(args[0]);
    final int d = Integer.parseInt(args[1]);
    final int seqDepth = Integer.parseInt(args[2]);
    final int b = Integer.parseInt(args[3]);

    final long before = System.nanoTime();
    staticInit(
        () -> {
          UTSDynamic.den = Math.log(b / (1.0 + b));
        });

    finishAsyncAny(
        () -> {
          asyncAny(
              () -> {
                LinkedList<TreeNode> list = new LinkedList<>();
                TreeNode.push(new SHA1Rand(seed, d), list, den);
                long count = TreeNode.processSequential(list, den, seqDepth);
                mergeAsyncAny(count + 1, PLUSLONG);
                UTSDynamic.run(new ArrayList(list));
              });
        });

    System.out.println("UTS Result=" + reduceAsyncAnyLong(PLUSLONG));
    final long after = System.nanoTime();
    System.out.println("Times:" + ((after - before) / 1E9) + " sec");
  }
}
