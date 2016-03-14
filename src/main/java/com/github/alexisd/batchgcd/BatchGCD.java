package com.github.alexisd.batchgcd;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alexisd.batchgcd.utils.GzippedByteSource;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

public class BatchGCD {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchGCD.class);
    private static final String SSH_RSA = "ssh-rsa";

    /**
     * Compute GCD of each number with all the other ones using Bernstein's Batch GCD algo.
     *
     * @see <a href="http://facthacks.cr.yp.to/batchgcd.html">http://facthacks.cr.yp.to/batchgcd.html</a>
     * @param numbers A list of number we want to find factors for.
     * @return The GCD for each number, can be equal to the number itself, if
     *          1. the number appears more than once in numbers
     *          2. the number is a factor of a bigger number
     *          3. the number is composite, and all its factors appear in other numbers
     */
    public static BigInteger[] batchGCD(BigInteger[] numbers) {
        LOGGER.info("Building Product Tree...");
        ProductTreeTask.ProductTreeNode productTree = ProductTreeTask.productTree(numbers);
        LOGGER.info("Built Product Tree...");
        LOGGER.info("Building Remainder Tree...");
        BigInteger[] remainders = RemainderTreeTask.remainderTree(productTree);
        LOGGER.info("Built Remainder Tree...");

        LOGGER.info("Computing GCDs...");
        BigInteger[] gcds = new BigInteger[numbers.length];

        for (int i = 0; i < numbers.length; ++i) {
            gcds[i] = numbers[i].gcd(remainders[i].divide(numbers[i]));
        }
        LOGGER.info("Computed GCDs...");
        return gcds;
    }

    /**
     * Tries to factor a list of composite numbers using the batchGCD method above. Further checking is needed to check
     * if some of those factors are also composite (or just use .isProbablePrime on each factors). Checking for trivial factors, e.g.
     * all primes below 1.000.000 could be useful too.
     *
     * @param numbers The numbers to factor.
     * @return A map of numbers that could be factored and the found factors.
     */
    public static Map<BigInteger, List<BigInteger>> factor(BigInteger[] numbers) {
        BigInteger[] gcds = batchGCD(numbers);
        Map<BigInteger, List<BigInteger>> result = Maps.newHashMap();

        for (int i = 0; i < numbers.length; ++i) {
            BigInteger gcd = gcds[i];
            BigInteger number = numbers[i];

            if (gcd.equals(BigInteger.ONE)) {
                continue;
            }

            // if the gcd is equal to the number itself, we revert back to the naive algorithm
            if (gcd.equals(number)) {
                for (BigInteger k : numbers) {
                    BigInteger slowGCD = k.gcd(number);

                    if (!slowGCD.equals(number)) {
                        result.put(number, Lists.newArrayList(slowGCD, number.divide(slowGCD)));
                    }
                }

                continue;
            }

            result.put(number, Lists.newArrayList(gcd, number.divide(gcd)));
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        // read all the SSH keys from http://natmchugh.blogspot.co.uk/2015/06/batch-gcd-ssh-key-challenge.html
        BigInteger[] numbers = new GzippedByteSource(Resources.asByteSource(Resources.getResource("10000_ssh_pub_keys.gz")))
            .asCharSource(Charsets.UTF_8)
            .readLines()
            .stream()
            .map(String::trim)
            .map(BatchGCD::parseSSHPublicKey)
            .toArray(BigInteger[]::new);

        LOGGER.info("Factored primes:\n{}", Joiner.on("\n").withKeyValueSeparator("=").join(factor(numbers)));
    }

    /**
     * Helper function to parse base64 encoded SSH public keys.
     *
     * @see <a href="https://tools.ietf.org/html/rfc4253#section-6.6">https://tools.ietf.org/html/rfc4253#section-6.6</a>
     * @param key The base64 encoded key.
     * @return The RSA N, e is discarded.
     */
    private static BigInteger parseSSHPublicKey(String key) {
        byte[] bytes = Base64.getDecoder().decode(key);
        int prefixSize = new BigInteger(Arrays.copyOfRange(bytes, 0, 4)).intValue();
        Preconditions.checkArgument(new String(Arrays.copyOfRange(bytes, 4, 4 + prefixSize)).equals(SSH_RSA), "This methods assumes ssh-rsa keys.");
        // skip after ssh-rsa
        int currentPosition = 4 + prefixSize;
        // skip after e
        currentPosition += 4 + new  BigInteger(Arrays.copyOfRange(bytes, currentPosition, currentPosition+4)).intValue();
        int nSize = new BigInteger(Arrays.copyOfRange(bytes, currentPosition, currentPosition + 4)).intValue();
        // skip to the begining of n
        currentPosition +=4;
        return new BigInteger(Arrays.copyOfRange(bytes, currentPosition, currentPosition + nSize));
    }
}
