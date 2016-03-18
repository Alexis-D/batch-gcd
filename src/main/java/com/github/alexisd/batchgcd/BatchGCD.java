package com.github.alexisd.batchgcd;

import java.io.IOException;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.primes.Primes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alexisd.batchgcd.utils.GzippedByteSource;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
     * Tries to factor a list of composite numbers using the batchGCD method above. Further checking is needed to verify
     * if some of those factors are also composite (e.g. using .isProbablePrime on each factors).
     *
     * @param numbers The numbers to factor.
     * @return A map of numbers that could be factored and the found factors.
     */
    public static Map<BigInteger, List<BigInteger>> factor(BigInteger[] numbers) {
        BigInteger[] gcds = batchGCD(numbers);
        LOGGER.info("Factoring using GCDs...");
        Map<BigInteger, List<BigInteger>> result =  IntStream.range(0, numbers.length).parallel().mapToObj(i -> {
            BigInteger gcd = gcds[i];
            BigInteger number = numbers[i];

            if (gcd.equals(BigInteger.ONE)) {
                return null;
            }

            // if the gcd is equal to the number itself, we revert back to the naive algorithm
            if (gcd.equals(number)) {
                LOGGER.warn("Found a number that appears more than once!");
                for (BigInteger k : numbers) {
                    BigInteger slowGCD = k.gcd(number);

                    if (!slowGCD.equals(number)) {
                        return new AbstractMap.SimpleImmutableEntry<>(number, Lists.newArrayList(slowGCD, number.divide(slowGCD)));
                    }
                }

                return null;
            }

            return new AbstractMap.SimpleImmutableEntry<>(number, Lists.newArrayList(gcd, number.divide(gcd)));
        }).filter(x -> x != null)
        .map(e -> {
            List<BigInteger> resultList = Lists.newArrayList();

            for (BigInteger n : e.getValue()) {
                if (n.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == -1) {
                    Primes.primeFactors(n.intValue())
                        .stream()
                        .map(BigInteger::valueOf)
                        .collect(Collectors.toCollection(() -> resultList));
                } else {
                    resultList.add(n);
                }
            }

            return new AbstractMap.SimpleImmutableEntry<>(e.getKey(), resultList);
        }).collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
        LOGGER.info("Factored using GCDs...");

        return result;
    }

    public static void main(String[] args) throws IOException {
//        // read all the SSH keys from http://natmchugh.blogspot.co.uk/2015/06/batch-gcd-ssh-key-challenge.html
//        BigInteger[] numbers = new GzippedByteSource(Resources.asByteSource(Resources.getResource("10000_ssh_pub_keys.gz")))
//            .asCharSource(Charsets.UTF_8)
//            .readLines()
//            .stream()
//            .map(String::trim)
//            .map(BatchGCD::parseSSHPublicKey)
//            .toArray(BigInteger[]::new);

        BigInteger[] numbers = Resources.readLines(Resources.getResource("certs/moduli"), Charsets.UTF_8)
            .stream()
            .map(modulus -> new BigInteger(modulus, 16))
            .toArray(BigInteger[]::new);

        Map<BigInteger, List<BigInteger>> factored = factor(numbers);
        Map<BigInteger, String> prettyFactored = factored.entrySet()
            .stream()
            .map(e -> {
                String joined = Joiner.on(" × ").join(e.getValue().stream().sorted().map(BigInteger::toString).iterator());
                return new AbstractMap.SimpleImmutableEntry<>(e.getKey(), joined);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        LOGGER.info("Factored primes:\n{}", Joiner.on("\n").withKeyValueSeparator(" = ").join(prettyFactored));
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
        // skip to the beginning of n
        currentPosition +=4;
        return new BigInteger(Arrays.copyOfRange(bytes, currentPosition, currentPosition + nSize));
    }
}
