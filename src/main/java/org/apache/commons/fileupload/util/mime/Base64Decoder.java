/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.fileupload.util.mime;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @since 1.3
 */
final class Base64Decoder {

    /**
     * Decoding table value for invalid bytes.
     */
    private static final int INVALID_BYTE = -1; // must be outside range 0-63

    /**
     * Decoding table value for padding bytes, so can detect PAD afer conversion.
     */
    private static final int PAD_BYTE = -2; // must be outside range 0-63

    /**
     * Mask to treat byte as unsigned integer.
     */
    private static final int MASK_BYTE_UNSIGNED = 0xFF;

    /**
     * Number of bytes per encoded chunk - 4 6bit bytes produce 3 8bit bytes on output.
     */
    private static final int INPUT_BYTES_PER_CHUNK = 4;

    /**
     * Set up the encoding table.
     */
    private static final byte[] ENCODING_TABLE = {
        (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G',
        (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
        (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U',
        (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',
        (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
        (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',
        (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u',
        (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z',
        (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6',
        (byte) '7', (byte) '8', (byte) '9',
        (byte) '+', (byte) '/'
    };

    /**
     * The padding byte.
     */
    private static final byte PADDING = (byte) '=';

    /**
     * Set up the decoding table; this is indexed by a byte converted to an unsigned int,
     * so must be at least as large as the number of different byte values,
     * positive and negative and zero.
     */
    private static final byte[] DECODING_TABLE = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];

    static {
        // Initialise as all invalid characters
        for (int i = 0; i < DECODING_TABLE.length; i++) {
            DECODING_TABLE[i] = INVALID_BYTE;
        }
        // set up valid characters
        for (int i = 0; i < ENCODING_TABLE.length; i++) {
            DECODING_TABLE[ENCODING_TABLE[i]] = (byte) i;
        }
        // Allow pad byte to be easily detected after conversion
        DECODING_TABLE[PADDING] = PAD_BYTE;
    }

    /**
     * Hidden constructor, this class must not be instantiated.
     */
    private Base64Decoder() {
        // do nothing
    }

    /**
     * Decode the base 64 encoded byte data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @param data the buffer containing the Base64-encoded data
     * @param out the output stream to hold the decoded bytes
     *
     * @return the number of bytes produced.
     */
    public static int decode(byte[] data, OutputStream out) throws IOException {
        int        outLen = 0;
        byte [] cache = new byte[INPUT_BYTES_PER_CHUNK];
        int cachedBytes = 0;

        for (byte b : data) {
            final byte d = DECODING_TABLE[MASK_BYTE_UNSIGNED & b];
            if (d == INVALID_BYTE) {
                continue; // Ignore invalid bytes
            }
            cache[cachedBytes++] = d;
            if (cachedBytes == INPUT_BYTES_PER_CHUNK) {
                // Convert 4 6-bit bytes to 3 8-bit bytes
                // CHECKSTYLE IGNORE MagicNumber FOR NEXT 1 LINE
                out.write((cache[0] << 2) | (cache[1] >> 4)); // 6 bits of b1 plus 2 bits of b2
                outLen++;
                if (cache[2] != PAD_BYTE) {
                    // CHECKSTYLE IGNORE MagicNumber FOR NEXT 1 LINE
                    out.write((cache[1] << 4) | (cache[2] >> 2)); // 4 bits of b2 plus 4 bits of b3
                    outLen++;
                    if (cache[3] != PAD_BYTE) {
                        // CHECKSTYLE IGNORE MagicNumber FOR NEXT 1 LINE
                        out.write((cache[2] << 6) | cache[3]);        // 2 bits of b3 plus 6 bits of b4
                        outLen++;
                    }
                } else if (cache[3] != PAD_BYTE) { // if byte 3 is pad, byte 4 must be pad too
                    throw new IOException("Invalid Base64 input: incorrect padding");                    
                }
                cachedBytes = 0;
            }
        }
        // Check for anything left over
        if (cachedBytes != 0){
            throw new IOException("Invalid Base64 input: truncated");
        }
        return outLen;
    }
}
