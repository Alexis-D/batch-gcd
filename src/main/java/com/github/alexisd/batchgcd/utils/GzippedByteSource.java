package com.github.alexisd.batchgcd.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.google.common.io.ByteSource;

public class GzippedByteSource extends ByteSource {
    final ByteSource byteSource;

    public GzippedByteSource(ByteSource byteSource) {
        this.byteSource = byteSource;
    }

    @Override
    public InputStream openStream() throws IOException {
        return new GZIPInputStream(byteSource.openStream());
    }
}
