package com.github.alexisd.batchgcd;

import java.math.BigInteger;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import com.google.common.collect.ObjectArrays;

public class RemainderTreeTask extends RecursiveTask<BigInteger[]> {
    private static int DO_NOT_FORK_IF_LESS_THAN_THIS_BITS = 65536;

    final BigInteger n;
    final ProductTreeTask.ProductTreeNode root;

    RemainderTreeTask(BigInteger n, ProductTreeTask.ProductTreeNode root) {
        this.n = n;
        this.root = root;
    }

    @Override
    protected BigInteger[] compute() {
        if (!root.left.isPresent()) {
            BigInteger[] single = {n.mod(root.value.pow(2))};
            return single;
        }

        BigInteger nextN;

        // if n is smaller or equals to the square root of the modulo there's no point in computing the modulo
        if (n.compareTo(root.value) < 1) {
            nextN = n;
        } else {
            nextN = n.mod(root.value.pow(2));
        }

        RemainderTreeTask leftTask = new RemainderTreeTask(nextN, root.left.get());
        RemainderTreeTask rightTask = new RemainderTreeTask(nextN, root.right.get());

        BigInteger[] left, right;

        if (nextN.bitLength() < DO_NOT_FORK_IF_LESS_THAN_THIS_BITS) {
            left = leftTask.compute();
            right = rightTask.compute();
        } else {
            leftTask.fork();
            right = rightTask.compute();
            left = leftTask.join();
        }

        return ObjectArrays.concat(left, right, BigInteger.class);
    }

    public static BigInteger[] remainderTree(ProductTreeTask.ProductTreeNode productTree) {
        return ForkJoinPool.commonPool().invoke(new RemainderTreeTask(productTree.value, productTree));
    }
}
