# Batch GCD, in Java, using Fork/Join

Batch GCD is an algorithm which was created by D. J. Bernstein to be able to
attempt to factor RSA keys, cf. [FactHacks: Batch
gcd](http://facthacks.cr.yp.to/batchgcd.html). It has successfully been used
in the paper [Mining Your Ps and Qs: Detection of Widespread Weak Keys in
Network Devices](https://factorable.net/weakkeys12.extended.pdf) to factor
real world keys.

Wondering what was the proper way to parallelize such problems and ran into
the Fork/Join idea from Doug Lea's paper, [A Java Fork/Join
Framework](http://gee.cs.oswego.edu/dl/papers/fj.pdf). Batch GCD is a prime
candidate for this kind of parallelization since the algorithm involves to
tree traversals.

In order to check whether this works there's a simple `main` method which
solves [Nat McHugh's ssh key factoring
challenge](http://natmchugh.blogspot.co.uk/2015/06/batch-gcd-ssh-key-challenge.html).
Other than that the code is untested.

If you're looking for more resources on Batch GCD or Fork Join, you might find
those links helpful:

- [Factoring RSA Moduli. Part I.](http://windowsontheory.org/2012/05/15/979/)
- [Beginner's Introduction to Java's ForkJoin Framework](Beginner's
  Introduction to Java's ForkJoin Framework)

Running the project should be a matter of runing the following:

    ./gradlew run