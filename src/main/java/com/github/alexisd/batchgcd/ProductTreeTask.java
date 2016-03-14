package com.github.alexisd.batchgcd;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import com.google.common.base.Preconditions;

class ProductTreeTask extends RecursiveTask<ProductTreeTask.ProductTreeNode> {
    private static int DO_NOT_FORK_IF_LESS_THAN_THIS_ELEMENTS = 64;

    final BigInteger[] numbers;
    final int i;
    final int j;

    private ProductTreeTask(BigInteger[] numbers, int i, int j) {
        this.numbers = numbers;
        this.i = i;
        this.j = j;
    }

    private ProductTreeTask(BigInteger[] numbers) {
        this(numbers, 0, numbers.length);
    }

    @Override
    protected ProductTreeNode compute() {
        Preconditions.checkArgument(i < j, "The invariant for this task is being broken.");

        // base case: single element
        if (j - i == 1) {
            return new ProductTreeNode(this.numbers[i]);
        }

        // base case: two elements
        if (j - i == 2) {
            BigInteger a = numbers[i];
            BigInteger b = numbers[i + 1];
            return new ProductTreeNode(a.multiply(b), new ProductTreeNode(a), new ProductTreeNode(b));
        }

        // general case, split array in half and delegate to sub tasks
        int halfPoint = (i + 1 + j) / 2;

        ProductTreeTask leftTask = new ProductTreeTask(numbers, i, halfPoint);
        ProductTreeTask rightTask = new ProductTreeTask(numbers, halfPoint, j);

        ProductTreeNode left, right;

        // do not fork if this is small enough to be computed in the current thread
        if (j - i < DO_NOT_FORK_IF_LESS_THAN_THIS_ELEMENTS) {
            left = leftTask.compute();
            right = rightTask.compute();
        } else {
            leftTask.fork();
            right = rightTask.compute();
            left = leftTask.join();
        }

        return new ProductTreeNode(left.value.multiply(right.value), left, right);
    }

    static ProductTreeNode productTree(BigInteger[] numbers) {
        Preconditions.checkArgument(numbers.length > 1, "Can compute GCDs with less than 2 numbers.");
        return ForkJoinPool.commonPool().invoke(new ProductTreeTask(numbers));
    }

    class ProductTreeNode {
        final BigInteger value;
        final Optional<ProductTreeNode> left;
        final Optional<ProductTreeNode> right;

        private ProductTreeNode(BigInteger value, ProductTreeNode left, ProductTreeNode right) {
            this.value = value;
            this.left = Optional.ofNullable(left);
            this.right = Optional.ofNullable(right);
        }

        private ProductTreeNode(BigInteger value) {
            this(value, null, null);
        }
    }
}
