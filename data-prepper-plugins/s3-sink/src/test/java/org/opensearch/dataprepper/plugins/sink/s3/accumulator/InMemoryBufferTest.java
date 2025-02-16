/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.sink.s3.accumulator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryBufferTest {

    public static final int MAX_EVENTS = 55;
    @Mock
    private S3Client s3Client;
    @Mock
    private Supplier<String> bucketSupplier;
    @Mock
    private Supplier<String> keySupplier;
    private InMemoryBuffer inMemoryBuffer;

    @Test
    void test_with_write_event_into_buffer() throws IOException {
        inMemoryBuffer = new InMemoryBuffer(s3Client, bucketSupplier, keySupplier);

        while (inMemoryBuffer.getEventCount() < MAX_EVENTS) {
            OutputStream outputStream = inMemoryBuffer.getOutputStream();
            outputStream.write(generateByteArray());
            int eventCount = inMemoryBuffer.getEventCount() +1;
            inMemoryBuffer.setEventCount(eventCount);
        }
        assertThat(inMemoryBuffer.getSize(), greaterThanOrEqualTo(54110L));
        assertThat(inMemoryBuffer.getEventCount(), equalTo(MAX_EVENTS));
        assertThat(inMemoryBuffer.getDuration(), greaterThanOrEqualTo(0L));

    }

    @Test
    void test_with_write_event_into_buffer_and_flush_toS3() throws IOException {
        inMemoryBuffer = new InMemoryBuffer(s3Client, bucketSupplier, keySupplier);

        while (inMemoryBuffer.getEventCount() < MAX_EVENTS) {
            OutputStream outputStream = inMemoryBuffer.getOutputStream();
            outputStream.write(generateByteArray());
            int eventCount = inMemoryBuffer.getEventCount() +1;
            inMemoryBuffer.setEventCount(eventCount);
        }
        assertDoesNotThrow(() -> {
            inMemoryBuffer.flushToS3();
        });
    }

    @Test
    void test_uploadedToS3_success() {
        inMemoryBuffer = new InMemoryBuffer(s3Client, bucketSupplier, keySupplier);
        Assertions.assertNotNull(inMemoryBuffer);
        assertDoesNotThrow(() -> {
            inMemoryBuffer.flushToS3();
        });
    }

    @Test
    void test_uploadedToS3_fails() {
        inMemoryBuffer = new InMemoryBuffer(s3Client, bucketSupplier, keySupplier);
        Assertions.assertNotNull(inMemoryBuffer);
        SdkClientException sdkClientException = mock(SdkClientException.class);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(sdkClientException);
        SdkClientException actualException = assertThrows(SdkClientException.class, () -> inMemoryBuffer.flushToS3());

        assertThat(actualException, Matchers.equalTo(sdkClientException));
    }

    private byte[] generateByteArray() {
        byte[] bytes = new byte[1000];
        for (int i = 0; i < 1000; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }
}