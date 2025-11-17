package com.swe.core.RPCinteface;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Interface to use the RPC.
 */
public interface AbstractRPC {

    void subscribe(String methodName, Function<byte[], byte[]> method);

    Thread connect(int portNumber) throws IOException, InterruptedException, ExecutionException;

    CompletableFuture<byte[]> call(String methodName, byte[] data);
}
